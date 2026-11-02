package wheelchair.item;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import wheelchair.network.PacketOpenTerminal;

/**
 * The wheelchair staff (手杖): one item, page-less cheat tool. Left-click mines
 * with the chain-mining profile shown on the staff page (时运/精准/范围/形状);
 * the staff page edits that profile. No durability (cheat item), limit-1 by
 * nature (isDamageable false +setMaxStackSize(1) — tools stay single-stack per
 * the InfiniStack policy we adopted).
 */
public class ItemStaff extends Item {

    /** singleton, set in preInit */
    public static ItemStaff STAFF;


    public ItemStaff() {
        setUnlocalizedName("wheelchair.staff");
        setMaxStackSize(1);
        setTextureName("wheelchair:staff");
        setCreativeTab(null); // obtainable via /give — not on any creative tab yet
    }

    /**
     * HARVEST LEVEL = MAX (user call): the staff mines everything vanilla can
     * name — obsidian, GCC ores, whatever. Forge compares getHarvestLevel
     * against the block's required level for EVERY tool class.
     */
    @Override
    public int getHarvestLevel(ItemStack stack, String toolClass) {
        return Integer.MAX_VALUE;
    }

    /** dig speed: diamond-pickaxe grade — fast but NOT instant (user call) */
    @Override
    public float getDigSpeed(ItemStack stack, net.minecraft.block.Block block, int meta) {
        return 8.0F;
    }

    /**
     * Chain-dig entry: fires on the CLIENT the moment a break starts (survival
     * long-press and creative instant both hit this). We cancel vanilla and let
     * the server mine — single block when 连锁 is off, the shape otherwise.
     */
    @Override
    public boolean onBlockStartBreak(ItemStack stack, int x, int y, int z, EntityPlayer p) {
        if (p.worldObj.isRemote) {
            wheelchair.WheelchairMod.network.sendToServer(
                    new wheelchair.network.PacketStaffDig(x, y, z));
        }
        return true; // vanilla never breaks it — our server path owns the write
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer p) {
        if (world.isRemote) return stack;
        if (p.isSneaking()) {
            // Shift+right-click into AIR also cycles the mode (onItemUse only fires on a block face)
            cycleMode(p, world);
            return stack;
        }
        // 整地 also fires on right-click into AIR (onItemUse only fires on a block face)
        if (wheelchair.value.StaffProfile.get(world).topMode == 3) {
            landLevel((net.minecraft.entity.player.EntityPlayerMP) p, world);
        }
        return stack;
    }

    /** true when the staff's power-mark gesture owns the right-click — machine
     *  blocks must yield (return false) so ItemStaff.onItemUse can run */
    public static boolean gesturing(EntityPlayer p, World w) {
        ItemStack held = p.getHeldItem();
        if (held == null || held.getItem() != STAFF) return false;
        return w.isRemote ? wheelchair.value.StaffProfile.cTopMode >= 2
                          : wheelchair.value.StaffProfile.get(w).topMode >= 2;
    }

    /** the single mode-cycle write: profile + chat + client mirror push */
    private static void cycleMode(EntityPlayer p, World world) {
        wheelchair.value.StaffProfile prof = wheelchair.value.StaffProfile.get(world);
        prof.topMode = (prof.topMode + 1) % 5;
        prof.markDirty();
        p.addChatMessage(new net.minecraft.util.ChatComponentText(
                "§e手杖模式: " + wheelchair.value.StaffProfile.TOP_MODE_NAMES[prof.topMode]));
        wheelchair.WheelchairMod.network.sendTo(
                new wheelchair.network.PacketStaffCfgSync(prof),
                (net.minecraft.entity.player.EntityPlayerMP) p);
    }

