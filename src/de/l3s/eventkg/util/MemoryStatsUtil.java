package de.l3s.eventkg.util;

import java.text.NumberFormat;

public class MemoryStatsUtil {

	public static void printMemoryStats() {
		Runtime runtime = Runtime.getRuntime();

		NumberFormat format = NumberFormat.getInstance();

		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();

		System.out.println("	--- " + TimeTransformer.getTime());
		System.out.println("	free memory: " + format.format(freeMemory / 1024));
		System.out.println("	allocated memory: " + format.format(allocatedMemory / 1024));
		System.out.println("	max memory: " + format.format(maxMemory / 1024));
		System.out
				.println("	total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024));
		System.out.println("	---");
	}

}
