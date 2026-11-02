package wheelchair.compat.mixins.bibliocraft;

import jds.bibliocraft.tileentities.TileEntityFramedChest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityFramedChest.class)
@Pseudo
public abstract class MixinTileEntityFramedChest {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
