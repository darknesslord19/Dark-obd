package com.darkobd.app;

import java.util.*;

/** ELM327 yanıt çözücü (ATH0, ATS0). */
final class Parser {
    /** Satırları mesajlara ayırır; CAN çok parçalı yanıtları birleştirir. */
    static List<String> messages(String raw) {
        List<String> out = new ArrayList<>();
        StringBuilder multi = null;
        int length = 0;
        for (String line : raw.split("[\\r\\n]+")) {
            String s = line.replace(" ", "").replace(">", "").toUpperCase(Locale.ROOT);
            if (s.matches("[0-9A-F]{3}")) {
                flush(out, multi, length);
                length = Integer.parseInt(s, 16) * 2;
                multi = new StringBuilder();
            } else if (multi != null && s.matches("[0-9A-F]:[0-9A-F]+")) {
                multi.append(s.substring(2));
            } else if (s.matches("([0-9A-F]{2})+")) {
                flush(out, multi, length);
                multi = null;
                out.add(s);
            }
        }
        flush(out, multi, length);
        return out;
    }

    private static void flush(List<String> out, StringBuilder m, int length) {
        if (m != null && length > 0 && m.length() >= length) out.add(m.substring(0, length));
    }

    /** mode: "43" kayıtlı, "47" bekleyen, "4A" kalıcı. null = geçerli yanıt yok. */
    static Set<String> codes(String raw, String mode, boolean can) {
        Set<String> result = new LinkedHashSet<>();
        boolean valid = false;
        for (String m : messages(raw)) {
            if (!m.startsWith(mode)) continue;
            int start = 2, end = m.length();
            if (can) {
                if (m.length() < 4) continue;
                start = 4;
                int needed = 4 + Integer.parseInt(m.substring(2, 4), 16) * 4;
                if (needed > m.length()) return null; // eksik yanıt
                end = needed;
            }
            valid = true;
            for (int i = start; i + 4 <= end; i += 4) {
                String h = m.substring(i, i + 4);
                if (h.equals("0000")) continue;
                int first = Character.digit(h.charAt(0), 16);
                result.add("" + "PCBU".charAt(first >> 2) + (first & 3) + h.substring(1));
            }
        }
        return valid ? result : null;
    }

    /** Mod 01 veri baytları. */
    static int[] pid(String raw, String pid, int count) {
        for (String m : messages(raw)) {
            if (m.startsWith("41" + pid) && m.length() >= 4 + count * 2) {
                int[] b = new int[count];
                for (int i = 0; i < count; i++)
                    b[i] = Integer.parseInt(m.substring(4 + i * 2, 6 + i * 2), 16);
                return b;
            }
        }
        return null;
    }
}
