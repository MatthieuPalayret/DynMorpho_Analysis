package MP;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import MP.Combine_Excell_Results.ExcelHolder;
import MP.objects.Histogram;
import MP.objects.ResultsTableMt;
import MP.utils.Parallel;
import MP.utils.ParallelJDpopulation;
import MP.utils.Utils;
import flanagan.analysis.Regression;
import ij.IJ;
import ij.gui.Plot;
import ij.plugin.PlugIn;

public class Analyse_Trajectories implements PlugIn {

	protected static final String sheetName = "Position", sheetNameTime = "Time";
	protected File directory;
	protected HashMap<Integer, ResultsTableMt> hashMapRt;

	boolean saveTextFile = false;

	private double[] stepHist;
	private double stepUM = 0;
	private double exposure = 0;
	private double[][] xYMaxMin = new double[2][2];

	public Analyse_Trajectories() {
	}

	@Override
	public void run(String arg0) {
		extractTrajectories();

		stepDistribution();

		msd_Analysis(true, 15, 2);

		populationJD_Analysis(saveTextFile);
	}

	private HashMap<Integer, ResultsTableMt> extractTrajectories() {
		return extractTrajectories(null);
	}

	protected HashMap<Integer, ResultsTableMt> extractTrajectories(String fileAsked) {
		String[] path = Utils.getAFile((fileAsked == null ? "Select a .xls Imaris-derived file:" : fileAsked), "", "");
		if (path == null || path[1].indexOf(".xls") <= 0)
			IJ.log("Either no file was selected, or the selected file is not an .xls Imaris-derived file.");
		directory = new File(path[0] + File.separator + path[1].substring(0, path[1].indexOf(".xls")));
		directory.mkdir();

		Combine_Excell_Results cir = new Combine_Excell_Results();

		try {
			ExcelHolder holder = new ExcelHolder(path[0] + File.separator + path[1]);
			if (holder.excelFile.exists()) {
				holder.fileIn = new FileInputStream(holder.excelFile);
				holder.wb = WorkbookFactory.create(holder.fileIn);
				Iterator<Sheet> itSheet = holder.wb.sheetIterator();
				while (itSheet.hasNext()) {
					String sheetName = itSheet.next().getSheetName();
					if (sheetName.equalsIgnoreCase(Analyse_Trajectories.sheetName)
							|| sheetName.equalsIgnoreCase(Analyse_Trajectories.sheetNameTime)) {
						cir.addToResults(holder, sheetName);
					}
				}
			}

			// Close the Excel file
			cir.closeExcelHolder(holder);
		} catch (EncryptedDocumentException | IOException e) {
			e.printStackTrace();
		}

		ResultsTableMt rtTemp = cir.hm.get(sheetName);
		// Transform in my ResultsTableMt, with proper heading names! And only keep the
		// relevant information!
		// headings = { "Position X", "Position Y", "Position Z", "Unit", "Category",
		// "Collection", "Time", "TrackID", "ID", "Origin" };
		ResultsTableMt timeTemp = cir.hm.get(sheetNameTime);
		ResultsTableMt allPositions = new ResultsTableMt();
		HashMap<String, Integer> originPath = new HashMap<String, Integer>();
		int originLastNumber = 0;
		for (int row = 0; row < rtTemp.getCounter(); row++) {
			allPositions.incrementCounter();
			allPositions.addValue(ResultsTableMt.FRAME, Double.parseDouble(rtTemp.getStringValue(6, row)));
			allPositions.addValue("Time", Double.parseDouble(timeTemp.getStringValue(0, row)));
			allPositions.addValue(ResultsTableMt.X, Double.parseDouble(rtTemp.getStringValue(0, row)));
			allPositions.addValue(ResultsTableMt.Y, Double.parseDouble(rtTemp.getStringValue(1, row)));
			// IJ.log("Row: " + row + " / Group value: " + rtTemp.getStringValue(7, row));
			allPositions.addValue(ResultsTableMt.GROUP, Double.parseDouble(rtTemp.getStringValue(7, row)));
			String temp = rtTemp.getStringValue(9, row);
			allPositions.addValue("Origin", temp);
			if (originPath.containsKey(temp))
				allPositions.addValue("Origin2", originPath.get(temp));
			else {
				originLastNumber++;
				allPositions.addValue("Origin2", originLastNumber);
				originPath.put(temp, originLastNumber);
			}
			allPositions.addValue("OriginalFit2", row);
		}
		timeTemp = null;
		rtTemp = null;
		originPath = null;
		cir.dispose();
		cir = null;

		hashMapRt = new HashMap<Integer, ResultsTableMt>();
		// Sort by Time, then TrackID, then Origin, so that to get the trajectories
		// ordered one below the following
		rtTemp = Utils.sortRt(allPositions, ResultsTableMt.FRAME);
		rtTemp = Utils.sortRt(rtTemp, ResultsTableMt.GROUP);
		rtTemp = Utils.sortRt(rtTemp, "Origin2");
		for (int row = 0; row < rtTemp.getCounter(); row++) {
			rtTemp.setValue("Origin", row,
					allPositions.getStringValue("Origin", (int) rtTemp.getValue("OriginalFit2", row)));
		}
		allPositions = rtTemp;
		allPositions.deleteColumn("OriginalFit2"); // Erase in this order, from the last created column to the first
		allPositions.deleteColumn("Origin2"); // because the ResultsTableMt headings are not updated after deletion.
		allPositions.deleteColumn("OriginalFit");

		// Determine stepUM, the maximal distance (in µm) between two consecutive
		// positions in a trajectory.
		{
			int trackIDTemp = (int) allPositions.getValueAsDouble(ResultsTableMt.GROUP, 0);
			String originTemp = allPositions.getStringValue("Origin", 0);
			for (int row = 1; row < allPositions.getCounter(); row++) {
				if (allPositions.getStringValue("Origin", row).equalsIgnoreCase(originTemp)
						&& (int) allPositions.getValueAsDouble(ResultsTableMt.GROUP, row) == trackIDTemp) {
					stepUM = Math.max(stepUM, Utils.getDistance(allPositions, row, row - 1));
				} else {
					trackIDTemp = (int) allPositions.getValueAsDouble(ResultsTableMt.GROUP, row);
					originTemp = allPositions.getStringValue("Origin", row);
				}
			}
			double stepUMMax = 2;
			if (stepUM > stepUMMax) {
				IJ.log("The maximal distance between two consecutive positions in a trajectory was " + stepUM
						+ " µm. We fixed it to " + stepUMMax + " µm for further jump-distance analysis.");
				stepUM = stepUMMax;
			}
			stepHist = new double[(int) (stepUM * 1000.0 + 0.5D) + 1];

			xYMaxMin[0] = Utils.maxMin(allPositions.getColumnAsDoubles(ResultsTableMt.X));
			xYMaxMin[1] = Utils.maxMin(allPositions.getColumnAsDoubles(ResultsTableMt.Y));
		}

		// Determine exposure.
		{
			double[] temp = allPositions.getColumnAsDoubles(allPositions.getColumnIndex("Time"));
			double[] tempN = new double[temp.length - 1], tempNPrevious = new double[temp.length - 1];
			for (int i = 0; i < tempN.length; i++) {
				tempN[i] = temp[i + 1];
				tempNPrevious[i] = temp[i];
			}
			double[] nMinusPrevious = Utils.minus(tempN, tempNPrevious);
			double[] meanAndStdev = Utils.getMeanAndStdev(nMinusPrevious, false);
			double median = Utils.getMedian(nMinusPrevious, false);
			if (meanAndStdev[0] < 0.01)
				exposure = meanAndStdev[0];
			else
				exposure = median;
		}

		int trajNumber = 0;
		int row = 0;
		if (allPositions.getCounter() <= 1)
			return hashMapRt;
		double trackIDTemp = allPositions.getValueAsDouble(ResultsTableMt.GROUP, row);
		String originTemp = allPositions.getStringValue("Origin", row);
		ResultsTableMt track = new ResultsTableMt();
		Utils.addRow(allPositions, track, row);
		row++;

		while (row < allPositions.getCounter()) {
			if (allPositions.getStringValue("Origin", row).equalsIgnoreCase(originTemp)
					&& allPositions.getValueAsDouble(ResultsTableMt.GROUP, row) == trackIDTemp) {
				stepHist[Math.min((int) (Utils.getDistance(allPositions, row, row - 1) * 1000.0 + 0.5D),
						stepHist.length - 1)]++;
			} else {
				if (track.getCounter() > 1) {
					hashMapRt.put(trajNumber, track);
					trajNumber++;
				}
				track = new ResultsTableMt();
				trackIDTemp = allPositions.getValueAsDouble(ResultsTableMt.GROUP, row);
				originTemp = allPositions.getStringValue("Origin", row);
			}
			Utils.addRow(allPositions, track, row);
			row++;
		}
		if (track.getCounter() > 1) {
			hashMapRt.put(trajNumber, track);
			trajNumber++;
		}

		IJ.log(trajNumber + " trajectories have been retrieved.");

		return hashMapRt;
	}

