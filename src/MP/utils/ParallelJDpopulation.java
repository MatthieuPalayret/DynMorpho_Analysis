package MP.utils;

import java.awt.Color;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import MP.objects.ResultsTableMt;
import MP.params.Params;

import java.util.Random;

import flanagan.analysis.Regression;
import flanagan.analysis.RegressionFunction;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.measure.CurveFitter;
import ij.measure.Minimizer;

public class ParallelJDpopulation implements Parallel.Operation<Integer> {

	private ResultsTableMt cumulJD;
	private int[] columns;
	private File directory;
	private double exposure;

	public ResultsTableMt stats;
	public double[] frames;

	public String localDir;
	public ResultsTableMt fit_;
	public ResultsTableMt fit_boot;

	public boolean saveTextFile = true;

	private int maxPopulationsToBeFitted = 3;
	private int minPopulationsToBeFitted = 1;
	public int bootstrapNumber = -1;
	private int decimalPlaces = 10;
	public int tausToBeFitted = 4;

	public ParallelJDpopulation(HashMap<Integer, ResultsTableMt> retour, double stepNM, int highestTimeStep,
			double exposure, File directory, boolean saveTextFile) {
		this.columns = new int[highestTimeStep + 1];
		this.directory = directory;
		this.exposure = exposure;
		this.saveTextFile = saveTextFile;
		this.cumulJD = fillDistributionMeanDisplacements(retour, highestTimeStep, columns);

		initialise(highestTimeStep);
	}

	private void initialise(int highestTimeStep) {

		if (columns == null) {
			columns = new int[highestTimeStep + 1];
			columns[1] = cumulJD.getColumnIndex("JD");
			for (int i = 2; i <= highestTimeStep; i++) {
				columns[i] = cumulJD.getColumnIndex("JD-" + i);
			}
		}

		stats = new ResultsTableMt();
		frames = new double[highestTimeStep - 1];

		localDir = directory + File.separator + "PopulationSD";
		new File(localDir).mkdir();
		fit_ = new ResultsTableMt();
		fit_boot = new ResultsTableMt();

		cumulJD.saveAsPrecise(localDir + File.separator + "cumulJD.txt", decimalPlaces);
	}

	@Override
	public void perform(Integer stepFrame) {
		if (bootstrapNumber == -1)
			jd_Analysis(cumulJD.getColumnAsDoubles(columns[stepFrame]), stepFrame, stats);
		else {
			if (cumulJD_bootstrap_traj.getCounter() > 0)
				jd_Statistical_Analysis_Traj(cumulJD.getColumnAsDoubles(columns[stepFrame]), stepFrame, stats);
			else
				jd_Statistical_Analysis_JD(cumulJD.getColumnAsDoubles(columns[stepFrame]), stepFrame, stats);
		}

		frames[stepFrame - 1] = stepFrame * exposure;
	}

	public void post() {
		stats = Utils.sortRt(stats, "Tau");
		stats.saveAsPrecise(localDir + File.separator + "PlotA_Fit_Values.txt", decimalPlaces);

		for (int population = minPopulationsToBeFitted; population <= maxPopulationsToBeFitted; population++)
			fit_ = fitDiffusionCoefficients(stats, population, bootstrapNumber, fit_);
		if (fit_.getCounter() > 0)
			fit_.saveAsPrecise(localDir + File.separator + "Diffusion_Coefficients.txt", decimalPlaces);

		if (fit_boot.getCounter() > 0) {
			fit_boot.saveAsPrecise(localDir + File.separator + "Diffusion_Coefficients_Bootstrap.txt", decimalPlaces);
			extractOriginRt(3).saveAsPrecise(localDir + File.separator + "Diffusion_coefficients_ForOrigin.txt",
					decimalPlaces);
		}
	}

	public void plot() {
		for (int population = minPopulationsToBeFitted; population <= maxPopulationsToBeFitted; population++)
			plot(population);
	}

