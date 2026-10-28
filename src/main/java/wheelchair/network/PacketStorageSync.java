package wheelchair.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import wheelchair.value.StorageData;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Server -> client storage snapshot. full=true sends the entire item map + the
 * resource map (on open), otherwise a single uid delta.
 *
 * NBT is synced as the stored stack's COMPRESSED NBT — the client rebuilds the
 * REAL ItemStack, so custom names, enchantments, durability and every mod's
 * tooltip lines come along for free (AE2 AESharedNBT idea).
 */
public class PacketStorageSync implements IMessage {
    public boolean full;
    public String uid = "";
    public long count;
    /** full payload: uid -> {count, damage, hasProto} */
    public Map<String, long[]> entries = new HashMap<String, long[]>();
    /** full payload: uid -> compressed NBT of the stored prototype */
    public Map<String, byte[]> nbt = new HashMap<String, byte[]>();
    /** full payload: resource uid -> amount */
    public Map<String, Long> resEntries = new HashMap<String, Long>();

    /** per-entry NBT cap: bigger stacks fall back to the plain item display */
    private static final int MAX_SYNC_NBT = 8192;

    public PacketStorageSync() {}
    public PacketStorageSync(boolean full, StorageData st) {
        this.full = full;
        if (full) {
            for (Map.Entry<String, Long> e : st.counts.entrySet()) {
                entries.put(e.getKey(), new long[]{e.getValue().longValue(), damageOf(st, e.getKey()), 0});
                byte[] nb = nbtOf(st, e.getKey());
                if (nb != null) nbt.put(e.getKey(), nb);
            }
            for (Map.Entry<String, Long> e : st.resources.entrySet()) {
                resEntries.put(e.getKey(), Long.valueOf(e.getValue().longValue()));
            }
        }
    }

    /** single-item delta */
    public PacketStorageSync(String uid, long count) {
        this.full = false;
        this.uid = uid;
        this.count = count;
    }

    private static int damageOf(StorageData st, String uid) {
        net.minecraft.item.ItemStack p = st.protoStore != null ? st.protoStore.protos.get(uid) : null;
        if (p != null) return p.getItemDamage();
        try {
            String core = uid.contains("~") ? uid.substring(0, uid.indexOf('~')) : uid;
            int at = core.lastIndexOf('@');
            return at < 0 ? 0 : Integer.parseInt(core.substring(at + 1));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static byte[] nbtOf(StorageData st, String uid) {
        try {
            net.minecraft.item.ItemStack p = st.protoStore != null ? st.protoStore.protos.get(uid) : null;
            if (p == null || !p.hasTagCompound()) return null;
            net.minecraft.nbt.NBTTagCompound t = new net.minecraft.nbt.NBTTagCompound();
            p.writeToNBT(t);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            net.minecraft.nbt.CompressedStreamTools.writeCompressed(t, bos);
            if (bos.size() > MAX_SYNC_NBT) return null;
            return bos.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        full = buf.readBoolean();
        if (full) {
            int n = buf.readInt();
            for (int i = 0; i < n; i++) {
                String u = readUid(buf);
                long c = buf.readLong();
                long d = buf.readInt();
                long hp = buf.readByte();
                entries.put(u, new long[]{c, d, hp});
                int blen = buf.readShort();
                if (blen > 0) {
                    byte[] nb = new byte[blen];
                    buf.readBytes(nb);
                    nbt.put(u, nb);
                }
            }
            int rn = buf.readInt();
            for (int i = 0; i < rn; i++) {
                String u = readUid(buf);
                resEntries.put(u, Long.valueOf(buf.readLong()));
            }
        } else {
            uid = readUid(buf);
            count = buf.readLong();
        }
    }

    private static String readUid(ByteBuf buf) {
        int len = buf.readShort();
        if (len < 0 || len > 256) len = 0; // hostile/malformed guard
        byte[] b = new byte[len];
        buf.readBytes(b);
        return new String(b, Charset.forName("UTF-8"));
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(full);
        if (full) {
            buf.writeInt(entries.size());
            for (Map.Entry<String, long[]> e : entries.entrySet()) {
                writeUid(buf, e.getKey());
                buf.writeLong(e.getValue()[0]);
                buf.writeInt((int) e.getValue()[1]);
                buf.writeByte((int) e.getValue()[2]);
                byte[] nb = nbt.get(e.getKey());
                buf.writeShort(nb == null ? 0 : nb.length);
                if (nb != null) buf.writeBytes(nb);
            }
            buf.writeInt(resEntries.size());
            for (Map.Entry<String, Long> e : resEntries.entrySet()) {
                writeUid(buf, e.getKey());
                buf.writeLong(e.getValue().longValue());
            }
        } else {
            writeUid(buf, uid);
            buf.writeLong(count);
        }
    }

    private static void writeUid(ByteBuf buf, String s) {
        byte[] b = s.getBytes(Charset.forName("UTF-8"));
        buf.writeShort(b.length);
        buf.writeBytes(b);
    }

    public static class Handler implements IMessageHandler<PacketStorageSync, IMessage> {
        @Override
        public IMessage onMessage(final PacketStorageSync msg, MessageContext ctx) {
            // 1.7.10 simpleimpl calls handlers on the netty thread; hop to the client thread
            wheelchair.client.ClientTaskQueue.add(new Runnable() {
                public void run() {
                    if (msg.full) wheelchair.client.StorageMirror.setFull(msg.entries, msg.nbt, msg.resEntries);
                    else wheelchair.client.StorageMirror.update(msg.uid, msg.count);
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
                    if (mc.currentScreen instanceof wheelchair.client.GuiStoragePage) {
                        ((wheelchair.client.GuiStoragePage) mc.currentScreen).onStorageUpdate();
                    }
                }
            });
            return null;
        }
    }
}
