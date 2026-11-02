package wheelchair.compat.mixins.tconstruct;

import tconstruct.armor.player.ArmorExtended;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(ArmorExtended.class)
@Pseudo
public abstract class MixinArmorExtended {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
