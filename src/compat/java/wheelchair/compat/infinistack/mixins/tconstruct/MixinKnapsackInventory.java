package wheelchair.compat.mixins.tconstruct;

import tconstruct.armor.player.KnapsackInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(KnapsackInventory.class)
@Pseudo
public abstract class MixinKnapsackInventory {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
