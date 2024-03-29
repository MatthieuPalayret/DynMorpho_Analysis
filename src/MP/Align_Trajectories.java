package MP;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import MP.objects.ResultsTableMt;
import MP.utils.Utils;
import ij.IJ;
import ij.gui.Plot;

/**
 * 
 * @author matth
 *
 *         The Align_Trajectories plugin further analyses outputs from Imaris:
 *         it aligns trajectories of various cells on a single (Ox) axis, and
 *         the behaviour of the movement of their detected particles, depending
 *         whether these are at the front (ahead of the center of mass of the
 *         cell, on the (Ox) axis) or at the back of the cell.
 * 
 *         It thus takes two consecutive inputs: (1) an ".xls" Imaris-derived
 *         file with the CELL tracks, and (2) an ".xls" Imaris-derived file for
 *         the ACTIN tracks.
 * 
 *         It then globally re-align each cell (and their corresponding particle
 *         tracks) along a common (Ox) axis (defined by the first and last
 *         position of the cell track - as we here suppose the cell trajectory
 *         to be roughly linear). The tracks of all the cells (and of their
 *         particles - with a twice thinner line) are plotted together in two
 *         different plots: (1) In a first plot (which is not automatically
 *         saved), the colour of the trajectories of the particles gets darker
 *         with time (to indicate their direction over time). (2) In the second
 *         plot (saved in "Trajectory_plot.tiff"), trajectories are plotted
 *         following a colour-coded time-gradient depending on whether they are
 *         behind the center of mass of the cell (gradient from yellow to green)
 *         or before it (gradient from orange to red).
 * 
 *         Finally, trajectories are analysed in a similar way as in the
 *         Get_Trajectories plugin: for each trajectory of each cell, the plugin
 *         calculates and saves : (1st column) the name of the cell this
 *         trajectory belongs to, (2nd) the name of the trajectory, (3rd) its
 *         average speed, (4th) its average speed along the (0x) axis, (5th) its
 *         global speed (distance between last and first position / number of
 *         frames), (6th) its directionality (1 if the trajectory is linear;
 *         close to 0 if it moves around a fixed position), (7th) its full
 *         length (along its path), (8th) its global length (distance between
 *         last and first positions), (9th) its total duration ("Time length"),
 *         (10th) whether, when the particle if first detected, it appears
 *         before the centre of mass of the cell (on the (Ox) axis) ("rear"
 *         particle) or after it ("upfront" particle). This data is saved in
 *         "Track_Analysis.csv".
 * 
 *         Then, a sum-up of these results is calculated and saved separately
 *         for all "rear" particles (saved in "Track_rear_statistics.csv") and
 *         for all "upfront" particles (saved in
 *         "Track_upfront_statistics.csv"). For each of the 3rd to 9th column,
 *         both an average value and a standard deviation of all the particles
 *         of interest are calculated and saved.
 */
public class Align_Trajectories extends Analyse_Trajectories {

	final int CELL = 0, TRAJ = 1;
	private File[] directorys = new File[2];
	HashMap<String, CellContainer> finalHashMap = new HashMap<String, CellContainer>();
	ResultsTableMt trackResults = new ResultsTableMt();
	private int savePrecision = 6;
	double[] stdev = null;
	double[] angle = null;

	public Align_Trajectories() {
	}