	public void plot(int population) {

		// Fill the stacks P(r2, Tau) = f(r2) stacking through Tau (and its residuals)
		ImageStack plotA = new ImageStack(512, 512);
		ImageStack plotAresiduals = new ImageStack(512, 200);
		for (int stepFrame = 1; stepFrame < frames.length + 1; stepFrame++) {
			ImagePlus[] temp = plot(population, stepFrame);
			plotA.addSlice("", temp[0].getProcessor(), stepFrame - 1);
			plotAresiduals.addSlice("", temp[1].getProcessor(), stepFrame - 1);
		}
		Utils.saveTiff(new ImagePlus("", plotA), localDir + File.separator + population + "_PlotA.tif", true);
		Utils.saveTiff(new ImagePlus("", plotAresiduals),
				localDir + File.separator + population + "_PlotAresiduals.tif", true);

		double[][] f = new double[frames.length][population];
		double[][] D = new double[frames.length][population];
		for (int stepFrame = 1; stepFrame < f.length + 1; stepFrame++) {
			f[stepFrame - 1] = f(population, stepFrame);
			D[stepFrame - 1] = D(population, stepFrame);
		}
		f = Utils.transpose(f);
		D = Utils.transpose(D);
		double[][] popSD = new double[population][frames.length];
		for (int pop = 0; pop < population; pop++)
			popSD[pop] = Utils.times(Utils.times(D[pop], frames), 4);

		Plot plotFractions = new Plot("Population fractions", "Tau (s)", "fraction");
		plotFractions.setLimits(0, frames[frames.length - 1], 0, 1);
		plotFractions.setSize(512, 512);
		for (int pop = 0; pop < population; pop++) {
			plotFractions.setColor(Utils.getGradientColor(Color.RED, Color.BLUE, population, pop));
			plotFractions.addPoints(frames, f[pop], Plot.CIRCLE);
			plotFractions.draw();
		}
		Utils.saveTiff(plotFractions.getImagePlus(), localDir + File.separator + population + "_PlotFractions.tif",
				true);

		ImageStack diffusion = new ImageStack(512, 512);
		Plot[] plotDiffusions = new Plot[population];
		for (int pop = 0; pop < population; pop++) {
			plotDiffusions[pop] = new Plot("Diffusion coefficients of the population #" + (pop + 1), "Tau (s)",
					"r^2 (µm^2)");
			plotDiffusions[pop].setLimits(0, frames[frames.length - 1], 0, Utils.maxMin(popSD[pop])[0]);
			plotDiffusions[pop].setSize(512, 512);
			plotDiffusions[pop].setColor(Color.BLACK);
			plotDiffusions[pop].addPoints(frames, popSD[pop], Plot.CIRCLE);
			plotDiffusions[pop].draw();

			int temp_popMinus1 = ((population - 1) * population) / 2;
			int temp_minPop = ((minPopulationsToBeFitted - 1) * minPopulationsToBeFitted) / 2;
			int row = temp_popMinus1 - temp_minPop + pop;

			// Tau <= 200ms
			// y = (yi(x=0)) + 4*Di*x;
			plotDiffusions[pop].setColor(Color.GRAY);
			plotDiffusions[pop].drawLine(0, fit_.getValue("yi(x=0)", row), frames[tausToBeFitted - 1],
					4.0 * fit_.getValue("Di (µm^2/s)", row) * frames[tausToBeFitted - 1]
							+ fit_.getValue("yi(x=0)", row));

			// Tau > 200ms
			// y = SDf;
			plotDiffusions[pop].drawLine(frames[tausToBeFitted], fit_.getValue("SDf (µm^2)", row),
					frames[frames.length - 1], fit_.getValue("SDf (µm^2)", row));

			// For all Tau
			// y = L^2/3 (1 - exp(-12 D Tau / L^2)) + Offset
			double l2 = fit_.getValue("L (µm)", row);
			l2 = l2 * l2;
			double[] y = Utils.plus(Utils.times(
					Utils.plus(Utils.exp(Utils.times(frames, -12.0 * fit_.getValue("D_exp (µm^2/s)", row) / l2)), -1),
					-l2 / 3.0), fit_.getValue("Offset (µm^2)", row));
			plotDiffusions[pop].setColor(Color.DARK_GRAY);
			plotDiffusions[pop].addPoints(frames, y, Plot.LINE);
			plotDiffusions[pop].draw();

			diffusion.addSlice(plotDiffusions[pop].getProcessor());
		}
		Utils.saveTiff(new ImagePlus("", diffusion), localDir + File.separator + population + "_PlotPopMSDs.tif", true);

	}

