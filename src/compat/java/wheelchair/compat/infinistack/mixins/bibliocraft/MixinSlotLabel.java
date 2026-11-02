package wheelchair.compat.mixins.bibliocraft;

import jds.bibliocraft.blocks.SlotLabel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(SlotLabel.class)
@Pseudo
public abstract class MixinSlotLabel {

    @Overwrite(remap = false)
    public int func_75219_a() {
        return Configurables.maxStackSize;
    }

}
