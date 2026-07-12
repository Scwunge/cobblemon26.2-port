import pathlib
import re

root = pathlib.Path(r"C:\Users\conne\Desktop\pixelmontest\fakemons\src")
n = 0
for p in list(root.rglob("*.java")) + list(root.rglob("*.json")):
    t = p.read_text(encoding="utf-8")
    o = t
    # message.Fakemon.x -> message.fakemon.x  (namespace segment)
    t = re.sub(r"(?<=[\.\"])Fakemon\.", "fakemon.", t)
    t = t.replace('literal("Fakemon")', 'literal("fakemon")')
    t = t.replace("/Fakemon give", "/fakemon give")
    if t != o:
        p.write_text(t, encoding="utf-8")
        n += 1
        print("updated", p.relative_to(root))
print("total", n)
