import fs from "node:fs";
import path from "node:path";

const root = path.resolve(import.meta.dirname, "../frontend/src/locales");
const languages = ["es", "ca"];

function flatten(value, prefix = "") {
  const keys = {};
  for (const [key, child] of Object.entries(value)) {
    const full = prefix ? `${prefix}.${key}` : key;
    if (child && typeof child === "object" && !Array.isArray(child)) {
      Object.assign(keys, flatten(child, full));
    } else {
      keys[full] = child;
    }
  }
  return keys;
}

const catalogs = {};
for (const language of languages) {
  const file = path.join(root, language, "translation.json");
  catalogs[language] = flatten(JSON.parse(fs.readFileSync(file, "utf8")));
}

const reference = Object.keys(catalogs.es).sort();
let failed = false;

for (const language of languages) {
  const keys = Object.keys(catalogs[language]).sort();
  const missing = reference.filter((key) => !(key in catalogs[language]));
  const extra = keys.filter((key) => !(key in catalogs.es));
  const empty = keys.filter((key) => {
    const value = catalogs[language][key];
    return typeof value === "string" && value.trim() === "";
  });
  if (missing.length || extra.length || empty.length) {
    failed = true;
    console.error(`[${language}] missing: ${missing.join(", ") || "-"}`);
    console.error(`[${language}] extra: ${extra.join(", ") || "-"}`);
    console.error(`[${language}] empty: ${empty.join(", ") || "-"}`);
  } else {
    console.log(`[${language}] ${keys.length} keys OK`);
  }
}

if (failed) {
  process.exit(1);
}
