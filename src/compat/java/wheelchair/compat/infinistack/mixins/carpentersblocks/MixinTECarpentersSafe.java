package wheelchair.compat.mixins.carpentersblocks;

import com.carpentersblocks.tileentity.TECarpentersSafe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TECarpentersSafe.class)
@Pseudo
public abstract class MixinTECarpentersSafe {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
