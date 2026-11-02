package wheelchair.compat.mixins.bibliocraft;

import jds.bibliocraft.blocks.SlotWritingDeskBooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(SlotWritingDeskBooks.class)
@Pseudo
public abstract class MixinSlotWritingDeskBooks {

    @Overwrite(remap = false)
    public int func_75219_a() {
        return Configurables.maxStackSize;
    }

}
