/**
 * Download every Pokémon sprite from pokemondb.net, minecraftify (pixel + outline),
 * write to assets/cobblemon/textures/entity/sprites/front/<id>.png
 *
 * Usage: node tools/download_pokemondb_sprites.js
 *        node tools/download_pokemondb_sprites.js --limit 20   (smoke test)
 */
const fs = require("fs");
const path = require("path");
const https = require("https");
const http = require("http");
const { PNG } = require("./sprite_gen/node_modules/pngjs");

const ROOT = path.join(__dirname, "..");
const SPECIES = path.join(ROOT, "src/main/resources/data/cobblemon/content/species_index.json");
const OUT = path.join(ROOT, "src/main/resources/assets/cobblemon/textures/entity/sprites/front");
const CACHE = path.join(__dirname, "_sprite_cache");
const SIZE = 64; // MC UI portraits

const LIMIT = (() => {
  const i = process.argv.indexOf("--limit");
  return i >= 0 ? parseInt(process.argv[i + 1], 10) : 0;
})();

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

function fetchBuffer(url) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith("https") ? https : http;
    const req = lib.get(
      url,
      {
        headers: {
          "User-Agent": "Cobblemon26PortSpriteTool/1.0 (personal mod; educational)",
          Accept: "image/png,image/*,*/*",
          Referer: "https://pokemondb.net/sprites",
        },
        timeout: 20000,
      },
      (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          fetchBuffer(res.headers.location).then(resolve, reject);
          return;
        }
        if (res.statusCode !== 200) {
          reject(new Error("HTTP " + res.statusCode + " " + url));
          res.resume();
          return;
        }
        const chunks = [];
        res.on("data", (c) => chunks.push(c));
        res.on("end", () => resolve(Buffer.concat(chunks)));
      }
    );
    req.on("error", reject);
    req.on("timeout", () => {
      req.destroy();
      reject(new Error("timeout " + url));
    });
  });
}

function fetchText(url) {
  return fetchBuffer(url).then((b) => b.toString("utf8"));
}

function normalize(s) {
  return String(s || "")
    .toLowerCase()
    .replace(/[^a-z0-9]/g, "");
}

/** Manual overrides when auto-match fails (our id → pokemondb slug) */
const SLUG_OVERRIDES = {
  mrmime: "mr-mime",
  mimejr: "mime-jr",
  mrrime: "mr-rime",
  nidoranf: "nidoran-f",
  nidoranm: "nidoran-m",
  hooh: "ho-oh",
  porygonz: "porygon-z",
  farfetchd: "farfetchd",
  sirfetchd: "sirfetchd",
  jangmoo: "jangmo-o",
  hakamoo: "hakamo-o",
  kommoo: "kommo-o",
  typenull: "type-null",
  tapukoko: "tapu-koko",
  tapulele: "tapu-lele",
  tapubulu: "tapu-bulu",
  tapufini: "tapu-fini",
  flabebe: "flabebe",
  greattusk: "great-tusk",
  screamtail: "scream-tail",
  brutebonnet: "brute-bonnet",
  fluttermane: "flutter-mane",
  slitherwing: "slither-wing",
  sandyshocks: "sandy-shocks",
  irontreads: "iron-treads",
  ironbundle: "iron-bundle",
  ironhands: "iron-hands",
  ironjugulis: "iron-jugulis",
  ironmoth: "iron-moth",
  ironthorns: "iron-thorns",
  ironvaliant: "iron-valiant",
  ironleaves: "iron-leaves",
  ironboulder: "iron-boulder",
  ironcrown: "iron-crown",
  roaringmoon: "roaring-moon",
  walkingwake: "walking-wake",
  gougingfire: "gouging-fire",
  ragingbolt: "raging-bolt",
  chienpao: "chien-pao",
  wochien: "wo-chien",
  tinglu: "ting-lu",
  chiyu: "chi-yu",
  tinglu: "ting-lu",
  open: "oinkologne",
};

