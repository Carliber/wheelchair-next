package wheelchair.compat.mixins.hbm;

import com.hbm.tileentity.bomb.TileEntityNukeBoy; // hbm\tileentity\bomb\TileEntityNukeBoy.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityNukeBoy.class)
@Pseudo
public abstract class MixinTileEntityNukeBoy {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}