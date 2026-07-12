/**
 * Fully automated: any .glb → boxes.json + GeneratedMeshModel.java
 * Usage: node glb_to_mc.js input.glb ModelName
 */
const fs = require("fs");
const path = require("path");

async function main() {
  const glbPath = process.argv[2] || path.join(__dirname, "fox.glb");
  const modelName = process.argv[3] || "GeneratedMesh";
  if (!fs.existsSync(glbPath)) {
    console.error("Missing", glbPath);
    process.exit(1);
  }
  const { NodeIO } = require("@gltf-transform/core");
  const io = new NodeIO();
  const doc = await io.read(glbPath);
  const positions = [];
  for (const mesh of doc.getRoot().listMeshes()) {
    for (const prim of mesh.listPrimitives()) {
      const pos = prim.getAttribute("POSITION");
      if (!pos) continue;
      const arr = pos.getArray();
      for (let i = 0; i < arr.length; i += 3) {
        positions.push([arr[i], arr[i + 1], arr[i + 2]]);
      }
    }
  }
  console.log("vertices", positions.length);
  fs.writeFileSync(path.join(__dirname, "positions.json"), JSON.stringify(positions));

  // Voxelize
  let minX = Infinity, minY = Infinity, minZ = Infinity;
  let maxX = -Infinity, maxY = -Infinity, maxZ = -Infinity;
  for (const [x, y, z] of positions) {
    minX = Math.min(minX, x); minY = Math.min(minY, y); minZ = Math.min(minZ, z);
    maxX = Math.max(maxX, x); maxY = Math.max(maxY, y); maxZ = Math.max(maxZ, z);
  }
  const sizeX = maxX - minX || 1;
  const sizeY = maxY - minY || 1;
  const sizeZ = maxZ - minZ || 1;
  const RES = 14;
  const nx = Math.max(6, Math.round((sizeX / Math.max(sizeX, sizeY, sizeZ)) * RES));
  const ny = Math.max(6, Math.round((sizeY / Math.max(sizeX, sizeY, sizeZ)) * RES));
  const nz = Math.max(6, Math.round((sizeZ / Math.max(sizeX, sizeY, sizeZ)) * RES));
  const grid = new Uint8Array(nx * ny * nz);
  const I = (x, y, z) => x + y * nx + z * nx * ny;
  for (const [x, y, z] of positions) {
    const gx = Math.min(nx - 1, Math.max(0, Math.floor(((x - minX) / sizeX) * (nx - 1e-6))));
    const gy = Math.min(ny - 1, Math.max(0, Math.floor(((y - minY) / sizeY) * (ny - 1e-6))));
    const gz = Math.min(nz - 1, Math.max(0, Math.floor(((z - minZ) / sizeZ) * (nz - 1e-6))));
    grid[I(gx, gy, gz)] = 1;
  }
  const filled = (x, y, z) =>
    x >= 0 && y >= 0 && z >= 0 && x < nx && y < ny && z < nz && grid[I(x, y, z)];
  const visited = new Uint8Array(grid.length);
  const boxes = [];
  for (let z = 0; z < nz; z++) {
    for (let y = 0; y < ny; y++) {
      for (let x = 0; x < nx; x++) {
        if (!grid[I(x, y, z)] || visited[I(x, y, z)]) continue;
        let x2 = x;
        while (x2 + 1 < nx && filled(x2 + 1, y, z) && !visited[I(x2 + 1, y, z)]) x2++;
        let y2 = y;
        outerY: while (y2 + 1 < ny) {
          for (let xx = x; xx <= x2; xx++)
            if (!filled(xx, y2 + 1, z) || visited[I(xx, y2 + 1, z)]) break outerY;
          y2++;
        }
        let z2 = z;
        outerZ: while (z2 + 1 < nz) {
          for (let yy = y; yy <= y2; yy++)
            for (let xx = x; xx <= x2; xx++)
              if (!filled(xx, yy, z2 + 1) || visited[I(xx, yy, z2 + 1)]) break outerZ;
          z2++;
        }
        for (let zz = z; zz <= z2; zz++)
          for (let yy = y; yy <= y2; yy++)
            for (let xx = x; xx <= x2; xx++) visited[I(xx, yy, zz)] = 1;

        const cell = 16 / Math.max(nx, ny, nz);
        const ox = -(nx * cell) / 2;
        const oy = 24 - ny * cell;
        const oz = -(nz * cell) / 2;
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
  const selected = boxes.sort((a, b) => b.w * b.h * b.d - a.w * a.h * a.d).slice(0, 90);
  console.log("boxes", boxes.length, "kept", selected.length);

  // Java model class
  const className = modelName.replace(/[^A-Za-z0-9]/g, "") + "Model";
  let java = `package ai.xai.fakemon.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Auto-generated from GLB via tools/meshy_import/glb_to_mc.js */
public class ${className} extends EntityModel<MonRenderState> {
    private final ModelPart root;
    public ${className}(ModelPart root) {
        super(root);
        this.root = root;
    }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation soft = new CubeDeformation(0.15F);
`;
  selected.forEach((b, i) => {
    java += `        root.addOrReplaceChild("p${i}", CubeListBuilder.create().texOffs(${(i * 3) % 48}, ${(i * 5) % 48})
                .addBox(${f(b.x)}F, ${f(b.y)}F, ${f(b.z)}F, ${f(b.w)}F, ${f(b.h)}F, ${f(b.d)}F, soft),
                PartPose.ZERO);\n`;
  });
  java += `        return LayerDefinition.create(mesh, 64, 64);
    }
    @Override
    public void setupAnim(MonRenderState state) {
        this.root.yRot = state.yRot * ((float) Math.PI / 180F) * 0.3F;
        this.root.y = Mth.sin(state.ageInTicks * 0.1F) * 0.4F;
    }
}
`;
  function f(n) {
    return Number(n.toFixed(3));
  }

  const outJava = path.join(
    __dirname,
    "..",
    "..",
    "src",
    "main",
    "java",
    "ai",
    "xai",
    "fakemon",
    "client",
    "model",
    className + ".java"
  );
  fs.writeFileSync(outJava, java);
  fs.writeFileSync(path.join(__dirname, "boxes.json"), JSON.stringify(selected));
  console.log("wrote", outJava);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
