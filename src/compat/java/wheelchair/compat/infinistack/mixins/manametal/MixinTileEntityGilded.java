package wheelchair.compat.mixins.manametal;

import project.studio.manametalmod.Lapuda.TileEntityGilded; // Lapuda\TileEntityGilded.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;


@Mixin(TileEntityGilded.class)
@Pseudo
public abstract class MixinTileEntityGilded {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}