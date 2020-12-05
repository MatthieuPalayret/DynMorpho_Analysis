package MP.params;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.util.ListIterator;

import javax.swing.JLabel;
import javax.swing.JRadioButton;

import MP.objects.Cell;
import MP.objects.CellDataR;
import MP.objects.Results;
import MP.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;
import ij.process.LUT;
import net.calm.iaclasslibrary.Cell.CellData;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;

public class ParamVisualisationTwoColour extends ParamVisualisation {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2992022379079451670L;

	protected ImagePlus imageRed;
	protected Results resultsRed;
	protected JRadioButton rdbtnExchangeGreenRedCell;
	protected ij.gui.ImageCanvas canvasRed;

	public ParamVisualisationTwoColour(Params params, Results resultsGreen, ImageStack stackGreen, Results resultsRed,
			ImageStack stackRed) {
		super();

		thisClass = this;

		this.params = params;
		paramTemp = params.clone();
		this.results = resultsGreen;
		this.resultsRed = resultsRed;

		image = new ImagePlus("Visualisation - Green channel", stackGreen);
		image = new ImagePlus("Visualisation - Green channel", GenUtils.convertStack(image, 32).getImageStack());
		frame = image.getCurrentSlice() - 1;
		image.setLut(LUT.createLutFromColor(Color.GREEN));

		imageRed = new ImagePlus("Visualisation - Red channel", stackRed);
		imageRed = new ImagePlus("Visualisation - Red channel", GenUtils.convertStack(imageRed, 32).getImageStack());
		imageRed.setSlice(frame + 1);
		imageRed.setLut(LUT.createLutFromColor(Color.RED));

		initialiseAllGreenRedCellExchange();

		image.updateAndDraw();
		image.show();
		IJ.run("Enhance Contrast", "saturated=0.35");
		image.getWindow().setLocation(505, 20);
		canvas = image.getCanvas();
		ImagePlus.addImageListener(this);
		canvas.addKeyListener(this);
		canvas.addMouseListener(this);

		imageRed.updateAndDraw();
		imageRed.show();
		IJ.run("Enhance Contrast", "saturated=0.35");
		imageRed.getWindow().setLocation(1010, 20);
		canvasRed = imageRed.getCanvas();
		canvasRed.addKeyListener(this);
		canvasRed.addMouseListener(this);

		this.setBounds(20, 20, 500, 440);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 139, 0, 53, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 1.0, Double.MIN_VALUE };
		getContentPane().setLayout(gridBagLayout);

		addKeyListener(this);

		setMainFeatures();

		lblInteractiveRejectionOfCells = new JLabel("Interactive rejection of cells:");
		lblInteractiveRejectionOfCells.setFont(font);
		GridBagConstraints gbc_lblInteractiveRejectionOfCells = new GridBagConstraints();
		gbc_lblInteractiveRejectionOfCells.gridwidth = 3;
		gbc_lblInteractiveRejectionOfCells.insets = new Insets(0, 0, 5, 5);
		gbc_lblInteractiveRejectionOfCells.gridx = 1;
		gbc_lblInteractiveRejectionOfCells.gridy = 10;
		getContentPane().add(lblInteractiveRejectionOfCells, gbc_lblInteractiveRejectionOfCells);

		rdbtnNoRejection = new JRadioButton("No rejection");
		buttonGroup.add(rdbtnNoRejection);
		GridBagConstraints gbc_rdbtnNoRejection = new GridBagConstraints();
		gbc_rdbtnNoRejection.gridwidth = 2;
		gbc_rdbtnNoRejection.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNoRejection.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnNoRejection.gridx = 1;
		gbc_rdbtnNoRejection.gridy = 11;
		if (!paramTemp.postRejectCellFrame && !paramTemp.postRejectWholeCell)
			rdbtnNoRejection.setSelected(true);
		rdbtnNoRejection.addActionListener(this);
		getContentPane().add(rdbtnNoRejection, gbc_rdbtnNoRejection);

