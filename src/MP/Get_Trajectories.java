package MP;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import MP.modifs.WalkingAverageMP;
import MP.objects.ResultsTableMt;
import MP.objects.Traj;
import MP.params.ParamAlignTraj;
import MP.params.Params;
import MP.utils.FittingPeakFit;
import MP.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

public class Get_Trajectories extends Align_Trajectories {

	ImagePlus imp;
	int frame;
	String fileDirName;
	boolean save = false;
	private boolean complexSavedTrajectories = false;

	public Get_Trajectories() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(String arg0) {
		IJ.log("MP plugin v." + Params.version);

		imp = IJ.getImage();
		if (imp == null) {
			IJ.log("Please open an movie of the cells to be analysed before running the Get_Trajectories plugin.");
			return;
		}
		fileDirName = imp.getOriginalFileInfo().directory;
		String fileName = imp.getOriginalFileInfo().fileName;
		if (fileDirName == null || fileDirName == "" || fileName == null || fileName == ""
				|| imp.getOriginalFileInfo().fileName.lastIndexOf(".tif") == -1)
			fileDirName = Utils.getADir("Get a directory to save results", "", "").getAbsolutePath();
		else
			fileDirName = fileDirName + File.separator + fileName.substring(0, fileName.lastIndexOf(".tif"))
					+ "_GetTrajectories";
		setDirectory(new File(fileDirName), TRAJ);

		// Apply a FFT bandpass filter to remove all features < 1 pix and > 10 pix in
		// frequency.
		IJ.run("Bandpass Filter...", "filter_large=10 filter_small=1 suppress=None tolerance=5 process");

		// Smooth the movie thanks to a walking average filter over 3 frames.
		WalkingAverageMP avg = new WalkingAverageMP();
		avg.run("3");

		IJ.selectWindow("walkAv");
		IJ.run("Enhance Contrast", "saturated=0.35");

		// Run PeakFit to get as many localisations as possible
		/*
		 * IJ.run("Peak Fit",
		 * "template=[None] config_file=C:\\Users\\matth\\gdsc.smlm.settings.xml calibration=100 gain=400 exposure_time=750 initial_stddev0=1.000 initial_stddev1=1.000 initial_angle=0.000 spot_filter_type=Single spot_filter=Mean smoothing=1.20 search_width=1 border=1 fitting_width=3 fit_solver=[Least Squares Estimator (LSE)] fit_function=[Free circular] fail_limit=10 neighbour_height=0.30 residuals_threshold=1 duplicate_distance=0.50 shift_factor=2 signal_strength=0 min_photons=0 min_width_factor=0.15 width_factor=5 precision=0 show_deviations results_table=Uncalibrated image=[Signal (width=precision)] weighted equalised image_precision=5 image_scale=1 results_dir=[] results_in_memory fit_criteria=[Least-squared error] significant_digits=5 coord_delta=0.0001 lambda=10.0000 max_iterations=20 stack"
		 * );
		 */

		IJ.selectWindow("walkAv");
		imp = IJ.getImage();
		imp.show();
		imp.getWindow().setVisible(true);
		imp.setRoi(new Rectangle(0, 0, this.imp.getWidth(), this.imp.getHeight()));
		imp.unlock();
		if (save)
			Utils.saveTiff(this.imp, fileDirName + File.separator + "walkAverage.tif", false);

		FittingPeakFit fitting = new FittingPeakFit();
		fitting.setConfig(fileDirName);
		fitting.fitImage(this.imp);
		ResultsTableMt rtFit = fitting.getResults();
		// Sort Rt following the "frame" column - for the further groupInTrajectories
		// step.
		rtFit = Utils.sortRt(rtFit, ResultsTableMt.FRAME);
		rtFit.saveAsPrecise(fileDirName + File.separator + "TableFit_PeakFit.txt", 10);

		// Filter localisations (width and intensity) and build trajectories (distance
		// and dark time) while plotting them in real time.
		ParamAlignTraj selectParams = new ParamAlignTraj(rtFit, imp.getImageStack());
		selectParams.run();

		double minIntensity = selectParams.params.minIntensity; // 200;
		double minSigma = selectParams.params.minSigma;// 1.25;
		double maxSigma = selectParams.params.maxSigma;// 2.75;
		selectParams.params.save(fileDirName);
		long startTime = System.nanoTime();
		rtFit = filterLoc(rtFit, minIntensity, minSigma, maxSigma);

		double maxStepPix = selectParams.params.maxStepPix;// 3;
		int maxDarkTimeFrame = selectParams.params.maxDarkTimeFrame;// 4;
		int minNumberOfLocPerTraj = selectParams.params.minNumberOfLocPerTraj;// 3;
		ResultsTableMt[] trajs = groupInTrajectories(rtFit, maxStepPix, maxDarkTimeFrame, minNumberOfLocPerTraj,
				FittingPeakFit.pixelSize, true);

		ResultsTableMt avgTrajDirections = calculateAvgTrajDirection(trajs, imp.getImageStackSize(), true);

		plotTrajs(this.imp, trajs, avgTrajDirections);
		long endTime = System.nanoTime();
		IJ.log("Elapsed time for trajectory update: " + (endTime - startTime) / 1000000000.0 + " s.");

		// Analyse trajectories...
		// Link with Align_Trajectories, by making Get_Trajectories an extension of
		// Align_Trajectories.
		// TODO : cell tracking to properly analyse REAR and FRONT tracks...
		analyseTheTracks(false, false, false, true);

		IJ.log("Beginning the MSD and JD analysis...");
		diffusionAnalysis(trajs, rtFit, minNumberOfLocPerTraj);
		endTime = System.nanoTime();
		IJ.log("All done! - " + (endTime - startTime) / 1000000000.0 + " s.");
	}

