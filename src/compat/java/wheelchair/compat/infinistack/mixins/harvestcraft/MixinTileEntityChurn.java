package wheelchair.compat.mixins.harvestcraft;

import com.pam.harvestcraft.TileEntityChurn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityChurn.class)
@Pseudo
public abstract class MixinTileEntityChurn {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
