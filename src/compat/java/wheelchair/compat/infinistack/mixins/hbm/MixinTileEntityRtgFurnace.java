package wheelchair.compat.mixins.hbm;

import com.hbm.tileentity.machine.TileEntityRtgFurnace; // hbm\tileentity\machine\TileEntityRtgFurnace.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityRtgFurnace.class)
@Pseudo
public abstract class MixinTileEntityRtgFurnace {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}