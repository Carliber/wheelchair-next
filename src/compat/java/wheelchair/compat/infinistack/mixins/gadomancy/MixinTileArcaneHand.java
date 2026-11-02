package wheelchair.compat.mixins.gadomancy;

import makeo.gadomancy.common.blocks.tiles.TileArcaneHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileArcaneHand.class)
@Pseudo
public abstract class MixinTileArcaneHand {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
