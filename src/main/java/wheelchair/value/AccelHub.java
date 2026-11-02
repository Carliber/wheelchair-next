package wheelchair.value;

import java.util.ArrayList;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Acceleration mark hub (加速标记): the list of accelerated spots + the tick
 * engine. GLOBAL per save (overworld perWorldStorage, EnergyMarkHub law).
 *
 * Two mark kinds (user call 2026-10-28):
 *  - block mark (方块): centre x/y/z + radius (Chebyshev box) + rate. Every
 *    tick each covered TileEntity gets rate-1 EXTRA updateEntity() calls;
 *    random-tick blocks (crops/reeds/saplings — no TE) get the same extra
 *    share via updateTick at the vanilla 3/4096 per-tick chance; every
 *    EntityAgeable inside the box gets its timers pushed (UselessMod
 *    law: age/egg/wool only — no AI, no pathing, no physics, no inLove).
 *  - entity mark (生物): single entity id + rate; timers pushed, no radius.
 *
 * RADIUS_STEPS/RATE_STEPS are the GUI chip ladders (left click up, right down).
 * remain: ticks left, -1 = permanent; set from the staff page's 加速时长 at
 * join time. Dead centre block / dead entity → mark auto-removed.
 *
 * Perf: the radius TE scan is heavy at r=16 (33^3 getTileEntity calls), so
 * the covered-TE list is cached and rescanned every SCAN_WINDOW=20t (same
 * window produces the cov telemetry shown on the GUI rows).
 */
public final class AccelHub extends WorldSavedData {

    private static final String KEY = "wheelchair.accel";
    private static final int SCAN_WINDOW = 20; // ticks per rescan + cov sample
    /** vanilla random-tick law (WorldServer.tickBlocks): 3 picks per 4096
     *  blocks per tick — our extra share rides the same odds */
    private static final int RT_TICKS = 3, RT_SPAN = 4096;

    public static final int[] RADIUS_STEPS = {1, 2, 4, 8, 16};
    public static final int[] RATE_STEPS = {2, 4, 8, 16, 32, 64, 128, 256};

    public static final class Mark {
        public int dim, x, y, z;   // block mark: box centre; entity mark: unused (0)
        public boolean isEntity;
        public int entityId;
        public int radius;         // block marks only (Chebyshev)
        public int rate;           // total ticks per game tick (extra = rate-1)
        public long remain;        // ticks left, -1 = permanent
        // telemetry + cached scan (server-transient)
        public transient int cov;          // covered TE count (SCAN_WINDOW average)
        private transient int covSum, covTicks;
        public transient java.util.List<int[]> teCache;   // [x,y,z] covered TEs (null = rescan due)
        public transient java.util.List<int[]> randCache;  // [x,y,z] random-tick blocks (crops etc.)
        public transient int rescanIn;

        Mark() {}

        Mark(int dim, int x, int y, int z, int radius, int rate, long remain) {
            this.dim = dim; this.x = x; this.y = y; this.z = z;
            this.radius = radius; this.rate = rate; this.remain = remain;
        }

        Mark(int dim, int entityId, int rate, long remain) {
            this.dim = dim; this.isEntity = true; this.entityId = entityId;
            this.rate = rate; this.remain = remain;
        }
    }

    public final ArrayList<Mark> marks = new ArrayList<Mark>();
    /** bumped on every change — packet pushers watch it to re-broadcast */
    public int version;
    /** master switch (GUI header chip) */
    public boolean on = true;

    public AccelHub(String key) {
        super(key);
    }

    /** the save's hub — always the overworld copy (EnergyMarkHub law) */
    public static AccelHub get() {
        World w0 = DimensionManager.getWorld(0);
        if (w0 == null) return null;
        AccelHub h = (AccelHub) w0.perWorldStorage.loadData(AccelHub.class, KEY);
        if (h == null) {
            h = new AccelHub(KEY);
            w0.perWorldStorage.setData(KEY, h);
        }
        return h;
    }

    // ---------------- staff gestures (server) ----------------

    /** staff right-click a block in 加速 mode: toggle mark (1 added, 2 removed, 0 no-op) */
    public int staffClickBlock(World w, int x, int y, int z, int timeSec) {
        if (w.getBlock(x, y, z).isAir(w, x, y, z)) return 0;
        Mark m = findBlock(w.provider.dimensionId, x, y, z);
        if (m != null) { marks.remove(m); markDirty(); version++; return 2; }
        marks.add(new Mark(w.provider.dimensionId, x, y, z, 4, 4, ticks(timeSec)));
        markDirty(); version++;
        return 1;
    }

