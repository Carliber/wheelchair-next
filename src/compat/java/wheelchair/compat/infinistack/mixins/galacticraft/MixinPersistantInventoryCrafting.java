package wheelchair.compat.mixins.galacticraft;

import micdoodle8.mods.galacticraft.core.inventory.PersistantInventoryCrafting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(PersistantInventoryCrafting.class)
@Pseudo
public abstract class MixinPersistantInventoryCrafting {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
