package wheelchair;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.config.Configuration;
import wheelchair.network.PacketAccelAction;
import wheelchair.network.PacketAccelSync;
import wheelchair.network.PacketEnchAction;
import wheelchair.network.PacketEnchBatch;
import wheelchair.network.PacketEnergyAction;
import wheelchair.network.PacketEnergySync;
import wheelchair.network.PacketOpenTerminal;
import wheelchair.network.PacketStaffCfg;
import wheelchair.network.PacketStaffCfgSync;
import wheelchair.network.PacketStaffDig;
import wheelchair.network.PacketStorageAction;
import wheelchair.network.PacketStorageSync;
import wheelchair.proxy.CommonProxy;
import wheelchair.recipe.RecipeIndex;
import wheelchair.value.PriceRegistry;

import java.io.File;

/**
 * 轮椅Mod（重启树）— 阶段: 无限存储（数据层 + O 键存储页 GUI，原型 v25）.
 * 数据格式与 2.x 完全兼容（wheelchair_storage / wheelchair_protos WorldSavedData）,
 * 老存档直接迁移.
 */
@Mod(modid = WheelchairMod.MODID, name = WheelchairMod.NAME, version = WheelchairMod.VERSION)
public class WheelchairMod {
    public static final String MODID = "wheelchair";
    public static final String NAME = "轮椅Mod";
    public static final String VERSION = "0.3.0";

    /** journey mode: withdrawals (items AND resources) do not drain stored counts */
    public static boolean journeyMode = false;
    /** journey mode detail: NBT/enchanted items ('~' uid) get their own switch, default = still drained */
    public static boolean journeyKeepNbt = false;
    /** EU->RF conversion factor (community convention 1EU=4RF, config-adjustable) */
    public static int euToRf = 4;
    /** enchant page: 上限 checkbox — ON (default) = respect the game max level,
     *  OFF = typed levels may go up to MAX_LEVEL_CAP (255) */
    public static boolean enchLimit = true;
    /** enchant page: 适配 — only list enchantments that can apply to the item */
    public static boolean enchAdaptive = true;

    public static SimpleNetworkWrapper network;

    @Mod.Instance(MODID)
    public static WheelchairMod instance;

