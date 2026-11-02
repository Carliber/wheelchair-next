package wheelchair.compat.mixins.bibliocraft;

import jds.bibliocraft.tileentities.TileEntityPaintPress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityPaintPress.class)
@Pseudo
public abstract class MixinTileEntityPaintPress {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
