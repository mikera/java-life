package mikera.life;

public class Rules {
	private int[] grad;
	private byte[] transitions;
	private byte[] colourValues;

	public Rules() {
		this.grad = new int[256];
		this.transitions = new byte[256*256];
		this.colourValues = new byte[256];
	}

	public int[] getGrad() {
		return grad;
	}

	public void setGrad(int[] grad) {
		this.grad = grad;
	}

	public byte[] getTransitions() {
		return transitions;
	}

	public void setTransitions(byte[] transitions) {
		this.transitions = transitions;
	}

	public byte[] getColourValues() {
		return colourValues;
	}

	public void setColourValues(byte[] colourValues) {
		this.colourValues = colourValues;
	}
}