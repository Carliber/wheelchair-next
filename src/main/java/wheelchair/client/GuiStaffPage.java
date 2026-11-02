package wheelchair.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;
import wheelchair.WheelchairMod;
import wheelchair.inventory.ContainerStaff;
import wheelchair.network.PacketOpenTerminal;
import wheelchair.network.PacketStaffCfg;
import wheelchair.value.StaffProfile;

/**
 * Staff page (guiId 5) — 1:1 from prototype/terminal.html (手杖页 v6):
 * two sections with a divider, title style distinct from body (colour + rule),
 * checkbox ON the 连锁 title, everything else inline.
 *
 *   基础
 *   [chk]精准  时运等级:[box] 0~255
 *   ────────────
 *   [chk]连锁
 *   范围[box] [形状:跟随] [模式:普通]
 *
 * Every change sends the WHOLE profile (PacketStaffCfg); the page itself starts
 * from StaffProfile's client mirror, pushed by PacketStaffCfgSync on open.
 */
@SideOnly(Side.CLIENT)
public class GuiStaffPage extends GuiContainer {
    // palette (shared with GuiStoragePage/GuiEnchPage)
    private static final int COL_BG = 0xffc6c6c6;
    private static final int COL_BEV_HI = 0xffffffff, COL_BEV_LO = 0xff555555;
    private static final int COL_SLOT = 0xff8b8b8b, COL_SLOT_DK = 0xff373737;
    private static final int COL_SEP = 0xff999999;
    private static final int COL_TEXT = 0xff404040, COL_TITLE = 0xff303030;
    private static final int COL_TITLE_RULE = 0xffb0b0b0;
    private static final int BTN_OUTLINE = 0xff030303, BTN_TOP_HI = 0xffaaaaaa;
    private static final int BTN_G1 = 0xff6b6b6b, BTN_G2 = 0xff707070, BTN_G3 = 0xff565656;
    private static final int BTN_H1 = 0xffa6a6a6, BTN_H2 = 0xff999999, BTN_H3 = 0xff7d7d7d;
    private static final int COL_HOVER_RAIL = 0xff9c9c9c;

    // layout (window px; panel content x8..168, window = panel + 35/+3)
    private static final int FRAME = 3;
    private static final int RAIL_X = 3, SEP_X = 31, SEP_W = 4, PANEL_X = 35;
    private static final int L = 43;                 // content left edge (panel x8)
    private static final int TITLE1_Y = 9;           // 基础
    private static final int ROW1_Y = 22;            // 精准 / 时运
    private static final int DIV_Y = 40;             // divider
    private static final int TITLE2_Y = 46;          // 连锁采掘 title
    private static final int ROW2_Y = 62;            // 范围 / 形状 / 挖法
    private static final int TITLE3_Y = 80;          // 加速 title
    private static final int ROW3_Y = 96;            // 加速时长行
    private static final int BOX_W = 26, BOX_H = 12;
    private static final int TBOX_W = 28;            // 加速时长框 (原型 84px/3)
    private static final int FORT_LBL_X = 78, RANGE_X = 66;
    private static final String FORT_LBL = "时运等级:";
    private static final String FORT_HINT = "0~255";
    private int fortX; // box x, = label x + MEASURED label width + 2 (no overlap)
    private static final int CHK1_X = L, CHK2_X = L;

    private static final String[] RAIL_TABS = {"存储", "附魔", "能源", "加速"};
    private static final String[] RAIL_PIN = {"手杖", "设置"};

    private GuiTextField fortBox, rangeBox, timeBox;
    private int focused = -1; // 0 fortune 1 range 2 time
    private int lastVer;      // last-seen profile push version

    public GuiStaffPage(ContainerStaff c, int rows) {
        super(c);
        if (rows < 3) rows = 3;
        if (rows > 10) rows = 10;
        // same adaptive window height law as storage/ench: every extra grid row
        // grows the window by 18 (content stays anchored to the top, rail
        // follows ySize via its ySize-36 pin block)
        xSize = 214;
        ySize = 254 + (rows - 3) * 18;
    }

