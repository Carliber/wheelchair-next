package wheelchair.compat.mixins.bibliocraft;

import jds.bibliocraft.tileentities.TileEntityLabel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityLabel.class)
@Pseudo
public abstract class MixinTileEntityLabel {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
