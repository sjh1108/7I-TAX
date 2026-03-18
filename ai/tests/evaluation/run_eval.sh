#!/usr/bin/env bash
# RAG 평가 파이프라인 실행 스크립트
# 사용법:
#   bash ai/tests/evaluation/run_eval.sh          # plain + optimized 모두 실행 후 비교
#   bash ai/tests/evaluation/run_eval.sh plain     # plain만 실행
#   bash ai/tests/evaluation/run_eval.sh optimized # optimized만 실행
#   bash ai/tests/evaluation/run_eval.sh both      # 명시적으로 both

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AI_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MODE="${1:-both}"

echo "====================================="
echo " tax7i RAG 평가 파이프라인 (RAGAS)"
echo " 모드: ${MODE}"
echo "====================================="

# 골든셋 존재 확인
GOLDEN_SET="${SCRIPT_DIR}/golden_set.json"
if [ ! -f "${GOLDEN_SET}" ]; then
    echo "[ERROR] golden_set.json 이 없습니다: ${GOLDEN_SET}"
    exit 1
fi
QA_COUNT=$(python3 -c "import json; print(len(json.load(open('${GOLDEN_SET}'))))")
echo "[INFO] 골든셋 QA 수: ${QA_COUNT}쌍"

# RAGAS 설치 확인
python3 -c "import ragas" 2>/dev/null || {
    echo "[INFO] ragas 패키지 설치 중..."
    pip install ragas datasets pyyaml pandas -q
}

# --- 평가 실행 함수 ---
run_eval() {
    local config_file=$1
    local label=$2
    echo ""
    echo "[INFO] ${label} 평가 시작 (config: ${config_file})..."
    cd "${AI_ROOT}"
    python3 tests/evaluation/test_ragas_eval.py --config "${config_file}"
}

# --- 비교 출력 함수 ---
compare_results() {
    echo ""
    echo "========================================="
    echo " Plain RAG vs Optimized RAG 비교 결과"
    echo "========================================="
    cd "${AI_ROOT}"
    python3 - <<'PYEOF'
import sys
from pathlib import Path
import pandas as pd

eval_dir = Path("tests/evaluation")
plain_path = eval_dir / "plain_results.csv"
opt_path   = eval_dir / "optimized_results.csv"

if not plain_path.exists() or not opt_path.exists():
    print("[WARN] 비교 대상 CSV 파일이 없습니다. plain + optimized를 모두 실행하세요.")
    sys.exit(0)

plain     = pd.read_csv(plain_path)
optimized = pd.read_csv(opt_path)

metrics = ["faithfulness", "answer_relevancy", "context_recall", "context_precision"]
targets = {"faithfulness": 0.95, "context_recall": 0.85, "answer_relevancy": 0.80}

print(f"{'지표':<25} {'Plain':>8} {'Optimized':>10} {'변화':>9}  {'목표 달성'}")
print("-" * 65)
for m in metrics:
    if m not in plain.columns or m not in optimized.columns:
        continue
    p_val = plain[m].mean()
    o_val = optimized[m].mean()
    diff  = o_val - p_val
    pct   = (diff / p_val * 100) if p_val > 0 else 0
    target = targets.get(m)
    status = ""
    if target:
        plain_ok = "PASS" if p_val >= target else "FAIL"
        opt_ok   = "PASS" if o_val >= target else "FAIL"
        status = f"Plain:{plain_ok} → Opt:{opt_ok}"
    print(f"{m:<25} {p_val:>8.4f} {o_val:>10.4f} {pct:>+8.1f}%  {status}")
PYEOF
}

# --- 모드별 실행 ---
if [[ "$MODE" == "plain" || "$MODE" == "both" ]]; then
    run_eval "plain_config.yaml" "Plain RAG"
fi

if [[ "$MODE" == "optimized" || "$MODE" == "both" ]]; then
    run_eval "optimized_config.yaml" "Optimized RAG"
fi

if [[ "$MODE" == "both" ]]; then
    compare_results
fi

echo ""
echo "[INFO] 완료."
