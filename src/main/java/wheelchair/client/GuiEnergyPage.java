package wheelchair.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Mouse;
import wheelchair.WheelchairMod;
import wheelchair.inventory.ContainerEnergy;
import wheelchair.network.EnergyMirror;
import wheelchair.network.PacketEnergyAction;
import wheelchair.network.PacketOpenTerminal;

/**
 * Energy page (guiId 6) — 1:1 from prototype/terminal.html (?cur=energy v6).
 *
 * 坐标系: 游戏窗 214 = rail(3..31) + sep(31..35) + 面板(35..211); 原型面板坐标
 * 8..168 换算 = 窗 CX..CR (43..203)。y 无偏移 (rail 只水平外扩)。
 * 原型规格 (CSS/3): 行卡 22 高 / pitch 24 / 5 行可视 / 两行 8 高字 (top 1/11)
 * / 色块 8x8 垂直居中 / X 12x12 贴内容右 6 / 滚动条贴内容右框 (8 宽, knob 4x24)。
 *
 * Read-only list + [X]; direction toggles ONLY via staff Shift+right-click.
 */
@SideOnly(Side.CLIENT)
public class GuiEnergyPage extends GuiContainer {
    // palette (shared law with GuiStoragePage/GuiEnchPage/GuiStaffPage)
    private static final int COL_BG = 0xffc6c6c6;
    private static final int COL_BEV_HI = 0xffffffff, COL_BEV_LO = 0xff555555;
    private static final int COL_SLOT = 0xff8b8b8b, COL_SLOT_DK = 0xff373737;
    private static final int COL_SEP = 0xff999999;
    private static final int COL_TEXT = 0xff404040, COL_SUB = 0xff707070, COL_GREY = 0xff909090;
    private static final int COL_GREEN = 0xff7fff7f, COL_RED = 0xffff5555, COL_XP = 0xff80ff20;
    private static final int COL_STORE_BLK = 0xc040ff40, COL_TAKE_BLK = 0xc0ff4040;
    private static final int BTN_OUTLINE = 0xff030303, BTN_TOP_HI = 0xffaaaaaa;
    private static final int BTN_G1 = 0xff6b6b6b, BTN_G2 = 0xff707070, BTN_G3 = 0xff565656;
    private static final int BTN_H1 = 0xffbec7ff, BTN_H2 = 0xff808ac1, BTN_H3 = 0xff5c669d;
    private static final int COL_CARD = 0x24000000, COL_CARD_EDGE = 0xff9a9a9a;
    private static final int COL_HOVER_RAIL = 0xff9c9c9c;

    // frame law (same as every page)
    private static final int FRAME = 3, RAIL_X = 3, SEP_X = 31, SEP_W = 4, PANEL_X = 35;
    // content band, WINDOW coords: 原型面板 8..168 → 43..203
    private static final int CX = 43, CR = 203, CW = CR - CX;

    // vertical layout (window y — no rail offset)
    private static final int TITLE_Y = 9;          // 电量行
    private static final int LBL_Y = 24;           // 标记机器 (n)
    private static final int LIST_Y = 34;          // first row top
    private static final int ROW_H = 24;           // pitch (原型 72/3)
    private static final int CARD_H = 22;          // 行卡高 (原型 66/3)
    private static final int ROWS_VIS = 5;         // 5×24=120 → 34..154
    private static final int SB_W = 8, KNOB_W = 4, KNOB_H = 24;
    private static final int SB_H = ROWS_VIS * ROW_H - 4; // 116

    private static final int X_W = 12, X_H = 12, X_X = CR - 12 - 6; // 内容右缘内收 6 (原型 right:6px/3)

    private static final int INV_X = 43;
    private static final int INV_LINE_Y = 164, INV_Y = 169, HOT_Y = 227;

    private static final String[] RAIL_TABS = {"存储", "附魔", "能源"};
    private static final String[] RAIL_PIN = {"手杖", "设置"};

    private int scroll;

    public GuiEnergyPage(ContainerEnergy c) {
        super(c);
        xSize = 214;
        ySize = 251;
    }

