package com.darkobd.app;

import java.util.*;

/** Yaygın genel (SAE) kodların Türkçe açıklamaları ve kontrol önerileri. */
final class Codes {
    private static final Map<String, String[]> MAP = new HashMap<>();
    private static void add(String code, String title, String check) {
        MAP.put(code, new String[]{title, check});
    }
    static {
        String air = "Hava kaçağı, emiş hortumları, sensör soketi ve kablosu.";
        add("P0100", "Hava akış (MAF) devresi arızası", air);
        add("P0101", "Hava akış (MAF) sinyali aralık dışı", air);
        add("P0102", "Hava akış (MAF) sinyali düşük", air);
        add("P0103", "Hava akış (MAF) sinyali yüksek", air);
        add("P0113", "Emme havası sıcaklık sensörü sinyali yüksek", "Sensör, soket ve açık devre.");
        add("P0117", "Soğutma suyu sıcaklık sensörü sinyali düşük", "Sensör, soket ve kısa devre.");
        add("P0118", "Soğutma suyu sıcaklık sensörü sinyali yüksek", "Sensör, soket ve açık devre.");
        add("P0128", "Motor çalışma sıcaklığına yeterince ulaşmıyor", "Termostat, soğutma suyu seviyesi, sıcaklık sensörü.");
        add("P0130", "O2 sensörü devresi (sıra 1, sensör 1)", "Sensör kablosu, soket, egzoz kaçağı.");
        add("P0135", "O2 sensörü ısıtıcı devresi (sıra 1, sensör 1)", "Sigorta, kablo, sensör ısıtıcısı.");
        add("P0141", "O2 sensörü ısıtıcı devresi (sıra 1, sensör 2)", "Sigorta, kablo, sensör ısıtıcısı.");
        String fuel = "Hava ve egzoz kaçağı, yakıt basıncı, MAF, enjektörler, O2 verileri.";
        add("P0171", "Karışım fakir (sıra 1)", fuel);
        add("P0172", "Karışım zengin (sıra 1)", fuel);
        String miss = "Buji, bobin, enjektör, kompresyon. Dizelde yakıt ve kompresyon. Motor lambası yanıp sönüyorsa aracı zorlamayın.";
        add("P0300", "Rastgele / birden fazla silindirde tekleme", miss);
        for (int i = 1; i <= 4; i++) add("P030" + i, i + ". silindirde tekleme", miss);
        add("P0325", "Vuruntu sensörü devresi", "Sensör, soket ve kablo.");
        add("P0335", "Krank mili konum sensörü devresi", "Sensör, soket, kablo, tetik dişlisi.");
        add("P0340", "Eksantrik mili konum sensörü devresi", "Sensör, soket, kablo, zamanlama.");
        add("P0401", "EGR akışı yetersiz", "EGR valfi, kanal tıkanıklığı, ilgili sensörler.");
        add("P0420", "Katalizör verimi düşük (sıra 1)", "Önce tekleme, karışım arızası ve egzoz kaçağını giderin; O2 sensörlerini kontrol edin. Kod tek başına katalizörün bozuk olduğunu kanıtlamaz.");
        add("P0441", "Yakıt buharı (EVAP) tahliye akışı hatalı", "Tahliye valfi ve hortumlar.");
        add("P0455", "Yakıt buharı sisteminde büyük kaçak", "Depo kapağı, contası ve hortumlar.");
        add("P0500", "Araç hız sensörü arızası", "Sensör, kablo ve ABS sistemi.");
        add("P0505", "Rölanti kontrol sistemi arızası", "Gaz kelebeği, hava kaçağı, kablo.");
        add("P0562", "Sistem voltajı düşük", "Akü, şarj dinamosu, şasi bağlantıları.");
        add("P0563", "Sistem voltajı yüksek", "Şarj regülatörü ve şarj sistemi.");
        add("P2002", "Dizel partikül filtresi (DPF) verimi düşük", "Fark basınç sensörü, hortumlar, egzoz kaçağı, kurum yükü.");
    }

    static String describe(String code) {
        String[] d = MAP.get(code);
        if (d != null) return code + " - " + d[0] + "\nKontrol: " + d[1];
        String area = code.startsWith("P") ? "Motor / aktarma organı" : code.startsWith("C") ? "Şasi"
                : code.startsWith("B") ? "Gövde" : "İletişim ağı";
        return code + " - " + area + " kodu\nBu sürümde açıklaması yok. Nissan servis bilgisinden doğrulayın.";
    }
}
