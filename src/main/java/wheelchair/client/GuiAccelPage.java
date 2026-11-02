package wheelchair.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Mouse;
import wheelchair.WheelchairMod;
import wheelchair.inventory.ContainerAccel;
import wheelchair.network.AccelMirror;
import wheelchair.network.PacketAccelAction;
import wheelchair.network.PacketOpenTerminal;

/**
 * Accel page (guiId 7) — 1:1 from prototype/terminal.html (?cur=accel v5).
 *
 * 布局照能源页律: 行卡 22 高 / pitch 24 / 5 行可视 / 两行 8 高字 / 色块 8x8
 * / 档位 chip 左键加右键减 / X 12x12 / 滚动条 8 宽贴内容右框。
 * 行主行 = 方块(维度+坐标) / 生物(名字); 副行 = 剩余时间 · 负载N/t。
 */
@SideOnly(Side.CLIENT)
public class GuiAccelPage extends GuiContainer {
    private static final int COL_BG = 0xffc6c6c6;
    private static final int COL_BEV_HI = 0xffffffff, COL_BEV_LO = 0xff555555;
    private static final int COL_SLOT = 0xff8b8b8b, COL_SLOT_DK = 0xff373737;
    private static final int COL_SEP = 0xff999999;
    private static final int COL_TEXT = 0xff404040, COL_SUB = 0xff707070;
    private static final int COL_BLUE = 0xff7fc8ff, COL_YELLOW = 0xffffff55;
    private static final int COL_CARD = 0x24000000, COL_CARD_EDGE = 0xff9a9a9a;
    private static final int COL_HOVER_RAIL = 0xff9c9c9c;
    private static final int COL_BLK_BLOCK = 0xc0409cff, COL_BLK_ENT = 0xc0ffff55;
    private static final int BTN_OUTLINE = 0xff030303, BTN_TOP_HI = 0xffaaaaaa;
    private static final int BTN_G1 = 0xff6b6b6b, BTN_G2 = 0xff707070, BTN_G3 = 0xff565656;
    private static final int BTN_H1 = 0xffbec7ff, BTN_H2 = 0xff808ac1, BTN_H3 = 0xff5c669d;

    private static final int FRAME = 3, RAIL_X = 3, SEP_X = 31, SEP_W = 4, PANEL_X = 35;
    private static final int CX = 43, CR = 203, CW = CR - CX;

    private static final int TITLE_Y = 9;
    private static final int LBL_Y = 24;
    private static final int LIST_Y = 34;
    private static final int ROW_H = 24, CARD_H = 22;
    private static final int SB_W = 8, KNOB_W = 4, KNOB_H = 24;

    private static final int X_W = 12, X_H = 12;
    private static final int CHIP_H = 12;

    private static final int INV_X = 43;

    private static final String[] RAIL_TABS = {"存储", "附魔", "能源", "加速"};
    private static final String[] RAIL_PIN = {"手杖", "设置"};

    private static final int[] R_STEPS = wheelchair.value.AccelHub.RADIUS_STEPS;
    private static final int[] M_STEPS = wheelchair.value.AccelHub.RATE_STEPS;

    private int scroll;
    /** chip rects of the visible rows, rebuilt every frame (index = row*3+{0=r,1=m,2=x}) */
    private int[][] chips;
    private final int rowsVis;
    private final int invLineY, invY, hotY;

