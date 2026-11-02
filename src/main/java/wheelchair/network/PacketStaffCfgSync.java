package wheelchair.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Server -> client: push the live staff profile when the staff page opens (so
 * the GUI starts from the truth, not the static defaults). Written into
 * StaffProfile's client mirror by the ClientTaskQueue.
 */
public class PacketStaffCfgSync implements IMessage {
    public boolean silk;
    public int fortune;
    public int topMode;
    public int range;
    public int shape;
    public int mode;
    public int time;

    public PacketStaffCfgSync() {}

    public PacketStaffCfgSync(wheelchair.value.StaffProfile p) {
        silk = p.silk;
        fortune = p.fortune;
        topMode = p.topMode;
        range = p.range;
        shape = p.shape;
        mode = p.mode;
        time = p.time;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        silk = buf.readByte() != 0;
        fortune = buf.readByte() & 0xFF;
        topMode = buf.readByte();
        range = buf.readByte();
        shape = buf.readByte();
        mode = buf.readByte();
        time = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(silk ? 1 : 0);
        buf.writeByte(fortune);
        buf.writeByte(topMode);
        buf.writeByte(range);
        buf.writeByte(shape);
        buf.writeByte(mode);
        buf.writeInt(time);
    }

    public static class Handler implements IMessageHandler<PacketStaffCfgSync, IMessage> {
        @Override
        public IMessage onMessage(final PacketStaffCfgSync msg, final MessageContext ctx) {
            wheelchair.client.ClientTaskQueue.add(new Runnable() {
                public void run() {
                    wheelchair.value.StaffProfile.cSilk = msg.silk;
                    wheelchair.value.StaffProfile.cFortune = msg.fortune;
                    wheelchair.value.StaffProfile.cTopMode = msg.topMode;
                    wheelchair.value.StaffProfile.cRange = msg.range;
                    wheelchair.value.StaffProfile.cShape = msg.shape;
                    wheelchair.value.StaffProfile.cMode = msg.mode;
                    wheelchair.value.StaffProfile.cTime = msg.time;
                    wheelchair.value.StaffProfile.cVersion++; 
                }
            });
            return null;
        }
    }
}
