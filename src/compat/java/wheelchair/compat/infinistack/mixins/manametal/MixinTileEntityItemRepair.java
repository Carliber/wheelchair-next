package wheelchair.compat.mixins.manametal;

import project.studio.manametalmod.tileentity.TileEntityItemRepair; // tileentity\TileEntityItemRepair.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;


@Mixin(TileEntityItemRepair.class)
@Pseudo
public abstract class MixinTileEntityItemRepair {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}