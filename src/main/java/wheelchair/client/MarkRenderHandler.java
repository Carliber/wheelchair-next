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
        double pt = e.partialTicks;
        double px = mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * pt;
        double py = mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * pt;
        double pz = mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * pt;
        RenderMarkWorld.draw(px, py, pz, mc.theWorld.getTotalWorldTime());
    }
}
