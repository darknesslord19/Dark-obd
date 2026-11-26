package com.darkobd.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.*;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.*;
import android.os.*;
import android.view.WindowManager;
import android.widget.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private volatile Elm elm;
    private volatile boolean busy;
    private TextView status, output;
    private EditText address;

    private interface Job { String run() throws Exception; }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(p, p, p, p);
        status = text(root, "Bağlı değil", 18);
        address = new EditText(this);
        address.setSingleLine();
        address.setText("192.168.0.10:35000");
        root.addView(address);
        button(root, "Wi-Fi ile bağlan", this::connectWifi);
        button(root, "Bluetooth ile bağlan", this::chooseBluetooth);
        button(root, "Arıza kodlarını oku", () -> run(this::readCodes));
        button(root, "Canlı veriyi oku", () -> run(this::readLive));
        button(root, "Arıza kodlarını sil", this::confirmClear);
        button(root, "Bağlantıyı kes", () -> run(() -> { disconnect(); return "Bağlantı kesildi."; }));
        output = text(root, "Adaptörü takın, kontağı açın ve bağlanın.\nBluetooth için adaptörü önce telefon ayarlarından eşleştirin.", 16);
        output.setTextIsSelectable(true);
        ScrollView scroll = new ScrollView(this);
        scroll.setFitsSystemWindows(true);
        scroll.addView(root);
        setContentView(scroll);
    }

    private TextView text(LinearLayout root, String s, int size) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(size); v.setPadding(0, 12, 0, 12);
        root.addView(v);
        return v;
    }

    private void button(LinearLayout root, String s, Runnable r) {
        Button v = new Button(this);
        v.setText(s);
        v.setOnClickListener(x -> {
            if (busy) Toast.makeText(this, "İşlem sürüyor, bekleyin", Toast.LENGTH_SHORT).show();
            else r.run();
        });
        root.addView(v);
    }

    private void run(Job job) {
        busy = true;
        status.setText("İşlem sürüyor...");
        io.execute(() -> {
            String result;
            try { result = job.run(); }
            catch (Exception e) {
                if (e instanceof IOException) disconnect();
                result = "Hata: " + e.getMessage();
            }
            String r = result;
            runOnUiThread(() -> {
                busy = false;
                status.setText(elm != null ? "Bağlı" : "Bağlı değil");
                output.setText(r);
            });
        });
    }

    private void disconnect() {
        Elm e = elm; elm = null;
        if (e != null) try { e.close(); } catch (IOException ignored) { }
    }

    private void connectWifi() {
        String[] a = address.getText().toString().trim().split(":");
        run(() -> {
            if (a.length != 2) throw new IllegalArgumentException("Adres IP:port biçiminde olmalı");
            disconnect();
            Socket s = new Socket();
            try {
                // İnternetsiz adaptör ağında trafiğin mobil veriye gitmesini önler.
                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                for (Network n : cm.getAllNetworks()) {
                    NetworkCapabilities c = cm.getNetworkCapabilities(n);
                    if (c != null && c.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) { n.bindSocket(s); break; }
                }
                s.connect(new InetSocketAddress(a[0], Integer.parseInt(a[1])), 8000);
                Elm e = new Elm(s, s.getInputStream(), s.getOutputStream());
                e.init();
                elm = e;
            } catch (Exception e) { s.close(); throw e; }
            return "Araca bağlanıldı.";
        });
    }

    private void chooseBluetooth() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
            return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) { output.setText("Bluetooth'u açın."); return; }
        List<BluetoothDevice> list = new ArrayList<>(adapter.getBondedDevices());
        if (list.isEmpty()) { output.setText("Eşleştirilmiş cihaz yok."); return; }
        String[] names = new String[list.size()];
        for (int i = 0; i < names.length; i++) names[i] = list.get(i).getName() + "\n" + list.get(i).getAddress();
        new AlertDialog.Builder(this).setTitle("OBD adaptörünü seçin")
                .setItems(names, (d, i) -> connectBluetooth(list.get(i))).show();
    }

    private void connectBluetooth(BluetoothDevice device) {
        run(() -> {
            disconnect();
            BluetoothSocket s = device.createRfcommSocketToServiceRecord(
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            try {
                s.connect();
                Elm e = new Elm(s, s.getInputStream(), s.getOutputStream());
                e.init();
                elm = e;
            } catch (Exception e) { s.close(); throw e; }
            return "Araca bağlanıldı.";
        });
    }

    private Elm need() {
        if (elm == null) throw new IllegalStateException("Önce araca bağlanın");
        return elm;
    }

    private String readCodes() throws IOException {
        Elm e = need();
        String[][] modes = {{"03", "43", "KAYITLI"}, {"07", "47", "BEKLEYEN"}, {"0A", "4A", "KALICI"}};
        StringBuilder sb = new StringBuilder("Yalnızca standart OBD2 (emisyon) kodları. ABS, airbag ve gövde modülleri taranmaz.\n");
        for (String[] m : modes) {
            Set<String> codes = Parser.codes(e.send(m[0], 10000), m[1], e.can);
            sb.append("\n== ").append(m[2]).append(" ==\n");
            if (codes == null) sb.append("Okunamadı veya desteklenmiyor.\n");
            else if (codes.isEmpty()) sb.append("Kod bildirilmedi.\n");
            else for (String c : codes) sb.append(Codes.describe(c)).append("\n\n");
        }
        return sb.toString();
    }

    private String readLive() throws IOException {
        Elm e = need();
        return live(e, "Devir (d/dk)", "0C", 2, 0.25, 0)
                + live(e, "Hız (km/s)", "0D", 1, 1, 0)
                + live(e, "Soğutma suyu (°C)", "05", 1, 1, -40)
                + live(e, "Emme havası (°C)", "0F", 1, 1, -40)
                + live(e, "Motor yükü (%)", "04", 1, 100.0 / 255, 0)
                + live(e, "Gaz kelebeği (%)", "11", 1, 100.0 / 255, 0)
                + live(e, "Kısa dönem yakıt trimi (%)", "06", 1, 100.0 / 128, -100)
                + live(e, "Uzun dönem yakıt trimi (%)", "07", 1, 100.0 / 128, -100)
                + live(e, "Hava akışı (g/s)", "10", 2, 0.01, 0)
                + live(e, "Katalizör sıcaklığı (°C)", "3C", 2, 0.1, -40)
                + live(e, "Modül voltajı (V)", "42", 2, 0.001, 0);
    }

    private String live(Elm e, String name, String pid, int n, double scale, double offset) throws IOException {
        int[] b = Parser.pid(e.send("01" + pid, 5000), pid, n);
        if (b == null) return name + ": yok\n";
        int v = n == 2 ? b[0] * 256 + b[1] : b[0];
        return String.format(Locale.forLanguageTag("tr"), "%s: %.1f%n", name, v * scale + offset);
    }

    private void confirmClear() {
        new AlertDialog.Builder(this).setTitle("Kodlar silinsin mi?")
                .setMessage("Önce kodları not edin. Silme arızayı onarmaz; donmuş veri ve muayene hazırlık bilgileri de sıfırlanır. Kalıcı kodlar bu komutla silinmez. Kontak açık, motor kapalı ve araç park halinde olmalı.")
                .setNegativeButton("Vazgeç", null)
                .setPositiveButton("Sil", (d, w) -> run(() -> {
                    boolean ok = Parser.messages(need().send("04", 10000)).contains("44");
                    return ok ? "Silme komutu kabul edildi. Kodları yeniden okuyun." : "Silme doğrulanamadı. Kodları yeniden okuyun.";
                })).show();
    }

    @Override protected void onDestroy() {
        disconnect();
        io.shutdownNow();
        super.onDestroy();
    }
}
