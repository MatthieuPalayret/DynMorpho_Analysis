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

		// Ask where to save the processed files
		fileDirName = Utils.getADir("Get a directory to save results", "", "").getAbsolutePath();
		setDirectory(new File(fileDirName), TRAJ);
		new File(fileDirName).mkdir();

		// Pre-processing of the movie
		{
			// Apply a FFT bandpass filter to remove all features < 1 pix and > 10 pix in
			// frequency.
			IJ.run("Bandpass Filter...", "filter_large=10 filter_small=1 suppress=None tolerance=5 process");

			// Smooth the movie thanks to a walking average filter over 3 frames.
			WalkingAverageMP avg = new WalkingAverageMP();
			avg.run("3");

			IJ.selectWindow("walkAv");
			IJ.run("Enhance Contrast", "saturated=0.35");
		}

		// Run PeakFit to get as many localisations as possible
		ResultsTableMt rtFit = new ResultsTableMt();
		{
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
			rtFit = fitting.getResults();
		}

		// Sort following the "frame" column - for the further groupInTrajectories step.
		rtFit = Utils.sortRt(rtFit, ResultsTableMt.FRAME);
		rtFit.saveAsPrecise(fileDirName + File.separator + "TableFit_PeakFit.txt", 10);

		// Filter localisations (width and intensity) and build trajectories (distance
		// and dark time) while plotting them in real time.
		ParamAlignTraj selectParams = new ParamAlignTraj(rtFit, imp.getImageStack());
		selectParams.run();
		long startTime = System.nanoTime();

		{
			double minIntensity = selectParams.params.minIntensity; // 200;
			double minSigma = selectParams.params.minSigma;// 1.25;
			double maxSigma = selectParams.params.maxSigma;// 2.75;
			selectParams.params.save(fileDirName);
			rtFit = filterLoc(rtFit, minIntensity, minSigma, maxSigma);
		}

		double maxStepPix = selectParams.params.maxStepPix;// 3;
		int maxDarkTimeFrame = selectParams.params.maxDarkTimeFrame;// 4;
		int minNumberOfLocPerTraj = selectParams.params.minNumberOfLocPerTraj;// 3;
		ResultsTableMt[] trajs = groupInTrajectories(rtFit, maxStepPix, maxDarkTimeFrame, minNumberOfLocPerTraj, true);

		// ANALYSIS 1 - Angle of the trajectories to the average direction
		ResultsTableMt avgTrajDirections = calculateAvgTrajDirection(trajs, imp.getImageStackSize(), true, false);

		plotTrajs(this.imp, trajs, avgTrajDirections);
		long endTime = System.nanoTime();
		IJ.log("Elapsed time for trajectory update: " + (endTime - startTime) / 1000000000.0 + " s.");

		// Analyse trajectories...
		// Link with Align_Trajectories, by making Get_Trajectories an extension of
		// Align_Trajectories.
		// TODO : cell tracking to properly analyse REAR and FRONT tracks...
		analyseTheTracks(false, false, false, true);

		// correctForCellDisplacement(trajs, imp.getImageStackSize(), true);
		pairwiseDistance(trajs, true);

		IJ.log("Beginning the MSD and JD analysis...");
		diffusionAnalysis(trajs, rtFit, minNumberOfLocPerTraj);

		endTime = System.nanoTime();
		IJ.log("All done! - " + (endTime - startTime) / 1000000000.0 + " s.");
	}

	public ResultsTableMt calculateAvgTrajDirection(ResultsTableMt[] trajs, int frameNumber, boolean save,
			boolean onlyUpdateAngle) {
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

		// ANGLE CALCULATION
		// Calculate the average angle theta between each trajectory and the average
		// trajectory (averaged over the timepoints)
		double[] avgThetaPerTraj = new double[trajs.length - 3];
		for (int traj = 3; traj < trajs.length; traj++) {
			for (int row = 0; row < trajs[traj].getCounter() - 1; row++) {
				int fr = (int) trajs[traj].getValueAsDouble(ResultsTableMt.FRAME, row) - 1;
				// First vector = traj(row+1) - traj(row)
				double x1 = trajs[traj].getValueAsDouble(ResultsTableMt.X, row + 1)
						- trajs[traj].getValueAsDouble(ResultsTableMt.X, row);
				double y1 = trajs[traj].getValueAsDouble(ResultsTableMt.Y, row + 1)
						- trajs[traj].getValueAsDouble(ResultsTableMt.Y, row);
				// Second vector = avg(traj(row+1) - traj(row)) =
				// avgTrajDirections[frame(row)-1]
				double x2 = avgTrajDirections.getValueAsDouble(ResultsTableMt.X, fr);
				double y2 = avgTrajDirections.getValueAsDouble(ResultsTableMt.Y, fr);
				// angle(A->B) = atan2(vectorB.y, vectorB.x) - atan2(vectorA.y, vectorA.x); A=2
				// ; B=1 ;
				double theta = Math.atan2(y1, x1) - Math.atan2(y2, x2);
				while (theta > Math.PI)
					theta -= 2.0 * Math.PI;
				while (theta < -Math.PI)
					theta += 2.0 * Math.PI;
				avgThetaPerTraj[traj - 3] += theta;
			}
			avgThetaPerTraj[traj - 3] /= (trajs[traj].getCounter() - 1);
		}

		// Plot the angles theta
		Plot plot2 = new Plot("Average angle of each trajectory related to the average trajectory", "", "");
		plot2.setLimits(-1, 1, -1, 1);
		plot2.setSize(700, 700);
		plot2.setLineWidth(1);
		plot2.setColor(Color.BLACK);

		// Colour the vector depending on the local density calculated +/- 5°
		double radius = Math.toRadians(5);
		double[] density = new double[avgThetaPerTraj.length];
		for (int traj = 0; traj < avgThetaPerTraj.length; traj++) {
			for (int traj2 = 0; traj2 < avgThetaPerTraj.length; traj2++) {
				if (traj2 != traj && Math.abs(avgThetaPerTraj[traj] - avgThetaPerTraj[traj2]) < radius)
					density[traj]++;
			}
		}
		int max = (int) MP.utils.Utils.getMax(density);

		for (int traj = 0; traj < avgThetaPerTraj.length; traj++) {
			plot2.setColor(MP.utils.Utils.getGradientColor(Color.GREEN, Color.RED, max + 1, (int) density[traj]));
			plot2.drawLine(0, 0, Math.cos(avgThetaPerTraj[traj]), Math.sin(avgThetaPerTraj[traj]));
		}

		plot2.setLineWidth(3);
		plot2.setColor(Color.BLACK);
		plot2.drawLine(0, 0, 1, 0);

		plot2.show();
		Utils.saveTiff(plot2.getImagePlus(), fileDirName + File.separator + "AvgAngles.tiff", false);
		this.angle = avgThetaPerTraj;

		// BACK TO PLOTTING THE AVERAGE TRAJECTORY
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

		if (!onlyUpdateAngle) {
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
					plot.setColor(Utils.getGradientColor(Color.GREEN, Color.RED, 100,
							(int) (stdev[traj] / maxStdev * 100.0)));
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
		}
		this.stdev = stdev;

		return avgTrajDirections;
	}

	public void correctForCellDisplacement(ResultsTableMt[] trajs, int frameNumber, boolean save) {
		// [1] Calculation of the cell displacement as the average displacement of
		// consecutive trajectories.
		double[][] averageDisplacement = new double[frameNumber - 1][2];
		HashMap<Integer, Integer> listTrajInPreviousFrame = listTrajInFrame(trajs, 0);

		for (int frame = 1; frame < frameNumber; frame++) {
			HashMap<Integer, Integer> listTrajInFrame = listTrajInFrame(trajs, frame);
			Iterator<Integer> it = listTrajInPreviousFrame.keySet().iterator();
			double counter = 0;
			// For all the trajectories present in frame-1, if they are also present in
			// frame, include them to calculate the average displacement of the cell.
			while (it.hasNext()) {
				int traj = it.next();
				if (listTrajInFrame.containsKey(traj)) {
					averageDisplacement[frame - 1][0] += trajs[traj].getValueAsDouble(ResultsTableMt.X,
							listTrajInFrame.get(traj))
							- trajs[traj].getValueAsDouble(ResultsTableMt.X, listTrajInPreviousFrame.get(traj));
					averageDisplacement[frame - 1][1] += trajs[traj].getValueAsDouble(ResultsTableMt.Y,
							listTrajInFrame.get(traj))
							- trajs[traj].getValueAsDouble(ResultsTableMt.Y, listTrajInPreviousFrame.get(traj));
					counter++;
				}
			}
			if (counter > 0)
				averageDisplacement[frame - 1] = Utils.divide(averageDisplacement[frame - 1], counter);

			// Update listTrajInPreviousFrame for next cycle
			listTrajInPreviousFrame = listTrajInFrame;
		}

		// [2] Correct the position of the trajectories for the cell displacement
		double[][] correctionToApply = new double[averageDisplacement.length][2];
		correctionToApply[0][0] = averageDisplacement[0][0];
		correctionToApply[0][1] = averageDisplacement[0][1];
		for (int i = 1; i < correctionToApply.length; i++) {
			correctionToApply[i][0] = averageDisplacement[i][0] + correctionToApply[i - 1][0];
			correctionToApply[i][1] = averageDisplacement[i][1] + correctionToApply[i - 1][1];
		}

		// Duplicate the trajs[] structure before correcting the positions.
		ResultsTableMt[] trajsCorr = new ResultsTableMt[trajs.length];
		for (int i = 3; i < trajs.length; i++) {
			trajsCorr[i] = trajs[i].clone();
		}

		// Apply the correction.
		for (int traj = 3; traj < trajsCorr.length; traj++) {
			for (int row = 0; row < trajsCorr[traj].getCounter(); row++) {
				if (trajsCorr[traj].getValueAsDouble(ResultsTableMt.FRAME, row) > 0) {
					trajsCorr[traj].setValue(ResultsTableMt.X, row, trajsCorr[traj].getValueAsDouble(ResultsTableMt.X,
							row)
							- correctionToApply[(int) trajsCorr[traj].getValueAsDouble(ResultsTableMt.FRAME, row)
									- 1][0]);
					trajsCorr[traj].setValue(ResultsTableMt.Y, row, trajsCorr[traj].getValueAsDouble(ResultsTableMt.Y,
							row)
							- correctionToApply[(int) trajsCorr[traj].getValueAsDouble(ResultsTableMt.FRAME, row)
									- 1][1]);
				}
			}
		}

		// [3] Determine the average centre of mass of the cell for each frame
		double[][] centreOfMass = new double[2][frameNumber];
		for (int frame = 0; frame < frameNumber; frame++) {
			HashMap<Integer, Integer> listTrajInFrame = listTrajInFrame(trajsCorr, frame);
			Iterator<Integer> it = listTrajInFrame.keySet().iterator();
			double counter = 0;
			while (it.hasNext()) {
				int traj = it.next();
				centreOfMass[0][frame] += trajsCorr[traj].getValueAsDouble(ResultsTableMt.X, listTrajInFrame.get(traj));
				centreOfMass[1][frame] += trajsCorr[traj].getValueAsDouble(ResultsTableMt.Y, listTrajInFrame.get(traj));
				counter++;
			}
			if (counter > 0) {
				centreOfMass[0][frame] /= counter;
				centreOfMass[1][frame] /= counter;
			} else {
				if (frame > 0 && centreOfMass[0][frame - 1] != 0) {
					centreOfMass[0][frame] = centreOfMass[0][frame - 1];
					centreOfMass[1][frame] = centreOfMass[1][frame - 1];
				}
			}
		}
		for (int frame = frameNumber - 2; frame >= 0; frame--) {
			if (centreOfMass[0][frame] == 0) {
				centreOfMass[0][frame] = centreOfMass[0][frame + 1];
				centreOfMass[1][frame] = centreOfMass[1][frame + 1];
			}
		}
		// Averaging the position of the CentreOfMass
		centreOfMass[0] = Utils.runningAverageStrict(centreOfMass[0], 10);
		centreOfMass[1] = Utils.runningAverageStrict(centreOfMass[1], 10);

		// Plot the new result...
		// TODO
		// TODO What should I save??
		Plot plot = new Plot("Corrected position of the trajectories", "", "");
		for (int traj = 3; traj < trajsCorr.length; traj++) {
			int frameNumberMax = (int) trajsCorr[traj].getValueAsDouble(ResultsTableMt.FRAME,
					trajsCorr[traj].getCounter() - 1);
			int frameNumberMin = (int) trajsCorr[traj].getValueAsDouble(ResultsTableMt.FRAME, 0);
			for (int row = 1; row < trajsCorr[traj].getCounter(); row++) {
				plot.setColor(Utils.getGradientColor(Color.GREEN, Color.RED, frameNumberMax - frameNumberMin + 1,
						(int) trajsCorr[traj].getValueAsDouble(ResultsTableMt.FRAME, row) - frameNumberMin));
				plot.addPoints(
						new double[] { trajsCorr[traj].getValueAsDouble(ResultsTableMt.X, row - 1),
								trajsCorr[traj].getValueAsDouble(ResultsTableMt.X, row) },
						new double[] { trajsCorr[traj].getValueAsDouble(ResultsTableMt.Y, row - 1),
								trajsCorr[traj].getValueAsDouble(ResultsTableMt.Y, row) },
						Plot.LINE);
				// plot.addPoints(trajsCorr[traj].getColumnAsDoubles(ResultsTableMt.X),
				// trajsCorr[traj].getColumnAsDoubles(ResultsTableMt.Y), Plot.LINE);
				plot.draw();
			}
		}
		plot.setColor(Color.BLACK);
		plot.addPoints(centreOfMass[0], centreOfMass[1], Plot.LINE);
		plot.draw();
		plot.setLimits(0, 100, 0, 100); // TODO Determine the correct limits
		plot.show();

		// [4] Calculate, for each point of each trajectory, its distance to the
		// centreOfMass of the cell.
		// Thus, we can calculate, for each trajectory, at which speed (average and
		// global) it goes towards (or not) the centre of the cell.
		double[] avgRadialSpeed = new double[trajsCorr.length - 3];
		double[] globalRadialSpeed = new double[trajsCorr.length - 3];
		double[] avgRadialDistance = new double[trajsCorr.length - 3];
		String radialDistance = "Distance to the centre of the cell (pix)";
		for (int traj = 3; traj < trajsCorr.length; traj++) {
			for (int row = 0; row < trajsCorr[traj].getCounter(); row++) {
				trajsCorr[traj].setValue(radialDistance, row, Math.pow(Math
						.pow(trajsCorr[traj].getValueAsDouble(ResultsTableMt.X, row)
								- centreOfMass[0][(int) trajsCorr[traj].getValueAsDouble(ResultsTableMt.FRAME, 0)], 2)
						+ Math.pow(trajsCorr[traj].getValueAsDouble(ResultsTableMt.Y, row)
								- centreOfMass[1][(int) trajsCorr[traj].getValueAsDouble(ResultsTableMt.FRAME, 0)], 2),
						0.5));

				if (row > 0) {
					double temp = (trajsCorr[traj].getValue(radialDistance, row)
							- trajsCorr[traj].getValue(radialDistance, row - 1));
					avgRadialSpeed[traj - 3] += (temp) / (trajsCorr[traj].getValueAsDouble(ResultsTableMt.FRAME, row)
							- trajsCorr[traj].getValueAsDouble(ResultsTableMt.FRAME, row - 1));
				}

			}
			avgRadialSpeed[traj - 3] /= -(trajsCorr[traj].getCounter() - 1);
			globalRadialSpeed[traj - 3] = -(trajsCorr[traj].getValue(radialDistance, trajsCorr[traj].getCounter() - 1)
					- trajsCorr[traj].getValue(radialDistance, 0))
					/ (trajsCorr[traj].getValueAsDouble(ResultsTableMt.FRAME, trajsCorr[traj].getCounter() - 1)
							- trajsCorr[traj].getValueAsDouble(ResultsTableMt.FRAME, 0));
			IJ.log("Traj #: " + (traj - 3) + " - Avg speed towards the centre of the cell : " + avgRadialSpeed[traj - 3]
					+ " // (global) " + globalRadialSpeed[traj - 3]);
			avgRadialDistance[traj - 3] = Utils.average(trajsCorr[traj].getColumn(radialDistance));
		}

		// Plot the results in a 2D plot: radial speed vs. radial distance to the centre
		// of the cell.
		Plot plot2D = new Plot("Centripetal tendency of the trajectories",
				"Radial speed (pix/frame) (grey = global speed ; green = avg)",
				"radial distance to the centre of the cell (pix)");
		plot2D.setColor(Color.GRAY);
		plot2D.addPoints(globalRadialSpeed, avgRadialDistance, Plot.CIRCLE);
		plot2D.draw();

		plot2D.setColor(Color.GREEN);
		plot2D.addPoints(avgRadialSpeed, avgRadialDistance, Plot.CIRCLE);
		plot2D.draw();

		plot2D.show();

	}

	public void pairwiseDistance(ResultsTableMt[] trajs, boolean save) {
		// For each couple of trajectories sharing two consecutive frames,
		// plot their average relative speed against their (initial) distance.
		// - Centripetal attraction should result in negative speeds;
		// - Centrifugal repulsion should result in positive speeds;
		// - Randomness should result in speeds close to 0.
		ResultsTableMt rt = new ResultsTableMt();

		for (int traj = 3; traj < trajs.length; traj++) {
			for (int traj2 = traj + 1; traj2 < trajs.length; traj2++) {
				if (trajs[traj].getCounter() > 1 && trajs[traj2].getCounter() > 1
						&& trajs[traj].getValueAsDouble(ResultsTableMt.FRAME, 0) < trajs[traj2]
								.getValueAsDouble(ResultsTableMt.FRAME, trajs[traj2].getCounter() - 1)
						&& trajs[traj2].getValueAsDouble(ResultsTableMt.FRAME, 0) < trajs[traj]
								.getValueAsDouble(ResultsTableMt.FRAME, trajs[traj].getCounter() - 1)) {
					ResultsTableMt commonFrames = getCommonFrames(trajs, traj, traj2);
					double initialDistance = Utils.getDistance(trajs[traj], (int) commonFrames.getValue("Row1", 0),
							trajs[traj2], (int) commonFrames.getValue("Row2", 0));
					double relativeSpeed = 0;
					if (commonFrames.getCounter() > 1) {
						for (int row = 1; row < commonFrames.getCounter(); row++) {
							relativeSpeed += (Utils.getDistance(trajs[traj], (int) commonFrames.getValue("Row1", row),
									trajs[traj2], (int) commonFrames.getValue("Row2", row))
									- Utils.getDistance(trajs[traj], (int) commonFrames.getValue("Row1", row - 1),
											trajs[traj2], (int) commonFrames.getValue("Row2", row - 1)))
									/ (commonFrames.getValueAsDouble(ResultsTableMt.FRAME, row)
											- commonFrames.getValueAsDouble(ResultsTableMt.FRAME, row - 1));
						}
						relativeSpeed /= commonFrames.getCounter() - 1.0;

						int rowFinal = commonFrames.getCounter() - 1;
						double globalSpeed = (Utils.getDistance(trajs[traj],
								(int) commonFrames.getValue("Row1", rowFinal), trajs[traj2],
								(int) commonFrames.getValue("Row2", rowFinal))
								- Utils.getDistance(trajs[traj], (int) commonFrames.getValue("Row1", 0), trajs[traj2],
										(int) commonFrames.getValue("Row2", 0)))
								/ (commonFrames.getValueAsDouble(ResultsTableMt.FRAME, rowFinal)
										- commonFrames.getValueAsDouble(ResultsTableMt.FRAME, 0));

						rt.incrementCounter();
						rt.addValue("Traj1", traj - 3);
						rt.addValue("Traj2", traj2 - 3);
						rt.addValue("Initial distance (pix)", initialDistance);
						rt.addValue("Relative speed (pix/frame)", relativeSpeed);
						rt.addValue("Relative global speed (pix/frame)", globalSpeed);
					}
				}
			}
		}

		Plot plot = new Plot("Pairwise distance between trajectories",
				"Average relative speed between two trajectories (pix/frame)",
				"(Initial) distance separating the trajectories (pix)");
		// plot.setColor(Color.GRAY);
		// double[] xx = rt.getColumn("Relative global speed (pix/frame)");
		// plot.addPoints(xx, rt.getColumn("Initial distance (pix)"), Plot.CIRCLE);
		// plot.draw();
		plot.setColor(Color.BLACK);
		double[] xxx = rt.getColumn("Relative speed (pix/frame)");
		plot.addPoints(xxx, rt.getColumn("Initial distance (pix)"), Plot.CIRCLE);
		plot.draw();
		plot.show();

		// xx = Utils.getMeanAndStdev(xx);
		xxx = Utils.getMeanAndStdev(xxx);
		IJ.log("Mean average relative speed between two trajectories: " + xxx[0] + " +/- " + xxx[1] + " (pix/frame).");
		// IJ.log("Mean (global) relative speed between two trajectories: " + xx[0] + "
		// +/- " + xx[1] + " (pix/frame).");

		if (save) {
			Utils.saveTiff(plot.getImagePlus(), fileDirName + File.separator + "Pairwise_Analysis.tiff", false);
			rt.saveAsPrecise(fileDirName + File.separator + "Pairwise_Analysis.csv", 6);
		}
	}

	private ResultsTableMt getCommonFrames(ResultsTableMt[] trajs, int traj1, int traj2) {
		ResultsTableMt retour = new ResultsTableMt();
		int row2 = 0;
		for (int row1 = 0; row1 < trajs[traj1].getCounter() && row2 < trajs[traj2].getCounter(); row1++) {
			while (row2 < trajs[traj2].getCounter() && trajs[traj2].getValueAsDouble(ResultsTableMt.FRAME,
					row2) < trajs[traj1].getValueAsDouble(ResultsTableMt.FRAME, row1))
				row2++;
			if (row2 < trajs[traj2].getCounter() && trajs[traj2].getValueAsDouble(ResultsTableMt.FRAME,
					row2) == trajs[traj1].getValueAsDouble(ResultsTableMt.FRAME, row1)) {
				retour.incrementCounter();
				retour.addValue(ResultsTableMt.FRAME, trajs[traj2].getValueAsDouble(ResultsTableMt.FRAME, row2));
				retour.addValue("Row1", row1);
				retour.addValue("Row2", row2);
			}
		}
		return retour;
	}

	private HashMap<Integer, Integer> listTrajInFrame(ResultsTableMt[] trajs, int frame) {
		HashMap<Integer, Integer> hm = new HashMap<Integer, Integer>();
		for (int traj = 3; traj < trajs.length; traj++) {
			if (frame >= trajs[traj].getValueAsDouble(ResultsTableMt.FRAME, 0)
					&& frame <= trajs[traj].getValueAsDouble(ResultsTableMt.FRAME, trajs[traj].getCounter() - 1)) {
				int absent = -1;
				for (int row = 0; row < trajs[traj].getCounter() && absent < 0; row++) {
					if (trajs[traj].getValueAsDouble(ResultsTableMt.FRAME, row) == frame) {
						absent = row;
					}
				}

				if (absent > 0) {
					hm.put(traj, absent);
				}
			}
		}
		return hm;
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
			int minNumberOfLocPerTraj, boolean save) {

		// Trajectories: linking the name of the last Loc of each traj to its traj
		Hashtable<Integer, Traj> trajectories = new Hashtable<Integer, Traj>();

		// NB: The Rt has previously been sorted following the "frame" column
		// ResultsTableMt rt_Sorted = Utils.sortRt(rt, ResultsTableMt.FRAME);

		// Build trajectories
		buildTrajectories(rt_Sorted, maxStepPix, maxDarkTimeFrame, trajectories);

		// Extract trajectories from 'trajectories' into 'retour[3...end]' and into
		// finalHashMap (from Align_Trajectories ; in a single "SingleCell" cell)
		ResultsTableMt[] retour = extractAndFilterTrajectories(trajectories, rt_Sorted, minNumberOfLocPerTraj);

		// Save the resultsTableMt as outputs.
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
							saveTraj.incrementCounter();
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

	private static void buildTrajectories(ResultsTableMt rt_Sorted, double stepPix, int memory,
			Hashtable<Integer, Traj> trajectories) {
		boolean mergeNotEnd = true;// false; //TODO
		boolean keepClosest = false;
		boolean keepLongest = !keepClosest;

		IJ.showStatus("Building trajectories...");

		int[] memoryRows = new int[memory];

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
		// For each trajectory...
		while (it.hasNext()) {
			retour[traj + 3] = new ResultsTableMt();

			LinkedList<Integer> trajList = it.next().getValue().list;

			// 1- Update groupSizes (= retour[0])
			while (groupSizes.getCounter() < traj + 1)
				groupSizes.incrementCounter();
			groupSizes.setValue(GROUPSIZE_groupSizes, traj, trajList.size());
			// IJ.log("Traj # "+(traj+1)+" Size: " + trajList.size());

			Iterator<Integer> itList = trajList.iterator();
			// 2- Create the rt corresponding to the traj (= retour[traj + 3])
			// and 3- Update rt_Sorted "Group" and "GroupSize" columns.
			// For each fit in the trajectory...
			while (itList.hasNext()) {
				int loc = itList.next();

				Utils.addRow(rt_Sorted, retour[traj + 3], loc);
				rt_Sorted.setValue(ResultsTableMt.GROUP, loc, traj + 1);
				rt_Sorted.setValue(GROUPSIZE_Sorted, loc, trajList.size());
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

		analysis.populationJD_Analysis(saveTextFile); // TODO Remove, as unnecessary for now...
	}

}
