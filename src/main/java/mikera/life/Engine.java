package mikera.life;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import mikera.util.Rand;
import mikera.util.TextUtils;

public class Engine {
	public final int GRIDSIZE = 256;

	public byte[] values = new byte[GRIDSIZE * GRIDSIZE];
	public byte[] totals = new byte[GRIDSIZE * GRIDSIZE];
	public byte[] nvalues = new byte[GRIDSIZE * GRIDSIZE];
	public byte[] ntotals = new byte[GRIDSIZE * GRIDSIZE];

	public Rules rules = new Rules();

	public byte nextState(int colour, int total) {
		int i = (((colour) & 255) << 8) + ((total) & 255);
		return rules.getTransitions()[i];
	}

	public byte value(byte colour) {
		return rules.getEffectValues()[(colour) & 255];
	}

	public void flip() {
		byte[] t = values;
		values = nvalues;
		nvalues = t;

		t = totals;
		totals = ntotals;
		ntotals = t;

		copyToNew();
	}

	public void copyToNew() {
		System.arraycopy(values, 0, nvalues, 0, values.length);
		System.arraycopy(totals, 0, ntotals, 0, totals.length);
	}

	private void changeColour(int i, byte oldColour, byte newColour) {
		byte diff = (byte) (value(newColour) - value(oldColour));
		nvalues[i] = newColour;
		if (diff == 0)
			return;
		ntotals[(i - 257) & 65535] += diff;
		ntotals[(i - 256) & 65535] += diff;
		ntotals[(i - 255) & 65535] += diff;
		ntotals[(i - 1) & 65535] += diff;
		ntotals[(i + 1) & 65535] += diff;
		ntotals[(i + 255) & 65535] += diff;
		ntotals[(i + 256) & 65535] += diff;
		ntotals[(i + 257) & 65535] += diff;
	}

	public synchronized void calculate() {
		for (int i = 0; i < values.length; i++) {
			byte c = values[i];
			byte tot = totals[i];
			byte nc = nextState(c, tot);
			if (c != nc) {
				changeColour(i, c, nc);
			}
		}

		flip();
	}

	public synchronized void setCell(int x, int y, byte c) {
		int i = (x & 255) + ((y & 255) << 8);
		byte oc = values[i];
		changeColour(i, oc, c);
	}

	public void setup() {
		rules=RuleSets.getRules("life");
		fillRandomBinary();
		flip();
	}

	synchronized void clear() {
		Arrays.fill(nvalues, (byte) 0);
		Arrays.fill(ntotals, (byte) 0);
		flip();
	}

	private void clearRules() {
		rules=new Rules();
	}


	public void fillRandomBinary() {
		for (int i = 0; i < nvalues.length; i++) {
			changeColour(i, nvalues[i], (byte) Rand.r(2));
		}
	}
	
	public void fillRandom() {
		int usedValues=rules.getUsedValues();
		for (int i = 0; i < nvalues.length; i++) {
			changeColour(i, nvalues[i], (byte) Rand.r(usedValues));
		}
	}

	public void scatterRandomPoints() {
		for (int i = 0; i < 300; i++) {
			int x = Rand.r(65536);
			changeColour(x, nvalues[x], Rand.nextByte());
		}
	}

	void setupRandomRules() {
		Rules newRules=new Rules();
		int coloursInUse=2+Rand.r(3)*Rand.r(3)+Rand.r(3);
		newRules.setUsedValues(coloursInUse);

		clearRules();
		newRules.getEffectValues()[0] = 0;
		for (int i = 1; i < 256; i++) {
			newRules.getEffectValues()[i] = Rand.nextByte();
		}

		for (int fromValue = 0; fromValue < coloursInUse; fromValue++) {
			newRules.getTransitions()[fromValue * 256 + 0] = 0;
			for (int t = 1; t < 256; t++) {
				byte nc = (byte)Rand.r(coloursInUse);
				if (Rand.d(2) == 1)
					nc = 0;
				newRules.getTransitions()[fromValue * 256 + t] = nc;
			}
		}
		
		// clear the rest
		for (int fromValue = coloursInUse; fromValue < Rules.MAX_VALUE; fromValue++) {
			Arrays.fill(newRules.getTransitions(),(byte)0);
			newRules.getEffectValues()[fromValue]=0;
		}

		this.rules=newRules;
	}

	public void saveRules(OutputStream os) {
		try {
			PrintStream ps = new PrintStream(os);
			for (int i = 0; i < 256; i++) {
				ps.println(lineString(i));
			}
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	private int countUsedTransitions(int i) {
		int r = 0;
		for (int x = 0; x < 256; x++) {
			if (rules.getTransitions()[256 * i + x] != 0)
				r = x + 1;
		}
		return r;
	}

	private String lineString(int i) {
		StringBuffer sb = new StringBuffer();
		sb.append(TextUtils.leftPad(i, 3));
		sb.append(":");
		sb.append(Integer.toHexString(rules.getColours()[i]));
		sb.append(":");
		sb.append(TextUtils.leftPad(rules.getEffectValues()[i], 4));
		sb.append(":");
		int tv = countUsedTransitions(i);
		for (int x = 0; x < tv; x++) {
			sb.append(TextUtils.leftPad(
					rules.getTransitions()[256 * i + x] & 255, 3));
			if (x < (tv - 1))
				sb.append(",");
		}
		return sb.toString();
	}


}