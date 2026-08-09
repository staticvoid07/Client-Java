package com.vortexscape.launcher;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Locale;

/**
 * Bootstrap that keeps rs2client.jar up to date and then runs it.
 *
 * Usage:  java -jar launcher.jar &lt;manifestUrl&gt; [client args...]
 *
 * Everything after the manifest URL is handed to the client untouched, so the
 * wrapper (exe / .sh) keeps owning the host and portOffset arguments and this
 * class never needs to know about them. That is also what lets one launcher
 * binary serve both live and staging - only the wrapper's arguments differ.
 *
 * Design rules worth keeping if this is ever edited:
 *
 *  - The launcher downloads and then EXECUTES code, so the transport has to be
 *    https and the payload has to be hash-verified. Both are enforced and
 *    neither should become optional.
 *  - A player must never be locked out because the website is unreachable. Any
 *    failure to reach or parse the manifest falls back to the cached jar; only a
 *    cold start with no cache is fatal.
 *  - No third-party dependencies. This jar is the one thing that cannot be
 *    auto-updated, so it stays small, boring, and rarely touched.
 */
public final class Launcher {
    private static final String CLIENT_MAIN = "jagex2.client.Client";

    private static Splash splash;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            fail("Launcher started without a channel and manifest URL.\n\n" + "Expected: java -jar launcher.jar <channel> <manifestUrl> [client args]");
            return;
        }

        // The channel is baked into the wrapper rather than read from the manifest.
        // It has to be known while offline, because it selects the cache directory -
        // taking it from the manifest would send an offline staging player looking in
        // the live cache and report "no cached copy" while one sits right there.
        String channel = args[0];
        String manifestUrl = System.getProperty("vortexscape.manifest", args[1]);
        String[] clientArgs = Arrays.copyOfRange(args, 2, args.length);

        if (!channel.matches("[A-Za-z0-9._-]{1,32}")) {
            fail("Launcher was given an unusable channel name: " + channel);
            return;
        }

        splash = new Splash();
        splash.status("Checking for updates...");

        Manifest manifest = null;
        try {
            manifest = Manifest.fetch(manifestUrl);
            if (!channel.equals(manifest.channel)) {
                // Wrong manifest for this build - a swapped file or a bad deploy.
                // Ignore it rather than pulling a live jar into a staging install
                // (or the reverse).
                System.err.println("[launcher] manifest is for channel '" + manifest.channel + "' but this build is '" + channel + "' - ignoring it");
                manifest = null;
            }
        } catch (Exception e) {
            // Non-fatal by design - fall through and use whatever is cached.
            System.err.println("[launcher] manifest fetch failed: " + e);
        }

        File jar = new File(cacheDir(channel), "rs2client.jar");

        if (manifest != null) {
            try {
                ensureCurrent(manifest, jar);
            } catch (Exception e) {
                System.err.println("[launcher] update failed: " + e);
                if (!jar.isFile()) {
                    splash.dispose();
                    fail("Could not download the game client.\n\n" + e.getMessage());
                    return;
                }
                // A cached jar exists - better to run slightly stale than not at all.
                System.err.println("[launcher] continuing with the cached client");
            }
        }

        if (!jar.isFile()) {
            splash.dispose();
            fail("The game client could not be downloaded and no cached copy is available.\n\n" + "Check your internet connection and try again.");
            return;
        }

        splash.status("Starting...");
        launch(jar, clientArgs);
        splash.dispose();
    }

    /** Download the jar only if the cached copy does not already match the manifest hash. */
    private static void ensureCurrent(Manifest manifest, File jar) throws Exception {
        if (jar.isFile()) {
            String local = LauncherIO.sha256(jar);
            if (local.equalsIgnoreCase(manifest.sha256)) {
                System.out.println("[launcher] client up to date (" + manifest.version + ")");
                return;
            }
            System.out.println("[launcher] update available: " + manifest.version);
        }

        splash.status("Downloading update " + manifest.version + "...");

        File parent = jar.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("could not create " + parent);
        }

        // Download beside the target then rename, so an interrupted download can
        // never leave a truncated jar that looks valid.
        File tmp = new File(parent, "rs2client.jar.part");
        LauncherIO.download(manifest.jarUrl, tmp);

        String got = LauncherIO.sha256(tmp);
        if (!got.equalsIgnoreCase(manifest.sha256)) {
            tmp.delete();
            throw new IOException("downloaded client failed verification\nexpected " + manifest.sha256 + "\ngot      " + got);
        }

        // Windows will not rename onto an existing file.
        if (jar.exists() && !jar.delete()) {
            throw new IOException("could not replace " + jar);
        }
        if (!tmp.renameTo(jar)) {
            throw new IOException("could not move the downloaded client into place");
        }
        System.out.println("[launcher] updated to " + manifest.version);
    }

    /**
     * Run the client in this JVM. Keeps it to a single process so the taskbar
     * entry and icon stay the wrapper's, rather than spawning a bare java.exe.
     */
    private static void launch(File jar, String[] clientArgs) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[] { jar.toURI().toURL() }, Launcher.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
        Class<?> clientClass = Class.forName(CLIENT_MAIN, true, loader);
        Method main = clientClass.getMethod("main", String[].class);
        main.invoke(null, (Object) clientArgs);
    }

    /**
     * Per-channel cache directory, so a staging build can never overwrite the
     * live client's jar.
     */
    static File cacheDir(String channel) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String home = System.getProperty("user.home", ".");
        File base;
        if (os.contains("win")) {
            String local = System.getenv("LOCALAPPDATA");
            base = new File(local != null ? local : home, "VortexScape");
        } else if (os.contains("mac")) {
            base = new File(home, "Library/Application Support/VortexScape");
        } else {
            String xdg = System.getenv("XDG_DATA_HOME");
            base = new File(xdg != null ? xdg : home + "/.local/share", "vortexscape");
        }
        return new File(base, channel);
    }

    private static void fail(String message) {
        System.err.println("[launcher] " + message);
        try {
            JOptionPane.showMessageDialog(null, message, "VortexScape Launcher", JOptionPane.ERROR_MESSAGE);
        } catch (Throwable ignored) {
            // headless - the stderr line above is all we can do
        }
        System.exit(1);
    }

    private Launcher() {}
}
