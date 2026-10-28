package wheelchair;

import com.google.gson.Gson;
import wheelchair.value.PriceRegistry;
import wheelchair.value.StorageData;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** 存储数据层入口（GUI 按原型重写前的唯一存取路径） */
public class CommandWheelchair extends CommandBase {
    @Override
    public String getCommandName() { return "wheelchair"; }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/wheelchair [store|take|giveres|resdump|list|dump|bot]";
    }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public boolean canCommandSenderUseCommand(net.minecraft.command.ICommandSender sender) {
        return true; // usable by everyone, incl. non-cheat single player
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String sub = args.length == 0 ? "list" : args[0];
        cpw.mods.fml.common.FMLLog.info("[wheelchair] called by %s sub=%s", sender.getCommandSenderName(), sub);

        if (sub.equals("dump")) { tryDump(sender); return; }
        if (sub.equals("bot")) { handleBot(sender, args); return; }

        if (!(sender instanceof EntityPlayerMP)) {
            sender.addChatMessage(new ChatComponentText("This subcommand needs a player. Try: /wheelchair dump|bot"));
            return;
        }
        EntityPlayerMP p = (EntityPlayerMP) sender;

        if (sub.equals("store")) { store(p); return; }
        if (sub.equals("take")) { take(p, args); return; }
        if (sub.equals("list")) { list(p); return; }
        if (sub.equals("giveres")) { giveres(p, args); return; }
        if (sub.equals("resdump")) { resdump(p); return; }
        if (sub.equals("debugfill")) { debugfill(p, args); return; }
        if (sub.equals("decontest")) { decontest(p); return; }
        if (sub.equals("mark")) { mark(p, args); return; }
        if (sub.equals("wcst")) { // dev aid: cycle the staff top mode (same write the Shift+right-click does)
            wheelchair.value.StaffProfile prof = wheelchair.value.StaffProfile.get(p.worldObj);
            prof.topMode = (prof.topMode + 1) % 3;
            prof.markDirty();
            wheelchair.WheelchairMod.network.sendTo(
                    new wheelchair.network.PacketStaffCfgSync(prof), p);
            p.addChatMessage(new ChatComponentText("topMode=" + prof.topMode
                    + " " + wheelchair.value.StaffProfile.TOP_MODE_NAMES[prof.topMode]));
            return;
        }
        if (sub.equals("wce")) {
            p.openGui(wheelchair.WheelchairMod.instance, 6, p.worldObj, 0, 0, 0);
            return;
        }
        if (sub.equals("wcp")) {
            wheelchair.value.StorageData st = wheelchair.value.StorageData.get(p.worldObj);
            p.addChatMessage(new ChatComponentText("pool rf=" + st.countRes("rf")));
            wheelchair.value.EnergyMarkHub hub = wheelchair.value.EnergyMarkHub.get();
            if (hub != null) {
                for (int i = 0; i < hub.marks.size(); i++) {
                    wheelchair.value.EnergyMarkHub.Mark m = hub.marks.get(i);
                    p.addChatMessage(new ChatComponentText("mark " + m.dim + " " + m.x + " " + m.y + " " + m.z
                            + " " + (m.store ? "store" : "take") + (m.eu ? " EU" : " RF") + " rate=" + m.rate + "/t"));
                }
            }
            return;
        }
        p.addChatMessage(new ChatComponentText(getCommandUsage(sender)));
    }

    /** /wheelchair store — deposit the held stack (same semantics as GUI deposit) */
    private void store(EntityPlayerMP p) {
        ItemStack held = p.inventory.getCurrentItem();
        if (held == null) { p.addChatMessage(new ChatComponentText("Hold an item first.")); return; }
        StorageData st = StorageData.get(p.worldObj);
        String uid = StorageData.uidOf(held);
        int n = held.stackSize;
        String err = st.deposit(held);
        if (err != null) { p.addChatMessage(new ChatComponentText(err)); return; }
        p.inventory.setInventorySlotContents(p.inventory.currentItem, null);
        p.updateHeldItem();
        p.addChatMessage(new ChatComponentText("stored " + n + "x -> " + uid + " (total " + st.count(uid) + ")"));
    }

    /** /wheelchair take <uid> [n] — withdraw into the inventory (GUI batch path, quantize-first) */
    private void take(EntityPlayerMP p, String[] args) {
        if (args.length < 2) { p.addChatMessage(new ChatComponentText("usage: take <uid> [n] — uids via /wheelchair list")); return; }
        String uid = args[1];
        int want = 64;
        try { want = Integer.parseInt(args.length > 2 ? args[2] : "64"); } catch (Exception ignored) {}
        if (want < 1) want = 1;
        if (want > 2304) want = 2304; // clamp: 36 full stacks = whole main inventory
        StorageData st = StorageData.get(p.worldObj);
        long have = st.count(uid);
        if (have <= 0) { p.addChatMessage(new ChatComponentText("not stored: " + uid)); return; }
        ItemStack proto = st.protoOf(uid);
        if (proto == null || proto.getItem() == null) { p.addChatMessage(new ChatComponentText("no prototype for: " + uid)); return; }
        proto = proto.copy(); // protoOf returns the SHARED stored prototype — never mutate it
        proto.stackSize = 1;
        int take = (int) Math.min(want, have);
        int cap = Math.max(1, proto.getMaxStackSize());
        int given = 0;
        while (given < take) { // hand out in maxStackSize batches; stop at the first overflow
            int batch = Math.min(cap, take - given);
            ItemStack give = proto.copy();
            give.stackSize = batch;
            if (p.inventory.addItemStackToInventory(give)) {
                given += batch;
            } else if (give.stackSize < batch) { // partial: give.stackSize = the remainder NOT stored
                given += batch - give.stackSize;
                break;
            } else {
                break; // inventory full — overflow stays stored, nothing dropped
            }
        }
        if (given <= 0) { p.addChatMessage(new ChatComponentText("inventory full")); return; }
        st.withdraw(uid, given); // withdraw ONLY what was actually handed out
        p.updateHeldItem();
        p.addChatMessage(new ChatComponentText("took " + given + "x " + uid + " (left " + st.count(uid) + ")"));
    }

    /** /wheelchair list [page] — stored item uids + counts (page = 8 lines) */
    private void list(EntityPlayer p) {
        StorageData st = StorageData.get(p.worldObj);
        if (st.counts.isEmpty() && st.resources.isEmpty()) {
            p.addChatMessage(new ChatComponentText("storage empty — hold an item and /wheelchair store"));
            return;
        }
        StringBuilder sb = new StringBuilder("items(" + st.counts.size() + "): ");
        int i = 0;
        for (Map.Entry<String, Long> e : st.counts.entrySet()) {
            if (i++ >= 8) { sb.append("... (more)"); break; }
            sb.append(e.getKey()).append("x").append(e.getValue()).append(' ');
        }
        p.addChatMessage(new ChatComponentText(sb.toString()));
        if (!st.resources.isEmpty()) resdump((EntityPlayerMP) p);
    }

    /** /wheelchair giveres <uid|-uid> [amount] — add/take a resource unit count */
    private void giveres(EntityPlayerMP p, String[] args) {
        if (args.length < 2) {
            p.addChatMessage(new ChatComponentText("usage: giveres <uid|-uid> [amount] e.g. rf 5000, fluid:water 2000, xp 64"));
            return;
        }
        long n = 1;
        try { n = Long.parseLong(args.length > 2 ? args[2] : "1"); } catch (Exception ignored) {}
        if (n > 1000000000L) n = 1000000000L; // clamp: hostile input guard
        StorageData st = StorageData.get(p.worldObj);
        String uid;
        if (args[1].startsWith("-")) {
            uid = args[1].substring(1);
            st.takeRes(uid, n);
        } else {
            uid = args[1];
            st.addRes(uid, n);
        }
        p.addChatMessage(new ChatComponentText(args[1] + " -> " + st.countRes(uid)));
    }

    private void resdump(EntityPlayerMP p) {
        StorageData st = StorageData.get(p.worldObj);
        StringBuilder sb = new StringBuilder("resources: ");
        for (Map.Entry<String, Long> e : st.resources.entrySet()) sb.append(e.getKey()).append('=').append(e.getValue()).append(' ');
        p.addChatMessage(new ChatComponentText(sb.toString()));
    }

    /** "~" = player coord (+ offset), else absolute — dev-aid coordinate parsing */
    private static int parseCoord(double base, String s) {
        s = s.trim();
        if (s.equals("~")) return (int) Math.floor(base);
        if (s.startsWith("~")) return (int) Math.floor(base) + Integer.parseInt(s.substring(1));
        return Integer.parseInt(s);
    }

    /** /wheelchair mark <x> <y> <z> [s] — dev aid: fire the staff's mark gesture
     *  through the REAL server path (same staffClick the item calls). 's' = sneak. */
    private void mark(EntityPlayerMP p, String[] args) {
        if (args.length < 4) {
            p.addChatMessage(new ChatComponentText("usage: mark <x> <y> <z> [s]"));
            return;
        }
        int x = parseCoord(p.posX, args[1]), y = parseCoord(p.posY, args[2]), z = parseCoord(p.posZ, args[3]);
        wheelchair.value.EnergyMarkHub hub = wheelchair.value.EnergyMarkHub.get();
        if (hub == null) return;
        int r;
        if (args.length >= 5 && args[4].equalsIgnoreCase("x")) {
            r = hub.unmark(p.worldObj.provider.dimensionId, x, y, z) ? 2 : 0; // dev: force unmark
        } else {
            r = hub.staffClick(p.worldObj, x, y, z); // real staff gesture path
        }
        wheelchair.network.EnergyNet.broadcast(hub, p.worldObj);
        p.addChatMessage(new ChatComponentText("mark -> " + r + " (0 not-machine 1 marked 3 toggled)"));
    }

    /** /wheelchair debugfill [count] — dev aid: fill the storage with vanilla items in
     *  CREATIVE INVENTORY TAB ORDER (CreativeTabs.displayAllReleventItems per tab,
     *  tabAllSearch/tabInventory skipped), counts spanning every badge format tier
     *  (1, 64, 999, 9_999, 12_345, 999_999, 3_456_789, 1_234_567_890, 2^32, 2^40) so
     *  scrolling and the big-number badges can be eyeballed at once */
    private void debugfill(EntityPlayerMP p, String[] args) {
        int count = 150;
        if (args.length > 1) {
            try { count = Integer.parseInt(args[1]); } catch (NumberFormatException e) {
                p.addChatMessage(new ChatComponentText("usage: debugfill [count]"));
                return;
            }
        }
        if (count < 1) count = 1;
        if (count > 2000) count = 2000;
        long[] tiers = {1L, 7L, 64L, 999L, 9999L, 12345L, 999999L, 3456789L, 1234567890L, 4294967296L, 1099511627776L};
        StorageData st = StorageData.get(p.worldObj);
        java.util.List<ItemStack> pool = new java.util.ArrayList<ItemStack>();
        for (net.minecraft.creativetab.CreativeTabs tab : net.minecraft.creativetab.CreativeTabs.creativeTabArray) {
            if (tab == net.minecraft.creativetab.CreativeTabs.tabAllSearch) continue; // would duplicate every item
            if (tab == net.minecraft.creativetab.CreativeTabs.tabInventory) continue; // player inventory tab
            tab.displayAllReleventItems(pool);
        }
        int added = 0;
        for (int i = 0; i < count && i < pool.size(); i++) {
            ItemStack proto = pool.get(i);
            if (proto == null || proto.getItem() == null) continue;
            ItemStack one = new ItemStack(proto.getItem(), 1, proto.getItemDamage());
            long n = tiers[added % tiers.length];
            st.counts.put(StorageData.uidOf(one), Long.valueOf(n));
            added++;
        }
        st.markDirty();
        wheelchair.network.StorageNet.broadcastFull(p, st);
        p.addChatMessage(new ChatComponentText("debugfill: " + added + " items (pool " + pool.size() + ")"));
    }

    /** /wheelchair decontest — diagnostics: dump the decon recipes for the held item */
    private void decontest(EntityPlayerMP p) {
        ItemStack probe = p.inventory.getCurrentItem();
        if (probe == null) {
            p.addChatMessage(new ChatComponentText("hold an item first"));
            return;
        }
        java.util.List<wheelchair.recipe.RecipeIndex.DeconRecipe> rs =
                wheelchair.recipe.RecipeIndex.getRecipes(probe);
        String head = "[wheelchair] decontest " + probe.getItem() + " recipes=" + rs.size()
                + " total=" + wheelchair.recipe.RecipeIndex.map.size();
        p.addChatMessage(new ChatComponentText(head));
        cpw.mods.fml.common.FMLLog.info(head, new Object[0]);
        for (int i = 0; i < rs.size(); i++) {
            wheelchair.recipe.RecipeIndex.DeconRecipe r = rs.get(i);
            StringBuilder sb = new StringBuilder("recipe " + i + " " + r.width + "x" + r.height
                    + " out=" + r.result.stackSize);
            for (int s = 0; s < r.ingredients.length; s++) {
                ItemStack ing = r.ingredients[s];
                if (ing == null) continue;
                int vc = r.variants != null && r.variants[s] != null ? r.variants[s].size() : 0;
                sb.append(" [").append(s).append(":").append(ing.getItem()).append("@")
                        .append(ing.getItemDamage()).append(" v").append(vc).append("]");
            }
            String line = sb.toString();
            p.addChatMessage(new ChatComponentText(line));
            cpw.mods.fml.common.FMLLog.info(line, new Object[0]);
        }
    }

    private void tryDump(ICommandSender sender) {
        try {
            File dir = new File("dumps");
            dir.mkdirs();
            File out = new File(dir, "items.json");
            Map<String, Object> root = new LinkedHashMap<String, Object>();
            Map<String, Object> items = new TreeMap<String, Object>();
            for (Object o : Item.itemRegistry) {
                Item it = (Item) o;
                Map<String, Object> e = new LinkedHashMap<String, Object>();
                ItemStack s = new ItemStack(it);
                e.put("display", s.getDisplayName());
                e.put("price", PriceRegistry.getUnitPrice(it));
                items.put(PriceRegistry.key(it), e);
            }
            root.put("items", items);
            String json = new Gson().toJson(root);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
            fos.write(json.getBytes("UTF-8"));
            fos.close();
            sender.addChatMessage(new ChatComponentText("Dumped to " + out.getAbsolutePath()));
        } catch (Throwable t) {
            sender.addChatMessage(new ChatComponentText("Dump failed: " + t));
        }
    }

    /** automated-test helpers via a server-side fake player */
    private void handleBot(ICommandSender sender, String[] args) {
        net.minecraft.server.MinecraftServer srv = net.minecraft.server.MinecraftServer.getServer();
        net.minecraft.world.WorldServer w = net.minecraftforge.common.DimensionManager.getWorld(0);
        if (w == null) { sender.addChatMessage(new ChatComponentText("overworld not loaded")); return; }
        net.minecraft.entity.player.EntityPlayerMP bot =
            net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(w);
        String op = args.length > 1 ? args[1] : "state";
        String a1 = args.length > 2 ? args[2] : "";
        String a2 = args.length > 3 ? args[3] : "1";

        if (op.equals("give")) {
            net.minecraft.item.Item it = PriceRegistry.getItem(a1);
            if (it == null) { sender.addChatMessage(new ChatComponentText("unknown item: " + a1)); return; }
            int n = 1; try { n = Integer.parseInt(a2); } catch (Exception ignored) {}
            ItemStack st = new ItemStack(it, n);
            net.minecraft.entity.player.InventoryPlayer inv = bot.inventory;
            if (!inv.addItemStackToInventory(st)) {
                net.minecraft.entity.item.EntityItem drop = new net.minecraft.entity.item.EntityItem(w, bot.posX, bot.posY, bot.posZ, st);
                w.spawnEntityInWorld(drop);
            }
            sender.addChatMessage(new ChatComponentText("gave bot " + n + "x " + a1));
        } else if (op.equals("clear")) {
            for (int i = 0; i < bot.inventory.mainInventory.length; i++) bot.inventory.mainInventory[i] = null;
            sender.addChatMessage(new ChatComponentText("bot inventory cleared"));
        }
        // state is always printed
        StringBuilder sb = new StringBuilder("BOT state: inv=[");
        for (int i = 0; i < 9; i++) {
            ItemStack s = bot.inventory.mainInventory[i];
            if (s != null) sb.append(i).append(":").append(s.stackSize).append("x").append(s.getDisplayName()).append(" ");
        }
        sender.addChatMessage(new ChatComponentText(sb.append("]").toString()));
    }
}
