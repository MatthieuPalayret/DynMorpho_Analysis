package MP;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import Cell.CellData;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.plugin.filter.Analyzer;

public class Results {

	public static int CELL, PROTRUSION, AREA, COLOUR;

	private static final String[] defaultHeadings = { "Cell", "Protrusion", "Area", "Channel" };

	LinkedList<Cell> cells;
	int frameNumber;
	ArrayList<CellDataR> cellData;
	// ImagePlus imp = null;
	ResultsTableMt rt = new ResultsTableMt();
	ResultsTableMt linearity = new ResultsTableMt(), linearityPrecised = new ResultsTableMt();
	private Params params;

	public Results(ArrayList<CellDataR> cellData, Params params, int frameNumber) {
		this.params = params;
		this.frameNumber = frameNumber;

		this.cellData = cellData;

		initialiseRt(rt, true);
		initialiseRt(linearity, false);
		initialiseRt(linearityPrecised, false);

		detectProbableCellEncounter();

		buildCells();
		splitCells();
		updateTrajLengthRejection();
		greenRedChannelUpdate();
	}

	private void detectProbableCellEncounter() {
		// Remove all previous REJECT_DRAMATIC_CHANGE
		ListIterator<CellDataR> it = cellData.listIterator();
		while (it.hasNext()) {
			CellDataR cellR = it.next();
			CellData cell = cellR.getCellData();
			if (cell.getEndFrame() >= cell.getStartFrame() + 1) {
				for (int ij = 1; ij < cell.getLength(); ij++) {
					if (cellR.whichRejectionInFrame(cell.getStartFrame() - 1 + ij) == CellDataR.REJECT_DRAMATIC_CHANGE)
						cellR.rejectFrame(cell.getStartFrame() - 1 + ij, CellDataR.NOT_REJECTED);
				}
			}
		}

		it = cellData.listIterator();
		while (it.hasNext()) {
			CellDataR cellR = it.next();
			CellData cell = cellR.getCellData();
			if (cell.getEndFrame() >= cell.getStartFrame() + 1) {
				double[][] x = cell.getCurveMap().getxCoords();
				double[][] y = cell.getCurveMap().getyCoords();
				double areaPreviousIJ = Utils.area(Utils.buildFloatPolygon(x[0], y[0]));
				for (int ij = 1; ij < cell.getLength(); ij++) {
					double areaIJ = Utils.area(Utils.buildFloatPolygon(x[ij], y[ij]));

					if (params.test)
						IJ.log("Areas :" + IJ.d2s(areaIJ, 0) + " / " + IJ.d2s(areaPreviousIJ, 0) + " = "
								+ IJ.d2s(areaIJ / areaPreviousIJ, 2));
					if (areaIJ > params.dramaticAreaIncrease / 100.0 * areaPreviousIJ) {
						if (params.test)
							IJ.log("EJECT 1 frame #" + (cell.getStartFrame() - 1 + ij));
						cellR.rejectFrame(cell.getStartFrame() - 1 + ij, CellDataR.REJECT_DRAMATIC_CHANGE);
					} else if (areaPreviousIJ > params.dramaticAreaIncrease / 100.0 * areaIJ) {
						if (params.test)
							IJ.log("EJECT -1 frame #" + (cell.getStartFrame() - 1 + ij - 1));
						cellR.rejectFrame(cell.getStartFrame() - 1 + ij - 1, CellDataR.REJECT_DRAMATIC_CHANGE);
					}
					areaPreviousIJ = areaIJ;
				}
			}

		}
	}

	private void greenRedChannelUpdate() {
		ListIterator<Cell> it = cells.listIterator();
		while (it.hasNext()) {
			Cell cell = it.next();

			// Get the original colour of the cell, and whether it was reversed
			int originalCellColour = cell.celldata.whichOriginalCellColour();
			int reverseCounts = 0;
			for (int frame = cell.startFrame; frame <= cell.endFrame; frame++) {
				if (cell.celldata.getStoreGreenRedChannel(frame))
					reverseCounts++;
			}
			if ((reverseCounts % 2) == 0) {
				cell.colour = originalCellColour;
				if (reverseCounts > 0) {
					for (int frame = cell.startFrame; frame <= cell.endFrame; frame++) {
						if (cell.celldata.getStoreGreenRedChannel(frame))
							cell.celldata.setStoreGreenRedChannel(frame);
					}
				}
			} else {
				cell.colour = originalCellColour == CellDataR.GREEN ? CellDataR.RED : CellDataR.GREEN;
				if (reverseCounts > 0) {
					boolean firstTime = true;
					for (int frame = cell.startFrame; frame <= cell.endFrame; frame++) {
						if (cell.celldata.getStoreGreenRedChannel(frame)) {
							if (firstTime)
								firstTime = false;
							else
								cell.celldata.setStoreGreenRedChannel(frame);
						}
					}
				}
			}
		}
	}

