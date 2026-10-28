package wheelchair.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import wheelchair.WheelchairMod;
import io.netty.buffer.ByteBuf;

/**
 * Client asks the server to open a GUI. guiId 0 = storage page; rows = grid
 * rows the client computed from its canvas (AE2-style adaptive row count,
 * carried in the openGui x parameter). Opening server-side is essential:
 * only then does the container get a real windowId and slot clicks sync.
 */
public class PacketOpenTerminal implements IMessage {
    public int guiId = 0;
    public int rows = 3;

    public PacketOpenTerminal() {}
    public PacketOpenTerminal(int guiId, int rows) { this.guiId = guiId; this.rows = rows; }

    @Override public void fromBytes(ByteBuf buf) { guiId = buf.readByte(); rows = buf.readByte(); }
    @Override public void toBytes(ByteBuf buf) { buf.writeByte(guiId); buf.writeByte(rows); }

    public static class Handler implements IMessageHandler<PacketOpenTerminal, IMessage> {
        @Override
        public IMessage onMessage(final PacketOpenTerminal msg, final MessageContext ctx) {
            ServerTaskQueue.add(new Runnable() {
                public void run() {
                    net.minecraft.entity.player.EntityPlayerMP p = ctx.getServerHandler().playerEntity;
                    if (msg.guiId != 0 && msg.guiId != 4 && msg.guiId != 5 && msg.guiId != 6) return; // storage + ench + staff + energy
                    int rows = msg.rows;
                    if (rows < 3) rows = 3;
                    if (rows > 10) rows = 10;
                    p.openGui(WheelchairMod.instance, msg.guiId, p.worldObj, rows, 0, 0);
                }
            });
            return null;
        }
    }
}
