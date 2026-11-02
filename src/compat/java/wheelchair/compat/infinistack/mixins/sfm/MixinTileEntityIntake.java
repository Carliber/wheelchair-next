package wheelchair.compat.mixins.sfm;

import vswe.stevesfactory.blocks.TileEntityIntake;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityIntake.class)
@Pseudo
public abstract class MixinTileEntityIntake {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
