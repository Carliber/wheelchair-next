package wheelchair.compat.mixins.ae2;

import appeng.parts.layers.LayerISidedInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(LayerISidedInventory.class)
@Pseudo
public abstract class MixinLayerISidedInventory {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
