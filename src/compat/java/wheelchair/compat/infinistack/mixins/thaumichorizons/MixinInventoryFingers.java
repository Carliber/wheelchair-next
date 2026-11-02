package wheelchair.compat.mixins.thaumichorizons;

import com.kentington.thaumichorizons.common.container.InventoryFingers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(InventoryFingers.class)
@Pseudo
public abstract class MixinInventoryFingers {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
