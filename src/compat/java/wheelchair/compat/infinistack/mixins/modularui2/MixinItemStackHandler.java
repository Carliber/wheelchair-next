package wheelchair.compat.mixins.modularui2;

import com.cleanroommc.modularui.utils.item.ItemStackHandler;
import wheelchair.compat.helper.Configurables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

@Mixin(ItemStackHandler.class)
@Pseudo
public abstract class MixinItemStackHandler {

    @Overwrite(remap = false)
    public int getSlotLimit(int slot) {
        return Configurables.maxStackSize;
    }
}
