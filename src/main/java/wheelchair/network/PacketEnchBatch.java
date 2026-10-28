package wheelchair.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import wheelchair.value.EnchCost;
import wheelchair.value.StorageData;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Batch enchantment apply (EPlus-style single-commit model, verbatim from the
 * old tree): the client stages a whole "name -> target level" table (level 0 =
 * remove) and submits it in ONE packet. The server recomputes every price from
 * EnchCost, checks the balance against the TOTAL, filters mutually-exclusive
 * entries and either applies all of it or nothing — never a partial charged
 * state.
 *
 * Protocol: force(byte) count(byte, max 64) then per entry: len(byte, max 128)
 * + name bytes + level(byte, 0..MAX_LEVEL_CAP). force=1 keeps entries that are
 * incompatible with what is already on the item (cheat semantics: the page
 * shows them red, the player decides).
 */
public class PacketEnchBatch implements IMessage {
    public boolean force;
    public final List<String> names = new ArrayList<String>();
    public final List<Integer> levels = new ArrayList<Integer>();

    public PacketEnchBatch() {}

    @Override
    public void fromBytes(ByteBuf buf) {
        names.clear();
        levels.clear();
        force = buf.readByte() != 0;
        int n = buf.readByte() & 0xFF;
        if (n < 0 || n > 64) return; // hostile packet: clamp to empty
        for (int i = 0; i < n; i++) {
            int len = buf.readByte() & 0xFF;
            if (len < 0 || len > 128) return;
            byte[] b = new byte[len];
            buf.readBytes(b);
            names.add(new String(b, Charset.forName("UTF-8")));
            levels.add(Integer.valueOf(buf.readByte() & 0xFF));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(force ? 1 : 0);
        int n = Math.min(64, names.size());
        buf.writeByte(n);
        for (int i = 0; i < n; i++) {
            byte[] b = names.get(i).getBytes(Charset.forName("UTF-8"));
            buf.writeByte(b.length);
            buf.writeBytes(b);
            buf.writeByte(levels.get(i).intValue());
        }
    }

    public static class Handler implements IMessageHandler<PacketEnchBatch, IMessage> {
        @Override
        public IMessage onMessage(final PacketEnchBatch msg, final MessageContext ctx) {
            ServerTaskQueue.add(new Runnable() { public void run() { handle(msg, ctx); } });
            return null;
        }

        private void handle(PacketEnchBatch msg, MessageContext ctx) {
            try {
                EntityPlayerMP p = ctx.getServerHandler().playerEntity;
                if (!(p.openContainer instanceof wheelchair.inventory.ContainerEnch)) return;
                wheelchair.inventory.ContainerEnch c =
                        (wheelchair.inventory.ContainerEnch) p.openContainer;
                ItemStack item = c.target.getStackInSlot(0);
                if (item == null) {
                    p.addChatMessage(new ChatComponentText("放入物品先"));
                    return;
                }
                StorageData st = StorageData.get(p.worldObj);

                // 1. resolve + clamp + drop no-ops
                Map<Enchantment, Integer> staged = new HashMap<Enchantment, Integer>();
                for (int i = 0; i < msg.names.size(); i++) {
                    Enchantment e = EnchCost.byName(msg.names.get(i));
                    int lvl = msg.levels.get(i).intValue();
                    if (e == null || lvl < 0 || lvl > EnchCost.MAX_LEVEL_CAP) continue;
                    int cur = c.currentLevel(e);
                    if (lvl == cur) continue; // unchanged: no charge, no entry
                    staged.put(e, Integer.valueOf(lvl));
                }
                if (staged.isEmpty()) {
                    p.addChatMessage(new ChatComponentText("没有可应用的变更"));
                    return;
                }

                // 2. mutual-exclusion filter: a staged enchant conflicts with an
                //    EXISTING one on the item that is NOT being changed away
                int skipped = 0;
                if (!msg.force) {
                    Map<Integer, Integer> onItem = EnchantmentHelper.getEnchantments(item);
                    List<Enchantment> drop = new ArrayList<Enchantment>();
                    for (Enchantment e : staged.keySet()) {
                        for (Map.Entry<Integer, Integer> en : onItem.entrySet()) {
                            if (en.getValue().intValue() <= 0 || en.getKey().intValue() == e.effectId) continue;
                            Enchantment other = en.getKey().intValue() >= 0
                                    && en.getKey().intValue() < Enchantment.enchantmentsList.length
                                    ? Enchantment.enchantmentsList[en.getKey().intValue()] : null;
                            if (other != null && !other.canApplyTogether(e)) {
                                drop.add(e);
                                skipped++;
                                break;
                            }
                        }
                    }
                    // staged-vs-staged conflicts too (e.g. Protection + Fire Protection)
                    for (Enchantment a : staged.keySet()) {
                        if (drop.contains(a)) continue;
                        for (Enchantment b : staged.keySet()) {
                            if (a == b || drop.contains(b)) continue;
                            if (!a.canApplyTogether(b)) {
                                drop.add(a);
                                skipped++;
                                break;
                            }
                        }
                    }
                    for (Enchantment e : drop) staged.remove(e);
                }
                if (staged.isEmpty()) {
                    p.addChatMessage(new ChatComponentText("互斥附魔被跳过(Shift应用=强制)"));
                    return;
                }

                // 3. total price over ALL changes: all-or-nothing
                long total = 0;
                for (Map.Entry<Enchantment, Integer> en : staged.entrySet()) {
                    Enchantment e = en.getKey();
                    int lvl = en.getValue().intValue();
                    int cur = c.currentLevel(e);
                    if (lvl > cur) {
                        total += EnchCost.xpPoints(e, lvl, cur, item);
                    }
                    // lvl < cur = removal / downgrade: free (same as the old stepper)
                }
                if (st.countRes("xp") < total) {
                    p.addChatMessage(new ChatComponentText("经验不够(需" + total + "点)"));
                    return;
                }

                // 4. commit: one map write, one charge, one broadcast
                if (total > 0) st.takeRes("xp", total);
                Map<Integer, Integer> m = new HashMap<Integer, Integer>(EnchantmentHelper.getEnchantments(item));
                for (Map.Entry<Enchantment, Integer> en : staged.entrySet()) {
                    Enchantment e = en.getKey();
                    int lvl = en.getValue().intValue();
                    if (lvl <= 0) m.remove(Integer.valueOf(e.effectId));
                    else m.put(Integer.valueOf(e.effectId), Integer.valueOf(lvl));
                }
                EnchCost.setAllEnchants(item, m);
                c.detectAndSendChanges();
                if (total > 0 || skipped == 0) StorageNet.broadcast(p, st, "xp");
                p.addChatMessage(new ChatComponentText("应用 " + staged.size() + " 项改动"
                        + (total > 0 ? ", 花费 " + total + "点" : "")
                        + (skipped > 0 ? ", 跳过 " + skipped + " 项互斥" : "")));
            } catch (Throwable t) {
                cpw.mods.fml.common.FMLLog.warning("[wheelchair] ench batch failed: %s",
                        new Object[]{String.valueOf(t)});
            }
        }
    }
}
