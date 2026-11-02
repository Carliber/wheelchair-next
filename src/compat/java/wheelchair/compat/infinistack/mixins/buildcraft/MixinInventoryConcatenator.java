package wheelchair.compat.mixins.buildcraft;

import buildcraft.core.lib.inventory.InventoryConcatenator;
import wheelchair.compat.helper.Configurables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(InventoryConcatenator.class)
@Pseudo
public abstract class MixinInventoryConcatenator {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