	public ResultsTableMt calculateAvgTrajDirection(ResultsTableMt[] trajs, int frameNumber, boolean save) {
		// For each frame, list the directions (vectors) of all the trajectories in it
		ResultsTableMt[] trajDirections = new ResultsTableMt[frameNumber - 1];
		// Initiate the object
		for (int fr = 0; fr < trajDirections.length; fr++) {
			trajDirections[fr] = new ResultsTableMt();
		}
		// Populate the object
		for (int traj = 3; traj < trajs.length; traj++) {
			for (int row = 0; row < trajs[traj].getCounter() - 1; row++) {
				int fr = (int) trajs[traj].getValueAsDouble(ResultsTableMt.FRAME, row) - 1;
				trajDirections[fr].incrementCounter();
				trajDirections[fr].addValue(ResultsTableMt.X, trajs[traj].getValueAsDouble(ResultsTableMt.X, row + 1)
						- trajs[traj].getValueAsDouble(ResultsTableMt.X, row));
				trajDirections[fr].addValue(ResultsTableMt.Y, trajs[traj].getValueAsDouble(ResultsTableMt.Y, row + 1)
						- trajs[traj].getValueAsDouble(ResultsTableMt.Y, row));
			}
		}
		ResultsTableMt avgTrajDirections = new ResultsTableMt();
		for (int fr = 0; fr < trajDirections.length; fr++) {
			avgTrajDirections.incrementCounter();
			avgTrajDirections.addValue(ResultsTableMt.FRAME, fr + 1);
			double[] meanX = Utils.getMeanAndStdev(trajDirections[fr], ResultsTableMt.X, true);
			if (meanX != null) {
				avgTrajDirections.addValue(ResultsTableMt.X, meanX[0]);
				avgTrajDirections.addValue("Stdev_X", meanX[1]);
			}
			double[] meanY = Utils.getMeanAndStdev(trajDirections[fr], ResultsTableMt.Y, true);
			if (meanY != null) {
				avgTrajDirections.addValue(ResultsTableMt.Y, meanY[0]);
				avgTrajDirections.addValue("Stdev_Y", meanY[1]);
			}
			avgTrajDirections.addValue("#ofTrajs", trajDirections[fr].getCounter());
		}

		if (save) {
			avgTrajDirections.saveAsPrecise(fileDirName + File.separator + "AvgTrajectory.txt", 10);
		}

		// Plot the average trajectory in a different window, in black
		// Plot below all the superposed trajectories, colour-coded with their stdev to
		// the avg
		double[][][] pols = new double[trajs.length - 3][][];

		// Determine the avgTraj
		double[][] avgPol = new double[2][];
		avgPol[0] = new double[trajDirections.length + 1];
		avgPol[1] = new double[avgPol[0].length];
		avgPol[0][0] = 0;
		avgPol[1][0] = 0;
		for (int fr = 1; fr < avgPol[0].length; fr++) {
			avgPol[0][fr] = avgPol[0][fr - 1] + avgTrajDirections.getValueAsDouble(ResultsTableMt.X, fr - 1);
			avgPol[1][fr] = avgPol[1][fr - 1] + avgTrajDirections.getValueAsDouble(ResultsTableMt.Y, fr - 1);
		}
		// Initialise pols
		for (int i = 0; i < pols.length; i++) {
			pols[i] = new double[2][];
			pols[i][0] = avgPol[0].clone();
			pols[i][1] = avgPol[1].clone();
		}
		// Fill pols: for each trajectory, with avgTraj before the trajectory begins,
		// then with the trajectory, and with the final value of the trajectory after it
		// ends.
		// At the same time, calculate the Stdev of each traj to the avgPol
		double[] stdev = new double[pols.length];
		for (int traj = 3; traj < trajs.length; traj++) {
			int frame = 0;
			for (int row = 1; row < trajs[traj].getCounter(); row++) {
				frame = (int) trajs[traj].getValueAsDouble(ResultsTableMt.FRAME, row) - 1;
				double xDiff = trajs[traj].getValueAsDouble(ResultsTableMt.X, row)
						- trajs[traj].getValueAsDouble(ResultsTableMt.X, row - 1);
				pols[traj - 3][0][frame] = pols[traj - 3][0][frame - 1] + xDiff;
				double yDiff = trajs[traj].getValueAsDouble(ResultsTableMt.Y, row)
						- trajs[traj].getValueAsDouble(ResultsTableMt.Y, row - 1);
				pols[traj - 3][1][frame] = pols[traj - 3][1][frame - 1] + yDiff;

				stdev[traj - 3] += Math.pow(xDiff - avgTrajDirections.getValueAsDouble(ResultsTableMt.X, frame - 1), 2)
						+ Math.pow(yDiff - avgTrajDirections.getValueAsDouble(ResultsTableMt.Y, frame - 1), 2);

				frame++;
				while ((row + 1 < trajs[traj].getCounter()
						&& frame < trajs[traj].getValueAsDouble(ResultsTableMt.FRAME, row + 1) - 1)
						|| (row + 1 == trajs[traj].getCounter() && frame < pols[traj - 3][0].length)) {
					pols[traj - 3][0][frame] = pols[traj - 3][0][frame - 1];
					pols[traj - 3][1][frame] = pols[traj - 3][1][frame - 1];
					frame++;
				}
			}
			stdev[traj - 3] = Math.sqrt(stdev[traj - 3] / (trajs[traj].getCounter() - 1.0));
		}
		// Calculate the limits of the plots
		double xMax = 0;
		double yMax = 0;
		for (int i = 0; i < pols.length; i++) {
			double[] temp = Utils.getMinMax(pols[i][0]);
			xMax = Math.max(xMax, Math.max(-temp[0], temp[1]));
			temp = Utils.getMinMax(pols[i][1]);
			yMax = Math.max(yMax, Math.max(-temp[0], temp[1]));
		}
		double limit = ((int) (Math.max(xMax, yMax) / 5.0) + 1) * 5;

		// Plot the cumulative trajectories
		Plot plot = new Plot("Detected trajectories", "x (pixel)", "y (pixel)");
		plot.setLimits(-limit, limit, -limit, limit);
		plot.setSize(700, 700);
		for (int frame = 1; frame < avgPol[0].length; frame++) {
			IJ.showStatus("Plotting trajectory directions");
			IJ.showProgress(frame, avgPol[0].length);

			double maxStdev = Utils.getMax(stdev);
			plot.setLineWidth(1);
			for (int traj = 0; traj < pols.length; traj++) {
				plot.setColor(
						Utils.getGradientColor(Color.GREEN, Color.RED, 100, (int) (stdev[traj] / maxStdev * 100.0)));
				for (int frameBelow = Math.max(1, frame - 10); frameBelow <= frame; frameBelow++) {
					if (pols[traj][0][frameBelow - 1] != pols[traj][0][frameBelow]
							|| pols[traj][1][frameBelow - 1] != pols[traj][1][frameBelow])
						plot.drawLine(pols[traj][0][frameBelow - 1], pols[traj][1][frameBelow - 1],
								pols[traj][0][frameBelow], pols[traj][1][frameBelow]);
				}
			}
			plot.setColor(Color.BLACK);
			plot.setLineWidth(2);
			for (int frameBelow = 1; frameBelow <= frame; frameBelow++) {
				plot.drawLine(avgPol[0][frameBelow - 1], avgPol[1][frameBelow - 1], avgPol[0][frameBelow],
						avgPol[1][frameBelow]);
			}
			plot.addToStack();
		}

		plot.show();
		Utils.saveTiff(plot.getImagePlus(), fileDirName + File.separator + "AvgTrajectory.tiff", false);
		this.stdev = stdev;

		return avgTrajDirections;
	}

