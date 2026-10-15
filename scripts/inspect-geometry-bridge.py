"""Check actual shrunk DEX definitions and native JNI export, not just source keep rules.

Static ABI evidence only: does not execute PDFium or replace a device release smoke test.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import struct
import subprocess
import tempfile
from zipfile import ZipFile


def uleb(data, offset):
    value = 0
    for shift in range(0, 35, 7):
        byte = data[offset]
        offset += 1
        value |= (byte & 127) << shift
        if byte < 128:
            return value, offset
    raise ValueError("Malformed DEX uleb")


def definitions(data):
    if data[:4] != b"dex\n":
        raise ValueError("Expected standard DEX")
    def word(offset):
        return struct.unpack_from("<I", data, offset)[0]
    strings = []
    for i in range(word(0x38)):
        _, start = uleb(data, word(word(0x3C) + i * 4))
        strings.append(data[start:data.index(0, start)].decode("utf-8", errors="replace"))
    types = [strings[word(word(0x44) + i * 4)] for i in range(word(0x40))]
    prototypes = []
    for i in range(word(0x48)):
        offset = word(0x4C) + i * 12
        params = word(offset + 8)
        arguments = "" if not params else "".join(types[struct.unpack_from("<H", data, params + 4 + j * 2)[0]] for j in range(word(params)))
        prototypes.append(f"({arguments}){types[word(offset + 4)]}")
    methods = []
    for i in range(word(0x58)):
        owner, proto, name = struct.unpack_from("<HHI", data, word(0x5C) + i * 8)
        methods.append((types[owner], strings[name], prototypes[proto]))
    result = {}
    for i in range(word(0x60)):
        start = word(word(0x64) + i * 32 + 24)
        if not start:
            continue
        counts = []
        for _ in range(4):
            number, start = uleb(data, start)
            counts.append(number)
        for _ in range((counts[0] + counts[1]) * 2):
            _, start = uleb(data, start)
        for count in counts[2:]:
            index = 0
            for _ in range(count):
                delta, start = uleb(data, start)
                access, start = uleb(data, start)
                code, start = uleb(data, start)
                index += delta
                result[methods[index]] = {"public": bool(access & 1), "native": bool(access & 0x100), "hasCode": code != 0}
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("--llvm-nm", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    required = [
        ("Lcom/kitap/pdf/PdfGeometry;", "glyphs", "(Lio/legere/pdfiumandroid/PdfPage;Lio/legere/pdfiumandroid/PdfTextPage;I)[F"),
        ("Lio/legere/pdfiumandroid/PdfPage;", "getPage$pdfiumandroid", "()Lio/legere/pdfiumandroid/core/unlocked/PdfPageU;"),
        ("Lio/legere/pdfiumandroid/PdfTextPage;", "getPage$pdfiumandroid", "()Lio/legere/pdfiumandroid/core/unlocked/PdfTextPageU;"),
        ("Lio/legere/pdfiumandroid/core/unlocked/PdfPageU;", "getPagePtr", "()J"),
        ("Lio/legere/pdfiumandroid/core/unlocked/PdfTextPageU;", "getPagePtr", "()J"),
    ]
    with ZipFile(args.apk) as apk:
        found = {}
        for name in apk.namelist():
            if name.startswith("classes") and name.endswith(".dex"):
                found.update(definitions(apk.read(name)))
        checks = [{"class": key[0], "method": key[1], "signature": key[2], **found.get(key, {})} for key in required]
        if not all(found.get(key, {}).get("public") for key in required) or not found.get(required[0], {}).get("native"):
            raise SystemExit("Shrunk geometry bridge JVM definitions differ or are absent")
        fd, name = tempfile.mkstemp(prefix="kitap-geometry-", suffix=".so")
        try:
            with os.fdopen(fd, "wb") as stream:
                stream.write(apk.read("lib/arm64-v8a/libappmodules.so"))
            output = subprocess.run([str(args.llvm_nm), "-D", name], check=True, capture_output=True, text=True).stdout
            symbol = "Java_com_kitap_pdf_PdfGeometry_glyphs"
            if not any(line.split()[-1] == symbol for line in output.splitlines() if line.split()):
                raise SystemExit("JNI geometry export absent")
        finally:
            Path(name).unlink(missing_ok=True)
    with args.apk.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    result = {"apkSha256": digest, "jniExport": symbol, "dexDefinitions": checks, "staticAbiPassed": True, "deviceExecutionValidated": False}
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Shrunk DEX accessors and ARM64 JNI export verified; device execution pending.")


if __name__ == "__main__":
    main()