	private ResultsTableMt fitDiffusionCoefficients(ResultsTableMt plotA_FitBootstrap, int fitOfInterest,
			int bootstrapNumber, ResultsTableMt fit_sum) {

		double[] zsum = new double[plotA_FitBootstrap.getCounter()];
		for (int pop = 1; pop <= fitOfInterest; pop++) {

			// Prepare the bootstrap data and the sum data to be fitted
			double[] x = plotA_FitBootstrap.getColumnAsDoubles(plotA_FitBootstrap.getColumnIndex("Tau"));
			double[] y = Utils.times(x,
					Utils.times(
							plotA_FitBootstrap.getColumnAsDoubles(
									plotA_FitBootstrap.getColumnIndex("" + fitOfInterest + "Population_D" + pop)),
							4.0));
			double[] z;
			if (pop != fitOfInterest) {
				z = plotA_FitBootstrap.getColumnAsDoubles(
						plotA_FitBootstrap.getColumnIndex("" + fitOfInterest + "Population_f" + pop));
				zsum = Utils.plus(zsum, z);
			} else {
				z = Utils.times(Utils.plus(zsum, -1), -1);
			}

			if (bootstrapNumber == -1) {

				fit_sum.incrementCounter();
				fit_sum.addValue("Populations", fitOfInterest);
				fit_sum.addValue("Fit_Pop#", pop);
				// Fit the (sum)SD_pop vs. tau curve
				fitDiffusionCoefficient(x, y, fit_sum);
				fitFraction(z, fit_sum);

			} else {
				// Prepare the sum data to be fitted
				double[] x_sum = new double[x.length / (bootstrapNumber + 1)];
				double[] y_sum = new double[x_sum.length];
				double[] z_sum = new double[x_sum.length];
				double[][] x_boot = new double[bootstrapNumber][x_sum.length];
				double[][] y_boot = new double[bootstrapNumber][x_sum.length];
				double[][] z_boot = new double[bootstrapNumber][x_sum.length];

				for (int i = 0; i < x_sum.length; i++) {
					for (int boot = 0; boot < bootstrapNumber; boot++) {
						x_boot[boot][i] = x[i * (bootstrapNumber + 1) + boot];
						y_boot[boot][i] = y[i * (bootstrapNumber + 1) + boot];
						z_boot[boot][i] = z[i * (bootstrapNumber + 1) + boot];
					}

					x_sum[i] = x[(i + 1) * (bootstrapNumber + 1) - 1];
					y_sum[i] = y[(i + 1) * (bootstrapNumber + 1) - 1];
					z_sum[i] = z[(i + 1) * (bootstrapNumber + 1) - 1];
				}

				// Fit the (avg)SD_pop vs. tau curve
				for (int boot = 0; boot < bootstrapNumber; boot++) {
					fit_boot.incrementCounter();
					fit_boot.addValue("Populations", fitOfInterest);
					fit_boot.addValue("Fit_Pop#", pop);
					fitDiffusionCoefficient(x_boot[boot], y_boot[boot], fit_boot);
					fitFraction(z_boot[boot], fit_boot);
				}

				fit_sum.incrementCounter();
				fit_sum.addValue("Populations", fitOfInterest);
				fit_sum.addValue("Fit_Pop#", pop);
				// Fit the (sum)SD_pop vs. tau curve
				fitDiffusionCoefficient(x_sum, y_sum, fit_sum);
				fitFraction(z_sum, fit_sum);
				// Add the stdev and avg values from the bootstrap fitting from above
				String[] Di = new String[] { "Di", "SDf", "L", "D_exp", "Offset", "fraction" };
				String[] Di_ = new String[] { "Di (µm^2/s)", "SDf (µm^2)", "L (µm)", "D_exp (µm^2/s)", "Offset (µm^2)",
						"fraction" };
				for (int j = 0; j < Di.length; j++) {
					double[] mean = Utils.getMeanAndStdev(fit_boot.getColumnAsDoubles(fit_boot.getColumnIndex(Di_[j])),
							true);
					fit_sum.addValue(Di[j] + "_avg_bootstrap", mean[0]);
					fit_sum.addValue(Di[j] + "_stdev_bootstrap", mean[1]);
				}

			}
		}

		return fit_sum;
	}