	private void splitCells() {
		ListIterator<Cell> it = cells.listIterator();
		while (it.hasNext()) {
			Cell cell = it.next();
			cell.cellNumber = it.previousIndex();

			if (cell.endFrame > cell.startFrame) { // Each new cell starts with the cut: newCell.startFrame = CutFrame
				// Find next cut (REJECT_MANUAL, REJECT_DRAMATIC_CHANGE, or REJECT_404)
				int nextCutFrame = -1;
				int frame = cell.startFrame + 1;
				while (frame <= cell.endFrame && nextCutFrame == -1) {
					if (cell.celldata.whichRejectionInFrame(frame) == CellDataR.REJECT_MANUAL
							|| cell.celldata.whichRejectionInFrame(frame) == CellDataR.REJECT_DRAMATIC_CHANGE
							|| cell.celldata.whichRejectionInFrame(frame) == CellDataR.REJECT_404)
						nextCutFrame = frame;
					frame++;
				}

				if (nextCutFrame > cell.startFrame) {
					// Cut the cell in two, and look in the first whether there is a
					// REJECT_WHOLE_TRAJ or getStopWholeCellRejection() frame).
					splitCells(nextCutFrame, it);
					if (params.test)
						IJ.log("Cell #" + cell.cellNumber + " split in two, from frame " + (cell.startFrame + 1)
								+ " to frame " + nextCutFrame + ".");
					updateRejectionStatusOfACell(cell);

				} else {
					// No cut was find until the end of the cell.
					// Update the rejection status of the cell.
					updateRejectionStatusOfACell(cell);
				}
			} else if (cell.endFrame == cell.startFrame) {
				cell.rejectCell = cell.celldata.whichRejectionInFrame(cell.endFrame);
			}
		}
	}

	private void updateRejectionStatusOfACell(Cell cell) {
		// If there is a getStopWholeCellRejection in one of the frames, remove all
		// REJECT_WHOLE_TRAJ from the frames
		int stopWholeCellRejection = cell.celldata.getStopWholeCellRejection();
		if (stopWholeCellRejection >= 0 && stopWholeCellRejection >= cell.startFrame
				&& stopWholeCellRejection <= cell.endFrame) {
			for (int frame = cell.startFrame; frame <= cell.endFrame; frame++) {
				if (cell.celldata.whichRejectionInFrame(frame) == CellDataR.REJECT_WHOLE_TRAJ)
					cell.celldata.rejectFrame(frame, CellDataR.NOT_REJECTED);
			}
			cell.rejectCell = CellDataR.NOT_REJECTED;
			cell.celldata.stopWholeCellRejection(-1);
			if (params.test)
				IJ.log("Cell #" + cell.cellNumber + " not rejected anymore, between frame " + (cell.startFrame + 1)
						+ " and frame " + (cell.endFrame + 1) + ".");
		} else { // If there is a REJECT_WHOLE_TRAJ frame, fire it up to rejectCell
			for (int frame = cell.startFrame; frame <= cell.endFrame; frame++) {
				if (cell.celldata.whichRejectionInFrame(frame) == CellDataR.REJECT_WHOLE_TRAJ) {
					cell.rejectCell = CellDataR.REJECT_WHOLE_TRAJ;
					if (params.test)
						IJ.log("Cell #" + cell.cellNumber + " is fully rejected, between frame " + (cell.startFrame + 1)
								+ " and frame " + (cell.endFrame + 1) + ".");
				}
			}
		}

	}

	private void updateTrajLengthRejection() {
		ListIterator<Cell> it = cells.listIterator();
		while (it.hasNext()) {
			Cell cell = it.next();
			if (cell.rejectCell != CellDataR.REJECT_WHOLE_TRAJ) {
				if (cell.getLastNonRejectedFrame() - cell.getFirstNonRejectedFrame() + 1 < params.minTrajLength)
					cell.rejectCell = CellDataR.REJECT_TRAJ_LENGTH;
				else
					cell.rejectCell = CellDataR.NOT_REJECTED;
			}
		}
	}

	private void splitCells(int frameNewCell, ListIterator<Cell> it) {
		Cell previousCell = it.previous();
		previousCell.endFrame = frameNewCell - 1;
		previousCell.buildTrajectory();

		Cell newCell = new Cell(frameNumber, it.nextIndex() + 1, previousCell.celldata, linearity, linearityPrecised,
				params);
		newCell.startFrame = frameNewCell;
		for (int k = newCell.startFrame; k <= newCell.endFrame; k++) {
			newCell.cellFrame[k] = previousCell.cellFrame[k];
			newCell.cellFrame[k].updateCell(newCell);
			previousCell.cellFrame[k] = null;
		}
		newCell.buildTrajectory();

		it.next();
		it.add(newCell);
		it.previous();
	}

