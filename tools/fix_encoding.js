const fs = require("fs");

const msPath =
  "C:/Users/conne/Desktop/pixelmontest/fakemons/src/main/java/ai/xai/fakemon/species/MonSpecies.java";
let b = fs.readFileSync(msPath);
if (b[0] === 0xef && b[1] === 0xbb && b[2] === 0xbf) b = b.subarray(3);
let s = b.toString("utf8");

// Strip accidental leading BOM char if still present as character
if (s.charCodeAt(0) === 0xfeff) s = s.slice(1);

s = s.replace(/\u00C2\u00B7/g, "\u00B7"); // Â· -> ·
s = s.replace(/Legendary .{1,8}" \+ category \+ " Pok[^\"]*"/g, 'Legendary · " + category + " Pokémon"');
// em dash mojibake variants
s = s.replace(/â€”/g, "\u2014");
s = s.replace(/Ã¢â‚¬â€/g, "\u2014");
s = s.replace(/Ã¢â‚¬â€/g, "\u2014");

// Ensure categoryLine body is exact
s = s.replace(
  /if \(isLegendary\(\)\) \{\s*return Component\.literal\("[^"]*" \+ category \+ "[^"]*"\);\s*\}/,
  'if (isLegendary()) {\n            return Component.literal("Legendary · " + category + " Pokémon");\n        }'
);
s = s.replace(
  /return Component\.literal\(category \+ " Pok[^\"]*"\);/,
  'return Component.literal(category + " Pokémon");'
);

s = s.replace(/^[ \t]+key = switch \(key\) \{/m, "        key = switch (key) {");

fs.writeFileSync(msPath, s, "utf8");
console.log("MonSpecies fixed, starts with:", JSON.stringify(s.slice(0, 40)));
const leg = s.split("\n").filter((l) => l.includes("Legendary") && l.includes("category"));
console.log("legendary lines:", leg);

const langPath =
  "C:/Users/conne/Desktop/pixelmontest/fakemons/src/main/resources/assets/fakemon/lang/en_us.json";
let lb = fs.readFileSync(langPath);
if (lb[0] === 0xef && lb[1] === 0xbb && lb[2] === 0xbf) lb = lb.subarray(3);
let ls = lb.toString("utf8");
ls = ls.replace(/Ã¢â‚¬Â¦/g, "\u2026");
ls = ls.replace(/â€¦/g, "\u2026");
ls = ls.replace(/â€”/g, "\u2014");
fs.writeFileSync(langPath, ls, "utf8");
console.log("lang fixed");
