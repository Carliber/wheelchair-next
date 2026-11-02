package wheelchair.mixin;

import net.minecraft.command.CommandGive;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** vanilla /give clamps stacks at 64 — lift to int-max (InfiniStack law) */
@Mixin(CommandGive.class)
public abstract class MixinCommandGive {

    @ModifyConstant(method = "processCommand", constant = @Constant(intValue = 64))
    private int wheelchair$bigGive(int original) {
        return Integer.MAX_VALUE;
    }
}
