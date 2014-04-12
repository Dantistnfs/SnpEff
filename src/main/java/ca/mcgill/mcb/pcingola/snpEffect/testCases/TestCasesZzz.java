package ca.mcgill.mcb.pcingola.snpEffect.testCases;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;
import junit.framework.TestCase;
import ca.mcgill.mcb.pcingola.interval.Chromosome;
import ca.mcgill.mcb.pcingola.interval.Gene;
import ca.mcgill.mcb.pcingola.interval.Genome;
import ca.mcgill.mcb.pcingola.interval.SeqChange;
import ca.mcgill.mcb.pcingola.interval.Transcript;
import ca.mcgill.mcb.pcingola.snpEffect.ChangeEffect;
import ca.mcgill.mcb.pcingola.snpEffect.ChangeEffect.EffectType;
import ca.mcgill.mcb.pcingola.snpEffect.ChangeEffects;
import ca.mcgill.mcb.pcingola.snpEffect.Config;
import ca.mcgill.mcb.pcingola.snpEffect.SnpEffectPredictor;
import ca.mcgill.mcb.pcingola.snpEffect.commandLine.SnpEff;
import ca.mcgill.mcb.pcingola.snpEffect.commandLine.SnpEffCmdEff;
import ca.mcgill.mcb.pcingola.snpEffect.factory.SnpEffPredictorFactoryRand;
import ca.mcgill.mcb.pcingola.util.Gpr;
import ca.mcgill.mcb.pcingola.util.GprSeq;
import ca.mcgill.mcb.pcingola.vcf.VcfEffect;
import ca.mcgill.mcb.pcingola.vcf.VcfEntry;

/**
 * 
 * Test case
 * 
 * @author pcingola
 */
public class TestCasesZzz extends TestCase {

	boolean debug = false;
	boolean verbose = false || debug;

	Random rand;
	Config config;
	Genome genome;
	Chromosome chromosome;
	Gene gene;
	Transcript transcript;
	SnpEffectPredictor snpEffectPredictor;
	String chromoSequence = "";
	char chromoBases[];

	public TestCasesZzz() {
		super();
		init();
	}

	/**
	 * Distance to transcription start site (from a position before CDS start)
	 * @param bases
	 * @param pos
	 * @param direction
	 * @return
	 */
	int codingBase(char bases[], int pos, int direction) {
		for (; (pos >= 0) && (pos < bases.length); pos += direction)
			if ((bases[pos] == '>') || (bases[pos] == '<')) return pos;

		throw new RuntimeException("This should never happen!");
	}

	/**
	 * Distance to UTR
	 * @param bases
	 * @param pos
	 * @param direction
	 * @return
	 */
	int distToUtr5(char bases[], int pos, int direction) {
		int count = 0;
		for (; (pos >= 0) && (pos < bases.length); pos -= direction) {
			if (bases[pos] == '5') return count;
			if (bases[pos] != '-') count++;
		}
		return count;
	}

	/**
	 * Count how many bases are there until the exon
	 * @param bases
	 * @param pos
	 * @return
	 */
	int exonBase(char bases[], int pos, int direction) {
		int countAfter = 0, countBefore = 0;
		int posBefore, posAfter;
		for (posAfter = pos; (posAfter >= 0) && (posAfter < bases.length); countAfter++, posAfter += direction)
			if (bases[posAfter] != '-') break;

		for (posBefore = pos; (posBefore >= 0) && (posBefore < bases.length); countBefore++, posBefore -= direction)
			if (bases[posBefore] != '-') break;

		if (countBefore <= countAfter) return posBefore;
		return posAfter;
	}

	void init() {
		initRand();
		initSnpEffPredictor(false);
	}

	void initRand() {
		rand = new Random(20130708);
	}