	@Override
	public void run(String arg0) {
		// Get cell file
		HashMap<Integer, ResultsTableMt> hashMapRtCell = extractTrajectories(
				"Select a .xls Imaris-derived file for the CELL tracks:");
		directorys[CELL] = directory;

		// Get track file
		HashMap<Integer, ResultsTableMt> hashMapRtTraj = extractTrajectories(
				"Select a .xls Imaris-derived file for the ACTIN tracks:");
		directorys[TRAJ] = directory;

		// Organise results in CellContainers
		Iterator<ResultsTableMt> it = hashMapRtCell.values().iterator();
		while (it.hasNext()) {
			ResultsTableMt temp = it.next();
			String temp2 = temp.getStringValue("Origin", 0);
			String cellName = temp2.substring(0, temp2.indexOf("_cell"));
			finalHashMap.put(cellName, new CellContainer(temp));
		}
		it = hashMapRtTraj.values().iterator();
		while (it.hasNext()) {
			ResultsTableMt temp = it.next();
			String temp2 = temp.getStringValue("Origin", 0);
			String cellName = temp2.substring(0, temp2.indexOf("_actin"));
			finalHashMap.get(cellName).trajTracks.put((int) temp.getValueAsDouble(ResultsTableMt.GROUP, 0), temp);
		}

		// Plot the structure of the HashMap
		IJ.log("Structure of the analysed data:");
		Iterator<String> it2 = finalHashMap.keySet().iterator();
		while (it2.hasNext()) {
			String cellName = it2.next();
			IJ.log("- " + cellName + " contains " + finalHashMap.get(cellName).trajTracks.size() + " tracks.");
		}

		// For each cell, globally re-orientate the cell trajectory
		double[] XYmaxMin = cellGlobalAlignment();
		// Plot all the trajectories
		plotAlignedCells(XYmaxMin, (int) XYmaxMin[4], false);

		// For each cell, re-orientate precisely the cell trajectory, squeezing it to
		// the x axis.
		boolean preciseAlignment = false;
		if (preciseAlignment) {
			cellPreciseAlignment();
			plotAlignedCells(XYmaxMin, (int) XYmaxMin[4], true);
		}

		// --------------------
		// Analyse the tracks
		analyseTheTracks(true, true, true, false);
	}

	public boolean setDirectory(File file, int type) {
		if (type == CELL || type == TRAJ) {
			directorys[type] = file;
			return true;
		}
		return false;
	}

