"""Synthetic save fixtures only; never generates a seed from the user's Save."""

import sys
sys.dont_write_bytecode = True

import hashlib
import json
from pathlib import Path
import struct
import tempfile
import unittest
from unittest.mock import patch
import zlib

try:
    import bhe_private_save_seed as seed
except ModuleNotFoundError:
    seed = None


APPROVED = (
    ("【设置菜单】", "【設定メニュー】"),
    ("『克里斯』 结束", "『クリス』 終了"),
    ("『终结之日』 结束", "『終結の日』 終了"),
    ("「菜叶不会使用战斗用电子体，亚季姐也讨厌麻烦的事啊……」",
     "「菜ノ葉は戦闘用シュミクラムを使えないし、亜季姉も面倒なことは嫌うからな……」"),
    ("『误解』 结束", "『誤解』 終了"),
    ("那么，要去哪里呢？", "さて、どこへ行こうか？"),
)


def block(payload):
    if not payload:
        return struct.pack("<I", 0)
    compressed = zlib.compress(payload)
    return struct.pack("<II", len(payload), len(compressed)) + compressed


def tail_fixture():
    # 2007 total pre-group blocks includes the first title header: 2006 remain.
    return (block(b"tail witness\0\xff") + block(b"") * 2005 + struct.pack("<I", 1) +
            block(b"group witness\0\xfe") + block(b"") * 1002)


def save_fixture(title=None, *, encoding="cp936", title_bytes=None, tail=None):
    title = APPROVED[0][0] if title is None else title
    raw = title.encode(encoding) if title_bytes is None else title_bytes
    header = bytearray(63336)
    header[:32] = bytes(range(32))
    header[0xF300:0xF300 + len(raw)] = raw
    header[0xF700:0xF710] = bytes(range(16))
    return struct.pack("<II", 100, 863596) + block(header) + (tail_fixture() if tail is None else tail)


def unpack_header(data):
    length = struct.unpack_from("<I", data, 12)[0]
    return zlib.decompress(data[16:16 + length]), data[16 + length:]


