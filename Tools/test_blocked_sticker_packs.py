#!/usr/bin/env python3
"""Run the account block-list and reveal-state regressions without Android/Gradle."""

from pathlib import Path
import subprocess
import tempfile


ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "tw/nekomimi/nekogram/helpers"
MAIN = ROOT / "TMessagesProj/src/main/java" / PACKAGE
TEST = ROOT / "TMessagesProj/src/test/java" / PACKAGE

with tempfile.TemporaryDirectory(prefix="blocked-sticker-tests-") as output:
    # The compiler module is also available in JDK installations without a javac launcher.
    subprocess.run([
        "java", "com.sun.tools.javac.Main", "-encoding", "UTF-8", "-d", output,
        str(MAIN / "BlockedStickerPacks.java"),
        str(MAIN / "BlockedStickerRevealState.java"),
        str(MAIN / "BlockedStickerDrawingScope.java"),
        str(TEST / "BlockedStickerPacksTest.java"),
    ], cwd=ROOT, check=True)
    subprocess.run([
        "java", "-cp", output, "tw.nekomimi.nekogram.helpers.BlockedStickerPacksTest",
    ], cwd=ROOT, check=True)
