package wheelchair.compat.mixins.floodlights;

import de.keridos.floodlights.tileentity.TileEntityMetaFloodlight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityMetaFloodlight.class)
@Pseudo
public abstract class MixinTileEntityMetaFloodlight {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
