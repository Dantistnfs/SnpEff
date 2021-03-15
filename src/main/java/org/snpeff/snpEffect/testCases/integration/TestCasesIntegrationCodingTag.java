package org.snpeff.snpEffect.testCases.integration;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.snpeff.SnpEff;
import org.snpeff.snpEffect.VariantEffect.EffectImpact;
import org.snpeff.snpEffect.commandLine.SnpEffCmdEff;
import org.snpeff.util.Log;
import org.snpeff.vcf.VcfEffect;
import org.snpeff.vcf.VcfEntry;

/**
 * Test case: Make sure VCF entries have some 'coding' (transcript biotype), even
 * when biotype info is not available (e.g. hg19), and we infer it
 * from 'isProteinCoding()'
 *
 * @author pcingola
 */
public class TestCasesIntegrationCodingTag extends TestCasesIntegrationBase {

	public TestCasesIntegrationCodingTag() {
		super();
	}

	@Test
	public void test_01() {
		Log.debug("Test");
		String args[] = { "-classic", "-ud", "0", "-noOut", "testHg19Chr1", path("missing_coding_tr_tag.vcf") };

		// Run snpeff
		SnpEff cmd = new SnpEff(args);
		SnpEffCmdEff cmdEff = (SnpEffCmdEff) cmd.cmd();
		cmdEff.setVerbose(verbose);
		cmdEff.setSupressOutput(!verbose);
		List<VcfEntry> vcfEntries = cmdEff.run(true);
		Assert.assertTrue("Errors while executing SnpEff", cmdEff.getTotalErrs() <= 0);

		// Make sure transcript coding tags are there
		for (VcfEntry ve : vcfEntries) {
			if (verbose) Log.info(ve.getChromosomeName() + "\t" + ve.getStart() + "\t" + ve.getInfoStr());

			for (VcfEffect veff : ve.getVcfEffects()) {
				if (veff.getImpact() == EffectImpact.MODERATE) {
					if (verbose) Log.info("\t" + veff);
					Assert.assertFalse(veff.getBioType() == null || (veff.getBioType() == null)); // Make sure the biotype field is avaialble
				}
			}
		}
	}
}
