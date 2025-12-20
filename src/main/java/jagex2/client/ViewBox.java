package jagex2.client;

import deob.ObfuscatedName;
import sign.signlink;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

@ObfuscatedName("b")
public class ViewBox extends Frame {

	@ObfuscatedName("b.a")
	public GameShell shell;

	public ViewBox(boolean arg0, int arg1, GameShell arg2, int arg3) {
		this.shell = arg2;
		this.setTitle("VortexScape");
		this.setResizable(false);

		Image icon = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png"));
		this.setIconImage(icon);

		BorderLayout manager = new BorderLayout();
		this.setLayout(manager);

		this.add(this.shell, BorderLayout.CENTER);
		this.pack();

		this.setVisible(true);
		this.toFront();
	}

	public void update(Graphics arg0) {
		this.shell.update(arg0);
	}

	public void paint(Graphics arg0) {
		this.shell.paint(arg0);
	}
}
