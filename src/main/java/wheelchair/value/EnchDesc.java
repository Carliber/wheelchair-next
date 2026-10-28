package wheelchair.value;

import net.minecraft.util.StatCollector;

/**
 * Short description lines for the enchantment list (hover tooltip), verbatim
 * from the old tree. Text lives in the language files under
 * {@code enchdesc.<Enchantment.getName()>}; a missing key hides the line,
 * which is how modded enchantments degrade gracefully.
 */
public final class EnchDesc {
    private EnchDesc() {}

    /** description for an enchantment name, or null when there is none */
    public static String of(String enchName) {
        if (enchName == null || enchName.length() == 0) return null;
        String key = "enchdesc." + enchName;
        String s;
        try {
            s = StatCollector.translateToLocal(key);
        } catch (Throwable t) {
            return null;
        }
        return (s == null || s.equals(key)) ? null : s;
    }
}