	public static ResultsTableMt filterLoc(ResultsTableMt rtFit, double minIntensity, double minSigma,
			double maxSigma) {
		ResultsTableMt rt = new ResultsTableMt();
		final int SIGMAX = rtFit.getColumnIndex("SigmaX");
		final int SIGMAY = rtFit.getColumnIndex("SigmaY");
		for (int row = 0; row < rtFit.getCounter(); row++) {
			double sigmaX = rtFit.getValueAsDouble(SIGMAX, row);
			double sigmaY = rtFit.getValueAsDouble(SIGMAY, row);
			if (rtFit.getValueAsDouble(ResultsTableMt.INTENSITY, row) >= minIntensity && sigmaX >= minSigma
					&& sigmaX <= maxSigma && sigmaY >= minSigma && sigmaY <= maxSigma) {
				Utils.addRow(rtFit, rt, row);
			}

		}
		return rt;
	}

	public ResultsTableMt[] groupInTrajectories(ResultsTableMt rt_Sorted, double maxStepPix, int maxDarkTimeFrame,
			int minNumberOfLocPerTraj, int pixelSize, boolean save) {

		// Trajectories: linking the name of the last Loc of each traj to its traj
		Hashtable<Integer, Traj> trajectories = new Hashtable<Integer, Traj>();

		// Sort Rt following the "frame" column
		// ResultsTableMt rt_Sorted = Utils.sortRt(rt, ResultsTableMt.FRAME); // - Done
		// previously.

		// Build trajectories
		buildTrajectories(rt_Sorted, maxStepPix, maxDarkTimeFrame, pixelSize, trajectories);

		// Extract trajectories from 'trajectories' into 'retour[2...end]' and into
		// finalHashMap (from Align_Trajectories ; in a single "SingleCell" cell)
		ResultsTableMt[] retour = extractAndFilterTrajectories(trajectories, rt_Sorted, minNumberOfLocPerTraj);

		if (save && fileDirName != null) {
			rt_Sorted.saveAsPrecise(fileDirName + File.separator + "TableFit_Sorted.txt", 10);

			Utils.sortRt(rt_Sorted, ResultsTableMt.GROUP)
					.saveAsPrecise(fileDirName + File.separator + "Table_Trajectories.txt", 10);

			if (complexSavedTrajectories) {
				// Save all trajectories in a single .csv file on consecutive triplets of
				// columns [frame_traj_# x_traj_# y_traj_#]
				ResultsTableMt saveTraj = new ResultsTableMt();
				String columnFrame0 = "Frame_Traj_";
				String columnX0 = "X_Traj_";
				String columnY0 = "Y_Traj_";
				int tableCounter = 1;

				for (int traj = 0; traj < retour.length - 3; traj++) {
					String columnFrame = columnFrame0 + traj;
					String columnX = columnX0 + traj;
					String columnY = columnY0 + traj;

					// A ResultsTable cannot contain more than 150 columns, so results are split
					if (traj % 40 == 0 && traj > 0) {
						saveTraj.saveAsPrecise(fileDirName + File.separator + "ListOfTrajectories-"
								+ ((tableCounter - 1) * 40) + "-" + ((tableCounter) * 40 - 1) + ".csv", 10);
						tableCounter++;
						saveTraj = new ResultsTableMt();
					}

					for (int row = 0; row < retour[traj + 3].getCounter(); row++) {
						while (saveTraj.getCounter() - 1 < row)
							saveTraj.addRow();
						saveTraj.setValue(columnFrame, row,
								retour[traj + 3].getValueAsDouble(ResultsTableMt.FRAME, row));
						saveTraj.setValue(columnX, row, retour[traj + 3].getValueAsDouble(ResultsTableMt.X, row));
						saveTraj.setValue(columnY, row, retour[traj + 3].getValueAsDouble(ResultsTableMt.Y, row));
					}
				}
				saveTraj.saveAsPrecise(fileDirName + File.separator + "ListOfTrajectories-" + +((tableCounter - 1) * 40)
						+ "-" + (retour.length - 4) + ".csv", 10);
			}
		}

		return retour;
	}

