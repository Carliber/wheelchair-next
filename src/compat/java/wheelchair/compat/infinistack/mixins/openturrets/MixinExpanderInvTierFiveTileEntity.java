package wheelchair.compat.mixins.openturrets;

import openmodularturrets.tileentity.expander.ExpanderInvTierFiveTileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(ExpanderInvTierFiveTileEntity.class)
@Pseudo
public abstract class MixinExpanderInvTierFiveTileEntity {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