    public GuiAccelPage(ContainerAccel c, int rows) {
        super(c);
        if (rows < 3) rows = 3;
        if (rows > 10) rows = 10;
        xSize = 214;
        ySize = 251 + (rows - 3) * 18;   // storage-page law: every extra row +18
        rowsVis = 5 + (rows - 3);
        hotY = ySize - 24;
        invY = ySize - 82;
        invLineY = ySize - 87;
        chips = new int[rowsVis * 3][];
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        int x = (width - xSize) / 2, y = (height - ySize) / 2;

        drawRect(x, y, x + xSize, y + ySize, COL_BG);
        bevel(x, y, xSize, ySize, COL_BEV_HI, COL_BEV_LO, FRAME);
        drawRect(x + RAIL_X, y + FRAME, x + SEP_X, y + ySize - FRAME, COL_SLOT);
        drawRect(x + SEP_X, y + FRAME, x + SEP_X + SEP_W, y + ySize - FRAME, COL_SEP);

        // header left: 全局 [开/关] chip; right: 额外负载 ~N 次/t
        String gs = AccelMirror.on ? "开" : "关";
        int gsw = fontRendererObj.getStringWidth(gs) + 8;
        drawBtn(x + CX + fontRendererObj.getStringWidth("全局") + 4, y + TITLE_Y - 1, gsw, CHIP_H, gs,
                inRect(mx, my, x + CX + fontRendererObj.getStringWidth("全局") + 4, y + TITLE_Y - 1, gsw, CHIP_H));
        fontRendererObj.drawString("全局", x + CX, y + TITLE_Y, COL_TEXT);
        if (AccelMirror.on) drawRect(x + CX + fontRendererObj.getStringWidth("全局") + 6, y + TITLE_Y + 2,
                x + CX + fontRendererObj.getStringWidth("全局") + 8, y + TITLE_Y + 10, 0xff7fff7f);
        long load = 0;
        for (int i = 0; i < AccelMirror.rows.size(); i++) {
            Object[] r = AccelMirror.rows.get(i);
            load += ((Integer) r[9]).intValue() * (((Integer) r[7]).intValue() - 1);
        }
        String ld = "额外负载 ~" + load + " 次/t";
        fontRendererObj.drawString(ld, x + CR - fontRendererObj.getStringWidth(ld), y + TITLE_Y, COL_TEXT);

        fontRendererObj.drawString("加速标记 (" + AccelMirror.rows.size() + ")", x + CX, y + LBL_Y, COL_TEXT);

        for (int i = 0; i < rowsVis; i++) {
            int idx = scroll + i;
            if (idx >= AccelMirror.rows.size()) { chips[i * 3] = chips[i * 3 + 1] = chips[i * 3 + 2] = null; continue; }
            Object[] r = AccelMirror.rows.get(idx);
            drawRow(x, y + LIST_Y + i * ROW_H, r, idx, mx, my);
        }

        int sbH = rowsVis * ROW_H - 4;
        int maxScroll = AccelMirror.rows.size() - rowsVis;
        if (maxScroll > 0) {
            int sx = x + CR, sy = y + LIST_Y;
            drawRect(sx, sy, sx + SB_W, sy + sbH, COL_SLOT);
            drawRect(sx, sy, sx + SB_W - 1, sy + sbH - 1, COL_SLOT_DK);
            drawRect(sx, sy + sbH - 1, sx + SB_W, sy + sbH, COL_BEV_HI);
            drawRect(sx + SB_W - 1, sy, sx + SB_W, sy + sbH, COL_BEV_HI);
            int ky = sy + 2 + (sbH - 4 - KNOB_H) * scroll / maxScroll;
            drawRect(sx + 2, ky, sx + 2 + KNOB_W, ky + KNOB_H, COL_BG);
            drawRect(sx + 2, ky, sx + 2 + KNOB_W, ky + 1, COL_BEV_HI);
            drawRect(sx + 2, ky + KNOB_H - 1, sx + 2 + KNOB_W, ky + KNOB_H, COL_BEV_LO);
        }

        drawRect(x + PANEL_X, y + invLineY, x + xSize - FRAME, y + invLineY + 2, COL_SEP);
        for (int rr = 0; rr < 3; rr++)
            for (int c = 0; c < 9; c++) slotRect(x + INV_X + c * 18, y + invY + rr * 18);
        for (int c = 0; c < 9; c++) slotRect(x + INV_X + c * 18, y + hotY);

        for (int i = 0; i < RAIL_TABS.length; i++)
            drawRailTab(i, x + RAIL_X + 1, y + 6 + i * 17, mx, my, i == 3);
        int py = ySize - 36;
        for (int i = 0; i < RAIL_PIN.length; i++)
            drawRailTabPin(i, x + RAIL_X + 1, y + py + i * 17, mx, my);
    }

