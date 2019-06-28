package MP;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import Cell.CellData;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;

public class Results {

	public static int CELL, PROTRUSION, AREA;

	private static final String[] defaultHeadings = { "Cell", "Protrusion", "Area" };

	LinkedList<Cell> cells;
	int frameNumber;
	ArrayList<CellDataR> cellData;
	ImageStack stack = null;
	ImagePlus imp = null;
	ResultsTableMt rt = new ResultsTableMt();
	ResultsTableMt linearity = new ResultsTableMt(), linearityPrecised = new ResultsTableMt();
	private Params params;

	public Results(ArrayList<CellDataR> cellData, Params params) {
		this.params = params;

		this.cellData = cellData;
		initialiseStack();

		initialiseRt(rt, true);
		initialiseRt(linearity, false);
		initialiseRt(linearityPrecised, false);

		detectProbableCellEncounter();

		buildCells();
		splitCells();
	}

	private void detectProbableCellEncounter() {
		ListIterator<CellDataR> it = cellData.listIterator();
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

	private void splitCells() {
		ListIterator<Cell> it = cells.listIterator();
		while (it.hasNext()) {
			Cell cell = it.next();
			cell.cellNumber = it.previousIndex();

			if (cell.endFrame >= cell.startFrame) {
				updateTrajLengthRejection(cell);
				int stopWholeCellRejection = cell.celldata.getStopWholeCellRejection();
				if (!cell.celldata.isCellRejected()
						|| (stopWholeCellRejection >= cell.startFrame && stopWholeCellRejection <= cell.endFrame)) {
					boolean previousWasRejected = false;
					boolean encounteredReject_Whole_Traj = false;
					int ij = cell.startFrame;
					while (ij <= cell.endFrame && (!previousWasRejected
							|| (previousWasRejected && cell.cellFrame[ij].reject != CellDataR.NOT_REJECTED))) {
						previousWasRejected = cell.cellFrame[ij].reject != CellDataR.NOT_REJECTED;

						// - If locally, one finds REJECT_WHOLE_TRAJ, continue anyway until
						// next rejection.
						if (cell.celldata.whichRejectionInFrame(ij) == CellDataR.REJECT_WHOLE_TRAJ) {
							previousWasRejected = false;
							encounteredReject_Whole_Traj = true;
						}

						ij++;
					}
					previousWasRejected = false;

					// - If stopWholeCellRejection != -1 and its frame is in cell,
					// cancel REJECT_WHOLE_TRAJ in cellReject and each frame of cell,
					// and cancel it in the corresponding cellData before erasing
					// stopWholeCellRejection itself.
					if (encounteredReject_Whole_Traj && stopWholeCellRejection >= cell.startFrame
							&& stopWholeCellRejection < ij - 1) {
						for (int k = cell.startFrame; k < ij - 1; k++) {
							cell.celldata.rejectFrame(k, CellDataR.NOT_REJECTED);
							cell.cellFrame[k].reject = CellDataR.NOT_REJECTED;
						}
						cell.rejectCell = CellDataR.NOT_REJECTED;

						cell.celldata.stopWholeCellRejection(-1);
					} else if (encounteredReject_Whole_Traj) {
						cell.rejectCell = CellDataR.REJECT_WHOLE_TRAJ;
					}

					if (ij <= cell.endFrame) {
						splitCells(ij, it);
					}
				}
			}
		}
	}

	private void updateTrajLengthRejection(Cell cell) {
		if (cell.rejectCell != CellDataR.REJECT_WHOLE_TRAJ) {
			if (cell.getLastNonRejectedFrame() - cell.getFirstNonRejectedFrame() + 1 < params.minTrajLength)
				cell.rejectCell = CellDataR.REJECT_TRAJ_LENGTH;
			else
				cell.rejectCell = CellDataR.NOT_REJECTED;
		}
	}

	private void splitCells(int frameNewCell, ListIterator<Cell> it) {
		Cell previousCell = it.previous();
		previousCell.endFrame = frameNewCell - 1;
		previousCell.buildTrajectory();
		updateTrajLengthRejection(previousCell);

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
				cells.getLast().rejectCell = cellR.whichCellRejection();
				for (int ij = cell.getStartFrame() - 1; ij < cell.getEndFrame(); ij++) {
					cells.getLast().cellFrame[ij] = new CellFrame(cell, ij, cells.getLast(), rt, linearityPrecised,
							params);
					if (!cellR.isFrameRejected(ij)) {
						cells.getLast().cellFrame[ij].reject = cellR.whichCellRejection();
					} else {
						cells.getLast().cellFrame[ij].reject = cellR.whichRejectionInFrame(ij);
					}
				}
				cells.getLast().buildTrajectory();
			}
		}
	}

	public Results(ArrayList<CellData> cellData, Params params, int frameNumber) {
		this(moveToCellDataR(cellData, frameNumber), params);
	}

	private static ArrayList<CellDataR> moveToCellDataR(ArrayList<CellData> cellData, int frameNumber) {
		ArrayList<CellDataR> output = new ArrayList<CellDataR>();
		Iterator<CellData> it = cellData.listIterator();
		while (it.hasNext()) {
			output.add(new CellDataR(it.next(), frameNumber));
		}
		return output;
	}

	void initialiseStack() {
		if (stack == null) {
			int ik = 0;
			for (int i = 0; i < cellData.size(); i++) {
				while (i < cellData.size() && (cellData.get(i) == null || cellData.get(i).getCellData() == null
						|| cellData.get(i).getCellData().getCurveMap() == null
						|| cellData.get(i).getCellData().getCurveMap().getzVals() == null)) {
					i++;
				}
				if (i < cellData.size() && ik == 0) {
					frameNumber = cellData.get(i).getCellData().getCellRegions().length;
					ik = i;
				}
			}
			CellData cell = cellData.get(ik).getCellData();
			stack = new ImageStack(cell.getImageWidth(), cell.getImageHeight());
			for (int ij = 0; ij < frameNumber; ij++) {
				stack.addSlice(new ByteProcessor(cell.getImageWidth(), cell.getImageHeight()));
			}
		} else {
			frameNumber = stack.getSize();
			int width = stack.getWidth();
			int height = stack.getHeight();
			stack = new ImageStack(width, height);
			for (int i = 0; i < frameNumber; i++) {
				stack.addSlice(new ByteProcessor(width, height));
			}
		}
		imp = new ImagePlus("", stack);
	}

	private void initialiseRt(ResultsTableMt rt, boolean initial) {
		rt.incrementCounter();
		rt.addValue(defaultHeadings[0], 0);
		if (initial) {
			CELL = rt.getColumnIndex(defaultHeadings[0]);
			rt.addValue(defaultHeadings[1], 0);
			PROTRUSION = rt.getColumnIndex(defaultHeadings[1]);
			rt.addValue(defaultHeadings[2], 0);
			AREA = rt.getColumnIndex(defaultHeadings[2]);
		}
		rt.deleteRow(rt.getCounter() - 1);

	}

	public void buildProtrusions(boolean save) {
		// Once buildCurveMap(allRegions, cellData.get(index)) has been done in
		// buildOutput!

		Iterator<Cell> it = cells.listIterator();
		while (it.hasNext()) {
			it.next().buildProtrusions(imp, save);
		}

		if (save) {

			IJ.run("To ROI Manager");
			ij.plugin.frame.RoiManager.getRoiManager().runCommand("Save",
					params.childDir + File.separator + "stack-RoiSet.zip");

			Utils.saveTiff(imp, params.childDir + File.separator + "stack-ini.tif", false);
			IJ.wait(300);
			ImagePlus imp2 = imp.duplicate();
			imp2.flattenStack();
			imp2.hide();
			Utils.saveGif(imp2, params.childDir + File.separator + "stack.gif", true);

//			ij.plugin.frame.RoiManager.getRoiManager().removeAll();
//			ij.plugin.frame.RoiManager.getRoiManager().close();
//
//			imp = new ImagePlus();
//			Open_MP op = new Open_MP("" + params.childDir, imp);
//			op.run(null);

			rt.saveAsPrecise(params.childDir + File.separator + "1-Protrusion_contours.csv", 3);
			reduceRt();
			rt.saveAsPrecise(params.childDir + File.separator + "1-Protrusion_center_of_mass_positions.csv", 3);
			linearity.saveAsPrecise(params.childDir + File.separator + "2-Results.csv", 5);
			linearityPrecised.saveAsPrecise(params.childDir + File.separator + "2-Results_per_frame.csv", 5);

			params.save();
		}
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
		stack = null;
		rt = null;
		linearity = null;
		linearityPrecised = null;
		params = null;
	}

}
