package wheelchair.compat.mixins.hbm;

import com.hbm.tileentity.TileEntityProxyInventory;
import wheelchair.compat.helper.Configurables;
import net.minecraft.inventory.ISidedInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TileEntityProxyInventory.class)
@Pseudo
public abstract class MixinTileEntityProxyInventory {

    @Shadow(remap = false)
    protected abstract ISidedInventory getBase();

    @Overwrite(remap = false)
    public int func_70297_j_() {
        ISidedInventory inv = this.getBase();

        if(inv != null)
            return inv.getInventoryStackLimit();

        return Configurables.maxStackSize;
    }

}
