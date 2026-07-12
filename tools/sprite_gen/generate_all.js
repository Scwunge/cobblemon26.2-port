/**
 * Generates Gen-style chibi battle sprites for every Fakemon species.
 * Output: 96x96 PNG with transparent background.
 * Usage: node generate_all.js
 */
const fs = require("fs");
const path = require("path");
const { PNG } = require("pngjs");

const SIZE = 96;
const OUT_FRONT = path.resolve(
  __dirname,
  "../../src/main/resources/assets/fakemon/textures/entity/sprites/front"
);
const OUT_BACK = path.resolve(
  __dirname,
  "../../src/main/resources/assets/fakemon/textures/entity/sprites/back"
);
const SPECIES = JSON.parse(fs.readFileSync(path.join(__dirname, "species.json"), "utf8"));

const ELEMENT_ACCENT = {
  FIRE: [232, 93, 4],
  WATER: [0, 119, 182],
  GRASS: [45, 106, 79],
  ELECTRIC: [255, 214, 10],
  FLYING: [144, 224, 239],
  ROCK: [108, 117, 125],
  GROUND: [188, 108, 37],
  DARK: [60, 9, 108],
  DRAGON: [157, 78, 221],
  PSYCHIC: [199, 125, 255],
  FIGHTING: [193, 18, 31],
  ICE: [0, 180, 216],
  STEEL: [141, 153, 174],
  BUG: [106, 153, 78],
  POISON: [155, 93, 229],
  GHOST: [90, 24, 154],
  FAIRY: [255, 175, 204],
  NORMAL: [173, 181, 189],
};

function hexToRgb(hex) {
  const h = hex.replace("#", "");
  return [
    parseInt(h.slice(0, 2), 16),
    parseInt(h.slice(2, 4), 16),
    parseInt(h.slice(4, 6), 16),
  ];
}

function mix(a, b, t) {
  return [
    Math.round(a[0] * (1 - t) + b[0] * t),
    Math.round(a[1] * (1 - t) + b[1] * t),
    Math.round(a[2] * (1 - t) + b[2] * t),
  ];
}

function lighten(c, t = 0.35) {
  return mix(c, [255, 255, 255], t);
}
function darken(c, t = 0.35) {
  return mix(c, [20, 16, 28], t);
}

