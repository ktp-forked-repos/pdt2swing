package pdt.prolog.elements;

public class PrologArgument {

	public static final int ID = 0;
	public static final int ATOM = 1;
	public static final int ATOM_FIXED = 2;
	public static final int NUMBER = 3;
	public static final int BOOLEAN = 4;
	public static final int NUMBER_LIMIT = 5;
	public static final int REFERENCE = 6;
	public static final int DATE = 7;
	
	private String name;
	private int type;

	// Factory methods
	
	public static PrologArgument createId() {
		return new PrologArgument("ID", ID);
	}
	
	public static PrologArgument createAtom(String name) {
		return new PrologArgument(name, ATOM);
	}

	public static PrologArgument createNumber(String name) {
		return new PrologArgument(name, NUMBER);
	}
	
	public static PrologArgument createBoolean(String name) {
		return new PrologArgument(name, BOOLEAN);
	}

	public static PrologArgument createLimitedNumber(String name, int limitMin, int limitMax) {
		return new PrologNumberRangeArgument(name, limitMin, limitMax);
	}
	
	public static PrologArgument createLimitedNumber(String name, int limitMin, int limitMax, boolean canBeUnsure) {
		return new PrologNumberRangeArgument(name, limitMin, limitMax, canBeUnsure);
	}

	public static PrologArgument createFixedAtom(String name, String... values) {
		return new PrologFixedAtom(name, values);
	}
	
	public static PrologArgument createReference(String name, String type) {
		return new PrologReferenceType(name, type);
	}
	
	public static PrologArgument createDate(String name) {
		return new PrologArgument(name, DATE);
	}
	
	// constructor
	
	protected PrologArgument(String name, int type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}
	
}