		rdbtnRejectCellIn = new JRadioButton("Reject cell in a single frame?");
		buttonGroup.add(rdbtnRejectCellIn);
		GridBagConstraints gbc_rdbtnRejectCellIn = new GridBagConstraints();
		gbc_rdbtnRejectCellIn.gridwidth = 2;
		gbc_rdbtnRejectCellIn.anchor = GridBagConstraints.WEST;
		gbc_rdbtnRejectCellIn.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnRejectCellIn.gridx = 1;
		gbc_rdbtnRejectCellIn.gridy = 12;
		if (paramTemp.postRejectCellFrame && !paramTemp.postRejectWholeCell)
			rdbtnRejectCellIn.setSelected(true);
		rdbtnRejectCellIn.addActionListener(this);
		getContentPane().add(rdbtnRejectCellIn, gbc_rdbtnRejectCellIn);

		rdbtnRejectAWhole = new JRadioButton("Reject a whole cell trajectory?");
		buttonGroup.add(rdbtnRejectAWhole);
		GridBagConstraints gbc_rdbtnRejectAWhole = new GridBagConstraints();
		gbc_rdbtnRejectAWhole.gridwidth = 2;
		gbc_rdbtnRejectAWhole.anchor = GridBagConstraints.WEST;
		gbc_rdbtnRejectAWhole.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnRejectAWhole.gridx = 1;
		gbc_rdbtnRejectAWhole.gridy = 13;
		if (!paramTemp.postRejectCellFrame && paramTemp.postRejectWholeCell)
			rdbtnRejectAWhole.setSelected(true);
		rdbtnRejectAWhole.addActionListener(this);
		getContentPane().add(rdbtnRejectAWhole, gbc_rdbtnRejectAWhole);

		rdbtnExchangeGreenRedCell = new JRadioButton("Change red <-> green cell?");
		buttonGroup.add(rdbtnExchangeGreenRedCell);
		GridBagConstraints gbc_rdbtnExchangeGreenRedCell = new GridBagConstraints();
		gbc_rdbtnExchangeGreenRedCell.gridwidth = 2;
		gbc_rdbtnExchangeGreenRedCell.anchor = GridBagConstraints.WEST;
		gbc_rdbtnExchangeGreenRedCell.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnExchangeGreenRedCell.gridx = 1;
		gbc_rdbtnExchangeGreenRedCell.gridy = 14;
		rdbtnExchangeGreenRedCell.setSelected(false);
		rdbtnExchangeGreenRedCell.addActionListener(this);
		getContentPane().add(rdbtnExchangeGreenRedCell, gbc_rdbtnExchangeGreenRedCell);

		setFinalButtons(15);

		updateImage();

