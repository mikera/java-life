package mikera.life;

/**
 * Built-in rulesets.
 *
 * The engine looks up transitions[state][sum of neighbour effect values], which
 * is exactly the class of outer-totalistic automata. Two standard notations map
 * onto it directly:
 *
 *  - Life-like "B/S": two states, live cells weigh 1. Born on the neighbour
 *    counts in B, survive on those in S. Conway's Life is B3/S23.
 *  - Generations "S/B/C": as above, but a cell leaving the live state ages
 *    through C-2 refractory states before dying. Only the live state weighs 1,
 *    so ageing cells are invisible to their neighbours. Brian's Brain is /2/3.
 *
 * Weights make the engine strictly more expressive than either notation - see
 * setupRivalRules for a rule that needs them.
 */
public class RuleSets {

	/** Two-state Life-like rules, as {menu label, ruleset name}. */
	public static final String[][] LIFE_LIKE = {
			{ "Classic Game Of Life (B3/S23)", "life" },
			{ "HighLife - replicators (B36/S23)", "highlife" },
			{ "Day & Night (B3678/S34678)", "daynight" },
			{ "Seeds - explosive (B2/S)", "seeds" },
			{ "Maze (B3/S12345)", "maze" },
			{ "Mazectric - long corridors (B3/S1234)", "mazectric" },
			{ "Coral (B3/S45678)", "coral" },
			{ "Diamoeba - blobs (B35678/S5678)", "diamoeba" },
			{ "Replicator - everything copies (B1357/S1357)", "replicator" },
			{ "Anneal - majority vote (B4678/S35678)", "anneal" },
			{ "Life Without Death (B3/S012345678)", "lwod" },
			{ "Move - slow spaceships (B368/S245)", "move" },
			{ "Gnarl (B1/S1)", "gnarl" },
			{ "Assimilation (B345/S4567)", "assimilation" },
	};

	/** Multi-state Generations rules, as {menu label, ruleset name}. */
	public static final String[][] GENERATIONS = {
			{ "Brian's Brain (/2/3)", "brians-brain" },
			{ "Star Wars (345/2/4)", "star-wars" },
			{ "Fireworks (2/13/21)", "fireworks" },
			{ "Frogs (12/34/3)", "frogs" },
			{ "Lava (12345/45678/8)", "lava" },
			{ "Prairie On Fire (345/34/6)", "prairie-on-fire" },
			{ "Burst (0235678/3468/9)", "burst" },
			{ "Rake (3467/2678/6)", "rake" },
			{ "Caterpillars (124567/378/4)", "caterpillars" },
			{ "Bloomerang (234/34678/24)", "bloomerang" },
	};

	/** Rules that rely on weighted effect values, as {menu label, ruleset name}. */
	public static final String[][] WEIGHTED = {
			{ "Magic Mike", "mikera-1" },
			{ "Warfare", "warfare" },
			{ "Rival Colonies", "rival" },
	};

	public static Rules getRules(String name) {
		switch (name) {
		// Life-like
		case "life": return lifeLike("3", "23");
		case "highlife": return lifeLike("36", "23");
		case "daynight": return lifeLike("3678", "34678");
		case "seeds": return lifeLike("2", "");
		case "maze": return lifeLike("3", "12345");
		case "mazectric": return lifeLike("3", "1234");
		case "coral": return lifeLike("3", "45678");
		case "diamoeba": return lifeLike("35678", "5678");
		case "replicator": return lifeLike("1357", "1357");
		case "anneal": return lifeLike("4678", "35678");
		case "lwod": return lifeLike("3", "012345678");
		case "move": return lifeLike("368", "245");
		case "gnarl": return lifeLike("1", "1");
		case "assimilation": return lifeLike("345", "4567");

		// Generations
		case "brians-brain": return setupBriansBrainRules();
		case "star-wars": return generations("345", "2", 4);
		case "fireworks": return generations("2", "13", 21);
		case "frogs": return generations("12", "34", 3);
		case "lava": return generations("12345", "45678", 8);
		case "prairie-on-fire": return generations("345", "34", 6);
		case "burst": return generations("0235678", "3468", 9);
		case "rake": return generations("3467", "2678", 6);
		case "caterpillars": return generations("124567", "378", 4);
		case "bloomerang": return generations("234", "34678", 24);

		// Weighted
		case "mikera-1": return setupMyRules();
		case "warfare": return setupWarfareRules();
		case "rival": return setupRivalRules();
		}

		throw new Error("Ruleset does not exist!");
	}

	/**
	 * Builds a two-state Life-like ruleset from B/S notation, where each string
	 * holds the live neighbour counts that trigger birth or survival. Conway's
	 * Life is lifeLike("3", "23").
	 */
	static Rules lifeLike(String born, String survives) {
		Rules rules = new Rules();
		rules.setUsedValues(2);
		rules.getEffectValues()[1] = 1;

		for (int n = 0; n <= 8; n++) {
			rules.getTransitions()[n] = (byte) (born.indexOf('0' + n) >= 0 ? 1 : 0);
			rules.getTransitions()[256 + n] = (byte) (survives.indexOf('0' + n) >= 0 ? 1 : 0);
		}
		return rules;
	}