    @SidedProxy(clientSide = "wheelchair.proxy.ClientProxy", serverSide = "wheelchair.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        Configuration cfg = new Configuration(new File(e.getModConfigurationDirectory(), MODID + ".cfg"));
        cfg.load();
        wheelchair.value.StorageData.MAX_PROTO_BYTES = cfg.get("storage", "maxProtoItemKB", 4,
                "Max compressed NBT size in KB for a single stored prototype item").getInt() * 1024;
        wheelchair.value.StorageData.PROTO_BUDGET_BYTES = (long) cfg.get("storage", "protoBudgetKB", 1024,
                "Total compressed KB budget for ALL stored NBT prototypes").getInt() * 1024;
        journeyMode = cfg.get("journey", "enabled", false,
                "Journey mode: withdrawing items/fluids/resources from the digital storage does not drain the stored counts").getBoolean(false);
        journeyKeepNbt = cfg.get("journey", "keepNbtEnch", false,
                "Journey mode: also keep NBT/enchanted items (items with a '~' uid); default they still drain").getBoolean(false);
        euToRf = Math.max(1, cfg.get("energy", "euToRf", 4,
                "EU to RF conversion (community convention 1EU=4RF)").getInt());
        enchLimit = cfg.get("ench", "limitLevels", true,
                "Enchant page: checkbox 上限 ON = respect each enchantment's game max level (default); unchecked = levels up to 255").getBoolean(true);
        if (cfg.hasChanged()) cfg.save();

        proxy.preInit(); // client: keybind O + task queue
        // ported InfiniStack compat layer (MIT): load its config — reflective so
        // the -source 1.6 main tree never needs the compat classes at compile time
        try {
            Class.forName("wheelchair.compat.infinistack.helper.Configurables")
                    .getMethod("init", FMLPreInitializationEvent.class).invoke(null, e);
        } catch (Throwable t) {
            cpw.mods.fml.common.FMLLog.warning("[wheelchair] compat config init failed: %s",
                    new Object[]{String.valueOf(t)});
        }
        wheelchair.item.ItemStaff staff = new wheelchair.item.ItemStaff();
        wheelchair.item.ItemStaff.STAFF = staff;
        cpw.mods.fml.common.registry.GameRegistry.registerItem(staff, "staff");
        // energy test nodes: infinite generator, energy trash, storage-pool battery
        cpw.mods.fml.common.registry.GameRegistry.registerBlock(
                new wheelchair.block.BlockGenerator(), "generator");
        cpw.mods.fml.common.registry.GameRegistry.registerBlock(
                new wheelchair.block.BlockSink(), "sink");
        cpw.mods.fml.common.registry.GameRegistry.registerBlock(
                new wheelchair.block.BlockBattery(), "battery");
        // TE mappings — WITHOUT this FML's writeToNBT hook throws "missing a mapping"
        // on chunk save (bit us on the generator during the energy-mark test)
        cpw.mods.fml.common.registry.GameRegistry.registerTileEntity(
                wheelchair.block.BlockGenerator.TileGenerator.class, "wheelchair:generator");
        cpw.mods.fml.common.registry.GameRegistry.registerTileEntity(
                wheelchair.block.BlockSink.TileSink.class, "wheelchair:sink");
        cpw.mods.fml.common.registry.GameRegistry.registerTileEntity(
                wheelchair.block.BlockBattery.TileBattery.class, "wheelchair:battery");
        network = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);
        network.registerMessage(PacketOpenTerminal.Handler.class, PacketOpenTerminal.class, 0, Side.SERVER);
        network.registerMessage(PacketStorageAction.Handler.class, PacketStorageAction.class, 1, Side.SERVER);
        network.registerMessage(PacketStorageSync.Handler.class, PacketStorageSync.class, 2, Side.CLIENT);
        network.registerMessage(PacketEnchAction.Handler.class, PacketEnchAction.class, 3, Side.SERVER);
        network.registerMessage(PacketEnchBatch.Handler.class, PacketEnchBatch.class, 4, Side.SERVER);
        network.registerMessage(PacketStaffCfg.Handler.class, PacketStaffCfg.class, 5, Side.SERVER);
        network.registerMessage(PacketStaffDig.Handler.class, PacketStaffDig.class, 6, Side.SERVER);
        network.registerMessage(PacketStaffCfgSync.Handler.class, PacketStaffCfgSync.class, 7, Side.CLIENT);
        network.registerMessage(PacketEnergySync.Handler.class, PacketEnergySync.class, 8, Side.CLIENT);
        network.registerMessage(PacketEnergyAction.Handler.class, PacketEnergyAction.class, 9, Side.SERVER);
        network.registerMessage(PacketAccelSync.Handler.class, PacketAccelSync.class, 10, Side.CLIENT);
        network.registerMessage(PacketAccelAction.Handler.class, PacketAccelAction.class, 11, Side.SERVER);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        // drain netty-thread packet work onto the server thread (1.7.10 has no addScheduledTask)
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(
                new wheelchair.network.ServerTaskQueue());
        // frame-sliced staff mining runs on the same server-tick bus
        cpw.mods.fml.common.FMLCommonHandler.instance().bus().register(
                new wheelchair.value.StaffDigTask());
        // energy-mark transfer engine (mark list + 存/取 + telemetry)
        wheelchair.server.EnergyTick.install();
        // acceleration-mark engine (TE extra ticks + entity timers)
        wheelchair.value.AccelHub.install();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
        // 手杖配方 = 与 vanilla 木铲完全一样 (X/#/#, X=木板)。两个配方共存于同一
        // 输入 — 这就是合成切换 (‹ ›) 的测试场景: 同输入多产物循环
        cpw.mods.fml.common.registry.GameRegistry.addRecipe(
                new net.minecraft.item.ItemStack(wheelchair.item.ItemStaff.STAFF),
                "X", "#", "#",
                'X', net.minecraft.init.Blocks.planks,
                '#', net.minecraft.init.Items.stick);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        proxy.postInit(); // client: short-count digit renderer (container GUIs + HUD)
        RecipeIndex.build();
        // every item stacks to Integer.MAX_VALUE (user feature): reflective patch
        // of Item.maxStackSize (mapped field, works dev + obf)
        int patched = 0;
        for (Object o : net.minecraft.item.Item.itemRegistry) {
            if (o == null) continue;
            net.minecraft.item.Item it = (net.minecraft.item.Item) o;
            // InfiniStack policy (user call): items that are ALREADY limit-1
            // (tools / weapons / rods / armor) keep their 1 — that keeps every
            // vanilla limit-1 check intact (isItemEnchantable, isItemTool, anvil,
            // furnace fuel slot ...). Only originally-stackable items get intmax.
            if (it.getItemStackLimit() == 1) continue;
            try {
                cpw.mods.fml.common.ObfuscationReflectionHelper.setPrivateValue(
                        net.minecraft.item.Item.class, it,
                        Integer.valueOf(Integer.MAX_VALUE),
                        "maxStackSize", "field_77777_bU"); // MCP name dev, SRG name production
                patched++;
            } catch (Throwable t) {
                cpw.mods.fml.common.FMLLog.warning("[wheelchair] maxStackSize patch failed on %s",
                        new Object[]{String.valueOf(o)});
            }
        }
        cpw.mods.fml.common.FMLLog.info("[wheelchair] maxStackSize -> Integer.MAX_VALUE on %s items",
                new Object[]{Integer.valueOf(patched)});
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent e) {
        RecipeIndex.build();
        PriceRegistry.build(); // also on the client, so search/pricing work when connected to a remote server
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent e) {
        RecipeIndex.build();
        PriceRegistry.build();
        e.registerServerCommand(new CommandWheelchair());
    }
}
