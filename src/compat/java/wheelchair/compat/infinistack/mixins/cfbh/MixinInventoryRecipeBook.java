package wheelchair.compat.mixins.cfbh;

import net.blay09.mods.cookingforblockheads.container.inventory.InventoryRecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(InventoryRecipeBook.class)
@Pseudo
public abstract class MixinInventoryRecipeBook {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
