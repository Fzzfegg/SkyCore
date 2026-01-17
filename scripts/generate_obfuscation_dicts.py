#!/usr/bin/env python3
"""
Utility to (re)generate the Allatori custom naming dictionaries with safer defaults.

The script lets you control how many identifiers each dictionary should contain and
the allowed length range per category. Identifiers are constructed from mixed
alphabets to avoid the repetitive 0/O/l patterns that are easy to spot. Optional
Unicode pools can be layered in to further diversify the generated symbols.
"""

from __future__ import annotations

import argparse
import pathlib
import random
import string
from dataclasses import dataclass


ASCII_LOWER = string.ascii_lowercase
ASCII_UPPER = string.ascii_uppercase
ASCII_ALPHA = string.ascii_letters
ASCII_DIGITS = string.digits
UNICODE_POOLS = {
    # Only includes characters valid as Java identifier starts.
    "greek": "αβγδεζηθικλμνξοπρστυφχψωΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩ",
    "cyrillic": "абвгдеёжзийклмнопрстуфхцчшщъыьэюяАБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ",
    "hiragana": "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをん",
    "katakana": "アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン",
    "bopomofo": "ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎㄏㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙㄚㄛㄜㄝㄞㄟㄠㄡㄢㄣㄤㄥㄦ",
}
UNICODE_POOL_NAMES = ", ".join(sorted(UNICODE_POOLS))


@dataclass(frozen=True)
class DictSpec:
    filename: str
    count: int
    length_span: tuple[int, int]
    first_chars: str
    body_chars: str


def merge_charset(base: str, extra: str) -> str:
    """Merge characters, preserving order and removing duplicates."""
    seen = set(base)
    merged = [*base]
    for char in extra:
        if char not in seen:
            seen.add(char)
            merged.append(char)
    return "".join(merged)


def compose_charset(
    base: str, unicode_sets: tuple[str, ...], lowercase_only: bool
) -> str:
    if not unicode_sets:
        return base
    extra = "".join(UNICODE_POOLS[name] for name in unicode_sets)
    if lowercase_only:
        extra = extra.lower()
    return merge_charset(base, extra)


def parse_span(expr: str | None, fallback: tuple[int, int], label: str) -> tuple[int, int]:
    if not expr:
        return fallback
    try:
        lo_str, hi_str = expr.split("-", 1)
        lo, hi = int(lo_str), int(hi_str)
    except (ValueError, TypeError) as exc:
        raise argparse.ArgumentTypeError(f"{label} 必须是 'min-max' 格式") from exc
    if lo < 1 or hi < lo:
        raise argparse.ArgumentTypeError(f"{label} 范围非法: {expr}")
    return lo, hi


def parse_unicode_sets(expr: str | None, label: str) -> tuple[str, ...]:
    if not expr:
        return ()
    names: list[str] = []
    for raw in expr.split(","):
        name = raw.strip().lower()
        if not name:
            continue
        if name not in UNICODE_POOLS:
            raise argparse.ArgumentTypeError(
                f"{label} 包含未知集合 '{name}'，可选项: {UNICODE_POOL_NAMES}"
            )
        if name not in names:
            names.append(name)
    return tuple(names)


def generate_identifiers(rng: random.Random, spec: DictSpec) -> list[str]:
    values: list[str] = []
    seen: set[str] = set()
    min_len, max_len = spec.length_span
    while len(values) < spec.count:
        length = rng.randint(min_len, max_len)
        first = rng.choice(spec.first_chars)
        body = "".join(rng.choice(spec.body_chars) for _ in range(length - 1))
        candidate = first + body
        if candidate in seen:
            continue
        seen.add(candidate)
        values.append(candidate)
    return values


def write_dictionary(path: pathlib.Path, values: list[str], dry_run: bool) -> None:
    if dry_run:
        preview = ", ".join(values[:5])
        print(f"[dry-run] {path.name}: {len(values)} entries (预览: {preview})")
        return
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(values) + "\n", encoding="utf-8")
    print(f"[done] {path} <- {len(values)} entries")


