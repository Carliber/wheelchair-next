package wheelchair.value;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Frame-sliced chain mining (user rules):
 *  - NO block-count cap: big veins just take more ticks
 *  - MOVEMENT STOPS THE DIG: the task snapshots the player's position and
 *    cancels the moment it moves (rotation does not count)
 *  - drops are computed (silk/fortune) and deposited straight into storage
 *
 * Modes (StaffProfile.mode): 0 普通 = adjacency-flood / shape + same-type,
 * 1 增强 = same-type without adjacency (range box), 2 挖空 = shape/box内
 * everything WITHOUT tile entities (containers/machines are never touched).
 * Harvest level is effectively max: we write air directly, bypassing vanilla.
 *
 * One ACTIVE task per player; a new dig request replaces the old one.
 */
public class StaffDigTask {

    /** blocks mined per server tick (frame budget) */
    private static final int PER_TICK = 128;

    private final EntityPlayerMP player;
    private final double startX, startY, startZ;
    private final Cells cells;
    private final StaffProfile snap;    // config snapshot at dig start
    private final StorageData st;
    private final World world;
    /** true = drops into storage (chain mode); false = into the player's
     *  inventory, spilling on the ground when full (single-block mode) */
    private final boolean toStorage;

    private static final Map<UUID, StaffDigTask> ACTIVE = new HashMap<UUID, StaffDigTask>();

    /** public no-arg: FML's ASM event bus needs a constructible named class */
    public StaffDigTask() {
        this.player = null;
        this.startX = this.startY = this.startZ = 0;
        this.cells = null;
        this.snap = null;
        this.st = null;
        this.world = null;
        this.toStorage = true;
    }

    private StaffDigTask(EntityPlayerMP p, Cells cells, StaffProfile snap, boolean toStorage) {
        this.player = p;
        this.startX = p.posX;
        this.startY = p.posY;
        this.startZ = p.posZ;
        this.cells = cells;
        this.snap = snap;
        this.st = StorageData.get(p.worldObj);
        this.world = p.worldObj;
        this.toStorage = toStorage;
    }

    /** start (or replace) this player's dig task */
    public static void start(EntityPlayerMP p, Cells cells, StaffProfile snap, boolean toStorage) {
        synchronized (ACTIVE) {
            ACTIVE.put(p.getUniqueID(), new StaffDigTask(p, cells, snap, toStorage));
        }
    }

