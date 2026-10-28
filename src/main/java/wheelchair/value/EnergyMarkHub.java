package wheelchair.value;

import java.util.ArrayList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * Energy mark hub (能源标记): the list of marked energy machines + the transfer
 * engine. GLOBAL per save (stored on the overworld's perWorldStorage — marks
 * from other dims all live in one list, the pool ledger is per-save too).
 *
 * mark semantics (user call 2026-10-25):
 *  - store (存, GREEN): machine → pool. RF provider: extractEnergy (real pull);
 *    EU source: getOfferedEnergy + drawEnergy, ×euToRf into the ledger.
 *  - take (取, RED): pool → machine, adaptive ramp (32 RF start, doubled up to
 *    what the machine actually accepted — the battery law; packet voltage is
 *    then never more than an amount already proven safe, no explosions).
 *  - direction toggle ONLY via staff Shift+right-click on the machine; the
 *    energy GUI is read-only + [X] unmark.
 *  - dead machine (no TE / no longer an energy face) → mark auto-removed.
 *  - chunk/dim not loaded → mark kept but skipped this tick.
 */
public final class EnergyMarkHub extends WorldSavedData {

    private static final String KEY = "wheelchair.emarks";
    private static final int RAMP_START = 32;      // RF per tick, LV-safe in EU too (32EU)
    private static final int RAMP_MAX = 1 << 20;   // hard cap: 1M RF/t (cheat mod, but not infinite)
    private static final int RATE_WINDOW = 20;     // ticks per rate sample

    public static final class Mark {
        public int dim, x, y, z;
        public boolean store;      // true 存 (machine→pool) / false 取 (pool→machine)
        public boolean eu;         // protocol badge
        public int ramp = RAMP_START;   // take-direction ramp state (RF ledger units)
        // rate telemetry (display units: eu marks count EU/t, rf marks RF/t)
        private long sum;
        private int ticks;
        public int rate;

        Mark() {}

        Mark(int dim, int x, int y, int z, boolean store, boolean eu) {
            this.dim = dim; this.x = x; this.y = y; this.z = z;
            this.store = store; this.eu = eu;
        }
    }

    public final ArrayList<Mark> marks = new ArrayList<Mark>();
    /** bumped on every change — packet pushers watch it to re-broadcast */
    public int version;

    public EnergyMarkHub(String key) {
        super(key);
    }

    /** the save's hub — always the overworld copy so cross-dim marks share one list */
    public static EnergyMarkHub get() {
        World w0 = DimensionManager.getWorld(0);
        if (w0 == null) return null; // too early / shutting down — callers must null-check
        EnergyMarkHub h = (EnergyMarkHub) w0.perWorldStorage.loadData(EnergyMarkHub.class, KEY);
        if (h == null) {
            h = new EnergyMarkHub(KEY);
            w0.perWorldStorage.setData(KEY, h);
        }
        return h;
    }

    // ---------------- machine detection (protocol-agnostic) ----------------

    /** may this TE be marked? RF: any cofh face; EU: any ic2 face (API classes
     *  are embedded — with no IC2 installed nothing implements them, zero risk) */
    public static boolean isMachine(TileEntity te) {
        if (te == null) return false;
        if (te instanceof cofh.api.energy.IEnergyReceiver
                || te instanceof cofh.api.energy.IEnergyProvider
                || te instanceof cofh.api.energy.IEnergyHandler) return true;
        return te instanceof ic2.api.energy.tile.IEnergySink
                || te instanceof ic2.api.energy.tile.IEnergySource;
    }

    /** default direction for a fresh mark: can it produce? → 存, else 取 */
    public static boolean defaultStore(TileEntity te) {
        return te instanceof cofh.api.energy.IEnergyProvider
                || te instanceof ic2.api.energy.tile.IEnergySource;
    }

    private static boolean isEuFace(TileEntity te) {
        if (te instanceof cofh.api.energy.IEnergyReceiver
                || te instanceof cofh.api.energy.IEnergyProvider
                || te instanceof cofh.api.energy.IEnergyHandler) {
            // dual-protocol blocks count as RF (their RF face is the primary)
            if (te instanceof ic2.api.energy.tile.IEnergySink
                    || te instanceof ic2.api.energy.tile.IEnergySource) return false;
            return false;
        }
        return true; // pure IC2 face
    }

    // ---------------- mark ops (server side only) ----------------