	private void buildCells() {
		cells = new LinkedList<Cell>();

		ListIterator<CellDataR> it = cellData.listIterator();
		while (it.hasNext()) {
			CellDataR cellR = it.next();
			CellData cell = cellR.getCellData();
			if (cell.getEndFrame() >= cell.getStartFrame()) {
				cells.add(new Cell(frameNumber, it.previousIndex(), cellR, linearity, linearityPrecised, params));
				for (int ij = cell.getStartFrame() - 1; ij < cell.getEndFrame(); ij++) {
					cells.getLast().cellFrame[ij] = new CellFrame(cell, ij, cells.getLast(), rt, linearityPrecised,
							params);
					// if (cells.getLast().cellFrame[ij].whichRejection() ==
					// CellDataR.REJECT_WHOLE_TRAJ)
					// cells.getLast().rejectCell = CellDataR.REJECT_WHOLE_TRAJ;
				}
				cells.getLast().buildTrajectory();
			}
		}
	}

	public Results(ArrayList<CellData> cellData, int frameNumber, Params params) {
		this(moveToCellDataR(cellData, frameNumber), params, frameNumber);
	}

	private static ArrayList<CellDataR> moveToCellDataR(ArrayList<CellData> cellData, int frameNumber) {
		ArrayList<CellDataR> output = new ArrayList<CellDataR>();
		Iterator<CellData> it = cellData.listIterator();
		while (it.hasNext()) {
			output.add(new CellDataR(it.next(), frameNumber));
		}
		return output;
	}

	private void initialiseRt(ResultsTableMt rt, boolean initial) {
		rt.incrementCounter();
		rt.addValue(defaultHeadings[0], 0);
		rt.addValue(defaultHeadings[3], 0);
		rt.addValue(defaultHeadings[1], 0);
		rt.addValue(defaultHeadings[2], 0);
		if (initial) {
			CELL = rt.getColumnIndex(defaultHeadings[0]);
			COLOUR = rt.getColumnIndex(defaultHeadings[3]);
			PROTRUSION = rt.getColumnIndex(defaultHeadings[1]);
			AREA = rt.getColumnIndex(defaultHeadings[2]);
		}
		rt.deleteRow(rt.getCounter() - 1);

	}

	public void buildProtrusions(ImagePlus imp, boolean save, boolean redGreenMode) {
		// Once buildCurveMap(allRegions, cellData.get(index)) has been done in
		// buildOutput!

		Iterator<Cell> it = cells.listIterator();
		while (it.hasNext()) {
			it.next().buildProtrusions(imp, save, redGreenMode);
		}

		if (save) {

			if (params.twoColourAnalysis)
				IJ.selectWindow("Visualisation - Green channel");
			IJ.run("To ROI Manager");
			ij.plugin.frame.RoiManager.getRoiManager().runCommand("Save",
					params.childDir + File.separator + "stack-RoiSet.zip");

			ImagePlus imp2 = imp.duplicate();
			if (params.finalAddedSlice) {
				imp.getImageStack().deleteLastSlice();
			}
			Utils.saveTiff(imp, params.childDir + File.separator + "stack-ini.tif", false);
			IJ.wait(300);

			imp2.show();
			imp2.setOverlay(new Overlay());
			imp2.getOverlay().drawLabels(true);
			Analyzer.drawLabels(true);
			imp2.getOverlay().drawNames(true);
			imp2.getOverlay().drawBackgrounds(false);
			imp2.getOverlay().setLabelColor(Color.WHITE);
			imp2.getOverlay().setLabelFont(new Font("SansSerif", Font.BOLD, 18), false);
			imp2.setHideOverlay(false);
			IJ.run("From ROI Manager");
			imp2.flattenStack();
			imp2.hide();
			if (params.finalAddedSlice) {
				imp2.getImageStack().deleteLastSlice();
			}
			imp2.getCalibration().fps = 3;
			Utils.saveGif(imp2, params.childDir + File.separator + "stack.gif", true);

			saveTrajectories();

			rt.saveAsPrecise(params.childDir + File.separator + "1-Protrusion_contours.csv", 3);
			if (params.twoColourAnalysis)
				extractAndSaveEachColourSeparately(rt, "1-Protrusion_contours", 3);
			reduceRt();
			rt.saveAsPrecise(params.childDir + File.separator + "1-Protrusion_center_of_mass_positions.csv", 3);
			linearity.saveAsPrecise(params.childDir + File.separator + "2-Results.csv", 5);
			linearityPrecised.saveAsPrecise(params.childDir + File.separator + "2-Results_per_frame.csv", 5);
			if (params.twoColourAnalysis) {
				extractAndSaveEachColourSeparately(rt, "1-Protrusion_center_of_mass_positions", 3);
				extractAndSaveEachColourSeparately(rt, "2-Results", 5);
				extractAndSaveEachColourSeparately(rt, "2-Results_per_frame", 5);
			}

			params.save();
		}
	}

