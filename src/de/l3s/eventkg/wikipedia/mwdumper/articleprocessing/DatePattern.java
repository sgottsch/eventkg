package de.l3s.eventkg.wikipedia.mwdumper.articleprocessing;

import java.util.regex.Pattern;

public class DatePattern {

	private Pattern pattern;

	private boolean d1;
	private boolean d2;
	private boolean m1;
	private boolean m2;
	private boolean y1;

	public DatePattern(Pattern pattern, boolean d1, boolean d2, boolean m1, boolean m2) {
		super();
		this.pattern = pattern;
		this.d1 = d1;
		this.d2 = d2;
		this.m1 = m1;
		this.m2 = m2;
		this.y1 = false;
	}

	public DatePattern(Pattern pattern, boolean d1, boolean d2, boolean m1, boolean m2, boolean y1) {
		super();
		this.pattern = pattern;
		this.d1 = d1;
		this.d2 = d2;
		this.m1 = m1;
		this.m2 = m2;
		this.y1 = y1;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	public boolean hasD1() {
		return d1;
	}

	public void setD1(boolean d1) {
		this.d1 = d1;
	}

	public boolean hasD2() {
		return d2;
	}

	public void setD2(boolean d2) {
		this.d2 = d2;
	}

	public boolean hasM1() {
		return m1;
	}

	public void setM1(boolean m1) {
		this.m1 = m1;
	}

	public boolean hasM2() {
		return m2;
	}

	public void setM2(boolean m2) {
		this.m2 = m2;
	}

	public boolean hasY1() {
		return y1;
	}

	public void setY1(boolean y1) {
		this.y1 = y1;
	}

}