class PrivateSaveSeedTests(unittest.TestCase):
    def setUp(self):
        if seed is None:
            self.fail("The strictly bounded private Save seed generator is missing")
        fixtures = Path(__file__).resolve().parents[1] / "target/bhe-private-save-checks/fixtures"
        fixtures.mkdir(parents=True, exist_ok=True)
        self.temp = tempfile.TemporaryDirectory(dir=fixtures)
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.project = self.root / "repo"
        self.project.mkdir()
        self.source = self.root / "source/Save"
        self.source.mkdir(parents=True)
        (self.source / "Save001.dat").write_bytes(save_fixture())
        (self.source / "Script.dat").write_bytes(b"opaque progress, unchanged")
        replacement = patch.object(seed, "PROJECT_ROOT", self.project)
        replacement.start()
        self.addCleanup(replacement.stop)

    def output(self, name="synthetic"):
        return self.project / "target/bhe-private-save-seeds" / name

    def test_all_six_exact_mappings_preserve_prefix_header_outside_span_and_tail(self):
        self.assertEqual(tuple(seed.TITLE_MAPPINGS), APPROVED)
        for number, (chinese, japanese) in enumerate(APPROVED, 1):
            with self.subTest(mapping=number):
                source = save_fixture(chinese)
                result, audit = seed.transform_save(source)
                old, old_tail = unpack_header(source)
                new, new_tail = unpack_header(result)
                span = max(len(chinese.encode("cp936")), len(japanese.encode("cp932"))) + 1
                self.assertEqual(result[:12], source[:12])
                self.assertEqual(new[:0xF300], old[:0xF300])
                self.assertEqual(new[0xF300 + span:], old[0xF300 + span:])
                self.assertEqual(new_tail, old_tail)
                self.assertEqual(new[0xF300:].split(b"\0", 1)[0], japanese.encode("cp932"))
                self.assertEqual(audit["action"], "mapped")
                self.assertEqual(audit["mapping_id"], chr(64 + number))
                self.assertEqual(audit["block_count"], 3010)
                self.assertEqual(audit["group_count"], 1)

    def test_approved_cp932_labels_are_preserved_byte_for_byte(self):
        for _, title in APPROVED:
            with self.subTest(title_id=len(title)):
                original = save_fixture(title, encoding="cp932")
                result, audit = seed.transform_save(original)
                self.assertEqual(result, original)
                self.assertEqual(audit["action"], "preserved_cp932")

    def test_unknown_title_even_if_legal_cp932_is_rejected_before_output_mkdir(self):
        for raw in ("未知标题".encode("cp936"), "未知の日本語".encode("cp932"), b"\x81"):
            (self.source / "Save001.dat").write_bytes(save_fixture(title_bytes=raw))
            with self.subTest(raw_length=len(raw)), self.assertRaises(ValueError):
                seed.generate_seed(self.source, "synthetic")
            self.assertFalse(self.output().exists())
            self.assertFalse((self.project / "target").exists())

    def test_title_capacity_missing_nul_and_nonzero_extension_are_rejected(self):
        original = save_fixture()
        header, tail = unpack_header(original)
        header = bytearray(header)
        header[0xF300:0xF700] = b"A" * 1024
        with self.assertRaises(ValueError):
            seed.transform_save(original[:8] + block(header) + tail)
        header, tail = unpack_header(original)
        header = bytearray(header)
        header[0xF300 + len(APPROVED[0][0].encode("cp936")) + 1] = 1
        with self.assertRaises(ValueError):
            seed.transform_save(original[:8] + block(header) + tail)
        mappings = ((APPROVED[0][0], "あ" * 512),) + APPROVED[1:]
        with patch.object(seed, "TITLE_MAPPINGS", mappings), self.assertRaises(ValueError):
            seed.transform_save(original)

    def test_version_magic_and_first_decoded_size_are_required(self):
        for offset, value in ((0, 101), (4, 1234), (8, 63335)):
            data = bytearray(save_fixture())
            struct.pack_into("<I", data, offset, value)
            with self.subTest(offset=offset), self.assertRaises(ValueError):
                seed.transform_save(bytes(data))

    def test_zlib_requires_exact_consumption_size_checksum_and_eof(self):
        original = save_fixture()
        length = struct.unpack_from("<I", original, 12)[0]
        compressed = original[16:16 + length]
        tail = original[16 + length:]
        bad_checksum = bytearray(compressed)
        bad_checksum[-1] ^= 1
        variants = [compressed[:-1], compressed + b"extra", compressed + zlib.compress(b"other"),
                    bytes(bad_checksum), zlib.compress(b"short"), zlib.compress(b"A" * 63337)]
        for payload in variants:
            data = original[:12] + struct.pack("<I", len(payload)) + payload + tail
            with self.subTest(length=len(payload)), self.assertRaises(ValueError):
                seed.transform_save(data)

    def test_tail_block_boundaries_group_count_and_final_eof_are_validated(self):
        for tail in (tail_fixture()[:-1], tail_fixture() + b"x", block(b"") * 2006 + struct.pack("<I", 2),
                     block(b"") * 2005 + struct.pack("<I", 0)):
            with self.subTest(length=len(tail)), self.assertRaises(ValueError):
                seed.transform_save(save_fixture(tail=tail))
        bad_tail = struct.pack("<II", 100, 3) + b"bad" + block(b"") * 2005 + struct.pack("<I", 0)
        with self.assertRaises(ValueError):
            seed.transform_save(save_fixture(tail=bad_tail))

    def test_generation_copies_only_one_seed_and_a_small_audit_preserving_all_other_files(self):
        (self.source / "nested").mkdir()
        (self.source / "nested/Replay001.dat").write_bytes(b"opaque replay fixture")
        before = {p.relative_to(self.source).as_posix(): p.read_bytes() for p in self.source.rglob("*") if p.is_file()}
        result = seed.generate_seed(self.source, "synthetic")
        destination = self.output() / "GIGA/BALDRSKY_DIVEX/Save"
        self.assertEqual(result["seed"], str(destination))
        self.assertEqual(sorted(p.relative_to(destination).as_posix() for p in destination.rglob("*") if p.is_file()), sorted(before))
        self.assertEqual((destination / "Script.dat").read_bytes(), before["Script.dat"])
        self.assertEqual((destination / "nested/Replay001.dat").read_bytes(), before["nested/Replay001.dat"])
        self.assertEqual({p.relative_to(self.source).as_posix(): p.read_bytes() for p in self.source.rglob("*") if p.is_file()}, before)
        audit_path = self.output() / "audit.json"
        self.assertLess(audit_path.stat().st_size, 10000)
        audit = json.loads(audit_path.read_text(encoding="utf-8"))
        self.assertEqual(audit["source_before"], audit["source_after"])
        self.assertEqual(audit["file_count"], 3)
        raw = audit_path.read_bytes()
        self.assertFalse(raw.startswith(b"\xef\xbb\xbf"))
        self.assertNotIn(b"\n", raw.replace(b"\r\n", b""))

    def test_multiple_file_source_drift_is_rejected_before_output_mkdir(self):
        transform = seed.transform_save
        def drift(data):
            result = transform(data)
            (self.source / "Script.dat").write_bytes(b"external source drift")
            return result
        with patch.object(seed, "transform_save", side_effect=drift), self.assertRaisesRegex(ValueError, "changed"):
            seed.generate_seed(self.source, "synthetic")
        self.assertFalse(self.output().exists())
        self.assertFalse((self.project / "target").exists())

    def test_invalid_case_names_existing_output_and_source_overlap_are_refused(self):
        for name in ("../escape", "C:\\outside", "a/b", "a:b", "", "NUL", ".", ".."):
            with self.subTest(name=name), self.assertRaises(ValueError):
                seed.generate_seed(self.source, name)
        self.output().mkdir(parents=True)
        (self.output() / "sentinel.txt").write_text("preserve", encoding="utf-8")
        with self.assertRaises(FileExistsError):
            seed.generate_seed(self.source, "synthetic")
        self.assertEqual((self.output() / "sentinel.txt").read_text(), "preserve")
        with self.assertRaises(ValueError):
            seed.generate_seed(self.project, "overlap")

    def test_reparse_input_and_output_ancestor_are_rejected(self):
        link = self.source / "linked.dat"
        try:
            link.symlink_to(self.source / "Script.dat")
        except OSError:
            self.fail("This Windows test account must support the required symlink rejection fixture")
        with self.assertRaises(ValueError):
            seed.generate_seed(self.source, "synthetic")
        link.unlink()
        external = self.root / "external"
        external.mkdir()
        self.project.joinpath("target").symlink_to(external, target_is_directory=True)
        with self.assertRaises(ValueError):
            seed.generate_seed(self.source, "synthetic")
        self.project.joinpath("target").unlink()
        self.assertEqual(list(external.iterdir()), [])

    def test_game_or_pac_files_are_never_copied_from_a_mislabeled_save_directory(self):
        for filename in ("BaldrSky.exe", "Update3.pac", "native.dll"):
            path = self.source / filename
            path.write_bytes(b"synthetic forbidden payload")
            with self.subTest(filename=filename), self.assertRaises(ValueError):
                seed.generate_seed(self.source, "synthetic")
            self.assertFalse(self.output().exists())
            path.unlink()


if __name__ == "__main__":
    unittest.main()
