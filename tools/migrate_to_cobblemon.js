/**
 * Rename Java package ai.xai.fakemon → com.cobblemon.mod
 * Rename classes Fakemon* → Cobblemon*
 * Rewrite string literals fakemon → cobblemon
 */
const fs = require("fs");
const path = require("path");

const ROOT = path.join(__dirname, "..");
const SRC = path.join(ROOT, "src", "main", "java");
const OLD = path.join(SRC, "ai", "xai", "fakemon");
const NEW = path.join(SRC, "com", "cobblemon", "mod");

function walk(dir, acc = []) {
  if (!fs.existsSync(dir)) return acc;
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) walk(p, acc);
    else acc.push(p);
  }
  return acc;
}

function ensureDir(d) {
  fs.mkdirSync(d, { recursive: true });
}

function rewriteJava(text, fileName) {
  let t = text;
  // Packages & imports
  t = t.replace(/ai\.xai\.fakemon/g, "com.cobblemon.mod");
  // Class renames (order matters — longer first)
  t = t.replace(/\bFakemonClient\b/g, "CobblemonClient");
  t = t.replace(/\bFakemonCommands\b/g, "CobblemonCommands");
  t = t.replace(/\bFakemonModel\b/g, "CobblemonModel");
  t = t.replace(/\bFakemon\b/g, "Cobblemon");
  // String / resource ids
  t = t.replace(/"fakemon"/g, '"cobblemon"');
  t = t.replace(/'fakemon'/g, "'cobblemon'");
  t = t.replace(/fakemon:/g, "cobblemon:");
  t = t.replace(/\.fakemon\./g, ".cobblemon.");
  t = t.replace(/itemGroup\.fakemon/g, "itemGroup.cobblemon");
  t = t.replace(/screen\.fakemon\./g, "screen.cobblemon.");
  t = t.replace(/key\.fakemon/g, "key.cobblemon");
  t = t.replace(/key\.categories\.fakemon/g, "key.categories.cobblemon");
  t = t.replace(/Fakemons/g, "Cobblemon");
  t = t.replace(/fakemons/g, "cobblemon");
  // ResourceLocation.of("fakemon:...") already handled by fakemon:
  return t;
}

function renameFile(name) {
  return name
    .replace(/^FakemonClient\.java$/, "CobblemonClient.java")
    .replace(/^FakemonCommands\.java$/, "CobblemonCommands.java")
    .replace(/^FakemonModel\.java$/, "CobblemonModel.java")
    .replace(/^Fakemon\.java$/, "Cobblemon.java");
}

function main() {
  if (!fs.existsSync(OLD)) {
    console.log("Old package not found at", OLD);
    if (fs.existsSync(NEW)) {
      console.log("Already migrated to", NEW);
      // Still rewrite files in place for any leftover strings
      for (const file of walk(NEW)) {
        if (!file.endsWith(".java")) continue;
        const text = fs.readFileSync(file, "utf8");
        const next = rewriteJava(text, path.basename(file));
        if (next !== text) {
          fs.writeFileSync(file, next, "utf8");
          console.log("patched", path.relative(NEW, file));
        }
      }
    }
    return;
  }

  const files = walk(OLD).filter((f) => f.endsWith(".java"));
  console.log("Migrating", files.length, "Java files...");
  for (const file of files) {
    const rel = path.relative(OLD, file);
    const dir = path.dirname(rel);
    const base = renameFile(path.basename(file));
    const dest = path.join(NEW, dir === "." ? "" : dir, base);
    ensureDir(path.dirname(dest));
    const text = fs.readFileSync(file, "utf8");
    fs.writeFileSync(dest, rewriteJava(text, base), "utf8");
  }

  // Remove old tree
  fs.rmSync(path.join(SRC, "ai"), { recursive: true, force: true });
  console.log("Removed", OLD);
  console.log("New package:", NEW);
  console.log("Done.");
}

main();
