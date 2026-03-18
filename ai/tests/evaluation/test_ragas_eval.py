# ai/tests/evaluation/test_ragas_eval.py
"""RAGAS 기반 RAG 평가 스크립트.

사용법:
    cd ai
    python tests/evaluation/test_ragas_eval.py --config plain_config.yaml
    python tests/evaluation/test_ragas_eval.py --config optimized_config.yaml
"""

import argparse
import asyncio
import json
import os
import sys
from pathlib import Path

import yaml

sys.path.insert(0, str(Path(__file__).parent.parent.parent))

from app.core.config import settings

# RAGAS가 LLM-as-judge를 위해 OpenAI API를 사용할 때 GMS 프록시를 사용하도록 환경변수 주입
# (RAGAS는 내부적으로 langchain ChatOpenAI를 사용하며, OPENAI_API_KEY와 OPENAI_BASE_URL을 참조)
os.environ["OPENAI_API_KEY"] = settings.gms_api_key
os.environ["OPENAI_BASE_URL"] = settings.gms_base_url

from datasets import Dataset
from ragas import evaluate
from ragas.metrics import (
    answer_relevancy,
    context_precision,
    context_recall,
    faithfulness,
)

from app.services.retrieval_service import BM25Index, RetrievalService
from app.services.vectorstore import VectorStoreService

GOLDEN_SET_PATH = Path(__file__).parent / "golden_set.json"
EVAL_DIR = Path(__file__).parent


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="RAGAS RAG 평가 스크립트")
    parser.add_argument(
        "--config",
        type=str,
        default="plain_config.yaml",
        help="RAG config YAML 파일명 (ai/tests/evaluation/ 기준)",
    )
    return parser.parse_args()


def load_config(config_file: str) -> dict:
    config_path = EVAL_DIR / config_file
    with open(config_path, encoding="utf-8") as f:
        return yaml.safe_load(f)


def load_golden_set(path: Path) -> list[dict]:
    with open(path, encoding="utf-8") as f:
        return json.load(f)


async def run_rag_pipeline(
    retrieval_service: RetrievalService,
    question: str,
    config: dict,
    llm,
) -> dict:
    """단일 질문에 대해 RAG 파이프라인 실행. 반환: {"answer": str, "contexts": list[str]}"""
    results = await retrieval_service.retrieve(
        query=question,
        top_k=config.get("retrieval_top_k", 5),
        search_strategy=config.get("search_strategy", "hybrid"),
    )
    contexts = [r.content for r in results]

    from langchain_core.messages import HumanMessage, SystemMessage

    context_str = "\n\n".join(
        f"[문서 {i+1}]\n{c}" for i, c in enumerate(contexts)
    )
    messages = [
        SystemMessage(
            content="당신은 세법 전문 AI 어시스턴트입니다. 아래 참고 문서만을 근거로 질문에 답하세요. 참고 문서에 없는 내용은 모른다고 답하세요."
        ),
        HumanMessage(content=f"참고 문서:\n{context_str}\n\n질문: {question}"),
    ]
    response = await llm.ainvoke(messages)
    return {"answer": response.content, "contexts": contexts}


async def build_ragas_dataset(
    golden_set: list[dict],
    retrieval_service: RetrievalService,
    config: dict,
    llm,
) -> Dataset:
    """golden_set + RAG 응답을 결합해 RAGAS Dataset 생성."""
    questions, answers, contexts, ground_truths = [], [], [], []

    for i, item in enumerate(golden_set):
        q = item["question"]
        print(f"  [{i+1}/{len(golden_set)}] {q[:40]}...")
        rag_result = await run_rag_pipeline(retrieval_service, q, config, llm)
        questions.append(q)
        answers.append(rag_result["answer"])
        contexts.append(rag_result["contexts"])
        ground_truths.append(item["ground_truth"])

    return Dataset.from_dict(
        {
            "question": questions,
            "answer": answers,
            "contexts": contexts,
            "ground_truth": ground_truths,
        }
    )


def run_evaluation(dataset: Dataset):
    return evaluate(
        dataset,
        metrics=[faithfulness, answer_relevancy, context_recall, context_precision],
    )


async def main():
    args = parse_args()
    config = load_config(args.config)
    config_name = config.get("name", Path(args.config).stem)
    results_path = EVAL_DIR / f"{config_name}_results.csv"

    golden_set = load_golden_set(GOLDEN_SET_PATH)
    print(f"[INFO] config: {config_name}")
    print(f"[INFO] 골든셋 로드: {len(golden_set)}쌍")
    print(
        f"[INFO] 파라미터: top_k={config.get('retrieval_top_k', 5)}, "
        f"strategy={config.get('search_strategy', 'hybrid')}, "
        f"rrf_k={config.get('rrf_k', 60)}, "
        f"temperature={config.get('temperature', 0.7)}"
    )

    # RAG 서비스 초기화
    vectorstore = VectorStoreService(settings)
    bm25_index = BM25Index()
    retrieval_service = RetrievalService(
        vectorstore_service=vectorstore,
        bm25_index=bm25_index,
    )
    if "rrf_k" in config:
        retrieval_service.RRF_K = config["rrf_k"]

    # LLM 초기화 (GMS 프록시 사용)
    from langchain_openai import ChatOpenAI

    llm = ChatOpenAI(
        base_url=settings.gms_base_url,
        api_key=settings.gms_api_key,
        model=settings.llm_model_standard,
        temperature=config.get("temperature", 0.7),
    )

    print("[INFO] RAG 파이프라인 실행 중...")
    dataset = await build_ragas_dataset(golden_set, retrieval_service, config, llm)

    print("[INFO] RAGAS 평가 실행 중...")
    result = run_evaluation(dataset)

    df = result.to_pandas()
    print(f"\n===== RAGAS 평가 결과 ({config_name}) =====")
    print(f"Faithfulness:      {df['faithfulness'].mean():.4f}  (목표 ≥ 0.95)")
    print(f"Answer Relevancy:  {df['answer_relevancy'].mean():.4f}  (목표 ≥ 0.80)")
    print(f"Context Recall:    {df['context_recall'].mean():.4f}  (목표 ≥ 0.85)")
    print(f"Context Precision: {df['context_precision'].mean():.4f}")

    print("\n===== 목표치 달성 여부 =====")
    print(
        f"Faithfulness >= 0.95:     {'PASS' if df['faithfulness'].mean() >= 0.95 else 'FAIL'}"
    )
    print(
        f"Context Recall >= 0.85:   {'PASS' if df['context_recall'].mean() >= 0.85 else 'FAIL'}"
    )
    print(
        f"Answer Relevancy >= 0.80: {'PASS' if df['answer_relevancy'].mean() >= 0.80 else 'FAIL'}"
    )

    df.to_csv(results_path, index=False, encoding="utf-8-sig")
    print(f"\n[INFO] 결과 저장: {results_path}")


if __name__ == "__main__":
    asyncio.run(main())
