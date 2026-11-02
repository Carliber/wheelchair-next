package wheelchair.compat.mixins.bloodmagic;

import WayofTime.alchemicalWizardry.common.tileEntity.TEAlchemicCalcinator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TEAlchemicCalcinator.class)
@Pseudo
public abstract class MixinTEAlchemicCalcinator {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