		this.setVisible(true);
	}

	protected void initialiseAllGreenRedCellExchange() {
		// (Re)initialisation :
		// Set all cells from the green channel to GREEN
		for (int i = 0; i < results.cellData.size(); i++) {
			CellDataR cell = results.cellData.get(i);
			cell.setOriginalCellColour(CellDataR.GREEN);
		}
		// Set all cells from the red channel to RED (not necessary)
		for (int i = 0; i < resultsRed.cellData.size(); i++) {
			CellDataR cell = resultsRed.cellData.get(i);
			cell.setOriginalCellColour(CellDataR.RED);
		}

		// Thanks to resultsRed, differentiate in results the green-only cells from the
		// red-and-green cells.
		// 1. Looking at each cell in the green channel
		ListIterator<Cell> it = results.cells.listIterator();
		while (it.hasNext()) {
			Cell cell = it.next();
			int frame = cell.startFrame;
			while (cell.celldata.whichOriginalCellColour() == CellDataR.GREEN && frame <= cell.endFrame) {
				PolygonRoi polG = new PolygonRoi(
						Utils.buildFloatPolygon(cell.cellFrame[frame].contourX, cell.cellFrame[frame].contourY),
						Roi.POLYGON);

				// 2. Search whether there is a cell in the red channel in the same frame which
				// intersects with this cell
				ListIterator<Cell> itRed = resultsRed.cells.listIterator();
				while (cell.celldata.whichOriginalCellColour() == CellDataR.GREEN && itRed.hasNext()) {
					Cell cellRed = itRed.next();
					if (cellRed.startFrame <= frame && frame <= cellRed.endFrame) {
						PolygonRoi polR = new PolygonRoi(Utils.buildFloatPolygon(cellRed.cellFrame[frame].contourX,
								cellRed.cellFrame[frame].contourY), Roi.POLYGON);

						if (params.test && cell.cellNumber == 0 && frame < 2) {
							ImagePlus img = new ImagePlus("",
									imageRed.getImageStack().getProcessor(frame + 1).duplicate());
							Overlay ov = img.getOverlay();
							if (ov == null)
								ov = new Overlay();
							img.setOverlay(ov);
							polR.setStrokeColor(Color.RED);
							polR.setStrokeWidth(1.5);
							ov.add(polR);
							img.setRoi(polR);
							polG.setStrokeColor(Color.GREEN);
							polG.setStrokeWidth(1.5);
							ov.add(polG);
							img.draw();
							img.show();
						}

						// 3. If there is a red cells that intersects with this green cell in a frame,
						// then the green cell must be a red-and-green cell!
						if (Utils.intersect(polG, polR))
							cell.celldata.setOriginalCellColour(CellDataR.RED);
					}
				}

				frame++;
			}
		}
	}

	@Override
	protected void disposeThis() {
		canvasRed.removeKeyListener(this);
		canvasRed.removeMouseListener(this);
		rdbtnExchangeGreenRedCell.removeActionListener(this);
		super.disposeThis();

		IJ.selectWindow("Visualisation - Red channel");
		ImagePlus temp = IJ.getImage();
		if (temp != null && temp.getTitle().startsWith("Visualisation - Red channel"))
			temp.close();
	}

	private boolean imageLock = false;

	private synchronized boolean aquireImageLock() {
		if (imageLock)
			return false;
		return imageLock = true;
	}

	@Override
	protected void updateImage() {
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
							results = updateAnalysis(paramtemp, true, results, image, false);
							updateAnalysis(paramtemp, true, results, imageRed, true);
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

	@Override
	public void mouseClicked(MouseEvent e) {
		if (paramTemp.postRejectCellFrame || paramTemp.postRejectWholeCell || paramTemp.postExchangeGreenRedCell) {
			boolean isClickInGreen = e.getSource() == canvas;
			ImageCanvas canvasTemp = isClickInGreen ? canvas : canvasRed;
			Results resultsTemp = results;
			ImagePlus imageTemp = isClickInGreen ? image : imageRed;
			int COLOUR = CellDataR.GREEN;

			int x = canvasTemp.offScreenX(e.getX());
			int y = canvasTemp.offScreenY(e.getY());
			int frame = imageTemp.getCurrentSlice() - 1;
			for (int i = 0; i < resultsTemp.cellData.size(); i++) {
				CellDataR cell = resultsTemp.cellData.get(i);
				CellData cellD = cell.getCellData();
				if (cellD.getStartFrame() - 1 <= frame && frame <= cellD.getEndFrame() - 1
						&& cellD.getCellRegions()[frame].getPolygonRoi(cellD.getCellRegions()[frame].getMask())
								.contains(x, y)) {
					int cellNumber = findCellNumber(frame, x, y, COLOUR);
					if (paramTemp.postRejectWholeCell) {
						if (resultsTemp.cells.get(cellNumber).rejectCell == CellDataR.REJECT_WHOLE_TRAJ) {
							IJ.log("Addition of cell #" + cellNumber + " for all frames.");
							cell.stopWholeCellRejection(frame);
						} else if (resultsTemp.cells.get(cellNumber).cellFrame[frame]
								.whichRejection() == CellDataR.NOT_REJECTED) {
							IJ.log("Suppression of cell #" + cellNumber + " for all frames.");
							cell.rejectFrame(frame, CellDataR.REJECT_WHOLE_TRAJ);
						} else {
							IJ.log("The cell #" + cellNumber + " in frame #" + (frame + 1) + " is already rejected ("
									+ (cell.isFrameRejected(frame) ? cell.whichRejectionInFrame(frame)
											: resultsTemp.cells.get(cellNumber).rejectCell)
									+ ").");
						}
					} else if (paramTemp.postRejectCellFrame) {
						if (cell.whichRejectionInFrame(frame) == CellDataR.REJECT_MANUAL) {
							IJ.log("Addition of cell #" + cellNumber + " in frame #" + (frame + 1) + ".");
							cell.rejectFrame(frame, CellDataR.NOT_REJECTED);
						} else if (!cell.isFrameRejected(frame)
								&& resultsTemp.cells.get(cellNumber).rejectCell == CellDataR.NOT_REJECTED) {
							IJ.log("Supression of cell #" + cellNumber + " in frame #" + (frame + 1) + ".");
							cell.rejectFrame(frame, CellDataR.REJECT_MANUAL);
						} else {
							IJ.log("The cell #" + cellNumber + " in frame #" + (frame + 1) + " is already rejected ("
									+ (cell.isFrameRejected(frame) ? cell.whichRejectionInFrame(frame)
											: resultsTemp.cells.get(cellNumber).rejectCell)
									+ ").");
						}
					} else if (paramTemp.postExchangeGreenRedCell) {
						cell.setStoreGreenRedChannel(frame);
						boolean boolTemp = resultsTemp.cells.get(cellNumber).colour == CellDataR.RED;
						IJ.log("The " + (boolTemp ? "red-and-green" : "green-only") + " cell #" + cellNumber
								+ " is now considered as a " + (!boolTemp ? "red-and-green" : "green-only") + " cell.");
					}
				}
			}
			updateImage();
		}

	}

	protected int findCellNumber(int frame, int x, int y, int channel) {
		if (channel == CellDataR.GREEN)
			return findCellNumber(frame, x, y, results);
		else if (channel == CellDataR.RED)
			return findCellNumber(frame, x, y, resultsRed);
		return -1;
	}

	@Override
	public void kill() {
		resultsRed.kill();
		super.kill();
	}

	@Override
	protected void somethingHappened(Object source) {
		if (source == rdbtnExchangeGreenRedCell) {
			if (rdbtnExchangeGreenRedCell.isSelected()) {
				paramTemp.postExchangeGreenRedCell = true;
				paramTemp.postRejectCellFrame = false;
				paramTemp.postRejectWholeCell = false;
			}
		} else if (source == rdbtnNoRejection) {
			if (rdbtnNoRejection.isSelected()) {
				paramTemp.postExchangeGreenRedCell = false;
			}
			super.somethingHappened(source);
		} else if (source == rdbtnRejectCellIn) {
			if (rdbtnRejectCellIn.isSelected()) {
				paramTemp.postExchangeGreenRedCell = false;
			}
			super.somethingHappened(source);
		} else if (source == rdbtnRejectAWhole) {
			if (rdbtnRejectAWhole.isSelected()) {
				paramTemp.postExchangeGreenRedCell = false;
			}
			super.somethingHappened(source);
		} else if (source == btnOk) {
			updateAnalysis(paramTemp, false, results, image, false);
			params = paramTemp;
			disposeThis();
			RoiManager.getInstance().setVisible(false);
			finished = FINISHED;
		} else if (source == btnReset) {
			rdbtnExchangeGreenRedCell.setSelected(false);
			initialiseAllGreenRedCellExchange();
			super.somethingHappened(source);
		} else if (source == btnCancel) {
			initialiseAllGreenRedCellExchange();
			disposeThis();
			imageRed.close();
			super.somethingHappened(source);
		} else
			super.somethingHappened(source);
	}

	@Override
	public void imageClosed(ImagePlus imp) {
		if (imp == imageRed)
			imageRed.show();
		else
			super.imageClosed(imp);
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
		if (imp == imageRed && (imp.getCurrentSlice() - 1 != frame || image.getCurrentSlice() - 1 != frame)) {
			frame = imp.getCurrentSlice() - 1;
			image.setSlice(frame + 1);
		} else if (imp == image && (imp.getCurrentSlice() - 1 != frame || imageRed.getCurrentSlice() - 1 != frame)) {
			frame = imp.getCurrentSlice() - 1;
			imageRed.setSlice(frame + 1);
		}
	}

}
