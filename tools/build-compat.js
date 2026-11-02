// Rebuild libs/wheelchair-compat.jar from src/compat/java (ported InfiniStack, MIT).
// Files with still-missing compile deps are EXCLUDED and recorded in
// ref/compat-fail-list.txt — drop the missing jars into compat-libs/ and rerun.
const fs = require('fs');
const path = require('path');
const cp = require('child_process');
const os = require('os');
const ROOT = path.resolve(__dirname, '..');

const cache = path.join(os.homedir(), '.gradle', 'caches');
const sep = process.platform === 'win32' ? ';' : ':';
const libs = [
  path.join(cache, 'minecraft/net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/forgeBin-1.7.10-10.13.4.1614-1.7.10.jar'),
  path.join(cache, 'modules-2/files-2.1/org.lwjgl.lwjgl/lwjgl/2.9.1/f58c5aabcef0e41718a564be9f8e412fff8db847/lwjgl-2.9.1.jar'),
  path.join(cache, 'modules-2/files-2.1/com.google.code.findbugs/jsr305/2.0.1/516c03b21d50a644d538de0f0369c620989cd8f0/jsr305-2.0.1.jar'),
  path.join(cache, 'modules-2/files-2.1/org.apache.logging.log4j/log4j-api/2.0-beta9/1dd66e68cccd907880229f9e2de1314bd13ff785/log4j-api-2.0-beta9.jar'),
  path.join(cache, 'modules-2/files-2.1/io.netty/netty-all/4.0.10.Final/9e50bd52ffe257a0e2cd8d971688d6ce7d174325/netty-all-4.0.10.Final.jar'),
  path.join(ROOT, 'libs/unimixins-all-1.7.10-0.2.1.jar'),
];
for (const j of fs.readdirSync(path.join(ROOT, 'compat-libs'))) {
  if (j.endsWith('.jar')) libs.push(path.join(ROOT, 'compat-libs', j));
}
const CPOPT = libs.join(sep);

const all = [];
(function walk(d) {
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    if (e.isDirectory()) walk(d + '/' + e.name);
    else if (e.name.endsWith('.java')) all.push(d + '/' + e.name);
  }
})(path.join(ROOT, 'src/compat/java').replace(/\\/g, '/'));

const outDir = path.join(ROOT, 'build/compat-classes');
fs.rmSync(outDir, { recursive: true, force: true });
fs.mkdirSync(outDir, { recursive: true });

const listFile = path.join(os.tmpdir(), 'compat-srcs.txt');
function tryCompile(srcs) {
  fs.writeFileSync(listFile, srcs.join('\n'), 'ascii');
  try {
    cp.execFileSync('javac', [
      '-J-Duser.language=en', '-proc:none', '-source', '1.8', '-target', '1.8',
      '-encoding', 'UTF-8', '-nowarn', '-d', outDir, '-cp', CPOPT, '@' + listFile,
    ], { stdio: ['ignore', 'pipe', 'pipe'], timeout: 600000, maxBuffer: 64 * 1024 * 1024 });
    return null;
  } catch (e) {
    return (e.stdout || '') + '\n' + (e.stderr || '');
  }
}

let active = all, excluded = [];
for (let round = 0; round < 15 && active.length; round++) {
  const err = tryCompile(active);
  if (!err) break;
  const missing = new Map();
  for (const m of err.matchAll(/^((?:[A-Za-z]:)?[^:\n]+\.java):(\d+): error: package ([\w.]+) does not exist/gm)) {
    if (!missing.has(m[1])) missing.set(m[1], m[3]);
  }
  if (!missing.size) {
    console.log('unresolvable errors (not missing packages):');
    console.log(err.split('\n').slice(0, 12).join('\n'));
    process.exit(1);
  }
  const bad = [...missing.keys()].map(f => f.replace(/\\/g, '/'));
  console.log(`round ${round}: ${new Set([...missing.values()]).size} pkgs missing -> excluding ${bad.length} files (active was ${active.length})`);
  excluded.push(...bad);
  const before = active.length;
  active = active.filter(f => !bad.includes(f));
  if (active.length === before) { console.log('filter removed NOTHING — path mismatch, dumping samples:'); console.log('bad[0]:', bad[0]); console.log('all sample:', all.find(f => f.includes(path.basename(bad[0], '.java')))); break; }
}
const err = tryCompile(active);
if (err) {
  console.log('FINAL COMPILE FAILED:');
  console.log(err.split('\n').slice(0, 12).join('\n'));
  process.exit(1);
}
excluded = [...new Set(excluded)];
fs.writeFileSync(path.join(ROOT, 'ref/compat-fail-list.txt'), excluded.map(f => f.replace(ROOT + path.sep, '').replace(/\\/g, '/')).sort().join('\n') + '\n');
cp.execSync(`jar cf "${path.join(ROOT, 'libs/wheelchair-compat.jar')}" .`, { cwd: outDir });
console.log(`OK: ${active.length} compiled, ${excluded.length} excluded (ref/compat-fail-list.txt)`);
