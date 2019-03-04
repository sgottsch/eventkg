package de.l3s.eventkg.integration.model;

public enum DateGranularity {

	YEAR,
	MONTH,
	DAY,
	DECADE,
	CENTURY;

	public boolean isLessExactThan(DateGranularity g) {
		if (this == MONTH && (g == DAY))
			return true;
		else if (this == YEAR && (g == DAY || g == MONTH))
			return true;
		else if (this == DECADE && (g == DAY || g == MONTH || g == YEAR))
			return true;
		else if (this == CENTURY && (g == DAY || g == MONTH || g == YEAR || g == DECADE))
			return true;
		return false;
	}
}
