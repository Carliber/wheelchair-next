package wheelchair.compat.mixins.buildcraft;

import buildcraft.core.lib.engines.TileEngineWithInventory;
import wheelchair.compat.helper.Configurables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TileEngineWithInventory.class)
@Pseudo
public abstract class MixinTileEngineWithInventory {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
