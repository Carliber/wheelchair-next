package wheelchair.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Staff page -> server: write the WHOLE staff profile (GuiStaffPage sends it
 * on every change). Values are clamped server-side (StaffProfile.apply).
 */
public class PacketStaffCfg implements IMessage {
    public boolean silk;
    public int fortune;
    public int topMode;
    public int range;
    public int shape;
    public int mode;
    public int time; // 加速时长(秒) -1=永久

    public PacketStaffCfg() {}

    public PacketStaffCfg(boolean silk, int fortune, int topMode, int range, int shape, int mode, int time) {
        this.silk = silk;
        this.fortune = fortune;
        this.topMode = topMode;
        this.range = range;
        this.shape = shape;
        this.mode = mode;
        this.time = time;
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

    public static class Handler implements IMessageHandler<PacketStaffCfg, IMessage> {
        @Override
        public IMessage onMessage(final PacketStaffCfg msg, final MessageContext ctx) {
            ServerTaskQueue.add(new Runnable() {
                public void run() {
                    try {
                        net.minecraft.entity.player.EntityPlayerMP p = ctx.getServerHandler().playerEntity;
                        wheelchair.value.StaffProfile.get(p.worldObj)
                                .apply(msg.silk, msg.fortune, msg.topMode, msg.range, msg.shape, msg.mode, msg.time);
                    } catch (Throwable t) {
                        cpw.mods.fml.common.FMLLog.warning("[wheelchair] staff cfg failed: %s",
                                new Object[]{String.valueOf(t)});
                    }
                }
            });
            return null;
        }
    }
}
