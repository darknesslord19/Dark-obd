package com.darkobd.app;

import java.io.*;
import java.nio.charset.StandardCharsets;

final class Elm implements Closeable {
    private final Closeable socket;
    private final InputStream in;
    private final OutputStream out;
    boolean can;

    Elm(Closeable socket, InputStream in, OutputStream out) {
        this.socket = socket; this.in = in; this.out = out;
    }

    void init() throws IOException {
        send("ATZ", 5000);
        for (String c : new String[]{"ATE0", "ATL0", "ATS0", "ATH0", "ATSP0"}) send(c, 3000);
        if (Parser.pid(send("0100", 15000), "00", 4) == null)
            throw new IOException("Araç ECU yanıt vermedi. Kontağı açın.");
        String p = send("ATDPN", 3000).replaceAll("[^0-9A-C]", "");
        if (p.isEmpty()) throw new IOException("Protokol okunamadı");
        can = Character.digit(p.charAt(p.length() - 1), 16) >= 6;
    }

    /** '>' istemi gelene kadar okur. Zaman aşımında bağlantı kullanılmamalıdır. */
    synchronized String send(String cmd, int timeout) throws IOException {
        while (in.available() > 0) in.read();
        out.write((cmd + "\r").getBytes(StandardCharsets.US_ASCII));
        out.flush();
        StringBuilder sb = new StringBuilder();
        long end = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < end) {
            if (in.available() > 0) {
                int c = in.read();
                if (c < 0) break;
                if (c == '>') return sb.toString();
                sb.append((char) c);
            } else {
                try { Thread.sleep(10); }
                catch (InterruptedException e) { throw new InterruptedIOException(); }
            }
        }
        throw new IOException("Adaptör yanıt vermedi (" + cmd + ")");
    }

    @Override public void close() throws IOException { socket.close(); }
}
