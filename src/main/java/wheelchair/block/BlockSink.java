package wheelchair.block;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/**
 * TEST sink (能量垃圾桶): destroys energy for power testing. It PULLS up to
 * PULL_RATE RF/tick from every adjacent IEnergyProvider (so no conduit is
 * needed — park it next to the generator) and also accepts anything pushed
 * into it via receiveEnergy. Right-click reports the destroyed total.
 */
public class BlockSink extends BlockContainer {

    public BlockSink() {
        super(Material.iron);
        setBlockName("wheelchair.sink");
        setHardness(3.0F);
        setResistance(10.0F);
        setCreativeTab(CreativeTabs.tabDecorations);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileSink();
    }

    @Override
    public void registerBlockIcons(IIconRegister r) {
        blockIcon = r.registerIcon("wheelchair:energy_sink");
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
            if (te instanceof TileSink) {
                p.addChatMessage(new net.minecraft.util.ChatComponentText(
                        "能量垃圾桶: 已销毁 " + ((TileSink) te).destroyed + " RF"));
            }
        }
        return true;
    }

    public static class TileSink extends TileEntity implements cofh.api.energy.IEnergyHandler {

        public long destroyed;
        /** RF pulled per adjacent provider per tick */
        public static final int PULL_RATE = 1000;

        @Override
        public void updateEntity() {
            if (this.worldObj == null || this.worldObj.isRemote) return;
            for (ForgeDirection dir : ForgeDirection.values()) {
                if (dir == ForgeDirection.UNKNOWN) continue;
                TileEntity t = this.worldObj.getTileEntity(
                        this.xCoord + dir.offsetX, this.yCoord + dir.offsetY, this.zCoord + dir.offsetZ);
                if (t instanceof cofh.api.energy.IEnergyProvider && t != this) {
                    int got = ((cofh.api.energy.IEnergyProvider) t)
                            .extractEnergy(dir.getOpposite(), PULL_RATE, false);
                    if (got > 0) {
                        destroyed += got;
                        markDirty();
                    }
                }
            }
        }

        @Override
        public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
            // pushed energy is destroyed too
            if (!simulate) {
                destroyed += maxReceive;
                markDirty();
            }
            return maxReceive; // accepts everything
        }

        @Override
        public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
            return 0; // nothing comes back out of the trash
        }

        @Override
        public int getEnergyStored(ForgeDirection from) {
            return 0;
        }

        @Override
        public int getMaxEnergyStored(ForgeDirection from) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canConnectEnergy(ForgeDirection from) {
            return true;
        }

        @Override
        public void readFromNBT(net.minecraft.nbt.NBTTagCompound nbt) {
            super.readFromNBT(nbt);
            destroyed = Math.max(0L, nbt.getLong("destroyed"));
        }

        @Override
        public void writeToNBT(net.minecraft.nbt.NBTTagCompound nbt) {
            super.writeToNBT(nbt);
            nbt.setLong("destroyed", destroyed);
        }
    }
}
