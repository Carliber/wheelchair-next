package wheelchair.compat.mixins.railcraft;

import mods.railcraft.common.util.inventory.InventoryConcatenator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(InventoryConcatenator.class)
@Pseudo
public abstract class MixinInventoryConcatenator {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