    /** staff right-click an entity in 加速 mode: toggle mark (1 added, 2 removed, 0 no-op) */
    public int staffClickEntity(World w, EntityLivingBase e, int timeSec) {
        if (e == null || e.isDead || e instanceof EntityPlayer) return 0;
        Mark m = findEntity(w.provider.dimensionId, e.getEntityId());
        if (m != null) { marks.remove(m); markDirty(); version++; return 2; }
        marks.add(new Mark(w.provider.dimensionId, e.getEntityId(), 8, ticks(timeSec)));
        markDirty(); version++;
        return 1;
    }

    private static long ticks(int timeSec) {
        return timeSec < 0 ? -1L : (long) timeSec * 20L;
    }

    private Mark findBlock(int dim, int x, int y, int z) {
        for (int i = 0; i < marks.size(); i++) {
            Mark m = marks.get(i);
            if (!m.isEntity && m.dim == dim && m.x == x && m.y == y && m.z == z) return m;
        }
        return null;
    }

    private Mark findEntity(int dim, int entityId) {
        for (int i = 0; i < marks.size(); i++) {
            Mark m = marks.get(i);
            if (m.isEntity && m.dim == dim && m.entityId == entityId) return m;
        }
        return null;
    }

    // ---------------- tick engine (server thread) ----------------

