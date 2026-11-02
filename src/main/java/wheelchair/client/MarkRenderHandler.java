package wheelchair.client;

import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;

/**
 * RenderWorldLastEvent bridge → RenderMarkWorld (mark highlight overlay).
 * Registered on the Forge client event bus from ClientProxy.
 */
@SideOnly(Side.CLIENT)
public final class MarkRenderHandler {

    private MarkRenderHandler() {}

    public static void install() {
        MinecraftForge.EVENT_BUS.register(new MarkRenderHandler());
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.theWorld == null) return;
        // mark overlay while holding the staff: 电力 mode shows energy marks,
        // 加速 mode shows accel marks (block boxes)
        net.minecraft.item.ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || held.getItem() != wheelchair.item.ItemStaff.STAFF
                || wheelchair.value.StaffProfile.cTopMode < 2) return;
        double pt = e.partialTicks;
        double px = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * pt;
        double py = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * pt;
        double pz = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * pt;
        if (wheelchair.value.StaffProfile.cTopMode == 2) {
            RenderMarkWorld.draw(px, py, pz, mc.theWorld.getTotalWorldTime());
        } else if (wheelchair.value.StaffProfile.cTopMode == 4) {
            RenderMarkWorld.drawAccel(px, py, pz);
        }
    }
}
