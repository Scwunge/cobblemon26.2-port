# Meshy → Minecraft auto-import

## Automated pipeline (works when you have a real `.glb`)

```bat
cd tools\meshy_import
:: put your file here as import.glb  (Download → GLB from Meshy while logged in)
node glb_to_mc.js import.glb SubshahMesh
```

That writes a Java model class into `src/main/java/.../client/model/`.

**Verified:** `node glb_to_mc.js fox.glb FoxVoxel` successfully voxelized a free Khronos GLB (66 boxes). Example output: `FoxVoxelModel.java.example`.

## What I can / cannot do without your login

| Step | Automated? |
|------|------------|
| Browse Meshy gallery | yes |
| Download preview + albedo texture | yes |
| Download `model.meshy` viewer package | yes |
| Download **GLB/OBJ** (Meshy “Download” button) | **no — login wall** |
| Decrypt proprietary `MESHY.AI` package | **no — encrypted, no public format** |
| GLB → voxel cube Java model | yes (`glb_to_mc.js`) |
| Wire species + renderer | yes (Subshah already) |

## Subshah in the mod right now

- Hand-built high-detail cube model from the free **preview** silhouette  
- Palette texture from free gallery maps  
- Spawn: `/fakemon spawn subshah 15`

## To get true Meshy mesh quality

1. Log into [meshy.ai](https://www.meshy.ai/3d-models/Subshah-019db99a-6de4-7383-bf75-2d2a28a7ae69)  
2. Download → **GLB**  
3. Save as `tools/meshy_import/import.glb`  
4. Tell me “convert it” — or run `node glb_to_mc.js import.glb SubshahMesh`  
5. I’ll wire the generated model over the hand-built one  

## License

Gallery pre-made assets: Meshy lists **CC0**. We only ship our cube retopo + atlas in the jar, not the proprietary `.meshy` binary.
