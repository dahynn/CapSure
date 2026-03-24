#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
보험 상품 Excel -> PostgreSQL 적재기

핵심 특징
- 생명보험 .xls(실제 HTML 테이블) / 손해보험 실제 XLS 자동 판별
- 현재 사용 중인 insurance 스키마 구조에 맞춰 적재
- company_name / product_name 이 비면 스킵하지 않고 즉시 예외 발생
- 병합셀로 인해 비어 보이는 값은 forward-fill 복구
- 원천 row는 raw_row_jsonb 로 보존
- 동일 파일 재적재 시 해당 source_file_id 의 기존 staging row 를 먼저 삭제 후 재삽입

예시
    python insurance_loader_strict.py \
      --data-dir /path/to/files \
      --database-url postgresql://user:pass@localhost:5432/dbname

    python insurance_loader_strict.py \
      --data-dir /path/to/files \
      --database-url postgresql://user:pass@localhost:5432/dbname \
      --schema-sql /path/to/insurance_schema_strict.sql
"""

from __future__ import annotations

import argparse
import math
import os
import re
import sys
from dataclasses import dataclass
from datetime import date
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Tuple

import pandas as pd

try:
    import psycopg2
    from psycopg2.extras import Json, execute_batch
except ImportError as exc:
    raise SystemExit("psycopg2-binary 가 필요합니다. pip install psycopg2-binary") from exc


# --------------------------------------------------------------------------------------
# Exceptions
# --------------------------------------------------------------------------------------

class LoaderError(Exception):
    pass


class SchemaError(LoaderError):
    pass


class DataValidationError(LoaderError):
    pass


# --------------------------------------------------------------------------------------
# Constants
# --------------------------------------------------------------------------------------

REQUIRED_TABLES = [
    "insurance.product_source",
]

PRODUCT_COLUMNS = [
    "source_file_name",
    "source_row_no",
    "insurer_sector",
    "company_name",
    "product_name",
    "sale_channel",
    "contract_type",
    "coverage_name",
    "claim_reason",
    "payout_amount",
    "join_amount",
    "payment_cycle",
    "payment_term",
    "coverage_term",
    "minimum_join_premium",
    "premium_male",
    "premium_female",
    "fixed_rate",
    "current_announced_rate",
    "minimum_guaranteed_rate",
    "coverage_part_interest_rate",
    "reserve_part_interest_rate",
    "price_index_male",
    "price_index_female",
    "extra_premium_index_male",
    "extra_premium_index_female",
    "extra_premium_index",
    "contract_cost_index_male",
    "contract_cost_index_female",
    "contract_cost_index",
    "coverage_scope_index_name",
    "coverage_scope_index_value",
    "coverage_scope_index_cancer_diagnosis",
    "coverage_scope_index_cancer_hospitalization",
    "expected_renewal_premium",
    "product_summary",
    "product_feature",
    "surrender_value",
    "minimum_death_benefit",
    "minimum_death_benefit_method",
    "minimum_surrender_value",
    "minimum_surrender_value_method",
    "mild_dementia_covered",
    "mild_dementia_benefit_amount",
    "product_subtype",
    "renewal",
    "universal",
    "special_note",
    "contact_phone",
    "sale_date",
    "coverage_category_code",
    "coverage_code",
    "mapping_status",
    "manual_note",
    "raw_row_jsonb",
]

LIFE_FFILL_FIELDS = [
    "company_name",
    "product_name",
    "product_feature",
    "surrender_value",
    "fixed_rate",
    "current_announced_rate",
    "minimum_guaranteed_rate",
    "renewal",
    "universal",
    "sale_channel",
    "sale_date",
    "special_note",
    "contact_phone",
]

NONLIFE_FFILL_FIELDS = [
    "company_name",
    "product_name",
    "sale_channel",
    "minimum_join_premium",
    "expected_renewal_premium",
    "product_summary",
    "renewal",
    "special_note",
    "contact_phone",
]

COLUMN_MAPPING: Dict[str, str] = {
    "보험회사명": "company_name",
    "회사명": "company_name",
    "상품명": "product_name",

    "보장내용 및 보험료 > 구분": "contract_type",

    "보장내용 및 보험료 > 급부명칭": "coverage_name",
    "보장내용 및 지급기준 > 담보명": "coverage_name",
    "지급기준및 보장내역 > 담보명": "coverage_name",

    "보장내용 및 보험료 > 지급사유": "claim_reason",
    "보장내용 및 지급기준 > 지급사유": "claim_reason",
    "지급기준및 보장내역 > 지급사유": "claim_reason",

    "보장내용 및 보험료 > 지급금액": "payout_amount",
    "보장내용 및 지급기준 > 지급액": "payout_amount",
    "지급기준및 보장내역 > 지급액설명보기": "payout_amount",

    "보장내용 및 보험료 > 보험료 > 가입금액": "join_amount",
    "보장내용 및 보험료 > 보험료 > 남자": "premium_male",
    "보험료 > 남자": "premium_male",
    "보장내용 및 보험료 > 보험료 > 여자": "premium_female",
    "보험료 > 여자": "premium_female",

    "가격요소(주계약기준) > 금리부가방식 및 적용금리 > 금리확정형 > 확정이율": "fixed_rate",
    "가격요소(주계약기준) > 적용금리 > 확정이율": "fixed_rate",
    "가격요소(주계약기준) > 금리부가방식 및 적용금리 > 금리연동형/자산연계형 > 현재공시이율": "current_announced_rate",
    "가격요소(주계약기준) > 금리부가방식 및 적용금리 > 금리연동형/자산연계형 > 최저보증이율": "minimum_guaranteed_rate",

    "공시이율(%) > 보장부분적용이율(예정이율)": "coverage_part_interest_rate",
    "공시이율 > 보장부분적용이율(예정이율)설명보기": "coverage_part_interest_rate",
    "공시이율(%) > 적립부분적용이율(최저보증이율)": "reserve_part_interest_rate",
    "공시이율 > 적립부분적용이율(최저보증이율)설명보기": "reserve_part_interest_rate",

    "가격요소(주계약기준) > 보험가격지수 > 남자": "price_index_male",
    "보험가격지수 > 남자": "price_index_male",
    "보험가격지수설명보기 > 남자": "price_index_male",
    "가격요소(주계약기준) > 보험가격지수 > 여자": "price_index_female",
    "보험가격지수 > 여자": "price_index_female",
    "보험가격지수설명보기 > 여자": "price_index_female",

    "부가보험료지수 > 남자": "extra_premium_index_male",
    "부가보험료지수 > 여자": "extra_premium_index_female",
    "계약체결비용지수 > 남자": "contract_cost_index_male",
    "계약체결비용지수 > 여자": "contract_cost_index_female",

    "부가보험료지수": "extra_premium_index",
    "부가보험료지수 설명보기": "extra_premium_index",

    "계약체결비용지수": "contract_cost_index",
    "계약체결비용지수 설명보기": "contract_cost_index",

    "최저가입보험료": "minimum_join_premium",
    "예상갱신보험료": "expected_renewal_premium",
    "상품요약서": "product_summary",
    "상품특징": "product_feature",
    "상품특징 > 해약환급금": "surrender_value",
    "상품특징 > 해지환급금": "surrender_value",
    "상품특징 > 갱신주기": "renewal",
    "갱신여부": "renewal",
    "상품특징 > 유니버셜 여부": "universal",
    "상품특징 > 유니버셜": "universal",
    "상품운용 > 유니버셜": "universal",
    "상품운용 > 판매채널": "sale_channel",
    "채널": "sale_channel",
    "상품운용 > 판매일자": "sale_date",

    "상품특징 > 최저보증사항 > 최저사망보험금": "minimum_death_benefit",
    "상품특징 > 최저보증사항 > 최저사망보험 부과방식": "minimum_death_benefit_method",
    "상품특징 > 최저보증사항 > 최저해약보험금": "minimum_surrender_value",
    "상품특징 > 최저보증사항 > 최저해약보험 부과방식": "minimum_surrender_value_method",

    "상품특징 > 경도치매 보장여부": "mild_dementia_covered",
    "상품특징 > 경도치매 진단급여금": "mild_dementia_benefit_amount",

    "가격요소(주계약기준) > 보장범위지수 > 암진단": "coverage_scope_index_cancer_diagnosis",
    "가격요소(주계약기준) > 보장범위지수 > 암입원": "coverage_scope_index_cancer_hospitalization",

    "상품특징 > 유형": "product_subtype",

    # 손해보험 표준 컬럼 (누락분 추가)
    "특이사항": "special_note",
    "대표번호": "contact_phone",
    # 생명보험 경로 (상품운용 그룹)
    "상품운용 > 특이사항": "special_note",
    "상품운용 > 대표번호": "contact_phone",

    # '선택' 컬럼(체크박스)은 매핑하지 않음 → 무시
}


# --------------------------------------------------------------------------------------
# Data structures
# --------------------------------------------------------------------------------------

@dataclass
class ParsedFile:
    sector: str
    parse_format: str
    df: pd.DataFrame
    header_row_from: int
    header_row_to: int
    data_row_from: int


# --------------------------------------------------------------------------------------
# Logging / formatting helpers
# --------------------------------------------------------------------------------------

def log(message: str) -> None:
    print(message, flush=True)


def normalize_space(text: str) -> str:
    text = text.replace("\xa0", " ")
    text = text.replace("\r", " ").replace("\n", " ")
    text = re.sub(r"\s*>\s*", " > ", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def is_blank(value: object) -> bool:
    if value is None:
        return True
    if isinstance(value, float) and math.isnan(value):
        return True
    try:
        if pd.isna(value):
            return True
    except Exception:
        pass
    if isinstance(value, str) and normalize_space(value) == "":
        return True
    return False


def clean_scalar(value: object) -> Optional[str]:
    if is_blank(value):
        return None
    if isinstance(value, str):
        text = value.strip()
        if text in {"-", "–", "—", "."}:
            return None
    if isinstance(value, pd.Timestamp):
        return value.strftime("%Y-%m-%d")
    text = normalize_space(str(value))
    if text.lower() in {"none", "null", "nan"}:
        return None
    return text


def to_date_or_none(value: Optional[str]) -> Optional[date]:
    if not value:
        return None
    text = re.sub(r"[./]", "-", value.strip())
    parsed = pd.to_datetime(text, errors="coerce")
    if pd.isna(parsed):
        return None
    return parsed.date()


def flatten_column(col: object) -> str:
    if isinstance(col, tuple):
        parts: List[str] = []
        for item in col:
            if item is None:
                continue
            text = str(item).strip()
            if not text or text.lower() == "nan" or text.startswith("Unnamed:"):
                continue
            if not parts or parts[-1] != text:
                parts.append(text)
        return normalize_space(" > ".join(parts))
    return normalize_space(str(col))


def dedupe_headers(headers: Iterable[str]) -> List[str]:
    counts: Dict[str, int] = {}
    result: List[str] = []
    for header in headers:
        key = normalize_space(header) if header else ""
        key = key or "EMPTY"
        counts[key] = counts.get(key, 0) + 1
        if counts[key] == 1:
            result.append(key)
        else:
            result.append(f"{key}__{counts[key]}")
    return result


def undecorate_header(header: str) -> str:
    return re.sub(r"__\d+$", "", header)


# --------------------------------------------------------------------------------------
# Readers
# --------------------------------------------------------------------------------------

def detect_parse_mode(file_path: Path) -> Tuple[str, str]:
    name = file_path.name

    try:
        head = file_path.read_bytes()[:4096].decode("latin1", errors="ignore").lower()
    except Exception:
        head = ""

    is_html = ("<html" in head) or ("<table" in head)
    if name.startswith("생명") or is_html:
        return "LIFE", "HTML_TABLE"
    return "NONLIFE", "XLS"


def read_life_html(file_path: Path) -> ParsedFile:
    df = pd.read_html(str(file_path))[0].copy()
    headers = dedupe_headers([flatten_column(col) for col in df.columns])
    df.columns = headers
    df = df.dropna(how="all").reset_index(drop=True)

    return ParsedFile(
        sector="LIFE",
        parse_format="HTML_TABLE",
        df=df,
        header_row_from=0,
        header_row_to=max(getattr(df.columns, "nlevels", 1) - 1, 0),
        data_row_from=1,
    )


def read_nonlife_xls(file_path: Path) -> ParsedFile:
    raw = pd.read_excel(str(file_path), engine="xlrd", header=None)

    # 머리글 병합(Merge) 처리를 위해 상위 헤더를 ffill(Forward Fill) 해서 빈칸 채우기
    top = pd.Series(raw.iloc[5].tolist()).ffill().tolist()
    bottom = raw.iloc[6].tolist()

    headers: List[str] = []
    for a, b in zip(top, bottom):
        a_clean = clean_scalar(a)
        b_clean = clean_scalar(b)

        if a_clean and b_clean:
            headers.append(normalize_space(f"{a_clean} > {b_clean}"))
        elif a_clean:
            headers.append(a_clean)
        elif b_clean:
            headers.append(b_clean)
        else:
            headers.append("EMPTY")

    headers = dedupe_headers(headers)

    df = raw.iloc[7:].copy()
    df.columns = headers
    df = df.dropna(how="all").reset_index(drop=True)

    return ParsedFile(
        sector="NONLIFE",
        parse_format="XLS",
        df=df,
        header_row_from=5,
        header_row_to=6,
        data_row_from=7,
    )


def parse_file(file_path: Path) -> ParsedFile:
    sector, parse_format = detect_parse_mode(file_path)
    if sector == "LIFE":
        return read_life_html(file_path)
    if parse_format != "XLS":
        raise LoaderError(f"손해보험 파일인데 XLS 로 판별되지 않았습니다: {file_path.name}")
    return read_nonlife_xls(file_path)


# --------------------------------------------------------------------------------------
# Mapping / normalization
# --------------------------------------------------------------------------------------

def map_header(file_name: str, raw_header: str) -> Optional[str]:
    base = undecorate_header(raw_header)

    if "손해-장기보장성-암보험" in file_name and base == "보장범위지수":
        if raw_header.endswith("__2"):
            return "coverage_scope_index_value"
        return "coverage_scope_index_name"

    return COLUMN_MAPPING.get(base)


def map_coverage(coverage_name: Optional[str], file_name: str, product_name: Optional[str]) -> Tuple[Optional[str], Optional[str], str]:
    name = str(coverage_name).replace(" ", "") if coverage_name else ""
    fn = str(file_name).replace(" ", "")
    pn = str(product_name).replace(" ", "") if product_name else ""
    
    # 1. 담보명(coverage_name) 우선매핑
    if name:
        if "수술" in name:
            category = "SURGERY"
            code = "SURGERY_GENERAL"
        elif any(k in name for k in ["실손", "의료비", "입원비", "통원비", "처방조제"]):
            category = "ACTUAL_LOSS"
            code = "ACTUAL_LOSS_GENERAL"
        elif "암" in name:
            category = "CANCER"
            if "진단" in name:
                code = "CANCER_DIAGNOSIS"
            elif "입원" in name or "요양" in name:
                code = "CANCER_CLINIC"
            elif "수술" in name:
                code = "CANCER_SURGERY"
            elif "치료" in name or "항암" in name:
                code = "CANCER_TREATMENT"
            elif "사망" in name:
                category = "DEATH"
                code = "DEATH_CANCER"
            else:
                code = "CANCER_GENERAL"
        elif any(k in name for k in ["뇌", "심장", "심근", "허혈성", "협심증"]):
            category = "BRAIN_HEART"
            if "진단" in name:
                code = "BRAIN_HEART_DIAGNOSIS"
            else:
                code = "BRAIN_HEART_GENERAL"
        elif "사망" in name or "유족" in name:
            category = "DEATH"
            code = "DEATH_GENERAL"
            if "상해" in name or "재해" in name:
                code = "DEATH_ACCIDENT"
            elif "질병" in name:
                code = "DEATH_DISEASE"
        elif any(k in name for k in ["배상", "책임", "벌금", "변호사", "교통사고처리"]):
            category = "LIABILITY"
            code = "LIABILITY_GENERAL"
        elif any(k in name for k in ["상해", "재해", "골절", "화상", "장해", "후유"]):
            category = "ACCIDENT"
            if "골절" in name:
                code = "ACCIDENT_FRACTURE"
            elif "장해" in name or "후유" in name:
                code = "ACCIDENT_DISABILITY"
            else:
                code = "ACCIDENT_GENERAL"
        else:
            # 담보명에선 못찾음 -> 파일/상품명 강제 폴백을 위해 통과
            pass
        
        # 정상적으로 위에서 category 할당되었으면 반환
        if 'category' in locals():
            return category, code, "AUTO_MAPPED"
            
    # 2. 파일명/상품명 기반 Fallback 매핑 (담보명이 비어있거나 위에서 실패했을 경우)
    if "암" in fn or "암" in pn:
        return "CANCER", "CANCER_GENERAL", "AUTO_MAPPED"
    elif any(k in fn for k in ["뇌", "심", "성인병", "치매", "간병"]) or any(k in pn for k in ["뇌", "심", "성인병", "치매", "간병"]):
        if "치매" in fn or "치매" in pn or "간병" in fn or "간병" in pn:
            return "BRAIN_HEART", "DEMENTIA_GENERAL", "AUTO_MAPPED"
        return "BRAIN_HEART", "BRAIN_HEART_GENERAL", "AUTO_MAPPED"
    elif any(k in fn for k in ["종신", "정기"]) or "종신" in pn or "정기" in pn:
        return "DEATH", "DEATH_GENERAL", "AUTO_MAPPED"
    elif "실손" in fn or "실손" in pn:
        return "ACTUAL_LOSS", "ACTUAL_LOSS_GENERAL", "AUTO_MAPPED"
    elif "종합" in fn or "상해" in fn or "화재" in fn or "상해" in pn:
        return "ACCIDENT", "ACCIDENT_GENERAL", "AUTO_MAPPED"
        
    return None, None, "UNMAPPED"


# --------------------------------------------------------------------------------------
# Payment / Coverage term extraction
# --------------------------------------------------------------------------------------

def extract_payment_info(special_note: Optional[str], raw_row: Optional[Dict]) -> Dict[str, Optional[str]]:
    """
    special_note 와 raw_row_jsonb 의 모든 텍스트를 합쳐
    payment_cycle / payment_term / coverage_term 을 정규식으로 추출합니다.

    출력 예)
      payment_cycle : '월납'  |  '연납'  |  '일시납'  |  None
      payment_term  : '20년납' | '전기납' | '종신납'   |  None
      coverage_term : '20년만기' | '100세만기' | '종신' |  None
    """
    # 분석할 텍스트 수집
    parts: List[str] = []
    if special_note:
        parts.append(special_note)
    if raw_row:
        for v in raw_row.values():
            if v and isinstance(v, str):
                parts.append(v)
    text = " ".join(parts)

    result: Dict[str, Optional[str]] = {
        "payment_cycle": None,
        "payment_term": None,
        "coverage_term": None,
    }

    # 1. payment_cycle: 월납 / 연납 / 일시납
    cycle_map = [("일시납", "일시납"), ("월납", "월납"), ("연납", "연납")]
    for keyword, label in cycle_map:
        if keyword in text:
            result["payment_cycle"] = label
            break  # 일시납 우선 체크 후 월납 순

    # 2. payment_term: N년납 / 전기납 / 종신납
    term_match = re.search(r"(\d+)년납", text)
    if term_match:
        result["payment_term"] = f"{term_match.group(1)}년납"
    elif "전기납" in text:
        result["payment_term"] = "전기납"
    elif "종신납" in text:
        result["payment_term"] = "종신납"

    # 3. coverage_term: N년만기 / N세만기 / 종신
    cterm_year = re.search(r"(\d+)년\s*만기", text)
    cterm_age  = re.search(r"(\d+)세\s*만기", text)
    if cterm_year:
        result["coverage_term"] = f"{cterm_year.group(1)}년만기"
    elif cterm_age:
        result["coverage_term"] = f"{cterm_age.group(1)}세만기"
    elif "종신" in text:
        result["coverage_term"] = "종신"

    return result


def base_row_template(sector: str) -> Dict[str, Optional[object]]:
    return {col: None for col in PRODUCT_COLUMNS}


def update_previous(previous: Dict[str, Optional[object]], out: Dict[str, Optional[object]], sector: str) -> None:
    fields = LIFE_FFILL_FIELDS if sector == "LIFE" else NONLIFE_FFILL_FIELDS
    for field in fields:
        if out.get(field):
            previous[field] = out[field]


def apply_forward_fill(previous: Dict[str, Optional[object]], out: Dict[str, Optional[object]], sector: str) -> None:
    fields = LIFE_FFILL_FIELDS if sector == "LIFE" else NONLIFE_FFILL_FIELDS
    for field in fields:
        if not out.get(field):
            out[field] = previous.get(field)


def has_payload(out: Dict[str, Optional[object]], sector: str) -> bool:
    if sector == "LIFE":
        fields = [
            "coverage_name",
            "claim_reason",
            "payout_amount",
            "join_amount",
            "premium_male",
            "premium_female",
            "product_feature",
            "fixed_rate",
            "current_announced_rate",
            "minimum_guaranteed_rate",
            "renewal",
        ]
    else:
        fields = [
            "coverage_name",
            "claim_reason",
            "payout_amount",
            "premium_male",
            "premium_female",
            "minimum_join_premium",
            "expected_renewal_premium",
            "product_summary",
            "renewal",
        ]
    return any(out.get(field) is not None for field in fields)


def validate_required_identifiers(
    file_name: str,
    source_row_no: int,
    out: Dict[str, Optional[object]],
    raw_record: Dict[str, Optional[str]],
) -> None:
    company_name = clean_scalar(out.get("company_name"))
    product_name = clean_scalar(out.get("product_name"))

    invalid = {"", "none", "null", "nan"}

    if not company_name or company_name.lower() in invalid:
        raise DataValidationError(
            f"[{file_name}] source_row_no={source_row_no} 에서 company_name 이 비어 있습니다. raw={raw_record}"
        )

    if not product_name or product_name.lower() in invalid:
        raise DataValidationError(
            f"[{file_name}] source_row_no={source_row_no} 에서 product_name 이 비어 있습니다. raw={raw_record}"
        )


def normalize_dataframe(df: pd.DataFrame, file_name: str, sector: str) -> List[Dict[str, Optional[object]]]:
    rows: List[Dict[str, Optional[object]]] = []
    previous: Dict[str, Optional[object]] = {}

    for idx, raw_record in enumerate(df.to_dict(orient="records"), start=1):
        clean_record: Dict[str, Optional[str]] = {
            str(key): clean_scalar(value) for key, value in raw_record.items()
        }

        out = base_row_template(sector)
        out["source_row_no"] = idx
        out["raw_row_jsonb"] = clean_record

        for raw_header, value in clean_record.items():
            if value is None:
                continue

            canonical = map_header(file_name, raw_header)
            if not canonical:
                continue

            if out.get(canonical) is None:
                out[canonical] = value

        if out.get("sale_date"):
            out["sale_date"] = to_date_or_none(str(out["sale_date"]))

        apply_forward_fill(previous, out, sector)

        if not has_payload(out, sector):
            # 완전히 의미 있는 payload 가 없는 행은 적재 대상에서 제외
            update_previous(previous, out, sector)
            continue

        cov_name_str = str(out.get("coverage_name")) if out.get("coverage_name") else None
        prod_name_str = str(out.get("product_name")) if out.get("product_name") else None
        cat, code, status = map_coverage(cov_name_str, file_name, prod_name_str)
        out["coverage_category_code"] = cat
        out["coverage_code"] = code
        out["mapping_status"] = status

        # 납입주기 / 납입기간 / 보험기간 추출
        _sn = out.get("special_note")
        _rr = out.get("raw_row_jsonb")
        payment_info = extract_payment_info(
            special_note=str(_sn) if _sn is not None else None,
            raw_row=dict(_rr) if _rr is not None else None,
        )
        out.update(payment_info)

        validate_required_identifiers(
            file_name=file_name,
            source_row_no=idx,
            out=out,
            raw_record=clean_record,
        )

        update_previous(previous, out, sector)
        rows.append(out)

    if not rows:
        raise DataValidationError(f"[{file_name}] 정규화 결과 적재할 row 가 0건입니다.")

    return rows


# --------------------------------------------------------------------------------------
# PostgreSQL
# --------------------------------------------------------------------------------------

def get_conn(database_url: str):
    return psycopg2.connect(database_url)


def run_schema_sql_if_needed(conn, schema_sql_path: Optional[Path]) -> None:
    if not schema_sql_path:
        return
    sql = schema_sql_path.read(encoding="utf-8")
    with conn.cursor() as cur:
        cur.execute(sql)
    conn.commit()


def verify_required_tables(conn) -> None:
    sql = """
    select table_schema || '.' || table_name as fq_name
    from information_schema.tables
    where table_schema = 'insurance'
      and table_name in ('product_source')
    order by fq_name;
    """
    with conn.cursor() as cur:
        cur.execute(sql)
        found = {row[0] for row in cur.fetchall()}

    missing = [table for table in REQUIRED_TABLES if table not in found]
    if missing:
        raise SchemaError(
            "insurance 스키마가 준비되지 않았습니다. 누락 테이블: "
            + ", ".join(missing)
            + " | 먼저 SQL 을 실행하거나 --schema-sql 옵션을 사용하세요."
        )




def delete_existing_rows(conn, file_name: str) -> None:
    with conn.cursor() as cur:
        cur.execute(
            "delete from insurance.product_source where source_file_name = %s;",
            (file_name,),
        )


def insert_rows(conn, table_name: str, columns: List[str], rows: List[Dict[str, Optional[object]]]) -> None:
    if not rows:
        return

    placeholders = ", ".join(["%s"] * len(columns))
    sql = f"""
    insert into {table_name} ({", ".join(columns)})
    values ({placeholders})
    """

    payload = []
    for row in rows:
        values = []
        for col in columns:
            value = row.get(col)
            if col == "raw_row_jsonb":
                values.append(Json(value or {}))
            else:
                values.append(value)
        payload.append(tuple(values))

    with conn.cursor() as cur:
        execute_batch(cur, sql, payload, page_size=500)


# --------------------------------------------------------------------------------------
# Pipeline
# --------------------------------------------------------------------------------------

def load_single_file(conn, file_path: Path) -> Tuple[str, int]:
    import unicodedata
    file_name = unicodedata.normalize('NFC', file_path.name)
    parsed = parse_file(file_path)
    normalized_rows = normalize_dataframe(parsed.df, file_name, parsed.sector)

    # 기존 데이터 삭제 (파일명 기준)
    delete_existing_rows(conn, file_name)

    # 데이터마다 파일명 및 업권 추가
    for row in normalized_rows:
        row["source_file_name"] = file_name
        row["insurer_sector"] = parsed.sector

    insert_rows(conn, "insurance.product_source", PRODUCT_COLUMNS, normalized_rows)

    return parsed.sector, len(normalized_rows)


def discover_files(data_dir: Path, patterns: List[str]) -> List[Path]:
    files: List[Path] = []
    for pattern in patterns:
        files.extend(sorted(data_dir.rglob(pattern)))
    unique_files = sorted({p.resolve() for p in files if p.is_file()})
    return [Path(p) for p in unique_files]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="보험 Excel -> PostgreSQL 적재기")
    parser.add_argument("--data-dir", type=Path, required=True, help="엑셀 파일들이 있는 폴더")
    parser.add_argument(
        "--patterns",
        nargs="+",
        default=["*.xls", "*.xlsx"],
        help="검색할 파일 패턴 (기본: *.xls *.xlsx)",
    )
    parser.add_argument(
        "--database-url",
        default=os.getenv("DATABASE_URL"),
        help="PostgreSQL 접속 문자열. 미지정 시 DATABASE_URL 환경변수 사용",
    )
    parser.add_argument(
        "--schema-sql",
        type=Path,
        default=None,
        help="실행할 스키마 SQL 파일 경로. 지정하면 적재 전에 먼저 실행",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    if not args.database_url:
        print("DATABASE_URL 또는 --database-url 이 필요합니다.", file=sys.stderr)
        return 1

    if not args.data_dir.exists():
        print(f"폴더가 없습니다: {args.data_dir}", file=sys.stderr)
        return 1

    files = discover_files(args.data_dir, args.patterns)
    if not files:
        print(f"적재할 파일이 없습니다: {args.data_dir}", file=sys.stderr)
        return 1

    conn = get_conn(args.database_url)

    life_total = 0
    nonlife_total = 0

    try:
        if args.schema_sql:
            log(f"스키마 SQL 실행: {args.schema_sql}")
            run_schema_sql_if_needed(conn, args.schema_sql)

        verify_required_tables(conn)

        for file_path in files:
            try:
                sector, inserted_count = load_single_file(conn, file_path)
                conn.commit()

                if sector == "LIFE":
                    life_total += inserted_count
                else:
                    nonlife_total += inserted_count

                log(f"[OK] {file_path.name} | sector={sector} | rows={inserted_count}")
            except Exception:
                conn.rollback()
                raise

        log("-" * 80)
        log(f"생명보험 적재 rows: {life_total}")
        log(f"손해보험 적재 rows: {nonlife_total}")
        log(f"총 적재 rows: {life_total + nonlife_total}")
        return 0

    finally:
        conn.close()


if __name__ == "__main__":
    raise SystemExit(main())