    public static boolean isBusy(EntityPlayerMP p) {
        synchronized (ACTIVE) {
            return ACTIVE.containsKey(p.getUniqueID());
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        List<UUID> done = new ArrayList<UUID>();
        synchronized (ACTIVE) {
            Iterator<Map.Entry<UUID, StaffDigTask>> it = ACTIVE.entrySet().iterator();
            while (it.hasNext()) {
                StaffDigTask t = it.next().getValue();
                if (t.tick()) it.remove();
            }
        }
    }

    /** @return true when finished/cancelled */
    private boolean tick() {
        if (player.isDead || player.worldObj != world) return true;
        if (Math.abs(player.posX - startX) > 0.001
                || Math.abs(player.posY - startY) > 0.001
                || Math.abs(player.posZ - startZ) > 0.001) {
            return true; // 移动后停止
        }
        for (int i = 0; i < PER_TICK; i++) {
            if (!cells.hasNext()) {
                wheelchair.network.StorageNet.broadcast(player, st, "xp");
                return true;
            }
            mineOne(cells.next());
        }
        return false;
    }

    private void mineOne(int[] pos) {
        int x = pos[0], y = pos[1], z = pos[2];
        if (y < 0 || y > 255) return;
        Block b = world.getBlock(x, y, z);
        if (b == null || b.isAir(world, x, y, z)) return;
        if (snap.mode == 2 && world.getTileEntity(x, y, z) != null) return; // 挖空保容器
        List<ItemStack> drops = new ArrayList<ItemStack>();
        if (snap.silk) {
            ItemStack s = silkDrop(b, world.getBlockMetadata(x, y, z));
            if (s != null) drops.add(s);
        } else {
            List<ItemStack> d = b.getDrops(world, x, y, z, world.getBlockMetadata(x, y, z), snap.fortune);
            if (d != null) drops.addAll(d);
        }
        for (ItemStack d : drops) {
            if (d == null || d.stackSize <= 0) continue;
            if (toStorage) {
                st.deposit(d);
            } else {
                // single-block mode: normal mining feel — into the inventory,
                // spill on the ground when full
                if (!player.inventory.addItemStackToInventory(d)) {
                    player.dropPlayerItemWithRandomChoice(d, false);
                }
            }
        }
        int xp = 0;
        try {
            xp = b.getExpDrop(world, world.getBlockMetadata(x, y, z), snap.fortune);
        } catch (Throwable ignored) {
        }
        if (xp > 0) {
            if (toStorage) st.addRes("xp", xp);
            else player.addExperience(xp);
        }
        world.setBlockToAir(x, y, z);
    }

    private static java.lang.reflect.Method SILK_DROP;

    /** Block.createStackedBlock is PROTECTED — call by reflection, dev name first
     *  then the srg name (aji/j per joined.srg) for the obf production env */
    private static ItemStack silkDrop(Block b, int meta) {
        try {
            if (SILK_DROP == null) {
                try {
                    SILK_DROP = Block.class.getDeclaredMethod("createStackedBlock", int.class);
                } catch (NoSuchMethodException e) {
                    SILK_DROP = Block.class.getDeclaredMethod("j", int.class); // aji/j: short obf name
                }
                SILK_DROP.setAccessible(true);
            }
            return (ItemStack) SILK_DROP.invoke(b, Integer.valueOf(meta));
        } catch (Throwable t) {
            return null;
        }
    }

    // ================= cell producers =================

    /** lazy cell source; hasNext/next may do world scans — keep them cheap-ish */
    public interface Cells {
        boolean hasNext();

        int[] next();
    }

    /** shape/mode -> producer (1.7.10 port of UselessMod ChainMiningShapes) */
    public static Cells producer(World w, int x, int y, int z, StaffProfile cfg, ForgeDirection face) {
        boolean digAll = cfg.mode == 2;
        if (cfg.shape == 0) {
            // 跟随: 普通=BFS 连通同类, 增强/挖空=range 盒
            if (digAll || cfg.mode == 1) return new BoxCells(w, x, y, z, cfg.range, !digAll);
            return new FloodCells(w, x, y, z, cfg.range);
        }
        List<int[]> cells = shapeCells(x, y, z, cfg, face);
        return new ListCells(w, cells, x, y, z, !digAll);
    }

    private static List<int[]> shapeCells(int x, int y, int z, StaffProfile cfg, ForgeDirection face) {
        List<int[]> out = new ArrayList<int[]>();
        switch (cfg.shape) {
            case 1: // 1x1 隧道
                for (int i = 0; i < cfg.range; i++)
                    out.add(new int[]{x - face.offsetX * i, y - face.offsetY * i, z - face.offsetZ * i});
                return out;
            case 2: // 3x3 单层
                return tunnelLayers(x, y, z, face, 1, cfg.range, out);
            case 3: // 3x3 隧道
                return tunnelLayers(x, y, z, face, Integer.MAX_VALUE, cfg.range, out);
            case 4: // 下阶梯
                return diagonal(x, y, z, face, -1, cfg.range, out);
            default: // 5 上阶梯
                return diagonal(x, y, z, face, 1, cfg.range, out);
        }
    }

    private static List<int[]> diagonal(int x, int y, int z, ForgeDirection face, int yDir, int range, List<int[]> out) {
        ForgeDirection f = face.offsetY == 0 ? face : ForgeDirection.EAST;
        for (int i = 0; i < range; i++)
            out.add(new int[]{x - f.offsetX * i, y + yDir * i, z - f.offsetZ * i});
        return out;
    }

    private static List<int[]> tunnelLayers(int bx, int by, int bz, ForgeDirection face, int maxDepth, int range, List<int[]> out) {
        out.add(new int[]{bx, by, bz});
        int depth = 0;
        while (depth < maxDepth && out.size() < range * 3 + 9) {
            for (int a = -1; a <= 1; a++)
                for (int b = -1; b <= 1; b++) {
                    if (depth == 0 && a == 0 && b == 0) continue;
                    int px, py, pz;
                    if (face.offsetX != 0) { px = bx; py = by + a; pz = bz + b; }
                    else if (face.offsetY != 0) { px = bx + a; py = by; pz = bz + b; }
                    else { px = bx + a; py = by + b; pz = bz; }
                    out.add(new int[]{px, py, pz});
                }
            bx -= face.offsetX;
            by -= face.offsetY;
            bz -= face.offsetZ;
            depth++;
        }
        return out;
    }

    /** BFS flood of the SAME block+meta through 26-neighbour adjacency, Chebyshev-capped */
    private static final class FloodCells implements Cells {
        private final World w;
        private final int ox, oy, oz, range;
        private final Block block;
        private final int meta;
        private final ArrayDeque<int[]> queue = new ArrayDeque<int[]>();
        private final HashSet<Long> seen = new HashSet<Long>();
        private int[] pending;

        FloodCells(World w, int x, int y, int z, int range) {
            this.w = w;
            this.ox = x;
            this.oy = y;
            this.oz = z;
            this.range = range;
            this.block = w.getBlock(x, y, z);
            this.meta = w.getBlockMetadata(x, y, z);
            queue.add(new int[]{x, y, z});
            seen.add(pack(x, y, z));
        }

        public boolean hasNext() {
            if (pending != null) return true;
            while (!queue.isEmpty()) {
                int[] c = queue.poll();
                // scan ALL 26 neighbours of c (the old loop stopped at the first
                // match, collapsing the flood into a single chain)
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) continue;
                            int nx = c[0] + dx, ny = c[1] + dy, nz = c[2] + dz;
                            if (Math.abs(nx - ox) > range || Math.abs(ny - oy) > range || Math.abs(nz - oz) > range)
                                continue;
                            long p = pack(nx, ny, nz);
                            if (seen.contains(p)) continue;
                            seen.add(p);
                            if (w.getBlock(nx, ny, nz) == block && w.getBlockMetadata(nx, ny, nz) == meta) {
                                queue.add(new int[]{nx, ny, nz});
                            }
                        }
                pending = c; // c is due for mining; its whole neighbourhood is queued
                return true;
            }
            return false;
        }

