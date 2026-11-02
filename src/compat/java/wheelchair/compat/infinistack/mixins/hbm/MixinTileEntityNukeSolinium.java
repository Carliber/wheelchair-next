package wheelchair.compat.mixins.hbm;

import com.hbm.tileentity.bomb.TileEntityNukeSolinium; // hbm\tileentity\bomb\TileEntityNukeSolinium.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityNukeSolinium.class)
@Pseudo
public abstract class MixinTileEntityNukeSolinium {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}