    /** one accel mark row (原型 v5: 主行=坐标/生物名, 副行=剩余·负载, chips=半径/倍率/X) */
    private void drawRow(int x, int ry, Object[] r, int idx, int mx, int my) {
        boolean isEnt = ((Boolean) r[0]).booleanValue();
        drawRect(x + CX, ry, x + CR, ry + CARD_H, COL_CARD_EDGE);
        drawRect(x + CX + 1, ry + 1, x + CR - 1, ry + CARD_H - 1, COL_CARD);
        int by = ry + (CARD_H - 8) / 2;
        drawRect(x + CX + 2, by, x + CX + 2 + 8, by + 8, BTN_OUTLINE);
        drawRect(x + CX + 3, by + 1, x + CX + 2 + 7, by + 7, isEnt ? COL_BLK_ENT : COL_BLK_BLOCK);

        String main;
        if (isEnt) {
            main = String.valueOf(r[10]);
        } else {
            main = GuiEnergyPage.dimName(((Integer) r[1]).intValue())
                    + " (" + r[2] + "," + r[3] + "," + r[4] + ")";
        }
        fontRendererObj.drawString(main, x + CX + 13, ry + 1, COL_TEXT);

        long remain = ((Long) r[8]).longValue();
        int rate = ((Integer) r[7]).intValue();
        int cov = ((Integer) r[9]).intValue();
        String sub = (remain < 0 ? "永久" : "剩余" + (remain / 20) + "s")
                + " · 负载" + (cov * (rate - 1)) + "/t";
        fontRendererObj.drawString(sub, x + CX + 13, ry + 11, COL_SUB);

        // right control group: [r格] [×m] [X] — built right-to-left from CR-6
        int cy = ry + (CARD_H - CHIP_H) / 2;
        int cx0 = x + CR - X_W - 6;
        int idxBase = (idx - scroll) * 3;
        chips[idxBase + 2] = new int[]{cx0, cy, X_W, X_H};
        drawBtn(cx0, cy, X_W, X_H, "X", inRect(mx, my, cx0, cy, X_W, X_H));

        String ms = "×" + rate;
        int mw = fontRendererObj.getStringWidth(ms) + 8;
        int mx0 = cx0 - mw - 2;
        chips[idxBase + 1] = new int[]{mx0, cy, mw, CHIP_H};
        drawBtn(mx0, cy, mw, CHIP_H, ms, inRect(mx, my, mx0, cy, mw, CHIP_H));

        if (!isEnt) {
            int radius = ((Integer) r[6]).intValue();
            String rs = radius + "格";
            int rw = fontRendererObj.getStringWidth(rs) + 8;
            int rx0 = mx0 - rw - 2;
            chips[idxBase] = new int[]{rx0, cy, rw, CHIP_H};
            drawBtn(rx0, cy, rw, CHIP_H, rs, inRect(mx, my, rx0, cy, rw, CHIP_H));
        } else {
            chips[idxBase] = null;
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        for (int i = 0; i < rowsVis; i++) {
            int idx = scroll + i;
            if (idx >= AccelMirror.rows.size()) break;
            Object[] r = AccelMirror.rows.get(idx);
            int ry = LIST_Y + i * ROW_H;
            int by = ry + (CARD_H - 8) / 2;
            if (inRect(mx, my, guiLeft + CX + 2, guiTop + by, 8, 8)) {
                boolean isEnt = ((Boolean) r[0]).booleanValue();
                drawHoveringText(java.util.Collections.singletonList(isEnt
                        ? "生物加速 (单体 · 计时器推进)"
                        : "区域加速 (机器附加tick · 生物计时器)"), mx - guiLeft, my - guiTop, fontRendererObj);
            }
        }
    }

    private void slotRect(int x, int y) {
        drawRect(x - 1, y - 1, x + 17, y + 17, COL_SLOT);
        drawRect(x - 1, y - 1, x + 17, y, COL_SLOT_DK);
        drawRect(x - 1, y - 1, x, y + 17, COL_SLOT_DK);
        drawRect(x - 1, y + 16, x + 17, y + 17, COL_BEV_HI);
        drawRect(x + 16, y - 1, x + 17, y + 17, COL_BEV_HI);
    }

    private void drawBtn(int bx, int by, int w, int h, String label, boolean hover) {
        int c1 = hover ? BTN_H1 : BTN_G1;
        int c2 = hover ? BTN_H2 : BTN_G2;
        int c3 = hover ? BTN_H3 : BTN_G3;
        int third = h / 3;
        if (third < 1) third = 1;
        drawRect(bx, by, bx + w, by + third, c1);
        drawRect(bx, by + third, bx + w, by + h - third, c2);
        drawRect(bx, by + h - third, bx + w, by + h, c3);
        drawRect(bx, by, bx + w, by + 1, BTN_OUTLINE);
        drawRect(bx, by + h - 1, bx + w, by + h, BTN_OUTLINE);
        drawRect(bx, by, bx + 1, by + h, BTN_OUTLINE);
        drawRect(bx + w - 1, by, bx + w, by + h, BTN_OUTLINE);
        drawRect(bx + 1, by + 1, bx + w - 1, by + 2, BTN_TOP_HI);
        int tw = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawString(label, bx + (w - tw) / 2, by + (h - 8) / 2, hover ? 0xffffffa0 : 0xffe0e0e0);
    }

    private void drawRailTab(int i, int tx, int ty, int mx, int my, boolean active) {
        railTabBody(RAIL_TABS[i], tx, ty, active, mx, my);
    }

    private void drawRailTabPin(int i, int tx, int ty, int mx, int my) {
        railTabBody(RAIL_PIN[i], tx, ty, false, mx, my);
    }

    private void railTabBody(String label, int tx, int ty, boolean on, int mx, int my) {
        int col = on ? 0xff5a7d5a : (inRect(mx, my, tx, ty, 26, 15) ? COL_HOVER_RAIL : COL_SLOT);
        drawRect(tx, ty, tx + 26, ty + 15, col);
        if (on) bevel(tx, ty, 26, 15, 0xffa8d0a8, 0xff2e4a2e, 2);
        else bevel(tx, ty, 26, 15, COL_BEV_HI, COL_BEV_LO, 2);
        int w = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawStringWithShadow(label, tx + (26 - w) / 2, ty + 4, on ? 0xeaffea : 0xe0e0e0);
    }

    private void bevel(int x, int y, int w, int h, int hi, int lo, int t) {
        drawRect(x, y, x + w, y + t, hi);
        drawRect(x, y, x + t, y + h, hi);
        drawRect(x, y + h - t, x + w, y + h, lo);
        drawRect(x + w - t, y, x + w, y + h, lo);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) {
            int max = Math.max(0, AccelMirror.rows.size() - rowsVis);
            scroll = Math.max(0, Math.min(max, scroll - Integer.signum(d)));
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        int x = (width - xSize) / 2, y = (height - ySize) / 2;
        // rail navigation (top tabs: 0 storage 1 ench 2 energy 3 accel=self)
        for (int i = 0; i < RAIL_TABS.length; i++) {
            if (i != 3 && inRect(mx, my, x + RAIL_X + 1, y + 6 + i * 17, 26, 15)) {
                net.minecraft.client.gui.ScaledResolution sr =
                        new net.minecraft.client.gui.ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
                int rows = GuiStoragePage.rowsFor(sr.getScaledHeight());
                if (i == 0) WheelchairMod.network.sendToServer(new PacketOpenTerminal(0, rows));
                else if (i == 1) WheelchairMod.network.sendToServer(new PacketOpenTerminal(4, 3));
                else WheelchairMod.network.sendToServer(new PacketOpenTerminal(6, rows));
                return;
            }
        }
        int py = ySize - 36;
        for (int i = 0; i < RAIL_PIN.length; i++) {
            if (inRect(mx, my, x + RAIL_X + 1, y + py + i * 17, 26, 15)) {
                net.minecraft.client.gui.ScaledResolution sr =
                        new net.minecraft.client.gui.ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
                WheelchairMod.network.sendToServer(new PacketOpenTerminal(5, GuiStoragePage.rowsFor(sr.getScaledHeight())));
                return;
            }
        }
        // master switch chip
        int gx = x + CX + fontRendererObj.getStringWidth("全局") + 4;
        if (button == 0 && inRect(mx, my, gx, y + TITLE_Y - 1,
                fontRendererObj.getStringWidth(AccelMirror.on ? "开" : "关") + 8, CHIP_H)) {
            WheelchairMod.network.sendToServer(new PacketAccelAction(5, false, 0, 0, 0, 0, 0));
            return;
        }
        // row chips: left = step up, right = step down; X = unmark
        if (button == 0 || button == 1) {
            for (int i = 0; i < rowsVis; i++) {
                int idx = scroll + i;
                if (idx >= AccelMirror.rows.size()) break;
                Object[] r = AccelMirror.rows.get(idx);
                boolean isEnt = ((Boolean) r[0]).booleanValue();
                int[] rc = chips[i * 3], mc = chips[i * 3 + 1], xc = chips[i * 3 + 2];
                if (xc != null && inRect(mx, my, xc[0], xc[1], xc[2], xc[3]) && button == 0) {
                    sendMarkAction(0, r, isEnt);
                    return;
                }
                int dir = button == 0 ? 1 : -1;
                if (mc != null && inRect(mx, my, mc[0], mc[1], mc[2], mc[3])) {
                    sendMarkAction(dir > 0 ? 3 : 4, r, isEnt);
                    return;
                }
                if (rc != null && inRect(mx, my, rc[0], rc[1], rc[2], rc[3])) {
                    sendMarkAction(dir > 0 ? 1 : 2, r, isEnt);
                    return;
                }
            }
        }
    }

    private void sendMarkAction(int op, Object[] r, boolean isEnt) {
        WheelchairMod.network.sendToServer(new PacketAccelAction(op, isEnt,
                ((Integer) r[1]).intValue(), ((Integer) r[2]).intValue(),
                ((Integer) r[3]).intValue(), ((Integer) r[4]).intValue(),
                ((Integer) r[5]).intValue()));
    }

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
