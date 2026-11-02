package wheelchair.compat.mixins;

import net.minecraft.inventory.InventoryMerchant;
import wheelchair.compat.helper.Configurables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(InventoryMerchant.class)
public abstract class MixinInventoryMerchant {

    @Overwrite
    public int getInventoryStackLimit()
    {
        return Configurables.maxStackSize;
    }
}