        public int[] next() {
            int[] r = pending;
            pending = null;
            return r;
        }
    }

    /** every block in the range cube, optionally filtered to the origin's type */
    private static final class BoxCells implements Cells {
        private final World w;
        private final int ox, oy, oz, range;
        private final boolean sameType;
        private final Block block;
        private final int meta;
        private int cx, cy, cz; // cursor offsets, from -range
        private int[] pending;

        BoxCells(World w, int x, int y, int z, int range, boolean sameType) {
            this.w = w;
            this.ox = x;
            this.oy = y;
            this.oz = z;
            this.range = range;
            this.sameType = sameType;
            this.block = w.getBlock(x, y, z);
            this.meta = w.getBlockMetadata(x, y, z);
            cx = cy = cz = -range;
            advance();
        }

        private void advance() {
            while (cy <= range) {
                if (cz > range) {
                    cz = -range;
                    cx++;
                    if (cx > range) {
                        cx = -range;
                        cy++;
                    }
                    continue;
                }
                int[] p = {ox + cx, oy + cy, oz + cz};
                cz++;
                int ny = p[1];
                if (ny < 0 || ny > 255) continue;
                Block b = w.getBlock(p[0], ny, p[2]);
                if (b == null || b.isAir(w, p[0], ny, p[2])) continue;
                if (sameType && (b != block || w.getBlockMetadata(p[0], ny, p[2]) != meta)) continue;
                pending = p;
                return;
            }
            pending = null;
        }

        public boolean hasNext() {
            return pending != null;
        }

        public int[] next() {
            int[] r = pending;
            advance();
            return r;
        }
    }

    /** fixed shape layout, optionally same-type filtered (public: the packet builds single-block digs with it) */
    public static final class ListCells implements Cells {
        private final World w;
        private final List<int[]> cells;
        private final Iterator<int[]> it;
        private final boolean sameType;
        private final Block block;
        private final int meta;
        private int[] pending;

        public ListCells(World w, List<int[]> cells, int ox, int oy, int oz, boolean sameType) {
            this.w = w;
            this.cells = cells;
            this.it = cells.iterator();
            this.sameType = sameType;
            this.block = w.getBlock(ox, oy, oz);
            this.meta = w.getBlockMetadata(ox, oy, oz);
            advance();
        }

        private void advance() {
            while (it.hasNext()) {
                int[] p = it.next();
                if (p[1] < 0 || p[1] > 255) continue;
                if (sameType) {
                    Block b = w.getBlock(p[0], p[1], p[2]);
                    if (b != block || w.getBlockMetadata(p[0], p[1], p[2]) != meta) continue;
                }
                pending = p;
                return;
            }
            pending = null;
        }

        public boolean hasNext() {
            return pending != null;
        }

        public int[] next() {
            int[] r = pending;
            advance();
            return r;
        }
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }
}