	private static double[] buildTrajectories(ResultsTableMt rt_Sorted, double stepPix, int memory, int pixelSize,
			Hashtable<Integer, Traj> trajectories) {
		boolean mergeNotEnd = true;// false; //TODO
		boolean keepClosest = false;
		boolean keepLongest = !keepClosest;

		IJ.showStatus("Building trajectories...");

		int[] memoryRows = new int[memory];
		double[] stepHist = new double[(int) (stepPix * pixelSize + 0.5D) + 1];

		int row = 0;
		while (rt_Sorted.getValueAsDouble(ResultsTableMt.FRAME, row) < 1)
			row++;
		memoryRows[1 % memory] = row;

		int endFrame = (int) rt_Sorted.getValueAsDouble(ResultsTableMt.FRAME, rt_Sorted.getCounter() - 1);
		// For frame 2 till end
		for (int frame = 2; frame < endFrame; frame++) {
			if (frame % 5 == 0)
				IJ.showProgress(frame, endFrame);

			while (rt_Sorted.getValueAsDouble(ResultsTableMt.FRAME, row) < frame)
				row++;
			memoryRows[frame % memory] = row;

			// For each correctly fitted PSF:
			while (rt_Sorted.getValueAsDouble(ResultsTableMt.FRAME, row) == frame) {
				if (rt_Sorted.getValue("isFitted", row) > 0) {

					// - find all PSFs from previous frame closer from it than step
					int psf = 0;

					// - if no PSF, look at previous-i frame (i<3)
					for (int back = 0; back < memory && psf == 0 && back < frame - 1; back++) {
						for (int row2 = memoryRows[(frame - back - 1) % memory]; row2 < memoryRows[(frame - back)
								% memory]; row2++) {
							if (rt_Sorted.getValue("isFitted", row2) > 0
									&& Utils.getDistance(rt_Sorted, row, row2) < stepPix) {
								psf++;
								stepHist[(int) (Utils.getDistance(rt_Sorted, row, row2) * pixelSize + 0.5D)]++;

								if (!trajectories.containsKey(row)) { // - if 1 PSF, create or add it to a trajectory
									if (trajectories.containsKey(row2) && !trajectories.get(row2).Ended) {
										trajectories.put(row, trajectories.get(row2));
										trajectories.remove(row2);
										trajectories.get(row).list.add(row);
									} else {
										trajectories.put(row, new Traj(row2));
										trajectories.get(row).list.add(row);
									}
								} else { // - if more PSFs, add it to their trajectories and end them
									if (trajectories.containsKey(row2)) {
										if (!trajectories.get(row2).Ended) {
											trajectories.get(row2).list.add(row);
											trajectories.get(row2).Ended = true;
										} else {
											// Forget it, linking ended merged trajectories other merging...
										}
									} else {
										trajectories.put(row2, new Traj(row2));
										trajectories.get(row2).list.add(row);
										trajectories.get(row2).Ended = true;
									}
									trajectories.get(row).Ended = true;

									if (mergeNotEnd) {
										int row2bis = trajectories.get(row).list
												.get(trajectories.get(row).list.size() - 2);
										if (row2bis != row2 && (keepClosest
												&& Utils.getDistance(rt_Sorted, row, row2) < Utils
														.getDistance(rt_Sorted, row, row2bis)
												|| keepLongest && trajectories.get(row).list
														.size() < trajectories.get(row2).list.size())) {
											trajectories.put(row2bis, trajectories.get(row));
											trajectories.remove(row);
											trajectories.put(row, trajectories.get(row2));
											trajectories.remove(row2);
											trajectories.get(row2bis).Ended = false;
										} else
											trajectories.get(row2).Ended = false;
										trajectories.get(row).Ended = false;
									}
								}
							}
						}
					}
				}

				row++;
			}
		}
		IJ.showProgress(endFrame, endFrame);

		return stepHist;
	}