	private void stepDistribution() {

		// Plot and save the histogram of the step sizes
		double[] stepTemp = new double[stepHist.length];
		for (int i = 0; i < stepTemp.length; i++)
			stepTemp[i] = i;
		Plot hist = new Plot("Histogram", "Step sizes (nm)", "Number of steps");
		hist.setLimits(0, stepTemp[stepTemp.length - 1], 0, Utils.maxMin(stepHist)[0]);
		hist.setColor(Color.BLACK);
		hist.addPoints(stepTemp, stepHist, Plot.CIRCLE);
		hist.draw();
		if (saveTextFile)
			Utils.saveVector(stepHist, directory + File.separator + "4-StepSizeHistogram.txt");

		// Fit it with a log-normal distribution
		double[] stepTemp2 = new double[stepTemp.length - 1];
		double[] stepHist2 = new double[stepTemp.length - 1];
		for (int i = 0; i < stepTemp2.length; i++) {
			stepTemp2[i] = stepTemp[i + 1];
			stepHist2[i] = stepHist[i + 1];
		}
		Regression reg = new Regression(stepTemp2, stepHist2);
		reg.logNormal();
		IJ.log("Log-normal fit: mu = " + reg.getBestEstimates()[0] + "   sigma = " + reg.getBestEstimates()[1]
				+ "   A0 = " + reg.getBestEstimates()[2] + "   R^2 = " + reg.getCoefficientOfDetermination());
		hist.setColor(Color.BLUE);
		hist.addPoints(stepTemp2, reg.getYcalc(), Plot.LINE);
		hist.draw();
		// hist.show();
		Utils.saveTiff(hist.getImagePlus(), directory + File.separator + "4-StepSizeHistogram.tif", false);

	}

