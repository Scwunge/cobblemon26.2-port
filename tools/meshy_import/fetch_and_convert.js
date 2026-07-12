/**
 * Try to obtain Subshah mesh assets + convert what we can into a Minecraft-friendly form.
 */
const fs = require("fs");
const path = require("path");
const https = require("https");
const http = require("http");
const zlib = require("zlib");
const { execSync } = require("child_process");

const OUT = __dirname;
const TASK = "019db99a-6de4-7383-bf75-2d2a28a7ae69";
const USER = "2097e464-f0f9-46b2-9eeb-0fc1293b04eb";

function get(url) {
  return new Promise((resolve, reject) => {
    const lib = url.startsWith("https") ? https : http;
    const req = lib.get(
      url,
      {
        headers: {
          "User-Agent":
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
          Accept: "*/*",
        },
      },
      (res) => {
        if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
          get(res.headers.location).then(resolve, reject);
          return;
        }
        const chunks = [];
        res.on("data", (c) => chunks.push(c));
        res.on("end", () => {
          resolve({
            status: res.statusCode,
            type: res.headers["content-type"] || "",
            buf: Buffer.concat(chunks),
          });
        });
      }
    );
    req.on("error", reject);
    req.setTimeout(60000, () => {
      req.destroy(new Error("timeout"));
    });
  });
}

async function main() {
  console.log("=== Fetch Meshy page HTML ===");
  const page = await get(`https://www.meshy.ai/3d-models/Subshah-${TASK}`);
  const html = page.buf.toString("utf8");
  fs.writeFileSync(path.join(OUT, "page.html"), html);

  const urls = [...html.matchAll(/https?:\\?\/\\?\/[^"'\\\s]+/g)].map((m) =>
    m[0].replace(/\\\//g, "/")
  );
  const modelUrls = urls.filter((u) => /cdn-models|download=|\.glb|\.obj|\.fbx|\.meshy/i.test(u));
  console.log("model-ish urls:", [...new Set(modelUrls)].slice(0, 30));

  // Known signed model.meshy
  const meshyUrl = modelUrls.find((u) => u.includes("model.meshy"));
  if (meshyUrl) {
    console.log("Downloading model.meshy...");
    const r = await get(meshyUrl);
    fs.writeFileSync(path.join(OUT, "model.meshy"), r.buf);
    console.log("model.meshy", r.status, r.buf.length, r.buf.slice(0, 16).toString("ascii"));
    await tryUnpackMeshy(r.buf);
  }

  // Try download endpoints
  const tries = [
    `https://www.meshy.ai/3d-models/Subshah-${TASK}?download=glb`,
    `https://www.meshy.ai/3d-models/Subshah-${TASK}?download=obj`,
    `https://api.meshy.ai/openapi/v1/tasks/${TASK}/download?format=glb`,
  ];
  for (const u of tries) {
    try {
      const r = await get(u);
      console.log("try", u, r.status, r.type, r.buf.length, r.buf.slice(0, 4).toString("ascii"));
      if (r.buf.slice(0, 4).toString("ascii") === "glTF") {
        fs.writeFileSync(path.join(OUT, "subshah.glb"), r.buf);
        console.log("GOT GLB!");
      } else if (r.buf.slice(0, 1).toString() === "{" || r.buf.includes("mtllib") || r.buf.includes("v ")) {
        fs.writeFileSync(path.join(OUT, "download_resp.bin"), r.buf);
      }
    } catch (e) {
      console.log("fail", u, e.message);
    }
  }

  // If we have glb, try npm convert
  const glbPath = path.join(OUT, "subshah.glb");
  if (fs.existsSync(glbPath) && fs.statSync(glbPath).size > 1000) {
    const magic = fs.readFileSync(glbPath).slice(0, 4).toString("ascii");
    if (magic === "glTF") {
      await convertGlb(glbPath);
    }
  } else {
    console.log("No GLB available without auth. Building voxel proxy from preview depth-ish...");
  }
}

async function tryUnpackMeshy(buf) {
  // Header: MESHY.AI + unknown
  // Search for zlib streams and try inflate
  const outDir = path.join(OUT, "meshy_extract");
  fs.mkdirSync(outDir, { recursive: true });
  let found = 0;
  for (let i = 0; i < buf.length - 2; i++) {
    if (buf[i] === 0x78 && (buf[i + 1] === 0x9c || buf[i + 1] === 0xda || buf[i + 1] === 0x01)) {
      try {
        const slice = buf.subarray(i, Math.min(buf.length, i + 2_000_000));
        const inflated = zlib.inflateSync(slice);
        if (inflated.length > 100) {
          const name = path.join(outDir, `chunk_${i}_${inflated.length}.bin`);
          fs.writeFileSync(name, inflated);
          found++;
          const head = inflated.slice(0, 20).toString("ascii");
          console.log("inflated @", i, "len", inflated.length, "head", JSON.stringify(head));
          if (inflated.slice(0, 4).toString("ascii") === "glTF") {
            fs.writeFileSync(path.join(OUT, "subshah.glb"), inflated);
            console.log("EXTRACTED glTF from zlib!");
            await convertGlb(path.join(OUT, "subshah.glb"));
          }
          // look for JSON mesh data
          const s = inflated.toString("utf8");
          if (s.includes("POSITION") || s.includes("accessors") || s.includes("meshes")) {
            fs.writeFileSync(path.join(outDir, `chunk_${i}.json.txt`), s.slice(0, 5000));
            console.log("possible mesh json @", i);
          }
          if (found >= 15) break;
        }
      } catch (_) {
        /* not a valid zlib stream start */
      }
    }
  }
  console.log("zlib extracts:", found);
}

async function convertGlb(glbPath) {
  console.log("Converting", glbPath);
  // Install @gltf-transform/core locally if needed
  try {
    execSync("npm install --no-save @gltf-transform/core @gltf-transform/extensions", {
      cwd: OUT,
      stdio: "inherit",
    });
  } catch (e) {
    console.log("npm install failed", e.message);
  }
  try {
    const { NodeIO } = require("@gltf-transform/core");
    const io = new NodeIO();
    const doc = await io.read(glbPath);
    const root = doc.getRoot();
    const meshes = root.listMeshes();
    console.log("meshes", meshes.length);
    // Dump rough stats for voxelization script
    let verts = 0;
    const positions = [];
    for (const mesh of meshes) {
      for (const prim of mesh.listPrimitives()) {
        const pos = prim.getAttribute("POSITION");
        if (!pos) continue;
        const arr = pos.getArray();
        for (let i = 0; i < arr.length; i += 3) {
          positions.push([arr[i], arr[i + 1], arr[i + 2]]);
          verts++;
        }
      }
    }
    console.log("vertices", verts);
    fs.writeFileSync(path.join(OUT, "positions.json"), JSON.stringify(positions.slice(0, 50000)));
    // Run voxelizer
    execSync(`node "${path.join(OUT, "voxelize_to_java.js")}"`, { stdio: "inherit" });
  } catch (e) {
    console.log("convert failed", e);
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