	void analyseTheTracks(boolean newCoord, boolean rearFrontAnalysis, boolean unitsKnown, boolean averageValues) {
		int Xcoord = newCoord ? ResultsTableMt.X_CENTROID : ResultsTableMt.X;
		double avgIntensity = 0;
		double avgSigma = 0;

		// For each track, calculate the average speed, the speed_x, the directionality
		// (global displacement / trajectory length), trajectory length, global
		// displacement, time length, and rear or upfront (cellName and trackNumber)
		Iterator<String> it = finalHashMap.keySet().iterator();
		// For each detected cell...
		while (it.hasNext()) {
			String cellName = it.next();
			Iterator<Entry<Integer, ResultsTableMt>> itTracks = finalHashMap.get(cellName).trajTracks.entrySet()
					.iterator();
			ResultsTableMt cellTrack = finalHashMap.get(cellName).cellTrack;

			// For each detected traj. in that cell...
			while (itTracks.hasNext()) {
				Entry<Integer, ResultsTableMt> entry = itTracks.next();
				ResultsTableMt rtTrack = entry.getValue();
				double trajLength = 0;
				double trajSpeed = 0;

				// Determine whether the traj. starts as a FRONT or ROW trajectory
				int type = 0;
				int cellRow = 0;
				while (cellRow < cellTrack.getCounter() && cellTrack.getValueAsDouble(ResultsTableMt.FRAME,
						cellRow) < rtTrack.getValueAsDouble(ResultsTableMt.FRAME, 0))
					cellRow++;
				if (cellRow < cellTrack.getCounter())
					if (rtTrack.getValueAsDouble(Xcoord, 0) < cellTrack.getValueAsDouble(Xcoord, cellRow))
						type = REAR;
					else
						type = UPFRONT;

				// For each localisation of that traj...
				for (int row = 1; row < rtTrack.getCounter(); row++) {
					trajLength += Utils.getDistance(rtTrack, row, row - 1);
					// trajSpeed += trajLength / (rtTrack.getValue(unitsKnown ? "Time" : "Frame",
					// row) - rtTrack.getValue(unitsKnown ? "Time" : "Frame", row - 1));
					if (averageValues) {
						if (row == 1) {
							avgIntensity += rtTrack.getValueAsDouble(ResultsTableMt.INTENSITY, 0);
							avgSigma += rtTrack.getValue("SigmaX", 0) + rtTrack.getValue("SigmaY", 0);
						}
						avgIntensity += rtTrack.getValueAsDouble(ResultsTableMt.INTENSITY, row);
						avgSigma += rtTrack.getValue("SigmaX", row) + rtTrack.getValue("SigmaY", row);
					}

					if (cellRow++ < cellTrack.getCounter())
						if (rtTrack.getValueAsDouble(Xcoord, row) < cellTrack.getValueAsDouble(Xcoord, cellRow))
							type = (type == REAR) ? REAR : MIXED;
						else
							type = (type == UPFRONT) ? UPFRONT : MIXED;
				}
				if (averageValues && rtTrack.getCounter() > 0) {
					avgIntensity /= (rtTrack.getCounter());
					avgSigma /= (2.0 * (rtTrack.getCounter()));
				}

				// For each track, calculate the average speed, the speed_x, the directionality
				// (global displacement / trajectory length), trajectory length, global
				// displacement, time length, and rear or upfront (cellName and trackNumber)
				trackResults.incrementCounter();
				trackResults.addValue("Origin", cellName);
				trackResults.addValue(ResultsTableMt.GROUP, entry.getKey());
				double timeLength = (rtTrack.getValue(unitsKnown ? "Time" : "Frame", rtTrack.getCounter() - 1)
						- rtTrack.getValue(unitsKnown ? "Time" : "Frame", 0));
				trackResults.addValue("Speed " + (unitsKnown ? "(�m/s)" : "(pix/frame)"), trajLength / timeLength);
				if (rearFrontAnalysis)
					trackResults.addValue("Speed_x " + (unitsKnown ? "(�m/s)" : "(pix/frame)"),
							(rtTrack.getValueAsDouble(Xcoord, rtTrack.getCounter() - 1)
									- rtTrack.getValueAsDouble(Xcoord, 0)) / timeLength);
				double globalDisplacement = Utils.getDistance(rtTrack, rtTrack.getCounter() - 1, 0);
				trackResults.addValue("Global speed " + (unitsKnown ? "(�m/s)" : "(pix/frame)"),
						globalDisplacement / timeLength);
				trackResults.addValue("Directionality [0..1]", globalDisplacement / trajLength);
				trackResults.addValue("Trajectory length " + (unitsKnown ? "(�m)" : "(pix)"), trajLength);
				trackResults.addValue("Global displacement " + (unitsKnown ? "(�m)" : "(pix)"), globalDisplacement);
				trackResults.addValue("Time length " + (unitsKnown ? "(s)" : "(frames)"), timeLength);
				if (rearFrontAnalysis)
					trackResults.addValue("Rear (-1) or upfront (+1)", type);
				if (averageValues) {
					trackResults.addValue("Average intensity " + (unitsKnown ? "(photons)" : "(a.u.)"), avgIntensity);
					trackResults.addValue("Average width sigma " + (unitsKnown ? "(�m)" : "(pix)"), avgSigma);
					trackResults.addValue("Average area " + (unitsKnown ? "(�m�)" : "(pix�)"),
							(Math.pow(avgSigma, 2) * Math.PI));
				}
				if (stdev != null && entry.getKey() < stdev.length) {
					trackResults.addValue(
							"Standard deviation from average trajectory " + (unitsKnown ? "(�m)" : "(pix)"),
							stdev[entry.getKey()]);
				}
				if (angle != null && entry.getKey() < angle.length) {
					trackResults.addValue("Average angle between the trajectory and the average trajectory (rad)",
							angle[entry.getKey()]);
				}
			}
		}
		trackResults.saveAsPrecise(directorys[TRAJ] + File.separator + "Track_Analysis.csv", savePrecision);

		// Output averages and stdev
		if (rearFrontAnalysis) {
			ResultsTableMt rtRear = new ResultsTableMt(), rtUpfront = new ResultsTableMt();
			calculateAvgStdev(trackResults, rtRear, rtUpfront, "Speed " + (unitsKnown ? "(�m/s)" : "(pix/frame)"));
			calculateAvgStdev(trackResults, rtRear, rtUpfront, "Speed_x " + (unitsKnown ? "(�m/s)" : "(pix/frame)"));
			calculateAvgStdev(trackResults, rtRear, rtUpfront,
					"Global speed " + (unitsKnown ? "(�m/s)" : "(pix/frame)"));
			calculateAvgStdev(trackResults, rtRear, rtUpfront, "Directionality [0..1]");
			calculateAvgStdev(trackResults, rtRear, rtUpfront, "Trajectory length " + (unitsKnown ? "(�m)" : "(pix)"));
			calculateAvgStdev(trackResults, rtRear, rtUpfront,
					"Global displacement " + (unitsKnown ? "(�m)" : "(pix)"));
			calculateAvgStdev(trackResults, rtRear, rtUpfront, "Time length " + (unitsKnown ? "(s)" : "(frames)"));
			if (averageValues) {
				calculateAvgStdev(trackResults, rtRear, rtUpfront,
						"Average intensity " + (unitsKnown ? "(photons)" : "(a.u.)"));
				calculateAvgStdev(trackResults, rtRear, rtUpfront,
						"Average width sigma " + (unitsKnown ? "(�m)" : "(pix)"));
				calculateAvgStdev(trackResults, rtRear, rtUpfront, "Average area " + (unitsKnown ? "(�m�)" : "(pix�)"));
			}
			if (stdev != null) {
				calculateAvgStdev(trackResults, rtRear, rtUpfront,
						"Standard deviation from average trajectory " + (unitsKnown ? "(�m)" : "(pix)"));
			}
			if (angle != null) {
				calculateAvgStdev(trackResults, rtRear, rtUpfront,
						"Average angle between the trajectory and the average trajectory (rad)");
			}
			rtRear.saveAsPrecise(directorys[TRAJ] + File.separator + "Track_rear_statistics.csv", savePrecision);
			rtUpfront.saveAsPrecise(directorys[TRAJ] + File.separator + "Track_upfront_statistics.csv", savePrecision);
		}
	}

