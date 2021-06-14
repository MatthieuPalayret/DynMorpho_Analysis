package MP.utils;

import java.awt.Rectangle;
import java.io.File;

import MP.objects.ResultsTableMt;
import gdsc.smlm.engine.DataFilter;
import gdsc.smlm.engine.DataFilterType;
import gdsc.smlm.engine.FitEngine;
import gdsc.smlm.engine.FitEngineConfiguration;
import gdsc.smlm.engine.FitJob;
import gdsc.smlm.engine.FitQueue;
import gdsc.smlm.fitting.FitConfiguration;
import gdsc.smlm.fitting.FitCriteria;
import gdsc.smlm.fitting.FitFunction;
import gdsc.smlm.fitting.FitSolver;
import gdsc.smlm.ij.IJImageSource;
import gdsc.smlm.ij.settings.GlobalSettings;
import gdsc.smlm.ij.settings.SettingsManager;
import gdsc.smlm.results.ImageSource;
import gdsc.smlm.results.MemoryPeakResults;
import gdsc.smlm.results.PeakResult;
import ij.IJ;
import ij.ImagePlus;
import ij.Prefs;

public class FittingPeakFit {

	boolean fitted = false;
	String fileDirName;

	gdsc.smlm.engine.FitEngineConfiguration config = null;
	MemoryPeakResults results = new MemoryPeakResults();

	public static final int pixelSize = 100;

	public FittingPeakFit() {
		super();
	}

	public void setConfig(String fileDirName) {
		this.fileDirName = fileDirName;

		FitConfiguration fitConf = new FitConfiguration();

		fitConf.setNmPerPixel(pixelSize);
		fitConf.setGain(400f);
		fitConf.setInitialPeakStdDev(1.0f);
		fitConf.setInitialAngle(0f);

		fitConf.setFitSolver(FitSolver.LVM);// LVM_WEIGHTED is better, but needs a good camera NoiseModel
		fitConf.setFitFunction(FitFunction.FREE_CIRCULAR);
		fitConf.setFitCriteria(FitCriteria.LEAST_SQUARED_ERROR);
		fitConf.setLambda(10.0f);
		fitConf.setDelta(0.0001f);
		// TODO ?? fitConf.setNoiseModel(new CCDCameraNoiseModel(805, 85));//
		// Param.Readnoise, Param.Offset, true));
		fitConf.setSignificantDigits(5);
		fitConf.setMaxIterations(20);

		fitConf.setCoordinateShiftFactor(2f);
		fitConf.setSignalStrength(0f);
		fitConf.setWidthFactor(5f);
		fitConf.setMinPhotons(0f);
		fitConf.setMinWidthFactor(0.15f);
		fitConf.setPrecisionThreshold(0f);

		fitConf.setDuplicateDistance(0.5f);

		config = new FitEngineConfiguration(fitConf);
		config.setDataFilterType(DataFilterType.SINGLE);
		config.setDataFilter(DataFilter.MEAN, 1.20f, 1);
		config.setSearch(1f);
		config.setBorder(1);
		config.setFitting(3);

		config.setFailuresLimit(10);
		config.setIncludeNeighbours(true);
		config.setNeighbourHeightThreshold(0.3);
		config.setResidualsThreshold(1);

		// config.setNoiseMethod(Noise.Method.QuickResidualsLeastMeanOfSquares);
		config.initialiseState();
		;
	}

	public void loadConfig(String path) {
		GlobalSettings settings = SettingsManager.loadSettings(path);
		config = settings.getFitEngineConfiguration();
		config.initialiseState();
		;
	}

	public void getNewConfig() {
		config = SettingsManager.loadSettings().getFitEngineConfiguration();
		config.initialiseState();
	}

	public void saveConfig(String path) {
		SettingsManager.saveFitEngineConfiguration(config, path);
	}

	public MemoryPeakResults fitImage(ImagePlus imp) {

		// Check if the image is open
		ImageSource source = new IJImageSource(imp);
		if (!source.open())
			return results;

		// Check the configuration
		if (config == null)
			return results;

		// Create a fit engine
		FitEngine engine = new FitEngine(config, results, Prefs.getThreads(), FitQueue.BLOCKING, 1);

		results.begin();

		boolean shutdown = false; // Flag to allow escape to shutdown the fitting

		// Show fitting progress
		int slice = 0;
		int totalFrames = imp.getStackSize();
		final int step = (totalFrames > 400) ? totalFrames / 200 : 2;

		// Extract a region if necessary
		Rectangle bounds = (imp.getRoi() != null) ? imp.getRoi().getBounds() : null;

		while (!shutdown) {
			float[] data = source.next(bounds);
			if (data == null)
				break;

			if (++slice % step == 0) {
				IJ.showProgress(slice, totalFrames);
				IJ.showStatus("Slice: " + slice + " / " + totalFrames);
			}

			engine.run(new FitJob(slice, data, bounds));

			if (IJ.escapePressed())
				shutdown = true;
		}

		engine.end(shutdown);
		results.end();
		fitted = true;

		saveConfig(fileDirName + File.separator + "gdsc.smlm.settings.xml");

		return results;
	}

	public ResultsTableMt getResults() {
		ResultsTableMt rtFit = new ResultsTableMt();
		int SIGMAX = rtFit.addNewColumn("SigmaX");
		int SIGMAY = rtFit.addNewColumn("SigmaY");
		int NOISE = rtFit.addNewColumn("Noise");
		int OFFSET = rtFit.addNewColumn("Offset");
		int ISFITTED = rtFit.addNewColumn("isFitted");

		for (PeakResult peak : results.getResults()) {
			rtFit.incrementCounter();
			rtFit.addValue(ResultsTableMt.FRAME, peak.getEndFrame());
			rtFit.addValue(ResultsTableMt.X, peak.getXPosition());
			rtFit.addValue(ResultsTableMt.Y, peak.getYPosition());
			rtFit.addValue(ResultsTableMt.INTENSITY, peak.getSignal());
			rtFit.addValue(SIGMAX, peak.getXSD());
			rtFit.addValue(SIGMAY, peak.getYSD());
			rtFit.addValue(NOISE, peak.noise);
			rtFit.addValue(OFFSET, peak.getBackground());
			rtFit.addValue(ISFITTED, peak.error);
		}

		return rtFit;
	}

}
