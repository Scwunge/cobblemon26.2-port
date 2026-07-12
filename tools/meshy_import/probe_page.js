const fs = require("fs");
const path = require("path");
const https = require("https");

const pagePath = path.join(__dirname, "page.html");
let h = fs.existsSync(pagePath)
  ? fs.readFileSync(pagePath, "utf8")
  : "";

function get(url) {
  return new Promise((resolve, reject) => {
    https
      .get(url, { headers: { "User-Agent": "Mozilla/5.0" } }, (res) => {
        const c = [];
        res.on("data", (d) => c.push(d));
        res.on("end", () => resolve(Buffer.concat(c).toString("utf8")));
      })
      .on("error", reject);
  });
}

(async () => {
  if (!h) {
    // already have page
  }
  const keys = ["model.meshy", "decrypt", "MESHY.AI", "parseMeshy", "loadModel", "wasm", "AES", "subtle"];
  for (const k of keys) {
    const i = h.indexOf(k);
    console.log(k, i);
    if (i >= 0) console.log(" ", JSON.stringify(h.slice(Math.max(0, i - 30), i + 60)));
  }
  const js = [...h.matchAll(/https:\/\/cdn\.meshy\.ai\/webapp-build-assets\/production\/_next\/static\/chunks\/[^"']+\.js/g)].map(
    (m) => m[0]
  );
  console.log("js", js.length);
  // Grep a few large chunks for MESHY format parser
  let found = [];
  for (const url of js.slice(0, 40)) {
    try {
      const body = await get(url);
      if (/MESHY|model\.meshy|decryptMeshy|parseMeshy|meshyFormat/i.test(body)) {
        found.push(url);
        const idx = body.search(/MESHY|model\.meshy|decryptMeshy/i);
        console.log("HIT", url);
        console.log(body.slice(Math.max(0, idx - 50), idx + 200));
        fs.writeFileSync(path.join(__dirname, "hit_" + path.basename(url)), body);
      }
    } catch (e) {
      console.log("skip", url, e.message);
    }
  }
  console.log("hits", found.length);
})();
