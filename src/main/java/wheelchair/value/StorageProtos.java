package wheelchair.value;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SEPARATE storage for NBT item prototypes (user-requested isolation):
 * - its own WorldSavedData file on disk ("wheelchair_storage_protos.dat"),
 *   so a bloated/corrupt prototype file can be deleted WITHOUT losing counts
 * - every entry is size-accounted (compressed bytes); a global budget
 *   (StorageData.PROTO_BUDGET_BYTES, default 1 MB) rejects new NBT types when full
 * - loading is per-entry fault tolerant: one corrupt proto never kills the file
 */
public class StorageProtos extends WorldSavedData {
    public static final String NAME = "wheelchair_storage_protos";

    /** uid -> prototype (insertion order kept) */
    public final LinkedHashMap<String, ItemStack> protos = new LinkedHashMap<String, ItemStack>();
    /** sum of compressed serialized sizes — the budget metric */
    public long totalBytes = 0;

    public StorageProtos() { super(NAME); }
    public StorageProtos(String name) { super(name); }

    public static StorageProtos get(World world) {
        StorageProtos d = (StorageProtos) world.perWorldStorage.loadData(StorageProtos.class, NAME);
        if (d == null) {
            d = new StorageProtos();
            world.perWorldStorage.setData(NAME, d);
        }
        return d;
    }

    public boolean has(String uid) { return protos.containsKey(uid); }

    /** measured compressed size of the stack's serialized NBT, or -1 on error */
    public static long measuredSize(ItemStack s) {
        try {
            NBTTagCompound t = new NBTTagCompound();
            s.writeToNBT(t);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(t, bos);
            return bos.size();
        } catch (Throwable ex) {
            return -1;
        }
    }

    public void put(String uid, ItemStack proto, long size) {
        ItemStack old = protos.get(uid);
        if (old != null) {
            long os = measuredSize(old);
            if (os > 0) totalBytes = Math.max(0, totalBytes - os);
        }
        protos.put(uid, proto.copy());
        totalBytes += Math.max(0, size);
        markDirty();
    }

    public ItemStack remove(String uid) {
        ItemStack gone = protos.remove(uid);
        if (gone != null) {
            // MUST give the budget back (put() does the same on replace): the old code
            // only deleted the entry, so each store-withdraw cycle leaked its size and
            // the budget filled up while the warehouse was actually empty
            long os = measuredSize(gone);
            if (os > 0) totalBytes = Math.max(0, totalBytes - os);
            markDirty();
        }
        return gone;
    }

    public ItemStack get(String uid) {
        ItemStack p = protos.get(uid);
        return p == null ? null : p.copy();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        protos.clear();
        totalBytes = 0;
        NBTTagList list = nbt.getTagList("protos", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            try {
                NBTTagCompound e = list.getCompoundTagAt(i);
                String uid = e.getString("u");
                if (uid == null || uid.length() == 0) continue;
                ItemStack p = ItemStack.loadItemStackFromNBT(e.getCompoundTag("p"));
                if (p == null) continue; // corrupt entry: skip, never abort the file
                long size = e.hasKey("s") ? Math.max(0, e.getLong("s")) : Math.max(0, measuredSize(p));
                protos.put(uid, p);
                totalBytes += size;
            } catch (Throwable ignored) {
                // one bad entry must not kill the rest
            }
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, ItemStack> en : protos.entrySet()) {
            try {
                NBTTagCompound e = new NBTTagCompound();
                e.setString("u", en.getKey());
                NBTTagCompound p = new NBTTagCompound();
                en.getValue().writeToNBT(p);
                e.setTag("p", p);
                e.setLong("s", Math.max(0, measuredSize(en.getValue())));
                list.appendTag(e);
            } catch (Throwable ignored) {
                // unserializable proto: skip it, keep the rest of the file valid
            }
        }
        nbt.setTag("protos", list);
    }
}
