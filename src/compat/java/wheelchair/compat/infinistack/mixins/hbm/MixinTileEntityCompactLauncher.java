package wheelchair.compat.mixins.hbm;

import com.hbm.tileentity.bomb.TileEntityCompactLauncher; // hbm\tileentity\bomb\TileEntityCompactLauncher.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityCompactLauncher.class)
@Pseudo
public abstract class MixinTileEntityCompactLauncher {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}