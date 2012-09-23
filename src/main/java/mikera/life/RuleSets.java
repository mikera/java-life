package mikera.life;

public class RuleSets {
	public static Rules getRules(String name) {
		switch (name) {
		case "life": return setupLifeRules();
		case "mikera-1": return setupMyRules();
		case "warfare": return setupWarfareRules();
		case "brians-brain": return setupBriansBrainRules();
		}
		
		
		throw new Error("Ruleset does not exist!");
	}
	

	static Rules setupLifeRules() {
		Rules rules=new Rules();
		rules.setUsedValues(2);
		rules.getEffectValues()[1] = 1;
		rules.setTransitions(0, new int[] { 0, 0, 0, 1 });
		rules.setTransitions(1, new int[] { 0, 0, 1, 1 });
		return rules;
	}
	
	static Rules setupBriansBrainRules() {
		Rules rules=new Rules();
		rules.setUsedValues(3);
		rules.getEffectValues()[1] = 1;
		rules.setTransitions(0, new int[] { 0, 0, 1, 0 });
		rules.setAllTransitions(1, 2);
		rules.setAllTransitions(2, 0);
		rules.getColours()[1] = 0xFFFFFFFF;
		rules.getColours()[2] = 0xFF0000FF;

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
