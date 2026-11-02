package wheelchair.compat.mixins.hbm;

import com.hbm.tileentity.machine.TileEntityMachineRTG; // hbm\tileentity\machine\TileEntityMachineRTG.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityMachineRTG.class)
@Pseudo
public abstract class MixinTileEntityMachineRTG {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}