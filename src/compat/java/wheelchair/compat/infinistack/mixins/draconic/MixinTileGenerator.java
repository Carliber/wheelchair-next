package wheelchair.compat.mixins.draconic;

import com.brandon3055.draconicevolution.common.tileentities.TileGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileGenerator.class)
@Pseudo
public abstract class MixinTileGenerator {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
