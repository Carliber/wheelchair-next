package wheelchair.compat.mixins.bibliocraft;

import jds.bibliocraft.tileentities.TileEntityGenericShelf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityGenericShelf.class)
@Pseudo
public abstract class MixinTileEntityGenericShelf {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