	/**
	 * Create a predictor
	 */
	void initSnpEffPredictor(boolean addUtrs) {
		// Create a config and force out snpPredictor for hg37 chromosome Y
		config = new Config("testCase", Config.DEFAULT_CONFIG_FILE);

		// Initialize factory
		int maxGeneLen = 1000;
		int maxTranscripts = 1;
		int maxExons = 5;
		SnpEffPredictorFactoryRand sepf = new SnpEffPredictorFactoryRand(config, rand, maxGeneLen, maxTranscripts, maxExons);
		sepf.setForcePositive(true); // WARNING: We only use positive strand here (the purpose is to check HGSV notation, not to check annotations)
		sepf.setAddUtrs(addUtrs);

		// Create predictor
		snpEffectPredictor = sepf.create();

		// Update config
		config.setSnpEffectPredictor(snpEffectPredictor);
		config.getSnpEffectPredictor().setSpliceRegionExonSize(0);
		config.getSnpEffectPredictor().setSpliceRegionIntronMin(0);
		config.getSnpEffectPredictor().setSpliceRegionIntronMax(0);

		// Chromosome sequence
		chromoSequence = sepf.getChromoSequence();
		chromoBases = chromoSequence.toCharArray();

		// No upstream or downstream
		config.getSnpEffectPredictor().setUpDownStreamLength(0);

		// Build forest
		config.getSnpEffectPredictor().buildForest();

		chromosome = sepf.getChromo();
		genome = config.getGenome();
		gene = genome.getGenes().iterator().next();
		transcript = gene.iterator().next();
	}

	/**
	 * Intronic HGS notation
	 * 
	 * @param bases
	 * @param j
	 * @param pos
	 * @param refStr
	 * @param altStr
	 * @return
	 */
	String intronHgsv(char bases[], int j, int pos, String refStr, String altStr) {
		if (transcript.isStrandMinus()) {
			refStr = GprSeq.wc(refStr);
			altStr = GprSeq.wc(altStr);
		}

		// Closest exon base
		int exonBase = exonBase(bases, j, transcript.getStrand());
		int exonDist = (j - exonBase) * transcript.getStrand();

		char type = bases[exonBase];
		String typeStr = "";
		int basesCount = 0;
		if (type == '5') {
			typeStr = "-";

			// Count UTR5 bases until TSS
			for (int i = exonBase; (i >= 0) && (i < bases.length); i += transcript.getStrand()) {
				if (bases[i] == type) basesCount++;
				else if (bases[i] != '-') break;
			}

		} else if (type == '3') {
			typeStr = "*";

			// Count UTR3 bases until end of coding 
			for (int i = exonBase; (i >= 0) && (i < bases.length); i += -transcript.getStrand()) {
				if (bases[i] == type) basesCount++;
				else if (bases[i] != '-') break;
			}
		} else if ((type == '>') || (type == '<')) {
			// Count coding bases until TSS
			for (int i = exonBase; (i >= 0) && (i < bases.length); i -= transcript.getStrand()) {
				if (bases[i] == type) basesCount++;
				else if ((bases[i] != '-') && (bases[i] != '>') && (bases[i] != '<')) break;
			}
		} else throw new RuntimeException("Unexpected base type '" + bases[exonBase] + "'");

		return "c." //
				+ typeStr //
				+ basesCount //
				+ (exonDist >= 0 ? "+" : "") + exonDist //
				+ refStr + ">" + altStr;
	}

