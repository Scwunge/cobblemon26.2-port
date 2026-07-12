const fs = require("fs");
const p = __dirname + "/model.meshy";
const b = fs.readFileSync(p);
console.log("size", b.length);
console.log("header", b.slice(0, 16).toString("ascii"));
function find(sig) {
  const s = Buffer.isBuffer(sig) ? sig : Buffer.from(sig);
  const hits = [];
  for (let i = 0; i < b.length - s.length; i++) {
    let ok = true;
    for (let j = 0; j < s.length; j++) if (b[i + j] !== s[j]) { ok = false; break; }
    if (ok) {
      hits.push(i);
      if (hits.length >= 10) break;
    }
  }
  return hits;
}
console.log("glTF", find("glTF"));
console.log("PNG", find(Buffer.from([0x89, 0x50, 0x4e, 0x47])));
console.log("zlib", find(Buffer.from([0x78, 0x9c])).slice(0, 8));
console.log("u32s", b.readUInt32LE(8), b.readUInt32LE(12), b.readUInt32LE(16), b.readUInt32LE(20));
console.log("hex64", [...b.slice(0, 64)].map((x) => x.toString(16).padStart(2, "0")).join(" "));
