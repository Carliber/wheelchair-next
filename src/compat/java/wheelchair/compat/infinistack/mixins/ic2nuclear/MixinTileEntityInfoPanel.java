package wheelchair.compat.mixins.ic2nuclear;

import shedar.mods.ic2.nuclearcontrol.tileentities.TileEntityInfoPanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(TileEntityInfoPanel.class)
@Pseudo
public abstract class MixinTileEntityInfoPanel {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