def build_specs(args: argparse.Namespace) -> list[DictSpec]:
    global_unicode = parse_unicode_sets(args.unicode_pools, "unicode-pools")
    packages_unicode = global_unicode + parse_unicode_sets(
        args.unicode_packages, "unicode-packages"
    )
    classes_unicode = global_unicode + parse_unicode_sets(
        args.unicode_classes, "unicode-classes"
    )
    methods_unicode = global_unicode + parse_unicode_sets(
        args.unicode_methods, "unicode-methods"
    )
    fields_unicode = global_unicode + parse_unicode_sets(
        args.unicode_fields, "unicode-fields"
    )
    return [
        DictSpec(
            filename="custom-packages.txt",
            count=args.packages_count,
            length_span=parse_span(args.packages_span, (6, 18), "packages-span"),
            first_chars=compose_charset(ASCII_LOWER, packages_unicode, True),
            body_chars=compose_charset(
                ASCII_LOWER + ASCII_DIGITS, packages_unicode, True
            ),
        ),
        DictSpec(
            filename="custom-classes.txt",
            count=args.classes_count,
            length_span=parse_span(args.classes_span, (6, 24), "classes-span"),
            first_chars=compose_charset(ASCII_UPPER, classes_unicode, False),
            body_chars=compose_charset(ASCII_ALPHA + ASCII_DIGITS, classes_unicode, False),
        ),
        DictSpec(
            filename="custom-methods.txt",
            count=args.methods_count,
            length_span=parse_span(args.methods_span, (8, 28), "methods-span"),
            first_chars=compose_charset(ASCII_LOWER + "_", methods_unicode, False),
            body_chars=compose_charset(
                ASCII_ALPHA + ASCII_DIGITS + "_", methods_unicode, False
            ),
        ),
        DictSpec(
            filename="custom-fields.txt",
            count=args.fields_count,
            length_span=parse_span(args.fields_span, (8, 28), "fields-span"),
            first_chars=compose_charset(ASCII_LOWER + "_", fields_unicode, False),
            body_chars=compose_charset(
                ASCII_ALPHA + ASCII_DIGITS + "_", fields_unicode, False
            ),
        ),
    ]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="生成 Allatori 自定义命名字典")
    parser.add_argument(
        "--output-dir",
        default="config",
        help="输出目录（默认: config）",
    )
    parser.add_argument("--packages-count", type=int, default=512)
    parser.add_argument("--classes-count", type=int, default=512)
    parser.add_argument("--methods-count", type=int, default=1024)
    parser.add_argument("--fields-count", type=int, default=1024)
    unicode_help = (
        "可混入的 Unicode 集（逗号分隔，可选: " + UNICODE_POOL_NAMES + "）"
    )
    parser.add_argument(
        "--unicode-pools",
        help="全局追加的 Unicode 集。" + unicode_help,
    )
    parser.add_argument(
        "--unicode-packages",
        help="仅包名追加的 Unicode 集；留空表示只用全局设置。" + unicode_help,
    )
    parser.add_argument(
        "--unicode-classes",
        help="仅类名追加的 Unicode 集；留空表示只用全局设置。" + unicode_help,
    )
    parser.add_argument(
        "--unicode-methods",
        help="仅方法名追加的 Unicode 集；留空表示只用全局设置。" + unicode_help,
    )
    parser.add_argument(
        "--unicode-fields",
        help="仅字段名追加的 Unicode 集；留空表示只用全局设置。" + unicode_help,
    )
    parser.add_argument(
        "--packages-span",
        help="包名长度范围（格式 min-max，默认 6-18）",
    )
    parser.add_argument(
        "--classes-span",
        help="类名长度范围（格式 min-max，默认 6-24）",
    )
    parser.add_argument(
        "--methods-span",
        help="方法名长度范围（格式 min-max，默认 8-28）",
    )
    parser.add_argument(
        "--fields-span",
        help="字段名长度范围（格式 min-max，默认 8-28）",
    )
    parser.add_argument(
        "--seed",
        type=int,
        help="可选随机数种子（指定后生成结果可复现）",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="只打印预览，不写入文件",
    )
    parser.add_argument(
        "--list-unicode-pools",
        action="store_true",
        help="列出脚本内置的 Unicode 集示例后退出",
    )
    return parser.parse_args()


def list_unicode_sets() -> None:
    print("可用 Unicode 集：")
    for name in sorted(UNICODE_POOLS):
        chars = UNICODE_POOLS[name]
        sample = "".join(chars[:12])
        print(f"- {name}: {sample}...")


def main() -> None:
    args = parse_args()
    if args.list_unicode_pools:
        list_unicode_sets()
        return
    rng: random.Random
    if args.seed is None:
        rng = random.SystemRandom()
    else:
        rng = random.Random(args.seed)
    output_dir = pathlib.Path(args.output_dir)
    specs = build_specs(args)
    for spec in specs:
        values = generate_identifiers(rng, spec)
        write_dictionary(output_dir / spec.filename, values, args.dry_run)


if __name__ == "__main__":
    main()
