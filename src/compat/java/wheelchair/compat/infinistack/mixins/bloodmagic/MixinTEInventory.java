package wheelchair.compat.mixins.bloodmagic;

import WayofTime.alchemicalWizardry.common.tileEntity.TEInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TEInventory.class)
@Pseudo
public abstract class MixinTEInventory {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
