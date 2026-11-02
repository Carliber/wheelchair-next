package wheelchair.compat.mixins.manametal;

import project.studio.manametalmod.blueprint.TileEntityBuild; // blueprint\TileEntityBuild.java
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import org.spongepowered.asm.mixin.Overwrite;
import wheelchair.compat.helper.Configurables;


@Mixin(TileEntityBuild.class)
@Pseudo
public abstract class MixinTileEntityBuild {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}