	static final int REAR = -1, MIXED = 0, UPFRONT = 1;

	private void calculateAvgStdev(ResultsTableMt rt, ResultsTableMt rtRear, ResultsTableMt rtUpfront, String column) {
		double avgRear = 0, avgUp = 0;
		int rearNb = 0, upNb = 0;
		for (int row = 0; row < rt.getCounter(); row++) {
			int rearOrUp = (int) rt.getValue("Rear (-1) or upfront (+1)", row);
			if (rearOrUp == REAR) {
				avgRear += rt.getValue(column, row);
				rearNb++;
			} else if (rearOrUp == UPFRONT) {
				avgUp += rt.getValue(column, row);
				upNb++;
			}
		}
		if (rearNb > 0)
			avgRear /= rearNb;
		if (upNb > 0)
			avgUp /= upNb;

		double stdevRear = 0, stdevUp = 0;
		for (int row = 0; row < rt.getCounter(); row++) {
			int rearOrUp = (int) rt.getValue("Rear (-1) or upfront (+1)", row);
			if (rearOrUp == REAR) {
				stdevRear += Math.pow(rt.getValue(column, row) - avgRear, 2);
			} else if (rearOrUp == UPFRONT) {
				stdevUp += Math.pow(rt.getValue(column, row) - avgUp, 2);
			}
		}
		if (rearNb > 0)
			stdevRear /= rearNb;
		if (upNb > 0)
			stdevUp /= upNb;

		if (rtRear.getCounter() == 0)
			rtRear.incrementCounter();
		rtRear.addValue(column + " Avg", avgRear);
		rtRear.addValue(column + " Stdev", stdevRear);
		if (rtUpfront.getCounter() == 0)
			rtUpfront.incrementCounter();
		rtUpfront.addValue(column + " Avg", avgUp);
		rtUpfront.addValue(column + " Stdev", stdevUp);
	}