class Canvas {
  constructor(w, h) {
    this.w = w;
    this.h = h;
    this.data = Buffer.alloc(w * h * 4);
  }
  idx(x, y) {
    return (y * this.w + x) * 4;
  }
  set(x, y, r, g, b, a = 255) {
    x = Math.round(x);
    y = Math.round(y);
    if (x < 0 || y < 0 || x >= this.w || y >= this.h) return;
    const i = this.idx(x, y);
    // alpha composite over existing
    const ea = this.data[i + 3] / 255;
    const na = a / 255;
    const outA = na + ea * (1 - na);
    if (outA <= 0) return;
    this.data[i] = Math.round((r * na + this.data[i] * ea * (1 - na)) / outA);
    this.data[i + 1] = Math.round((g * na + this.data[i + 1] * ea * (1 - na)) / outA);
    this.data[i + 2] = Math.round((b * na + this.data[i + 2] * ea * (1 - na)) / outA);
    this.data[i + 3] = Math.round(outA * 255);
  }
  fill(x0, y0, x1, y1, rgb, a = 255) {
    const [r, g, b] = rgb;
    for (let y = Math.min(y0, y1); y < Math.max(y0, y1); y++) {
      for (let x = Math.min(x0, x1); x < Math.max(x0, x1); x++) {
        this.set(x, y, r, g, b, a);
      }
    }
  }
  oval(cx, cy, rx, ry, fill, outline) {
    const r = Math.ceil(rx);
    const s = Math.ceil(ry);
    for (let y = -s - 1; y <= s + 1; y++) {
      for (let x = -r - 1; x <= r + 1; x++) {
        const nx = x / (rx || 1);
        const ny = y / (ry || 1);
        const d = nx * nx + ny * ny;
        if (d <= 1.0) {
          // soft highlight
          const hi = nx * 0.3 - ny * 0.4;
          let c = fill;
          if (hi > 0.2) c = lighten(fill, 0.22);
          if (hi < -0.35) c = darken(fill, 0.25);
          this.set(cx + x, cy + y, c[0], c[1], c[2], 255);
        } else if (d <= 1.18 && outline) {
          this.set(cx + x, cy + y, outline[0], outline[1], outline[2], 255);
        }
      }
    }
  }
  rect(x, y, w, h, fill, outline) {
    if (outline) {
      this.fill(x - 1, y - 1, x + w + 1, y + h + 1, outline);
    }
    this.fill(x, y, x + w, y + h, fill);
    // top highlight
    this.fill(x, y, x + w, y + Math.max(1, h / 5), lighten(fill, 0.2));
  }
  eyes(hx, hy, head, outline, monocular = false) {
    const eye = [16, 16, 22];
    const shine = [255, 255, 255];
    if (monocular) {
      const ex = hx + head * 0.45;
      const ey = hy + head * 0.42;
      this.oval(ex, ey, head * 0.22, head * 0.22, eye, outline);
      this.oval(ex - 2, ey - 2, 3, 3, shine, null);
      // cyan ring for psychic/grok vibe
      return;
    }
    const ey = hy + head * 0.42;
    const el = hx + head * 0.32;
    const er = hx + head * 0.68;
    const erx = Math.max(2, head * 0.12);
    this.oval(el, ey, erx, erx * 1.1, eye, outline);
    this.oval(er, ey, erx, erx * 1.1, eye, outline);
    this.oval(el - 1, ey - 1, 2, 2, shine, null);
    this.oval(er - 1, ey - 1, 2, 2, shine, null);
  }
  flipH() {
    const next = new Canvas(this.w, this.h);
    for (let y = 0; y < this.h; y++) {
      for (let x = 0; x < this.w; x++) {
        const i = this.idx(x, y);
        const j = next.idx(this.w - 1 - x, y);
        next.data[j] = this.data[i];
        next.data[j + 1] = this.data[i + 1];
        next.data[j + 2] = this.data[i + 2];
        next.data[j + 3] = this.data[i + 3];
      }
    }
    return next;
  }
  write(file) {
    const png = new PNG({ width: this.w, height: this.h });
    this.data.copy(png.data);
    fs.writeFileSync(file, PNG.sync.write(png));
  }
}

function drawSprite(sp) {
  const c = new Canvas(SIZE, SIZE);
  const p = hexToRgb(sp.p);
  const s = hexToRgb(sp.s);
  const o = [26, 20, 32];
  const accent = ELEMENT_ACCENT[sp.element] || p;
  const stage = sp.stage || 1;
  const scale = 0.82 + stage * 0.08; // larger finals
  const pad = Math.round((1 - scale) * SIZE * 0.5);
  const box = SIZE - pad * 2;

  if (sp.special === "grok") {
    drawGrok(c, pad, pad, box, p, s, o, accent, stage);
  } else if (sp.special === "subshah") {
    drawSubshah(c, pad, pad, box, p, s, o, accent);
  } else {
    switch (sp.shape) {
      case "BIPED":
        drawBiped(c, pad, pad, box, p, s, o, accent, stage);
        break;
      case "AVIAN":
        drawAvian(c, pad, pad, box, p, s, o, accent, stage);
        break;
      case "AQUATIC":
        drawAquatic(c, pad, pad, box, p, s, o, accent, stage);
        break;
      case "SERPENT":
        drawSerpent(c, pad, pad, box, p, s, o, accent, stage);
        break;
      case "INSECTOID":
        drawInsect(c, pad, pad, box, p, s, o, accent, stage);
        break;
      case "AMORPH":
        drawAmorph(c, pad, pad, box, p, s, o, accent, stage);
        break;
      case "ARMORED":
        drawArmored(c, pad, pad, box, p, s, o, accent, stage);
        break;
      case "FEY":
        drawFey(c, pad, pad, box, p, s, o, accent, stage);
        break;
      default:
        drawQuad(c, pad, pad, box, p, s, o, accent, stage);
    }
  }

  // type chip (top-right)
  const chip = Math.max(4, Math.floor(box / 8));
  c.fill(SIZE - chip - 3, 3, SIZE - 3, 3 + chip, accent);
  c.fill(SIZE - chip - 2, 4, SIZE - 4, 2 + chip, lighten(accent, 0.3));

  // stage pips bottom-left
  for (let i = 0; i < stage; i++) {
    const px = 3 + i * (chip + 1);
    c.fill(px, SIZE - chip - 3, px + chip - 1, SIZE - 3, o);
    c.fill(px + 1, SIZE - chip - 2, px + chip - 2, SIZE - 4, accent);
  }

  return c;
}