	private void fitDiffusionCoefficient(double[] x_, double[] y_, ResultsTableMt fit_) {

		// for the first 4 points (Tau <= 200ms)
		// Tau from ms to s / SD from nm^2 to um^2
		double[] x = Utils.trunc(x_, 0, tausToBeFitted - 1);
		double[] y = Utils.trunc(y_, 0, tausToBeFitted - 1);
		CurveFitter fit = new CurveFitter(x, y);
		fit.doFit(CurveFitter.STRAIGHT_LINE, false);

		if (fit.getStatus() == Minimizer.SUCCESS) {
			// y = a + b*x;

			// https://en.wikipedia.org/wiki/Simple_linear_regression
			double bStdev = Math.sqrt(Utils.getMeanAndStdev(fit.getResiduals(), false)[1]
					/ Utils.sumOfSquares(Utils.plus(x, -Utils.getMean(x))));

			fit_.addValue("Di (µm^2/s)", fit.getParams()[1] / 4.0);
			fit_.addValue("Di_stdev", bStdev);
			fit_.addValue("Di_avg_bootstrap", 0);
			fit_.addValue("Di_stdev_bootstrap", 0);
			fit_.addValue("yi(x=0)", fit.getParams()[0]);
			fit_.addValue("Loc_Precision", Math.sqrt(fit.getParams()[0] / 4.0));
			fit_.addValue("Ri^2", fit.getRSquared());
		}

		// Get average +/- stdev
		// for the last 10-4 = 6 points (Tau>=250ms)
		y = Utils.trunc(y_, tausToBeFitted, y_.length - 1);
		double mean[] = Utils.getMeanAndStdev(y, false);

		fit_.addValue("SDf (µm^2)", mean[0]);
		fit_.addValue("SDf_stdev", mean[1]);
		fit_.addValue("SDf_avg_bootstrap", 0);
		fit_.addValue("SDf_stdev_bootstrap", 0);

		// Fit with SD = L^2/3 (1 - exp(-12 D Tau / L^2))
		// Tau from ms to s / SD from nm^2 to um^2
		fit = new CurveFitter(x_, y_);
		fit.doFit(CurveFitter.EXP_RECOVERY, false);

		if (fit.getStatus() == Minimizer.SUCCESS) {
			// y = a*(1 - exp(-b*x)) + c;

			double l = Math.sqrt(3.0 * fit.getParams()[0]);
			fit_.addValue("L (µm)", l);
			fit_.addValue("L_avg_bootstrap", 0);
			fit_.addValue("L_stdev_bootstrap", 0);
			double d = l * l * fit.getParams()[1] / 12.0;
			fit_.addValue("D_exp (µm^2/s)", d);
			fit_.addValue("D_exp_avg_bootstrap", 0);
			fit_.addValue("D_exp_stdev_bootstrap", 0);
			fit_.addValue("Offset (µm^2)", fit.getParams()[2]);
			fit_.addValue("Offset_stdev_bootstrap", 0);
			fit_.addValue("Offset_avg_bootstrap", 0);
			fit_.addValue("R_exp^2", fit.getRSquared());
		}
	}

	private void fitFraction(double[] y, ResultsTableMt fit_) {

		// Get average +/- stdev
		double[] mean = Utils.getMeanAndStdev(y, false);

		fit_.addValue("fraction", mean[0]);
		fit_.addValue("fraction_stdev", mean[1]);
		fit_.addValue("fraction_avg_bootstrap", 0);
		fit_.addValue("fraction_stdev_bootstrap", 0);
	}

