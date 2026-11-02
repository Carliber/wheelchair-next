package wheelchair.compat.mixins.hee;

import chylex.hee.gui.slots.SlotBrewingStandIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(SlotBrewingStandIngredient.class)
@Pseudo
public abstract class MixinSlotBrewingStandIngredient {

    @Overwrite(remap = false)
    public int func_75219_a() {
        return Configurables.maxStackSize;
    }

}