function drawQuad(c, x, y, size, p, s, o, accent, stage) {
  const head = Math.floor(size * 0.55);
  const hx = x + Math.floor((size - head) / 2);
  const hy = y + Math.floor(size * 0.08);
  const bw = Math.floor(size * 0.58);
  const bh = Math.floor(size * 0.36);
  const bx = x + Math.floor((size - bw) / 2);
  const by = y + size - bh - Math.floor(size * 0.1);
  c.oval(bx + bw / 2, by + bh / 2, bw / 2, bh / 2, p, o);
  // legs
  const lw = Math.max(3, Math.floor(size / 9));
  c.rect(bx + 2, by + bh - 2, lw, Math.floor(size * 0.12), s, o);
  c.rect(bx + bw - lw - 2, by + bh - 2, lw, Math.floor(size * 0.12), s, o);
  // head + ears
  c.oval(hx + head / 2, hy + head / 2, head / 2, head / 2, p, o);
  const ew = Math.max(3, Math.floor(size / 8));
  c.rect(hx + 2, hy - ew + 2, ew, ew, s, o);
  c.rect(hx + head - ew - 2, hy - ew + 2, ew, ew, s, o);
  c.fill(hx + 3, hy - ew + 3, hx + ew, hy + 1, accent);
  c.fill(hx + head - ew, hy - ew + 3, hx + head - 3, hy + 1, accent);
  // snout
  c.oval(hx + head / 2, hy + head * 0.62, head * 0.18, head * 0.12, lighten(p, 0.15), o);
  c.eyes(hx, hy, head, o);
  // tail accent
  c.oval(bx + bw + 2, by + bh * 0.3, size * 0.08, size * 0.06, accent, o);
}

function drawBiped(c, x, y, size, p, s, o, accent, stage) {
  const bodyH = Math.floor(size * (0.38 + stage * 0.04));
  const bodyW = Math.floor(size * 0.42);
  const bx = x + Math.floor((size - bodyW) / 2);
  const by = y + Math.floor(size * 0.38);
  c.rect(bx, by, bodyW, bodyH, p, o);
  // chest plate
  c.fill(bx + 3, by + 3, bx + bodyW - 3, by + bodyH * 0.45, lighten(p, 0.15));
  // legs
  const lw = Math.max(4, Math.floor(bodyW / 3));
  c.rect(bx + 1, by + bodyH - 2, lw, Math.floor(size * 0.18), s, o);
  c.rect(bx + bodyW - lw - 1, by + bodyH - 2, lw, Math.floor(size * 0.18), s, o);
  // arms
  c.rect(bx - Math.floor(size * 0.08), by + 4, Math.floor(size * 0.1), Math.floor(size * 0.22), p, o);
  c.rect(bx + bodyW - 2, by + 4, Math.floor(size * 0.1), Math.floor(size * 0.22), p, o);
  // head
  const head = Math.floor(size * 0.42);
  const hx = x + Math.floor((size - head) / 2);
  const hy = y + Math.floor(size * 0.06);
  c.oval(hx + head / 2, hy + head / 2, head / 2, head / 2, p, o);
  // crest
  c.oval(hx + head / 2, hy + 2, head * 0.2, head * 0.12, accent, o);
  c.eyes(hx, hy, head, o);
}

