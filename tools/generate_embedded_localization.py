#!/usr/bin/env python3
"""既存の言語YAMLを、実行時解析を必要としないKotlinカタログへ変換します。

このスクリプトは移行作業中だけ使用します。生成物を唯一の正データへ切り替えた後は、
YAMLとともに削除し、以後の文言は生成済みKotlinカタログを直接編集します。
"""

from __future__ import annotations

import re
import shutil
from pathlib import Path

import yaml


ROOT = Path(__file__).resolve().parents[1]
LANG_ROOT = ROOT / "src/main/resources/lang"
OUTPUT_ROOT = ROOT / "src/main/kotlin/com/awabi2048/ccsystem/core/localization/generated"
CHUNK_SIZE = 120


def kotlin_string(value: str) -> str:
    escaped = (
        value.replace("\\", "\\\\")
        .replace('"', '\\"')
        .replace("$", "\\$")
        .replace("\r", "\\r")
        .replace("\n", "\\n")
        .replace("\t", "\\t")
    )
    return f'"{escaped}"'


def identifier(relative: Path) -> str:
    words = re.split(r"[^A-Za-z0-9]+", relative.with_suffix("").as_posix())
    return "".join(word[:1].upper() + word[1:] for word in words if word)


def flatten(value: object, prefix: str = "") -> list[tuple[str, object]]:
    result: list[tuple[str, object]] = []
    if isinstance(value, dict):
        for raw_key, child in value.items():
            key = str(raw_key)
            path = f"{prefix}.{key}" if prefix else key
            result.extend(flatten(child, path))
    elif isinstance(value, list):
        if not all(isinstance(item, str) for item in value):
            raise ValueError(f"文字列以外を含むリストです: {prefix}")
        result.append((prefix, value))
    elif value is None or value == "":
        # 値なしのYAMLノードはセクションの残骸であり、取得可能な文言ではありません。
        return result
    elif isinstance(value, str):
        result.append((prefix, value))
    else:
        raise ValueError(f"サポート外の値型です: {prefix} ({type(value).__name__})")
    return result


def render_catalog(locale: str, relative: Path, entries: list[tuple[str, object]]) -> str:
    object_name = f"{locale.title().replace('_', '')}{identifier(relative)}Catalog"
    chunks = [entries[index:index + CHUNK_SIZE] for index in range(0, len(entries), CHUNK_SIZE)]
    lines = [
        "package com.awabi2048.ccsystem.core.localization.generated",
        "",
        "import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizedValue",
        "import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry",
        "",
        "/** YAML資産から機械変換した不変カタログです。実行時に外部ファイルを参照しません。 */",
        f"internal object {object_name} {{",
        f"    const val LOCALE: String = {kotlin_string(locale)}",
        f"    const val DOMAIN: String = {kotlin_string(relative.with_suffix('').as_posix())}",
        "",
        "    fun entries(): List<EmbeddedLocalizationEntry> = buildList {",
    ]
    for index in range(len(chunks)):
        lines.append(f"        addAll(chunk{index + 1}())")
    lines += ["    }", ""]
    for index, chunk in enumerate(chunks, start=1):
        lines.append(f"    private fun chunk{index}(): List<EmbeddedLocalizationEntry> = listOf(")
        for key, value in chunk:
            if isinstance(value, list):
                values = ", ".join(kotlin_string(item) for item in value)
                rendered = f"EmbeddedLocalizedValue.TextList(listOf({values}))"
            else:
                rendered = f"EmbeddedLocalizedValue.Text({kotlin_string(value)})"
            lines.append(
                "        EmbeddedLocalizationEntry("
                f"key = {kotlin_string(key)}, value = {rendered}, domain = DOMAIN),"
            )
        lines += ["    )", ""]
    lines += ["}", ""]
    return "\n".join(lines)