	private ResultsTableMt[] extractAndFilterTrajectories(Hashtable<Integer, Traj> trajectories,
			ResultsTableMt rt_Sorted, int minNumberOfLocPerTraj) {
		// Remove all trajectories which count less localisations than
		// minNumberOfLocPerTraj.
		Iterator<Entry<Integer, Traj>> it0 = trajectories.entrySet().iterator();
		while (it0.hasNext()) {
			Entry<Integer, Traj> temp = it0.next();
			if (temp.getValue().list.size() < minNumberOfLocPerTraj) {
				it0.remove();
			}
		}

		// Extract the remaining trajectories.
		ResultsTableMt[] retour = new ResultsTableMt[trajectories.size() + 3];
		ResultsTableMt groupSizes = new ResultsTableMt();
		rt_Sorted.addValue(ResultsTableMt.GROUP, 0);
		final int GROUPSIZE_Sorted = rt_Sorted.addNewColumn("GroupSize");
		groupSizes.incrementCounter();
		final int GROUPSIZE_groupSizes = groupSizes.addNewColumn("GroupSize");

		finalHashMap = new HashMap<String, CellContainer>();
		CellContainer allTrajs = new CellContainer(new ResultsTableMt());
		finalHashMap.put("SingleCell", allTrajs);

		Iterator<Entry<Integer, Traj>> it = trajectories.entrySet().iterator();
		int traj = 0;
		while (it.hasNext()) {
			retour[traj + 3] = new ResultsTableMt();

			LinkedList<Integer> trajList = it.next().getValue().list;
			double[] x = new double[trajList.size()];
			double[] y = new double[trajList.size()];
			while (groupSizes.getCounter() < traj + 1)
				groupSizes.incrementCounter();
			groupSizes.setValue(GROUPSIZE_groupSizes, traj, trajList.size());
			// IJ.log("Traj # "+(traj+1)+" Size: " + trajList.size());

			Iterator<Integer> itList = trajList.iterator();
			int i = 0;
			while (itList.hasNext()) {
				int loc = itList.next();
				// IJ.log(""+loc);
				x[i] = rt_Sorted.getValueAsDouble(ResultsTableMt.X, loc);
				y[i] = rt_Sorted.getValueAsDouble(ResultsTableMt.Y, loc);

				Utils.addRow(rt_Sorted, retour[traj + 3], loc);
				rt_Sorted.setValue(ResultsTableMt.GROUP, loc, traj + 1);
				rt_Sorted.setValue(GROUPSIZE_Sorted, loc, trajList.size());
				i++;
			}

			allTrajs.trajTracks.put(traj, retour[traj + 3]);

			traj++;
		}

		retour[0] = groupSizes;
		return retour;
	}

