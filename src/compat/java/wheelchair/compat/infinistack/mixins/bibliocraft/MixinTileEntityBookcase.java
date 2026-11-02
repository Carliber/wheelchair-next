package wheelchair.compat.mixins.bibliocraft;

import jds.bibliocraft.tileentities.TileEntityBookcase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityBookcase.class)
@Pseudo
public abstract class MixinTileEntityBookcase {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
