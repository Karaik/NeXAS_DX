#!/usr/bin/env python3
"""生成仅关注战斗行为的 BHE/Jinki 与 Mod 技能差分报告。"""

from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable


TOOL_VERSION = "1.2.0"
ABSENT = "<不存在>"

# 这是生产代码使用的 BHE -> BSDX 顶层 WAZ 槽位映射。
# 映射为 -1 的条目无法在 BSDX 中表示，报告会将其列为风险数据。
BHE_TO_BSDX_SLOT = {
    0: 0, 1: 1, 2: 2, 3: 3, 4: 4, 5: 5, 6: 6, 7: 7, 8: 8, 9: 9,
    10: 10, 11: 11, 12: 12, 13: 13, 14: 14, 15: 15, 16: 16, 17: 17,
    18: 18, 19: 19, 20: 20, 21: 21, 22: 22, 23: -1, 24: 23,
    25: 24, 26: 25, 27: 26, 28: 27, 29: 28, 30: 29, 31: 30,
    32: 31, 33: 32, 34: 33, 35: -1, 36: 34, 37: 35, 38: -1,
    39: 36, 40: -1, 41: 37, 42: 38, 43: -1, 44: 39, 45: 40,
    46: 41, 47: 42, 48: 43, 49: 44, 50: 45, 51: 46, 52: -1,
    53: 47, 54: 48, 55: 49, 56: 50, 57: 51, 58: 52, 59: 53,
    60: 54, 61: 55, 62: -1, 63: 56, 64: 57, 65: 58, 66: -1,
    67: -1, 68: -1, 69: -1, 70: 59, 71: 60, 72: 61, 73: 62,
    74: 63, 75: 64, 76: 65, 77: 66, 78: 67, 79: 68, 80: 69,
    81: 70, 82: 71,
}

TARGET_SLOT_NAMES = {
    41: "CEventHit",
    42: "CEventHit",
    47: "CEventCancel",
    48: "CEventEffect",
    59: "CEventScreenScale",
    60: "CEventScreenYure",
    65: "CEventBlur",
    69: "CEventSlow",
    70: "CEventSlowRate",
    71: "CEventChange",
}

HIGH_VALUE_SLOTS = set(TARGET_SLOT_NAMES)
HIT_SLOTS = {41, 42}
SCREEN_SLOTS = {59, 60, 65, 69, 70}
EFFECT_NESTED_SLOTS = {
    3,   # WAZ 引用
    4,   # 同时发射数
    6,   # 间隔
    7,   # 发射锚点
    8, 9, 10, 11,  # 锚点方向、距离和高度
    25,  # 位置
    28, 29, 30, 31, 32, 33, 34, 35,  # 向量族
    39, 40,  # 优先级和耐久度
}
HIT_NESTED_SLOTS = {6, 16, 25, 27, 31}

# CEventHit 使用独立的嵌套 BHE -> BSDX 索引映射。
# 此处直接复制 CEventHit#transBheCEventHitToBsdx，不根据描述文本推断。
BHE_HIT_TO_BSDX_SLOT = (
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
    -1, -1, -1, -1, -1, -1, -1,
    10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
    -1,
    21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
)

NOISE_FIELDS = {
    "offset", "slotNum", "typeId", "unitQuantity", "unitDescription",
    "description", "buffer", "ceventEffectUnitQuantity",
    "ceventHitUnitQuantity", "unitSlotNum",
}

HIT_SOURCE_FIELD_MAP = {
    "attackTargetType": "short1",
    "short2": "short2",
    "int1": "int1",
    "int2": "int2",
    "int3": "int3",
    "hitCount": "int4",
    "hitInterval": "int5",
    "internalCorrection": "int6",
    "midComboCorrection": "int7",
    "endCorrection": "int8",
    "minDamage": "int9",
    "startComboCorrection": "int16",
    "int11": "int17",
    "chargeDamageRate": "int18",
    "int13": "int20",
    "int14": "int21",
    "int15": "int23",
    "int16": "int24",
    "int17": "int25",
    "int18": "int26",
    "screenShakeFrame": "int27",
    "int20": "int28",
    "int21": "int29",
    "int22": "int30",
    "selfStunFrame": "int31",
}

HIT_FIELDS = [
    "attackTargetType", "hitCount", "hitInterval", "internalCorrection",
    "midComboCorrection", "endCorrection", "minDamage",
    "startComboCorrection", "int11", "chargeDamageRate",
    "screenShakeFrame", "selfStunFrame",
]

RUNTIME_OBSERVATIONS = {
    ("sou", "R_HEAD03"): "实机释放直接闪退。",
    ("sou", "N_HOUDAN03"): "实机释放后本体隐形且未恢复。",
    ("sou", "KAWARA03"): "实机砸地阶段直接闪退。",
    ("misaki", "G_BIT07"): "实机释放时严重卡顿并瞬时掉帧。",
    ("misaki", "G_BIT04"): "实机命中时出现 999+ Combo Hit 并直接击破目标。",
}

REQUIRED_SKILLS = {
    "sou": {"STRAIGHT03", "UPPER03", "KAWARA03", "N_HOUDAN03", "R_HEAD03"},
    "misaki": {"WARP_P1_11", "S_HAMMER06", "0013"},
    "tsukuyomi": {"G_FIELD", "0014"},
}

EXPECTED_SKILL_COUNTS = {
    "sou": 150,
    "tsukuyomi": 8,
    "misaki": 20,
}

CATEGORY_LABELS = {
    "crash": "① 闪退避让点",
    "damage": "② 伤害与连段补正",
    "effect": "③ 粒子特效与坐标向量",
    "phase": "④ 动作相位衔接与取消窗口",
    "screen": "⑤ 屏幕演出与慢动作",
}

CATEGORY_ORDER = tuple(CATEGORY_LABELS)


@dataclass(frozen=True)
class CharacterSpec:
    role: str
    source_waz: str
    source_mek: str
    mod_waz: str
    mod_mek: str


CHARACTER_SPECS = (
    CharacterSpec(
        "sou",
        "src/main/resources/wazBheJson/sou.waz.json",
        "src/main/resources/mekBheJson/sou.mek.json",
        "aki.waz.json",
        "aki.mek.json",
    ),
    CharacterSpec(
        "tsukuyomi",
        "src/main/resources/wazBheJson/zako681a.waz.json",
        "src/main/resources/mekBheJson/zako681a.mek.json",
        "zako681a.waz.json",
        "zako102a.mek.json",
    ),
    CharacterSpec(
        "misaki",
        "src/main/resources/wazBheJson/misaki.waz.json",
        "src/main/resources/mekBheJson/misaki.mek.json",
        "misaki.waz.json",
        "mohawk.mek.json",
    ),
)


@dataclass
class ResolvedReference:
    canonical: str
    display: str
    file_code: str
    skill_identity: tuple[str, str] | None
    raw_file_no: int | None
    raw_sequence: int | None
    valid: bool
    issue: str | None


@dataclass
class ResolvedSpriteReference:
    canonical: str
    display: str
    raw_file_no: int | None
    valid: bool
    issue: str | None


@dataclass
class Signal:
    key: tuple[Any, ...]
    category: str
    locator: str
    field: str
    canonical: Any
    display: str


@dataclass
class DiffRow:
    category: str
    locator: str
    field: str
    source: str
    mod: str
    mechanism: str


@dataclass
class CharacterData:
    spec: CharacterSpec
    source_waz: dict[str, Any]
    source_mek: dict[str, Any]
    mod_waz: dict[str, Any]
    mod_mek: dict[str, Any]
    source_self_waz: int
    source_self_spm: int
    mod_self_waz: int
    mod_self_spm: int


@dataclass
class SkillAnalysis:
    role: str
    source_index: int
    mod_index: int
    skill: dict[str, Any]
    mek_keys: list[str]
    rows: list[DiffRow]
    involved_nodes: list[str]
    dropped_nodes: list[str]
    unresolved_source_references: int
    unresolved_mod_references: int