	public static void plotTrajs(ImagePlus imp, ResultsTableMt[] trajs, ResultsTableMt avgTrajDirections) {
		Overlay ov = new Overlay();
		ImageCanvas ic = imp.getCanvas();
		if (ic != null)
			ov = ic.getShowAllList();
		if (ov == null)
			ov = imp.getOverlay();
		if (ov == null)
			ov = new Overlay();
		imp.setOverlay(ov);

		for (int traj = 3; traj < trajs.length; traj++) {
			if (traj % 10 == 3) {
				IJ.showStatus("Plotting trajectory " + (traj - 3) + " of " + (trajs.length - 3));
				IJ.showProgress(traj - 3, trajs.length - 3);
			}

			int firstFrame = (int) trajs[traj].getValueAsDouble(ResultsTableMt.FRAME, 0);
			int lastFrame = (int) trajs[traj].getValueAsDouble(ResultsTableMt.FRAME, trajs[traj].getCounter() - 1);
			int loc = 0;

			for (int frame = firstFrame; frame <= lastFrame; frame++) {
				FloatPolygon pol = new FloatPolygon();
				PointRoi roiDot = new PointRoi(trajs[traj].getValueAsDouble(ResultsTableMt.X, loc),
						trajs[traj].getValueAsDouble(ResultsTableMt.Y, loc));

				for (int locRow = 0; locRow <= loc; locRow++) {
					pol.addPoint(trajs[traj].getValueAsDouble(ResultsTableMt.X, locRow),
							trajs[traj].getValueAsDouble(ResultsTableMt.Y, locRow));
				}

				PolygonRoi polRoi = new PolygonRoi(pol, Roi.POLYLINE);
				polRoi.setStrokeColor(Color.RED);
				polRoi.setStrokeWidth(1.5);
				polRoi.setPosition(frame + 1);

				roiDot.setStrokeColor(Color.RED);
				roiDot.setPosition(frame + 1);
				roiDot.setPointType(3);

				ov.add(polRoi);
				ov.add(roiDot);

				if (loc < trajs[traj].getCounter() - 2
						&& trajs[traj].getValueAsDouble(ResultsTableMt.FRAME, loc + 1) == frame + 1)
					loc++;
			}
		}

		// if (avgTrajDirections != null)
		// plotTheAvgTrajectory(imp, trajs, avgTrajDirections, ov);

		imp.setHideOverlay(false);
		imp.draw();
	}

