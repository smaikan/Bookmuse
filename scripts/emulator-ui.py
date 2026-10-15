"""ADB accessibility driver for the dedicated Kitap emulator. Does not log PDF text.

Use interactively from Python or compose a scenario. Hierarchies/screenshots stay in
the private toolchain directory; public reports should contain assertions only.
This is functional evidence, never a Huawei performance benchmark.
"""
import hashlib
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path


class EmulatorUI:
    def __init__(self, serial="emulator-5554"):
        if not serial.startswith("emulator-"):
            raise ValueError("This test driver only operates on a dedicated emulator")
        self.serial = serial
        self.private = Path.home() / ".codex/toolchains/kitap-android"
        self.adb = self.private / "sdk/platform-tools/adb.exe"

    def run(self, *args, timeout=45):
        result = subprocess.run([str(self.adb), "-s", self.serial, *args], capture_output=True, timeout=timeout)
        if result.returncode:
            raise RuntimeError(f"ADB {args[0]} failed: {result.stderr.decode('utf8', 'replace')[:200]}")
        return result.stdout.decode("utf8", "replace")

    def root(self):
        result = "No fresh hierarchy returned"
        for attempt in range(3):
            try:
                self.run("shell", "rm", "-f", "/data/local/tmp/kitap-ui.xml")
                result = self.run("shell", "uiautomator", "dump", "/data/local/tmp/kitap-ui.xml")
                if "ERROR" not in result and "dumped to" in result:
                    return ET.fromstring(self.run("shell", "cat", "/data/local/tmp/kitap-ui.xml"))
            except (RuntimeError, subprocess.TimeoutExpired, ET.ParseError) as issue:
                result = str(issue)
            time.sleep(.5)
        raise RuntimeError("Android accessibility snapshot was not idle; no stale hierarchy was used: " + result[:150])

    def find(self, label, contains=False, root=None):
        root = self.root() if root is None else root
        for node in root.iter("node"):
            for value in (node.get("content-desc", ""), node.get("text", "")):
                if (label in value if contains else label == value):
                    return node
        return None

    def click(self, label, contains=False, timeout=20):
        start = time.monotonic()
        while time.monotonic() - start < timeout:
            root = self.root()
            parents = {child: parent for parent in root.iter() for child in parent}
            for candidate in root.iter("node"):
                matched = any(label in v if contains else label == v for v in (candidate.get("content-desc", ""), candidate.get("text", "")))
                if not matched:
                    continue
                node = candidate
                while node is not None and node.get("clickable") != "true":
                    node = parents.get(node)
                if node is None:
                    continue  # A recent-book title is text, not the book's open control.
                coords = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
                if len(coords) == 4 and coords[2] > coords[0] and coords[3] > coords[1]:
                    x, y = str((coords[0] + coords[2]) // 2), str((coords[1] + coords[3]) // 2)
                    self.run("shell", "input", "swipe", x, y, x, y, "160")
                    return
            time.sleep(.4)
        raise AssertionError(f"Accessible control not found: {label}")

    def wait(self, label, contains=False, timeout=25):
        start = time.monotonic()
        while time.monotonic() - start < timeout:
            node = self.find(label, contains)
            if node is not None:
                return node
            time.sleep(.4)
        raise AssertionError(f"Expected state not found: {label}")

    def screenshot(self, name="emulator-screen.png"):
        self.run("shell", "screencap", "-p", "/data/local/tmp/kitap-screen.png")
        self.run("pull", "/data/local/tmp/kitap-screen.png", str(self.private / name))
        return self.private / name

    def summaries(self):
        # Only call for non-personal app controls; do not publish raw selected PDF text.
        return [(n.get("text"), n.get("content-desc"), n.get("bounds")) for n in self.root().iter("node") if n.get("text") or n.get("content-desc")]

    def push_downloads(self):
        self.run("shell", "mkdir", "-p", "/sdcard/Download/kitap-tests")
        items = []
        for i, pdf in enumerate(sorted((Path.home() / "Downloads").glob("*.pdf")), 1):
            name = f"DL-{i:02}.pdf"
            self.run("push", str(pdf), f"/sdcard/Download/kitap-tests/{name}")
            items.append(dict(id=f"DL-{i:02}", sha256=hashlib.sha256(pdf.read_bytes()).hexdigest(), bytes=pdf.stat().st_size))
        return items

    def view_pdf(self, name):
        uri = f"content://com.android.externalstorage.documents/document/primary%3ADownload%2Fkitap-tests%2F{name}.pdf"
        return self.run("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", uri, "-t", "application/pdf", "-f", "1", "-n", "com.kitap/.MainActivity")


if __name__ == "__main__":
    ui = EmulatorUI()
    print(json.dumps({"driverReady": True, "bootCompleted": ui.run("shell", "getprop", "sys.boot_completed").strip() == "1"}))
