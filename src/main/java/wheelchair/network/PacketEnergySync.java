package wheelchair.network;

import java.util.ArrayList;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * S→C energy-mark mirror (discriminator 8): the full mark list + pool balance.
 * Two consumers: the world highlight renderer and the energy page GUI. Pushed
 * on GUI open and after every mark change; the client mirror is a plain static
 * list (read on the client thread after the ClientTaskQueue hop).
 */
public class PacketEnergySync implements IMessage {

    public long pool;
    /** {dim, x, y, z, store, eu, rate, nameKey} — nameKey is the block's
     *  unlocalized name (client translates; server never has the lang) */
    public final ArrayList<Object[]> rows = new ArrayList<Object[]>();

    public PacketEnergySync() {}

    @Override
    public void fromBytes(ByteBuf b) {
        pool = b.readLong();
        int n = b.readShort();
        for (int i = 0; i < n; i++) {
            Object[] r = new Object[8];
            r[0] = Integer.valueOf(b.readInt());
            r[1] = Integer.valueOf(b.readInt());
            r[2] = Integer.valueOf(b.readInt());
            r[3] = Integer.valueOf(b.readInt());
            r[4] = Boolean.valueOf(b.readBoolean());
            r[5] = Boolean.valueOf(b.readBoolean());
            r[6] = Integer.valueOf(b.readInt());
            r[7] = ByteBufUtils.readUTF8String(b);
            rows.add(r);
        }
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeLong(pool);
        b.writeShort(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            Object[] r = rows.get(i);
            b.writeInt(((Integer) r[0]).intValue());
            b.writeInt(((Integer) r[1]).intValue());
            b.writeInt(((Integer) r[2]).intValue());
            b.writeInt(((Integer) r[3]).intValue());
            b.writeBoolean(((Boolean) r[4]).booleanValue());
            b.writeBoolean(((Boolean) r[5]).booleanValue());
            b.writeInt(((Integer) r[6]).intValue());
            ByteBufUtils.writeUTF8String(b, (String) r[7]);
        }
    }

    public static class Handler implements IMessageHandler<PacketEnergySync, IMessage> {
        @Override
        public IMessage onMessage(final PacketEnergySync msg, MessageContext ctx) {
            wheelchair.client.ClientTaskQueue.add(new Runnable() {
                public void run() {
                    wheelchair.network.EnergyMirror.accept(msg);
                }
            });
            return null;
        }
    }
}