function drawAvian(c, x, y, size, p, s, o, accent, stage) {
  const bodyW = Math.floor(size * 0.4);
  const bodyH = Math.floor(size * 0.32);
  const bx = x + Math.floor((size - bodyW) / 2);
  const by = y + Math.floor(size * 0.4);
  c.oval(bx + bodyW / 2, by + bodyH / 2, bodyW / 2, bodyH / 2, p, o);
  // wings
  const ww = Math.floor(size * (0.28 + stage * 0.04));
  c.oval(bx - ww * 0.3, by + bodyH * 0.3, ww * 0.5, bodyH * 0.35, s, o);
  c.oval(bx + bodyW + ww * 0.3, by + bodyH * 0.3, ww * 0.5, bodyH * 0.35, s, o);
  // head
  const head = Math.floor(size * 0.36);
  const hx = x + Math.floor((size - head) / 2);
  const hy = y + Math.floor(size * 0.12);
  c.oval(hx + head / 2, hy + head / 2, head / 2, head / 2, p, o);
  // beak
  c.oval(hx + head / 2, hy + head * 0.7, head * 0.14, head * 0.1, accent, o);
  // crest feathers
  c.rect(hx + head * 0.4, hy - 4, 3, 8, accent, o);
  c.rect(hx + head * 0.55, hy - 6, 3, 10, s, o);
  c.eyes(hx, hy, head, o);
  // feet
  c.rect(bx + 4, by + bodyH - 1, 4, 6, s, o);
  c.rect(bx + bodyW - 8, by + bodyH - 1, 4, 6, s, o);
}

function drawAquatic(c, x, y, size, p, s, o, accent, stage) {
  const bw = Math.floor(size * 0.62);
  const bh = Math.floor(size * 0.38);
  const bx = x + Math.floor((size - bw) / 2);
  const by = y + Math.floor(size * 0.32);
  c.oval(bx + bw / 2, by + bh / 2, bw / 2, bh / 2, p, o);
  // belly
  c.oval(bx + bw / 2, by + bh * 0.65, bw * 0.28, bh * 0.22, lighten(p, 0.35), o);
  // tail fin
  c.oval(bx + bw + 2, by + bh * 0.4, size * 0.12, size * 0.16, s, o);
  // dorsal
  c.oval(bx + bw * 0.5, by - 2, size * 0.1, size * 0.08, accent, o);
  // head blob
  const head = Math.floor(size * 0.34);
  const hx = bx - 2;
  const hy = by + Math.floor(bh * 0.15);
  c.oval(hx + head / 2, hy + head / 2, head / 2, head / 2, p, o);
  c.eyes(hx, hy, head, o);
  // side fins
  c.oval(bx + bw * 0.35, by + bh * 0.85, size * 0.08, size * 0.05, s, o);
  c.oval(bx + bw * 0.65, by + bh * 0.85, size * 0.08, size * 0.05, s, o);
}

function drawSerpent(c, x, y, size, p, s, o, accent, stage) {
  // coiled S body
  const segs = 5 + stage;
  for (let i = 0; i < segs; i++) {
    const t = i / (segs - 1);
    const cx = x + size * (0.25 + t * 0.5);
    const cy = y + size * (0.55 + Math.sin(t * Math.PI * 2) * 0.18);
    const r = size * (0.12 - t * 0.02);
    c.oval(cx, cy, r, r * 0.85, i % 2 === 0 ? p : s, o);
  }
  // head
  const head = Math.floor(size * 0.32);
  const hx = x + Math.floor(size * 0.12);
  const hy = y + Math.floor(size * 0.22);
  c.oval(hx + head / 2, hy + head / 2, head / 2, head / 2, p, o);
  // horns
  c.rect(hx + 4, hy - 6, 3, 10, accent, o);
  c.rect(hx + head - 8, hy - 8, 3, 12, accent, o);
  c.eyes(hx, hy, head, o);
  // snout
  c.oval(hx + head * 0.2, hy + head * 0.55, head * 0.2, head * 0.12, lighten(p, 0.1), o);
}

function drawInsect(c, x, y, size, p, s, o, accent, stage) {
  const bodyW = Math.floor(size * 0.36);
  const bodyH = Math.floor(size * 0.4);
  const bx = x + Math.floor((size - bodyW) / 2);
  const by = y + Math.floor(size * 0.35);
  c.oval(bx + bodyW / 2, by + bodyH / 2, bodyW / 2, bodyH / 2, p, o);
  // abdomen stripe
  c.fill(bx + 3, by + bodyH * 0.5, bx + bodyW - 3, by + bodyH * 0.65, s);
  // legs
  for (let i = 0; i < 3; i++) {
    const ly = by + 6 + i * 8;
    c.rect(bx - 8, ly, 10, 2, s, o);
    c.rect(bx + bodyW - 2, ly, 10, 2, s, o);
  }
  // head
  const head = Math.floor(size * 0.3);
  const hx = x + Math.floor((size - head) / 2);
  const hy = y + Math.floor(size * 0.12);
  c.oval(hx + head / 2, hy + head / 2, head / 2, head / 2, p, o);
  // antennae
  c.rect(hx + 4, hy - 8, 2, 10, accent, o);
  c.rect(hx + head - 6, hy - 10, 2, 12, accent, o);
  c.oval(hx + 5, hy - 8, 3, 3, accent, o);
  c.oval(hx + head - 5, hy - 10, 3, 3, accent, o);
  c.eyes(hx, hy, head, o);
  if (stage >= 2) {
    // wings
    c.oval(bx - 6, by + 4, size * 0.14, size * 0.2, lighten(accent, 0.4), o);
    c.oval(bx + bodyW + 6, by + 4, size * 0.14, size * 0.2, lighten(accent, 0.4), o);
  }
}

