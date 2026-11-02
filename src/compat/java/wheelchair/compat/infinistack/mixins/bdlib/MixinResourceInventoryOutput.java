package wheelchair.compat.mixins.bdlib;

import net.bdew.lib.resource.ResourceInventoryOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(ResourceInventoryOutput.class)
@Pseudo
public abstract class MixinResourceInventoryOutput {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
