from app.services.retrieval_service import SearchResult


def format_search_results(results: list[SearchResult]) -> str:
    """검색 결과를 LLM 프롬프트용 텍스트로 포맷팅한다.

    포맷 예시:
    [출처: 소득세법 (법률)]
    제19조(사업소득) ① ...

    [출처: 소득세법 시행령 (시행령)]
    제40조(필요경비의 계산) ...
    """
    if not results:
        return ""

    parts = []
    for result in results:
        law_name = result.metadata.get("law_name", "알 수 없음")
        law_type = result.metadata.get("law_type", "알 수 없음")
        header = f"[출처: {law_name} ({law_type})]"
        parts.append(f"{header}\n{result.content}")

    return "\n\n".join(parts)


def truncate_text(text: str, max_length: int = 3000) -> str:
    """텍스트를 최대 길이로 자른다.

    LLM 컨텍스트 윈도우 초과 방지용.
    문장 단위로 자르되, max_length를 초과하지 않도록 한다.
    """
    if len(text) <= max_length:
        return text

    truncated = text[:max_length]
    # 마지막 문장 종결 부호 위치에서 자름
    for sep in (".", "。", "!", "?", "\n"):
        pos = truncated.rfind(sep)
        if pos != -1:
            return truncated[: pos + 1]

    return truncated
