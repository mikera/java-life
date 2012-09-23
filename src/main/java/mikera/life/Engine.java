package mikera.life;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import mikera.image.Gradient;
import mikera.util.Rand;
import mikera.util.TextUtils;

public class Engine {
        public final int GRIDSIZE = 256;

        public int[] grad=new int[256];
        
        public byte[] colours=new byte[GRIDSIZE*GRIDSIZE];
        public byte[] totals=new byte[GRIDSIZE*GRIDSIZE];
        public byte[] ncolours=new byte[GRIDSIZE*GRIDSIZE];
        public byte[] ntotals=new byte[GRIDSIZE*GRIDSIZE];

        public byte[] transitions=new byte[256*256];
        public byte[] colourValues=new byte[256];
        
        public byte nextState(int colour, int total) {
                int i=(((colour)&255)<<8) + ((total)&255);
                return transitions[i];
        }
        
        public byte value(byte colour) {
                return colourValues[(colour)&255];
        }
        
        public void flip() {
                byte[] t=colours;
                colours=ncolours;
                ncolours=t;
                
                t=totals;
                totals=ntotals;
                ntotals=t;
                
                copyToNew();
        }
        
        public void copyToNew() {
                System.arraycopy(colours, 0, ncolours, 0, colours.length);
                System.arraycopy(totals, 0, ntotals, 0, totals.length);         
        }
        
        public void changeColour(int i, byte oldColour, byte newColour) {
                byte diff=(byte)(value(newColour)-value(oldColour));
                ncolours[i]=newColour;
                if (diff==0) return;
                ntotals[(i-257)&65535]+=diff;
                ntotals[(i-256)&65535]+=diff;
                ntotals[(i-255)&65535]+=diff;
                ntotals[(i-1)  &65535]+=diff;
                ntotals[(i+1)  &65535]+=diff;
                ntotals[(i+255)&65535]+=diff;
                ntotals[(i+256)&65535]+=diff;
                ntotals[(i+257)&65535]+=diff;
        }
        
        public void calculate() {
                for (int i=0; i<colours.length; i++) {
                        byte c=colours[i];
                        byte tot=totals[i];
                        byte nc=nextState(c,tot);
                        if (c!=nc) {
                                changeColour(i,c,nc);
                        }
                }
                
                flip();
        }
        
        public void setCell(int x, int y, byte c) {
                int i=(x&255)+((y&255)<<8);
                byte oc=colours[i];
                changeColour(i,oc,c);
        }
        
        public void setup() {
                clear();
                setupMyRules();
                fillRandomBinary();
                setupDefaultGradient();
                flip();
        }
        
        void clear() {
                Arrays.fill(ncolours, (byte)0);
                Arrays.fill(ntotals, (byte)0);
                flip();
        }
        
        private void clearRules() {
                Arrays.fill(transitions, (byte)0);
                Arrays.fill(colourValues, (byte)0);
        }
        
        void setupLifeRules() {
                colourValues[1]=1;
                setTransitions(0, new int[] {0,0,0,1});
                setTransitions(1, new int[] {0,0,1,1});         
        }

        
        void setupMyRules() {
                colourValues[1]=1;
                colourValues[2]=2;
                colourValues[3]=3;
                colourValues[4]=-1;
                setTransitions(0, new int[] {0,0,0,1,0,2,0,0,0,0,0,0,0,0,0,0});
                setTransitions(1, new int[] {0,0,1,1,0,0,2,0,0,0,0,0,0,0,0,0});
                setTransitions(2, new int[] {0,0,1,3,0,4,2,1,1,0,0,0,0,0,0,0});
                setTransitions(3, new int[] {0,4,0,4,2,4,4,4,1,4,0,4,0,4,0,4});
                setAllTransitions(4, 5);
        }
        
        private void setTransitions(int i, int[] ts) {
                for (int x=0; x<ts.length; x++) {
                        transitions[256*i+x]=(byte)ts[x];
                }
        }
        
        private void setAllTransitions(int i, int ts) {
                for (int x=0; x<256; x++) {
                        transitions[256*i+x]=(byte)ts;
                }
        }
        
        public void fillRandomBinary() {
                for (int i=0; i<ncolours.length; i++) {
                        changeColour(i,ncolours[i],(byte)Rand.r(2));
                }
        }
        
        public void scatterRandomPoints() {
                for (int i=0; i<300; i++) {
                        int x=Rand.r(65536);
                        changeColour(x,ncolours[x],Rand.nextByte());
                }
        }

        void setupRandomRules() {
                clearRules();
                colourValues[0]=0;
                for (int i=1; i<256; i++) {
                        colourValues[i]=Rand.nextByte();
                }
                
                for (int c=0; c<256; c++) {
                        transitions[c*256+0]=0;
                        for (int t=1; t<256; t++) {
                                byte nc=Rand.nextByte();
                                if (Rand.d(2)==1) nc=0;
                                transitions[c*256+t]=nc;
                        }
                }
                
        }
        
        public void saveRules(OutputStream os) {
                try {
                        PrintStream ps=new PrintStream(os);
                        for (int i=0; i<256; i++) {
                                ps.println(lineString(i));
                        }
                } catch (Exception e) {
                        throw new Error(e);
                }
        }
        
        private int countUsedTransitions(int i) {
                int r=0;
                for (int x=0; x<256; x++) {
                        if (transitions[256*i+x]!=0) r=x+1;
                }
                return r;
        }

        
        private String lineString(int i) {
                StringBuffer sb=new StringBuffer();
                sb.append(TextUtils.leftPad(i,3));
                sb.append(":");
                sb.append(Integer.toHexString(grad[i]));
                sb.append(":");
                sb.append(TextUtils.leftPad(colourValues[i],4));
                sb.append(":");
                int tv=countUsedTransitions(i);
                for (int x=0; x<tv; x++) {
                        sb.append(TextUtils.leftPad(transitions[256*i+x]&255,3));
                        if (x<(tv-1)) sb.append(",");
                }
                return sb.toString();
        }
        
        void setupDefaultGradient() {
                
                
                grad[0]=0xFF000000;
                grad[1]=0xFFFF0000;
                grad[2]=0xFFFF8000;
                grad[3]=0xFFFFFF00;
                grad[4]=0xFF808000;

                Gradient.fillLinearGradient(grad, 5, 0xFF404040, 255, 0xFFFFFFFF);
        }
}