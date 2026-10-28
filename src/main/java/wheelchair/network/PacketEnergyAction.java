package wheelchair.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * C→S energy page action (discriminator 9). op 0 = unmark [X] (dim/x/y/z).
 * Server removes the mark and re-broadcasts the mirror.
 */
public class PacketEnergyAction implements IMessage {

    public byte op;
    public int dim, x, y, z;

    public PacketEnergyAction() {}

    public PacketEnergyAction(int dim, int x, int y, int z) {
        op = 0;
        this.dim = dim; this.x = x; this.y = y; this.z = z;
    }

    @Override
    public void fromBytes(ByteBuf b) {
        op = b.readByte();
        dim = b.readInt(); x = b.readInt(); y = b.readInt(); z = b.readInt();
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeByte(op);
        b.writeInt(dim); b.writeInt(x); b.writeInt(y); b.writeInt(z);
    }

    public static class Handler implements IMessageHandler<PacketEnergyAction, IMessage> {
        @Override
        public IMessage onMessage(final PacketEnergyAction m, MessageContext ctx) {
            final net.minecraft.entity.player.EntityPlayerMP p = ctx.getServerHandler().playerEntity;
            wheelchair.network.ServerTaskQueue.add(new Runnable() {
                public void run() {
                    wheelchair.value.EnergyMarkHub hub = wheelchair.value.EnergyMarkHub.get();
                    if (hub == null) return;
                    if (m.op == 0) {
                        hub.unmark(m.dim, m.x, m.y, m.z);
                        wheelchair.network.EnergyNet.broadcast(hub, p.worldObj);
                    }
                }
            });
            return null;
        }
    }
}
