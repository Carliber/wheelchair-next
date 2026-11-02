package wheelchair.compat.mixins.harvestcraft;

import com.pam.harvestcraft.TileEntityQuern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityQuern.class)
@Pseudo
public abstract class MixinTileEntityQuern {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
