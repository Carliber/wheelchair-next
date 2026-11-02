package wheelchair.compat.mixins.galaxyspace;

import galaxyspace.core.tile.machine.TileEntityFuelGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityFuelGenerator.class)
@Pseudo
public abstract class MixinTileEntityFuelGenerator {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
