import logging
from dataclasses import dataclass
from pathlib import Path

from langchain_text_splitters import RecursiveCharacterTextSplitter
from pypdf import PdfReader

logger = logging.getLogger(__name__)


KOREAN_LAW_SEPARATORS: list[str] = [
    r"\n제\d+편",  # 편
    r"\n제\d+장",  # 장
    r"\n제\d+절",  # 절
    r"\n제\d+조",  # 조
    "\n\n",  # 빈 줄
    "\n",  # 줄바꿈
    ".",  # 문장
]

DEFAULT_CHUNK_SIZE: int = 1000
DEFAULT_CHUNK_OVERLAP: int = 200


@dataclass
class RawDocument:
    """PDF에서 추출한 원본 텍스트 + 메타데이터."""

    content: str
    metadata: dict
    page_count: int
    source_path: str


@dataclass
class TextChunk:
    """청크 분할된 텍스트 + 메타데이터."""

    content: str
    metadata: dict


PDF_METADATA_MAP: dict[str, dict[str, str]] = {
    "소득세법(법률)": {
        "law_name": "소득세법",
        "law_type": "법률",
        "tax_type": "소득세",
    },
    "소득세법 시행령": {
        "law_name": "소득세법 시행령",
        "law_type": "시행령",
        "tax_type": "소득세",
    },
    "소득세법 시행규칙": {
        "law_name": "소득세법 시행규칙",
        "law_type": "시행규칙",
        "tax_type": "소득세",
    },
    "부가가치세법(법률)": {
        "law_name": "부가가치세법",
        "law_type": "법률",
        "tax_type": "부가가치세",
    },
    "부가가치세법 시행령": {
        "law_name": "부가가치세법 시행령",
        "law_type": "시행령",
        "tax_type": "부가가치세",
    },
    "세무사법(법률)": {
        "law_name": "세무사법",
        "law_type": "법률",
        "tax_type": "세무사",
    },
    "조세특례제한법(법률)": {
        "law_name": "조세특례제한법",
        "law_type": "법률",
        "tax_type": "조세특례",
    },
    "조세특례제한법 시행령": {
        "law_name": "조세특례제한법 시행령",
        "law_type": "시행령",
        "tax_type": "조세특례",
    },
    "기준(단순)경비율": {
        "law_name": "기준경비율표",
        "law_type": "테이블",
        "tax_type": "소득세",
    },
}


class DocumentProcessor:
    """PDF 문서에서 텍스트를 추출하는 프로세서."""

    def __init__(
        self,
        resources_dir: str = "ai/resources",
        chunk_size: int = DEFAULT_CHUNK_SIZE,
        chunk_overlap: int = DEFAULT_CHUNK_OVERLAP,
    ) -> None:
        self.resources_dir = Path(resources_dir)
        self.splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            separators=KOREAN_LAW_SEPARATORS,
            is_separator_regex=True,
        )

    def extract_text_from_pdf(self, pdf_path: Path) -> RawDocument:
        """단일 PDF에서 텍스트를 추출한다.

        Args:
            pdf_path: PDF 파일 경로.

        Returns:
            추출된 텍스트와 메타데이터를 담은 RawDocument.

        Raises:
            FileNotFoundError: PDF 파일이 존재하지 않을 때.
            Exception: PDF 읽기에 실패했을 때.
        """
        reader = PdfReader(pdf_path)
        pages: list[str] = []

        for page in reader.pages:
            text = page.extract_text()
            if text:
                pages.append(text)

        content = "\n".join(pages)
        metadata = self._resolve_metadata(pdf_path.stem)

        return RawDocument(
            content=content,
            metadata=metadata,
            page_count=len(reader.pages),
            source_path=str(pdf_path),
        )

    def extract_all(self) -> list[RawDocument]:
        """resources 디렉토리의 모든 PDF를 추출한다.

        Returns:
            추출된 RawDocument 리스트. 실패한 파일은 건너뛴다.
        """
        pdf_files = sorted(self.resources_dir.glob("*.pdf"))

        if not pdf_files:
            logger.warning("PDF 파일을 찾을 수 없습니다: %s", self.resources_dir)
            return []

        documents: list[RawDocument] = []

        for pdf_path in pdf_files:
            try:
                doc = self.extract_text_from_pdf(pdf_path)
                logger.info(
                    "PDF 추출 완료: %s (%d페이지)", pdf_path.name, doc.page_count
                )
                documents.append(doc)
            except Exception:
                logger.exception("PDF 추출 실패: %s", pdf_path.name)

        logger.info("총 %d/%d개 PDF 추출 완료", len(documents), len(pdf_files))
        return documents

    def _resolve_metadata(self, filename: str) -> dict[str, str]:
        """파일명으로 PDF_METADATA_MAP에서 메타데이터를 조회한다.

        파일명이 매핑 키로 시작하는지 확인하여 매칭한다.
        예: '소득세법(법률)(제21065호)(20260102)' -> '소득세법(법률)' 키로 매칭.

        Args:
            filename: 확장자를 제외한 PDF 파일명.

        Returns:
            법률명, 법 유형, 세목이 담긴 메타데이터 딕셔너리.
        """
        # 긴 키부터 매칭하여 '소득세법 시행령'이 '소득세법'보다 먼저 매칭되도록 함
        for key in sorted(PDF_METADATA_MAP, key=len, reverse=True):
            if filename.startswith(key):
                return dict(PDF_METADATA_MAP[key])

        logger.warning("메타데이터 매핑 실패, 기본값 사용: %s", filename)
        return {
            "law_name": filename,
            "law_type": "unknown",
            "tax_type": "unknown",
        }

    def chunk_document(self, raw_doc: RawDocument) -> list[TextChunk]:
        """단일 RawDocument를 청크로 분할한다.

        1. RecursiveCharacterTextSplitter로 content를 분할
        2. 각 청크에 부모 문서의 메타데이터 복사
        3. chunk_id 부여: "{law_name}_{순번}" (예: "소득세법_001")
        4. TextChunk 리스트 반환

        Args:
            raw_doc: PDF에서 추출한 원본 문서.

        Returns:
            청크 분할된 TextChunk 리스트.
        """
        texts = self.splitter.split_text(raw_doc.content)
        law_name = raw_doc.metadata.get("law_name", "unknown")
        chunks: list[TextChunk] = []

        for idx, text in enumerate(texts, start=1):
            chunk_metadata = dict(raw_doc.metadata)
            chunk_metadata["chunk_id"] = f"{law_name}_{idx:03d}"
            chunk_metadata["source_path"] = raw_doc.source_path
            chunks.append(TextChunk(content=text, metadata=chunk_metadata))

        logger.info(
            "청크 분할 완료: %s -> %d개 청크", law_name, len(chunks)
        )
        return chunks

    def process_all(self) -> list[TextChunk]:
        """전체 파이프라인: PDF 추출 -> 청킹 -> TextChunk 리스트 반환.

        1. extract_all()로 모든 PDF 추출
        2. 각 RawDocument에 대해 chunk_document() 호출
        3. 모든 청크를 하나의 리스트로 결합
        4. 총 청크 수를 로그로 출력

        Returns:
            모든 문서의 TextChunk를 합친 리스트.
        """
        raw_docs = self.extract_all()
        all_chunks: list[TextChunk] = []

        for raw_doc in raw_docs:
            chunks = self.chunk_document(raw_doc)
            all_chunks.extend(chunks)

        logger.info("전체 청크 수: %d", len(all_chunks))
        return all_chunks
