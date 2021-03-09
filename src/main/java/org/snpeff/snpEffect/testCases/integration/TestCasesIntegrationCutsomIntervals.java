package org.snpeff.snpEffect.testCases.integration;

import java.util.List;

import org.junit.Test;
import org.snpeff.SnpEff;
import org.snpeff.snpEffect.commandLine.SnpEffCmdEff;
import org.snpeff.util.Log;
import org.snpeff.vcf.VcfEntry;

import junit.framework.Assert;

/**
 * Test Loss of Function prediction
 *
 * @author pcingola
 */
public class TestCasesIntegrationCutsomIntervals extends TestCasesIntegrationBase {

	public static final int NUM_DEL_TEST = 10; // number of random test per transcript

	public TestCasesIntegrationCutsomIntervals() {
		super();
	}

	@Test
	public void test_01() {
		Log.debug("Test");

		// Load database
		String[] args = { "-classic"//
				, "-interval"//
				, path("custom_intervals_01.gff") //
				, "-ud" //
				, "0" //
				, "testHg3770Chr22"//
				, path("custom_intervals_01.vcf") //
		};

		SnpEff cmd = new SnpEff(args);
		SnpEffCmdEff cmdEff = (SnpEffCmdEff) cmd.cmd();
		cmdEff.setVerbose(verbose);
		cmdEff.setSupressOutput(!verbose);

		// Run
		List<VcfEntry> vcfEntries = cmdEff.run(true);

		// Check propper annotations
		VcfEntry ve = vcfEntries.get(0);
		if (verbose) Log.debug("VCF entry: " + ve);
		Assert.assertEquals("R02837:N/A", ve.getInfo("custom_intervals_01_type"));
		Assert.assertEquals("TRANSFAC_site", ve.getInfo("custom_intervals_01_source"));
		Assert.assertEquals("R02837", ve.getInfo("custom_intervals_01_siteAcc"));
	}
}
