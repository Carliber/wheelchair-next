package wheelchair.compat.mixins.railcraft;

import mods.railcraft.common.gui.slots.SlotStackFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(SlotStackFilter.class)
@Pseudo
public abstract class MixinSlotStackFilter {

    @Overwrite(remap = false)
    public int func_75219_a() {
        return Configurables.maxStackSize;
    }

}
