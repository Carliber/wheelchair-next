package wheelchair.compat.mixins.bibliocraft;

import jds.bibliocraft.tileentities.TileEntityDiscRack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityDiscRack.class)
@Pseudo
public abstract class MixinTileEntityDiscRack {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