	private void populationJD_Analysis(boolean saveTextFile) { // Fill cumulJD with other time steps (1 ..
																// highestTimeStep)
		int highestTimeStep = 11; // TODO

		// Get JD back from each stepFrame's diffusion coefficient
		ParallelJDpopulation par = new ParallelJDpopulation(hashMapRt, stepUM, highestTimeStep, exposure, directory,
				saveTextFile);
		Collection<Integer> elems = new LinkedList<Integer>();
		for (int stepFrame = 1; stepFrame < highestTimeStep; stepFrame++) {
			elems.add(stepFrame);
		}
		Parallel.For(elems, par);

		par.post();
		par.plot();

		// Prepare bootstrap for SR_Imaging_Multi int bootstrapNumber = 5;
		// par.prepareBootstrapTraj(retour, highestTimeStep, param, bootstrapNumber); //
	}

	/**
	 * 
	 * @param showGroup
	 * @param nMSD       - Maximum time interval (in frames) for MSD analysis.
	 * @param trajLength - Only consider trajectories > trajLength.
	 */
	@SuppressWarnings("deprecation")
	public void msd_Analysis(boolean showGroup, int nMSD, int trajLength) {
		int polynomial = 2;
		ResultsTableMt recap = new ResultsTableMt();

		Iterator<Entry<Integer, ResultsTableMt>> it = hashMapRt.entrySet().iterator();
		while (it.hasNext()) {
			Entry<Integer, ResultsTableMt> itTemp = it.next();
			ResultsTableMt traj = itTemp.getValue();

			traj.addValue("MSD_D", 0);
			traj.addValue("MSD_Precision", 0);
			traj.addValue("MSD_R^2", 0);
			if (polynomial == 2)
				traj.addValue("MSD_V", 0);

			if (traj.getCounter() > trajLength) {
				double[] msd = new double[nMSD];
				double[] numberMSD = new double[nMSD];

				for (int row = 0; row < traj.getCounter(); row++) {
					int row2 = row + 1;
					int deltaT;
					while (row2 < traj.getCounter()
							&& (deltaT = (int) (traj.getValueAsDouble(ResultsTableMt.FRAME, row2)
									- traj.getValueAsDouble(ResultsTableMt.FRAME, row))) <= nMSD) {
						msd[deltaT - 1] += Math.pow(Utils.getDistance(traj, row, row2), 2.0);
						numberMSD[deltaT - 1]++;
						row2++;
					}
				}

				double[] msdX = new double[nMSD];
				for (int iMSD = 0; iMSD < nMSD; iMSD++) {
					if (numberMSD[iMSD] != 0)
						msd[iMSD] *= 1.0 / numberMSD[iMSD]; // in um
					msdX[iMSD] = (iMSD + 1.0) * (exposure); // in s
				}

				// Remove all the last points for which there is no msd data
				msd = Utils.removeZeros(msd);
				if (msd != null && msd.length > 3) {
					msdX = Utils.trunc(msdX, 0, msd.length - 1);

					// Directed movement: MSD = 4D * DeltaT + (V * DeltaT)^2
					// Constrained movement : MSD = 4D * DeltaT^alpha (alpha is a constant <1)
					// Freely diffusive movement: MSD = 4D * DeltaT
					Regression regMSD = new Regression(msdX, msd);
					regMSD.linear();
					double[] coeffEvaluated = regMSD.getBestEstimates();

					if (polynomial == 2 && msd.length > 4) {
						Regression regMSD2 = new Regression(msdX, msd);
						regMSD2.polynomial(2);
						double[] coeffEvaluated2 = regMSD2.getBestEstimates();
						if (coeffEvaluated2[1] > 0 && coeffEvaluated2[2] > 0 && (coeffEvaluated[1] < 0
								|| regMSD2.getCoefficientOfDetermination() > regMSD.getCoefficientOfDetermination())) {
							regMSD = regMSD2;
							coeffEvaluated = coeffEvaluated2;
						}
					}

					traj.setValue("MSD_D", 0, coeffEvaluated[1] / 4.0D);
					if (coeffEvaluated[0] > 0)
						traj.setValue("MSD_Precision", 0, Math.sqrt(coeffEvaluated[0]));
					if (coeffEvaluated.length > 2)
						traj.setValue("MSD_V", 0, Math.sqrt(coeffEvaluated[2]));
					traj.setValue("MSD_R^2", 0, regMSD.getCoefficientOfDetermination());
					IJ.log("MSD analysis. D = " + (coeffEvaluated[1] / 4.0D) + " µm^2/s R^2 = "
							+ regMSD.getCoefficientOfDetermination()
							+ ((coeffEvaluated.length > 2) ? (" v = " + coeffEvaluated[2] + " µm/s") : "") + ".");
				}
			}
		}

		// Replot with MSD colours
		if (showGroup) {
			// int pixelsize = 10; // rebuiltScale;

			Plot plot = new Plot("Rebuild trajectories", "µm", "µm");

			plot.setSize(512, 512);
			// plot.setSize(pixelsize * (int) (xYMaxMin[0][0] + 0.5) + Plot.RIGHT_MARGIN +
			// Plot.LEFT_MARGIN,
			// pixelsize * (int) (xYMaxMin[1][0] + 0.5) + Plot.BOTTOM_MARGIN +
			// Plot.TOP_MARGIN);
			plot.setLimits(0, xYMaxMin[0][0], 0, xYMaxMin[1][0]);
			plot.setLineWidth(2);
			plot.setAxes(false, false, false, false, false, false, 0, 0);

			it = hashMapRt.entrySet().iterator();
			while (it.hasNext()) {
				Entry<Integer, ResultsTableMt> itTemp = it.next();
				ResultsTableMt traj = itTemp.getValue();

				int msdColumn = traj.getColumnIndex("MSD_D");

				if (traj.getCounter() >= trajLength) {
					double[] x = traj.getColumnAsDoubles(ResultsTableMt.X);
					double[] y = traj.getColumnAsDoubles(ResultsTableMt.Y);

					if (traj.getValueAsDouble(msdColumn, 0) > 0 && traj.getValue("MSD_R^2", 0) > 0.6) {
						if (traj.getValue("MSD_V", 0) > 0) {
							plot.setColor(Color.LIGHT_GRAY);
							plot.drawVectors(new double[] { x[0] }, new double[] { y[0] },
									new double[] { x[x.length - 1] }, new double[] { y[y.length - 1] });
						}
						plot.setColor(Utils.getGradientColor(Color.RED, Color.BLUE, 4000,
								(int) ((Math.log10(traj.getValueAsDouble(msdColumn, 0)) + 4.0) * 1000.0)));

						recap.incrementCounter();
						recap.addValue("MSD_D", traj.getValue("MSD_D", 0));
						recap.addValue("MSD_Precision", traj.getValue("MSD_Precision", 0));
						recap.addValue("MSD_R^2", traj.getValue("MSD_R^2", 0));
						if (polynomial == 2)
							recap.addValue("MSD_V", traj.getValue("MSD_V", 0));

					} else {
						plot.setColor(Color.BLACK);
					}
					plot.setLineWidth(3);
					plot.addPoints(x, y, Plot.LINE);
					plot.draw();
				}
			}

			plot.show();
			Utils.saveTiff(plot.getImagePlus(), directory + File.separator + "5-RebuildTrajectories_MSD.tif", false);

			Histogram hist = new Histogram(recap.getColumnAsDoubles(recap.getColumnIndex("MSD_D")), 20, true, false,
					"Distribution of MSD diffusion coefficients (µm^2/s)");
			hist.refresh(0.05, true, false);
			hist.plotAndSave(directory + File.separator + "6-MSD_Diffusion_Histogram.tif", false).show();
			IJ.log("The distribution of MSD diffusion coefficients peaks at " + hist.getPeak() + " µm^2/s.");

			Histogram hist2 = new Histogram(recap.getColumnAsDoubles(recap.getColumnIndex("MSD_V")), 10, true, false,
					"Distribution of MSD speed coefficients (µm/s)");
			hist2.refresh(0.3, true, false);
			hist2.plotAndSave(directory + File.separator + "6-MSD_Speed_Histogram.tif", false).show();
			IJ.log("The distribution of MSD speed coefficients peaks at " + hist2.getPeak() + " µm/s.");
		}

	}

}
