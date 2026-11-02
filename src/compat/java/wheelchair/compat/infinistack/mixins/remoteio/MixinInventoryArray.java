package wheelchair.compat.mixins.remoteio;

import remoteio.common.inventory.wrapper.InventoryArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(InventoryArray.class)
@Pseudo
public abstract class MixinInventoryArray {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