function drawAmorph(c, x, y, size, p, s, o, accent, stage) {
  const cx = x + size / 2;
  const cy = y + size * 0.48;
  c.oval(cx, cy, size * 0.32, size * 0.36, p, o);
  c.oval(cx - size * 0.1, cy - size * 0.08, size * 0.18, size * 0.2, lighten(p, 0.2), null);
  // float blobs
  c.oval(cx + size * 0.28, cy - size * 0.2, size * 0.1, size * 0.1, s, o);
  c.oval(cx - size * 0.3, cy + size * 0.15, size * 0.08, size * 0.08, accent, o);
  if (stage >= 2) {
    // big eye
    c.oval(cx, cy, size * 0.14, size * 0.14, [16, 16, 22], o);
    c.oval(cx - 2, cy - 2, 4, 4, [255, 255, 255], null);
    c.oval(cx, cy, size * 0.06, size * 0.06, accent, null);
  } else {
    c.eyes(cx - size * 0.18, cy - size * 0.18, size * 0.36, o);
  }
}

function drawArmored(c, x, y, size, p, s, o, accent, stage) {
  const bodyW = Math.floor(size * (0.5 + stage * 0.04));
  const bodyH = Math.floor(size * 0.42);
  const bx = x + Math.floor((size - bodyW) / 2);
  const by = y + Math.floor(size * 0.36);
  c.rect(bx, by, bodyW, bodyH, p, o);
  // plates
  c.fill(bx + 2, by + 2, bx + bodyW - 2, by + 8, lighten(p, 0.15));
  c.fill(bx + 4, by + bodyH * 0.35, bx + bodyW - 4, by + bodyH * 0.45, s);
  // legs
  const lw = Math.max(5, Math.floor(bodyW / 3.5));
  c.rect(bx + 2, by + bodyH - 2, lw, Math.floor(size * 0.16), s, o);
  c.rect(bx + bodyW - lw - 2, by + bodyH - 2, lw, Math.floor(size * 0.16), s, o);
  // head helm
  const head = Math.floor(size * 0.38);
  const hx = x + Math.floor((size - head) / 2);
  const hy = y + Math.floor(size * 0.08);
  c.rect(hx, hy, head, head, p, o);
  c.fill(hx + 2, hy + 2, hx + head - 2, hy + 6, lighten(p, 0.2));
  // visor
  c.fill(hx + 4, hy + head * 0.4, hx + head - 4, hy + head * 0.55, [16, 16, 22]);
  c.fill(hx + 6, hy + head * 0.42, hx + head * 0.45, hy + head * 0.52, accent);
  // spikes
  c.rect(hx + head * 0.2, hy - 6, 4, 8, s, o);
  c.rect(hx + head * 0.7, hy - 8, 4, 10, s, o);
}

function drawFey(c, x, y, size, p, s, o, accent, stage) {
  const head = Math.floor(size * 0.48);
  const hx = x + Math.floor((size - head) / 2);
  const hy = y + Math.floor(size * 0.1);
  c.oval(hx + head / 2, hy + head / 2, head / 2, head / 2, p, o);
  // big ears / fins
  c.oval(hx - 4, hy + head * 0.3, size * 0.12, size * 0.18, s, o);
  c.oval(hx + head + 4, hy + head * 0.3, size * 0.12, size * 0.18, s, o);
  // sparkles
  c.oval(hx + head + 8, hy, 3, 3, accent, null);
  c.oval(hx - 6, hy + 4, 2, 2, lighten(accent, 0.4), null);
  // body
  const bw = Math.floor(size * 0.36);
  const bh = Math.floor(size * 0.28);
  const bx = x + Math.floor((size - bw) / 2);
  const by = y + Math.floor(size * 0.55);
  c.oval(bx + bw / 2, by + bh / 2, bw / 2, bh / 2, p, o);
  // ribbon legs
  c.rect(bx + 2, by + bh - 1, 3, 10, accent, o);
  c.rect(bx + bw - 5, by + bh - 1, 3, 10, accent, o);
  c.eyes(hx, hy, head, o);
  // mane tuft
  c.oval(hx + head / 2, hy + 4, head * 0.15, head * 0.1, accent, o);
}

