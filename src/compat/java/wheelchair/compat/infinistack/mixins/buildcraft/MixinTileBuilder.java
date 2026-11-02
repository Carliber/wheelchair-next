package wheelchair.compat.mixins.buildcraft;

import buildcraft.builders.TileBuilder;
import wheelchair.compat.helper.Configurables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;



@Mixin(TileBuilder.class)
@Pseudo
public abstract class MixinTileBuilder {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
