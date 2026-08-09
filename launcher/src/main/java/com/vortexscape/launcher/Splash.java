package com.vortexscape.launcher;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.net.URL;

/**
 * Minimal "checking for updates" window.
 *
 * Exists so a slow download does not look like the exe silently failing to
 * start - the most common support complaint for launchers that show nothing.
 * Degrades to no-ops when headless so the launcher stays usable from a terminal.
 */
final class Splash {
    private JFrame frame;
    private JLabel label;

    Splash() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame("VortexScape");
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.setUndecorated(false);
                frame.setResizable(false);

                URL icon = Splash.class.getResource("/icon.png");
                if (icon != null) {
                    Image img = new ImageIcon(icon).getImage();
                    frame.setIconImage(img);
                }

                JPanel panel = new JPanel(new BorderLayout());
                panel.setBackground(new Color(0x1b, 0x1b, 0x1b));

                label = new JLabel("Starting...", SwingConstants.CENTER);
                label.setForeground(new Color(0xe0, 0xe0, 0xe0));
                label.setFont(new Font("SansSerif", Font.PLAIN, 13));
                panel.add(label, BorderLayout.CENTER);

                frame.setContentPane(panel);
                frame.setSize(320, 110);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            });
        } catch (Throwable ignored) {
            frame = null;
        }
    }

    void status(String text) {
        System.out.println("[launcher] " + text);
        if (frame == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> label.setText(text));
    }

    void dispose() {
        if (frame == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            frame.setVisible(false);
            frame.dispose();
        });
    }
}
