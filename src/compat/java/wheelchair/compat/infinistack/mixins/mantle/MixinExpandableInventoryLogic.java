package wheelchair.compat.mixins.mantle;

import mantle.blocks.abstracts.ExpandableInventoryLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(ExpandableInventoryLogic.class)
@Pseudo
public abstract class MixinExpandableInventoryLogic {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
