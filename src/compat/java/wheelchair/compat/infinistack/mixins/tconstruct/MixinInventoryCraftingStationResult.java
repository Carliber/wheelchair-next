package wheelchair.compat.mixins.tconstruct;

import tconstruct.tools.inventory.InventoryCraftingStationResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(InventoryCraftingStationResult.class)
@Pseudo
public abstract class MixinInventoryCraftingStationResult {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
