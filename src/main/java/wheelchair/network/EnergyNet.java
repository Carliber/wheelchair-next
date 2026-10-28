package wheelchair.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * Energy net helpers: build + broadcast the mark mirror (PacketEnergySync).
 */
public final class EnergyNet {

    private EnergyNet() {}

    /** full push to everyone (mark lists are global, mirrors are global too) */
    public static void broadcast(wheelchair.value.EnergyMarkHub hub, World anyWorld) {
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv == null) return;
        wheelchair.value.StorageData pool =
                wheelchair.value.StorageData.get(srv.worldServers[0]);
        PacketEnergySync p = new PacketEnergySync();
        p.pool = pool.countRes("rf");
        for (int i = 0; i < hub.marks.size(); i++) {
            wheelchair.value.EnergyMarkHub.Mark m = hub.marks.get(i);
            String name = "?";
            World w = m.dim == 0 ? srv.worldServers[0] : srv.worldServerForDimension(m.dim);
            if (w != null && w.blockExists(m.x, m.y, m.z)) {
                name = w.getBlock(m.x, m.y, m.z).getUnlocalizedName() + ".name";
            }
            p.rows.add(new Object[]{
                    Integer.valueOf(m.dim), Integer.valueOf(m.x), Integer.valueOf(m.y),
                    Integer.valueOf(m.z), Boolean.valueOf(m.store), Boolean.valueOf(m.eu),
                    Integer.valueOf(m.rate), name});
        }
        wheelchair.WheelchairMod.network.sendToAll(p);
    }
}
