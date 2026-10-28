package wheelchair.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import wheelchair.value.StorageData;
import wheelchair.value.XpMath;

/**
 * Enchant-page xp actions (prototype v26: the 存/取 buttons next to the apply
 * button). op 0 = deposit, 1 = withdraw; n 0 = ONE level, 1 = ALL. Everything
 * is recomputed server-side from the live player fields — experienceTotal is
 * never trusted (see XpMath).
 */
public class PacketEnchAction implements IMessage {
    public int op;
    public int n;

    public PacketEnchAction() {}
    public PacketEnchAction(int op, int n) { this.op = op; this.n = n; }

    @Override public void fromBytes(ByteBuf buf) { op = buf.readByte(); n = buf.readByte(); }
    @Override public void toBytes(ByteBuf buf) { buf.writeByte(op); buf.writeByte(n); }

    public static class Handler implements IMessageHandler<PacketEnchAction, IMessage> {
        @Override
        public IMessage onMessage(final PacketEnchAction msg, final MessageContext ctx) {
            ServerTaskQueue.add(new Runnable() { public void run() { handle(msg, ctx); } });
            return null;
        }

        private void handle(PacketEnchAction msg, MessageContext ctx) {
            try {
                EntityPlayerMP p = ctx.getServerHandler().playerEntity;
                StorageData st = StorageData.get(p.worldObj);
                if (msg.op == 0) { // deposit
                    long got = msg.n == 1 ? XpMath.depositAll(p, st) : XpMath.depositOneLevel(p, st);
                    if (got > 0) StorageNet.broadcast(p, st, "xp");
                    else p.addChatMessage(new ChatComponentText("没有可存的经验"));
                    return;
                }
                if (msg.op == 1) { // withdraw
                    long have = st.countRes("xp");
                    int granted = XpMath.withdrawLevels(p, st, msg.n == 1 ? 1000 : 1);
                    if (granted > 0 && st.countRes("xp") != have) StorageNet.broadcast(p, st, "xp");
                    if (granted == 0) p.addChatMessage(new ChatComponentText("存储经验不够升一级"));
                    return;
                }
            } catch (Throwable t) {
                cpw.mods.fml.common.FMLLog.warning("[wheelchair] ench action failed: %s",
                        new Object[]{String.valueOf(t)});
            }
        }
    }
}
