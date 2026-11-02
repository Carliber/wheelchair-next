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
 * TEST generator (测试发电机): an INFINITE RF source for power testing —
 * extractEnergy always grants whatever is asked (nothing is consumed,
 * nothing is stored). Right-click reports how much has been drawn through it.
 */
public class BlockGenerator extends BlockContainer {

    public BlockGenerator() {
        super(Material.iron);
        setBlockName("wheelchair.generator");
        setHardness(3.0F);
        setResistance(10.0F);
        setCreativeTab(CreativeTabs.tabDecorations);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileGenerator();
    }

    @Override
    public void registerBlockIcons(IIconRegister r) {
        blockIcon = r.registerIcon("wheelchair:energy_generator");
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return blockIcon;
    }

    /** right-click: report the drawn-total (server side) */
    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer p, int side,
                                     float hx, float hy, float hz) {
        if (wheelchair.item.ItemStaff.gesturing(p, w)) return false; // 手杖电力手势优先
        if (!w.isRemote) {
            TileEntity te = w.getTileEntity(x, y, z);
            if (te instanceof TileGenerator) {
                p.addChatMessage(new net.minecraft.util.ChatComponentText(
                        "测试发电机: 已输出 " + ((TileGenerator) te).produced + " RF"));
            }
        }
        return true;
    }

    /** the infinite source: grants every request, tracks the total for the report */
    public static class TileGenerator extends TileEntity implements cofh.api.energy.IEnergyHandler {

        public long produced;

        @Override
        public int extractEnergy(net.minecraftforge.common.util.ForgeDirection from, int maxExtract, boolean simulate) {
            if (!simulate) {
                produced += maxExtract;
                markDirty();
            }
            return maxExtract; // infinite: nothing drains
        }

        @Override
        public int receiveEnergy(net.minecraftforge.common.util.ForgeDirection from, int maxReceive, boolean simulate) {
            return 0; // a source accepts nothing
        }

        @Override
        public int getEnergyStored(net.minecraftforge.common.util.ForgeDirection from) {
            return Integer.MAX_VALUE;
        }

        @Override
        public int getMaxEnergyStored(net.minecraftforge.common.util.ForgeDirection from) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean canConnectEnergy(net.minecraftforge.common.util.ForgeDirection from) {
            return true;
        }

        @Override
        public void readFromNBT(net.minecraft.nbt.NBTTagCompound nbt) {
            super.readFromNBT(nbt);
            produced = Math.max(0L, nbt.getLong("produced"));
        }

        @Override
        public void writeToNBT(net.minecraft.nbt.NBTTagCompound nbt) {
            super.writeToNBT(nbt);
            nbt.setLong("produced", produced);
        }
    }
}