    /**
     * Staff gestures (user call 2026-10-25): Shift+right-click ANYWHERE cycles
     * the top mode 普通 -> 连锁挖矿 -> 设置电力 (chat feedback + name suffix).
     * In 设置电力 mode a plain right-click on an energy machine: unmarked ->
     * mark (direction auto), marked -> toggle 存⇄取; unmark via the energy
     * page [X]. Other modes: right-click does nothing special.
     */
    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer p, World world, int x, int y, int z,
                             int side, float hx, float hy, float hz) {
        if (world.isRemote) return false; // server owns every write
        wheelchair.value.StaffProfile prof = wheelchair.value.StaffProfile.get(world);
        if (p.isSneaking()) { // mode cycle — any target
            cycleMode(p, world);
            return true;
        }
        if (prof.topMode == 3) { landLevel((net.minecraft.entity.player.EntityPlayerMP) p, world); return true; } // 整地: 右键任意处触发
        if (prof.topMode == 4) { // 加速: 右键方块加入/再点移除
            wheelchair.value.AccelHub hub = wheelchair.value.AccelHub.get();
            if (hub == null) return false;
            int r = hub.staffClickBlock(world, x, y, z, prof.time);
            if (r == 0) return false; // air — nothing to mark
            p.addChatMessage(new net.minecraft.util.ChatComponentText(
                    r == 1 ? "§b已加速标记 " + machineName(world, x, y, z)
                           : "§c已移除加速 " + machineName(world, x, y, z)));
            wheelchair.network.AccelNet.broadcast(hub);
            return true;
        }
        if (prof.topMode != 2) return false; // only 设置电力 marks machines
        wheelchair.value.EnergyMarkHub hub = wheelchair.value.EnergyMarkHub.get();
        if (hub == null) return false;
        int r = hub.staffClick(world, x, y, z);
        if (r == 0) return false; // not an energy machine — vanilla behaviour
        if (r == 1) p.addChatMessage(new net.minecraft.util.ChatComponentText(
                "§a已标记 " + machineName(world, x, y, z) + " (" + dirWord(hub, world, x, y, z) + ")"));
        if (r == 3) p.addChatMessage(new net.minecraft.util.ChatComponentText(
                "§e方向切换: " + dirWord(hub, world, x, y, z)));
        if (r == 4) p.addChatMessage(new net.minecraft.util.ChatComponentText(
                "§c已取消 " + machineName(world, x, y, z)));
        wheelchair.network.EnergyNet.broadcast(hub, world);
        return true;
    }

    /** live mode suffix on the item name (client mirror) — 普通无后缀 */
    @Override
    public String getItemStackDisplayName(ItemStack s) {
        String base = net.minecraft.util.StatCollector.translateToLocal(
                getUnlocalizedName(s) + ".name");
        return base + wheelchair.value.StaffProfile.modeSuffix();
    }

    /** 加速模式右键生物: 单体加速标记 (加入/再点移除); 其余模式 vanilla 行为 */
    @Override
    public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer p, EntityLivingBase target) {
        if (p.worldObj.isRemote) return false;
        wheelchair.value.StaffProfile prof = wheelchair.value.StaffProfile.get(p.worldObj);
        if (!p.isSneaking() && prof.topMode == 4) {
            wheelchair.value.AccelHub hub = wheelchair.value.AccelHub.get();
            if (hub == null) return false;
            int r = hub.staffClickEntity(p.worldObj, target, prof.time);
            if (r == 0) return false;
            p.addChatMessage(new net.minecraft.util.ChatComponentText(
                    r == 1 ? "§b已加速 " + target.getCommandSenderName()
                           : "§c已移除加速 " + target.getCommandSenderName()));
            wheelchair.network.AccelNet.broadcast(hub);
            return true;
        }
        return false;
    }

    /** 整地 (top mode 3): fill air/water with dirt below the player's feet
     *  down to bedrock inside the home chunk; clear every NBT-free block
     *  above — drops go to storage via the frame-sliced dig task. */
    private static void landLevel(net.minecraft.entity.player.EntityPlayerMP p, World world) {
        int cx = ((int) Math.floor(p.posX)) >> 4;
        int cz = ((int) Math.floor(p.posZ)) >> 4;
        int baseY = (int) Math.floor(p.posY); // 脚
        java.util.List<int[]> clear = new java.util.ArrayList<int[]>();
        int filled = 0;
        for (int xx = cx * 16; xx < cx * 16 + 16; xx++) {
            for (int zz = cz * 16; zz < cz * 16 + 16; zz++) {
                for (int y = baseY; y <= 255; y++) { // 地面(脚)起全清, 含人物站位两格
                    net.minecraft.block.Block b = world.getBlock(xx, y, zz);
                    if (b == null || b.isAir(world, xx, y, zz)) continue;
                    if (world.getTileEntity(xx, y, zz) != null) continue; // 保容器/机器
                    clear.add(new int[]{xx, y, zz});
                }
                for (int y = baseY - 1; y >= 0; y--) { // 下方: 空气/水填泥土, 到基岩
                    net.minecraft.block.Block b = world.getBlock(xx, y, zz);
                    if (b.isAir(world, xx, y, zz)
                            || b == net.minecraft.init.Blocks.water
                            || b == net.minecraft.init.Blocks.flowing_water) {
                        world.setBlock(xx, y, zz, net.minecraft.init.Blocks.dirt);
                        filled++;
                    }
                }
            }
        }
        wheelchair.value.StaffProfile prof = wheelchair.value.StaffProfile.get(world);
        wheelchair.value.StaffProfile snap = new wheelchair.value.StaffProfile("snap");
        snap.silk = prof.silk;
        snap.fortune = prof.fortune;
        wheelchair.value.StaffDigTask.start(p,
                new wheelchair.value.StaffDigTask.ListCells(world, clear,
                        (int) Math.floor(p.posX), baseY, (int) Math.floor(p.posZ), false),
                snap, true);
        p.addChatMessage(new net.minecraft.util.ChatComponentText(
                "§a整地: 填充 " + filled + " 格 · 清理 " + clear.size() + " 块(掉落入存储)"));
    }

    private static String machineName(World w, int x, int y, int z) {
        return w.getBlock(x, y, z).getLocalizedName();
    }

    private static String dirWord(wheelchair.value.EnergyMarkHub hub, World w, int x, int y, int z) {
        wheelchair.value.EnergyMarkHub.Mark m = hub.find(w.provider.dimensionId, x, y, z);
        return m == null ? "" : (m.store ? "\u00a7a存" : "\u00a7c取");
    }
}
