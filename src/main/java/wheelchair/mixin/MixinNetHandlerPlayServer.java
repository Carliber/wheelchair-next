package wheelchair.mixin;

import net.minecraft.network.NetHandlerPlayServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Creative C10 packets carry a server-side guard: stackSize <= 64 or the
 * action is silently DROPPED (NetHandlerPlayServer.processCreativeInventoryAction,
 * vanilla line "stackSize <= 64 && stackSize > 0"). The client already showed
 * the item locally (creative GUIs are client-authoritative), so a big stack
 * looks placed — until the next vanilla container sync (opening ANY machine)
 * reveals the server truth: gone. Lifting the cap to int-max lets creative
 * placement of big stacks actually stick.
 */
@Mixin(NetHandlerPlayServer.class)
public abstract class MixinNetHandlerPlayServer {

    /** the only 64 constant in that method is the clamp; hotbar math uses getHotbarSize() */
    @ModifyConstant(method = "processCreativeInventoryAction", constant = @Constant(intValue = 64))
    private int wheelchair$bigCreativeStacks(int original) {
        return Integer.MAX_VALUE;
    }
}
