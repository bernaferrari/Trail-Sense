#!/usr/bin/env python3
"""Generate the service worker's asset list for a built Trail Sense web distribution."""
import hashlib
import json
import sys
from pathlib import Path


def package(distribution: Path) -> None:
    if not (distribution / "index.html").is_file() or not (distribution / "shared.js").is_file():
        raise SystemExit(f"Build the browser distribution before packaging {distribution}")
    assets = []
    build = hashlib.sha256()
    for file in sorted(distribution.rglob("*")):
        if not file.is_file() or file.name == "offline-assets.js" or file.suffix == ".map":
            continue
        path = file.relative_to(distribution).as_posix()
        assets.append(path)
        build.update(path.encode("utf-8"))
        build.update(hashlib.sha256(file.read_bytes()).digest())
    (distribution / "offline-assets.js").write_text(
        f"self.TRAIL_SENSE_BUILD = {json.dumps(build.hexdigest())};\n"
        f"self.TRAIL_SENSE_ASSETS = {json.dumps(assets)};\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    package(Path(sys.argv[1]))
