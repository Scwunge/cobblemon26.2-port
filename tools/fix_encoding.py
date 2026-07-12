# -*- coding: utf-8 -*-
from pathlib import Path

ms = Path(r"C:\Users\conne\Desktop\pixelmontest\fakemons\src\main\java\ai\xai\fakemon\species\MonSpecies.java")
text = ms.read_bytes()
if text.startswith(b"\xef\xbb\xbf"):
    text = text[3:]
s = text.decode("utf-8")

# Normalize mojibake / broken punctuation
replacements = [
    ("Legendary Â· ", "Legendary · "),
    ("Legendary Ã‚· ", "Legendary · "),
    ("â€”", "—"),
    ("Ã¢â‚¬â€", "—"),
    ("Ã¢â‚¬â€", "—"),
]
for a, b in replacements:
    s = s.replace(a, b)

# Ensure categoryLine is clean
import re
s = re.sub(
    r'return Component\.literal\("Legendary .{1,8}" \+ category \+ " Pokémon"\);',
    'return Component.literal("Legendary · " + category + " Pokémon");',
    s,
)

# Fix accidental leading spaces before key = switch
s = re.sub(r'(?m)^[ \t]+key = switch \(key\) \{', "        key = switch (key) {", s)

ms.write_text(s, encoding="utf-8", newline="\n")
print("MonSpecies OK")
print("categoryLine:", [line.strip() for line in s.splitlines() if "Legendary" in line and "category" in line][:3])

lang = Path(r"C:\Users\conne\Desktop\pixelmontest\fakemons\src\main\resources\assets\fakemon\lang\en_us.json")
lb = lang.read_bytes()
if lb.startswith(b"\xef\xbb\xbf"):
    lb = lb[3:]
ls = lb.decode("utf-8")
for a, b in [
    ("Ã¢â‚¬Â¦", "…"),
    ("â€¦", "…"),
    ("â€”", "—"),
    ("Final form â€” no", "Final form — no"),
    ("Final form Â€” no", "Final form — no"),
]:
    ls = ls.replace(a, b)
lang.write_text(ls, encoding="utf-8", newline="\n")
print("lang OK")
print("sample names:", [l for l in ls.splitlines() if "species.fakemon." in l][:5])