class TermDecoder:
    def __init__(self, term_json: dict[str, Any]):
        self.groups = term_json.get("termList", [])

    def decode(self, collection: dict[str, Any]) -> dict[str, Any]:
        start = collection.get("int1")
        types = collection.get("typeList") or []
        current = start
        path: list[str] = []
        warnings: list[str] = []
        terminated = False
        for depth, item_index in enumerate(types):
            if not isinstance(current, int) or current < 0 or current >= len(self.groups):
                warnings.append(f"第 {depth} 层的分组超出范围：{current}")
                break
            group = self.groups[current]
            items = group.get("termItemList") or []
            if not isinstance(item_index, int) or item_index < 0 or item_index >= len(items):
                warnings.append(f"第 {depth} 层的条目超出范围：{item_index}")
                break
            item = items[item_index]
            group_code = group.get("termGroupCodeName") or ""
            item_description = item.get("termItemDescription") or ""
            path.append(f"{group_code}/{item_description}")
            current = item.get("param2")
            if isinstance(current, int) and current < 0:
                terminated = True
                break
        return {
            "path": " -> ".join(path),
            "paramList": collection.get("paramList") or [],
            "intList3": collection.get("intList3") or [],
            "intList4": collection.get("intList4") or [],
            "int2": collection.get("int2"),
            "terminated": terminated,
            "warnings": warnings,
        }


class ReferenceResolver:
    def __init__(self, repo_root: Path, mod_dump: Path):
        self.repo_root = repo_root
        self.mod_dump = mod_dump
        source_group_path = repo_root / "src/main/resources/grpBheJson/wazagroup.grp.json"
        mod_group_path = find_case_insensitive(mod_dump, "wazagroup.grp.json")
        self.loaded_paths: set[Path] = {source_group_path, mod_group_path}
        self.source_group = load_json(source_group_path)
        self.mod_group = load_json(mod_group_path)
        self.source_entries = self.source_group.get("wazaList", [])
        self.mod_entries = self.mod_group.get("wazaList", [])
        self._waz_cache: dict[tuple[str, str], dict[str, Any] | None] = {}

    def resolve(
        self,
        engine: str,
        character: CharacterData,
        raw_file_no: Any,
        raw_sequence: Any,
    ) -> ResolvedReference:
        if not isinstance(raw_file_no, int) or not isinstance(raw_sequence, int):
            display = f"{json_inline(raw_file_no)}:{json_inline(raw_sequence)}（未解析）"
            return ResolvedReference(
                display,
                display,
                "UNRESOLVED",
                None,
                None,
                None,
                False,
                "WAZ 引用不是整数",
            )

        self_index = character.source_self_waz if engine == "source" else character.mod_self_waz
        issue = None
        if raw_file_no == self_index:
            file_code = "SELF"
            waz = character.source_waz if engine == "source" else character.mod_waz
        else:
            entries = self.source_entries if engine == "source" else self.mod_entries
            if not 0 <= raw_file_no < len(entries):
                entry = {}
                issue = f"wazFileNo 超出范围（条目数={len(entries)}）"
            else:
                entry = entries[raw_file_no]
            file_code = (entry.get("wazaCodeName") or f"INDEX_{raw_file_no}").upper()
            waz = self._load_external_waz(engine, entry)
            if issue is None and waz is None:
                issue = "引用的 WAZ 载荷未解码"

        skill = None
        skills = waz.get("skillList", []) if isinstance(waz, dict) else []
        if 0 <= raw_sequence < len(skills):
            candidate = skills[raw_sequence]
            if candidate.get("skillNameJapanese") is not None or candidate.get("skillNameEnglish") is not None:
                skill = skill_identity(candidate)
        else:
            issue = issue or f"wazSequenceNo 超出范围（技能数={len(skills)}）"

        if skill is None:
            canonical = f"{file_code}#{raw_sequence}"
            display = f"{raw_file_no}:{raw_sequence} ({file_code} / <范围内未命名条目>)"
        else:
            jp, en = skill
            canonical = f"{file_code}\x1f{jp}\x1f{en}"
            display = f"{raw_file_no}:{raw_sequence} ({file_code} / {jp} / {en})"
        if issue is not None:
            display += f" [无效：{issue}]"
        return ResolvedReference(
            canonical,
            display,
            file_code,
            skill,
            raw_file_no,
            raw_sequence,
            issue is None,
            issue,
        )

    def _load_external_waz(self, engine: str, entry: dict[str, Any]) -> dict[str, Any] | None:
        display_name = entry.get("wazaDisplayName")
        code_name = entry.get("wazaCodeName")
        cache_key = (engine, str(display_name or code_name or ""))
        if cache_key in self._waz_cache:
            return self._waz_cache[cache_key]

        if not display_name:
            self._waz_cache[cache_key] = None
            return None
        if engine == "source":
            path = find_case_insensitive(
                self.repo_root / "src/main/resources/wazBheJson",
                f"{display_name}.waz.json",
                required=False,
            )
        else:
            path = find_case_insensitive(self.mod_dump, f"{display_name}.waz.json", required=False)
        if path is not None:
            self.loaded_paths.add(path)
        value = load_json(path) if path is not None else None
        self._waz_cache[cache_key] = value
        return value


class SpriteReferenceResolver:
    def __init__(self, repo_root: Path, mod_dump: Path):
        source_path = find_case_insensitive(
            repo_root / "src/main/resources/grpBheJson",
            "spritegroup.grp.json",
        )
        mod_path = find_case_insensitive(mod_dump, "spritegroup.grp.json")
        self.loaded_paths: set[Path] = {source_path, mod_path}
        self.source_entries = load_json(source_path).get("spriteList", [])
        self.mod_entries = load_json(mod_path).get("spriteList", [])

    def resolve(
        self,
        engine: str,
        character: CharacterData,
        raw_file_no: Any,
    ) -> ResolvedSpriteReference:
        if not isinstance(raw_file_no, int):
            display = f"{json_inline(raw_file_no)}（未解析）"
            return ResolvedSpriteReference(
                display,
                display,
                None,
                False,
                "SPM 引用不是整数",
            )

        entries = self.source_entries if engine == "source" else self.mod_entries
        self_index = character.source_self_spm if engine == "source" else character.mod_self_spm
        effective_index = self_index if raw_file_no == -1 else raw_file_no
        if not 0 <= effective_index < len(entries):
            issue = f"spmFileSequence 超出范围（条目数={len(entries)}）"
            display = f"{raw_file_no} (INDEX_{effective_index}) [无效：{issue}]"
            return ResolvedSpriteReference(
                f"INDEX_{effective_index}",
                display,
                raw_file_no,
                False,
                issue,
            )

        entry = entries[effective_index]
        code = (entry.get("spriteCodeName") or "").upper()
        file_name = entry.get("spriteFileName") or ""
        is_self = raw_file_no == -1 or effective_index == self_index
        canonical = "SELF" if is_self else f"{code}\x1f{file_name.casefold()}"
        self_marker = "SELF / " if is_self else ""
        display = f"{raw_file_no} ({self_marker}{code} / {file_name})"
        return ResolvedSpriteReference(canonical, display, raw_file_no, True, None)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--mod-dump", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    return parser.parse_args()


def load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def find_case_insensitive(directory: Path, name: str, required: bool = True) -> Path | None:
    target = name.casefold()
    if directory.is_dir():
        for child in directory.iterdir():
            if child.name.casefold() == target:
                return child
    if required:
        raise FileNotFoundError(f"{directory} 下缺少 {name}")
    return None


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def skill_identity(skill: dict[str, Any]) -> tuple[str, str]:
    return (skill.get("skillNameJapanese") or "", skill.get("skillNameEnglish") or "")