	public static void plotTrajs(ImagePlus imp, ResultsTableMt[] trajs) {
		plotTrajs(imp, trajs, null);
	}

	private static void plotTheAvgTrajectory(ImagePlus imp, ResultsTableMt[] trajs, ResultsTableMt avgTrajDirections,
			Overlay ov) {
		// PLOT THE AVERAGE TRAJECTORY
		// Get the initialise estimated position of the cell (average positions of all
		// trajectories in the first frame)
		double x0 = 0;
		double y0 = 0;
		int trajNumber = 0;
		for (int traj = 3; traj < trajs.length; traj++) {
			if (trajs[traj].getValueAsDouble(ResultsTableMt.FRAME, 0) == 1) {
				x0 += trajs[traj].getValueAsDouble(ResultsTableMt.X, 0);
				y0 += trajs[traj].getValueAsDouble(ResultsTableMt.Y, 0);
				trajNumber++;
			}
		}
		if (trajNumber > 0) {
			x0 /= (trajNumber);
			y0 /= (trajNumber);
		}

		// Plot the average trajectory of the fitted trajectories, colour-coding it with
		// its stdev
		double xTemp = x0;
		double yTemp = y0;
		for (int fr = 0; fr < imp.getImageStackSize(); fr++) {
			xTemp += ((fr > 0) ? avgTrajDirections.getValueAsDouble(ResultsTableMt.X, fr - 1) : 0);
			yTemp += ((fr > 0) ? avgTrajDirections.getValueAsDouble(ResultsTableMt.Y, fr - 1) : 0);

			FloatPolygon pol = new FloatPolygon();
			PointRoi roiDot = new PointRoi(xTemp, yTemp);

			double x1Temp = x0;
			double y1Temp = y0;
			for (int frLower = 0; frLower <= fr; frLower++) {
				pol.addPoint(x1Temp, y1Temp);
				if (frLower < avgTrajDirections.getCounter()) {
					x1Temp += avgTrajDirections.getValueAsDouble(ResultsTableMt.X, frLower);
					y1Temp += avgTrajDirections.getValueAsDouble(ResultsTableMt.Y, frLower);
				}
			}

			PolygonRoi polRoi = new PolygonRoi(pol, Roi.POLYLINE);
			polRoi.setStrokeColor(Color.BLUE);
			polRoi.setStrokeWidth(1.5);
			polRoi.setPosition(fr + 1);

			roiDot.setStrokeColor(Color.BLUE);
			roiDot.setPosition(fr + 1);
			roiDot.setPointType(3);

			ov.add(polRoi);
			ov.add(roiDot);
		}
	}

	public void diffusionAnalysis(ResultsTableMt[] trajs, ResultsTableMt rt, int minNumberOfLocPerTraj) {
		double[][] xYMaxMin = new double[2][];
		xYMaxMin[0] = Utils.maxMin(rt.getColumnAsDoubles(ResultsTableMt.X));
		xYMaxMin[1] = Utils.maxMin(rt.getColumnAsDoubles(ResultsTableMt.Y));

		Analyse_Trajectories analysis = new Analyse_Trajectories(0.1, 0.1, xYMaxMin); // 1 frame = 0.1 s ; 1 pix = 0.1
																						// µm. //TODO
		analysis.directory = new File(fileDirName);

		// Populate hashMapRt
		analysis.hashMapRt = new HashMap<Integer, ResultsTableMt>();
		for (int traj = 0; traj < trajs.length - 3; traj++) {
			analysis.hashMapRt.put(traj, trajs[traj + 3]);
		}

		analysis.msd_Analysis(true, 10, minNumberOfLocPerTraj);

		analysis.populationJD_Analysis(saveTextFile);
	}

}
