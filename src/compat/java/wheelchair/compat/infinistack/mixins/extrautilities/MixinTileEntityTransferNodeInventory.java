package wheelchair.compat.mixins.extrautilities;

import com.rwtema.extrautils.tileentity.transfernodes.TileEntityTransferNodeInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityTransferNodeInventory.class)
@Pseudo
public abstract class MixinTileEntityTransferNodeInventory {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