    @Override
    public void initGui() {
        super.initGui();
        Keyboard.enableRepeatEvents(true);
        fortX = FORT_LBL_X + this.fontRendererObj.getStringWidth(FORT_LBL) + 2;
        fortBox = box(fortX, ROW1_Y - 1);
        rangeBox = box(RANGE_X, ROW2_Y - 1);
        timeBox = new GuiTextField(this.fontRendererObj, guiLeft + RANGE_X, guiTop + ROW3_Y - 1, TBOX_W, BOX_H);
        timeBox.setEnableBackgroundDrawing(false);
        timeBox.setTextColor(0xffff55);
        timeBox.setMaxStringLength(6);
        refreshBoxes();
    }

    private GuiTextField box(int x, int y) {
        GuiTextField b = new GuiTextField(this.fontRendererObj, guiLeft + x, guiTop + y, BOX_W, BOX_H);
        b.setEnableBackgroundDrawing(false); // self-drawn bed
        b.setTextColor(0xffff55);
        b.setMaxStringLength(3);
        return b;
    }

    @Override
    public void onGuiClosed() {
        // commit any typed-but-uncommitted value BEFORE closing (typing then
        // closing without Enter used to silently drop the change)
        if (focused >= 0 || (fortBox != null && rangeBox != null)) {
            send();
            if (fortBox != null) fortBox.setFocused(false);
            if (rangeBox != null) rangeBox.setFocused(false);
            if (timeBox != null) timeBox.setFocused(false);
            focused = -1;
        }
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    // ---------------- background ----------------

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        int x = guiLeft, y = guiTop;
        drawRect(x, y, x + xSize, y + ySize, COL_BG);
        bevel(x, y, xSize, ySize, COL_BEV_HI, COL_BEV_LO, FRAME);
        drawRect(x + RAIL_X, y + FRAME, x + SEP_X, y + ySize - FRAME, COL_SLOT);
        drawRect(x + SEP_X, y + FRAME, x + SEP_X + SEP_W, y + ySize - FRAME, COL_SEP);
        for (int i = 0; i < RAIL_TABS.length; i++) drawRailTab(i, x + RAIL_X + 1, y + 6 + i * 17, true, mx, my);
        int py = ySize - 36;
        for (int i = 0; i < RAIL_PIN.length; i++) drawRailTab(i, x + RAIL_X + 1, y + py + i * 17, false, mx, my);

        // 基础 title + rule
        fontRendererObj.drawString("基础", L, TITLE1_Y, COL_TITLE);
        drawRect(x + L, y + TITLE1_Y + 10, x + 43 + 160, y + TITLE1_Y + 12, COL_TITLE_RULE);

        // divider between sections
        drawRect(x + L, y + DIV_Y, x + 43 + 160, y + DIV_Y + 2, COL_SEP);

        // 连锁 title rule (under the checkbox+label row, drawn in FG)
    }

    // ---------------- foreground (panel coords) ----------------

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        boolean silk = StaffProfile.cSilk;

        // row 1: [chk]精准  时运等级:[box] 0~255  (box x measured in initGui)
        drawCheckBox(CHK1_X, ROW1_Y, silk);
        fontRendererObj.drawString("精准", CHK1_X + 11, ROW1_Y + 2, silk ? COL_TITLE : COL_TEXT);
        fontRendererObj.drawString(FORT_LBL, FORT_LBL_X, ROW1_Y + 2, silk ? 0xff999999 : COL_TEXT);
        fontRendererObj.drawString(FORT_HINT, fortX + BOX_W + 4, ROW1_Y + 2, silk ? 0xffaaaaaa : 0xff707070);

        // top-mode chip: 页面右上角 (原型 right 54px/3=18 from CR=203, top 9)
        String tm = "模式:" + StaffProfile.TOP_MODE_NAMES[Math.max(0, Math.min(4, StaffProfile.cTopMode))];
        int tmW = fontRendererObj.getStringWidth(tm) + 6;
        int tmX = 203 - 18 - tmW;
        drawButton(tmX, TITLE1_Y - 1, tmW, BOX_H, tm,
                inRect(mx, my, guiLeft + tmX, guiTop + TITLE1_Y - 1, tmW, BOX_H));

        // 连锁采掘 title (原型: 无框节标题+下划线)
        fontRendererObj.drawString("连锁采掘", L, TITLE2_Y, COL_TITLE);
        drawRect(L, TITLE2_Y + 11, L + 160, TITLE2_Y + 13, COL_TITLE_RULE);

