#!/usr/bin/env python3
"""Check that the standalone arm64 build produced packaged Android client APKs."""

import argparse
from pathlib import Path
import zipfile


def verify_apk(path):
    if path.suffix != ".apk" or not path.is_file():
        raise ValueError(f"APK not found: {path}")
    required = {
        "AndroidManifest.xml",
        "classes.dex",
        "lib/arm64-v8a/libtmessages.49.so",
    }
    with zipfile.ZipFile(path) as apk:
        for name in required:
            if name not in apk.namelist() or apk.getinfo(name).file_size == 0:
                raise ValueError(f"{path.name}: missing or empty {name}")
        corrupt = apk.testzip()
        if corrupt is not None:
            raise ValueError(f"{path.name}: damaged ZIP entry {corrupt}")
    print(f"Verified {path.name}: manifest, DEX, and arm64 Telegram native library")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path, nargs="+")
    arguments = parser.parse_args()
    for path in arguments.apk:
        verify_apk(path)