	/**
	 * Builds a Generations ruleset from S/B/C notation. A live cell that fails to
	 * survive ages through states 2..(states-1) before returning to 0. Only state
	 * 1 has a non-zero effect value, so ageing cells do not count as neighbours.
	 */
	static Rules generations(String survives, String born, int states) {
		Rules rules = new Rules();
		rules.setUsedValues(states);
		rules.getEffectValues()[1] = 1;

		for (int n = 0; n <= 8; n++) {
			rules.getTransitions()[n] = (byte) (born.indexOf('0' + n) >= 0 ? 1 : 0);
			rules.getTransitions()[256 + n] = (byte) (survives.indexOf('0' + n) >= 0 ? 1 : (2 % states));
		}

		// Ageing chain: each refractory state advances, and the last one dies.
		for (int s = 2; s < states; s++) {
			rules.setAllTransitions(s, (s + 1) % states);
		}

		fadeColours(rules, states, 0xFFFFFFFF, 0xFFFF6000, 0xFF200030);
		return rules;
	}

	/** Live cells bright, then a fade across the refractory states. */
	private static void fadeColours(Rules rules, int states, int live, int from, int to) {
		rules.getColours()[1] = live;
		int ageing = states - 2;
		for (int s = 2; s < states; s++) {
			double f = (ageing <= 1) ? 0.0 : (double) (s - 2) / (ageing - 1);
			rules.getColours()[s] = blend(from, to, f);
		}
	}

	private static int blend(int a, int b, double f) {
		int r = (int) (((a >> 16) & 255) * (1 - f) + ((b >> 16) & 255) * f);
		int g = (int) (((a >> 8) & 255) * (1 - f) + ((b >> 8) & 255) * f);
		int bl = (int) ((a & 255) * (1 - f) + (b & 255) * f);
		return 0xFF000000 | (r << 16) | (g << 8) | bl;
	}

	static Rules setupBriansBrainRules() {
		Rules rules = generations("", "2", 3);
		rules.getColours()[1] = 0xFFFFFFFF;
		rules.getColours()[2] = 0xFF0000FF;
		return rules;
	}

	/**
	 * Two competing species, which plain B/S notation cannot express: it can only
	 * see a single neighbour count, whereas this rule needs both.
	 *
	 * Species A weighs 1 and species B weighs 16, so a total of a + 16b encodes
	 * the two counts without collision - neither can exceed 8, so the low nibble
	 * never carries into the high one. The maximum total is 8 + 128 = 136, well
	 * inside the 256-entry transition table.
	 *
	 * Each species then follows Life's own birth and survival counts, but loses a
	 * cell wherever it is outnumbered locally.
	 */
	static Rules setupRivalRules() {
		Rules rules = new Rules();
		rules.setUsedValues(3);
		rules.getEffectValues()[1] = 1;
		rules.getEffectValues()[2] = 16;

		for (int a = 0; a <= 8; a++) {
			for (int b = 0; a + b <= 8; b++) {
				int total = a + 16 * b;
				int live = a + b;

				// An empty cell goes to whichever species has exactly 3 neighbours;
				// if both do, the claim is contested and it stays empty.
				byte birth = 0;
				if (a == 3 && b != 3) birth = 1;
				if (b == 3 && a != 3) birth = 2;
				rules.getTransitions()[total] = birth;

				boolean lives = (live == 2) || (live == 3);
				rules.getTransitions()[256 + total] = (byte) (lives && a >= b ? 1 : 0);
				rules.getTransitions()[512 + total] = (byte) (lives && b >= a ? 2 : 0);
			}
		}

		rules.getColours()[1] = 0xFF30A0FF;
		rules.getColours()[2] = 0xFFFF7000;
		return rules;
	}

	static Rules setupWarfareRules() {
		Rules rules=new Rules();
		rules.setUsedValues(3);
		rules.getEffectValues()[1] = 1;
		rules.getEffectValues()[2] = -1;
		rules.setTransitions(0, new int[] { 0, 0, 0, 1 });
		rules.setTransitions(1, new int[] { 0, 0, 1, 1 });
		rules.getTransitions()[0*256+253]=2;
		rules.getTransitions()[2*256+254]=2;
		rules.getTransitions()[2*256+253]=2;

		rules.getColours()[1] = 0xFFFF8000;
		rules.getColours()[2] = 0xFF00FF00;
		return rules;
	}

	static Rules setupMyRules() {
		Rules rules=new Rules();
		rules.setUsedValues(5);
		rules.getEffectValues()[1] = 1;
		rules.getEffectValues()[2] = 2;
		rules.getEffectValues()[3] = 3;
		rules.getEffectValues()[4] = -1;
		rules.setTransitions(0, new int[] { 0, 0, 0, 1, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0 });
		rules.setTransitions(1, new int[] { 0, 0, 1, 1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0,
				0, 0 });
		rules.setTransitions(2, new int[] { 0, 0, 1, 3, 0, 4, 2, 1, 1, 0, 0, 0, 0, 0,
				0, 0 });
		rules.setTransitions(3, new int[] { 0, 4, 0, 4, 2, 4, 4, 4, 1, 4, 0, 4, 0, 4,
				0, 4 });
		rules.setAllTransitions(4, 5);
		return rules;
	}
}
