package org.snpeff.snpEffect.testCases.unity;

import java.util.Random;

import org.junit.Test;
import org.snpeff.stats.IntStats;
import org.snpeff.util.Gpr;
import org.snpeff.util.Log;

import junit.framework.Assert;

public class TestCasesIntStats extends TestCasesBase {

	public static double EPSILON = 0.000001;

	public TestCasesIntStats() {
		super();
		initRand();
	}

	@Override
	protected void initRand() {
		rand = new Random(20110328);
	}

	/**
	 * Calculate statistics, compare results with other programs' results
	 */
	@Test
	public void test_01() {
		Log.debug("Test");
		String fileName = path("intStats_test_01.txt");
		String file = Gpr.readFile(fileName);
		String lines[] = file.split("\n");

		for (String line : lines) {
			String recs[] = line.split("\t");

			// Get stats results
			int min = (int) Gpr.parseDoubleSafe(recs[0]);
			int max = (int) Gpr.parseDoubleSafe(recs[1]);
			double mean = Gpr.parseDoubleSafe(recs[2]);
			double median = Gpr.parseDoubleSafe(recs[3]);
			double std = Gpr.parseDoubleSafe(recs[4]);

			// Perform stats using the rest of the data
			IntStats intStats = new IntStats();
			for (int i = 5; i < recs.length; i++) {
				int value = Gpr.parseIntSafe(recs[i]);
				intStats.sample(value);
			}

			// Check that sats are OK
			Assert.assertEquals(min, intStats.getMin());
			Assert.assertEquals(max, intStats.getMax());
			Assert.assertEquals(mean, intStats.getMean(), EPSILON);
			Assert.assertEquals(median, intStats.getMedian(), EPSILON);
			Assert.assertEquals(std, intStats.getStd(), EPSILON);
		}
	}

}
