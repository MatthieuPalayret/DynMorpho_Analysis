package MP;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import Cell.CellData;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.ImageCanvas;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Overlay;
import ij.plugin.filter.Analyzer;

public class ParamVisualisation implements DialogListener, MouseListener {

	private Params params, paramTemp;
	private ImagePlus image;
	private ImageCanvas canvas;
	private Results results;
	private static final String[] rejectionRadioButton = new String[] { "No rejection",
			"Reject cell in a single frame?", "Reject a whole cell trajectory?" };

	public ParamVisualisation(Params params, Results results, ImageStack stack) {
		super();
		this.params = params;
		paramTemp = params.clone();
		this.results = results;
		results.stack = stack;
		image = new ImagePlus("Previsualisation", results.stack);
		results.imp = image;
	}

	public void run() {
		image.updateAndDraw();
		image.show();
		image.getWindow().setLocation(600, 100);
		canvas = image.getWindow().getCanvas();
		canvas.addMouseListener(this);

		// Interactively ask for parameters
		NonBlockingGenericDialog gui = new NonBlockingGenericDialog("Choose parameters");

		gui.addMessage("Parameters:");
		gui.addSlider("Minimal curvature for protrusions (°):", -180, 180, -paramTemp.curvatureMinLevel, 1);
		gui.addSlider("Minimal length of a trajectory (s):", 0, 5.0D * paramTemp.minTrajLength * paramTemp.frameLengthS,
				paramTemp.minTrajLength * paramTemp.frameLengthS);
		gui.addSlider("Dramatic cell area in-/de-crease (%):", 100, 200, paramTemp.dramaticAreaIncrease, 1);
		gui.addSlider("Minimal area of a protrusion (µm²):", 0, 1000.0D * Math.pow(paramTemp.pixelSizeNm / 1000.0D, 2),
				paramTemp.minAreaDetection * Math.pow(paramTemp.pixelSizeNm / 1000.0D, 2), 0.02);
		gui.addSlider("Maximal protrusion to cell surface ratio:", 0, 1.0D, paramTemp.maxProtrusionToCellAreaRatio,
				0.05);
		gui.addSlider("Smoothing window (pixel):", 0, 20, paramTemp.smoothingCoeffInPixels, 1);
		gui.addCheckbox("Detect uropods?", paramTemp.detectUropod);

		if (!Params.officialVersion) {
			gui.addSlider("Reducing coefficient (if applicable):", 0, 1, paramTemp.reducingCoeff, 0.01);
			String[] Methods = { "Simple smoothing", "Derivative of the curvature", "Remove equivalent curvature",
					"Remove math-equivalent curvature" };
			gui.addChoice("Method:", Methods, "Simple smoothing");
		}

		gui.addMessage("Interactive rejection of cells:");
		gui.addRadioButtonGroup("Interactive rejection of cells:", rejectionRadioButton, 3, 1, rejectionRadioButton[0]);

		gui.addDialogListener(this);
		gui.setLocation(150, 100);

		gui.showDialog();

		if (gui.wasCanceled()) {
			cancelAllRejections();
			updateAnalysis(params, false);
			canvas.removeMouseListener(this);
			gui.removeAll();
		} else if (gui.wasOKed()) {
			canvas.removeMouseListener(this);
			updateAnalysis(paramTemp, false);
			params = paramTemp;
			gui.removeAll();
		}
	}

	private void cancelAllRejections() {
		for (int i = 0; i < results.cellData.size(); i++) {
			CellDataR cell = results.cellData.get(i);
			cell.rejectCell(CellDataR.NOT_REJECTED);
			for (int j = 0; j < cell.getFrameNumber(); j++)
				cell.rejectFrame(j, CellDataR.NOT_REJECTED);
		}
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gui, AWTEvent e) {

		paramTemp.curvatureMinLevel = -gui.getNextNumber();
		paramTemp.minTrajLength = (int) (gui.getNextNumber() / paramTemp.frameLengthS);
		paramTemp.dramaticAreaIncrease = gui.getNextNumber();
		paramTemp.minAreaDetection = gui.getNextNumber() / Math.pow(paramTemp.pixelSizeNm / 1000.0D, 2);
		paramTemp.maxProtrusionToCellAreaRatio = gui.getNextNumber();
		paramTemp.smoothingCoeffInPixels = (int) gui.getNextNumber();
		paramTemp.detectUropod = gui.getNextBoolean();

		if (!Params.officialVersion) {
			paramTemp.reducingCoeff = gui.getNextNumber();
			paramTemp.method = gui.getNextChoiceIndex() + 1;
		}

		String rejection = gui.getNextRadioButton();
		paramTemp.postRejectCellFrame = rejection.equals(rejectionRadioButton[1]);
		paramTemp.postRejectWholeCell = rejection.equals(rejectionRadioButton[2]);

		updateImage();
		return true;
	}

