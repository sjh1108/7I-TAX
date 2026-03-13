"""세법 PDF 문서를 ChromaDB에 인덱싱하는 스크립트.

사용법:
    python -m app.scripts.index_documents
    python -m app.scripts.index_documents --force  # 기존 컬렉션 삭제 후 재인덱싱
"""

import argparse
import logging
import time
from collections import Counter

from app.core.config import settings
from app.services.document_processor import DocumentProcessor
from app.services.vectorstore import VectorStoreService

logger = logging.getLogger(__name__)


def main() -> None:
    """세법 PDF 문서를 ChromaDB에 인덱싱한다.

    처리 순서:
    1. argparse로 --force 옵션 파싱
    2. --force면 기존 컬렉션 삭제
    3. DocumentProcessor.process_all()로 전체 청크 생성
    4. VectorStoreService.add_documents()로 저장
    5. 결과 통계 출력:
       - 총 PDF 수 (law_name unique 개수)
       - 총 청크 수
       - 법률별 청크 수 (law_name 기준 그룹핑)
       - 소요 시간
    """
    parser = argparse.ArgumentParser(
        description="세법 PDF 문서를 ChromaDB에 인덱싱합니다."
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="기존 컬렉션을 삭제하고 재인덱싱합니다.",
    )
    args = parser.parse_args()

    vectorstore_service = VectorStoreService(settings)

    if args.force:
        logger.info("--force 옵션 감지: 기존 컬렉션을 삭제합니다.")
        vectorstore_service.delete_collection()
        logger.info("컬렉션 삭제 완료.")

    logger.info("문서 처리를 시작합니다.")
    start_time = time.time()

    processor = DocumentProcessor()
    chunks = processor.process_all()

    if not chunks:
        logger.warning("처리된 청크가 없습니다. 인덱싱을 중단합니다.")
        return

    texts = [chunk.content for chunk in chunks]
    metadatas = [chunk.metadata for chunk in chunks]
    ids = [chunk.metadata["chunk_id"] for chunk in chunks]

    logger.info("벡터 저장소에 %d개 청크를 저장합니다.", len(chunks))
    vectorstore_service.add_documents(texts=texts, metadatas=metadatas, ids=ids)

    elapsed = time.time() - start_time

    law_name_counter: Counter[str] = Counter(
        chunk.metadata.get("law_name", "unknown") for chunk in chunks
    )
    total_pdfs = len(law_name_counter)
    total_chunks = len(chunks)

    logger.info("=" * 50)
    logger.info("인덱싱 완료")
    logger.info("  총 PDF 수      : %d", total_pdfs)
    logger.info("  총 청크 수     : %d", total_chunks)
    logger.info("  법률별 청크 수 :")
    for law_name, count in sorted(law_name_counter.items()):
        logger.info("    %-30s : %d", law_name, count)
    logger.info("  소요 시간      : %.2f초", elapsed)
    logger.info("=" * 50)


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    main()
