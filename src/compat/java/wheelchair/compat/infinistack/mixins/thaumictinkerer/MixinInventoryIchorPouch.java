package wheelchair.compat.mixins.thaumictinkerer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(targets = "thaumic.tinkerer.common.block.tile.container.kami.ContainerIchorPouch$InventoryIchorPouch")
@Pseudo
public abstract class MixinInventoryIchorPouch {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
