package wheelchair.compat.mixins.hbm;

import com.hbm.tileentity.machine.TileEntityMachineShredder; // hbm\tileentity\machine\TileEntityMachineShredder.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityMachineShredder.class)
@Pseudo
public abstract class MixinTileEntityMachineShredder {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}