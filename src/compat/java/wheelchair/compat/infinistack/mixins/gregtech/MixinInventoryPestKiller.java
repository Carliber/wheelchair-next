package wheelchair.compat.mixins.gregtech;

import gtPlusPlus.core.inventories.InventoryPestKiller; // gtPlusPlus\core\inventories\InventoryPestKiller.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(InventoryPestKiller.class)
@Pseudo
public abstract class MixinInventoryPestKiller {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}