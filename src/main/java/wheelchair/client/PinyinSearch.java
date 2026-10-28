package wheelchair.client;

import com.github.promeg.pinyinhelper.Pinyin;

/**
 * Pinyin search support on top of TinyPinyin (com.github.promeg:tinypinyin,
 * pure Java, zero transitive deps, bundled into the mod jar by the embed
 * configuration in build.gradle — 1.7.10 has no jar-in-jar).
 *
 * A query of a-z letters matches a Chinese display name when either the FULL
 * pinyin ("shitou" for 石头) or the initials ("st") contain the query.
 */
public final class PinyinSearch {
    /** does the pinyin of the name contain the query? q must be a-z only, lowercase */
    public static boolean matches(String name, String q) {
        if (name == null || q == null || q.length() == 0) return false;
        StringBuilder full = new StringBuilder();
        StringBuilder head = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Pinyin.isChinese(c)) {
                String py = Pinyin.toPinyin(c).toLowerCase();
                full.append(py);
                head.append(py.charAt(0));
            } else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                char lc = c >= 'A' && c <= 'Z' ? (char) (c + 32) : c;
                full.append(lc);
                head.append(lc);
            }
        }
        return full.indexOf(q) >= 0 || head.indexOf(q) >= 0;
    }

    /** sort key: hanzi fold to lowercase pinyin so Chinese names sort by pinyin
     *  letter order; non-hanzi chars pass through (English names sort as-is) */
    public static String key(String name) {
        if (name == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Pinyin.isChinese(c)) {
                sb.append(Pinyin.toPinyin(c).toLowerCase());
            } else if (c >= 'A' && c <= 'Z') {
                sb.append((char) (c + 32));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private PinyinSearch() {}
}
