package wheelchair.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

/**
 * Battery block (电池方块, ported from the old tree verbatim): a placed
 * dual-role RF node. The TE exposes cofh IEnergyHandler to neighbours; it only
 * buffers one working set internally — SURPLUS is pushed into the global
 * storage pool ("rf"), deficits are topped up from it (Mekanism energy-cube
 * single-ledger pattern). Right-click reports buffer + pool.
 */
public class BlockBattery extends BlockContainer {

    public BlockBattery() {
        super(Material.iron);
        setBlockName("wheelchair.battery");
        setHardness(3.0F);
        setResistance(10.0F);
        setCreativeTab(CreativeTabs.tabDecorations);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileBattery();
    }

    @Override
    public void registerBlockIcons(IIconRegister r) {
        blockIcon = r.registerIcon("wheelchair:energy_battery");
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return blockIcon;
    }

    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side,
                                     float hx, float hy, float hz) {
        if (wheelchair.item.ItemStaff.gesturing(p, w)) return false; // 手杖电力手势优先
        if (!w.isRemote) {
            TileEntity te = w.getTileEntity(x, y, z);
            if (te instanceof TileBattery) {
                wheelchair.value.StorageData st = wheelchair.value.StorageData.get(w);
                p.addChatMessage(new net.minecraft.util.ChatComponentText(
                        "电池: 缓冲 " + ((TileBattery) te).buffer + "/" + TileBattery.BUFFER_MAX
                                + " RF · 存储池 " + st.countRes("rf") + " RF"));
            }
        }
        return true;
    }

    /** the single-ledger RF<->storage-pool node. Also speaks IC2 EU on every
     *  face when IC2 is present (soft-dep: EnergyNet registration is gated on
     *  Loader.isModLoaded("IC2"), Mekanism's exact pattern) — EU is only a
     *  PORT conversion: 1 EU = euToRf RF against the same single ledger. */
    public static class TileBattery extends TileEntity
            implements cofh.api.energy.IEnergyHandler,
            ic2.api.energy.tile.IEnergySink, ic2.api.energy.tile.IEnergySource {

        /** internal working buffer (RF); surplus/deficit syncs with the pool */
        public long buffer = 0;
        public static final long BUFFER_MAX = 1000000L; // 1M RF
        /** per-tick pool<->buffer sync rate, each way */
        public static final long SYNC_RATE = 2000L;

        /** EnergyNet registered (only ever true with IC2 in the runtime) */
        private boolean ic2Registered;

        private static boolean ic2Present() {
            return cpw.mods.fml.common.Loader.isModLoaded("IC2");
        }

        @Override
        public void updateEntity() {
            if (this.worldObj == null || this.worldObj.isRemote) return;
            // soft-dep EnergyNet join (Mekanism TileEntityElectricBlock pattern)
            if (!ic2Registered && ic2Present()) {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS
                        .post(new ic2.api.energy.event.EnergyTileLoadEvent(this));
                ic2Registered = true;
            }
            // ACTIVE charge: pull from adjacent IEnergyProviders (no conduit needed
            // — the RF API itself never moves energy, someone must initiate)
            for (net.minecraftforge.common.util.ForgeDirection dir : net.minecraftforge.common.util.ForgeDirection.values()) {
                if (dir == net.minecraftforge.common.util.ForgeDirection.UNKNOWN) continue;
                net.minecraft.tileentity.TileEntity t = this.worldObj.getTileEntity(
                        this.xCoord + dir.offsetX, this.yCoord + dir.offsetY, this.zCoord + dir.offsetZ);
                if (t instanceof cofh.api.energy.IEnergyProvider && t != this && this.buffer < BUFFER_MAX) {
                    int got = ((cofh.api.energy.IEnergyProvider) t).extractEnergy(dir.getOpposite(),
                            (int) Math.min(SYNC_RATE, BUFFER_MAX - this.buffer), false);
                    if (got > 0) {
                        this.buffer += got;
                        this.markDirty();
                    }
                }
            }
            wheelchair.value.StorageData st = wheelchair.value.StorageData.get(this.worldObj);
            // keep a working set of SYNC_RATE in the buffer; push surplus, pull deficit
            if (this.buffer > SYNC_RATE) {
                long push = Math.min(this.buffer - SYNC_RATE, SYNC_RATE);
                if (st.addRes("rf", push)) this.buffer -= push;
            } else if (this.buffer < SYNC_RATE) {
                long want = Math.min(SYNC_RATE - this.buffer, SYNC_RATE);
                long got = st.takeRes("rf", want);
                if (got > 0) this.buffer += got;
            }
        }

        @Override
        public void invalidate() {
            super.invalidate();
            ic2Unregister();
        }

        @Override
        public void onChunkUnload() {
            super.onChunkUnload();
            ic2Unregister();
        }

        private void ic2Unregister() {
            if (ic2Registered) {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS
                        .post(new ic2.api.energy.event.EnergyTileUnloadEvent(this));
                ic2Registered = false;
            }
        }

        // ---------------- IC2 EU face (1 EU = euToRf RF, single ledger) ----------------

        @Override
        public boolean acceptsEnergyFrom(net.minecraft.tileentity.TileEntity emitter,
                                         net.minecraftforge.common.util.ForgeDirection dir) {
            return true;
        }

        @Override
        public double getDemandedEnergy() {
            // RF headroom converted to EU; overflow-accept per the API advice
            return (BUFFER_MAX - this.buffer) / (double) wheelchair.WheelchairMod.euToRf;
        }

        @Override
        public int getSinkTier() {
            return Integer.MAX_VALUE; // any voltage (API doc recommendation)
        }

        @Override
        public double injectEnergy(net.minecraftforge.common.util.ForgeDirection from, double amount, double voltage) {
            // accept ALL (overflow into the pool next tick); leftover = 0
            this.buffer += (long) (amount * wheelchair.WheelchairMod.euToRf);
            if (this.buffer > BUFFER_MAX) this.buffer = BUFFER_MAX;
            this.markDirty();
            return 0;
        }

        @Override
        public boolean emitsEnergyTo(net.minecraft.tileentity.TileEntity receiver,
                                     net.minecraftforge.common.util.ForgeDirection dir) {
            return true;
        }

        /** adaptive offer ramp (EU): starts at LV-safe 32/t, doubles each tick
         *  up to what the net ACTUALLY drew last tick. The packet voltage is then
         *  never more than an amount that was already consumed safely — any
         *  connected machine tolerates it, no explosions, and the output
         *  automatically follows each machine's appetite (user: 连的机器能接受
         *  多少输出多少). Re-ramps after idle. */
        private double lastDrawnEu;

        @Override
        public double getOfferedEnergy() {
            double cap = Math.min(this.lastDrawnEu * 2.0 + 32.0,
                    this.buffer / (double) wheelchair.WheelchairMod.euToRf);
            return cap;
        }

        @Override
        public void drawEnergy(double amount) {
            this.lastDrawnEu = amount; // remember what was safely consumed
            long rf = (long) (amount * wheelchair.WheelchairMod.euToRf);
            this.buffer -= Math.min(rf, this.buffer);
            this.markDirty();
        }

        @Override
        public int getSourceTier() {
            return 4; // EV (2048+ EU/t band) — 8192 offer is fine on direct links;
            // IC2 only enforces voltage limits on conductors (cables)
        }

        // ---------------- IEnergyHandler (RF face: neighbours wire directly, 1:1) ----------------

        @Override
        public int receiveEnergy(net.minecraftforge.common.util.ForgeDirection from, int maxReceive, boolean simulate) {
            int amt = (int) Math.min(maxReceive, Math.min(SYNC_RATE, BUFFER_MAX - this.buffer));
            if (amt <= 0) return 0;
            if (!simulate) {
                this.buffer += amt;
                this.markDirty();
            }
            return amt;
        }

        @Override
        public int extractEnergy(net.minecraftforge.common.util.ForgeDirection from, int maxExtract, boolean simulate) {
            int amt = (int) Math.min(maxExtract, Math.min(SYNC_RATE, this.buffer));
            if (amt <= 0) return 0;
            if (!simulate) {
                this.buffer -= amt;
                this.markDirty();
            }
            return amt;
        }

        @Override
        public int getEnergyStored(net.minecraftforge.common.util.ForgeDirection from) {
            return (int) Math.min(this.buffer, Integer.MAX_VALUE);
        }

        @Override
        public int getMaxEnergyStored(net.minecraftforge.common.util.ForgeDirection from) {
            return (int) Math.min(BUFFER_MAX, Integer.MAX_VALUE);
        }

        @Override
        public boolean canConnectEnergy(net.minecraftforge.common.util.ForgeDirection from) {
            return true;
        }

        @Override
        public void readFromNBT(net.minecraft.nbt.NBTTagCompound nbt) {
            super.readFromNBT(nbt);
            this.buffer = Math.max(0, Math.min(BUFFER_MAX, nbt.getLong("buf")));
        }

        @Override
        public void writeToNBT(net.minecraft.nbt.NBTTagCompound nbt) {
            super.writeToNBT(nbt);
            nbt.setLong("buf", this.buffer);
        }
    }
}
