package wheelchair.compat.mixins.galaxyspace;

import galaxyspace.core.tile.machine.TileEntityGeothermalGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityGeothermalGenerator.class)
@Pseudo
public abstract class MixinTileEntityGeothermalGenerator {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
