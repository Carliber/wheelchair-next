package wheelchair.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import wheelchair.value.StorageData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import java.nio.charset.Charset;

/**
 * Storage actions from the storage page GUI.
 * type 0 = withdraw item (uid; amount 1..64, -1 = everything; toInv = deliver
 *          straight to the inventory instead of the cursor)
 * type 1 = deposit the cursor stack
 * type 3 = fluid cell click with a container on the cursor (uid = fluid:xxx;
 *          filled container deposits its contents, empty one is FILLED from the
 *          network — the server decides, the old-tree/AE2 fluid-terminal way)
 * type 4 = deposit the whole main inventory
 * type 5 = deposit the CONTENTS of the cursor container (right-click grid:
 *          pour the bucket into the network, keep/return the empty container)
 *
 * Withdraw follows QUANTIFY FIRST, MUTATE SECOND: take is bounded by actual
 * stock BEFORE anything is handed out, overflow stays stored, nothing dropped.
 */
public class PacketStorageAction implements IMessage {
    public int type;
    public String uid = "";
    public int amount;
    public boolean toInv;

    public PacketStorageAction() {}
    public PacketStorageAction(int type, String uid, int amount, boolean toInv) {
        this.type = type; this.uid = uid; this.amount = amount; this.toInv = toInv;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = buf.readByte();
        uid = readUid(buf);
        amount = buf.readByte();
        toInv = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(type);
        byte[] b = uid.getBytes(Charset.forName("UTF-8"));
        buf.writeShort(b.length);
        buf.writeBytes(b);
        buf.writeByte(amount);
        buf.writeBoolean(toInv);
    }

    private static String readUid(ByteBuf buf) {
        int len = buf.readShort();
        if (len < 0 || len > 256) { // hostile/malformed packet: uid max ~32 chars
            return "";
        }
        byte[] b = new byte[len];
        buf.readBytes(b);
        return new String(b, Charset.forName("UTF-8"));
    }

    public static class Handler implements IMessageHandler<PacketStorageAction, IMessage> {
        @Override
        public IMessage onMessage(final PacketStorageAction msg, final MessageContext ctx) {
            ServerTaskQueue.add(new Runnable() {
                public void run() { handle(msg, ctx); }
            });
            return null;
        }