	private boolean imageLock = false;

	private synchronized boolean aquireImageLock() {
		if (imageLock)
			return false;
		return imageLock = true;
	}

	private void updateImage() {
		if (aquireImageLock()) {
			// Run in a new thread to allow the GUI to continue updating
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// Continue while the parameter is changing
						boolean parametersChanged = true;
						while (parametersChanged) {
							// Store the parameters to be processed
							Params paramtemp = paramTemp.clone();
							// Do something with parameters
							updateAnalysis(paramtemp, true);
							// Check if the parameters have changed again
							parametersChanged = !paramtemp.compare(paramTemp);
						}
					} finally {
						// Ensure the running flag is reset
						imageLock = false;
					}
				}
			}).start();
		}
	}

	private void updateAnalysis(Params paramTemp, boolean previsualisation) {
		results = new Results(results.cellData, paramTemp);
		if (image.getOverlay() == null) {
			image.setOverlay(new Overlay());
			image.getOverlay().drawLabels(true);
			Analyzer.drawLabels(true);
			image.getOverlay().drawNames(true);
			image.getOverlay().drawBackgrounds(false);
			image.getOverlay().setLabelColor(Color.WHITE);
			image.getOverlay().setLabelFont(new Font("SansSerif", Font.BOLD, 18), false);
		} else
			image.getOverlay().clear();
		results.imp = image;
		results.stack = image.getStack();
		results.buildProtrusions(!previsualisation);
		image.setHideOverlay(false);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (paramTemp.postRejectCellFrame || paramTemp.postRejectWholeCell) {
			int x = canvas.offScreenX(e.getX());
			int y = canvas.offScreenY(e.getY());
			int frame = image.getCurrentSlice() - 1;
			for (int i = 0; i < results.cellData.size(); i++) {
				CellDataR cell = results.cellData.get(i);
				CellData cellD = cell.getCellData();
				if (cellD.getStartFrame() - 1 <= frame && frame <= cellD.getEndFrame() - 1
						&& cellD.getCellRegions()[frame].getPolygonRoi(cellD.getCellRegions()[frame].getMask())
								.contains(x, y)) {
					int cellNumber = findCellNumber(frame, x, y);
					if (paramTemp.postRejectWholeCell) {
						if (results.cells.get(cellNumber).rejectCell == CellDataR.REJECT_WHOLE_TRAJ) {
							IJ.log("Addition of cell #" + cellNumber + " for all frames.");
							cell.stopWholeCellRejection(frame);
						} else if (results.cells.get(cellNumber).rejectCell == CellDataR.NOT_REJECTED) {
							IJ.log("Suppression of cell #" + cellNumber + " for all frames.");
							cell.rejectFrame(frame, CellDataR.REJECT_WHOLE_TRAJ);
						}
						// TODO if in results.cells.get
//						if (cell.whichCellRejection() == CellDataR.REJECT_WHOLE_TRAJ) {
//							IJ.log("Addition of cell #" + cellNumber + " for all frames.");
//							cell.rejectCell(CellDataR.NOT_REJECTED);
//						} else if (!cell.isCellRejected()) {
//							IJ.log("Suppression of cell #" + cellNumber + " for all frames.");
//							cell.rejectCell(CellDataR.REJECT_WHOLE_TRAJ);
//						}
					} else if (paramTemp.postRejectCellFrame) {
						if (cell.whichRejectionInFrame(frame) == CellDataR.REJECT_MANUAL) {
							IJ.log("Addition of cell #" + cellNumber + " in frame #" + (frame + 1) + ".");
							cell.rejectFrame(frame, CellDataR.NOT_REJECTED);
						} else if (!cell.isFrameRejected(frame)) {
							IJ.log("Supression of cell #" + cellNumber + " in frame #" + (frame + 1) + ".");
							cell.rejectFrame(frame, CellDataR.REJECT_MANUAL);
						} else {
							IJ.log("The cell #" + cellNumber + " in frame #" + (frame + 1) + " is already rejected ("
									+ cell.isFrameRejected(frame) + ").");
						}
					}
				}
			}
			updateAnalysis(paramTemp.clone(), true);
		}

	}

	private int findCellNumber(int frame, int x, int y) {
		for (int i = 0; i < results.cells.size(); i++) {
			Cell cell = results.cells.get(i);
			if (cell.startFrame <= frame && frame <= cell.endFrame && cell.cellFrame[frame].contains(x, y)) {
				return cell.cellNumber;
			}
		}
		return -1;
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	public void kill() {
		paramTemp = null;
		image.close();
		image.flush();
		canvas = null;
		results.kill();
	}

}
