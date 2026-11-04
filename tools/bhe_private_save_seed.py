"""Create one private Save seed using only the six approved CP932 title labels.

No unlock flags, game data or original Save bytes are modified. Mapping and native
block boundaries are documented in vanilla-encoding-forensics.md and confirmed
against 721780/722150/76CA60/76CC20.c. Unknown titles are refused, even when CP932
can decode their bytes. The resulting private seed must not be distributed.
"""

import sys
sys.dont_write_bytecode = True

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import stat
import struct
import zlib


PROJECT_ROOT = Path(__file__).resolve().parents[1]
TITLE_OFFSET = 0xF300
TITLE_CAPACITY = 1024
HEADER_SIZE = 63336
TITLE_MAPPINGS = (
    ("【设置菜单】", "【設定メニュー】"),
    ("『克里斯』 结束", "『クリス』 終了"),
    ("『终结之日』 结束", "『終結の日』 終了"),
    ("「菜叶不会使用战斗用电子体，亚季姐也讨厌麻烦的事啊……」",
     "「菜ノ葉は戦闘用シュミクラムを使えないし、亜季姉も面倒なことは嫌うからな……」"),
    ("『误解』 结束", "『誤解』 終了"),
    ("那么，要去哪里呢？", "さて、どこへ行こうか？"),
)


def _sha(data):
    return hashlib.sha256(data).hexdigest()


def _u32(data, offset):
    if offset < 0 or offset + 4 > len(data):
        raise ValueError("Truncated Save block boundary")
    return struct.unpack_from("<I", data, offset)[0]


def _block(data, offset, *, keep=False):
    size = _u32(data, offset)
    offset += 4
    if size == 0:
        return (b"" if keep else None), offset
    compressed_size = _u32(data, offset)
    offset += 4
    end = offset + compressed_size
    if not compressed_size or end > len(data):
        raise ValueError("Truncated or empty nonzero Save zlib block")
    inflater = zlib.decompressobj()
    pending = data[offset:end]
    count = 0
    decoded = bytearray() if keep else None
    try:
        while True:
            chunk = inflater.decompress(pending, min(65536, size - count + 1))
            count += len(chunk)
            if count > size:
                raise ValueError("Save zlib decoded length exceeds its declaration")
            if keep:
                decoded.extend(chunk)
            pending = inflater.unconsumed_tail
            if inflater.eof or (not pending and not chunk):
                break
        if not inflater.eof or pending or inflater.unused_data or count != size:
            raise ValueError("Save zlib size, full consumption or EOF mismatch")
    except zlib.error as exc:
        raise ValueError("Invalid Save zlib stream/checksum") from exc
    return (bytes(decoded) if keep else None), end


def _layout(data):
    if (_u32(data, 0), _u32(data, 4), _u32(data, 8)) != (100, 863596, HEADER_SIZE):
        raise ValueError("Unknown Save version, magic or first decoded size")
    header, end = _block(data, 8, keep=True)
    offset = end
    # 722150.c:338..668: 2007 blocks INCLUDING the first title header.
    for _ in range(2006):
        _, offset = _block(data, offset)
    groups = _u32(data, offset)  # 722150.c:670 writes one raw DWORD, not a zlib block.
    offset += 4
    # 722150.c:754/816, the 1000-iteration loop, then :945 => 1003 per group.
    if groups > (len(data) - offset) // (1003 * 4):
        raise ValueError("Save group count exceeds remaining block boundaries")
    for _ in range(groups * 1003):
        _, offset = _block(data, offset)
    if offset != len(data):
        raise ValueError("Unexpected bytes after the final Save block")
    return header, end, {"block_count": 2007 + groups * 1003, "group_count": groups}


def _title(raw):
    for index, (chinese, japanese) in enumerate(TITLE_MAPPINGS):
        original, target = chinese.encode("cp936", "strict"), japanese.encode("cp932", "strict")
        if original.decode("cp936", "strict").encode("cp936", "strict") != original:
            raise ValueError("Nonreversible approved CP936 label")
        if target.decode("cp932", "strict").encode("cp932", "strict") != target:
            raise ValueError("Nonreversible approved CP932 label")
        if raw == original:
            return target, chr(65 + index), "mapped"
        if raw == target:
            return target, chr(65 + index), "preserved_cp932"
    raise ValueError("Unknown or unapproved Save title bytes; no translation guessed")


def transform_save(data):
    """Validate the complete native structure; change only the approved title span."""
    header, tail_offset, audit = _layout(data)
    field = header[TITLE_OFFSET:TITLE_OFFSET + TITLE_CAPACITY]
    nul = field.find(b"\0")
    if nul < 0:
        raise ValueError("Save title has no NUL within its 1024-byte capacity")
    original = field[:nul]
    target, mapping_id, action = _title(original)
    span = max(len(original), len(target)) + 1
    if span > TITLE_CAPACITY:
        raise ValueError("Approved Save title exceeds its field capacity")
    if len(target) > len(original) and any(field[len(original) + 1:span]):
        raise ValueError("Save title extension would overwrite nonzero original bytes")
    changed = bytearray(header)
    changed[TITLE_OFFSET:TITLE_OFFSET + span] = target + bytes(span - len(target))
    if action == "preserved_cp932":
        result = data
        changed = header
    else:
        compressed = zlib.compress(changed)
        result = data[:12] + struct.pack("<I", len(compressed)) + compressed + data[tail_offset:]
    actual, new_tail_offset = _block(result, 8, keep=True)
    if (result[:12] != data[:12] or actual != changed or
            actual[:TITLE_OFFSET] != header[:TITLE_OFFSET] or
            actual[TITLE_OFFSET + span:] != header[TITLE_OFFSET + span:] or
            result[new_tail_offset:] != data[tail_offset:]):
        raise ValueError("Save title-only preservation validation failed")
    audit.update(action=action, mapping_id=mapping_id, title_span_bytes=span,
                 header_before_sha256=_sha(header), header_after_sha256=_sha(actual),
                 tail_sha256=_sha(data[tail_offset:]))
    return result, audit