	private void cellPreciseAlignment() {
		Iterator<String> it = finalHashMap.keySet().iterator();
		while (it.hasNext()) {
			String cellName = it.next();
			// for the cell trajectory and all the track trajectories, add two columns X'
			// and Y' in the ResultsTable
			CellContainer cell = finalHashMap.get(cellName);

			// Squeeze the cell trajectory to the x axis.
			double trajlength = 0;
			cell.cellTrack.setValue(ResultsTableMt.X_CENTROID, 0, 0);
			cell.cellTrack.setValue(ResultsTableMt.Y_CENTROID, 0, 0);
			for (int row = 1; row < cell.cellTrack.getCounter(); row++) {
				trajlength += Utils.getDistance(cell.cellTrack, row, row - 1);
				cell.cellTrack.setValue(ResultsTableMt.X_CENTROID, row, trajlength);
				cell.cellTrack.setValue(ResultsTableMt.Y_CENTROID, row, 0);
			}

			// Change system coordinate for each frame for each track trajectory
			Iterator<Integer> it3 = cell.trajTracks.keySet().iterator();
			while (it3.hasNext()) {
				ResultsTableMt rtTrack = cell.trajTracks.get(it3.next());
				int rowCell = 0;

				for (int rowTrack = 0; rowTrack < rtTrack.getCounter(); rowTrack++) {
					// Find the corresponding cell point (for the same frame)
					int frameTrack = (int) rtTrack.getValueAsDouble(ResultsTableMt.FRAME, rowTrack);
					while (rowCell < cell.cellTrack.getCounter()
							&& cell.cellTrack.getValueAsDouble(ResultsTableMt.FRAME, rowCell) < frameTrack)
						rowCell++;
					if (cell.cellTrack.getValueAsDouble(ResultsTableMt.FRAME, rowCell) == frameTrack) {

						double[] coordP = new double[] { rtTrack.getValueAsDouble(ResultsTableMt.X, rowTrack),
								rtTrack.getValueAsDouble(ResultsTableMt.Y, rowTrack) };
						double[] originX = new double[] { cell.cellTrack.getValueAsDouble(ResultsTableMt.X, rowCell),
								cell.cellTrack.getValueAsDouble(ResultsTableMt.Y, rowCell) };
						double[] originXnew = new double[] {
								cell.cellTrack.getValueAsDouble(ResultsTableMt.X_CENTROID, rowCell), 0 };
						if (rowCell == 0) { // Just do the translation, not the rotation
							coordP = Utils.minus(coordP, originX);
						} else {
							double[] originXminusOne = new double[] {
									cell.cellTrack.getValueAsDouble(ResultsTableMt.X, rowCell - 1),
									cell.cellTrack.getValueAsDouble(ResultsTableMt.Y, rowCell - 1) };
							double[] uX = Utils.normalise(Utils.minus(originX, originXminusOne));

							// First translation: remove origin X
							coordP = Utils.minus(coordP, originX);

							// Rotation
							double[] result = new double[2];
							result[0] = Utils.scalar(uX, coordP); // Rotation (matrix) of coordinate system
							result[1] = -uX[1] * coordP[0] + uX[0] * coordP[1];
							coordP = result;

							// Add origin X'
							coordP = Utils.plus(coordP, originXnew);
						}

						rtTrack.setValue(ResultsTableMt.X_CENTROID, rowTrack, coordP[0]);
						rtTrack.setValue(ResultsTableMt.Y_CENTROID, rowTrack, coordP[1]);
					}
				}
			}
		}
	}

	private double[] cellGlobalAlignment() {
		// For each cell, re-orientate the cell trajectory
		double[] XYmaxMin = new double[5]; // Store X'max, X'min, Y'max, Y'min and frameNumber for the further plot.
		Iterator<String> it = finalHashMap.keySet().iterator();
		while (it.hasNext()) {
			String cellName = it.next();
			// for the cell trajectory and all the track trajectories, add two columns X'
			// and Y' in the ResultsTable
			CellContainer cell = finalHashMap.get(cellName);
			double[] newOrigin = { cell.cellTrack.getValueAsDouble(ResultsTableMt.X, 0),
					cell.cellTrack.getValueAsDouble(ResultsTableMt.Y, 0) };
			double[] newXDirection = {
					cell.cellTrack.getValueAsDouble(ResultsTableMt.X, cell.cellTrack.getCounter() - 1),
					cell.cellTrack.getValueAsDouble(ResultsTableMt.Y, cell.cellTrack.getCounter() - 1) };
			CoordinateSystemChange syst = new CoordinateSystemChange(newOrigin, newXDirection);
			changeSystemCoordinate(cell.cellTrack, syst);
			XYmaxMin = updateXYmaxMin(XYmaxMin, cell.cellTrack);
			XYmaxMin[4] = Math.max(XYmaxMin[4], Utils.getMax(cell.cellTrack, ResultsTableMt.FRAME));

			Iterator<Integer> it3 = cell.trajTracks.keySet().iterator();
			while (it3.hasNext()) {
				ResultsTableMt rtTemp = cell.trajTracks.get(it3.next());
				changeSystemCoordinate(rtTemp, syst);
				XYmaxMin = updateXYmaxMin(XYmaxMin, rtTemp);
			}
		}
		return XYmaxMin;
	}

