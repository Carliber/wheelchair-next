package wheelchair.compat.mixins.emt;

import emt.tile.TileEntityEtherealMacerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityEtherealMacerator.class)
@Pseudo
public abstract class MixinTileEntityEtherealMacerator {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
