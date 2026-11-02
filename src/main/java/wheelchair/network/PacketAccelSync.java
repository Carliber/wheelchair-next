package wheelchair.network;

import java.util.ArrayList;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;

/**
 * S→C accel-mark mirror (discriminator 10): the full mark list + master
 * switch. Consumers: the accel page GUI and the world renderer (block-mark
 * boxes). Pushed on GUI open and after every change.
 */
public class PacketAccelSync implements IMessage {

    public boolean on;
    /** {isEntity, dim, x, y, z, entityId, radius, rate, remain, cov, nameKey} */
    public final ArrayList<Object[]> rows = new ArrayList<Object[]>();

    public PacketAccelSync() {}

    @Override
    public void fromBytes(ByteBuf b) {
        on = b.readBoolean();
        int n = b.readShort();
        for (int i = 0; i < n; i++) {
            Object[] r = new Object[11];
            r[0] = Boolean.valueOf(b.readBoolean());
            r[1] = Integer.valueOf(b.readInt());
            r[2] = Integer.valueOf(b.readInt());
            r[3] = Integer.valueOf(b.readInt());
            r[4] = Integer.valueOf(b.readInt());
            r[5] = Integer.valueOf(b.readInt());
            r[6] = Integer.valueOf(b.readByte());
            r[7] = Integer.valueOf(b.readShort()); // rate max 256 — byte would wrap to -128/0
            r[8] = Long.valueOf(b.readLong());
            r[9] = Integer.valueOf(b.readInt());
            r[10] = ByteBufUtils.readUTF8String(b);
            rows.add(r);
        }
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeBoolean(on);
        b.writeShort(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Object[] r = rows.get(i);
            b.writeBoolean(((Boolean) r[0]).booleanValue());
            b.writeInt(((Integer) r[1]).intValue());
            b.writeInt(((Integer) r[2]).intValue());
            b.writeInt(((Integer) r[3]).intValue());
            b.writeInt(((Integer) r[4]).intValue());
            b.writeInt(((Integer) r[5]).intValue());
            b.writeByte(((Integer) r[6]).intValue());
            b.writeShort(((Integer) r[7]).intValue());
            b.writeLong(((Long) r[8]).longValue());
            b.writeInt(((Integer) r[9]).intValue());
            ByteBufUtils.writeUTF8String(b, (String) r[10]);
        }
    }

    public static class Handler implements IMessageHandler<PacketAccelSync, IMessage> {
        @Override
        public IMessage onMessage(final PacketAccelSync msg, MessageContext ctx) {
            wheelchair.client.ClientTaskQueue.add(new Runnable() {
                public void run() {
                    wheelchair.network.AccelMirror.accept(msg);
                }
            });
            return null;
        }
    }
}
