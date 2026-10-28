package wheelchair.mixin;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * Big-stacking wire format (InfiniStack idea): vanilla writes stackSize as a
 * signed BYTE (max 127) — anything bigger wraps negative and the item
 * vanishes on every container sync. We APPEND a VarInt with the real count
 * after the NBT on write, and overwrite the byte value on read. Both sides
 * must run the mod; a vanilla peer still reads the truncated byte (same as
 * without this patch, never worse).
 */
@Mixin(value = PacketBuffer.class, priority = 1001)
public abstract class MixinPacketBuffer extends ByteBuf {

    // NO @Shadow methods — method shadows need runtime mapping our refmap
    // chain does not emit (production InvalidMixinException, caught live in
    // the 轮椅IC2测试 pack). Cast-call compiles a normal method reference that
    // ForgeGradle reobf rewrites to the SRG name at build time.
    private PacketBuffer wheelchair$self() {
        return (PacketBuffer) (Object) this;
    }

    @Inject(
            method = "writeItemStackToBuffer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/PacketBuffer;writeNBTTagCompoundToBuffer(Lnet/minecraft/nbt/NBTTagCompound;)V",
                    shift = At.Shift.AFTER))
    public void wheelchair$writeFullSize(ItemStack stack, CallbackInfo ci) {
        wheelchair$self().writeVarIntToBuffer(stack.stackSize);
    }

    @Inject(
            method = "readItemStackFromBuffer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/network/PacketBuffer;readNBTTagCompoundFromBuffer()Lnet/minecraft/nbt/NBTTagCompound;",
                    shift = At.Shift.AFTER),
            locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    public void wheelchair$readFullSize(CallbackInfoReturnable<ItemStack> cir, ItemStack stack) {
        stack.stackSize = wheelchair$self().readVarIntFromBuffer();
    }
}
