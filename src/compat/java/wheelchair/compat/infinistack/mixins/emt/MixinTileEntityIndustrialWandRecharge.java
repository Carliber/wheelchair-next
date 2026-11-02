package wheelchair.compat.mixins.emt;

import emt.tile.TileEntityIndustrialWandRecharge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityIndustrialWandRecharge.class)
@Pseudo
public abstract class MixinTileEntityIndustrialWandRecharge {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
