package wheelchair.compat.mixins.thaumicexploration;

import flaxbeard.thaumicexploration.misc.TempInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TempInventory.class)
@Pseudo
public abstract class MixinTempInventory {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