def _no_links(path):
    path = Path(os.path.abspath(path))
    for item in (path, *path.parents):
        if item.is_symlink() or (item.exists() and getattr(item.lstat(), "st_file_attributes", 0) & 0x400):
            raise ValueError("Reparse point/symlink refused in Save seed path")
    return path.resolve()


def _read_tree(source):
    _no_links(source)
    if not source.is_dir():
        raise ValueError("Save input is not a directory")
    files, directories = {}, []
    for current, dirs, names in os.walk(source, followlinks=False):
        for name in sorted(dirs + names):
            path = _no_links(Path(current) / name)
            relative = path.relative_to(source).as_posix()
            if path.is_dir():
                directories.append(relative)
            elif stat.S_ISREG(path.stat().st_mode):
                if path.suffix.casefold() in {".exe", ".pac", ".dll"}:
                    raise ValueError("Game/native payload is not a Save seed input")
                files[relative] = path.read_bytes()
                _no_links(path)
            else:
                raise ValueError("Nonregular file refused in Save seed input")
    return dict(sorted(files.items())), sorted(directories)


def _records(files):
    return {name: {"size": len(data), "sha256": _sha(data)} for name, data in files.items()}


def generate_seed(source_dir, case_name):
    """Validate everything before mkdir; produce one new private seed and audit."""
    if (not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_-]{0,63}", case_name) or
            case_name.upper() in {"CON", "PRN", "AUX", "NUL", *[f"COM{i}" for i in range(10)],
                                  *[f"LPT{i}" for i in range(10)]}):
        raise ValueError("Invalid private seed case name")
    project = _no_links(PROJECT_ROOT)
    source = _no_links(source_dir)
    output = _no_links(project / "target/bhe-private-save-seeds" / case_name)
    if not output.is_relative_to(project / "target/bhe-private-save-seeds"):
        raise ValueError("Private seed output escapes repository target")
    if output == source or output.is_relative_to(source) or source.is_relative_to(output):
        raise ValueError("Save source and private seed output overlap")
    if output.exists():
        raise FileExistsError("Private seed output already exists; nothing overwritten")
    files, directories = _read_tree(source)
    before = _records(files)
    prepared, title_audits = {}, {}
    for name, data in files.items():
        if re.fullmatch(r"Save[0-9]{3}\.dat", Path(name).name, re.I):
            prepared[name], title_audits[name] = transform_save(data)
        else:
            prepared[name] = data
    if not title_audits:
        raise ValueError("No validated numbered Save files found")
    reloaded, current_dirs = _read_tree(source)
    if _records(reloaded) != before or current_dirs != directories:
        raise ValueError("Save source changed during validation; output not created")
    _no_links(output)
    output.mkdir(parents=True, exist_ok=False)
    destination = output / "GIGA/BALDRSKY_DIVEX/Save"
    destination.mkdir(parents=True)
    for directory in directories:
        _no_links(destination / directory).mkdir(parents=True, exist_ok=True)
    for name, data in prepared.items():
        target = _no_links(destination / name)
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("xb") as stream:
            stream.write(data)
    final_source, final_dirs = _read_tree(source)
    after = _records(final_source)
    if after != before or final_dirs != directories:
        raise ValueError("Save source changed during output; unsealed private output retained for review")
    actual, actual_dirs = _read_tree(destination)
    if _records(actual) != _records(prepared) or actual_dirs != directories:
        raise ValueError("Private seed output differs from its validated bytes")
    audit = {"schema": 1, "source": str(source), "seed": str(destination), "file_count": len(files),
             "source_before": before, "source_after": after, "output_files": _records(actual),
             "titles": title_audits, "original_save_unchanged": True,
             "scope": "six approved display labels only; no unlock/progress edits; private seed"}
    audit_path = output / "audit.json"
    with audit_path.open("x", encoding="utf-8", newline="\r\n") as stream:
        stream.write(json.dumps(audit, ensure_ascii=False, indent=2) + "\n")
    return {"seed": str(destination), "audit": str(audit_path), "file_count": len(files),
            "mapped_titles": sum(item["action"] == "mapped" for item in title_audits.values())}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--case", required=True)
    args = parser.parse_args(argv)
    try:
        print(json.dumps(generate_seed(args.source, args.case), ensure_ascii=False))
        return 0
    except (ValueError, OSError) as exc:
        print(json.dumps({"status": "refused", "error": str(exc)}, ensure_ascii=False), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
