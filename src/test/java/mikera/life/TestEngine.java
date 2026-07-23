package mikera.life;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

public class TestEngine {

	/** The eight neighbour offsets used by Engine.changeColour, applied flat with &amp; 65535. */
	private static final int[] NEIGHBOURS = { -257, -256, -255, -1, 1, 255, 256, 257 };

	/** Flat grid index: x in the low byte, y in the high byte. */
	private static int index(int x, int y) {
		return (x & 255) + ((y & 255) << 8);
	}

	private static Engine lifeEngine() {
		Engine e = new Engine();
		e.rules = RuleSets.getRules("life");
		return e;
	}

	/** Sets cells to a colour. setCell publishes each write, so repeats are safe. */
	private static void setCells(Engine e, byte colour, int[]... cells) {
		for (int[] c : cells) {
			e.setCell(c[0], c[1], colour);
		}
	}

	private static Set<Integer> liveCells(Engine e) {
		Set<Integer> live = new TreeSet<>();
		for (int i = 0; i < e.values.length; i++) {
			if (e.values[i] != 0) live.add(i);
		}
		return live;
	}

	/** Recomputes totals from scratch, mirroring the engine's flat wrapping. */
	private static byte[] bruteForceTotals(Engine e) {
		byte[] expected = new byte[e.values.length];
		for (int i = 0; i < e.values.length; i++) {
			int sum = 0;
			for (int d : NEIGHBOURS) {
				sum += e.value(e.values[(i + d) & 65535]);
			}
			expected[i] = (byte) sum;
		}
		return expected;
	}

	/**
	 * The engine's central invariant: totals[i] is the sum of the effect values of
	 * cell i's eight neighbours, maintained incrementally rather than recomputed.
	 */
	private static void assertTotalsConsistent(Engine e) {
		assertArrayEquals(bruteForceTotals(e), e.totals, "totals out of sync with values");
	}

	@Test
	public void setCellUpdatesNeighbourTotals() {
		Engine e = lifeEngine();
		setCells(e, (byte) 1, new int[] { 100, 100 });

		int i = index(100, 100);
		assertEquals((byte) 1, e.values[i]);
		assertEquals((byte) 0, e.totals[i]);
		for (int d : NEIGHBOURS) {
			assertEquals((byte) 1, e.totals[(i + d) & 65535]);
		}
		assertTotalsConsistent(e);
	}

	@Test
	public void repeatedSetCellDoesNotDoubleCount() {
		Engine e = lifeEngine();
		int i = index(100, 100);

		// Painting the same cell repeatedly is what a mouse drag does. Each write
		// must be idempotent, not accumulate into the neighbours' totals.
		for (int n = 0; n < 3; n++) {
			setCells(e, (byte) 1, new int[] { 100, 100 });
			assertEquals((byte) 1, e.values[i]);
			assertEquals((byte) 1, e.totals[(i + 1) & 65535]);
			assertTotalsConsistent(e);
		}
	}

	@Test
	public void setCellCanChangeAndClearACellBetweenSteps() {
		Engine e = new Engine();
		e.rules = RuleSets.getRules("warfare"); // state 2 has effect value -1
		int i = index(100, 100);

		setCells(e, (byte) 1, new int[] { 100, 100 });
		assertEquals((byte) 1, e.totals[(i + 1) & 65535]);
		assertTotalsConsistent(e);

		setCells(e, (byte) 2, new int[] { 100, 100 });
		assertEquals((byte) 2, e.values[i]);
		assertEquals((byte) -1, e.totals[(i + 1) & 65535]);
		assertTotalsConsistent(e);

		setCells(e, (byte) 0, new int[] { 100, 100 });
		assertEquals((byte) 0, e.values[i]);
		assertEquals((byte) 0, e.totals[(i + 1) & 65535]);
		assertTotalsConsistent(e);
	}

	@Test
	public void setCellIsVisibleWithoutStepping() {
		Engine e = lifeEngine();
		e.setCell(100, 100, (byte) 1);

		// LifePanel renders values, so a cell drawn while paused - with no
		// calculate() to flip the buffers - must already be there.
		assertEquals((byte) 1, e.values[index(100, 100)]);
		assertTotalsConsistent(e);
	}

	@Test
	public void gridWrapsAtTheEdges() {
		Engine e = lifeEngine();
		setCells(e, (byte) 1, new int[] { 0, 0 });

		// Offsets are applied flat with & 65535, so column 0 joins the previous row.
		assertEquals((byte) 1, e.totals[65535]);            // 0 - 1  -> (255,255)
		assertEquals((byte) 1, e.totals[index(0, 255)]);    // 0 - 256 -> (0,255)
		assertEquals((byte) 1, e.totals[index(1, 0)]);      // 0 + 1  -> (1,0)
		assertTotalsConsistent(e);
	}