	public ImagePlus[] plot(int population, int stepFrame) {

		// Organise fitted values
		double[] f = f(population, stepFrame);
		double[] D = D(population, stepFrame);

		// Cumulative density
		double[] jd = cumulJD.getColumnAsDoubles(columns[stepFrame]);
		Arrays.sort(jd);
		jd = Utils.removeZeros(jd);
		double[] r2 = Utils.times(jd, jd);

		double[] pData = new double[jd.length];
		for (int i = 0; i < pData.length; i++)
			pData[i] = i + 1;
		pData = Utils.times(pData, 1.0 / Utils.getMax(pData));

		double[] pCalc = new double[jd.length];
		for (int i = 0; i < pCalc.length; i++) {
			pCalc[i] = 1;
			for (int pop = 0; pop < population; pop++)
				pCalc[i] -= f[pop] * Math.exp(-r2[i] * 0.001 * 0.001 / (4.0 * D[pop] * frames[stepFrame - 1]));
		}

		double[] resid = new double[jd.length];
		for (int i = 0; i < resid.length; i++)
			resid[i] = pData[i] - pCalc[i];

		Plot plot = new Plot("Populations: " + population + " / Tau = " + frames[stepFrame - 1] + " (s)", "r^2 (nm^2)",
				"P(r^2, Tau)");
		plot.setSize(512, 512);
		plot.setLimits(0, r2[r2.length - 1], 0, 1);
		plot.setColor(Color.BLACK);
		plot.addPoints(r2, pData, Plot.DOT);
		plot.draw();
		plot.setColor(Color.GRAY);
		plot.addPoints(r2, pCalc, Plot.LINE);
		plot.draw();

		Plot residuals = new Plot("Populations: " + population + " / Tau = " + frames[stepFrame - 1] + " (s)",
				"r^2 (nm^2)", "residuals");
		double yMax = Math.max(Utils.getMax(resid), -Utils.getMin(resid));
		residuals.setLimits(0, r2[r2.length - 1], -yMax, yMax);
		residuals.setColor(Color.BLACK);
		residuals.addPoints(r2, resid, Plot.LINE);
		residuals.draw();
		residuals.setColor(Color.GRAY);
		residuals.drawLine(0, 0, r2[r2.length - 1], 0);
		residuals.setSize(512, 200);

		// Save the coordinates as a ResultsTableMt
		if (saveTextFile) {
			ResultsTableMt toSave = new ResultsTableMt();
			for (int row = 0; row < r2.length; row++) {
				toSave.incrementCounter();
				toSave.addValue("r^2", r2[row]);
				toSave.addValue("P(r^2, Tau)_Data", pData[row]);
				toSave.addValue("P(r^2, Tau)_Calc", pCalc[row]);
				toSave.addValue("Residuals", resid[row]);
			}
			toSave.saveAsPrecise(
					localDir + File.separator + "_" + population + "PopFit_" + stepFrame + "stepFrame_PlotA.txt",
					decimalPlaces);
		}

		return new ImagePlus[] { plot.getImagePlus(), residuals.getImagePlus() };
	}

	public double[] f(int population, int stepFrame) {
		double[] f = new double[population];
		double sum = 1;

		int row = stepFrame - 1;
		if (bootstrapNumber > 0)
			row = (row + 1) * (bootstrapNumber + 1) - 1;

		for (int pop = 0; pop < population - 1; pop++) {
			f[pop] = stats.getValue("" + population + "Population_f" + (pop + 1), row);
			sum -= f[pop];
		}
		f[population - 1] = sum;
		return f;
	}

	public double[] D(int population, int stepFrame) {
		double[] D = new double[population];

		int row = stepFrame - 1;
		if (bootstrapNumber > 0)
			row = (row + 1) * (bootstrapNumber + 1) - 1;

		for (int pop = 0; pop < population; pop++) {
			D[pop] = stats.getValue("" + population + "Population_D" + (pop + 1), row);
		}
		return D;
	}

	public void jd_Analysis(double[] jd, int stepFrame, ResultsTableMt stats) {
		jd_Analysis(jd, stepFrame, stats, -1);
	}