    public void tick() {
        if (marks.isEmpty()) return;
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv == null) return;
        for (int i = marks.size() - 1; i >= 0; i--) {
            Mark m = marks.get(i);
            if (m.remain != -1L) {
                m.remain--;
                if (m.remain <= 0) { marks.remove(i); markDirty(); version++; continue; }
            }
            if (!on) continue;
            World w = m.dim == 0 ? srv.worldServers[0] : srv.worldServerForDimension(m.dim);
            if (w == null) continue; // chunk/dim unloaded: keep the mark, skip
            if (m.isEntity) {
                tickEntityMark(w, m, i);
            } else {
                tickBlockMark(w, m, i);
            }
        }
    }

    private void tickEntityMark(World w, Mark m, int idx) {
        Entity e = w.getEntityByID(m.entityId);
        if (e == null || e.isDead || !(e instanceof EntityLivingBase)) {
            marks.remove(idx); markDirty(); version++;
            return;
        }
        entityTimers((EntityLivingBase) e, m.rate - 1);
    }

    private void tickBlockMark(World w, Mark m, int idx) {
        if (w.getBlock(m.x, m.y, m.z).isAir(w, m.x, m.y, m.z)) {
            marks.remove(idx); markDirty(); version++;
            return;
        }
        // rescan the covered TEs every SCAN_WINDOW; between scans use the cache
        if (m.teCache == null || --m.rescanIn <= 0) {
            java.util.List<int[]> rand = new ArrayList<int[]>();
            m.teCache = scan(w, m, rand);
            m.randCache = rand;
            m.rescanIn = SCAN_WINDOW;
            m.cov = m.covTicks == 0 ? m.teCache.size() : (m.covSum + m.teCache.size()) / (m.covTicks + 1);
            m.covSum += m.teCache.size();
            m.covTicks++;
        }
        int extra = m.rate - 1;
        for (int j = 0; j < m.teCache.size(); j++) {
            int[] p = m.teCache.get(j);
            TileEntity te = w.getTileEntity(p[0], p[1], p[2]);
            if (te == null) continue;
            try {
                for (int k = 0; k < extra; k++) te.updateEntity();
            } catch (Throwable t) {
                // one broken machine must not kill the tick loop — drop the TE
                // from this mark's cache and log once per rescan window
                m.teCache.remove(j);
                j--;
                cpw.mods.fml.common.FMLLog.warning("[wheelchair] accel: TE threw, skipped: %s",
                        new Object[]{String.valueOf(t)});
            }
        }
        // random-tick blocks (crops/reeds/saplings): the same extra share at
        // the vanilla 3/4096 per-tick odds — expected ticks stay proportional
        if (m.randCache != null) {
            for (int j = 0; j < m.randCache.size(); j++) {
                int[] p = m.randCache.get(j);
                for (int k = 0; k < extra; k++) {
                    if (w.rand.nextInt(RT_SPAN) < RT_TICKS) {
                        Block b = w.getBlock(p[0], p[1], p[2]);
                        if (b != null && b.getTickRandomly()) {
                            b.updateTick(w, p[0], p[1], p[2], w.rand);
                        }
                    }
                }
            }
        }
        // animals inside the box: timers only (UselessMod law — no AI/pathing)
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(
                m.x - m.radius, m.y - m.radius, m.z - m.radius,
                m.x + m.radius + 1, m.y + m.radius + 1, m.z + m.radius + 1);
        java.util.List<?> mobs = w.getEntitiesWithinAABB(EntityAgeable.class, box);
        for (int j = 0; j < mobs.size(); j++) {
            entityTimers((EntityLivingBase) mobs.get(j), extra);
        }
    }

    /** scan the Chebyshev box: tile entities + random-tick blocks (rand out) */
    private static java.util.List<int[]> scan(World w, Mark m, java.util.List<int[]> randOut) {
        java.util.List<int[]> out = new ArrayList<int[]>();
        int r = m.radius;
        for (int x = m.x - r; x <= m.x + r; x++)
            for (int y = Math.max(0, m.y - r); y <= Math.min(255, m.y + r); y++)
                for (int z = m.z - r; z <= m.z + r; z++) {
                    if (w.getTileEntity(x, y, z) != null) {
                        out.add(new int[]{x, y, z});
                    } else {
                        Block b = w.getBlock(x, y, z);
                        if (b != null && b.getTickRandomly()) randOut.add(new int[]{x, y, z});
                    }
                }
        return out;
    }

    /**
     * push ONLY the timer-ish numbers (UselessMod BeefTimeAcceleration law):
     * growing age (baby growth + breed cooldown), chicken egg timer, sheared
     * wool regrow. Deliberately NOT inLove (it is the breed WINDOW — pushing
     * it to 0 would make animals never manage to pair) and not AI/physics.
     */
    public static void entityTimers(EntityLivingBase e, int times) {
        if (times <= 0 || e.isDead) return;
        if (e instanceof EntityAgeable) {
            EntityAgeable a = (EntityAgeable) e;
            int age = a.getGrowingAge();
            if (age != 0) {
                int step = Math.min(Math.abs(age), times);
                a.setGrowingAge(age < 0 ? age + step : age - step);
            }
        }
        if (e instanceof EntityChicken) {
            EntityChicken c = (EntityChicken) e;
            if (c.getGrowingAge() >= 0) { // adults only
                c.timeUntilNextEgg = Math.max(0, c.timeUntilNextEgg - times);
            }
        }
        if (e instanceof EntitySheep) {
            EntitySheep s = (EntitySheep) e;
            if (s.getSheared() && s.getGrowingAge() >= 0) s.setSheared(false);
        }
    }

    // ---------------- NBT ----------------

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        on = !nbt.hasKey("on") || nbt.getBoolean("on");
        NBTTagList l = nbt.getTagList("marks", 10);
        for (int i = 0; i < l.tagCount(); i++) {
            NBTTagCompound t = l.getCompoundTagAt(i);
            Mark m = new Mark();
            m.dim = t.getInteger("dim");
            m.isEntity = t.getBoolean("ent");
            m.entityId = t.getInteger("eid");
            m.x = t.getInteger("x"); m.y = t.getInteger("y"); m.z = t.getInteger("z");
            m.radius = Math.max(1, t.getInteger("r"));
            m.rate = Math.max(2, t.getInteger("m"));
            m.remain = t.getLong("left");
            marks.add(m);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setBoolean("on", on);
        NBTTagList l = new NBTTagList();
        for (int i = 0; i < marks.size(); i++) {
            Mark m = marks.get(i);
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("dim", m.dim);
            t.setBoolean("ent", m.isEntity);
            t.setInteger("eid", m.entityId);
            t.setInteger("x", m.x); t.setInteger("y", m.y); t.setInteger("z", m.z);
            t.setInteger("r", m.radius);
            t.setInteger("m", m.rate);
            t.setLong("left", m.remain);
            l.appendTag(t);
        }
        nbt.setTag("marks", l);
    }

    // ---------------- tick-bus install (EnergyTick law) ----------------

    public static void install() {
        FMLCommonHandler.instance().bus().register(new AccelHub("tickbus"));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        AccelHub h = get();
        if (h == null) return;
        int v0 = h.version;
        h.tick();
        // push when something changed, or on the 1s heartbeat (remaining timers tick down)
        if (h.version != v0 || ++beat >= 20) {
            beat = 0;
            wheelchair.network.AccelNet.broadcast(h);
        }
    }

    private int beat;
}
