package wheelchair.compat.mixins.railcraft;

import mods.railcraft.common.gui.slots.SlotFuel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(SlotFuel.class)
@Pseudo
public abstract class MixinSlotFuel {

    @Overwrite(remap = false)
    public int func_75219_a() {
        return Configurables.maxStackSize;
    }

}
