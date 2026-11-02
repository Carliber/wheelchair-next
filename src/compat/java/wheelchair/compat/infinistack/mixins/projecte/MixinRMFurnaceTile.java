package wheelchair.compat.mixins.projecte;

import moze_intel.projecte.gameObjs.tiles.RMFurnaceTile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(RMFurnaceTile.class)
@Pseudo
public abstract class MixinRMFurnaceTile {

    @Overwrite(remap = false)
    public int func_70297_j_()
    {
        return Configurables.maxStackSize;
    }

}
