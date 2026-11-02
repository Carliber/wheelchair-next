package wheelchair.compat.mixins.thaumichorizons;

import com.kentington.thaumichorizons.common.tiles.TileInspiratron;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileInspiratron.class)
@Pseudo
public abstract class MixinTileInspiratron {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
