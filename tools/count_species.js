const fs = require("fs");
const t = fs.readFileSync("src/main/java/com/cobblemon/mod/species/MonSpecies.java", "utf8");
const m = t.match(/^\s+[A-Z][A-Z0-9_]*\("/gm);
console.log("enum", m ? m.length : 0);
const tex = fs.readdirSync("src/main/resources/assets/cobblemon/textures/pokemon").filter(n => fs.statSync("src/main/resources/assets/cobblemon/textures/pokemon/"+n).isDirectory());
console.log("tex_dirs", tex.length);
function walk(d, acc=[]) { for (const e of fs.readdirSync(d,{withFileTypes:true})) { const p=require("path").join(d,e.name); if(e.isDirectory()) walk(p,acc); else if(p.endsWith(".json")) acc.push(p);} return acc; }
console.log("species_json", walk("src/main/resources/data/cobblemon/species").length);
