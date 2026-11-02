package wheelchair.compat.mixins.openturrets;

import openmodularturrets.tileentity.TileEntityContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityContainer.class)
@Pseudo
public abstract class MixinTileEntityContainer {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