def json_inline(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def markdown_value(value: str) -> str:
    return value.replace("|", "\\|").replace("\r", "").replace("\n", "<br>")


def write_text_crlf(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    normalized = text.replace("\r\n", "\n").replace("\r", "\n")
    with path.open("w", encoding="utf-8", newline="") as handle:
        handle.write(normalized.replace("\n", "\r\n"))


def load_character_data(repo_root: Path, mod_dump: Path, spec: CharacterSpec) -> CharacterData:
    source_waz = load_json(repo_root / spec.source_waz)
    source_mek = load_json(repo_root / spec.source_mek)
    mod_waz = load_json(find_case_insensitive(mod_dump, spec.mod_waz))
    mod_mek = load_json(find_case_insensitive(mod_dump, spec.mod_mek))
    source_basic = source_mek.get("mekBasicInfo", {})
    mod_basic = mod_mek.get("mekBasicInfo", {})
    return CharacterData(
        spec=spec,
        source_waz=source_waz,
        source_mek=source_mek,
        mod_waz=mod_waz,
        mod_mek=mod_mek,
        source_self_waz=require_int(source_basic, "wazFileSequence", spec.role),
        source_self_spm=require_int(source_basic, "spmFileSequence", spec.role),
        mod_self_waz=require_int(mod_basic, "wazFileSequence", spec.role),
        mod_self_spm=require_int(mod_basic, "spmFileSequence", spec.role),
    )


def require_int(obj: dict[str, Any], key: str, label: str) -> int:
    value = obj.get(key)
    if not isinstance(value, int):
        raise ValueError(f"{label}: {key} 应为整数，实际为 {value!r}")
    return value


def selected_mod_skills(character: CharacterData) -> list[tuple[int, dict[str, Any]]]:
    skills = character.mod_waz.get("skillList", [])
    refs = {
        weapon.get("wazSequence")
        for weapon in character.mod_mek.get("mekWeaponInfoMap", {}).values()
        if isinstance(weapon, dict)
        and isinstance(weapon.get("wazSequence"), int)
        and 0 <= weapon["wazSequence"] < len(skills)
    }
    selected = [(index, skills[index]) for index in sorted(refs)]
    if not selected:
        raise ValueError(f"{character.spec.role}: Mod MEK 未选中任何 WAZ 技能")
    return selected


def source_skill_lookup(character: CharacterData) -> dict[tuple[str, str], list[tuple[int, dict[str, Any]]]]:
    result: dict[tuple[str, str], list[tuple[int, dict[str, Any]]]] = defaultdict(list)
    for index, skill in enumerate(character.source_waz.get("skillList", [])):
        identity = skill_identity(skill)
        if identity != ("", ""):
            result[identity].append((index, skill))
    return result


def normalized_top_slot(raw_slot: Any, engine: str) -> int | None:
    if not isinstance(raw_slot, int):
        return None
    if engine == "source":
        mapped = BHE_TO_BSDX_SLOT.get(raw_slot)
        return None if mapped is None or mapped < 0 else mapped
    return raw_slot


def normalized_effect_slot(raw_slot: Any, engine: str) -> int | None:
    if not isinstance(raw_slot, int):
        return None
    if engine != "source":
        return raw_slot
    if raw_slot == 28:
        return None
    return raw_slot if raw_slot < 28 else raw_slot - 1


def normalized_hit_slot(raw_slot: Any, engine: str) -> int | None:
    if not isinstance(raw_slot, int):
        return None
    if engine != "source":
        return raw_slot
    if not 0 <= raw_slot < len(BHE_HIT_TO_BSDX_SLOT):
        return None
    mapped = BHE_HIT_TO_BSDX_SLOT[raw_slot]
    return None if mapped < 0 else mapped


def phase_events(skill: dict[str, Any], phase_index: int, slot: int, engine: str) -> list[dict[str, Any]]:
    phases = skill.get("phasesInfo") or []
    if not 0 <= phase_index < len(phases):
        return []
    result: list[dict[str, Any]] = []
    for unit in phases[phase_index].get("skillUnitCollection") or []:
        if normalized_top_slot(unit.get("unitQuantity"), engine) == slot:
            result.extend(
                item
                for item in unit.get("skillInfoObjectList") or []
                if isinstance(item, dict)
            )
    return result


def has_waz_reference(value: Any) -> bool:
    if isinstance(value, dict):
        if "wazFileNo" in value and (
            "wazSequenceNo" in value or "wazSequence" in value
        ):
            return True
        return any(has_waz_reference(item) for item in value.values())
    if isinstance(value, list):
        return any(has_waz_reference(item) for item in value)
    return False


def slots_with_waz_references(skill: dict[str, Any], engine: str) -> set[int]:
    result: set[int] = set()
    for phase in skill.get("phasesInfo") or []:
        for unit in phase.get("skillUnitCollection") or []:
            slot = normalized_top_slot(unit.get("unitQuantity"), engine)
            if slot is None:
                continue
            if any(has_waz_reference(item) for item in unit.get("skillInfoObjectList") or []):
                result.add(slot)
    return result


def direct_waz_reference(payload: dict[str, Any]) -> tuple[Any, Any] | None:
    if "wazFileNo" not in payload:
        return None
    if "wazSequenceNo" in payload:
        return payload.get("wazFileNo"), payload.get("wazSequenceNo")
    if "wazSequence" in payload:
        return payload.get("wazFileNo"), payload.get("wazSequence")
    return None


def recursive_waz_references(value: Any) -> list[tuple[Any, Any]]:
    result: list[tuple[Any, Any]] = []

    def visit(node: Any) -> None:
        if isinstance(node, dict):
            reference = direct_waz_reference(node)
            if reference is not None:
                result.append(reference)
            for child in node.values():
                visit(child)
        elif isinstance(node, list):
            for child in node:
                visit(child)

    visit(value)
    return result


def decoded_collection_canonical(decoded: dict[str, Any]) -> tuple[Any, ...]:
    return (
        decoded.get("path"),
        tuple(decoded.get("paramList") or []),
        tuple(decoded.get("intList3") or []),
        tuple(decoded.get("intList4") or []),
        decoded.get("int2"),
        bool(decoded.get("terminated")),
        tuple(decoded.get("warnings") or []),
    )


def decoded_collection_display(decoded: dict[str, Any]) -> str:
    value = {
        "path": decoded.get("path"),
        "paramList": decoded.get("paramList") or [],
        "intList3": decoded.get("intList3") or [],
        "intList4": decoded.get("intList4") or [],
        "int2": decoded.get("int2"),
        "terminated": bool(decoded.get("terminated")),
    }
    if decoded.get("warnings"):
        value["warnings"] = decoded["warnings"]
    return json_inline(value)


def info_collection_entries(
    payload: dict[str, Any],
    engine: str,
) -> Iterable[tuple[str, int, dict[str, Any]]]:
    prefix = "bhe" if engine == "source" else "bsdx"
    for key, value in payload.items():
        if not key.startswith(prefix + "InfoCollectionList") or not isinstance(value, list):
            continue
        neutral = key[len(prefix):]
        for index, collection in enumerate(value):
            if isinstance(collection, dict):
                yield neutral, index, collection


def generic_scalar_fields(payload: dict[str, Any]) -> Iterable[tuple[str, Any]]:
    ignored = NOISE_FIELDS | {
        "wazFileNo",
        "wazSequenceNo",
        "wazSequence",
        "spmFileSequence",
    }
    for key in sorted(payload):
        value = payload[key]
        if key in ignored or key.startswith("bheInfoCollectionList") or key.startswith("bsdxInfoCollectionList"):
            continue
        if key.endswith("UnitList") or key in {"unitList", "ceventEffectUnitList", "ceventHitUnitList"}:
            continue
        if value is None or isinstance(value, (str, int, float, bool)):
            yield key, value
        elif isinstance(value, list) and all(
            item is None or isinstance(item, (str, int, float, bool))
            for item in value
        ):
            yield key, value


def make_signal(
    key: tuple[Any, ...],
    category: str,
    locator: str,
    field: str,
    canonical: Any,
    display: str | None = None,
) -> Signal:
    return Signal(
        key=key,
        category=category,
        locator=locator,
        field=field,
        canonical=canonical,
        display=json_inline(canonical) if display is None else display,
    )


def add_collection_signals(
    signals: list[Signal],
    payload: dict[str, Any],
    engine: str,
    decoder: TermDecoder,
    category: str,
    locator: str,
    prefix: str,
) -> None:
    for name, index, collection in info_collection_entries(payload, engine):
        decoded = decoder.decode(collection)
        signals.append(
            make_signal(
                ("collection", prefix, name, index),
                category,
                locator,
                f"{prefix} / {name} #{index + 1}",
                decoded_collection_canonical(decoded),
                decoded_collection_display(decoded),
            )
        )


def add_payload_signals(
    signals: list[Signal],
    payload: dict[str, Any],
    engine: str,
    character: CharacterData,
    decoder: TermDecoder,
    reference_resolver: ReferenceResolver,
    category: str,
    locator: str,
    prefix: str,
    key_prefix: tuple[Any, ...],
) -> int:
    unresolved = 0
    reference = direct_waz_reference(payload)
    if reference is not None:
        resolved = reference_resolver.resolve(engine, character, reference[0], reference[1])
        signals.append(
            make_signal(
                key_prefix + ("waz-reference",),
                category,
                locator,
                f"{prefix} / wazFileNo:wazSequenceNo（已解析）",
                (resolved.canonical, resolved.valid, resolved.issue),
                resolved.display,
            )
        )
        unresolved += 0 if resolved.valid else 1

    for field, value in generic_scalar_fields(payload):
        signals.append(
            make_signal(
                key_prefix + ("field", field),
                category,
                locator,
                f"{prefix} / {field}",
                value,
            )
        )
    add_collection_signals(signals, payload, engine, decoder, category, locator, prefix)
    return unresolved


def grouped_nested_units(
    event: dict[str, Any],
    list_name: str,
    engine: str,
    mapper: Any,
) -> Iterable[tuple[int, int, dict[str, Any]]]:
    occurrences: Counter[int] = Counter()
    for unit in event.get(list_name) or []:
        if not isinstance(unit, dict):
            continue
        slot = mapper(unit.get("unitSlotNum"), engine)
        if slot is None:
            continue
        occurrence = occurrences[slot]
        occurrences[slot] += 1
        yield slot, occurrence, unit


def extract_nested_signals(
    event: dict[str, Any],
    engine: str,
    character: CharacterData,
    decoder: TermDecoder,
    reference_resolver: ReferenceResolver,
    category: str,
    locator: str,
    kind: str,
) -> tuple[list[Signal], int]:
    signals: list[Signal] = []
    unresolved = 0
    if kind == "effect":
        list_name = "ceventEffectUnitList"
        mapper = normalized_effect_slot
        selected_slots = EFFECT_NESTED_SLOTS
    else:
        list_name = "unitList" if engine == "source" else "ceventHitUnitList"
        mapper = normalized_hit_slot
        selected_slots = HIT_NESTED_SLOTS

    for slot, occurrence, unit in grouped_nested_units(event, list_name, engine, mapper):
        if slot not in selected_slots:
            continue
        prefix = f"Unit {slot}"
        if occurrence:
            prefix += f"（第 {occurrence + 1} 个）"
        data = unit.get("data")
        signals.append(
            make_signal(
                ("nested", slot, occurrence, "presence"),
                category,
                locator,
                f"{prefix} / 存在性",
                isinstance(data, dict),
                "存在" if isinstance(data, dict) else ABSENT,
            )
        )
        if isinstance(data, dict):
            unresolved += add_payload_signals(
                signals,
                data,
                engine,
                character,
                decoder,
                reference_resolver,
                category,
                locator,
                prefix,
                ("nested", slot, occurrence),
            )
    return signals, unresolved


def change_collection_signals(
    event: dict[str, Any],
    engine: str,
    decoder: TermDecoder,
    category: str,
    locator: str,
    phase_index: int,
) -> list[Signal]:
    signals: list[Signal] = []
    prefix = "bhe" if engine == "source" else "bsdx"
    condition_key = prefix + "InfoCollectionList1"
    transition_key = prefix + "InfoCollectionList2"

    for index, collection in enumerate(event.get(condition_key) or []):
        if not isinstance(collection, dict):
            continue
        decoded = decoder.decode(collection)
        signals.append(
            make_signal(
                ("change", "condition", index),
                category,
                locator,
                f"条件 #{index + 1}",
                decoded_collection_canonical(decoded),
                decoded_collection_display(decoded),
            )
        )

    for index, collection in enumerate(event.get(transition_key) or []):
        if not isinstance(collection, dict):
            continue
        decoded = decoder.decode(collection)
        path = decoded.get("path") or ""
        params = decoded.get("paramList") or []
        signals.append(
            make_signal(
                ("change", "transition", index),
                category,
                locator,
                f"转移 #{index + 1}",
                decoded_collection_canonical(decoded),
                decoded_collection_display(decoded),
            )
        )
        if path.endswith("TURN/NEXT"):
            signals.append(
                make_signal(
                    ("change", "nextPhaseNo", index),
                    category,
                    locator,
                    "nextPhaseNo（推导值，从零开始）",
                    phase_index + 1,
                )
            )
        elif path.endswith("TURN/END"):
            signals.append(
                make_signal(
                    ("change", "nextPhaseNo", index),
                    category,
                    locator,
                    "nextPhaseNo（推导值）",
                    "END",
                )
            )
        elif path.endswith("TURN/INPUT") and params:
            signals.append(
                make_signal(
                    ("change", "branchPhaseNo", index),
                    category,
                    locator,
                    "branchPhaseNo（原始 paramList[0]）",
                    params[0],
                )
            )
    return signals


def category_for_slot(slot: int) -> str:
    if slot in HIT_SLOTS:
        return "damage"
    if slot == 48:
        return "effect"
    if slot in {47, 71}:
        return "phase"
    if slot in SCREEN_SLOTS:
        return "screen"
    return "crash"


def slot_event_name(slot: int) -> str:
    if slot == 0:
        return "CEventSprite"
    return TARGET_SLOT_NAMES.get(slot, "WAZ 引用事件")


def event_locator(
    phase_index: int,
    slot: int,
    source_ordinal: int | None,
    mod_ordinal: int | None,
) -> str:
    source_label = "无" if source_ordinal is None else f"BHE #{source_ordinal + 1}"
    mod_label = "无" if mod_ordinal is None else f"Mod #{mod_ordinal + 1}"
    return (
        f"阶段 {phase_index + 1} - 槽位 {slot} {slot_event_name(slot)} "
        f"({source_label} / {mod_label})"
    )


def extract_event_signals(
    event: dict[str, Any],
    engine: str,
    character: CharacterData,
    decoder: TermDecoder,
    reference_resolver: ReferenceResolver,
    sprite_resolver: SpriteReferenceResolver,
    phase_index: int,
    slot: int,
    locator: str,
    reference_only: bool = False,
) -> tuple[list[Signal], int]:
    category = category_for_slot(slot)
    signals: list[Signal] = []
    unresolved = 0

    if reference_only:
        for index, raw in enumerate(recursive_waz_references(event)):
            resolved = reference_resolver.resolve(engine, character, raw[0], raw[1])
            signals.append(
                make_signal(
                    ("recursive-waz-reference", index),
                    "crash",
                    locator,
                    f"嵌套 WAZ 引用 #{index + 1} / wazFileNo:wazSequenceNo（已解析）",
                    (resolved.canonical, resolved.valid, resolved.issue),
                    resolved.display,
                )
            )
            unresolved += 0 if resolved.valid else 1
        return signals, unresolved

    for field in ("startFrame", "endFrame"):
        signals.append(
            make_signal(("frame", field), category, locator, field, event.get(field))
        )

    if slot == 0:
        resolved_spm = sprite_resolver.resolve(engine, character, event.get("spmFileSequence"))
        signals.append(
            make_signal(
                ("sprite", "spmFileSequence"),
                "crash",
                locator,
                "spmFileSequence（已解析）",
                (resolved_spm.canonical, resolved_spm.valid, resolved_spm.issue),
                resolved_spm.display,
            )
        )
        unresolved += 0 if resolved_spm.valid else 1
        for field in ("actionGroupNumber", "actionNumber"):
            signals.append(
                make_signal(("sprite", field), "crash", locator, field, event.get(field))
            )
        return signals, unresolved

    if slot in HIT_SLOTS:
        for target_field in HIT_FIELDS:
            source_field = HIT_SOURCE_FIELD_MAP[target_field] if engine == "source" else target_field
            signals.append(
                make_signal(
                    ("hit", target_field),
                    "damage",
                    locator,
                    target_field,
                    event.get(source_field),
                )
            )
        nested, nested_unresolved = extract_nested_signals(
            event,
            engine,
            character,
            decoder,
            reference_resolver,
            "damage",
            locator,
            "hit",
        )
        signals.extend(nested)
        unresolved += nested_unresolved
        return signals, unresolved

    if slot == 48:
        for field in (f"int{index}" for index in range(1, 12)):
            signals.append(
                make_signal(("effect", field), "effect", locator, field, event.get(field))
            )
        nested, nested_unresolved = extract_nested_signals(
            event,
            engine,
            character,
            decoder,
            reference_resolver,
            "effect",
            locator,
            "effect",
        )
        signals.extend(nested)
        unresolved += nested_unresolved
        return signals, unresolved

    if slot == 71:
        for field in ("flag", "int1"):
            signals.append(
                make_signal(("change", field), "phase", locator, field, event.get(field))
            )
        signals.extend(
            change_collection_signals(event, engine, decoder, "phase", locator, phase_index)
        )
        return signals, unresolved

    for field, value in generic_scalar_fields(event):
        if field not in {"startFrame", "endFrame"}:
            signals.append(
                make_signal(("top", field), category, locator, field, value)
            )
    add_collection_signals(signals, event, engine, decoder, category, locator, "事件")
    return signals, unresolved


def first_effect_reference(
    event: dict[str, Any],
    engine: str,
    character: CharacterData,
    resolver: ReferenceResolver,
) -> str | None:
    for slot, _, unit in grouped_nested_units(
        event,
        "ceventEffectUnitList",
        engine,
        normalized_effect_slot,
    ):
        if slot != 3 or not isinstance(unit.get("data"), dict):
            continue
        reference = direct_waz_reference(unit["data"])
        if reference is not None:
            return resolver.resolve(engine, character, reference[0], reference[1]).canonical
    return None


def transition_anchor(event: dict[str, Any], engine: str, decoder: TermDecoder) -> tuple[Any, ...]:
    prefix = "bhe" if engine == "source" else "bsdx"
    result = []
    for collection in event.get(prefix + "InfoCollectionList2") or []:
        if isinstance(collection, dict):
            result.append(decoded_collection_canonical(decoder.decode(collection)))
    return tuple(result)


def event_anchor(
    event: dict[str, Any],
    engine: str,
    character: CharacterData,
    decoder: TermDecoder,
    reference_resolver: ReferenceResolver,
    sprite_resolver: SpriteReferenceResolver,
    slot: int,
) -> tuple[Any, ...]:
    frames = (event.get("startFrame"), event.get("endFrame"))
    if slot == 48:
        return frames + (first_effect_reference(event, engine, character, reference_resolver),)
    if slot == 71:
        return frames + (transition_anchor(event, engine, decoder),)
    if slot == 0:
        resolved = sprite_resolver.resolve(engine, character, event.get("spmFileSequence"))
        return frames + (
            resolved.canonical,
            event.get("actionGroupNumber"),
            event.get("actionNumber"),
        )
    references = recursive_waz_references(event)
    if references:
        resolved = reference_resolver.resolve(
            engine,
            character,
            references[0][0],
            references[0][1],
        )
        return frames + (resolved.canonical,)
    return frames


def substitution_cost(source_anchor: tuple[Any, ...], mod_anchor: tuple[Any, ...], slot: int) -> float:
    if source_anchor == mod_anchor:
        return 0.0
    cost = 0.0
    if source_anchor[:2] != mod_anchor[:2]:
        cost += 1.0
    source_extra = source_anchor[2:]
    mod_extra = mod_anchor[2:]
    if source_extra != mod_extra:
        cost += 4.0 if slot == 71 else 2.0
    return max(1.0, cost)


def align_events(
    source_events: list[dict[str, Any]],
    mod_events: list[dict[str, Any]],
    source_anchor: Any,
    mod_anchor: Any,
    slot: int,
) -> list[tuple[int | None, dict[str, Any] | None, int | None, dict[str, Any] | None]]:
    delete_cost = 3.0
    rows = len(source_events) + 1
    columns = len(mod_events) + 1
    scores = [[0.0] * columns for _ in range(rows)]
    back: list[list[str | None]] = [[None] * columns for _ in range(rows)]
    for i in range(1, rows):
        scores[i][0] = i * delete_cost
        back[i][0] = "delete"
    for j in range(1, columns):
        scores[0][j] = j * delete_cost
        back[0][j] = "insert"

    source_anchors = [source_anchor(event) for event in source_events]
    mod_anchors = [mod_anchor(event) for event in mod_events]
    for i in range(1, rows):
        for j in range(1, columns):
            candidates = [
                (
                    scores[i - 1][j - 1]
                    + substitution_cost(source_anchors[i - 1], mod_anchors[j - 1], slot),
                    0,
                    "pair",
                ),
                (scores[i - 1][j] + delete_cost, 1, "delete"),
                (scores[i][j - 1] + delete_cost, 2, "insert"),
            ]
            score, _, operation = min(candidates)
            scores[i][j] = score
            back[i][j] = operation

    aligned = []
    i = len(source_events)
    j = len(mod_events)
    while i or j:
        operation = back[i][j]
        if operation == "pair":
            aligned.append((i - 1, source_events[i - 1], j - 1, mod_events[j - 1]))
            i -= 1
            j -= 1
        elif operation == "delete":
            aligned.append((i - 1, source_events[i - 1], None, None))
            i -= 1
        elif operation == "insert":
            aligned.append((None, None, j - 1, mod_events[j - 1]))
            j -= 1
        else:
            raise AssertionError(f"事件对齐在 {i}, {j} 处中断")
    aligned.reverse()
    return aligned


def event_presence_display(event: dict[str, Any] | None) -> str:
    if event is None:
        return ABSENT
    return f"存在（帧 {event.get('startFrame')}..{event.get('endFrame')}）"


def mechanism_for(
    category: str,
    field: str,
    source: str,
    mod: str,
    locator: str = "",
) -> str:
    label = CATEGORY_LABELS[category]
    if field == "事件存在性":
        if mod == ABSENT:
            return f"{label}: Mod 删除了这个可表示事件，完整窗口/引用路径不再执行。"
        return f"{label}: Mod 新增完整事件及其明确帧窗口。"
    if "wazFileNo:wazSequenceNo" in field:
        if "[无效：" in source and "[无效：" not in mod:
            return f"{label}: BHE 侧非法 WAZ 目标变为 Mod 侧可解析目标，这是明确的引用安全边界。"
        return f"{label}: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。"
    if "spmFileSequence" in field:
        if "[无效：" in source and "[无效：" not in mod:
            return f"{label}: 精灵载荷引用变为范围内可解析值。"
        return f"{label}: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。"
    if field in {"startFrame", "endFrame"}:
        return f"{label}: 改变该事件的精确激活/终止帧边界。"
    if field == "int11":
        return f"{label}: 改变 CEventHit 的 int11（power），即该命中节点携带的基础威力。"
    hit_meanings = {
        "hitCount": "改变单事件命中数",
        "hitInterval": "改变重复命中的帧间隔",
        "internalCorrection": "改变技能内部多段命中补正",
        "midComboCorrection": "改变连段中途补正",
        "endCorrection": "改变技能终了补正",
        "minDamage": "改变保底伤害阈值",
        "startComboCorrection": "改变起始/基底连段补正",
        "chargeDamageRate": "改变蓄力对伤害的贡献率",
        "attackTargetType": "改变攻击目标掩码",
        "screenShakeFrame": "改变命中触发的画面震颤时长",
        "selfStunFrame": "改变攻击方自身命中停顿/僵直时长",
    }
    if field in hit_meanings:
        return f"{label}: {hit_meanings[field]}。"
    if field == "flag":
        return f"{label}: 改变 CEventChange 的 flag，从而改变编码的条件列表/分支结构。"
    if "条件 #" in field:
        return f"{label}: 改变解码后的 Term 条件路径或操作数。"
    if "转移 #" in field:
        return f"{label}: 改变解码后的 CEventChange 转移路径或操作数。"
    if "nextPhaseNo" in field:
        return f"{label}: 改变推导出的线性/结束阶段目标。"
    if "branchPhaseNo" in field:
        return f"{label}: 改变条件分支的原始阶段参数。"
    if "Unit 4" in field:
        return f"{label}: 改变 CEventEffect 同时发射数。"
    if "Unit 6" in field and category == "effect":
        return f"{label}: 改变 CEventEffect 发射间隔控制。"
    if "Unit 6" in field and category == "damage" and "InfoCollectionList" in field:
        return f"{label}: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。"
    if "Unit 7" in field:
        return f"{label}: 改变解码后的发射锚点表达式或操作数。"
    anchor_meanings = {
        "Unit 8": "发射位置方向",
        "Unit 9": "发射位置方向补正",
        "Unit 10": "发射位置距离",
        "Unit 11": "发射位置高度偏移",
        "Unit 25": "位置表达式",
    }
    for prefix, meaning in anchor_meanings.items():
        if prefix in field:
            return f"{label}: 改变 CEventEffect {meaning}数据。"
    vector_meanings = {
        "Unit 28": "向量方向",
        "Unit 29": "向量方向补正",
        "Unit 30": "向量方向增量",
        "Unit 31": "初速度",
        "Unit 32": "向量高度",
        "Unit 33": "重力",
        "Unit 34": "最低高度",
        "Unit 35": "惯性",
    }
    for prefix, meaning in vector_meanings.items():
        if prefix in field:
            return f"{label}: 改变 CEventEffect {meaning}数据。"
    screen_meanings = {
        59: "画面缩放",
        60: "画面震颤",
        65: "动态模糊",
        69: "慢动作持续时间",
        70: "慢动作倍率",
    }
    for slot, meaning in screen_meanings.items():
        if f"槽位 {slot} " in locator:
            return f"{label}: 改变{meaning}控制数据。"
    if "InfoCollectionList" in field:
        return f"{label}: 改变解码后的 Term 表达式或其精确操作数。"
    if field in {"actionGroupNumber", "actionNumber"}:
        return f"{label}: 改变精灵事件使用的精确动作组/动作索引。"
    return f"{label}: 精确行为控制值不同；数据可证明修改，但不能单独证明唯一运行时根因。"


def compare_event_pair(
    source_ordinal: int | None,
    source_event: dict[str, Any] | None,
    mod_ordinal: int | None,
    mod_event: dict[str, Any] | None,
    character: CharacterData,
    source_decoder: TermDecoder,
    mod_decoder: TermDecoder,
    reference_resolver: ReferenceResolver,
    sprite_resolver: SpriteReferenceResolver,
    phase_index: int,
    slot: int,
    reference_only: bool,
) -> tuple[list[DiffRow], int, int]:
    locator = event_locator(phase_index, slot, source_ordinal, mod_ordinal)
    category = category_for_slot(slot)
    rows: list[DiffRow] = []
    if source_event is None or mod_event is None:
        source_display = event_presence_display(source_event)
        mod_display = event_presence_display(mod_event)
        rows.append(
            DiffRow(
                category,
                locator,
                "事件存在性",
                source_display,
                mod_display,
                mechanism_for(category, "事件存在性", source_display, mod_display, locator),
            )
        )

    source_signals: list[Signal] = []
    mod_signals: list[Signal] = []
    source_unresolved = 0
    mod_unresolved = 0
    if source_event is not None:
        source_signals, source_unresolved = extract_event_signals(
            source_event,
            "source",
            character,
            source_decoder,
            reference_resolver,
            sprite_resolver,
            phase_index,
            slot,
            locator,
            reference_only,
        )
    if mod_event is not None:
        mod_signals, mod_unresolved = extract_event_signals(
            mod_event,
            "mod",
            character,
            mod_decoder,
            reference_resolver,
            sprite_resolver,
            phase_index,
            slot,
            locator,
            reference_only,
        )

    source_map = {signal.key: signal for signal in source_signals}
    mod_map = {signal.key: signal for signal in mod_signals}
    if len(source_map) != len(source_signals) or len(mod_map) != len(mod_signals):
        raise AssertionError(f"{locator} 存在重复信号键")
    for key in sorted(set(source_map) | set(mod_map), key=repr):
        source_signal = source_map.get(key)
        mod_signal = mod_map.get(key)
        source_value = ABSENT if source_signal is None else source_signal.display
        mod_value = ABSENT if mod_signal is None else mod_signal.display
        source_canonical = ABSENT if source_signal is None else source_signal.canonical
        mod_canonical = ABSENT if mod_signal is None else mod_signal.canonical
        if source_canonical == mod_canonical:
            continue
        signal = source_signal or mod_signal
        assert signal is not None
        rows.append(
            DiffRow(
                signal.category,
                locator,
                signal.field,
                source_value,
                mod_value,
                mechanism_for(signal.category, signal.field, source_value, mod_value, locator),
            )
        )
    return rows, source_unresolved, mod_unresolved


def mek_keys_for_sequence(character: CharacterData, sequence: int) -> list[str]:
    keys = [
        str(key)
        for key, weapon in character.mod_mek.get("mekWeaponInfoMap", {}).items()
        if isinstance(weapon, dict) and weapon.get("wazSequence") == sequence
    ]
    return sorted(keys, key=lambda value: (not value.isdigit(), int(value) if value.isdigit() else value))


def dropped_source_nodes(skill: dict[str, Any]) -> list[str]:
    result = []
    for phase_index, phase in enumerate(skill.get("phasesInfo") or []):
        for unit in phase.get("skillUnitCollection") or []:
            raw_slot = unit.get("unitQuantity")
            if not isinstance(raw_slot, int) or BHE_TO_BSDX_SLOT.get(raw_slot) != -1:
                continue
            events = [item for item in unit.get("skillInfoObjectList") or [] if isinstance(item, dict)]
            if not events:
                continue
            frames = ", ".join(
                f"{item.get('startFrame')}..{item.get('endFrame')}"
                for item in events
            )
            result.append(
                f"阶段 {phase_index + 1} - 原始 BHE 槽位 {raw_slot}: "
                f"{len(events)} 个事件，帧范围 {frames}"
            )
    return result


def analyze_skill(
    character: CharacterData,
    source_index: int,
    source_skill: dict[str, Any],
    mod_index: int,
    mod_skill: dict[str, Any],
    source_decoder: TermDecoder,
    mod_decoder: TermDecoder,
    reference_resolver: ReferenceResolver,
    sprite_resolver: SpriteReferenceResolver,
) -> SkillAnalysis:
    source_phases = source_skill.get("phasesInfo") or []
    mod_phases = mod_skill.get("phasesInfo") or []
    if len(source_phases) != len(mod_phases):
        raise ValueError(
            f"{character.spec.role}/{skill_identity(mod_skill)}: 阶段数量不一致 "
            f"{len(source_phases)} != {len(mod_phases)}"
        )

    extra_reference_slots = (
        slots_with_waz_references(source_skill, "source")
        | slots_with_waz_references(mod_skill, "mod")
    ) - HIGH_VALUE_SLOTS
    comparison_slots = HIGH_VALUE_SLOTS | {0} | extra_reference_slots
    rows: list[DiffRow] = []
    involved_nodes: list[str] = []
    source_unresolved = 0
    mod_unresolved = 0

    for phase_index in range(len(source_phases)):
        for slot in sorted(comparison_slots):
            source_events = phase_events(source_skill, phase_index, slot, "source")
            mod_events = phase_events(mod_skill, phase_index, slot, "mod")
            if not source_events and not mod_events:
                continue
            reference_only = slot not in HIGH_VALUE_SLOTS and slot != 0
            aligned = align_events(
                source_events,
                mod_events,
                lambda event: event_anchor(
                    event,
                    "source",
                    character,
                    source_decoder,
                    reference_resolver,
                    sprite_resolver,
                    slot,
                ),
                lambda event: event_anchor(
                    event,
                    "mod",
                    character,
                    mod_decoder,
                    reference_resolver,
                    sprite_resolver,
                    slot,
                ),
                slot,
            )
            node_rows: list[DiffRow] = []
            for source_ordinal, source_event, mod_ordinal, mod_event in aligned:
                pair_rows, pair_source_unresolved, pair_mod_unresolved = compare_event_pair(
                    source_ordinal,
                    source_event,
                    mod_ordinal,
                    mod_event,
                    character,
                    source_decoder,
                    mod_decoder,
                    reference_resolver,
                    sprite_resolver,
                    phase_index,
                    slot,
                    reference_only,
                )
                node_rows.extend(pair_rows)
                source_unresolved += pair_source_unresolved
                mod_unresolved += pair_mod_unresolved
            rows.extend(node_rows)
            if slot in HIGH_VALUE_SLOTS or node_rows:
                involved_nodes.append(
                    f"阶段 {phase_index + 1} - 槽位 {slot} {slot_event_name(slot)}"
                )

    # Python 排序是稳定的，因此按类别分组后仍保留阶段、槽位和事件序号的遍历顺序。
    rows.sort(key=lambda row: CATEGORY_ORDER.index(row.category))
    return SkillAnalysis(
        role=character.spec.role,
        source_index=source_index,
        mod_index=mod_index,
        skill=mod_skill,
        mek_keys=mek_keys_for_sequence(character, mod_index),
        rows=rows,
        involved_nodes=involved_nodes,
        dropped_nodes=dropped_source_nodes(source_skill),
        unresolved_source_references=source_unresolved,
        unresolved_mod_references=mod_unresolved,
    )


def render_diff_table(rows: list[DiffRow]) -> list[str]:
    output = [
        "| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |",
        "| :--- | :--- | :--- | :--- |",
    ]
    if not rows:
        output.append(
            "| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | "
            "五类限定信号中没有字段级修改；原始索引重绑已过滤。 |"
        )
        return output
    for row in rows:
        field = markdown_value(f"{row.locator} / {row.field}")
        output.append(
            f"| `{field}` | {markdown_value(row.source)} | {markdown_value(row.mod)} | "
            f"{markdown_value(row.mechanism)} |"
        )
    return output


def render_skill_section(analysis: SkillAnalysis) -> list[str]:
    jp, en = skill_identity(analysis.skill)
    lines = [
        f"### {jp} / {en}（原索引：BHE {analysis.source_index:03d} / Mod wazSeq {analysis.mod_index:03d}）",
        "",
        "#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）",
        "",
        f"- **Mod MEK 键**: {', '.join(analysis.mek_keys)}",
        "- **涉及阶段与槽位**: "
        + ("; ".join(analysis.involved_nodes) if analysis.involved_nodes else "无高价值事件节点"),
    ]
    category_counts = Counter(row.category for row in analysis.rows)
    lines.append(
        "- **五类差分计数**: "
        + "; ".join(
            f"{CATEGORY_LABELS[category]}={category_counts.get(category, 0)}"
            for category in CATEGORY_ORDER
        )
    )
    if analysis.dropped_nodes:
        lines.append(
            "- **仅 BHE 存在的转换风险边界**: "
            + "; ".join(analysis.dropped_nodes)
            + "。这些节点在生产槽位映射中映射为 `-1`，因此默认转换基线与 Mod 均不包含它们；不能将其归因于朋友对 Mod 的修改。"
        )
    lines.extend(["- **关键字段数值差分表**:", ""])
    lines.extend(render_diff_table(analysis.rows))
    lines.extend(["", "#### 2. 实际游戏战斗表现对比", ""])

    observation = RUNTIME_OBSERVATIONS.get((analysis.role, en))
    if observation:
        source_impact = (
            f"现有实机记录仅确认症状：{observation} "
            "该症状不作为字段因果证明；因果边界仍以本节表内的 Mod 数据差异为准。"
        )
    elif analysis.rows:
        source_impact = (
            f"默认转换在 {len(analysis.rows)} 个限定字段/节点上不等于 Mod 基准数据。"
            "这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。"
        )
    else:
        source_impact = (
            "在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；"
            "本数据集不能把独立行为差异归因于这些节点。"
        )
    if analysis.rows:
        changed_categories = [
            CATEGORY_LABELS[category]
            for category in CATEGORY_ORDER
            if category_counts.get(category, 0)
        ]
        mod_impact = (
            f"Mod 基准数据明确采用表中 {len(analysis.rows)} 项值，涉及 "
            f"{'、'.join(changed_categories)}。"
            "依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。"
        )
    else:
        mod_impact = (
            "Mod 基准数据保留了默认转换基线在限定信号上的语义；"
            "依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。"
        )
    lines.extend(
        [
            f"- **原有默认转换在游戏里的表现**: {source_impact}",
            f"- **朋友 Mod 修改后的实机表现**: {mod_impact}",
            "",
        ]
    )
    return lines


def character_summary(analyses: list[SkillAnalysis]) -> dict[str, Any]:
    category_counts = Counter(
        row.category
        for analysis in analyses
        for row in analysis.rows
    )
    return {
        "skillCount": len(analyses),
        "skillsWithDiff": sum(bool(analysis.rows) for analysis in analyses),
        "diffRowCount": sum(len(analysis.rows) for analysis in analyses),
        "categoryDiffRows": {
            category: category_counts.get(category, 0)
            for category in CATEGORY_ORDER
        },
        "droppedBheNodeGroups": sum(len(analysis.dropped_nodes) for analysis in analyses),
        "unresolvedSourceReferences": sum(
            analysis.unresolved_source_references for analysis in analyses
        ),
        "unresolvedModReferences": sum(
            analysis.unresolved_mod_references for analysis in analyses
        ),
    }


def render_report(
    analyses_by_role: dict[str, list[SkillAnalysis]],
    akao_evidence: dict[str, Any],
) -> str:
    lines = [
        "# Mod 样本技能战斗行为基准差分",
        "",
        "## 证据边界",
        "",
        "本文只比较五类战斗表现信号：闪退引用/帧边界、CEventHit 伤害与连段、CEventEffect 特效与向量、CEventChange/CEventCancel 相位窗口、屏幕与慢动作。覆盖替换造成的机体槽位、基础文本及非战斗环境差异不进入结果。",
        "",
        "- BHE 事件先按生产代码 `BheToBsdxWazSlotMap`、`CEventHit#transBheCEventHitToBsdx` 与 `CEventEffect#transBheCEventEffectToBsdx` 归一化为 BSDX 语义，再与 Mod 比较。",
        "- WAZ/SPM 数字索引先通过各自 `WazaGroup.grp` / `SpriteGroup.grp` 解析成资源与技能身份；相同身份的纯索引重绑被过滤。",
        "- `BheInfoCollection` 与 `BsdxInfoCollection` 分别通过各自 `Term.grp` 解码为原始 Term 路径与操作数；不比较引擎内部的分组编号。",
        "- 主键是 `(skillNameJapanese, skillNameEnglish)`；BHE Index、Mod wazSeq 与 Mod MEK 键仅作为辅助定位。阶段与事件编号在本文中从 1 开始；`branchPhaseNo` 保留原始参数值。",
        "- Lv3 `+2` Customizer 映射不在本次分析范围内，未作任何变更。",
        "",
        "## 覆盖范围汇总",
        "",
        "| 角色 | MEK 选中的可执行技能 | 含高价值差分的技能 | 精确差分行 | 默认转换丢弃的 BHE 节点组 | 未解析源引用 | 未解析 Mod 引用 |",
        "| :--- | ---: | ---: | ---: | ---: | ---: | ---: |",
    ]
    for role in ("sou", "tsukuyomi", "misaki"):
        summary = character_summary(analyses_by_role[role])
        lines.append(
            f"| `{role}` | {summary['skillCount']} | {summary['skillsWithDiff']} | "
            f"{summary['diffRowCount']} | {summary['droppedBheNodeGroups']} | "
            f"{summary['unresolvedSourceReferences']} | {summary['unresolvedModReferences']} |"
        )

    lines.extend(
        [
            "",
            "## akao",
            "",
            "### 基准数据可用性",
            "",
            f"- `Update3` 解包载荷包含 {akao_evidence['update3WazCount']} 个 `.waz` 文件和 {akao_evidence['update3MekCount']} 个 `.mek` 文件。",
            f"- `Update4` 包含 {akao_evidence['update4WazCount']} 个 `.waz` 文件和 {akao_evidence['update4MekCount']} 个 `.mek` 文件；其中可直接对应角色的文件对为 `aki.waz` / `aki.mek`、`misaki.waz` / `mohawk.mek`、`zako681a.waz` / `zako102a.mek`。",
            "- 本次请求范围内与 `akao` 有关的证据只有 `HellConfig.dat` 和 `SelectMekaMenuMeka.spm`，两者均不包含 Mod `akao` 的 CEventHit/CEventEffect/CEventChange/CEventCancel 技能图。",
            "- 因此，在不虚构证据的前提下，无法生成 `akao` 的逐技能 BHE 源数据与 Mod 基准数据差分行。本章记录这一否定性结果，并从战斗差分中排除编组和槽位替换产物。",
            "",
        ]
    )

    for role in ("sou", "tsukuyomi", "misaki"):
        lines.extend([f"## {role}", ""])
        summary = character_summary(analyses_by_role[role])
        lines.extend(
            [
                f"- **MEK 选中的可执行技能数**: {summary['skillCount']}",
                f"- **含高价值差分的技能数**: {summary['skillsWithDiff']}",
                f"- **精确字段/节点差分行数**: {summary['diffRowCount']}",
                "- **分类行数**: "
                + "; ".join(
                    f"{CATEGORY_LABELS[category]}={summary['categoryDiffRows'][category]}"
                    for category in CATEGORY_ORDER
                ),
                "",
            ]
        )
        for analysis in analyses_by_role[role]:
            lines.extend(render_skill_section(analysis))

    lines.extend(
        [
            "## 解释边界",
            "",
            "- Mod 数据是本次比较的基准，但静态差分只能证明数据与语义引用发生变化，不能证明唯一的闪退调用栈。既有实机记录只作为症状保留。",
            "- 解析后的 WAZ 资源身份相同时会过滤原始序号变化。分析器不会把每个外部弹药的内部载荷递归归因到所有调用技能；调用方差分行只证明它选择了哪个语义弹药或特效。",
            "- 映射为 `-1` 的 BHE 顶层槽位只作为转换风险边界报告，不归类为朋友对 Mod 的修改，因为生产默认转换基线本就会丢弃这些节点。",
            "- 报告覆盖每个 Mod MEK 选中的全部唯一 WAZ 序号。未被选中的辅助技能不属于本次交付物的可执行战斗范围。",
            "",
        ]
    )
    return "\n".join(lines)


def path_record(repo_root: Path, path: Path) -> dict[str, Any]:
    resolved = path.resolve()
    return {
        "path": resolved.relative_to(repo_root).as_posix(),
        "size": resolved.stat().st_size,
        "sha256": sha256(resolved),
    }


def akao_evidence(repo_root: Path) -> dict[str, Any]:
    update3 = repo_root / "src/main/resources/modsample/Update3"
    update4 = repo_root / "src/main/resources/modsample/Update4"
    return {
        "update3WazCount": len(list(update3.glob("*.waz"))),
        "update3MekCount": len(list(update3.glob("*.mek"))),
        "update4WazCount": len(list(update4.glob("*.waz"))),
        "update4MekCount": len(list(update4.glob("*.mek"))),
        "scopedFiles": ["HellConfig.dat", "SelectMekaMenuMeka.spm"],
    }


def validate_coverage(analyses_by_role: dict[str, list[SkillAnalysis]], report: str) -> None:
    total = 0
    for role, expected_count in EXPECTED_SKILL_COUNTS.items():
        analyses = analyses_by_role[role]
        if len(analyses) != expected_count:
            raise AssertionError(f"{role}: 应选中 {expected_count} 个技能，实际为 {len(analyses)} 个")
        english = {skill_identity(analysis.skill)[1] for analysis in analyses}
        missing = REQUIRED_SKILLS[role] - english
        if missing:
            raise AssertionError(f"{role}: 报告缺少必需技能：{sorted(missing)}")
        composite = [skill_identity(analysis.skill) for analysis in analyses]
        if len(composite) != len(set(composite)):
            raise AssertionError(f"{role}: 选中范围内存在重复复合身份")
        total += len(analyses)

    if report.count("#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）") != total:
        raise AssertionError("报告缺少一个或多个语义节点差分模板")
    if report.count("#### 2. 实际游戏战斗表现对比") != total:
        raise AssertionError("报告缺少一个或多个实机战斗表现对比模板")
    for forbidden in ("Pilot ID", "MekaPilot.dat", "SelectMekaMenu.dat"):
        if forbidden in report:
            raise AssertionError(f"禁止的环境噪音标记泄漏到报告中：{forbidden}")


def build_manifest(
    repo_root: Path,
    mod_dump: Path,
    report_path: Path,
    analyses_by_role: dict[str, list[SkillAnalysis]],
    reference_resolver: ReferenceResolver,
    sprite_resolver: SpriteReferenceResolver,
    evidence: dict[str, Any],
) -> dict[str, Any]:
    source_paths: set[Path] = {
        repo_root / "src/main/resources/grpBheJson/term.grp.json",
        repo_root / "src/main/resources/grpBsdxJson/Term.grp.json",
    }
    for spec in CHARACTER_SPECS:
        source_paths.add(repo_root / spec.source_waz)
        source_paths.add(repo_root / spec.source_mek)
    source_paths.update(reference_resolver.loaded_paths)
    source_paths.update(sprite_resolver.loaded_paths)

    dump_paths = sorted(mod_dump.glob("*.json"), key=lambda path: path.name.casefold())
    raw_root = repo_root / "src/main/resources/modsample/Update4"
    raw_paths = {
        raw_root / path.name.removesuffix(".json")
        for path in dump_paths
        if (raw_root / path.name.removesuffix(".json")).is_file()
    }
    raw_paths.update(
        {
            raw_root / "HellConfig.dat",
            raw_root / "SelectMekaMenuMeka.spm",
        }
    )
    tool_root = repo_root / "tools/mod_diff_analyzer"
    tool_paths = [
        tool_root / "analyze.py",
        tool_root / "ModSampleJsonDumper.java",
        tool_root / "run.ps1",
        tool_root / "README.md",
    ]
    summaries = {
        role: character_summary(analyses)
        for role, analyses in analyses_by_role.items()
    }
    return {
        "schemaVersion": 1,
        "toolVersion": TOOL_VERSION,
        "comparison": "BHE 归一化默认转换与 Mod 基准数据",
        "primaryKey": ["skillNameJapanese", "skillNameEnglish"],
        "roleOrder": ["akao", "sou", "tsukuyomi", "misaki"],
        "coverage": {
            "totalSelectedSkills": sum(len(items) for items in analyses_by_role.values()),
            "roles": summaries,
            "requiredSkills": {role: sorted(values) for role, values in REQUIRED_SKILLS.items()},
        },
        "akaoNegativeEvidence": evidence,
        "inputs": {
            "sourceJson": [
                path_record(repo_root, path)
                for path in sorted(source_paths, key=lambda item: str(item).casefold())
            ],
            "modRawPayload": [
                path_record(repo_root, path)
                for path in sorted(raw_paths, key=lambda item: str(item).casefold())
            ],
            "decodedModJson": [path_record(repo_root, path) for path in dump_paths],
            "toolFiles": [path_record(repo_root, path) for path in tool_paths],
        },
        "output": path_record(repo_root, report_path),
    }


def main() -> None:
    args = parse_args()
    repo_root = args.repo_root.resolve()
    mod_dump = args.mod_dump.resolve()
    report_path = (args.report if args.report.is_absolute() else repo_root / args.report).resolve()
    manifest_path = (args.manifest if args.manifest.is_absolute() else repo_root / args.manifest).resolve()

    source_term_path = repo_root / "src/main/resources/grpBheJson/term.grp.json"
    mod_term_path = repo_root / "src/main/resources/grpBsdxJson/Term.grp.json"
    source_decoder = TermDecoder(load_json(source_term_path))
    mod_decoder = TermDecoder(load_json(mod_term_path))
    reference_resolver = ReferenceResolver(repo_root, mod_dump)
    sprite_resolver = SpriteReferenceResolver(repo_root, mod_dump)
    analyses_by_role: dict[str, list[SkillAnalysis]] = {}

    for spec in CHARACTER_SPECS:
        character = load_character_data(repo_root, mod_dump, spec)
        lookup = source_skill_lookup(character)
        analyses: list[SkillAnalysis] = []
        for mod_index, mod_skill in selected_mod_skills(character):
            identity = skill_identity(mod_skill)
            matches = lookup.get(identity, [])
            if len(matches) != 1:
                raise ValueError(
                    f"{spec.role}: {identity!r} 应有且仅有一个 BHE 匹配，实际为 {len(matches)} 个"
                )
            source_index, source_skill = matches[0]
            analyses.append(
                analyze_skill(
                    character,
                    source_index,
                    source_skill,
                    mod_index,
                    mod_skill,
                    source_decoder,
                    mod_decoder,
                    reference_resolver,
                    sprite_resolver,
                )
            )
        analyses_by_role[spec.role] = analyses

    evidence = akao_evidence(repo_root)
    report = render_report(analyses_by_role, evidence)
    validate_coverage(analyses_by_role, report)
    write_text_crlf(report_path, report)

    manifest = build_manifest(
        repo_root,
        mod_dump,
        report_path,
        analyses_by_role,
        reference_resolver,
        sprite_resolver,
        evidence,
    )
    write_text_crlf(
        manifest_path,
        json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n",
    )
    print(
        f"已写入 {report_path}（{manifest['coverage']['totalSelectedSkills']} 个技能）"
    )
    print(f"已写入 {manifest_path}")


if __name__ == "__main__":
    main()