        // row 2: 范围[box] [形状:..] [挖法:..]
        fontRendererObj.drawString("范围", L, ROW2_Y + 2, COL_TEXT);
        String shape = "形状:" + StaffProfile.SHAPE_NAMES[StaffProfile.cShape];
        int shapeW = fontRendererObj.getStringWidth(shape) + 6;
        int shapeX = RANGE_X + BOX_W + 6;
        drawButton(shapeX, ROW2_Y, shapeW, BOX_H, shape,
                inRect(mx, my, guiLeft + shapeX, guiTop + ROW2_Y, shapeW, BOX_H));
        String mode = "挖法:" + StaffProfile.MODE_NAMES[StaffProfile.cMode];
        int modeW = fontRendererObj.getStringWidth(mode) + 6;
        int modeX = shapeX + shapeW + 4;
        drawButton(modeX, ROW2_Y, modeW, BOX_H, mode,
                inRect(mx, my, guiLeft + modeX, guiTop + ROW2_Y, modeW, BOX_H));

        // 加速 section (原型 v27: 节标题 + 时长框, 无提示文字)
        fontRendererObj.drawString("加速", L, TITLE3_Y, COL_TITLE);
        drawRect(L, TITLE3_Y + 11, L + 160, TITLE3_Y + 13, COL_TITLE_RULE);
        fontRendererObj.drawString("加速时长", L, ROW3_Y + 2, COL_TEXT);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        super.drawScreen(mx, my, pt);
        // number boxes: self-drawn centered digits (absolute coords)
        drawBox(fortBox, 0, StaffProfile.cSilk);
        drawBox(rangeBox, 1, false);
        drawBox(timeBox, 2, false, TBOX_W);
        // rail hints
    }

    private void drawBox(GuiTextField box, int id, boolean dead) {
        drawBox(box, id, dead, BOX_W);
    }

    private void drawBox(GuiTextField box, int id, boolean dead, int w) {
        if (box == null) return;
        boolean focus = focused == id;
        int bx = box.xPosition, by = box.yPosition;
        drawRect(bx - 1, by - 1, bx + w + 1, by + BOX_H + 1,
                dead ? 0xff555555 : focus ? 0xffffff55 : 0xffa0a0a0);
        drawRect(bx, by, bx + w, by + BOX_H, dead ? 0xff3a3a3a : 0xff000000);
        String t = box.getText();
        int tw = fontRendererObj.getStringWidth(t);
        int tx = bx + (BOX_W - tw) / 2, ty = by + (BOX_H - 8) / 2;
        fontRendererObj.drawString(t, tx, ty, dead ? 0xff999999 : 0xffff55);
        if (focus && !dead && (System.currentTimeMillis() / 500) % 2 == 0)
            drawRect(tx + tw + 1, ty + 1, tx + tw + 2, ty + 9, 0xffffff55);
    }

    private void drawCheckBox(int x, int y, boolean on) {
        drawRect(x, y, x + 8, y + 8, 0xffa0a0a0);
        drawRect(x + 1, y + 1, x + 7, y + 7, 0xff000000);
        if (on) drawRect(x + 2, y + 2, x + 6, y + 6, 0xff7fff7f);
    }

    /** vanilla-structure button, grey hover ramp */
    private void drawButton(int x, int y, int w, int h, String label, boolean hover) {
        int c1 = hover ? BTN_H1 : BTN_G1;
        int c2 = hover ? BTN_H2 : BTN_G2;
        int c3 = hover ? BTN_H3 : BTN_G3;
        int third = Math.max(1, h / 3);
        drawRect(x, y, x + w, y + third, c1);
        drawRect(x, y + third, x + w, y + h - third, c2);
        drawRect(x, y + h - third, x + w, y + h, c3);
        drawRect(x, y, x + w, y + 1, BTN_OUTLINE);
        drawRect(x, y + h - 1, x + w, y + h, BTN_OUTLINE);
        drawRect(x, y, x + 1, y + h, BTN_OUTLINE);
        drawRect(x + w - 1, y, x + w, y + h, BTN_OUTLINE);
        drawRect(x + 1, y + 1, x + w - 1, y + 2, BTN_TOP_HI);
        int tw = fontRendererObj.getStringWidth(label);
        fontRendererObj.drawStringWithShadow(label, x + (w - tw) / 2, y + (h - 8) / 2,
                hover ? 0xffffa0 : 0xe0e0e0);
    }

    private void drawRailTab(int i, int tx, int ty, boolean top, int mx, int my) {
        boolean on = !top && i == 0; // 手杖 is this page
        String label = top ? RAIL_TABS[i] : RAIL_PIN[i];
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


    // ---------------- input ----------------

    /** push the mirror values into the (unfocused) boxes */
    private void refreshBoxes() {
        if (fortBox != null && focused != 0) fortBox.setText(String.valueOf(StaffProfile.cFortune));
        if (rangeBox != null && focused != 1) rangeBox.setText(String.valueOf(StaffProfile.cRange));
        if (timeBox != null && focused != 2) timeBox.setText(String.valueOf(StaffProfile.cTime));
    }

    /** commit + send the whole profile */
    private void send() {
        int fort = parse(fortBox);
        int range = parse(rangeBox);
        int time = parse(timeBox);
        StaffProfile.cFortune = StaffProfile.cSilk ? 0 : Math.max(0, Math.min(255, fort));
        StaffProfile.cRange = Math.max(1, Math.min(64, range));
        StaffProfile.cTime = Math.max(-1, Math.min(86400, time));
        WheelchairMod.network.sendToServer(new PacketStaffCfg(
                StaffProfile.cSilk, StaffProfile.cFortune, StaffProfile.cTopMode,
                StaffProfile.cRange, StaffProfile.cShape, StaffProfile.cMode, StaffProfile.cTime));
        refreshBoxes();
    }

    private static int parse(GuiTextField b) {
        if (b == null) return 0;
        try {
            return Integer.parseInt(b.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void keyTyped(char c, int code) {
        if (focused == 0 && fortBox != null) {
            if (code == Keyboard.KEY_RETURN) {
                send();
                fortBox.setFocused(false);
                focused = -1;
                return;
            }
            if (code == Keyboard.KEY_ESCAPE) {
                fortBox.setFocused(false);
                focused = -1;
            } else if (fortBox.textboxKeyTyped(c, code)) {
                return;
            }
        }
        if (focused == 1 && rangeBox != null) {
            if (code == Keyboard.KEY_RETURN) {
                send();
                rangeBox.setFocused(false);
                focused = -1;
                return;
            }
            if (code == Keyboard.KEY_ESCAPE) {
                rangeBox.setFocused(false);
                focused = -1;
            } else if (rangeBox.textboxKeyTyped(c, code)) {
                return;
            }
        }
        if (focused == 2 && timeBox != null) {
            if (code == Keyboard.KEY_RETURN) {
                send();
                timeBox.setFocused(false);
                focused = -1;
                return;
            }
            if (code == Keyboard.KEY_ESCAPE) {
                timeBox.setFocused(false);
                focused = -1;
            } else if (timeBox.textboxKeyTyped(c, code)) {
                return;
            }
        }
        super.keyTyped(c, code);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        // rail tabs
        net.minecraft.client.gui.ScaledResolution sr = new net.minecraft.client.gui.ScaledResolution(
                this.mc, this.mc.displayWidth, this.mc.displayHeight);
        for (int i = 0; i < RAIL_TABS.length; i++) {
            if (inRect(mx, my, guiLeft + RAIL_X + 1, guiTop + 6 + i * 17, 26, 15)) {
                if (i == 0) {
                    WheelchairMod.network.sendToServer(new PacketOpenTerminal(0,
                            GuiStoragePage.rowsFor(sr.getScaledHeight())));
                } else if (i == 1) {
                    WheelchairMod.network.sendToServer(new PacketOpenTerminal(4, 3));
                } else if (i == 2) {
                    WheelchairMod.network.sendToServer(new PacketOpenTerminal(6,
                            GuiStoragePage.rowsFor(sr.getScaledHeight())));
                } else if (i == 3) {
                    WheelchairMod.network.sendToServer(new PacketOpenTerminal(7,
                            GuiStoragePage.rowsFor(sr.getScaledHeight())));
                }
                return;
            }
        }
        int py = ySize - 36;
        for (int i = 0; i < RAIL_PIN.length; i++) {
            if (inRect(mx, my, guiLeft + RAIL_X + 1, guiTop + py + i * 17, 26, 15)) return; // 手杖=self 设置=none yet
        }
        // checkboxes
        if (button == 0 && inRect(mx, my, guiLeft + CHK1_X, guiTop + ROW1_Y, 10, 12)) {
            StaffProfile.cSilk = !StaffProfile.cSilk;
            if (StaffProfile.cSilk) StaffProfile.cFortune = 0;
            send();
            return;
        }
        // top-mode chip (右上角): cycle 五态
        String tm = "模式:" + StaffProfile.TOP_MODE_NAMES[Math.max(0, Math.min(4, StaffProfile.cTopMode))];
        int tmW = fontRendererObj.getStringWidth(tm) + 6;
        int tmX = 203 - 18 - tmW;
        if (button == 0 && inRect(mx, my, guiLeft + tmX, guiTop + TITLE1_Y - 1, tmW, BOX_H)) {
            StaffProfile.cTopMode = (StaffProfile.cTopMode + 1) % 5;
            send();
            return;
        }
        // shape / mode chips
        String shape = "形状:" + StaffProfile.SHAPE_NAMES[StaffProfile.cShape];
        int shapeW = fontRendererObj.getStringWidth(shape) + 6;
        int shapeX = RANGE_X + BOX_W + 6;
        if (button == 0 && inRect(mx, my, guiLeft + shapeX, guiTop + ROW2_Y, shapeW, BOX_H)) {
            StaffProfile.cShape = (StaffProfile.cShape + 1) % StaffProfile.SHAPE_NAMES.length;
            send();
            return;
        }
        String mode = "挖法:" + StaffProfile.MODE_NAMES[StaffProfile.cMode];
        int modeW = fontRendererObj.getStringWidth(mode) + 6;
        int modeX = shapeX + shapeW + 4;
        if (button == 0 && inRect(mx, my, guiLeft + modeX, guiTop + ROW2_Y, modeW, BOX_H)) {
            StaffProfile.cMode = (StaffProfile.cMode + 1) % StaffProfile.MODE_NAMES.length;
            send();
            return;
        }
        // number boxes take focus
        if (fortBox != null && inRect(mx, my, fortBox.xPosition - 1, fortBox.yPosition - 1, BOX_W + 2, BOX_H + 2)) {
            focused = 0;
            fortBox.setFocused(true);
            if (rangeBox != null) rangeBox.setFocused(false);
            fortBox.mouseClicked(mx, my, button);
            return;
        }
        if (rangeBox != null && inRect(mx, my, rangeBox.xPosition - 1, rangeBox.yPosition - 1, BOX_W + 2, BOX_H + 2)) {
            focused = 1;
            rangeBox.setFocused(true);
            if (fortBox != null) fortBox.setFocused(false);
            rangeBox.mouseClicked(mx, my, button);
            return;
        }
        if (timeBox != null && inRect(mx, my, timeBox.xPosition - 1, timeBox.yPosition - 1, TBOX_W + 2, BOX_H + 2)) {
            focused = 2;
            timeBox.setFocused(true);
            if (fortBox != null) fortBox.setFocused(false);
            if (rangeBox != null) rangeBox.setFocused(false);
            timeBox.mouseClicked(mx, my, button);
            return;
        }
        if (focused >= 0) { // click outside: commit + blur
            send();
            if (fortBox != null) fortBox.setFocused(false);
            if (rangeBox != null) rangeBox.setFocused(false);
            if (timeBox != null) timeBox.setFocused(false);
            focused = -1;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (fortBox != null) fortBox.updateCursorCounter();
        if (rangeBox != null) rangeBox.updateCursorCounter();
        if (timeBox != null) timeBox.updateCursorCounter();
        // the server push (PacketStaffCfgSync) lands AFTER initGui — refresh
        // the boxes when a new version arrives so the page shows the truth
        if (lastVer != StaffProfile.cVersion) {
            lastVer = StaffProfile.cVersion;
            refreshBoxes();
        }
    }

    private boolean inRect(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
