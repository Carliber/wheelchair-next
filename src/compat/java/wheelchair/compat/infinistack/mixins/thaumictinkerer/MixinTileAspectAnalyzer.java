package wheelchair.compat.mixins.thaumictinkerer;

import thaumic.tinkerer.common.block.tile.TileAspectAnalyzer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileAspectAnalyzer.class)
@Pseudo
public abstract class MixinTileAspectAnalyzer {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