function drawGrok(c, x, y, size, p, s, o, accent, stage) {
  const voidC = p;
  const glow = s;
  if (stage >= 3) {
    // serpentine constellation
    for (let i = 0; i < 6; i++) {
      const t = i / 5;
      const cx = x + size * (0.2 + t * 0.55);
      const cy = y + size * (0.45 + Math.sin(t * Math.PI) * 0.2);
      c.oval(cx, cy, size * 0.11, size * 0.09, voidC, o);
      if (i % 2 === 0) c.oval(cx, cy - 2, 3, 3, glow, null);
    }
    const head = size * 0.34;
    c.oval(x + size * 0.22, y + size * 0.28, head / 2, head / 2, voidC, o);
    c.oval(x + size * 0.22, y + size * 0.28, head * 0.28, head * 0.28, [8, 8, 16], o);
    c.oval(x + size * 0.2, y + size * 0.26, 4, 4, glow, null);
    // gold mane
    c.oval(x + size * 0.22, y + size * 0.12, size * 0.12, size * 0.08, glow, o);
    return;
  }
  if (stage === 2) {
    // biped truthseeker
    const bodyW = size * 0.4;
    const bodyH = size * 0.42;
    const bx = x + (size - bodyW) / 2;
    const by = y + size * 0.38;
    c.rect(bx, by, bodyW, bodyH, voidC, o);
    c.fill(bx + 3, by + 4, bx + bodyW - 3, by + bodyH * 0.4, [20, 18, 40]);
    // data mane
    for (let i = 0; i < 5; i++) {
      c.rect(bx + bodyW * 0.15 + i * 5, by - 10 - (i % 2) * 3, 3, 14, glow, o);
    }
    const head = size * 0.4;
    const hx = x + (size - head) / 2;
    const hy = y + size * 0.08;
    c.oval(hx + head / 2, hy + head / 2, head / 2, head / 2, voidC, o);
    // eye ring
    c.oval(hx + head / 2, hy + head * 0.45, head * 0.2, head * 0.2, glow, o);
    c.oval(hx + head / 2, hy + head * 0.45, head * 0.1, head * 0.1, [10, 10, 18], o);
    c.oval(hx + head / 2 - 2, hy + head * 0.42, 3, 3, [255, 255, 255], null);
    // grin
    c.fill(hx + head * 0.3, hy + head * 0.72, hx + head * 0.7, hy + head * 0.78, glow);
    c.rect(bx + 2, by + bodyH - 1, 6, 12, voidC, o);
    c.rect(bx + bodyW - 8, by + bodyH - 1, 6, 12, voidC, o);
    return;
  }
  // grokling — small void kit
  const head = size * 0.5;
  const hx = x + (size - head) / 2;
  const hy = y + size * 0.15;
  c.oval(hx + head / 2, hy + head / 2, head / 2, head / 2, voidC, o);
  c.oval(hx + head / 2, hy + head * 0.45, head * 0.22, head * 0.22, glow, o);
  c.oval(hx + head / 2, hy + head * 0.45, head * 0.1, head * 0.1, [8, 8, 14], o);
  c.oval(hx + head / 2 - 2, hy + head * 0.42, 3, 3, [255, 255, 255], null);
  // ears
  c.rect(hx + 4, hy - 4, 5, 10, voidC, o);
  c.rect(hx + head - 9, hy - 6, 5, 12, voidC, o);
  // body
  const bw = size * 0.38;
  const bh = size * 0.28;
  const bx = x + (size - bw) / 2;
  const by = y + size * 0.58;
  c.oval(bx + bw / 2, by + bh / 2, bw / 2, bh / 2, voidC, o);
  // spark tail
  c.oval(bx + bw + 4, by + bh * 0.3, 6, 5, glow, o);
  c.rect(bx + 3, by + bh - 1, 4, 8, voidC, o);
  c.rect(bx + bw - 7, by + bh - 1, 4, 8, voidC, o);
}

