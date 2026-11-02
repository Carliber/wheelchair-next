package wheelchair.compat.mixins.hbm;

import com.hbm.items.tool.ItemCasingBag.InventoryCasingBag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import org.spongepowered.asm.mixin.Overwrite;

import wheelchair.compat.helper.Configurables;



@Mixin(InventoryCasingBag.class)
@Pseudo
public abstract class MixinInventoryCasingBag {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
