package wheelchair.compat.mixins.opencomputers;

import li.cil.oc.common.tileentity.Robot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import wheelchair.compat.helper.Configurables;

@Mixin(Robot.class)
@Pseudo
public abstract class MixinRobot {

    @Overwrite(remap = false)
    public int func_70297_j_() {
        return Configurables.maxStackSize;
    }

}
