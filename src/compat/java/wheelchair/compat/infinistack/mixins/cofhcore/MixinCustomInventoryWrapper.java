package wheelchair.compat.mixins.cofhcore;

import cofh.lib.gui.container.CustomInventoryWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(CustomInventoryWrapper.class)
@Pseudo
public abstract class MixinCustomInventoryWrapper {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
