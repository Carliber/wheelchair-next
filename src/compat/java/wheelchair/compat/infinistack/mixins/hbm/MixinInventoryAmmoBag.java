package wheelchair.compat.mixins.hbm;

import com.hbm.items.tool.ItemAmmoBag.InventoryAmmoBag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(InventoryAmmoBag.class)
@Pseudo
public abstract class MixinInventoryAmmoBag {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
