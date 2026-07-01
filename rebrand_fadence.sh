#!/usr/bin/env bash
set -Eeuo pipefail

APP_DISPLAY_NAME="Fadence Music"
PROJECT_NAME="FadenceMusic"

OLD_PACKAGE="com.mardous.booming"
NEW_PACKAGE="com.fadly.fadence"

OLD_PACKAGE_PATH="com/mardous/booming"
NEW_PACKAGE_PATH="com/fadly/fadence"

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

echo
echo "=========================================="
echo " BoomingMusic → Fadence Music"
echo "=========================================="
echo

[[ -f settings.gradle.kts ]] || \
  fail "settings.gradle.kts tidak ditemukan. Pastikan posisi sudah di root repository."

[[ -f app/build.gradle.kts ]] || \
  fail "app/build.gradle.kts tidak ditemukan."

[[ -d app/src ]] || \
  fail "Folder app/src tidak ditemukan."

command -v git >/dev/null 2>&1 || \
  fail "Git belum terpasang."

command -v python >/dev/null 2>&1 || \
  fail "Python belum terpasang. Jalankan: pkg install python"

export APP_DISPLAY_NAME
export PROJECT_NAME
export OLD_PACKAGE
export NEW_PACKAGE

python <<'PY'
from __future__ import annotations

import os
import re
from pathlib import Path

root = Path.cwd()

app_name = os.environ["APP_DISPLAY_NAME"]
project_name = os.environ["PROJECT_NAME"]
old_package = os.environ["OLD_PACKAGE"]
new_package = os.environ["NEW_PACKAGE"]

changed = set()


def replace_text(path: Path, replacements: list[tuple[str, str]]) -> None:
    if not path.exists() or not path.is_file():
        return

    try:
        original = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return

    updated = original

    for old, new in replacements:
        updated = updated.replace(old, new)

    if updated != original:
        path.write_text(updated, encoding="utf-8")
        changed.add(path)


# 1. Nama project Gradle
settings_file = root / "settings.gradle.kts"
settings_text = settings_file.read_text(encoding="utf-8")

updated_settings, count = re.subn(
    r'rootProject\.name\s*=\s*"[^"]+"',
    f'rootProject.name = "{project_name}"',
    settings_text,
    count=1,
)

if count != 1:
    raise SystemExit(
        "rootProject.name tidak ditemukan atau formatnya tidak dikenali."
    )

if updated_settings != settings_text:
    settings_file.write_text(updated_settings, encoding="utf-8")
    changed.add(settings_file)


# 2. Namespace dan nama APK
build_file = root / "app" / "build.gradle.kts"
build_text = build_file.read_text(encoding="utf-8")

updated_build = build_text.replace(
    f'namespace = "{old_package}"',
    f'namespace = "{new_package}"',
)

updated_build = updated_build.replace(
    f'applicationId = "{old_package}"',
    f'applicationId = "{new_package}"',
)

updated_build = updated_build.replace(
    "BoomingMusic-${output.versionName.get()}",
    "FadenceMusic-${output.versionName.get()}",
)

if updated_build != build_text:
    build_file.write_text(updated_build, encoding="utf-8")
    changed.add(build_file)


# 3. Package, theme, dan nama aplikasi
allowed_suffixes = {
    ".kt",
    ".kts",
    ".java",
    ".xml",
    ".pro",
    ".properties",
    ".json",
    ".toml",
    ".md",
}

excluded_parts = {
    ".git",
    ".gradle",
    "build",
    ".idea",
}

for path in root.rglob("*"):
    if not path.is_file():
        continue

    if path.suffix.lower() not in allowed_suffixes:
        continue

    if excluded_parts.intersection(path.parts):
        continue

    replace_text(
        path,
        [
            (old_package, new_package),
            ("Base.Theme.Booming", "Base.Theme.Fadence"),
            ("Theme.Booming", "Theme.Fadence"),
            ("Widget.Booming", "Widget.Fadence"),
            ("Booming Music", app_name),
            ("BoomingMusic", project_name),
        ],
    )


# 4. Pastikan app_name menjadi Fadence Music
app_name_pattern = re.compile(
    r'(<string\b[^>]*\bname=["\']app_name["\'][^>]*>)'
    r'(.*?)'
    r'(</string>)',
    flags=re.DOTALL,
)

for path in (root / "app" / "src").rglob("*.xml"):
    if "res" not in path.parts:
        continue

    try:
        original = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        continue

    updated, count = app_name_pattern.subn(
        lambda match: (
            f"{match.group(1)}"
            f"{app_name}"
            f"{match.group(3)}"
        ),
        original,
    )

    if count and updated != original:
        path.write_text(updated, encoding="utf-8")
        changed.add(path)


print(f"Berhasil mengubah {len(changed)} file.")

for path in sorted(changed):
    print(f"  modified: {path.relative_to(root)}")
PY

echo
echo "Memindahkan folder package..."

mapfile -t PACKAGE_DIRS < <(
  find app/src \
    -type d \
    -path "*/${OLD_PACKAGE_PATH}" \
    -print |
    sort
)

if [[ "${#PACKAGE_DIRS[@]}" -eq 0 ]]; then
  echo "Folder package lama tidak ditemukan atau sudah dipindahkan."
else
  for old_dir in "${PACKAGE_DIRS[@]}"; do
    source_root="${old_dir%/${OLD_PACKAGE_PATH}}"
    new_dir="${source_root}/${NEW_PACKAGE_PATH}"

    if [[ -e "$new_dir" ]]; then
      echo "Folder tujuan sudah ada, menggabungkan file:"
      echo "  $new_dir"

      mkdir -p "$new_dir"

      cp -a "$old_dir"/. "$new_dir"/
      rm -rf "$old_dir"
    else
      mkdir -p "$(dirname "$new_dir")"
      mv "$old_dir" "$new_dir"
    fi

    echo "  $old_dir"
    echo "  → $new_dir"
  done

  find app/src -type d -empty -delete 2>/dev/null || true
fi

echo
echo "Memeriksa kesalahan perubahan..."

git diff --check

echo
echo "Sisa identitas lama:"
echo

git grep -n -I -E \
  'Booming Music|BoomingMusic|Theme\.Booming|Base\.Theme\.Booming|Widget\.Booming|com\.mardous\.booming' \
  -- \
  ':!LICENSE.txt' \
  ':!rebrand_fadence.sh' \
  || true

echo
echo "=========================================="
echo "Rebranding selesai"
echo "=========================================="
echo
echo "Periksa:"
echo "  git status"
echo "  git diff --stat"
echo "  git diff"
echo
echo "Lalu push:"
echo "  git add -A"
echo '  git commit -m "Rebrand app to Fadence Music"'
echo "  git push origin HEAD"