	/**
	 * Run SnpEff on VCF file
	 * @param vcfFile
	 */
	public void snpEffect(String vcfFile, String genomeVer) {
		// Create command
		String args[] = { "-hgvs", "-ud", "0", genomeVer, vcfFile };

		SnpEff cmd = new SnpEff(args);
		SnpEffCmdEff cmdEff = (SnpEffCmdEff) cmd.snpEffCmd();

		// Run command
		List<VcfEntry> list = cmdEff.run(true);

		// Find HGVS in any 'EFF' field
		int entryNum = 1;
		for (VcfEntry vcfEntry : list) {
			boolean found = false;

			// Load hgvs expected annotations into set
			String hgvsStr = vcfEntry.getInfo("HGVS");
			HashSet<String> hgvsExpected = new HashSet<String>();
			for (String h : hgvsStr.split(",")) {
				if (h.indexOf(':') > 0) h = h.substring(h.indexOf(':') + 1);
				hgvsExpected.add(h);
			}

			if (debug) System.err.println(entryNum + "\t" + vcfEntry);

			// Find if HGVS predicted by SnpEff matches tha expected annotations
			for (VcfEffect eff : vcfEntry.parseEffects()) {
				String hgvsReal = eff.getAa();
				if (debug) System.err.println("\tHGVS: " + hgvsExpected.contains(hgvsReal) + "\t" + hgvsExpected + "\tAA: " + eff.getAa() + "\t" + eff.getGenotype() + "\t" + eff);
				if (hgvsExpected.contains(hgvsReal)) found = true;
			}

			// Not found? Error
			if (!found) throw new RuntimeException("HGVS not found in variant\n" + vcfEntry);
			entryNum++;
		}
	}

	public void test_05_intron() {
		int N = 250;

		int testIter = -1;
		int testPos = -1;

		// Test N times
		//	- Create a random gene transcript, exons
		//	- Change each base in the exon
		//	- Calculate effect
		for (int checked = 0, it = 1; checked < N; it++) {
			initSnpEffPredictor(true);
			boolean tested = false;

			// Skip test?
			if (testIter >= 0 && it < testIter) {
				Gpr.debug("Skipping iteration: " + it);
				continue;
			}

			// No introns? Nothing to test
			if (transcript.introns().size() < 1) continue;

			// !!!!!!!!!!!!!!!!!!!!
			// CHECK NEGATIVE STRAND TRANSCRIPTS
			// !!!!!!!!!!!!!!!!!!!!
			if (transcript.isStrandMinus()) {
				Gpr.debug("!!!!!!!!!!!!!!!!!!!!");
				continue;
			}

			// Character representation
			String trstr = transcript.toStringAsciiArt();
			char bases[] = trstr.toCharArray();

			// Show data
			System.out.println("HGSV Intron\tIteration:" + it + "\tChecked: " + checked);
			if (verbose) {
				System.out.println(trstr);
				System.out.println("Length   : " + transcript.size());
				System.out.println("CDS start: " + transcript.getCdsStart());
				System.out.println("CDS end  : " + transcript.getCdsEnd());
				System.out.println(transcript);
			}

			// Check each intronic base
			for (int j = 0, pos = transcript.getStart(); pos < transcript.getEnd(); j++, pos++) {
				// Intron?
				if (bases[j] == '-') {
					tested = true;

					// Skip base?
					if (testPos >= 0 && pos < testPos) {
						Gpr.debug("\tSkipping\tpos: " + pos + " [" + j + "]");
						continue;
					}

					// Ref & Alt
					String refStr = "A", altStr = "T";

					// Calculate expected hgsv string 
					String hgsv = intronHgsv(bases, j, pos, refStr, altStr);

					// Calculate effect and compare to expected
					SeqChange sc = new SeqChange(transcript.getChromosome(), pos, refStr, altStr, 1, "", 0, 0);
					ChangeEffects ceffs = snpEffectPredictor.seqChangeEffect(sc);
					ChangeEffect ceff = ceffs.get();
					String hgsvEff = ceffs.get().getHgvs();
					if (debug) System.out.println("\tpos: " + pos + " [" + j + "]\thgsv: '" + hgsv + "'\tEff: '" + hgsvEff + "'\t" + ceff.getEffectType());

					// Is this an intron? (i.e. skip other effects, such as splice site)
					// Compare expected to real HGSV strings
					if (ceff.getEffectType() == EffectType.INTRON) Assert.assertEquals(hgsv, hgsvEff);
				}
			}

			if (tested) checked++;
		}
	}
}