    // ---------------- background ----------------

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        int x = (width - xSize) / 2, y = (height - ySize) / 2;

        // window + rail + separator — storage-page law (BG fill + FRAME bevel, no black outline)
        drawRect(x, y, x + xSize, y + ySize, COL_BG);
        bevel(x, y, xSize, ySize, COL_BEV_HI, COL_BEV_LO, FRAME);
        drawRect(x + RAIL_X, y + FRAME, x + SEP_X, y + ySize - FRAME, COL_SLOT);
        drawRect(x + SEP_X, y + FRAME, x + SEP_X + SEP_W, y + ySize - FRAME, COL_SEP);

        // header: 电量: <green pool> RF
        fontRendererObj.drawString("电量:", x + CX, y + TITLE_Y, COL_TEXT);
        String pv = fmt(EnergyMirror.pool) + " RF";
        fontRendererObj.drawString(pv, x + CX + fontRendererObj.getStringWidth("电量:") + 2, y + TITLE_Y, COL_XP);

        fontRendererObj.drawString("标记机器 (" + EnergyMirror.rows.size() + ")", x + CX, y + LBL_Y, COL_TEXT);

        // rows
        for (int i = 0; i < ROWS_VIS; i++) {
            int idx = scroll + i;
            if (idx >= EnergyMirror.rows.size()) break;
            Object[] r = EnergyMirror.rows.get(idx);
            int ry = y + LIST_Y + i * ROW_H;
            drawRow(x, ry, r, mx, my);
        }

        // scrollbar (storage-page widget law), flush to the content right edge
        int maxScroll = EnergyMirror.rows.size() - ROWS_VIS;
        if (maxScroll > 0) {
            int sx = x + CR, sy = y + LIST_Y;
            drawRect(sx, sy, sx + SB_W, sy + SB_H, COL_SLOT);
            drawRect(sx, sy, sx + SB_W - 1, sy + SB_H - 1, COL_SLOT_DK);
            drawRect(sx, sy + SB_H - 1, sx + SB_W, sy + SB_H, COL_BEV_HI);
            drawRect(sx + SB_W - 1, sy, sx + SB_W, sy + SB_H, COL_BEV_HI);
            int ky = sy + 2 + (SB_H - 4 - KNOB_H) * scroll / maxScroll;
            drawRect(sx + 2, ky, sx + 2 + KNOB_W, ky + KNOB_H, COL_BG);
            drawRect(sx + 2, ky, sx + 2 + KNOB_W, ky + 1, COL_BEV_HI);
            drawRect(sx + 2, ky + KNOB_H - 1, sx + 2 + KNOB_W, ky + KNOB_H, COL_BEV_LO);
        }

        // inventory band (storage-page law: panel-wide rule + 3x9 + hotbar)
        drawRect(x + PANEL_X, y + INV_LINE_Y, x + xSize - FRAME, y + INV_LINE_Y + 2, COL_SEP);
        for (int rr = 0; rr < 3; rr++)
            for (int c = 0; c < 9; c++) slotRect(x + INV_X + c * 18, y + INV_Y + rr * 18);
        for (int c = 0; c < 9; c++) slotRect(x + INV_X + c * 18, y + HOT_Y);

