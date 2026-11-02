package wheelchair.compat.mixins.hbm;

import com.hbm.tileentity.machine.TileEntityMachineSiren; // hbm\tileentity\machine\TileEntityMachineSiren.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityMachineSiren.class)
@Pseudo
public abstract class MixinTileEntityMachineSiren {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}