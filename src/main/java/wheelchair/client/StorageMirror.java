package wheelchair.client;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import wheelchair.value.PriceRegistry;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side mirror of the storage (fed by PacketStorageSync). Items are kept
 * as uid -> {count, damage, compressed NBT}; display ItemStacks are rebuilt
 * lazily the AE2 way: real stacks, so custom names / enchants / tooltips all
 * come along for free.
 */
public class StorageMirror {
    /** uid -> {count, damage, hasProto(1/0)} (insertion order kept: creative-order sort + stable grid) */
    public static final Map<String, long[]> items = new LinkedHashMap<String, long[]>();
    /** uid -> compressed NBT of the stored prototype (only for NBT items) */
    public static final Map<String, byte[]> nbt = new LinkedHashMap<String, byte[]>();
    /** resource uid -> amount (rf, fluid:..., xp ...) */
    public static final Map<String, Long> res = new LinkedHashMap<String, Long>();
    /** uid -> rebuilt display stack (cache; invalidated on update) */
    private static final Map<String, ItemStack> cache = new HashMap<String, ItemStack>();

    public static void clear() {
        items.clear();
        nbt.clear();
        res.clear();
        cache.clear();
    }

    public static void setFull(Map<String, long[]> entries, Map<String, byte[]> nb,
                               Map<String, Long> resEntries) {
        clear();
        items.putAll(entries);
        if (nb != null) nbt.putAll(nb);
        if (resEntries != null) res.putAll(resEntries);
    }

    public static void update(String uid, long count) {
        if (count <= 0) {
            items.remove(uid);
            nbt.remove(uid);
            cache.remove(uid);
        } else if (items.containsKey(uid)) {
            items.get(uid)[0] = count;
        } else {
            // delta for a type we have no metadata for: damage 0, no proto —
            // request-safe fallback, the next full sync fixes it up
            items.put(uid, new long[]{count, 0, 0});
        }
    }

    public static long count(String uid) {
        long[] m = items.get(uid);
        if (m != null) return m[0];
        Long r = res.get(uid);
        return r == null ? 0L : r.longValue();
    }

    /** rebuilt display stack or null; caller must NOT mutate the result (shared cache) */
    public static ItemStack stackOf(String uid) {
        if (cache.containsKey(uid)) return cache.get(uid);
        ItemStack s = rebuild(uid);
        cache.put(uid, s);
        return s;
    }

    private static ItemStack rebuild(String uid) {
        try {
            byte[] nb = nbt.get(uid);
            if (nb != null) {
                net.minecraft.nbt.NBTTagCompound t = CompressedStreamTools.readCompressed(
                        new ByteArrayInputStream(nb));
                ItemStack s = ItemStack.loadItemStackFromNBT(t);
                if (s != null) return s;
            }
            long[] m = items.get(uid);
            int dmg = m == null ? 0 : (int) m[1];
            String core = uid.contains("~") ? uid.substring(0, uid.indexOf('~')) : uid;
            int at = core.lastIndexOf('@');
            String key = at < 0 ? core : core.substring(0, at);
            if (at >= 0) {
                try { dmg = Integer.parseInt(core.substring(at + 1)); } catch (Exception ignored) {}
            }
            net.minecraft.item.Item it = PriceRegistry.getItem(key);
            if (it == null) return null;
            return new ItemStack(it, 1, dmg);
        } catch (Throwable t) {
            return null;
        }
    }
}
