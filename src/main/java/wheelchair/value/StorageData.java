package wheelchair.value;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * World-wide digital storage: one entry per DISTINCT item type with a long count
 * (AE2-cell idea taken to "infinite"): data grows with the number of TYPES,
 * never with the number of items, so NBT size is structurally bounded.
 *
 * Layered persistence (user-requested isolation of NBT items):
 * - THIS file keeps only uid -> count (plain, tiny, corruption-proof)
 * - NBT prototypes live in a SEPARATE WorldSavedData (StorageProtos) with a
 *   per-item cap (compressed bytes) and a global budget
 * - legacy inline "p" compounds in old entries are migrated on load
 *
 * Safety bounds:
 * - prototype NBT over MAX_PROTO_BYTES (compressed, default 4 KB) is refused
 * - total prototype bytes over PROTO_BUDGET_BYTES (default 1 MB) refuses NEW types
 * - hard cap of 100000 distinct types (counts + prototypes)
 */
public class StorageData extends WorldSavedData {
    public static final String NAME = "wheelchair_storage";
    private static final int MAX_TYPES = 100000;

    /** configurable limits (set from wheelchair.cfg in preInit) */
    public static int MAX_PROTO_BYTES = 4 * 1024;
    public static long PROTO_BUDGET_BYTES = 1024 * 1024;

    /** uid -> count (insertion order kept for a stable grid) */
    public final LinkedHashMap<String, Long> counts = new LinkedHashMap<String, Long>();
    /**
     * Non-item resources (ExtraCells2 fluid/gas-cell + ThaumicEnergistics essentia-cell
     * idea: per-type long counts). uid prefixes: fluid: gas: aspect: eu rf mana
     */
    public final LinkedHashMap<String, Long> resources = new LinkedHashMap<String, Long>();

    /** separate prototype store; wired in get(world) */
    public StorageProtos protoStore;

    /** old-format prototypes read from inline "p" — moved into the store on load */
    private final Map<String, ItemStack> legacyProtos = new LinkedHashMap<String, ItemStack>();

    public StorageData() { super(NAME); }
    public StorageData(String name) { super(name); }

    public static StorageData get(World world) {
        // perWorldStorage: per-dimension MapStorage (1.7.10 field; loadData = 1.7.10 getOrLoadData)
        StorageData d = (StorageData) world.perWorldStorage.loadData(StorageData.class, NAME);
        if (d == null) {
            d = new StorageData();
            world.perWorldStorage.setData(NAME, d);
        }
        d.protoStore = StorageProtos.get(world);
        d.migrateLegacyProtos();
        return d;
    }

    /** move old inline prototypes into the separate store, then forget them */
    private void migrateLegacyProtos() {
        if (legacyProtos.isEmpty() || protoStore == null) return;
        for (Map.Entry<String, ItemStack> e : legacyProtos.entrySet()) {
            if (!protoStore.has(e.getKey())) {
                long size = StorageProtos.measuredSize(e.getValue());
                if (size >= 0) protoStore.put(e.getKey(), e.getValue(), size);
            }
        }
        legacyProtos.clear();
        markDirty();
        protoStore.markDirty();
    }

    /** unique key for a stack: item + damage (+ strong nbt hash when present).
     *  SHA-256 over the COMPRESSED nbt bytes — no hashCode collisions. */
    public static String uidOf(ItemStack s) {
        String base = PriceRegistry.key(s.getItem()) + "@" + s.getItemDamage();
        if (s.hasTagCompound()) {
            base += "~" + nbtKey(s);
        }
        return base;
    }

