package wheelchair.compat.mixins.bibliocraft;

import jds.bibliocraft.items.SlotAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(SlotAtlas.class)
@Pseudo
public abstract class MixinSlotAtlas {

    @Overwrite(remap = false)
    public int func_75219_a() {
        return Configurables.maxStackSize;
    }

}
