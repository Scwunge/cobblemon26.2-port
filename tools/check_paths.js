const fs = require("fs");
const path = require("path");
const st = fs.readFileSync(
  "C:/Users/conne/Desktop/pixelmontest/fakemons/src/main/java/ai/xai/fakemon/species/SpeciesTextures.java",
  "utf8"
);
const geoRoot =
  "C:/Users/conne/Desktop/pixelmontest/fakemons/src/main/resources/assets/fakemon/bedrock/pokemon/models";
const animRoot =
  "C:/Users/conne/Desktop/pixelmontest/fakemons/src/main/resources/assets/fakemon/bedrock/pokemon/animations";
const texRoot =
  "C:/Users/conne/Desktop/pixelmontest/fakemons/src/main/resources/assets/fakemon/textures/pokemon";

const geoCases = [...st.matchAll(/case (\w+) -> "([^"]+\.geo\.json)"/g)];
const texCases = [...st.matchAll(/case (\w+) -> "([^"]+\.png)"/g)];
const geoMap = Object.fromEntries(geoCases.map((m) => [m[1], m[2]]));
const texMap = Object.fromEntries(texCases.map((m) => [m[1], m[2]]));

let missingGeo = [],
  missingAnim = [],
  missingTex = [];
for (const [sp, g] of Object.entries(geoMap)) {
  if (!fs.existsSync(path.join(geoRoot, g))) missingGeo.push(sp + ":" + g);
  let folder = g.split("/")[0];
  let file = g
    .split("/")[1]
    .replace(".geo.json", "")
    .replace("_male", "")
    .replace("_female", "");
  let ap = folder + "/" + file + ".animation.json";
  if (!fs.existsSync(path.join(animRoot, ap))) {
    const dir = path.join(animRoot, folder);
    if (fs.existsSync(dir)) {
      const files = fs.readdirSync(dir).filter((f) => f.endsWith(".animation.json"));
      missingAnim.push(sp + ": tried " + ap + " have " + files.join(","));
    } else missingAnim.push(sp + ": no anim folder " + folder);
  }
}
for (const [sp, t] of Object.entries(texMap)) {
  if (!fs.existsSync(path.join(texRoot, t))) missingTex.push(sp + ":" + t);
}
console.log("species geos", Object.keys(geoMap).length);
console.log("missing geo", missingGeo.length, missingGeo.slice(0, 15));
console.log("missing anim", missingAnim.length);
missingAnim.slice(0, 20).forEach((x) => console.log(" ", x));
console.log("missing tex", missingTex.length, missingTex.slice(0, 15));