	public void jd_Analysis(double[] jd, int stepFrame, ResultsTableMt stats, int bootstrap) {

		// Jump distance analysis
		Arrays.sort(jd);
		jd = Utils.removeZeros(jd);
		double[] jdY = new double[jd.length];
		for (int i = 0; i < jd.length; i++)
			jdY[i] = i + 1;
		jdY = Utils.times(jdY, 1.0 / Utils.getMax(jdY));

		Regression regJD = new Regression(jd, jdY);
		class RegJD implements RegressionFunction {

			double exp;
			int population;

			@Override
			public double function(double[] param, double[] x) {
				// param = {D1, f1, D2, etc.} in um^2/s
				// x = {r}
				double r = x[0]; // in um.
				double r2 = r * r;
				double y = 1;
				double f_last = 1;
				for (int pop = 0; pop < population - 1; pop++) {
					y -= param[2 * pop + 1] * Math.exp(-r2 / (4.0 * param[2 * pop] * exp));
					f_last -= param[2 * pop + 1];
				}
				y -= f_last * Math.exp(-r2 / (4.0 * param[2 * (population - 1)] * exp));
				return y;
			}

		}
		;

		ResultsTableMt toStats = new ResultsTableMt();
		toStats.incrementCounter();

		for (int population = minPopulationsToBeFitted; population <= maxPopulationsToBeFitted; population++) {
			RegJD regFunct = new RegJD();
			regFunct.population = population;
			regFunct.exp = exposure * stepFrame; // in s.

			double[] start = new double[2 * population - 1];
			for (int pop = 0; pop < population - 1; pop++) {
				start[2 * pop] = Math.pow(10, -4.0 / (population + 1) * (pop + 1));
				start[2 * pop + 1] = 1.0 / population;
			}
			start[2 * (population - 1)] = Math.pow(10, -4.0 / (population + 1) * population);

			for (int pop = 0; pop < population - 1; pop++) {
				regJD.addConstraint(2 * pop, -1, 0);
				regJD.addConstraint(2 * pop + 1, 1, 1);
				regJD.addConstraint(2 * pop + 1, -1, 0);
			}
			regJD.addConstraint(2 * (population - 1), -1, 0);

			regJD.simplex(regFunct, start);

			toStats.addValue("Tau", stepFrame * exposure);
			for (int pop = 0; pop < population - 1; pop++) {
				toStats.addValue("" + population + "Population_D" + (pop + 1), regJD.getBestEstimates()[2 * pop]);
				toStats.addValue("" + population + "Population_D" + (pop + 1) + "_stdev",
						regJD.getBestEstimatesStandardDeviations()[2 * pop]);
				toStats.addValue("" + population + "Population_f" + (pop + 1), regJD.getBestEstimates()[2 * pop + 1]);
				toStats.addValue("" + population + "Population_f" + (pop + 1) + "_stdev",
						regJD.getBestEstimatesStandardDeviations()[2 * pop + 1]);
			}
			toStats.addValue("" + population + "Population_D" + population,
					regJD.getBestEstimates()[2 * (population - 1)]);
			toStats.addValue("" + population + "Population_D" + population + "_stdev",
					regJD.getBestEstimatesStandardDeviations()[2 * (population - 1)]);
			toStats.addValue("Bootstrap", bootstrap);
		}

		// Save the fitted values to stats
		Utils.addRow(toStats, stats, toStats.getCounter() - 1);

	}

	public void jd_Statistical_Analysis_JD(double[] jd, int stepFrame, ResultsTableMt stats) {
		double[][] jdStat = new double[bootstrapNumber][jd.length / bootstrapNumber];
		ResultsTableMt JDrt = new ResultsTableMt();
		for (int row = 0; row < jd.length; row++) {
			JDrt.incrementCounter();
			JDrt.addValue(ResultsTableMt.JD, jd[row]);
		}

		Random rand = new Random();
		for (int bootstrap = 0; bootstrap < bootstrapNumber; bootstrap++) {
			for (int row = 0; row < jdStat[bootstrap].length; row++) {
				int randIndex = rand.nextInt(JDrt.getCounter());
				jdStat[bootstrap][row] = JDrt.getValueAsDouble(ResultsTableMt.JD, randIndex);
				JDrt.deleteRow(randIndex);
			}
		}

		for (int bootstrap = 0; bootstrap < bootstrapNumber; bootstrap++) {
			jd_Analysis(jdStat[bootstrap], stepFrame, stats, bootstrap);
		}
		jd_Analysis(jd, stepFrame, stats, -1);
	}

	private ResultsTableMt cumulJD_bootstrap_traj = new ResultsTableMt();

	public void jd_Statistical_Analysis_Traj(double[] jdTotal, int stepFrame, ResultsTableMt stats) {
		for (int bootstrap = 0; bootstrap < bootstrapNumber; bootstrap++) {
			jd_Analysis(
					cumulJD_bootstrap_traj.getColumnAsDoubles(cumulJD_bootstrap_traj.getColumnIndex(
							"JD" + ((stepFrame == 1) ? "" : ("-" + stepFrame)) + "_bootstrap_traj_" + bootstrap)),
					stepFrame, stats, bootstrap);
		}
		jd_Analysis(jdTotal, stepFrame, stats, -1);
	}

