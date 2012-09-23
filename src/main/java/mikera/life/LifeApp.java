package mikera.life;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.JFrame;

public final class LifeApp implements Runnable {
	public static boolean LOGGING = false;

	public static JFrame frame;
	public static LifePanel canvas;
	public static Engine engine = new Engine();
	private boolean running = true;
	private int mouseX;
	private int mouseY;
	private byte drawColour = 1;
	private boolean mouseDown = false;
	private static final LifeApp app = new LifeApp();
	
	private int delay=20;

	private static char keyPress = '[';

	private void menuCommand(MenuItem m, final Object c) {
		m.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				command(c);
			}
		});
	}

	private void addMenuCommand(Menu menu, String text, Object command) {
		MenuItem m1 = new MenuItem(text);
		menuCommand(m1, command);
		menu.add(m1);
	}

	public MenuBar createMenuBar() {
		MenuBar mb = new MenuBar();

		{
			Menu menu = new Menu("File");
			addMenuCommand(menu, "New", "clear");
			MenuItem m2 = new MenuItem("Open...");
			MenuItem m3 = new MenuItem("Save As...");
			menu.add(m2);
			menu.add(m3);
			mb.add(menu);
		}

		{
			Menu menu = new Menu("Zoom");
			addMenuCommand(menu, "x1", "zoom:1");
			addMenuCommand(menu, "x2", "zoom:2");
			addMenuCommand(menu, "x5", "zoom:5");
			addMenuCommand(menu, "x10", "zoom:10");
			mb.add(menu);
		}
		
		{
			Menu menu = new Menu("Speed");
			addMenuCommand(menu, "Ultra-fast", "speed:1");
			addMenuCommand(menu, "Fast", "speed:20");
			addMenuCommand(menu, "Medium", "speed:100");
			addMenuCommand(menu, "Slow", "speed:250");
			addMenuCommand(menu, "Very Slow", "speed:1000");
			mb.add(menu);
		}

		{
			Menu menu = new Menu("Simulation");
			addMenuCommand(menu, "Scatter random points (p)", "randompoints");
			addMenuCommand(menu, "Fill with random values (f)", "randomfill");
			addMenuCommand(menu, "Clear screen", "clear");
			menu.addSeparator();
			addMenuCommand(menu, "Pause execution (space)", "pause");
			mb.add(menu);
		}

		{
			Menu menu = new Menu("Rules");
			addMenuCommand(menu, "Randomise rules (r)", "randomrules");
			menu.addSeparator();
			addMenuCommand(menu, "Classic Game Of Life", "rules:life");
			addMenuCommand(menu, "Magic Mike", "rules:mikera-1");
			addMenuCommand(menu, "Brian's Brain", "rules:brians-brain");
			addMenuCommand(menu, "Warfare", "rules:warfare");
			mb.add(menu);
		}

		return mb;
	}

	public Queue<Object> commandQueue = new LinkedList<>();

	public void command(Object o) {
		synchronized (commandQueue) {
			commandQueue.add(o);
		}
	}

	public void processCommands() {
		synchronized (commandQueue) {
			for (Object o : commandQueue) {
				processCommand(o);
			}
			commandQueue.clear();
		}
	}

	public static void main(String[] args) {
		frame = new JFrame("Life");
		canvas = new LifePanel();
		canvas.setEngine(engine);
		canvas.setBackground(Color.DARK_GRAY);

		frame.setMenuBar(app.createMenuBar());

		frame.setSize(600, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(canvas);

		KeyAdapter ka = new KeyAdapter() {
			public void keyPressed(KeyEvent k) {
				char c = k.getKeyChar();
				keyPress = c;
			}
		};

		MouseMotionListener mma = new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent me) {
				app.mouseX = me.getX() / canvas.getScale();
				app.mouseY = me.getY() / canvas.getScale();
			}

			@Override
			public void mouseMoved(MouseEvent me) {
				app.mouseX = me.getX() / canvas.getScale();
				app.mouseY = me.getY() / canvas.getScale();
			}
		};

		MouseListener ma = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent me) {
				if (me.getButton() == MouseEvent.BUTTON1) {
					app.mouseDown = true;
				}
			}

			@Override
			public void mouseReleased(MouseEvent me) {
				if (me.getButton() == MouseEvent.BUTTON1) {
					app.mouseDown = false;
				}
			}
		};

		frame.addKeyListener(ka);
		canvas.addMouseMotionListener(mma);
		canvas.addMouseListener(ma);
		canvas.addKeyListener(ka);

		frame.setVisible(true);

		new Thread(app).run();
	}

	public void run() {
		System.out.println("Running lifeapp...");

		engine.setup();

		while (true) {
			status();
			handleKeyPress();
			processCommands();
			canvas.repaint();

			if (running) {
				engine.calculate();
			}

			try {
				Thread.sleep(delay);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void processCommand(Object o) {
		if (o instanceof Runnable) {
			((Runnable) o).run();
			return;
		}

		String s = o.toString();

		if (s.equals("clear")) {
			engine.clear();
			canvas.repaint();
		}

		if (s.equals("randomrules")) {
			engine.setupRandomRules();
		}

		if (s.equals("randomfill")) {
			engine.fillRandom();
		}

		if (s.equals("randompoints")) {
			engine.scatterRandomPoints();
		}

		if (s.equals("pause")) {
			running = !running;
		}

		if (s.startsWith("zoom")) {
			canvas.setScale(Integer.parseInt(s.substring(5)));
			canvas.revalidate();
		}
		
		if (s.startsWith("speed")) {
			setDelay(Integer.parseInt(s.substring(6)));
			canvas.revalidate();
		}
		
		
		if (s.startsWith("rules")) {
			try {
			engine.rules=RuleSets.getRules(s.substring(6));
			} catch (Throwable x) {
				System.err.println(x);
			}
		}
	}

	private void setDelay(int delay) {
		this.delay=delay;
	}

	private void handleKeyPress() {
		if (keyPress == 'c') {
			command("clear");
		}

		if (keyPress == 'r') {
			command("randomrules");
		}

		if (keyPress == 'f') {
			command("randomfill");
		}

		if (keyPress == 'p') {
			command("randompoints");
		}

		if (keyPress == ' ') {
			command("pause");
		}

		if ((keyPress >= '0') && (keyPress <= '9')) {
			drawColour = (byte) (keyPress - '0');
		}

		if ((mouseDown) && (mouseX < 256) && (mouseY < 256)) {
			engine.setCell(mouseX, mouseY, drawColour);
		}

		keyPress = '`';
	}

	int iteration = 0;

	public void status() {
		int pop = 0;
		for (int i = 0; i < engine.values.length; i++) {
			if (engine.values[i] != 0)
				pop++;
		}

		if (LOGGING) {
			System.out.println("Iteration " + (iteration++) + " : population="
					+ pop);
		}
	}
}