function drawSubshah(c, x, y, size, p, s, o, accent) {
  // mantis-shrimp silhouette
  const bodyW = size * 0.45;
  const bodyH = size * 0.38;
  const bx = x + (size - bodyW) / 2;
  const by = y + size * 0.4;
  c.oval(bx + bodyW / 2, by + bodyH / 2, bodyW / 2, bodyH / 2, p, o);
  c.oval(bx + bodyW / 2, by + bodyH * 0.7, bodyW * 0.28, bodyH * 0.2, s, o);
  // hood
  c.oval(bx + bodyW / 2, by - 4, bodyW * 0.4, size * 0.12, darken(p, 0.2), o);
  // orbs
  c.oval(bx + 4, by - 8, 6, 6, [240, 200, 80], o);
  c.oval(bx + bodyW - 4, by - 8, 6, 6, [240, 200, 80], o);
  // claws
  c.rect(bx - 10, by + 8, 14, 5, s, o);
  c.rect(bx + bodyW - 4, by + 8, 14, 5, s, o);
  c.rect(bx - 12, by + 6, 6, 10, accent, o);
  c.rect(bx + bodyW + 6, by + 6, 6, 10, accent, o);
  // head/eyes
  const head = size * 0.28;
  const hx = x + (size - head) / 2;
  const hy = y + size * 0.22;
  c.oval(hx + head / 2, hy + head / 2, head / 2, head / 2, p, o);
  c.oval(hx + head * 0.35, hy + head * 0.45, 4, 4, [0, 220, 255], o);
  c.oval(hx + head * 0.65, hy + head * 0.45, 4, 4, [0, 220, 255], o);
  // tentacles
  for (let i = 0; i < 3; i++) {
    c.oval(bx + bodyW * 0.3 + i * 8, by + bodyH + 2, 4, 6, darken(p, 0.15), o);
  }
}

function main() {
  fs.mkdirSync(OUT_FRONT, { recursive: true });
  fs.mkdirSync(OUT_BACK, { recursive: true });
  let n = 0;
  for (const sp of SPECIES) {
    const front = drawSprite(sp);
    const frontPath = path.join(OUT_FRONT, `${sp.id}.png`);
    front.write(frontPath);
    const back = front.flipH();
    // darken back slightly for "back sprite" feel
    for (let i = 0; i < back.data.length; i += 4) {
      if (back.data[i + 3] > 0) {
        back.data[i] = Math.round(back.data[i] * 0.92);
        back.data[i + 1] = Math.round(back.data[i + 1] * 0.92);
        back.data[i + 2] = Math.round(back.data[i + 2] * 0.95);
      }
    }
    back.write(path.join(OUT_BACK, `${sp.id}.png`));
    n++;
    process.stdout.write(`  ${sp.id}\n`);
  }
  // sprite sheet
  const cols = 10;
  const rows = Math.ceil(SPECIES.length / cols);
  const sheet = new Canvas(cols * SIZE, rows * SIZE);
  SPECIES.forEach((sp, i) => {
    const col = i % cols;
    const row = Math.floor(i / cols);
    const png = PNG.sync.read(fs.readFileSync(path.join(OUT_FRONT, `${sp.id}.png`)));
    for (let y = 0; y < SIZE; y++) {
      for (let x = 0; x < SIZE; x++) {
        const si = (y * SIZE + x) * 4;
        const dx = col * SIZE + x;
        const dy = row * SIZE + y;
        sheet.set(dx, dy, png.data[si], png.data[si + 1], png.data[si + 2], png.data[si + 3]);
      }
    }
  });
  const sheetDir = path.resolve(OUT_FRONT, "../sheet");
  fs.mkdirSync(sheetDir, { recursive: true });
  sheet.write(path.join(sheetDir, "fakemon_all_front.png"));
  console.log(`\nDone: ${n} front + ${n} back sprites → ${OUT_FRONT}`);
  console.log(`Sheet: ${path.join(sheetDir, "fakemon_all_front.png")}`);
}

main();