	@Test
	public void nextStateLooksUpTransitionTable() {
		Engine e = lifeEngine();

		assertEquals((byte) 1, e.nextState(0, 3)); // birth on exactly 3
		assertEquals((byte) 0, e.nextState(0, 2));
		assertEquals((byte) 0, e.nextState(0, 4));
		assertEquals((byte) 1, e.nextState(1, 2)); // survival on 2 or 3
		assertEquals((byte) 1, e.nextState(1, 3));
		assertEquals((byte) 0, e.nextState(1, 1));
		assertEquals((byte) 0, e.nextState(1, 4));
	}

	@Test
	public void blinkerOscillatesWithPeriodTwo() {
		Engine e = lifeEngine();
		setCells(e, (byte) 1, new int[] { 99, 100 }, new int[] { 100, 100 }, new int[] { 101, 100 });
		Set<Integer> horizontal = liveCells(e);

		e.calculate();
		assertEquals(Set.of(index(100, 99), index(100, 100), index(100, 101)), liveCells(e));
		assertTotalsConsistent(e);

		e.calculate();
		assertEquals(horizontal, liveCells(e));
		assertTotalsConsistent(e);
	}

	@Test
	public void blockIsStable() {
		Engine e = lifeEngine();
		setCells(e, (byte) 1, new int[] { 100, 100 }, new int[] { 101, 100 },
				new int[] { 100, 101 }, new int[] { 101, 101 });
		Set<Integer> block = liveCells(e);

		for (int gen = 0; gen < 4; gen++) {
			e.calculate();
			assertEquals(block, liveCells(e));
		}
		assertTotalsConsistent(e);
	}

	@Test
	public void gliderTranslatesDiagonally() {
		Engine e = lifeEngine();
		int x = 100, y = 100;
		setCells(e, (byte) 1,
				new int[] { x + 1, y },
				new int[] { x + 2, y + 1 },
				new int[] { x, y + 2 }, new int[] { x + 1, y + 2 }, new int[] { x + 2, y + 2 });

		// A glider returns to its original shape, offset by (1,1), every 4 generations.
		for (int gen = 0; gen < 4; gen++) {
			e.calculate();
		}

		assertEquals(Set.of(
				index(x + 2, y + 1),
				index(x + 3, y + 2),
				index(x + 1, y + 3), index(x + 2, y + 3), index(x + 3, y + 3)), liveCells(e));
		assertTotalsConsistent(e);
	}

	@Test
	public void briansBrainCyclesThroughStates() {
		Engine e = new Engine();
		e.rules = RuleSets.getRules("brians-brain");
		setCells(e, (byte) 1, new int[] { 100, 100 });

		int i = index(100, 100);
		e.calculate();
		assertEquals((byte) 2, e.values[i]); // alive -> dying
		assertTotalsConsistent(e);

		e.calculate();
		assertEquals((byte) 0, e.values[i]); // dying -> dead
		assertTotalsConsistent(e);
	}

	@Test
	public void clearEmptiesTheGrid() {
		Engine e = lifeEngine();
		e.fillRandomBinary();
		e.flip();

		e.clear();
		for (byte v : e.values) {
			assertEquals((byte) 0, v);
		}
		assertTotalsConsistent(e);
	}

	@Test
	public void setupProducesAConsistentLifeState() {
		Engine e = new Engine();
		e.setup();
		assertTotalsConsistent(e);

		e.calculate();
		assertTotalsConsistent(e);
	}

	@Test
	public void randomRulesPopulateTransitionTable() {
		Engine e = new Engine();
		e.setupRandomRules();
		Rules r = e.rules;

		int used = r.getUsedValues();
		assertTrue(used >= 2, "expected at least two states in use, got " + used);

		// Regression: the clear-up loop used to wipe the whole table, leaving an
		// inert ruleset. At least one transition for a used state must survive.
		boolean populated = false;
		for (int i = 0; i < used * 256; i++) {
			if (r.getTransitions()[i] != 0) {
				populated = true;
				break;
			}
		}
		assertTrue(populated, "randomised rules left the transition table empty");

		// States beyond those in use must be fully cleared.
		for (int from = used; from < Rules.MAX_VALUE; from++) {
			assertEquals((byte) 0, r.getEffectValues()[from]);
			for (int total = 0; total < 256; total++) {
				assertEquals((byte) 0, r.getTransitions()[from * 256 + total]);
			}
		}
	}

	@Test
	public void ruleSetsExposeAllBuiltIns() {
		for (String name : new String[] { "life", "mikera-1", "warfare", "brians-brain" }) {
			assertNotNull(RuleSets.getRules(name), name);
		}
		assertThrows(Error.class, () -> RuleSets.getRules("no-such-ruleset"));
	}

