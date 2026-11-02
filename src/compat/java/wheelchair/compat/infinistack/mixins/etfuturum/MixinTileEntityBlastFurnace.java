package wheelchair.compat.mixins.etfuturum;

import ganymedes01.etfuturum.tileentities.TileEntityBlastFurnace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityBlastFurnace.class)
@Pseudo
public abstract class MixinTileEntityBlastFurnace {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
