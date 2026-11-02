package wheelchair.compat.mixins.hopperduct;

import fyber.hopperductmod.TileEntityHopperDuct; // fyber\hopperductmod\TileEntityHopperDuct.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityHopperDuct.class)
public abstract class MixinTileEntityHopperDuct {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}