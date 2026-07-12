/**
 * Voxelize position cloud → simplified Minecraft Java model parts.
 * Input: positions.json [[x,y,z],...]
 * Output: SubshahMeshData.java fragment + mon_subshah_mesh.png suggestion
 */
const fs = require("fs");
const path = require("path");

const OUT = __dirname;
const posPath = path.join(OUT, "positions.json");
if (!fs.existsSync(posPath)) {
  console.log("No positions.json — skip voxelize");
  process.exit(0);
}

const positions = JSON.parse(fs.readFileSync(posPath, "utf8"));
console.log("positions", positions.length);
if (positions.length < 10) process.exit(0);

// Bounds
let minX = Infinity,
  minY = Infinity,
  minZ = Infinity,
  maxX = -Infinity,
  maxY = -Infinity,
  maxZ = -Infinity;
for (const [x, y, z] of positions) {
  if (x < minX) minX = x;
  if (y < minY) minY = y;
  if (z < minZ) minZ = z;
  if (x > maxX) maxX = x;
  if (y > maxY) maxY = y;
  if (z > maxZ) maxZ = z;
}
console.log("bounds", { minX, minY, minZ, maxX, maxY, maxZ });

// Target Minecraft units ~ 16 tall
const sizeY = maxY - minY || 1;
const sizeX = maxX - minX || 1;
const sizeZ = maxZ - minZ || 1;
const scale = 16 / sizeY;

const RES = 12; // grid resolution on longest axis
const nx = Math.max(4, Math.round((sizeX / sizeY) * RES));
const ny = RES;
const nz = Math.max(4, Math.round((sizeZ / sizeY) * RES));

const grid = new Uint8Array(nx * ny * nz);
const idx = (x, y, z) => x + y * nx + z * nx * ny;

for (const [x, y, z] of positions) {
  const gx = Math.min(nx - 1, Math.max(0, Math.floor(((x - minX) / sizeX) * (nx - 0.001))));
  const gy = Math.min(ny - 1, Math.max(0, Math.floor(((y - minY) / sizeY) * (ny - 0.001))));
  const gz = Math.min(nz - 1, Math.max(0, Math.floor(((z - minZ) / sizeZ) * (nz - 0.001))));
  grid[idx(gx, gy, gz)] = 1;
}

// Greedy merge into boxes along X then Y then Z (simple run-length)
const boxes = [];
const visited = new Uint8Array(grid.length);

function filled(x, y, z) {
  return x >= 0 && y >= 0 && z >= 0 && x < nx && y < ny && z < nz && grid[idx(x, y, z)];
}

for (let z = 0; z < nz; z++) {
  for (let y = 0; y < ny; y++) {
    for (let x = 0; x < nx; x++) {
      const i = idx(x, y, z);
      if (!grid[i] || visited[i]) continue;
      // expand X
      let x2 = x;
      while (x2 + 1 < nx && filled(x2 + 1, y, z) && !visited[idx(x2 + 1, y, z)]) x2++;
      // expand Y
      let y2 = y;
      outerY: while (y2 + 1 < ny) {
        for (let xx = x; xx <= x2; xx++) {
          if (!filled(xx, y2 + 1, z) || visited[idx(xx, y2 + 1, z)]) break outerY;
        }
        y2++;
      }
      // expand Z
      let z2 = z;
      outerZ: while (z2 + 1 < nz) {
        for (let yy = y; yy <= y2; yy++) {
          for (let xx = x; xx <= x2; xx++) {
            if (!filled(xx, yy, z2 + 1) || visited[idx(xx, yy, z2 + 1)]) break outerZ;
          }
        }
        z2++;
      }
      for (let zz = z; zz <= z2; zz++)
        for (let yy = y; yy <= y2; yy++)
          for (let xx = x; xx <= x2; xx++) visited[idx(xx, yy, zz)] = 1;

      // Convert grid cells to MC pixels (1 cell = unit)
      // Center model: MC coords with feet near y=24, body around 0
      const cell = 16 / ny; // block size in MC
      const ox = -((nx * cell) / 2);
      const oy = 24 - ny * cell; // top-ish
      const oz = -((nz * cell) / 2);
      boxes.push({
        x: ox + x * cell,
        y: oy + y * cell,
        z: oz + z * cell,
        w: (x2 - x + 1) * cell,
        h: (y2 - y + 1) * cell,
        d: (z2 - z + 1) * cell,
      });
    }
  }
}

console.log("boxes", boxes.length);
// Cap boxes for performance
const maxBoxes = 80;
const selected = boxes
  .sort((a, b) => b.w * b.h * b.d - a.w * a.h * a.d)
  .slice(0, maxBoxes);

// Emit Java model builder snippet
let java = `// AUTO-GENERATED from Meshy vertex cloud — ${selected.length} boxes\n`;
java += `// Place inside createBodyLayer()\n`;
selected.forEach((b, i) => {
  const name = `v${i}`;
  java += `root.addOrReplaceChild("${name}", CubeListBuilder.create().texOffs(${(i * 2) % 48}, ${(i * 3) % 48})
    .addBox(${b.x.toFixed(2)}F, ${b.y.toFixed(2)}F, ${b.z.toFixed(2)}F, ${b.w.toFixed(2)}F, ${b.h.toFixed(2)}F, ${b.d.toFixed(2)}F, soft),
    PartPose.ZERO);\n`;
});
fs.writeFileSync(path.join(OUT, "generated_boxes.java"), java);
fs.writeFileSync(path.join(OUT, "boxes.json"), JSON.stringify(selected, null, 2));
console.log("wrote generated_boxes.java");