    private static String nbtKey(ItemStack s) {
        try {
            NBTTagCompound t = new NBTTagCompound();
            s.writeToNBT(t);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(t, bos);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bos.toByteArray());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) sb.append(String.format("%02x", digest[i]));
            return sb.toString();
        } catch (Throwable ex) {
            return Integer.toHexString(s.getTagCompound().hashCode()); // legacy fallback
        }
    }

    public long count(String uid) {
        Long v = counts.get(uid);
        return v == null ? 0L : v.longValue();
    }

    /** rebuild ONE prototype stack from a uid (NBT proto first, then modid:name@meta);
     *  shared by virtual furnace / feeder / restocker. stackSize == 1. */
    public ItemStack protoStack(String uid) {
        if (uid == null) return null;
        if (protoStore != null) {
            ItemStack p = protoStore.get(uid);
            if (p != null) {
                ItemStack c = p.copy();
                c.stackSize = 1;
                return c;
            }
        }
        int at = uid.indexOf('@');
        if (at <= 0) return null;
        try {
            String key = uid.substring(0, at);
            int til = uid.indexOf('~');
            int meta = Integer.parseInt(uid.substring(at + 1, til > 0 ? til : uid.length()));
            net.minecraft.item.Item it = PriceRegistry.getItem(key);
            if (it == null) return null;
            ItemStack s = new ItemStack(it, 1, meta);
            return s.getItem() == null ? null : s;
        } catch (Throwable t) {
            return null;
        }
    }

    public long countRes(String uid) {
        Long v = resources.get(uid);
        return v == null ? 0L : v.longValue();
    }

    /** add a non-item resource amount (mB / EU / RF / mana / essentia points).
     *  @return true when stored; false = silently NOT stored (type cap reached) —
     *  callers must handle this or resources vanish without a trace */
    public boolean addRes(String uid, long amount) {
        if (uid == null || amount <= 0) return false;
        if (!resources.containsKey(uid) && counts.size() + resources.size() >= MAX_TYPES) {
            cpw.mods.fml.common.FMLLog.warning("[wheelchair] addRes dropped uid=%s: type cap reached",
                    new Object[]{uid});
            return false;
        }
        long v = countRes(uid) + amount;
        if (v < 0) v = Long.MAX_VALUE;
        resources.put(uid, Long.valueOf(v));
        markDirty();
        return true;
    }

    /** take up to amount of a resource; returns the amount actually taken */
    public long takeRes(String uid, long amount) {
        long have = countRes(uid);
        long take = Math.min(have, Math.max(0, amount));
        if (take <= 0) return 0;
        // journey mode: hand the resource out WITHOUT draining the stored count.
        // Covers every withdrawal path (ModCompat.withdrawInto containers, ench
        // point spend, /wheelchair giveres -uid) because they all go through here.
        if (wheelchair.WheelchairMod.journeyMode) return take;
        long left = have - take;
        if (left <= 0) resources.remove(uid);
        else resources.put(uid, Long.valueOf(left));
        markDirty();
        return take;
    }

    /** returns error message or null on success */
    public String deposit(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null; // nothing
        long size = -1; // measured once, reused for the protoStore.put below
        if (stack.hasTagCompound()) {
            size = StorageProtos.measuredSize(stack);
            if (size < 0) return "\u8be5\u7269\u54c1NBT\u5f02\u5e38, \u65e0\u6cd5\u5165\u5e93";
            if (size > MAX_PROTO_BYTES) {
                return "\u8be5\u7269\u54c1NBT\u8fc7\u5927(" + (size / 1024) + "KB>\u4e0a\u9650" + (MAX_PROTO_BYTES / 1024) + "KB), \u65e0\u6cd5\u5165\u5e93";
            }
            if (protoStore != null && !protoStore.has(uidOf(stack))
                    && protoStore.totalBytes + size > PROTO_BUDGET_BYTES) {
                return "NBT\u7269\u54c1\u603b\u91cf\u5df2\u8fbe\u4e0a\u9650(" + (PROTO_BUDGET_BYTES / 1024) + "KB), \u65e0\u6cd5\u65b0\u589e\u7c7b\u578b";
            }
        }
        String uid = uidOf(stack);
        if (!counts.containsKey(uid) && counts.size() + resources.size() >= MAX_TYPES) {
            return "\u5b58\u50a8\u7269\u54c1\u79cd\u7c7b\u5df2\u8fbe\u4e0a\u9650";
        }
        long v = count(uid) + stack.stackSize;
        if (v < 0) v = Long.MAX_VALUE; // overflow guard
        counts.put(uid, Long.valueOf(v));
        if (stack.hasTagCompound() && protoStore != null && !protoStore.has(uid)) {
            protoStore.put(uid, stack, Math.max(0, size)); // size measured once above
        }
        markDirty();
        return null;
    }

    /** withdraw up to amount; returns the withdrawn count */
    public long withdraw(String uid, long amount) {
        long have = count(uid);
        long take = Math.min(have, Math.max(0, amount));
        if (take <= 0) return 0;
        // journey mode: hand items out WITHOUT draining the stored count. NBT /
        // enchanted items carry the '~' hash in their uid (see uidOf) and stay
        // drained unless their own switch journeyKeepNbt is on as well.
        if (wheelchair.WheelchairMod.journeyMode
                && (wheelchair.WheelchairMod.journeyKeepNbt || uid.indexOf('~') < 0)) {
            return take;
        }
        long left = have - take;
        if (left <= 0) {
            counts.remove(uid);
            if (protoStore != null) protoStore.remove(uid);
        } else {
            counts.put(uid, Long.valueOf(left));
        }
        markDirty();
        return take;
    }

    /** display stack for the grid */
    public ItemStack protoOf(String uid) {
        ItemStack p = protoStore != null ? protoStore.get(uid) : null;
        if (p != null) return p;
        if (legacyProtos.containsKey(uid)) return legacyProtos.get(uid).copy();
        // reconstruct plain item from uid "modid:name@damage~hash"
        try {
            String core = uid.contains("~") ? uid.substring(0, uid.indexOf('~')) : uid;
            int at = core.lastIndexOf('@');
            String key = at < 0 ? core : core.substring(0, at);
            int dmg = at < 0 ? 0 : Integer.parseInt(core.substring(at + 1));
            net.minecraft.item.Item it = PriceRegistry.getItem(key);
            if (it == null) return null;
            ItemStack s = new ItemStack(it, 1, dmg);
            return s;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        counts.clear();
        legacyProtos.clear();
        resources.clear();
        NBTTagList list = nbt.getTagList("entries", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            try {
                NBTTagCompound e = list.getCompoundTagAt(i);
                String uid = e.getString("u");
                long c = e.getLong("c");
                if (uid != null && uid.length() > 0 && c > 0) counts.put(uid, Long.valueOf(c));
                if (e.hasKey("p")) {
                    // legacy inline prototype — migrated to StorageProtos in get()
                    ItemStack p = ItemStack.loadItemStackFromNBT(e.getCompoundTag("p"));
                    if (p != null) legacyProtos.put(uid, p);
                }
            } catch (Throwable ignored) {
                // one bad entry must not kill the counts file
            }
        }
        NBTTagList res = nbt.getTagList("res", 10);
        for (int i = 0; i < res.tagCount(); i++) {
            try {
                NBTTagCompound e = res.getCompoundTagAt(i);
                String uid = e.getString("u");
                long c = e.getLong("c");
                if (c > 0) resources.put(uid, Long.valueOf(c));
            } catch (Throwable ignored) {}
        }
        // 2026-10-20 单一 RF 账本: 老 eu 存量 ×4 并入 rf (公约 1EU=4RF), eu 键删除.
        // 幂等: 并入后 eu 键不再存在, 重读不重复加.
        Long eu = resources.remove("eu");
        if (eu != null && eu.longValue() > 0) {
            long rf = eu.longValue() * wheelchair.WheelchairMod.euToRf;
            Long old = resources.get("rf");
            resources.put("rf", Long.valueOf((old == null ? 0L : old.longValue()) + rf));
            markDirty();
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        List<String> uids = new ArrayList<String>(counts.keySet());
        for (String uid : uids) {
            NBTTagCompound e = new NBTTagCompound();
            e.setString("u", uid);
            e.setLong("c", counts.get(uid).longValue());
            // NOTE: no inline "p" anymore — prototypes live in StorageProtos
            list.appendTag(e);
        }
        nbt.setTag("entries", list);
        NBTTagList res = new NBTTagList();
        for (Map.Entry<String, Long> en : resources.entrySet()) {
            NBTTagCompound e = new NBTTagCompound();
            e.setString("u", en.getKey());
            e.setLong("c", en.getValue().longValue());
            res.appendTag(e);
        }
        nbt.setTag("res", res);
    }
}
