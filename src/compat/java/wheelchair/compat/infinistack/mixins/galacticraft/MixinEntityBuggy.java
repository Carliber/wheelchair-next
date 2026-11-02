package wheelchair.compat.mixins.galacticraft;

import micdoodle8.mods.galacticraft.core.entities.EntityBuggy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(EntityBuggy.class)
@Pseudo
public abstract class MixinEntityBuggy {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