        private void handle(PacketStorageAction msg, MessageContext ctx) {
            try {
                final EntityPlayerMP p = ctx.getServerHandler().playerEntity;
                StorageData st = StorageData.get(p.worldObj);

                if (msg.type == 1) { // deposit cursor
                    ItemStack cursor = p.inventory.getItemStack();
                    if (cursor != null) {
                        String err = st.deposit(cursor);
                        if (err != null) {
                            p.addChatMessage(new ChatComponentText(err));
                        } else {
                            p.inventory.setItemStack(null);
                            StorageNet.broadcast(p, st, StorageData.uidOf(cursor));
                        }
                    }
                    p.updateHeldItem();
                    return;
                }

                if (msg.type == 4) { // deposit the whole main inventory
                    int deposited = 0;
                    long kinds = 0;
                    String lastUid = null;
                    for (int i = 0; i < p.inventory.mainInventory.length; i++) {
                        ItemStack s = p.inventory.mainInventory[i];
                        if (s == null || s.getItem() == null) continue;
                        String err = st.deposit(s);
                        if (err == null) {
                            deposited += s.stackSize;
                            kinds++;
                            lastUid = StorageData.uidOf(s);
                            p.inventory.mainInventory[i] = null;
                        }
                    }
                    p.updateHeldItem();
                    if (deposited > 0) {
                        StorageNet.broadcastFull(p, st);
                        p.addChatMessage(new ChatComponentText(
                                "一键存入: " + deposited + " 个物品 (" + kinds + " 种)"));
                    } else if (lastUid == null) {
                        p.addChatMessage(new ChatComponentText("背包是空的"));
                    }
                    cpw.mods.fml.common.FMLLog.info("[wheelchair] deposit-all player=%s items=%s kinds=%s",
                            new Object[]{p.getCommandSenderName(), Integer.valueOf(deposited), Long.valueOf(kinds)});
                    return;
                }

                if (msg.type == 5) { // pour the cursor container's contents into the network
                    ItemStack cursor = p.inventory.getItemStack();
                    if (cursor != null && cursor.getItem() == net.minecraft.init.Items.experience_bottle) {
                        // bottle o' enchanting: 8 xp each (vanilla rolls 3+rand(5)+rand(5),
                        // EV 8), the whole stack goes in, glass bottles come back
                        if (!st.addRes("xp", 8L * cursor.stackSize)) {
                            p.addChatMessage(new ChatComponentText("\u5b58\u50a8\u7c7b\u578b\u5df2\u6ee1"));
                            return;
                        }
                        int bottles = cursor.stackSize;
                        p.inventory.setItemStack(new ItemStack(net.minecraft.init.Items.glass_bottle, bottles));
                        p.updateHeldItem();
                        StorageNet.broadcastFull(p, st);
                        return;
                    }
                    if (cursor == null || !net.minecraftforge.fluids.FluidContainerRegistry.isFilledContainer(cursor)) {
                        p.addChatMessage(new ChatComponentText("\u624b\u91cc\u4e0d\u662f\u88c5\u6ee1\u7684\u6d41\u4f53\u5bb9\u5668"));
                        return;
                    }
                    net.minecraftforge.fluids.FluidStack fs =
                            net.minecraftforge.fluids.FluidContainerRegistry.getFluidForFilledItem(cursor);
                    if (fs == null || fs.getFluid() == null) return;
                    String uid = "fluid:" + fs.getFluid().getName();
                    if (!st.addRes(uid, fs.amount)) {
                        p.addChatMessage(new ChatComponentText("\u5b58\u50a8\u7c7b\u578b\u5df2\u6ee1, \u65e0\u6cd5\u5165\u5e93"));
                        return;
                    }
                    ItemStack empty = net.minecraftforge.fluids.FluidContainerRegistry.drainFluidContainer(cursor);
                    if (empty == null) {
                        cursor.stackSize--;
                        p.inventory.setItemStack(cursor.stackSize <= 0 ? null : cursor);
                    } else {
                        p.inventory.setItemStack(empty);
                    }
                    p.updateHeldItem();
                    StorageNet.broadcastFull(p, st); // fluid lives in resources: full sync (no res delta yet)
                    return;
                }

                if (msg.type == 3) { // fluid cell click: server decides fill vs pour
                    String uid = msg.uid;
                    if (uid == null || !uid.startsWith("fluid:")) return;
                    net.minecraftforge.fluids.Fluid fluid =
                            net.minecraftforge.fluids.FluidRegistry.getFluid(uid.substring(6));
                    if (fluid == null) return;
                    int want = net.minecraftforge.fluids.FluidContainerRegistry.BUCKET_VOLUME;
                    net.minecraftforge.fluids.FluidStack wantFs =
                            new net.minecraftforge.fluids.FluidStack(fluid, want);
                    ItemStack cursor = p.inventory.getItemStack();
                    if (cursor != null && net.minecraftforge.fluids.FluidContainerRegistry.isFilledContainer(cursor)) {
                        // deposit this container's contents (must be the same fluid)
                        net.minecraftforge.fluids.FluidStack fs =
                                net.minecraftforge.fluids.FluidContainerRegistry.getFluidForFilledItem(cursor);
                        if (fs == null || fs.getFluid() != fluid) {
                            p.addChatMessage(new ChatComponentText("\u5bb9\u5668\u91cc\u4e0d\u662f\u8fd9\u79cd\u6d41\u4f53"));
                            return;
                        }
                        if (!st.addRes(uid, fs.amount)) return;
                        ItemStack empty = net.minecraftforge.fluids.FluidContainerRegistry.drainFluidContainer(cursor);
                        if (empty == null) {
                            cursor.stackSize--;
                            p.inventory.setItemStack(cursor.stackSize <= 0 ? null : cursor);
                        } else {
                            p.inventory.setItemStack(empty);
                        }
                    } else {
                        // FILL a container: the cursor's empty one, or (empty hand) one pulled
                        // from the player inventory / the network's empty buckets
                        ItemStack empty;
                        boolean onCursor = false;
                        if (cursor != null && net.minecraftforge.fluids.FluidContainerRegistry.isEmptyContainer(cursor)
                                && net.minecraftforge.fluids.FluidContainerRegistry.fillFluidContainer(wantFs, cursor) != null) {
                            empty = cursor;
                            onCursor = true;
                        } else {
                            empty = null;
                            // 1) an empty container sitting in the player inventory
                            for (int i = 0; i < p.inventory.mainInventory.length; i++) {
                                ItemStack s = p.inventory.mainInventory[i];
                                if (s == null || s.stackSize <= 0) continue;
                                if (net.minecraftforge.fluids.FluidContainerRegistry.isEmptyContainer(s)
                                        && net.minecraftforge.fluids.FluidContainerRegistry.fillFluidContainer(wantFs, s) != null) {
                                    empty = s.copy();
                                    empty.stackSize = 1;
                                    s.stackSize--;
                                    if (s.stackSize <= 0) p.inventory.mainInventory[i] = null;
                                    p.inventory.markDirty();
                                    break;
                                }
                            }
                            // 2) an empty bucket from the network storage
                            if (empty == null) {
                                String bucketUid = StorageData.uidOf(new ItemStack(net.minecraft.init.Items.bucket));
                                if (st.count(bucketUid) >= 1) {
                                    ItemStack proto = st.protoOf(bucketUid);
                                    if (proto != null && proto.getItem() != null) {
                                        st.withdraw(bucketUid, 1);
                                        empty = proto;
                                        empty.stackSize = 1;
                                    }
                                }
                            }
                            if (empty == null) {
                                p.addChatMessage(new ChatComponentText("\u6ca1\u6709\u7a7a\u5bb9\u5668\uff08\u80cc\u5305\u548c\u7f51\u7edc\u91cc\u90fd\u6ca1\u6709\uff09"));
                                p.updateHeldItem();
                                return;
                            }
                        }
                        // QUANTIFY FIRST: check the fluid stock before spending anything
                        if (st.countRes(uid) < want) {
                            p.addChatMessage(new ChatComponentText(uid + " \u5b58\u91cf\u4e0d\u8db3"));
                            if (!onCursor) { // put the borrowed container back
                                if (!p.inventory.addItemStackToInventory(empty)) {
                                    st.deposit(empty);
                                }
                            }
                            p.updateHeldItem();
                            return;
                        }
                        ItemStack full = net.minecraftforge.fluids.FluidContainerRegistry.fillFluidContainer(wantFs, empty);
                        if (full == null) {
                            p.addChatMessage(new ChatComponentText("\u6ca1\u6709\u5339\u914d\u7684\u6ee1\u5bb9\u5668"));
                            if (!onCursor) {
                                if (!p.inventory.addItemStackToInventory(empty)) st.deposit(empty);
                            }
                            p.updateHeldItem();
                            return;
                        }
                        st.takeRes(uid, want);
                        if (onCursor && cursor.stackSize > 1) {
                            cursor.stackSize--;
                            p.inventory.setItemStack(cursor);
                            if (!p.inventory.addItemStackToInventory(full)) {
                                st.addRes(uid, want); // no room: roll the take back
                                cursor.stackSize++;
                                p.inventory.setItemStack(cursor);
                            }
                        } else {
                            p.inventory.setItemStack(full); // the FULL container lands on the cursor
                        }
                    }
                    p.updateHeldItem();
                    StorageNet.broadcastFull(p, st); // fluid lives in resources: full sync (no res delta yet)
                    return;
                }

                if (msg.type == 6) { // decon recipe cycle (‹ › in decon mode)
                    if (p.openContainer instanceof wheelchair.inventory.ContainerStoragePage) {
                        ((wheelchair.inventory.ContainerStoragePage) p.openContainer).cycle(msg.amount);
                    }
                    return;
                }

                if (msg.type == 8) { // craft recipe cycle (‹ › in craft mode)
                    if (p.openContainer instanceof wheelchair.inventory.ContainerStoragePage) {
                        ((wheelchair.inventory.ContainerStoragePage) p.openContainer).cycleCraft(msg.amount);
                    }
                    return;
                }

                if (msg.type == 0) { // withdraw item: amount 1..64, -1 = all, -2 = half a stack
                    if (msg.uid == null || msg.uid.length() < 1) return;
                    long have = st.count(msg.uid);
                    if (have <= 0) return;
                    int amt = msg.amount;
                    if (amt != -1 && amt != -2 && (amt < 1 || amt > 64)) return; // clamp
                    long want;
                    {
                        long base = amt == -1 ? have
                                : amt == -2 ? have // halves resolved below, after the item's own max is known
                                : amt;
                        want = base;
                    }

                    ItemStack proto = st.protoOf(msg.uid);
                    if (proto == null || proto.getItem() == null) return;
                    proto = proto.copy(); // protoOf returns the SHARED prototype — never mutate it
                    proto.stackSize = 1;

                    // "a stack" = the item's OWN stack limit (a bed = 1, not 64), and
                    // the take must never overflow int either
                    int max = Math.max(1, proto.getMaxStackSize());
                    if (amt == -2) want = Math.max(1, Math.min(Math.min(max, have), (long) Integer.MAX_VALUE) / 2); // half a stack
                    if (want > max) want = max;
                    want = Math.min(want, (long) Integer.MAX_VALUE);

                    ItemStack cursor = p.inventory.getItemStack();
                    boolean cursorOk = !msg.toInv && (cursor == null
                            || (cursor.isItemEqual(proto)
                                && cursor.hasTagCompound() == proto.hasTagCompound()
                                && (!cursor.hasTagCompound()
                                    || cursor.getTagCompound().equals(proto.getTagCompound()))));
                    if (!cursorOk) {
                        // hand the batch straight to the inventory; withdraw ONLY what was
                        // actually handed out — overflow stays stored, nothing is dropped
                        int take2 = (int) Math.min(want, have);
                        ItemStack give = proto.copy();
                        give.stackSize = take2;
                        if (p.inventory.addItemStackToInventory(give)) {
                            st.withdraw(msg.uid, take2);
                        } else if (give.stackSize < take2) {
                            st.withdraw(msg.uid, take2 - give.stackSize);
                        }
                        p.updateHeldItem();
                        StorageNet.broadcast(p, st, msg.uid);
                        return;
                    }
                    // QUANTIFY FIRST, MUTATE SECOND: bounded by actual stock BEFORE
                    // anything touches the cursor
                    int take = (int) Math.min(want, have);
                    if (take <= 0) return;
                    int onCursor = 0;
                    if (cursor != null) {
                        // poison-patched items can report maxStackSize <= 0 — clamp the cap
                        int cap = Math.max(1, cursor.getMaxStackSize());
                        onCursor = Math.max(0, Math.min(take, cap - cursor.stackSize));
                        if (onCursor > 0) cursor.stackSize += onCursor;
                    }
                    int rest = take - onCursor;

                    st.withdraw(msg.uid, take);
                    if (cursor == null) {
                        ItemStack give = proto.copy();
                        give.stackSize = take;
                        p.inventory.setItemStack(give);
                    } else if (rest > 0) {
                        ItemStack give = proto.copy();
                        give.stackSize = rest;
                        if (!p.inventory.addItemStackToInventory(give) && give.stackSize > 0) {
                            // return overflow to storage; if storage rejects it (NBT budget),
                            // drop it rather than void — never eat a player's items
                            if (st.deposit(give) != null) {
                                p.dropPlayerItemWithRandomChoice(give, false);
                            }
                        }
                    }
                    p.updateHeldItem();
                    StorageNet.broadcast(p, st, msg.uid);
                }
            } catch (Throwable t) {
                cpw.mods.fml.common.FMLLog.warning("[wheelchair] storage action failed: %s",
                        new Object[]{String.valueOf(t)});
            }
        }
    }
}