	/** Guards against the menu catalogue drifting away from the getRules switch. */
	@Test
	public void everyCatalogueEntryResolves() {
		for (String[][] catalogue : new String[][][] {
				RuleSets.LIFE_LIKE, RuleSets.GENERATIONS, RuleSets.WEIGHTED }) {
			for (String[] entry : catalogue) {
				Rules rules = RuleSets.getRules(entry[1]);
				assertNotNull(rules, entry[1]);
				assertTrue(rules.getUsedValues() >= 2,
						entry[1] + " should use at least two states");
			}
		}
	}

	/**
	 * Runs every catalogue ruleset from a dense random soup. A typo in a B/S
	 * string usually shows up as a rule that annihilates itself immediately, which
	 * the table-shape assertions above would not catch.
	 */
	@Test
	public void everyRulesetSustainsActivity() {
		for (String[][] catalogue : new String[][][] {
				RuleSets.LIFE_LIKE, RuleSets.GENERATIONS, RuleSets.WEIGHTED }) {
			for (String[] entry : catalogue) {
				Engine e = new Engine();
				e.rules = RuleSets.getRules(entry[1]);
				e.fillRandomBinary();
				e.flip();

				for (int gen = 0; gen < 20; gen++) {
					e.calculate();
				}

				int population = 0;
				for (byte v : e.values) {
					if (v != 0) population++;
				}
				assertTrue(population > 0, entry[1] + " died out within 20 generations");
			}
		}
	}

	@Test
	public void lifeLikeEncodesBirthAndSurvivalCounts() {
		Rules highLife = RuleSets.getRules("highlife"); // B36/S23

		for (int n = 0; n <= 8; n++) {
			assertEquals((byte) (n == 3 || n == 6 ? 1 : 0), highLife.getTransitions()[n],
					"birth on " + n);
			assertEquals((byte) (n == 2 || n == 3 ? 1 : 0), highLife.getTransitions()[256 + n],
					"survival on " + n);
		}
	}

	@Test
	public void replicatorTurnsOneCellIntoARing() {
		Engine e = new Engine();
		e.rules = RuleSets.getRules("replicator"); // B1357/S1357
		setCells(e, (byte) 1, new int[] { 100, 100 });

		// The centre has no live neighbours, so it dies; each of the eight
		// surrounding cells sees exactly one, so all eight are born.
		e.calculate();

		assertEquals(Set.of(
				index(99, 99), index(100, 99), index(101, 99),
				index(99, 100), index(101, 100),
				index(99, 101), index(100, 101), index(101, 101)), liveCells(e));
		assertTotalsConsistent(e);
	}

	@Test
	public void generationsAgeThroughRefractoryStates() {
		Rules starWars = RuleSets.getRules("star-wars"); // 345/2/4
		assertEquals(4, starWars.getUsedValues());

		// A live cell that fails to survive enters the ageing chain 2 -> 3 -> 0,
		// and ageing states advance regardless of their neighbours.
		assertEquals((byte) 2, starWars.getTransitions()[256 + 1]);
		assertEquals((byte) 1, starWars.getTransitions()[256 + 3]);
		for (int n = 0; n <= 8; n++) {
			assertEquals((byte) 3, starWars.getTransitions()[512 + n]);
			assertEquals((byte) 0, starWars.getTransitions()[768 + n]);
		}

		// Only the live state is visible to neighbours.
		assertEquals((byte) 1, starWars.getEffectValues()[1]);
		assertEquals((byte) 0, starWars.getEffectValues()[2]);
		assertEquals((byte) 0, starWars.getEffectValues()[3]);
	}

	@Test
	public void weightedRulesTellTheTwoSpeciesApart() {
		// Species A weighs 1 and B weighs 16, so three of one is a different
		// total from three of the other - something B/S notation cannot express.
		Engine a = new Engine();
		a.rules = RuleSets.getRules("rival");
		setCells(a, (byte) 1, new int[] { 99, 99 }, new int[] { 100, 99 }, new int[] { 101, 99 });
		a.calculate();
		assertEquals((byte) 1, a.values[index(100, 100)]);
		assertTotalsConsistent(a);

		Engine b = new Engine();
		b.rules = RuleSets.getRules("rival");
		setCells(b, (byte) 2, new int[] { 99, 99 }, new int[] { 100, 99 }, new int[] { 101, 99 });
		b.calculate();
		assertEquals((byte) 2, b.values[index(100, 100)]);
		assertTotalsConsistent(b);

		// A mixed neighbourhood gives neither species a claim.
		Engine mixed = new Engine();
		mixed.rules = RuleSets.getRules("rival");
		mixed.setCell(99, 99, (byte) 1);
		mixed.setCell(100, 99, (byte) 1);
		mixed.setCell(101, 99, (byte) 2);
		mixed.calculate();
		assertEquals((byte) 0, mixed.values[index(100, 100)]);
		assertTotalsConsistent(mixed);
	}
}