async function scrapeSlugIndex() {
  console.log("Scraping pokemondb.net/sprites for available slugs…");
  const html = await fetchText("https://pokemondb.net/sprites");
  // e.g. https://img.pokemondb.net/sprites/scarlet-violet/icon/bulbasaur.png
  const re = /img\.pokemondb\.net\/sprites\/([^/"']+)\/icon\/([a-z0-9\-]+)\.png/gi;
  const byNorm = new Map(); // normalized -> { slug, iconUrl }
  let m;
  while ((m = re.exec(html))) {
    const set = m[1];
    const slug = m[2];
    const norm = normalize(slug);
    if (!byNorm.has(norm)) {
      byNorm.set(norm, {
        slug,
        iconUrl: `https://img.pokemondb.net/sprites/${set}/icon/${slug}.png`,
      });
    }
  }
  console.log("Found", byNorm.size, "unique icon slugs");
  return byNorm;
}

function candidateUrls(slug) {
  return [
    `https://img.pokemondb.net/sprites/black-white/normal/${slug}.png`,
    `https://img.pokemondb.net/sprites/x-y/normal/${slug}.png`,
    `https://img.pokemondb.net/sprites/sword-shield/normal/${slug}.png`,
    `https://img.pokemondb.net/sprites/home/normal/${slug}.png`,
    `https://img.pokemondb.net/sprites/scarlet-violet/icon/${slug}.png`,
    `https://img.pokemondb.net/sprites/scarlet-violet/normal/${slug}.png`,
  ];
}

async function downloadBestSprite(slug) {
  const cacheFile = path.join(CACHE, slug + ".png");
  if (fs.existsSync(cacheFile) && fs.statSync(cacheFile).size > 100) {
    return fs.readFileSync(cacheFile);
  }
  let lastErr;
  for (const url of candidateUrls(slug)) {
    try {
      const buf = await fetchBuffer(url);
      if (buf.length < 80 || buf[0] !== 0x89) continue; // not a PNG
      fs.writeFileSync(cacheFile, buf);
      return buf;
    } catch (e) {
      lastErr = e;
    }
  }
  throw lastErr || new Error("no sprite for " + slug);
}

function decodePng(buf) {
  return PNG.sync.read(buf);
}

function encodePng(png) {
  return PNG.sync.write(png);
}

/** Nearest-neighbor scale into SIZE×SIZE, preserving aspect, centered. */
function minecraftify(srcPng) {
  const sw = srcPng.width;
  const sh = srcPng.height;
  // crop transparent padding
  let minX = sw,
    minY = sh,
    maxX = 0,
    maxY = 0;
  for (let y = 0; y < sh; y++) {
    for (let x = 0; x < sw; x++) {
      const i = (sw * y + x) << 2;
      if (srcPng.data[i + 3] > 16) {
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
      }
    }
  }
  if (maxX < minX) {
    // empty — solid placeholder
    const empty = new PNG({ width: SIZE, height: SIZE });
    return empty;
  }
  const cw = maxX - minX + 1;
  const ch = maxY - minY + 1;
  const pad = 2; // room for outline
  const fit = SIZE - pad * 2;
  const scale = Math.min(fit / cw, fit / ch);
  // snap scale to nicer pixel steps for chunky look
  let pixScale = Math.max(1, Math.floor(scale));
  if (scale >= 1.5 && pixScale === 1) pixScale = 2;
  if (scale >= 2.5) pixScale = Math.min(Math.floor(scale), 4);

  // Intermediate: scale source crop by pixScale (nearest)
  const midW = cw * pixScale;
  const midH = ch * pixScale;
  const mid = new PNG({ width: midW, height: midH });
  for (let y = 0; y < midH; y++) {
    for (let x = 0; x < midW; x++) {
      const sx = minX + Math.floor(x / pixScale);
      const sy = minY + Math.floor(y / pixScale);
      const si = (sw * sy + sx) << 2;
      const di = (midW * y + x) << 2;
      mid.data[di] = srcPng.data[si];
      mid.data[di + 1] = srcPng.data[si + 1];
      mid.data[di + 2] = srcPng.data[si + 2];
      mid.data[di + 3] = srcPng.data[si + 3];
    }
  }

  // If still larger than fit box, nearest downscale to fit
  let body = mid;
  if (midW > fit || midH > fit) {
    const s2 = Math.min(fit / midW, fit / midH);
    const bw = Math.max(1, Math.floor(midW * s2));
    const bh = Math.max(1, Math.floor(midH * s2));
    body = new PNG({ width: bw, height: bh });
    for (let y = 0; y < bh; y++) {
      for (let x = 0; x < bw; x++) {
        const sx = Math.min(midW - 1, Math.floor(x / s2));
        const sy = Math.min(midH - 1, Math.floor(y / s2));
        const si = (midW * sy + sx) << 2;
        const di = (bw * y + x) << 2;
        body.data[di] = mid.data[si];
        body.data[di + 1] = mid.data[si + 1];
        body.data[di + 2] = mid.data[si + 2];
        body.data[di + 3] = mid.data[si + 3];
      }
    }
  }

  // Slight color punch + alpha threshold (MC-friendly)
  for (let i = 0; i < body.data.length; i += 4) {
    if (body.data[i + 3] < 40) {
      body.data[i + 3] = 0;
      continue;
    }
    body.data[i + 3] = 255;
    // mild contrast
    for (let c = 0; c < 3; c++) {
      let v = body.data[i + c];
      v = Math.round((v - 128) * 1.08 + 128);
      body.data[i + c] = Math.max(0, Math.min(255, v));
    }
  }

  // Canvas with outline
  const out = new PNG({ width: SIZE, height: SIZE });
  const ox = Math.floor((SIZE - body.width) / 2);
  const oy = Math.floor((SIZE - body.height) / 2);

  // Draw dark outline (4-neighbor + diagonals) around opaque pixels
  const outline = [20, 16, 28, 255];
  for (let y = 0; y < body.height; y++) {
    for (let x = 0; x < body.width; x++) {
      const i = (body.width * y + x) << 2;
      if (body.data[i + 3] < 128) continue;
      for (let dy = -1; dy <= 1; dy++) {
        for (let dx = -1; dx <= 1; dx++) {
          if (dx === 0 && dy === 0) continue;
          const nx = ox + x + dx;
          const ny = oy + y + dy;
          if (nx < 0 || ny < 0 || nx >= SIZE || ny >= SIZE) continue;
          // only write outline where final body won't cover
          const bi = ((body.width * y + x) << 2);
          void bi;
          const oi = (SIZE * ny + nx) << 2;
          // mark outline candidates
          if (out.data[oi + 3] === 0) {
            out.data[oi] = outline[0];
            out.data[oi + 1] = outline[1];
            out.data[oi + 2] = outline[2];
            out.data[oi + 3] = 255;
          }
        }
      }
    }
  }

  // Blit body on top
  for (let y = 0; y < body.height; y++) {
    for (let x = 0; x < body.width; x++) {
      const i = (body.width * y + x) << 2;
      if (body.data[i + 3] < 128) continue;
      const dx = ox + x;
      const dy = oy + y;
      if (dx < 0 || dy < 0 || dx >= SIZE || dy >= SIZE) continue;
      const oi = (SIZE * dy + dx) << 2;
      out.data[oi] = body.data[i];
      out.data[oi + 1] = body.data[i + 1];
      out.data[oi + 2] = body.data[i + 2];
      out.data[oi + 3] = 255;
    }
  }

  // Soft bottom shadow for depth (1px)
  for (let x = 2; x < SIZE - 2; x++) {
    for (let y = SIZE - 4; y < SIZE - 1; y++) {
      const oi = (SIZE * y + x) << 2;
      if (out.data[oi + 3] === 0) {
        // only under silhouette
        let above = false;
        for (let ay = y - 3; ay < y; ay++) {
          const ai = (SIZE * ay + x) << 2;
          if (out.data[ai + 3] > 128) {
            above = true;
            break;
          }
        }
        if (above) {
          out.data[oi] = 0;
          out.data[oi + 1] = 0;
          out.data[oi + 2] = 0;
          out.data[oi + 3] = 40;
        }
      }
    }
  }

  return out;
}

function resolveSlug(id, byNorm) {
  if (SLUG_OVERRIDES[id]) return SLUG_OVERRIDES[id];
  const norm = normalize(id);
  if (byNorm.has(norm)) return byNorm.get(norm).slug;
  // try common splits for paradox / multi-word (iron*, great*, etc.) already in overrides
  // fuzzy: find unique slug whose norm equals our id
  for (const [n, v] of byNorm) {
    if (n === norm) return v.slug;
  }
  // strip trailing form junk
  const base = norm.replace(/(gmax|mega|alola|galar|hisui|paldea)$/, "");
  if (byNorm.has(base)) return byNorm.get(base).slug;
  return null;
}

async function main() {
  fs.mkdirSync(OUT, { recursive: true });
  fs.mkdirSync(CACHE, { recursive: true });

  const index = JSON.parse(fs.readFileSync(SPECIES, "utf8"));
  let ids = Object.keys(index.species).sort(
    (a, b) =>
      (index.species[a].nationalPokedexNumber || 0) -
      (index.species[b].nationalPokedexNumber || 0)
  );
  if (LIMIT > 0) ids = ids.slice(0, LIMIT);

  const byNorm = await scrapeSlugIndex();

  let ok = 0,
    fail = 0;
  const failed = [];

  for (let i = 0; i < ids.length; i++) {
    const id = ids[i];
    const outFile = path.join(OUT, id + ".png");
    process.stdout.write(`[${i + 1}/${ids.length}] ${id} … `);

    try {
      const slug = resolveSlug(id, byNorm);
      if (!slug) throw new Error("no slug match");
      const raw = await downloadBestSprite(slug);
      const src = decodePng(raw);
      const mc = minecraftify(src);
      fs.writeFileSync(outFile, encodePng(mc));
      ok++;
      console.log("OK (" + slug + ")");
    } catch (e) {
      fail++;
      failed.push(id + ": " + e.message);
      console.log("FAIL " + e.message);
    }
    // be polite to CDN
    if (i % 5 === 4) await sleep(120);
    else await sleep(40);
  }

  console.log("\n=== DONE ===");
  console.log("OK:", ok, "FAIL:", fail);
  console.log("Wrote to", OUT);
  if (failed.length) {
    console.log("Failures:");
    failed.slice(0, 40).forEach((f) => console.log(" ", f));
    if (failed.length > 40) console.log(" … +" + (failed.length - 40) + " more");
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
