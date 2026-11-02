package wheelchair.compat.mixins.galaxyspace;

import galaxyspace.core.tile.machine.TileEntitySolarWind;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntitySolarWind.class)
@Pseudo
public abstract class MixinTileEntitySolarWind {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