	private void plotAlignedCells(double[] XYmaxMin, int frameNumber, boolean plotTowardsDarkerColours) {

		// Plot all the trajectories
		Plot plot = new Plot("Aligned trajectories - timescale: towards darker colours", "X' axis (�m)",
				"Y' axis (�m)");
		plot.setLimits(XYmaxMin[1], XYmaxMin[0], XYmaxMin[3], XYmaxMin[2]);
		Plot plot2 = new Plot("Aligned trajectories - darker rear actin spots", "X' axis (�m)", "Y' axis (�m)");
		// plot2.setLimits(XYmaxMin[1], XYmaxMin[0], XYmaxMin[3], XYmaxMin[2]);
		plot2.setLimits(-9, 20, -9, 10);

		Iterator<String> it = finalHashMap.keySet().iterator();
		int i = 0;
		while (it.hasNext()) {
			String cellName = it.next();
			CellContainer cell = finalHashMap.get(cellName);
			Color color = Color.BLACK; // Utils.getGradientColor(Color.RED, Color.BLUE, finalHashMap.size(), i++);
			if (plotTowardsDarkerColours)
				drawTraj(plot, cell.cellTrack, color, true, true, frameNumber);
			drawTraj(plot2, cell.cellTrack, color, false, true, -1);

			Iterator<Integer> it3 = cell.trajTracks.keySet().iterator();
			while (it3.hasNext()) {
				ResultsTableMt rtTemp = cell.trajTracks.get(it3.next());
				if (plotTowardsDarkerColours)
					drawTraj(plot, rtTemp, color, false, true, frameNumber);

				// Other plot: if actin traj are behind the cell centre, colour it darker,
				// otherwise, colour it brighter!
				drawTraj2(plot2, rtTemp, color, cell.cellTrack, true);
			}
		}
		if (plotTowardsDarkerColours)
			plot.show();
		plot2.show();

		Utils.saveTiff(plot2.makeHighResolution(plot2.getTitle(), 2, true, false),
				directorys[TRAJ] + File.separator + "Trajectory_plot.tiff", false);
	}

	private void drawTraj(Plot plot, ResultsTableMt rt, Color color, boolean cellTrack, boolean newCoord,
			int frameNumber) {
		// plot.setColor(color);
		plot.setLineWidth(cellTrack ? 1 : 2);
		int xColumn = newCoord ? ResultsTableMt.X_CENTROID : ResultsTableMt.X;
		int yColumn = newCoord ? ResultsTableMt.Y_CENTROID : ResultsTableMt.Y;
		// plot.addPoints(rt.getColumnAsDoubles(xColumn),
		// rt.getColumnAsDoubles(yColumn), Plot.LINE);
		// plot.draw();

		for (int row = 1; row < rt.getCounter(); row++) {
			Color colorTemp = (frameNumber <= 0) ? color
					: Utils.getDarkGradient(color.brighter(), frameNumber,
							(int) rt.getValueAsDouble(ResultsTableMt.FRAME, row));
			plot.setColor(colorTemp);
			plot.drawLine(rt.getValueAsDouble(xColumn, row - 1), rt.getValueAsDouble(yColumn, row - 1),
					rt.getValueAsDouble(xColumn, row), rt.getValueAsDouble(yColumn, row));
		}

	}

