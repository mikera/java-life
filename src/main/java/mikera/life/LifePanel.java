package mikera.life;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class LifePanel extends JPanel {
	private Engine engine=null;
	private int scale=2;

	BufferedImage bi = null;
	int[] disp = new int[65536];

	public Engine getEngine() {
		return engine;
	}

	public void setEngine(Engine engine) {
		this.engine = engine;
	}
	
	public void paintComponent(Graphics g) {
		display(g);
	}	

	private void display(Graphics g) {
		if (bi == null) {
			bi = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
		}

		for (int i = 0; i < 65536; i++) {
			disp[i] = engine.rules.getColours()[engine.values[i] & 255];
		}

		bi.setRGB(0, 0, 256, 256, disp, 0, 256);

		g.drawImage(bi, 0, 0, 256 * getScale(), 256 * getScale(), null);
	}

	public int getScale() {
		return scale;
	}

	public void setScale(int scale) {
		this.scale = scale;
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(256*scale,256*scale);
	}
}
