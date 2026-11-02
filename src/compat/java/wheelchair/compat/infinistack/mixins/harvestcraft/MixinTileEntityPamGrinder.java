package wheelchair.compat.mixins.harvestcraft;

import com.pam.harvestcraft.TileEntityPamGrinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityPamGrinder.class)
@Pseudo
public abstract class MixinTileEntityPamGrinder {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