	private void drawTraj2(Plot plot, ResultsTableMt rt, Color color, ResultsTableMt rtCell, boolean newCoord) {
		boolean noVectorButGradient = true;

		plot.setLineWidth(2);
		int xColumn = newCoord ? ResultsTableMt.X_CENTROID : ResultsTableMt.X;
		int yColumn = newCoord ? ResultsTableMt.Y_CENTROID : ResultsTableMt.Y;
		int cellRow = 0;

		for (int row = 1; row < rt.getCounter(); row++) {
			int frame = (int) rt.getValueAsDouble(ResultsTableMt.FRAME, row);
			while (cellRow < rtCell.getCounter() && rtCell.getValueAsDouble(ResultsTableMt.FRAME, cellRow) < frame)
				cellRow++;
			if (cellRow < rtCell.getCounter()) {
				if (rt.getValueAsDouble(xColumn, row) < rtCell.getValueAsDouble(xColumn, cellRow)) {
					if (noVectorButGradient)
						plot.setColor(Utils.getGradientColor(Color.ORANGE, Color.RED, rt.getCounter(), row));
					else
						plot.setColor(color.darker());
				} else {
					if (noVectorButGradient)
						plot.setColor(Utils.getGradientColor(Color.YELLOW, Color.GREEN, rt.getCounter(), row));
					else
						plot.setColor(color.brighter());
				}
			} else
				plot.setColor(color);

			if (!noVectorButGradient && row == 1) {
				plot.setLineWidth(1);
				plot.addPoints(new double[] { rt.getValueAsDouble(xColumn, 0) },
						new double[] { rt.getValueAsDouble(yColumn, 0) }, Plot.X);
				plot.draw();
				plot.setLineWidth(2);
			}

			if (!noVectorButGradient && row == rt.getCounter() - 1) {
				plot.drawVectors(new double[] { rt.getValueAsDouble(xColumn, row - 1) },
						new double[] { rt.getValueAsDouble(yColumn, row - 1) },
						new double[] { rt.getValueAsDouble(xColumn, row) },
						new double[] { rt.getValueAsDouble(yColumn, row) });
			} else {
				plot.drawLine(rt.getValueAsDouble(xColumn, row - 1), rt.getValueAsDouble(yColumn, row - 1),
						rt.getValueAsDouble(xColumn, row), rt.getValueAsDouble(yColumn, row));
			}
		}
	}

	private double[] updateXYmaxMin(double[] XYmaxMin, ResultsTableMt rt) {
		double[] temp = Utils.getMinMax(rt, ResultsTableMt.X_CENTROID);
		XYmaxMin[0] = Math.max(XYmaxMin[0], temp[1]);
		XYmaxMin[1] = Math.min(XYmaxMin[1], temp[0]);
		temp = Utils.getMinMax(rt, ResultsTableMt.Y_CENTROID);
		XYmaxMin[2] = Math.max(XYmaxMin[2], temp[1]);
		XYmaxMin[3] = Math.min(XYmaxMin[3], temp[0]);
		return XYmaxMin;
	}

	protected class CellContainer {
		ResultsTableMt cellTrack;
		HashMap<Integer, ResultsTableMt> trajTracks = new HashMap<Integer, ResultsTableMt>();

		public CellContainer(ResultsTableMt cellTrack) {
			this.cellTrack = cellTrack;
		}
	}

	protected class CoordinateSystemChange {
		double[] newOrigin = new double[2];
		double[] newXDirection = new double[2];
		double[] newVectorX = new double[2];

		public CoordinateSystemChange(double[] newOrigin, double[] newXDirection) {
			this.newOrigin = newOrigin;
			this.newXDirection = newXDirection;
			newVectorX = Utils.normalise(Utils.minus(newXDirection, newOrigin));
		}

		public double[] newCoordinates(double[] p) {
			p = Utils.minus(p, newOrigin); // Translation of coordinate system
			double[] result = new double[2];
			result[0] = Utils.scalar(newVectorX, p); // Rotation (matrix) of coordinate system
			result[1] = -newVectorX[1] * p[0] + newVectorX[0] * p[1];
			return result;
		}
	}

	private void changeSystemCoordinate(ResultsTableMt rt, CoordinateSystemChange coord) {
		double[] newCoord = new double[2];
		for (int row = 0; row < rt.getCounter(); row++) {
			newCoord[0] = rt.getValueAsDouble(ResultsTableMt.X, row);
			newCoord[1] = rt.getValueAsDouble(ResultsTableMt.Y, row);
			newCoord = coord.newCoordinates(newCoord);

			rt.setValue(ResultsTableMt.X_CENTROID, row, newCoord[0]);
			rt.setValue(ResultsTableMt.Y_CENTROID, row, newCoord[1]);
		}
	}

}