	public static ResultsTableMt fillDistributionMeanDisplacements(HashMap<Integer, ResultsTableMt> hm,
			int highestTimeStep, int[] columns) {
		ResultsTableMt cumulJD = new ResultsTableMt();
		cumulJD.incrementCounter();
		int[] columnSize = new int[columns.length];

		cumulJD.addValue("JD", 0);
		columns[1] = cumulJD.getColumnIndex("JD");
		for (int i = 2; i <= highestTimeStep; i++) {
			cumulJD.addValue("JD-" + i, 0);
			columns[i] = cumulJD.getColumnIndex("JD-" + i);
		}

		// Fill cumulJD with other time steps (1 .. highestTimeStep)
		Iterator<Entry<Integer, ResultsTableMt>> it = hm.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Integer, ResultsTableMt> itTemp = it.next();
			ResultsTableMt traj = itTemp.getValue();

			for (int row2 = 0; row2 < traj.getCounter(); row2++) {

				for (int row3 = row2 + 1; row3 < Math.min(traj.getCounter(), row2 + highestTimeStep + 1); row3++) {

					if (cumulJD.getCounter() <= columnSize[row3 - row2])
						cumulJD.incrementCounter();
					cumulJD.setValue(columns[row3 - row2], columnSize[row3 - row2],
							Utils.getDistance(traj, row2, row3));
					columnSize[row3 - row2]++;

				}
			}
		}
		return cumulJD;
	}

	public void initialiseBootstrapTraj(String[][] dirTemp) {

		for (int analysis = 0; analysis < dirTemp.length; analysis++) {
			String subDirName = dirTemp[analysis][0] + File.separator
					+ dirTemp[analysis][1].substring(0, dirTemp[analysis][1].lastIndexOf(".tif")) + File.separator
					+ "PopulationSD" + File.separator + "Temp";
			for (int bootstrap = 0; bootstrap < bootstrapNumber; bootstrap++) {

				ResultsTableMt temp = ResultsTableMt
						.open2(subDirName + File.separator + "JD_bootstrap_" + bootstrap + ".txt");

				while (cumulJD_bootstrap_traj.getCounter() < temp.getCounter())
					cumulJD_bootstrap_traj.incrementCounter();

				for (int stepFrame = 0; stepFrame < columns.length; stepFrame++) {
					String columnHeading = "JD" + ((stepFrame == 0) ? "" : ("-" + (stepFrame + 1)));

					for (int row = 0; row < temp.getCounter(); row++) {

						cumulJD_bootstrap_traj.setValue(columnHeading + "_bootstrap_traj_" + bootstrap, row,
								temp.getValue(columnHeading, row));
					}
				}
			}
		}

	}

	public ResultsTableMt extractOriginRt(int fitOfInterest) {
		String[] to = new String[] { "Di", "SDf", "f" };
		String[] from = new String[] { "Di (µm^2/s)", "SDf (µm^2)", "fraction" };

		ResultsTableMt retour = new ResultsTableMt();
		while (retour.getCounter() < bootstrapNumber + 1)
			retour.incrementCounter();

		int row = 0;
		while (fit_boot.getValue("Populations", row) < fitOfInterest)
			row++;

		for (int pop = 1; pop <= fitOfInterest; pop++) {
			for (int bootstrap = 0; bootstrap < bootstrapNumber; bootstrap++) {
				if (pop == 1)
					retour.setValue("Bootstrap", bootstrap, bootstrap);
				for (int i = 0; i < to.length; i++)
					retour.setValue(to[i] + pop, bootstrap, fit_boot.getValue(from[i], row));
				row++;

			}
		}

		// Add as last line the corresponding sum from fit_
		row = 0;
		while (fit_.getValue("Populations", row) < fitOfInterest)
			row++;
		retour.setValue("Bootstrap", bootstrapNumber, -1);
		for (int pop = 1; pop <= fitOfInterest; pop++) {
			for (int i = 0; i < to.length; i++)
				retour.setValue(to[i] + pop, bootstrapNumber, fit_.getValue(from[i], row));
			row++;
		}

		// Calculate the sub-fraction f' (f'1 = f1 / (f1+f2) )
		if (fitOfInterest == 3) {
			for (int bootstrap = 0; bootstrap < bootstrapNumber + 1; bootstrap++) {
				int pop = 2;
				double f1 = retour.getValue(to[2] + (pop - 1), bootstrap), f2 = retour.getValue(to[2] + pop, bootstrap),
						f_ = f1 / (f1 + f2);
				retour.setValue("f'" + (pop - 1), bootstrap, f_);
				retour.setValue("Width_" + (pop - 1), bootstrap, f_ * 30);
			}
		}

		return retour;
	}

	public void updateOriginRt(int fitOfInterest, Params param) {
		localDir = directory + File.separator + "PopulationSD";
		fit_boot = ResultsTableMt.open2(localDir + File.separator + "Diffusion_Coefficients_Bootstrap.txt");
		fit_ = ResultsTableMt.open2(localDir + File.separator + "Diffusion_Coefficients.txt");
		extractOriginRt(fitOfInterest).saveAsPrecise(localDir + File.separator + "Diffusion_coefficients_ForOrigin.txt",
				10);
	}

}
