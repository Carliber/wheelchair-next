package wheelchair.compat.mixins.hopperduct;

import fyber.hopperductmod.TileEntityGratedHopper; // fyber\hopperductmod\TileEntityGratedHopper.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityGratedHopper.class)
public abstract class MixinTileEntityGratedHopper {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}