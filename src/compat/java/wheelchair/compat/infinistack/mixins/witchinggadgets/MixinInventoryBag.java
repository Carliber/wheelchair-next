package wheelchair.compat.mixins.witchinggadgets;

import witchinggadgets.common.gui.InventoryBag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(InventoryBag.class)
@Pseudo
public abstract class MixinInventoryBag {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
