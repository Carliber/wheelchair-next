package wheelchair.compat.mixins.daoza;

import com.daozcraft.container.InvMultiwand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;
import org.spongepowered.asm.mixin.Pseudo;

@Mixin(InvMultiwand.class)
@Pseudo
public abstract class MixinInvMultiwand {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}