    /** staff right-click in 设置电力 mode (user call 2026-10-25):
     *  returns 0 no-op (not a machine), 1 marked, 3 toggled direction.
     *  Unmark lives on the energy page [X] — no gesture for it (the old
     *  Shift gesture now cycles the staff top mode). */
    public int staffClick(World w, int x, int y, int z) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (!isMachine(te)) return 0;
        Mark m = find(w.provider.dimensionId, x, y, z);
        if (m != null) { // marked -> toggle 存⇄取
            m.store = !m.store;
            m.ramp = RAMP_START;
            m.sum = 0; m.ticks = 0; m.rate = 0;
            markDirty(); version++;
            return 3;
        }
        marks.add(new Mark(w.provider.dimensionId, x, y, z, defaultStore(te), isEuFace(te)));
        markDirty(); version++;
        return 1;
    }

    /** GUI [X] unmark */
    public boolean unmark(int dim, int x, int y, int z) {
        Mark m = find(dim, x, y, z);
        if (m == null) return false;
        marks.remove(m);
        markDirty(); version++;
        return true;
    }

    public Mark find(int dim, int x, int y, int z) {
        for (int i = 0; i < marks.size(); i++) {
            Mark m = marks.get(i);
            if (m.dim == dim && m.x == x && m.y == y && m.z == z) return m;
        }
        return null;
    }

    // ---------------- transfer engine (one server tick) ----------------

    public void tick() {
        if (marks.isEmpty()) return;
        MinecraftServer srv = MinecraftServer.getServer();
        if (srv == null) return;
        World w0 = srv.worldServers[0];
        StorageData pool = StorageData.get(w0);
        boolean changed = false;

        for (int i = marks.size() - 1; i >= 0; i--) {
            Mark m = marks.get(i);
            World w = m.dim == 0 ? w0 : DimensionManager.getWorld(m.dim);
            if (w == null || !w.blockExists(m.x, m.y, m.z)) continue; // unloaded — keep the mark
            TileEntity te = w.getTileEntity(m.x, m.y, m.z);
            if (!isMachine(te)) { // machine gone/replaced → auto-unmark (user call)
                marks.remove(i);
                markDirty(); changed = true;
                continue;
            }
            long moved = m.store ? pullFrom(te, m, pool) : pushTo(te, m, pool);
            m.sum += moved;
            m.ticks++;
            if (m.ticks >= RATE_WINDOW) {
                // display units: EU marks show EU/t (ledger RF /4)
                m.rate = m.eu ? (int) (m.sum / RATE_WINDOW / wheelchair.WheelchairMod.euToRf)
                              : (int) (m.sum / RATE_WINDOW);
                m.sum = 0; m.ticks = 0;
            }
        }
        if (changed) version++;
    }

    /** 存: machine → pool. returns RF moved */
    private long pullFrom(TileEntity te, Mark m, StorageData pool) {
        long got = 0;
        if (te instanceof cofh.api.energy.IEnergyProvider) {
            cofh.api.energy.IEnergyProvider p = (cofh.api.energy.IEnergyProvider) te;
            int r = p.extractEnergy(ForgeDirection.UNKNOWN, m.ramp, false);
            if (r > 0) {
                got = r;
                // providers: ramp follows what was actually available (up), decay on idle
                m.ramp = r >= m.ramp ? Math.min(m.ramp * 2, RAMP_MAX) : Math.max(RAMP_START, r * 2);
            } else {
                m.ramp = Math.max(RAMP_START, m.ramp / 2);
            }
        }
        if (te instanceof ic2.api.energy.tile.IEnergySource) {
            ic2.api.energy.tile.IEnergySource src = (ic2.api.energy.tile.IEnergySource) te;
            double offered = src.getOfferedEnergy();
            if (offered > 0) {
                src.drawEnergy(offered);
                got += (long) (offered * wheelchair.WheelchairMod.euToRf);
            }
        }
        if (got > 0) pool.addRes("rf", got);
        return got;
    }

    /** 取: pool → machine, adaptive ramp (the battery law). returns RF moved */
    private long pushTo(TileEntity te, Mark m, StorageData pool) {
        long moved = 0;
        if (te instanceof cofh.api.energy.IEnergyReceiver) {
            cofh.api.energy.IEnergyReceiver r = (cofh.api.energy.IEnergyReceiver) te;
            int cap = (int) Math.min(m.ramp, pool.countRes("rf"));
            if (cap > 0) {
                int acc = r.receiveEnergy(ForgeDirection.UNKNOWN, cap, false);
                if (acc > 0) {
                    moved = acc;
                    m.ramp = acc >= cap ? Math.min(m.ramp * 2, RAMP_MAX) : Math.max(RAMP_START, acc * 2);
                } else {
                    m.ramp = Math.max(RAMP_START, m.ramp / 2);
                }
            }
        }
        if (te instanceof ic2.api.energy.tile.IEnergySink) {
            ic2.api.energy.tile.IEnergySink sink = (ic2.api.energy.tile.IEnergySink) te;
            // ramp is RF ledger units; EU budget = /4, voltage = amount given
            // (never more than an already-proven-safe value — no explosions)
            long rfAvail = pool.countRes("rf");
            double euCap = Math.min(m.ramp, rfAvail) / (double) wheelchair.WheelchairMod.euToRf;
            double demanded = sink.getDemandedEnergy();
            double give = Math.min(euCap, demanded);
            if (give > 0) {
                double left = sink.injectEnergy(ForgeDirection.UNKNOWN, give, give);
                double acc = give - left;
                if (acc > 0) {
                    moved = (long) (acc * wheelchair.WheelchairMod.euToRf);
                    m.ramp = acc >= give ? Math.min(m.ramp * 2, RAMP_MAX)
                                         : Math.max(RAMP_START, (int) (acc * 2 * wheelchair.WheelchairMod.euToRf));
                } else {
                    m.ramp = Math.max(RAMP_START, m.ramp / 2);
                }
            }
        }
        if (moved > 0) pool.takeRes("rf", moved);
        return moved;
    }

    // ---------------- persistence ----------------

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        marks.clear();
        NBTTagList l = nbt.getTagList("marks", 10);
        for (int i = 0; i < l.tagCount(); i++) {
            NBTTagCompound t = l.getCompoundTagAt(i);
            Mark m = new Mark(t.getInteger("dim"), t.getInteger("x"), t.getInteger("y"),
                    t.getInteger("z"), t.getBoolean("store"), t.getBoolean("eu"));
            m.ramp = Math.max(RAMP_START, t.getInteger("ramp"));
            marks.add(m);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList l = new NBTTagList();
        for (int i = 0; i < marks.size(); i++) {
            Mark m = marks.get(i);
            NBTTagCompound t = new NBTTagCompound();
            t.setInteger("dim", m.dim);
            t.setInteger("x", m.x);
            t.setInteger("y", m.y);
            t.setInteger("z", m.z);
            t.setBoolean("store", m.store);
            t.setBoolean("eu", m.eu);
            t.setInteger("ramp", m.ramp);
            l.appendTag(t);
        }
        nbt.setTag("marks", l);
    }
}
