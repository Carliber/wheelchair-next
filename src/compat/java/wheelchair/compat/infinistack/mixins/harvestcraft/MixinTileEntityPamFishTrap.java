package wheelchair.compat.mixins.harvestcraft;

import com.pam.harvestcraft.TileEntityPamFishTrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityPamFishTrap.class)
@Pseudo
public abstract class MixinTileEntityPamFishTrap {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
