package wheelchair.compat.mixins.hbm;

import com.hbm.tileentity.bomb.TileEntityNukeCustom; // hbm\tileentity\bomb\TileEntityNukeCustom.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityNukeCustom.class)
@Pseudo
public abstract class MixinTileEntityNukeCustom {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}