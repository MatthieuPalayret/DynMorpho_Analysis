package MP;

import java.awt.Color;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

import ij.IJ;
import ij.gui.Plot;

public class Align_Trajectories extends Analyse_Trajectories {

	private final int CELL = 0, TRAJ = 1;
	private File[] directorys = new File[2];
	private HashMap<String, CellContainer> finalHashMap = new HashMap<String, CellContainer>();

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
			IJ.log("- " + cellName + " countains " + finalHashMap.get(cellName).trajTracks.size() + " tracks.");
		}

		// For each cell, globally re-orientate the cell trajectory
		double[] XYmaxMin = cellGlobalAlignment();
		// Plot all the trajectories
		plotAlignedCells(XYmaxMin, (int) XYmaxMin[4]);

		// For each cell, re-orientate precisely the cell trajectory, squeezing it to
		// the x axis.
		it2 = finalHashMap.keySet().iterator();
		while (it2.hasNext()) {
			String cellName = it2.next();
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

		plotAlignedCells(XYmaxMin, (int) XYmaxMin[4]);
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

	private void plotAlignedCells(double[] XYmaxMin, int frameNumber) {

		// Plot all the trajectories
		Plot plot = new Plot("Aligned trajectories - timescale: towards darker colours", "X' axis (µm)",
				"Y' axis (µm)");
		plot.setLimits(XYmaxMin[1], XYmaxMin[0], XYmaxMin[3], XYmaxMin[2]);
		Plot plot2 = new Plot("Aligned trajectories - darker rear actin spots", "X' axis (µm)", "Y' axis (µm)");
		plot2.setLimits(XYmaxMin[1], XYmaxMin[0], XYmaxMin[3], XYmaxMin[2]);

		Iterator<String> it = finalHashMap.keySet().iterator();
		int i = 0;
		while (it.hasNext()) {
			String cellName = it.next();
			CellContainer cell = finalHashMap.get(cellName);
			Color color = Utils.getGradientColor(Color.RED, Color.BLUE, finalHashMap.size(), i++);
			drawTraj(plot, cell.cellTrack, color, true, true, frameNumber);
			drawTraj(plot2, cell.cellTrack, color, true, true, -1);

			Iterator<Integer> it3 = cell.trajTracks.keySet().iterator();
			while (it3.hasNext()) {
				ResultsTableMt rtTemp = cell.trajTracks.get(it3.next());
				drawTraj(plot, rtTemp, color, false, true, frameNumber);

				// Other plot: if actin traj are behind the cell centre, colour it darker,
				// otherwise, colour it brighter!
				drawTraj2(plot2, rtTemp, color, cell.cellTrack, true);
			}
		}
		plot.show();
		plot2.show();
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
		plot.setLineWidth(2);
		int xColumn = newCoord ? ResultsTableMt.X_CENTROID : ResultsTableMt.X;
		int yColumn = newCoord ? ResultsTableMt.Y_CENTROID : ResultsTableMt.Y;
		int cellRow = 0;

		for (int row = 1; row < rt.getCounter(); row++) {
			int frame = (int) rt.getValueAsDouble(ResultsTableMt.FRAME, row);
			while (cellRow < rtCell.getCounter() && rtCell.getValueAsDouble(ResultsTableMt.FRAME, cellRow) < frame)
				cellRow++;
			if (cellRow < rtCell.getCounter()) {
				if (rt.getValueAsDouble(xColumn, row) < rtCell.getValueAsDouble(xColumn, cellRow))
					plot.setColor(color.darker());
				else
					plot.setColor(color.brighter());
			} else
				plot.setColor(color);

			if (row == 1) {
				plot.setLineWidth(1);
				plot.addPoints(new double[] { rt.getValueAsDouble(xColumn, 0) },
						new double[] { rt.getValueAsDouble(yColumn, 0) }, Plot.X);
				plot.draw();
				plot.setLineWidth(2);
			}

			if (row == rt.getCounter() - 1) {
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