def render_keys(relative: Path, entries: list[tuple[str, object]]) -> str:
    """基準localeから、値型を型引数として持つキー定義を領域ごとに生成します。"""
    object_name = f"{identifier(relative)}Keys"
    lines = [
        "package com.awabi2048.ccsystem.api.localization.generated",
        "",
        "import com.awabi2048.ccsystem.api.localization.LocalizationKey",
        "",
        "/** コンパイル時に値型を保証する、領域別の生成済みローカライズキーです。 */",
        f"object {object_name} {{",
    ]
    used: set[str] = set()
    names: list[str] = []
    for key, value in entries:
        base = re.sub(r"[^A-Za-z0-9_]", "_", key).upper()
        if base[:1].isdigit():
            base = f"KEY_{base}"
        name = base
        suffix = 2
        while name in used:
            name = f"{base}_{suffix}"
            suffix += 1
        used.add(name)
        names.append(name)
        type_name = "List<String>" if isinstance(value, list) else "String"
        factory = "textList" if isinstance(value, list) else "text"
        lines.append(
            f"    @JvmField val {name}: LocalizationKey<{type_name}> = "
            f"LocalizationKey.{factory}({kotlin_string(key)})"
        )
    lines += ["", "    internal fun all(): List<LocalizationKey<*>> = listOf("]
    lines.extend(f"        {name}," for name in names)
    lines += ["    )", "}", ""]
    return "\n".join(lines)


def main() -> None:
    if OUTPUT_ROOT.exists():
        shutil.rmtree(OUTPUT_ROOT)
    OUTPUT_ROOT.mkdir(parents=True)

    catalog_objects: list[tuple[str, str]] = []
    key_objects: list[str] = []
    key_output_root = ROOT / "src/main/kotlin/com/awabi2048/ccsystem/api/localization/generated"
    if key_output_root.exists():
        shutil.rmtree(key_output_root)
    key_output_root.mkdir(parents=True)
    for locale_dir in sorted(path for path in LANG_ROOT.iterdir() if path.is_dir()):
        locale = locale_dir.name.lower()
        for source in sorted(locale_dir.rglob("*.yml")):
            relative = source.relative_to(locale_dir)
            # BaseLoaderでON/OFF等を真偽値へ暗黙変換せず、表示文字列として保持します。
            data = yaml.load(source.read_text(encoding="utf-8"), Loader=yaml.BaseLoader)
            entries = flatten(data)
            object_name = f"{locale.title().replace('_', '')}{identifier(relative)}Catalog"
            target = OUTPUT_ROOT / locale / relative.with_suffix(".kt")
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(render_catalog(locale, relative, entries), encoding="utf-8", newline="\n")
            catalog_objects.append((locale, object_name))
            if locale == "ja_jp":
                key_target = key_output_root / relative.with_suffix(".kt")
                key_target.parent.mkdir(parents=True, exist_ok=True)
                key_target.write_text(render_keys(relative, entries), encoding="utf-8", newline="\n")
                key_objects.append(f"{identifier(relative)}Keys")

    registry_lines = [
        "package com.awabi2048.ccsystem.core.localization.generated",
        "",
        "import com.awabi2048.ccsystem.core.localization.EmbeddedLocalizationEntry",
        "",
        "/** 全領域の生成済みカタログをlocale単位で合成する唯一の索引です。 */",
        "internal object GeneratedLocalizationCatalogIndex {",
        "    fun entriesByLocale(): Map<String, List<EmbeddedLocalizationEntry>> = mapOf(",
    ]
    for locale in sorted({item[0] for item in catalog_objects}):
        objects = [name for item_locale, name in catalog_objects if item_locale == locale]
        registry_lines.append(f"        {kotlin_string(locale)} to buildList {{")
        for object_name in objects:
            registry_lines.append(f"            addAll({object_name}.entries())")
        registry_lines.append("        },")
    registry_lines += ["    )", "}", ""]
    (OUTPUT_ROOT / "GeneratedLocalizationCatalogIndex.kt").write_text(
        "\n".join(registry_lines), encoding="utf-8", newline="\n"
    )

    key_index = [
        "package com.awabi2048.ccsystem.api.localization.generated",
        "",
        "import com.awabi2048.ccsystem.api.localization.LocalizationKey",
        "",
        "/** 生成済みキーとカタログ値型の完全対応をビルド時に検証する索引です。 */",
        "internal object GeneratedLocalizationKeyIndex {",
        "    fun all(): List<LocalizationKey<*>> = buildList {",
    ]
    for object_name in key_objects:
        key_index.append(f"        addAll({object_name}.all())")
    key_index += ["    }", "}", ""]
    (key_output_root / "GeneratedLocalizationKeyIndex.kt").write_text(
        "\n".join(key_index), encoding="utf-8", newline="\n"
    )

    print(f"generated {len(catalog_objects)} catalogs into {OUTPUT_ROOT}")


if __name__ == "__main__":
    main()
