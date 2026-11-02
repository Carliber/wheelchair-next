// Regenerate the compat mixin configs + late loader from the CURRENT source
// tree minus ref/compat-fail-list.txt (files excluded from the jar build).
const fs = require('fs');
const path = require('path');
const ROOT = path.resolve(__dirname, '..');
const SRC = path.join(ROOT, 'src/compat/java/wheelchair/compat');

const fail = new Set(
  fs.readFileSync(path.join(ROOT, 'ref/compat-fail-list.txt'), 'utf8')
    .split('\n').map(s => s.trim()).filter(Boolean)
    .map(s => s.replace(/\\/g, '/'))
);
const VAN = ['net.minecraft.', 'org.spongepowered.', 'java.', 'wheelchair.', 'cpw.mods.', 'net.minecraftforge.', 'org.lwjgl.'];

const files = [];
(function w(d) {
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    if (e.isDirectory()) w(d + '/' + e.name);
    else if (e.name.endsWith('.java') && !e.name.includes('$')) files.push(d + '/' + e.name);
  }
})(SRC);

const early = [], late = [];
for (const p of files) {
  if (fail.has(p.replace(/\\/g, '/'))) continue;
  const src = fs.readFileSync(p, 'utf8');
  if (!/@Mixin/.test(src)) continue;
  const external = [...src.matchAll(/^import ([\w.]+);/gm)].some(m => !VAN.some(v => m[1].trim().startsWith(v)));
  // declared package from the package line (physical path has an infinistack segment)
  const pkgm = /^package ([\w.]+);/m.exec(src);
  const pkg = pkgm ? pkgm[1] : '';
  const rel = pkg.replace(/^wheelchair\.compat\./, '') + '.' + p.split('/').pop().replace(/\.java$/, '');
  const isClient = /\/client\//.test(p) || /Client|NEI|Render|Font/.test(p.split('/').pop()) || rel.startsWith('client.');
  (external ? late : early).push({ rel: rel.replace(/^mixins\./, ''), isClient });
}

const base = {
  required: true, minVersion: '0.4.10',
  package: 'wheelchair.compat.mixins',
  plugin: 'wheelchair.mixin.WheelchairMixinPlugin',
  target: '1.7.10', compatibilityLevel: 'JAVA_8',
};
const earlyJson = { ...base, mixins: early.filter(e => !e.isClient).map(e => e.rel), client: early.filter(e => e.isClient).map(e => e.rel) };
const lateJson = { required: true, minVersion: '0.7.11', package: 'wheelchair.compat.mixins', compatibilityLevel: 'JAVA_8' };
fs.writeFileSync(path.join(ROOT, 'src/main/resources/mixins.wheelchair.compat.json'), JSON.stringify(earlyJson, null, 2));
fs.writeFileSync(path.join(ROOT, 'src/main/resources/mixins.wheelchair.compat.late.json'), JSON.stringify(lateJson, null, 2));

const arr = late.map(e => '"' + e.rel + '"').join(',\n            ');
const loader = `package wheelchair;

import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Late-applied mod-compat mixins (ported from InfiniStack, MIT). Mod classes
 * must be on the classpath before these mixins parse. @Pseudo mixins skip
 * absent targets on their own, so we apply the whole list unconditionally.
 */
@LateMixin
public class CompatLateMixin implements ILateMixinLoader {

    private static final String[] LATE = {
            ${arr}
    };

    @Override
    public String getMixinConfig() {
        return "mixins.wheelchair.compat.late.json";
    }

    @Override
    @Nonnull
    public List<String> getMixins(Set<String> loadedMods) {
        return new ArrayList<String>(Arrays.asList(LATE));
    }
}
`;
fs.writeFileSync(path.join(ROOT, 'src/compat/java/wheelchair/CompatLateMixin.java'), loader);
console.log('early:', earlyJson.mixins.length + earlyJson.client.length, 'late:', late.length);
