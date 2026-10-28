package wheelchair.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.util.ForgeDirection;
import wheelchair.value.StaffDigTask;
import wheelchair.value.StaffProfile;

/**
 * Staff chain-dig request (client onBlockStartBreak -> server). The server
 * snapshots the profile, builds the shape/mode producer and starts a
 * frame-sliced StaffDigTask: no block cap, stops when the player MOVES, drops
 * go straight into storage (see StaffDigTask).
 */
public class PacketStaffDig implements IMessage {
    public int x, y, z;

    public PacketStaffDig() {}
    public PacketStaffDig(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }

    @Override public void fromBytes(ByteBuf buf) { x = buf.readInt(); y = buf.readInt(); z = buf.readInt(); }
    @Override public void toBytes(ByteBuf buf) { buf.writeInt(x); buf.writeInt(y); buf.writeInt(z); }

    public static class Handler implements IMessageHandler<PacketStaffDig, IMessage> {
        @Override
        public IMessage onMessage(final PacketStaffDig msg, final MessageContext ctx) {
            ServerTaskQueue.add(new Runnable() { public void run() { handle(msg, ctx); } });
            return null;
        }

        private void handle(PacketStaffDig msg, MessageContext ctx) {
            try {
                EntityPlayerMP p = ctx.getServerHandler().playerEntity;
                net.minecraft.world.World w = p.worldObj;
                // sanity: the clicked block must be within reach-ish of the player
                double d2 = (msg.x + 0.5 - p.posX) * (msg.x + 0.5 - p.posX)
                        + (msg.y + 0.5 - p.posY) * (msg.y + 0.5 - p.posY)
                        + (msg.z + 0.5 - p.posZ) * (msg.z + 0.5 - p.posZ);
                if (d2 > 64.0 * 64.0) return;
                if (w.getBlock(msg.x, msg.y, msg.z).isAir(w, msg.x, msg.y, msg.z)) return;

                StaffProfile cfg = StaffProfile.get(w);
                StaffProfile snap = new StaffProfile("snapshot");
                snap.silk = cfg.silk;
                snap.fortune = cfg.fortune;
                snap.topMode = cfg.topMode;
                snap.range = cfg.range;
                snap.shape = cfg.shape;
                snap.mode = cfg.mode;

                if (snap.topMode != 1) { // 普通/电力模式: 单块挖掘
                    // single block: base profile still applies (silk/fortune/max
                    // level), but drops go to the PLAYER, not storage
                    java.util.List<int[]> one = new java.util.ArrayList<int[]>();
                    one.add(new int[]{msg.x, msg.y, msg.z});
                    StaffDigTask.start(p,
                            new StaffDigTask.ListCells(w, one, msg.x, msg.y, msg.z, false), snap, false);
                    return;
                }
                StaffDigTask.start(p,
                        StaffDigTask.producer(w, msg.x, msg.y, msg.z, snap, faceOf(p, msg)), snap, true);
            } catch (Throwable t) {
                cpw.mods.fml.common.FMLLog.warning("[wheelchair] staff dig failed: %s",
                        new Object[]{String.valueOf(t)});
            }
        }

        /** approximate clicked face from the player's position relative to the block */
        private static ForgeDirection faceOf(EntityPlayerMP p, PacketStaffDig msg) {
            double dx = msg.x + 0.5 - p.posX;
            double dy = msg.y + 0.5 - (p.posY + p.getEyeHeight());
            double dz = msg.z + 0.5 - p.posZ;
            double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
            if (ay >= ax && ay >= az) return dy > 0 ? ForgeDirection.DOWN : ForgeDirection.UP;
            if (ax >= az) return dx > 0 ? ForgeDirection.WEST : ForgeDirection.EAST;
            return dz > 0 ? ForgeDirection.NORTH : ForgeDirection.SOUTH;
        }
    }
}
