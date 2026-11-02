package wheelchair.compat.mixins.hbm;

import com.hbm.tileentity.machine.TileEntityMachineKeyForge; // hbm\tileentity\machine\TileEntityMachineKeyForge.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityMachineKeyForge.class)
@Pseudo
public abstract class MixinTileEntityMachineKeyForge {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}