package wheelchair.compat.mixins.bibliocraft;

import jds.bibliocraft.blocks.SlotWorkbenchBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(SlotWorkbenchBook.class)
@Pseudo
public abstract class MixinSlotWorkbenchBook {

    @Overwrite(remap = false)
    public int func_75219_a() {
        return Configurables.maxStackSize;
    }

}
