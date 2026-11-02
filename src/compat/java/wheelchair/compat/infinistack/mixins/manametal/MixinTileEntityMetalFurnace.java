package wheelchair.compat.mixins.manametal;

import project.studio.manametalmod.tileentity.TileEntityMetalFurnace; // tileentity\TileEntityMetalFurnace.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;


@Mixin(TileEntityMetalFurnace.class)
@Pseudo
public abstract class MixinTileEntityMetalFurnace {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}