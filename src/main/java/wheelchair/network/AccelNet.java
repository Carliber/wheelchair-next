package wheelchair.network;

import net.minecraft.server.MinecraftServer;

/**
 * Accel net helpers: build + broadcast the accel-mark mirror (PacketAccelSync).
 * Row shape: {isEntity, dim, x, y, z, entityId, radius, rate, remainTicks,
 * cov, nameKey} — nameKey unlocalized for blocks, plain text for entities
 * (client renders as-is).
 */
public final class AccelNet {

    private AccelNet() {}

    public static void broadcast(wheelchair.value.AccelHub hub) {
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv == null) return;
        PacketAccelSync p = new PacketAccelSync();
        p.on = hub.on;
        for (int i = 0; i < hub.marks.size(); i++) {
            wheelchair.value.AccelHub.Mark m = hub.marks.get(i);
            String name = "";
            net.minecraft.world.World w = m.dim == 0 ? srv.worldServers[0]
                    : srv.worldServerForDimension(m.dim);
            if (m.isEntity) {
                net.minecraft.entity.Entity e = w == null ? null : w.getEntityByID(m.entityId);
                if (e != null) name = e.getCommandSenderName();
            } else if (w != null && w.blockExists(m.x, m.y, m.z)) {
                name = w.getBlock(m.x, m.y, m.z).getUnlocalizedName() + ".name";
            }
            p.rows.add(new Object[]{
                    Boolean.valueOf(m.isEntity), Integer.valueOf(m.dim),
                    Integer.valueOf(m.x), Integer.valueOf(m.y), Integer.valueOf(m.z),
                    Integer.valueOf(m.entityId), Integer.valueOf(m.radius), Integer.valueOf(m.rate),
                    Long.valueOf(m.remain), Integer.valueOf(m.cov), name});
        }
        wheelchair.WheelchairMod.network.sendToAll(p);
    }
}
