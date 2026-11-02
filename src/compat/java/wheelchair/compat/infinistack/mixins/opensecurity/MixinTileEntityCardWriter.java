package wheelchair.compat.mixins.opensecurity;

import pcl.opensecurity.tileentity.TileEntityCardWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityCardWriter.class)
@Pseudo
public abstract class MixinTileEntityCardWriter {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
