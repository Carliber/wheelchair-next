package wheelchair.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Big-stacking save format (InfiniStack idea): vanilla stores the stack Count
 * as a NBT BYTE — int-max stacks wrap negative and the item is destroyed on
 * save/load. Write an int under the same key; readers that only see a byte
 * (vanilla) still work for small counts, our reader prefers the int.
 *
 * NOTE: no @Shadow on the public field — field shadows need RUNTIME mapping
 * that our AP/refmap chain does not emit (production InvalidMixinException).
 * Accessing it through a cast to the target compiles a normal field reference
 * which ForgeGradle's reobf rewrites to the SRG name at build time.
 */
@Mixin(ItemStack.class)
public abstract class MixinItemStack {

    private ItemStack wheelchair$self() {
        return (ItemStack) (Object) this;
    }

    @Inject(method = "writeToNBT", at = @At("RETURN"))
    private void wheelchair$writeIntCount(NBTTagCompound nbt, CallbackInfoReturnable<NBTTagCompound> cir) {
        nbt.setInteger("Count", wheelchair$self().stackSize);
    }

    @Inject(method = "readFromNBT", at = @At("RETURN"))
    private void wheelchair$readIntCount(NBTTagCompound nbt, CallbackInfo ci) {
        if (nbt.hasKey("Count", 3)) { // 3 = int
            wheelchair$self().stackSize = nbt.getInteger("Count");
        } else {
            wheelchair$self().stackSize = nbt.getByte("Count");
        }
    }
}
