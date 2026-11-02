package wheelchair.compat.mixins.gregtech;

import gtPlusPlus.core.inventories.projecttable.InventoryProjectMain; // gtPlusPlus\core\inventories\projecttable\InventoryProjectMain.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(InventoryProjectMain.class)
@Pseudo
public abstract class MixinInventoryProjectMain {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}