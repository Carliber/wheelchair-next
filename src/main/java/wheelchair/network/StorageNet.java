package wheelchair.network;

import wheelchair.WheelchairMod;
import wheelchair.value.StorageData;
import net.minecraft.entity.player.EntityPlayerMP;

/** push storage snapshots to the players (delta on action, full on open/deposit-all) */
public class StorageNet {
    /** single-uid delta to everyone (cheap: most actions change one type) */
    public static void broadcast(EntityPlayerMP self, StorageData st, String uid) {
        long count = st.count(uid);
        PacketStorageSync pkt = new PacketStorageSync(uid, count);
        for (Object o : net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            if (o instanceof EntityPlayerMP) {
                WheelchairMod.network.sendTo(pkt, (EntityPlayerMP) o);
            }
        }
    }

    /** full snapshot to everyone */
    public static void broadcastFull(EntityPlayerMP self, StorageData st) {
        for (Object o : net.minecraft.server.MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            if (o instanceof EntityPlayerMP) {
                WheelchairMod.network.sendTo(new PacketStorageSync(true, st), (EntityPlayerMP) o);
            }
        }
    }
}
