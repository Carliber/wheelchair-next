package wheelchair.compat.mixins.draconic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(targets = "com.brandon3055.draconicevolution.common.container.ContainerDissEnchanter$SlotBook")
@Pseudo
public abstract class MixinSlotBook {

    @Overwrite(remap = false)
    public int func_75219_a() {
        return Configurables.maxStackSize;
    }

}
