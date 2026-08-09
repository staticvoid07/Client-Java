package com.vortexscape.launcher;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/** Network and hashing helpers. No third-party dependencies on purpose - see Launcher. */
final class LauncherIO {
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 60000;
    private static final long MAX_DOWNLOAD_BYTES = 64L * 1024 * 1024;
    private static final int MAX_MANIFEST_BYTES = 1024 * 1024;

    static byte[] read(String url) throws IOException {
        HttpURLConnection conn = open(url);
        try (InputStream in = conn.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                if (out.size() + n > MAX_MANIFEST_BYTES) {
                    throw new IOException("manifest is implausibly large, refusing");
                }
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    static void download(String url, File dest) throws IOException {
        HttpURLConnection conn = open(url);
        try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[16384];
            long total = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > MAX_DOWNLOAD_BYTES) {
                    throw new IOException("client download exceeded " + (MAX_DOWNLOAD_BYTES / 1024 / 1024) + "MB, refusing");
                }
                out.write(buf, 0, n);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static HttpURLConnection open(String url) throws IOException {
        // The launcher executes what it downloads. Plain http would let anyone on
        // the network path replace the client, so it is refused outright rather
        // than warned about. localhost is permitted for development only.
        String lower = url.toLowerCase(Locale.ROOT);
        boolean local = lower.startsWith("http://localhost") || lower.startsWith("http://127.0.0.1");
        if (!lower.startsWith("https://") && !local) {
            throw new IOException("refusing to fetch over an insecure connection: " + url);
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("User-Agent", "VortexScape-Launcher");
        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IOException("HTTP " + code + " for " + url);
        }
        return conn;
    }

    static String sha256(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            byte[] buf = new byte[16384];
            int n;
            while ((n = raf.read(buf)) != -1) {
                md.update(buf, 0, n);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) {
            sb.append(Character.forDigit((b >> 4) & 0xf, 16));
            sb.append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }

    private LauncherIO() {}
}
