"""Static APK inspection; does not establish installation or device performance."""

import argparse
import hashlib
import json
from pathlib import Path
import struct
import zipfile


def load_segment_alignments(data):
    if data[:4] != b"\x7fELF" or data[4] not in (1, 2) or data[5] not in (1, 2):
        raise ValueError("Unsupported ELF header")
    endian = "<" if data[5] == 1 else ">"
    wide = data[4] == 2
    table = struct.unpack_from(endian + ("Q" if wide else "I"), data, 32 if wide else 28)[0]
    entry_size, count = struct.unpack_from(endian + "HH", data, 54 if wide else 42)
    if entry_size < (56 if wide else 32):
        raise ValueError("Invalid ELF program-header size")
    alignments = []
    for index in range(count):
        offset = table + entry_size * index
        if struct.unpack_from(endian + "I", data, offset)[0] == 1:
            alignments.append(struct.unpack_from(endian + ("Q" if wide else "I"), data, offset + (48 if wide else 28))[0])
    return alignments


def inspect(apk):
    libraries = []
    with zipfile.ZipFile(apk) as archive, apk.open("rb") as handle:
        names = archive.namelist()
        for entry in archive.infolist():
            if entry.filename.startswith("lib/") and entry.filename.endswith(".so"):
                alignments = load_segment_alignments(archive.read(entry))
                handle.seek(entry.header_offset)
                header = handle.read(30)
                name_len, extra_len = struct.unpack_from("<HH", header, 26)
                data_offset = entry.header_offset + 30 + name_len + extra_len
                compressed = entry.compress_type != zipfile.ZIP_STORED
                libraries.append({
                    "path": entry.filename,
                    "abi": entry.filename.split("/")[1],
                    "uncompressedBytes": entry.file_size,
                    "loadSegmentAlignments": alignments,
                    "elf16kAligned": bool(alignments) and min(alignments) >= 16384,
                    "storedUncompressed": not compressed,
                    "zip16kAligned": None if compressed else data_offset % 16384 == 0,
                })
    digest = hashlib.sha256()
    with apk.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return {
        "schemaVersion": 1, "apk": str(apk.resolve()),
        "sizeMiB": apk.stat().st_size / (1024 * 1024),
        "sha256": digest.hexdigest(),
        "abis": sorted({lib["abi"] for lib in libraries}),
        "hasBundledJavaScript": "assets/index.android.bundle" in names,
        "hasHermesLibrary": any("hermes" in lib["path"].lower() for lib in libraries),
        "hasPdfiumLibrary": any("pdfium" in lib["path"].lower() for lib in libraries),
        "allElf16kAligned": bool(libraries) and all(lib["elf16kAligned"] for lib in libraries),
        "allStoredLibrariesZip16kAligned": all(lib["zip16kAligned"] is not False for lib in libraries),
        "libraries": libraries, "physicalDeviceValidated": False,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    if args.output and args.output.resolve() == args.apk.resolve():
        parser.error("Output must not overwrite APK")
    result = inspect(args.apk)
    encoded = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(encoded, encoding="utf-8")
    print(encoded, end="")


if __name__ == "__main__":
    main()
