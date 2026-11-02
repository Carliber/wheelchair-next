package wheelchair.compat.mixins.gadomancy;

import makeo.gadomancy.common.blocks.tiles.TileArcaneDropper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileArcaneDropper.class)
@Pseudo
public abstract class MixinTileArcaneDropper {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
