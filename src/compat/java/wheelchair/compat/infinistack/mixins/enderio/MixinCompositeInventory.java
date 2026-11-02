package wheelchair.compat.mixins.enderio;

import crazypants.enderio.machine.hypercube.CompositeInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(CompositeInventory.class)
@Pseudo
public abstract class MixinCompositeInventory {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
