package wheelchair.compat.mixins.daoza;

import com.daozcraft.container.Slot_EX;
import wheelchair.compat.helper.Configurables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Mixin(Slot_EX.class)
@Pseudo
public abstract class MixinSlot_EX {

    @Overwrite(remap = false)
    public int func_75219_a() {
        return Configurables.maxStackSize;
    }

}