	@SuppressWarnings("deprecation")
	private void extractAndSaveEachColourSeparately(ResultsTableMt rt, String fileName, int precision) {
		ResultsTableMt green = new ResultsTableMt();
		initialiseRt(green, false);
		ResultsTableMt red = new ResultsTableMt();
		initialiseRt(red, false);

		if (rt.getCounter() <= 1)
			return;
		for (int row = 0; row < rt.getCounter(); row++) {
			ResultsTableMt.addRow(rt, (rt.getValue(COLOUR, row) == CellDataR.GREEN) ? green : red, row);
		}

		green.saveAsPrecise(params.childDir + File.separator + fileName + "_green.csv", precision);
		red.saveAsPrecise(params.childDir + File.separator + fileName + "_red.csv", precision);
	}

	public void buildProtrusions(ImagePlus imp, boolean save) {
		buildProtrusions(imp, save, false);
	}

	private void saveTrajectories() {
		Iterator<Cell> it = cells.listIterator();
		ResultsTableMt traj = new ResultsTableMt();

		while (it.hasNext()) {
			Cell cell = it.next();
			if (cell.rejectCell == CellDataR.NOT_REJECTED) {
				int firstFrame = cell.getFirstNonRejectedFrame();
				int lastFrame = cell.getLastNonRejectedFrame();
				for (int frame = firstFrame; frame <= lastFrame; frame++) {
					if (cell.cellFrame[frame].whichRejection() == CellDataR.NOT_REJECTED) {
						traj.incrementCounter();
						traj.addValue(ResultsTableMt.FRAME, frame);
						traj.addValue("Cell number", cell.cellNumber);
						traj.addValue(defaultHeadings[3], cell.colour);
						double[] xyCentreOfMass = cell.cellFrame[frame].getCentreOfMass();
						traj.addValue(ResultsTableMt.X, xyCentreOfMass[0]);
						traj.addValue(ResultsTableMt.Y, xyCentreOfMass[1]);
					}
				}
			}
		}

		traj.saveAsPrecise(params.childDir + File.separator + "0-Trajectories.csv", 3);
	}

	@SuppressWarnings("deprecation")
	private void reduceRt() {
		if (rt.getCounter() <= 1)
			return;
		int cell = (int) rt.getValue(CELL, 0);
		int protrusion = (int) rt.getValue(PROTRUSION, 0);
		int frame = (int) rt.getValue(ResultsTableMt.FRAME, 0);
		double[] centreOfMass = Utils.getCentreOfMass(cells.get(cell).cellFrame[frame].pols.get(protrusion));
		rt.setValue(ResultsTableMt.X, 0, centreOfMass[0]);
		rt.setValue(ResultsTableMt.Y, 0, centreOfMass[1]);
		for (int i = 1; i < rt.getCounter(); i++) {
			while (i < rt.getCounter() && rt.getValue(CELL, i) == cell && rt.getValue(PROTRUSION, i) == protrusion
					&& rt.getValue(ResultsTableMt.FRAME, i) == frame) {
				rt.deleteRow(i);
			}
			if (i < rt.getCounter()) {
				cell = (int) rt.getValue(CELL, i);
				protrusion = (int) rt.getValue(PROTRUSION, i);
				frame = (int) rt.getValue(ResultsTableMt.FRAME, i);
				centreOfMass = Utils.getCentreOfMass(cells.get(cell).cellFrame[frame].pols.get(protrusion));
				rt.setValue(ResultsTableMt.X, i, centreOfMass[0]);
				rt.setValue(ResultsTableMt.Y, i, centreOfMass[1]);
			}
		}
	}

	void setParams(Params paramsTemp) {
		params = paramsTemp;
		Iterator<Cell> it = cells.listIterator();
		while (it.hasNext()) {
			Cell cell = it.next();
			if (cell.endFrame >= cell.startFrame) {
				cell.setParams(paramsTemp);
				for (int ij = 0; ij < frameNumber; ij++) {
					cell.cellFrame[ij].setParams(paramsTemp);
				}
			}
		}
	}

	public void kill() {
		cells.clear();
		cellData.clear();
		rt = null;
		linearity = null;
		linearityPrecised = null;
		params = null;
	}

}
