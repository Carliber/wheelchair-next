package wheelchair.client;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;
import wheelchair.network.EnergyMirror;

/**
 * World-space mark highlight (user call 2026-10-25): the marked machine block
 * is drawn with a translucent overlay — 存 (store) = pulsing translucent green
 * ("glow": alpha breathes on a sine), 取 (take) = steady translucent red.
 * Read-only from the EnergyMirror; fires on RenderWorldLastEvent.
 */
public final class RenderMarkWorld {

    private RenderMarkWorld() {}

    public static void draw(double camX, double camY, double camZ, long tick) {
        if (EnergyMirror.rows.isEmpty()) return;
        GL11.glPushMatrix();
        GL11.glTranslated(-camX, -camY, -camZ);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDepthMask(false);
        float breathe = 0.10F + 0.08F * (float) Math.sin(tick * 0.12D); // glow pulse

        for (int i = 0; i < EnergyMirror.rows.size(); i++) {
            Object[] r = EnergyMirror.rows.get(i);
            // only render marks in the dimension the player is looking at —
            // the mirror is global, the camera is not
            if (((Integer) r[0]).intValue() != net.minecraft.client.Minecraft.getMinecraft().theWorld.provider.dimensionId)
                continue;
            boolean store = ((Boolean) r[4]).booleanValue();
            float cr = store ? 0.25F : 1.0F;
            float cg = store ? 1.0F : 0.25F;
            float cb = 0.25F;
            float alpha = store ? breathe : 0.22F;
            int x = ((Integer) r[1]).intValue(), y = ((Integer) r[2]).intValue(), z = ((Integer) r[3]).intValue();
            box(x, y, z, cr, cg, cb, alpha);
            wire(x, y, z, cr, cg, cb, store ? 0.9F : 0.8F);
        }

        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glPopMatrix();
    }

    private static void box(int x, int y, int z, float r, float g, float b, float a) {
        double e = 0.002D; // slight inflate to avoid z-fighting with the block
        double x0 = x - e, y0 = y - e, z0 = z - e, x1 = x + 1 + e, y1 = y + 1 + e, z1 = z + 1 + e;
        Tessellator t = Tessellator.instance;
        t.startDrawing(GL11.GL_QUADS);
        t.setColorRGBA_F(r, g, b, a);
        // bottom / top
        t.addVertex(x0, y0, z0); t.addVertex(x1, y0, z0); t.addVertex(x1, y0, z1); t.addVertex(x0, y0, z1);
        t.addVertex(x0, y1, z0); t.addVertex(x0, y1, z1); t.addVertex(x1, y1, z1); t.addVertex(x1, y1, z0);
        // north / south
        t.addVertex(x0, y0, z0); t.addVertex(x0, y1, z0); t.addVertex(x1, y1, z0); t.addVertex(x1, y0, z0);
        t.addVertex(x0, y0, z1); t.addVertex(x1, y0, z1); t.addVertex(x1, y1, z1); t.addVertex(x0, y1, z1);
        // west / east
        t.addVertex(x0, y0, z0); t.addVertex(x0, y0, z1); t.addVertex(x0, y1, z1); t.addVertex(x0, y1, z0);
        t.addVertex(x1, y0, z0); t.addVertex(x1, y1, z0); t.addVertex(x1, y1, z1); t.addVertex(x1, y0, z1);
        t.draw();
    }

    private static void wire(int x, int y, int z, float r, float g, float b, float a) {
        double e = 0.004D;
        double x0 = x - e, y0 = y - e, z0 = z - e, x1 = x + 1 + e, y1 = y + 1 + e, z1 = z + 1 + e;
        Tessellator t = Tessellator.instance;
        GL11.glLineWidth(2.0F);
        t.startDrawing(GL11.GL_LINES);
        t.setColorRGBA_F(r, g, b, a);
        // bottom rectangle
        t.addVertex(x0, y0, z0); t.addVertex(x1, y0, z0);
        t.addVertex(x1, y0, z0); t.addVertex(x1, y0, z1);
        t.addVertex(x1, y0, z1); t.addVertex(x0, y0, z1);
        t.addVertex(x0, y0, z1); t.addVertex(x0, y0, z0);
        // top rectangle
        t.addVertex(x0, y1, z0); t.addVertex(x1, y1, z0);
        t.addVertex(x1, y1, z0); t.addVertex(x1, y1, z1);
        t.addVertex(x1, y1, z1); t.addVertex(x0, y1, z1);
        t.addVertex(x0, y1, z1); t.addVertex(x0, y1, z0);
        // verticals
        t.addVertex(x0, y0, z0); t.addVertex(x0, y1, z0);
        t.addVertex(x1, y0, z0); t.addVertex(x1, y1, z0);
        t.addVertex(x1, y0, z1); t.addVertex(x1, y1, z1);
        t.addVertex(x0, y0, z1); t.addVertex(x0, y1, z1);
        t.draw();
    }
}
