package wheelchair.value;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/**
 * Staff profile (手杖配置), one GLOBAL profile per world (cheat mod semantics):
 * silk/fortune are the BASE mining profile, chainOn/range/shape drive the
 * chain-mining. Stored in WorldSavedData, edited by GuiStaffPage via
 * PacketStaffCfg, read by the dig handler.
 */
public final class StaffProfile extends WorldSavedData {

    private static final String KEY = "wheelchair.staff";

    // ---- base ----
    public boolean silk;        // 精准采集 (exclusive with fortune)
    public int fortune;         // 时运等级 0..255 (ignored while silk)
    // ---- top mode (user call 2026-10-25: Shift+right-click cycles) ----
    public int topMode;         // 0 普通 1 连锁挖矿 2 设置电力 3 整地 4 加速
    public static final String[] TOP_MODE_NAMES = {"普通", "连锁挖矿", "设置电力", "整地", "加速"};
    public int range = 8;       // 范围 1..64: shapeless 半径 / 隧道层数 / 3x3 深度
    public int shape;           // 0 跟随 1 1x1隧道 2 3x3 3 3x3隧道 4 下阶梯 5 上阶梯
    public int mode;            // 0 普通(连锁采掘) 1 增强(不相邻也挖) 2 挖空(形状内所有无NBT方块)
    public int time = 120;      // 加速标记初始时长(秒), -1=永久 (加速模式手势用)

    public StaffProfile(String key) {
        super(key);
    }

    /** the world's profile (created on first access) */
    public static StaffProfile get(World w) {
        StaffProfile p = (StaffProfile) w.perWorldStorage.loadData(StaffProfile.class, KEY);
        if (p == null) {
            p = new StaffProfile(KEY);
            w.perWorldStorage.setData(KEY, p);
        }
        return p;
    }

    /** clamp + apply an incoming config (server side, hostile input safe) */
    public void apply(boolean silk, int fortune, int topMode, int range, int shape, int mode, int time) {
        this.silk = silk;
        this.fortune = silk ? 0 : Math.max(0, Math.min(255, fortune));
        this.topMode = Math.max(0, Math.min(4, topMode));
        this.range = Math.max(1, Math.min(64, range));
        this.shape = Math.max(0, Math.min(5, shape));
        this.mode = Math.max(0, Math.min(2, mode));
        this.time = Math.max(-1, Math.min(86400, time));
        markDirty();
    }

    public static final String[] SHAPE_NAMES = {"跟随", "1x1隧道", "3x3", "3x3隧道", "下阶梯", "上阶梯"};
    public static final String[] MODE_NAMES = {"普通", "增强", "挖空"};

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        silk = nbt.getBoolean("silk");
        fortune = Math.max(0, Math.min(255, nbt.getInteger("fortune")));
        // 迁移: 老存档 chain 布尔 -> topMode 0/1; 新档读 topMode
        topMode = Math.max(0, Math.min(4, nbt.getInteger("topMode")));
        if (!nbt.hasKey("topMode") && nbt.getBoolean("chain")) topMode = 1;
        range = Math.max(1, Math.min(64, nbt.getInteger("range")));
        if (range == 1 && !nbt.hasKey("range")) range = 8; // fresh worlds: default 8
        shape = Math.max(0, Math.min(5, nbt.getInteger("shape")));
        mode = Math.max(0, Math.min(2, nbt.getInteger("mode")));
        time = Math.max(-1, Math.min(86400, nbt.getInteger("time")));
        if (!nbt.hasKey("time")) time = 120;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setBoolean("silk", silk);
        nbt.setInteger("fortune", fortune);
        nbt.setInteger("topMode", topMode);
        nbt.setInteger("range", range);
        nbt.setInteger("shape", shape);
        nbt.setInteger("mode", mode);
        nbt.setInteger("time", time);
    }

    // ---------------- client mirror (pushed by PacketStaffCfgSync on GUI open) ----------------

    public static boolean cSilk;
    public static int cFortune;
    public static int cTopMode;
    public static int cRange = 8;
    public static int cShape;
    public static int cMode;
    public static int cTime = 120;
    /** bumped every time the server push lands — GUIs watch it to refresh */
    public static int cVersion;
    /** item-name suffix shows the live mode (client mirror) */
    public static String modeSuffix() {
        if (cTopMode == 1) return " (连锁)";
        if (cTopMode == 2) return " (电力)";
        if (cTopMode == 3) return " (整地)";
        if (cTopMode == 4) return " (加速)";
        return "";
    }
}
