package mikera.life;

import mikera.image.Gradient;

public final class Rules {
	public static final int MAX_VALUE=256;
	public static final int MAX_COLOUR=256;
	
	private int[] colours;
	private byte[] transitions;
	private byte[] effectValues;

	public Rules() {
		this.colours = new int[MAX_COLOUR];
		this.transitions = new byte[MAX_VALUE*MAX_VALUE];
		this.effectValues = new byte[MAX_VALUE];
		setupDefaultColours();
	}

	public int[] getColours() {
		return colours;
	}

	public void setColours(int[] grad) {
		this.colours = grad;
	}

	public byte[] getTransitions() {
		return transitions;
	}

	public void setTransitions(byte[] transitions) {
		this.transitions = transitions;
	}

	public byte[] getEffectValues() {
		return effectValues;
	}

	public void setEffectValues(byte[] colourValues) {
		this.effectValues = colourValues;
	}
	
	public void setTransitions(int i, int[] ts) {
		for (int x = 0; x < ts.length; x++) {
			getTransitions()[256 * i + x] = (byte) ts[x];
		}
	}

	public void setAllTransitions(int i, int ts) {
		for (int x = 0; x < 256; x++) {
			getTransitions()[256 * i + x] = (byte) ts;
		}
	}
	
	void setupDefaultColours() {
		getColours()[0] = 0xFF000000;
		getColours()[1] = 0xFFFF0000;
		getColours()[2] = 0xFFFF8000;
		getColours()[3] = 0xFFFFFF00;
		getColours()[4] = 0xFF808000;

		Gradient.fillLinearGradient(getColours(), 5, 0xFF404040, 255,
				0xFFFFFFFF);
	}
}