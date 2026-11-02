package wheelchair.compat.mixins.endercore;

import com.enderio.core.common.util.ArrayInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(ArrayInventory.class)
@Pseudo
public abstract class MixinArrayInventory {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