        // rail (energy tab active)
        for (int i = 0; i < RAIL_TABS.length; i++)
            drawRailTab(i, x + RAIL_X + 1, y + 6 + i * 17, mx, my, i == 2);
        int py = ySize - 36;
        for (int i = 0; i < RAIL_PIN.length; i++)
            drawRailTabPin(i, x + RAIL_X + 1, y + py + i * 17, mx, my);
    }

    /** one mark row: full-width card + direction chip + two 8-high text lines + [X] */
    private void drawRow(int x, int ry, Object[] r, int mx, int my) {
        boolean store = ((Boolean) r[4]).booleanValue();
        // card: 1-GUI #9a9a9a frame + 14% black fill, full content width
        drawRect(x + CX, ry, x + CR, ry + CARD_H, COL_CARD_EDGE);
        drawRect(x + CX + 1, ry + 1, x + CR - 1, ry + CARD_H - 1, COL_CARD);
        // direction block 8x8, vertically centred (原型 left:6px → 2 GUI)
        int by = ry + (CARD_H - 8) / 2;
        drawRect(x + CX + 2, by, x + CX + 2 + 8, by + 8, BTN_OUTLINE);
        drawRect(x + CX + 3, by + 1, x + CX + 2 + 7, by + 7, store ? COL_STORE_BLK : COL_TAKE_BLK);
        // line 1: name + grey protocol badge (原型 left:39px → 13 GUI)
        String name = StatCollector.translateToLocal((String) r[7]);
        boolean eu = ((Boolean) r[5]).booleanValue();
        fontRendererObj.drawString(name, x + CX + 13, ry + 1, COL_TEXT);
        fontRendererObj.drawString(eu ? "[EU]" : "[RF]",
                x + CX + 13 + fontRendererObj.getStringWidth(name) + 1, ry + 1, COL_GREY);
        // line 2: dim (coords) · rate/t (red), same 8-high font
        String loc = dimName(((Integer) r[0]).intValue()) + " (" + r[1] + "," + r[2] + "," + r[3] + ")";
        String rate = ((Integer) r[6]).intValue() + "/t";
        fontRendererObj.drawString(loc, x + CX + 13, ry + 11, COL_SUB);
        String mid = " · ";
        fontRendererObj.drawString(mid, x + CX + 13 + fontRendererObj.getStringWidth(loc) + 1, ry + 11, COL_SUB);
        int rx = x + CX + 13 + fontRendererObj.getStringWidth(loc) + fontRendererObj.getStringWidth(mid) + 2;
        fontRendererObj.drawString(rate, rx, ry + 11, COL_RED);
        // [X], vertically centred
        int xy = ry + (CARD_H - X_H) / 2;
        boolean hov = inRect(mx, my, x + X_X, xy, X_W, X_H);
        drawBtn(x + X_X, xy, X_W, X_H, hov);
        fontRendererObj.drawString("X", x + X_X + 3, xy + 2, hov ? 0xffffff00 : COL_RED);
    }

    // ---------------- foreground (tooltips) ----------------

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        for (int i = 0; i < ROWS_VIS; i++) {
            int idx = scroll + i;
            if (idx >= EnergyMirror.rows.size()) break;
            Object[] r = EnergyMirror.rows.get(idx);
            int ry = LIST_Y + i * ROW_H;
            int by = ry + (CARD_H - 8) / 2;
            if (inRect(mx, my, guiLeft + CX + 2, guiTop + by, 8, 8)) {
                boolean store = ((Boolean) r[4]).booleanValue();
                drawHoveringText(java.util.Collections.singletonList(
                        store ? "存 (机器 → 存储池)" : "取 (存储池 → 机器)"), mx - guiLeft, my - guiTop, fontRendererObj);
            }
        }
    }

    // ---------------- widgets ----------------

    private void slotRect(int x, int y) {
        // vanilla slot well: 18x18 around the 16x16 anchor, bevel outward
        drawRect(x - 1, y - 1, x + 17, y + 17, COL_SLOT);
        drawRect(x - 1, y - 1, x + 17, y, COL_SLOT_DK);
        drawRect(x - 1, y - 1, x, y + 17, COL_SLOT_DK);
        drawRect(x - 1, y + 16, x + 17, y + 17, COL_BEV_HI);
        drawRect(x + 16, y - 1, x + 17, y + 17, COL_BEV_HI);
    }

    /** storage-page drawButton law: 3-band grey body + 1px black outline + top hi */
    private void drawBtn(int bx, int by, int w, int h, boolean hover) {
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
    }

    private void drawRailTab(int i, int tx, int ty, int mx, int my, boolean active) {
        railTabBody(RAIL_TABS[i], tx, ty, active, mx, my);
    }

    private void drawRailTabPin(int i, int tx, int ty, int mx, int my) {
        railTabBody(RAIL_PIN[i], tx, ty, false, mx, my);
    }

    /** storage-page bevel: four edges, hi top/left + lo bottom/right */
    private void bevel(int x, int y, int w, int h, int hi, int lo, int t) {
        drawRect(x, y, x + w, y + t, hi);
        drawRect(x, y, x + t, y + h, hi);
        drawRect(x, y + h - t, x + w, y + h, lo);
        drawRect(x + w - t, y, x + w, y + h, lo);
    }

    /** storage-page law: ONE horizontal label with shadow; fill + 4-edge bevel(2), active = green */
    private void railTabBody(String label, int tx, int ty, boolean on, int mx, int my) {
        int col = on ? 0xff5a7d5a : (inRect(mx, my, tx, ty, 26, 15) ? COL_HOVER_RAIL : COL_SLOT);
        drawRect(tx, ty, tx + 26, ty + 15, col);
        if (on) bevel(tx, ty, 26, 15, 0xffa8d0a8, 0xff2e4a2e, 2);
        else bevel(tx, ty, 26, 15, COL_BEV_HI, COL_BEV_LO, 2);
        int w = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawStringWithShadow(label, tx + (26 - w) / 2, ty + 4, on ? 0xeaffea : 0xe0e0e0);
    }

    // ---------------- input ----------------

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) {
            int max = Math.max(0, EnergyMirror.rows.size() - ROWS_VIS);
            scroll = Math.max(0, Math.min(max, scroll - Integer.signum(d)));
        }
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        int x = (width - xSize) / 2, y = (height - ySize) / 2;
        // rail navigation (top tabs: 0 storage 1 ench 2 energy=self)
        for (int i = 0; i < RAIL_TABS.length; i++) {
            if (i != 2 && inRect(mx, my, x + RAIL_X + 1, y + 6 + i * 17, 26, 15)) {
                net.minecraft.client.gui.ScaledResolution sr =
                        new net.minecraft.client.gui.ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
                WheelchairMod.network.sendToServer(new PacketOpenTerminal(i == 0 ? 0 : 4,
                        GuiStoragePage.rowsFor(sr.getScaledHeight())));
                return;
            }
        }
        int py = ySize - 36;
        for (int i = 0; i < RAIL_PIN.length; i++) {
            if (inRect(mx, my, x + RAIL_X + 1, y + py + i * 17, 26, 15)) {
                net.minecraft.client.gui.ScaledResolution sr =
                        new net.minecraft.client.gui.ScaledResolution(this.mc, this.mc.displayWidth, this.mc.displayHeight);
                WheelchairMod.network.sendToServer(new PacketOpenTerminal(5,
                        GuiStoragePage.rowsFor(sr.getScaledHeight())));
                return;
            }
        }
        // [X] unmark
        for (int i = 0; i < ROWS_VIS; i++) {
            int idx = scroll + i;
            if (idx >= EnergyMirror.rows.size()) break;
            Object[] r = EnergyMirror.rows.get(idx);
            int ry = y + LIST_Y + i * ROW_H;
            int xy = ry + (CARD_H - X_H) / 2;
            if (inRect(mx, my, x + X_X, xy, X_W, X_H)) {
                WheelchairMod.network.sendToServer(new PacketEnergyAction(
                        ((Integer) r[0]).intValue(), ((Integer) r[1]).intValue(),
                        ((Integer) r[2]).intValue(), ((Integer) r[3]).intValue()));
                return;
            }
        }
    }

    // ---------------- helpers ----------------

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    static String dimName(int dim) {
        if (dim == 0) return "主世界";
        if (dim == -1) return "下界";
        if (dim == 1) return "末地";
        return "维度" + dim;
    }

    static String fmt(long v) {
        if (v >= 1000000000L) return trim(v / 1000000000.0D) + "G";
        if (v >= 1000000L) return trim(v / 1000000.0D) + "M";
        if (v >= 1000L) return trim(v / 1000.0D) + "K";
        return String.valueOf(v);
    }

    private static String trim(double d) {
        String s = String.format("%.1f", Double.valueOf(d));
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
}
