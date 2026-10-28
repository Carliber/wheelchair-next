package wheelchair.value;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.Map;

/**
 * Enchanting cost — faithful port of the EnchantingPlus 1.7.10 (3.0.2) formula
 * used by the pack's 高级附魔台 (decompiled ContainerEnchantTable.enchantmentCost),
 * evaluated in a bookshelf-free environment (defaults: costFactor=5; the
 * no-bookshelf adjustment never raises the price there).
 *
 * Cost unit = player levels (EPlus semantics); our storage-xp exchange rate
 * is XP_PER_LEVEL points per cost level. Verbatim from the old tree.
 */
public final class EnchCost {
    private EnchCost() {}

    /** storage-xp points charged per cost level (exchange rate, declared not derived) */
    public static final int XP_PER_LEVEL = 10;

    /** absolute level ceiling for the over-max switch */
    public static final int MAX_LEVEL_CAP = 255;

    /** 1..20 in roman numerals, beyond that plain numbers */
    public static String roman(int level) {
        if (level <= 0 || level > 20) return String.valueOf(level);
        String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
                "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};
        return r[level];
    }

    /** registration name ("enchantment.damage.all") -> Enchantment, or null */
    public static Enchantment byName(String enchName) {
        for (int i = 0; i < Enchantment.enchantmentsList.length; i++) {
            Enchantment e = Enchantment.enchantmentsList[i];
            if (e != null && e.getName().equals(enchName)) return e;
        }
        return null;
    }

    /**
     * @param ench    the enchantment
     * @param target  desired level
     * @param current current level on the item (0 = none) — the DELTA is priced
     * @param item    the item being enchanted (its enchantability scales the cost)
     * @return cost in player-level units, min 1; extrapolates linearly past the
     *         game max level (vanilla min/maxEnchantability are linear in level)
     */
    public static int levels(Enchantment ench, int target, int current, ItemStack item) {
        if (ench == null || target < 1 || item == null) return 0;
        int maxLevel = Math.max(1, ench.getMaxLevel());
        // 1.7.10 vanilla (javap-verified): getMinEnchantability(l) = 1 + 10*l,
        // getMaxEnchantability(l) = min + 5 — linear in l, so a target beyond the
        // game max level still prices sanely (over-max switch support)
        int avg = (ench.getMinEnchantability(target) + ench.getMaxEnchantability(target)) / 2;
        int enchab = item.getItem().getItemEnchantability();
        if (enchab < 1) enchab = 1;
        double base = (double) avg * (double) (target - current + maxLevel)
                / ((double) maxLevel * (double) enchab);
        double cost = base * (5.0 / 3.0); // costFactor 5 / 3 (EPlus default)
        cost = enchab > 1 ? cost * (Math.log(enchab) / 2.0) : cost / 10.0;
        if (cost < 1.0) cost = 1.0;
        return (int) cost;
    }

    /** storage-xp price (points) */
    public static long xpPoints(Enchantment ench, int target, int current, ItemStack item) {
        return (long) levels(ench, target, current, item) * XP_PER_LEVEL;
    }

    /** price a book take is charged at (book enchantability is 0 -> priced against
     *  a diamond pick, matching what applying onto good gear costs) */
    public static long bookPrice(Enchantment e, int level) {
        return xpPoints(e, level, 0, new ItemStack(Items.diamond_pickaxe));
    }

    /** Replaces the whole enchantment map on a stack.
     *  EnchantmentHelper.setEnchantments() APPENDS for enchanted books (it writes
     *  StoredEnchantments one entry at a time) and does nothing when the map is
     *  empty, so a removal could never take an enchantment off a book. Books are
     *  therefore written through their own tag here. */
    public static void setAllEnchants(ItemStack s, Map<Integer, Integer> m) {
        if (s == null) return;
        if (s.getItem() == Items.enchanted_book) {
            if (m.isEmpty()) {
                if (s.hasTagCompound()) {
                    s.getTagCompound().removeTag("StoredEnchantments");
                    if (s.getTagCompound().hasNoTags()) s.setTagCompound(null);
                }
                return;
            }
            NBTTagList list = new NBTTagList();
            for (Map.Entry<Integer, Integer> en : m.entrySet()) {
                NBTTagCompound c = new NBTTagCompound();
                c.setShort("id", (short) en.getKey().intValue());
                c.setShort("lvl", (short) en.getValue().intValue());
                list.appendTag(c);
            }
            s.setTagInfo("StoredEnchantments", list);
            return;
        }
        EnchantmentHelper.setEnchantments(m, s);
    }
}
