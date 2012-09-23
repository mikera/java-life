package mikera.life;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;

public final class LifeApp implements Runnable {
        public static Frame frame;
        public static Canvas canvas;
        public static Engine engine=new Engine();
        private boolean running=true;
        private int scale=3;
        private int mouseX;
        private int mouseY;
        private byte drawColour=1;
        private boolean mouseDown=false;
        private static final LifeApp app=new LifeApp();
        
        private static char keyPress=' ';
        
        private void menuCommand(MenuItem m, final String c) {
                m.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent arg0) {
                                command(c);
                        }               
                });             
        }
        
        public MenuBar createMenuBar() {
                
                Menu menu=new Menu("File");
                {
                        MenuItem m1=new MenuItem("New");
                        menuCommand(m1,"clear");        
                        menu.add(m1);
                        MenuItem m2=new MenuItem("Open...");
                        MenuItem m3=new MenuItem("Save As...");
                        menu.add(m2);
                        menu.add(m3);
                }

                Menu menu2=new Menu("Zoom");
                {
                        MenuItem m1=new MenuItem("x1");
                        menuCommand(m1,"zoom:1");       
                        menu2.add(m1);
                        
                        MenuItem m2=new MenuItem("x2");
                        menuCommand(m2,"zoom:2");       
                        menu2.add(m2);
                        
                        MenuItem m3=new MenuItem("x5");
                        menuCommand(m3,"zoom:5");       
                        menu2.add(m3);
                }
                
                Menu menu3=new Menu("Simulation");
                {
                        MenuItem m1=new MenuItem("Scatter random points (space)");
                        menuCommand(m1,"randompoints");       
                        menu3.add(m1);
                        MenuItem m2=new MenuItem("Fill with random values (f)");
                        menuCommand(m2,"randomfill");       
                        menu3.add(m2);
                        MenuItem m3=new MenuItem("Clear screen");
                        menuCommand(m3,"clear");        
                        menu3.add(m3);
                }
                
                Menu menu4=new Menu("Rules");
                {         
                    MenuItem m4=new MenuItem("Randomise rules (r)");
                    menuCommand(m4,"randomrules");       
                    menu4.add(m4);
                }
                
                
                MenuBar mb=new MenuBar();
                mb.add(menu);
                mb.add(menu2);
                mb.add(menu3);
                mb.add(menu4);

                return mb;
        }
        
        public Queue<Object> commandQueue=new LinkedList<Object>();
        
        public void command(Object o) {
                synchronized(commandQueue) {
                        commandQueue.add(o);
                }
        }
        
        public void processCommands() {
                synchronized(commandQueue) {
                        for (Object o:commandQueue) {
                                processCommand(o);
                        }
                        commandQueue.clear();
                }
        }
        
        public static void main(String[] args) {
                frame=new Frame("Life");
                canvas=new Canvas();
                canvas.setBackground(Color.DARK_GRAY);
                

                frame.setMenuBar(app.createMenuBar());
                
                frame.setSize(400,400);
                frame.addWindowListener(new WindowAdapter() {
                        public void windowClosing(WindowEvent e) {
                                System.exit(0);
                        }
                });
                frame.add(canvas);
                
                KeyAdapter ka=new KeyAdapter() {
                        public void keyPressed(KeyEvent k) {
                                char c=k.getKeyChar();
                                
                                keyPress=c;
                        }
                };
                
                MouseMotionListener mma=new MouseMotionAdapter() {
                        @Override
                        public void mouseDragged(MouseEvent me) {
                                app.mouseX=me.getX()/app.scale;
                                app.mouseY=me.getY()/app.scale;
                        }

                        @Override
                        public void mouseMoved(MouseEvent me) {
                                app.mouseX=me.getX()/app.scale;
                                app.mouseY=me.getY()/app.scale;
                        }
                };
                
                MouseListener ma=new MouseAdapter() {
                        @Override 
                        public void mousePressed(MouseEvent me) {
                                if (me.getButton()==MouseEvent.BUTTON1) {
                                        app.mouseDown=true;
                                }
                        }
                        
                        @Override 
                        public void mouseReleased(MouseEvent me) {
                                if (me.getButton()==MouseEvent.BUTTON1) {
                                        app.mouseDown=false;
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
                        display();
                        
                        if (running) {
                                engine.calculate();
                        }
                        
                        try {
                                Thread.sleep(10);
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                }
        }
        
        private void processCommand(Object o) {
                String s=o.toString();
                
                if (s.equals("clear")) {
                        engine.clear();
                        canvas.repaint();
                }
                
                if (s.equals("randomrules")) {
                        engine.setupRandomRules();
                }
                
                if (s.equals("randomfill")) {
                        engine.fillRandomBinary();
                }
                
                if (s.equals("randompoints")) {
                        engine.scatterRandomPoints();
                }
                
                if (s.equals("pause")) {
                        running=!running;
                }       
                
                if (s.startsWith("zoom")) {
                        scale=Integer.parseInt(s.substring(5));
                }
        }
        
        private void handleKeyPress() {
                if (keyPress=='c') {
                        command("clear");
                }
                
                if (keyPress=='r') {
                        command("randomrules");
                }
                
                if (keyPress=='f') {
                        command("randomfill");
                }
                
                if (keyPress==' ') {
                        command("randompoints");
                }
                
                if (keyPress=='p') {
                        command("pause");
                }
                
                if ((keyPress>='0')&&(keyPress<='9')) {
                        drawColour=(byte)(keyPress-'0');
                }
                
                if ((mouseDown)&&(mouseX<256)&&(mouseY<256)) {
                        engine.setCell(mouseX, mouseY, drawColour);
                }

                
                keyPress='`';
        }

        BufferedImage bi=null;
        int[] disp=new int[65536];

        private void display() {
                if (bi==null) {
                        bi=new BufferedImage(256,256,BufferedImage.TYPE_INT_ARGB);
                }
                
                for (int i=0; i<65536; i++) {
                        disp[i]=engine.data.getGrad()[engine.values[i]&255];
                }
                
                bi.setRGB(0, 0, 256, 256, disp, 0, 256);
                
                Graphics g=canvas.getGraphics();
                g.drawImage(bi,0,0,256*scale,256*scale,null);
        }

        int iteration=0;
        
        public void status() {
                int pop=0;
                for (int i=0; i<engine.values.length; i++) {
                        if (engine.values[i]!=0) pop++;
                }
                
                System.out.println("Iteration "+(iteration++)+" : population="+pop);
        }
}