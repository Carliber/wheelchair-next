package wheelchair.compat.mixins.ic2nuclear;

import shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityRemoteThermo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityRemoteThermo.class)
@Pseudo
public abstract class MixinTileEntityRemoteThermo {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
