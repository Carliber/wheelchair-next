package wheelchair.compat.mixins.automagy;

import tuhljin.automagy.tiles.ModTileEntityWithInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(ModTileEntityWithInventory.class)
@Pseudo
public abstract class MixinModTileEntityWithInventory {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
