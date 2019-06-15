package MP;

import java.awt.AWTEvent;
import java.awt.Canvas;
import java.awt.Color;
import java.util.ArrayList;

import Cell.CellData;
import UserVariables.UserVariables;
import UtilClasses.GenUtils;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.gui.ImageWindow;
import ij.gui.NonBlockingGenericDialog;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;

public class ParamPreview implements DialogListener, ImageListener {

	protected Params params, paramTemp;
	private ImagePlus image;
	private ImageStack stack8bit;
	private ImagePlus imageIni;
	private int frame = 0;
	private Canvas canvas;

	public ParamPreview(Params params, ImagePlus img) {
		super();
		this.params = params;
		paramTemp = params.clone();
		imageIni = img;
		stack8bit = GenUtils.convertStack(img.getImageStack(), 8);
		image = new ImagePlus("Previsualisation", GenUtils.convertStack(img, 32).getImageStack());
		frame = image.getCurrentSlice() - 1;
	}

	public void run() {
		image.updateAndDraw();
		image.show();
		image.getWindow().setLocation(600, 100);
		ImagePlus.addImageListener(this);
		canvas = image.getWindow().getCanvas();

		// Interactively ask for parameters
		NonBlockingGenericDialog gui = new NonBlockingGenericDialog("Choose parameters");

		gui.addMessage("General parameters:");
		gui.addStringField("Additional tag to the name", paramTemp.tagName, 10);
		gui.addNumericField("Pixel size (nm)", paramTemp.pixelSizeNm, 1);
		gui.addNumericField("Frame length (s)", paramTemp.frameLengthS, 2);

		gui.addMessage("Parameters for analysis:");
		gui.addCheckbox("Automatic intensity threshold?", paramTemp.autoThreshold);
		gui.addSlider("Contour intensity threshold ([0..1]):", 0, 1, paramTemp.greyThreshold, 0.005);
		gui.addSlider("Smoothing contour coefficient:", 0, 5, paramTemp.smoothingContour);
		double temp = paramTemp.minCellSurface * Math.pow(paramTemp.pixelSizeNm / 1000.0D, 2);
		gui.addSlider("Minimal area of a cell (µm²):", 0, 5 * temp, temp, 1);
		double temp2 = paramTemp.maxCellSurface * Math.pow(paramTemp.pixelSizeNm / 1000.0D, 2);
		gui.addSlider("Maximal area of a cell (µm²):", 0, 3 * temp2, temp2, 1);

		gui.addDialogListener(this);
		gui.setLocation(150, 100);

		gui.showDialog();

		if (gui.wasCanceled()) {
			// updateAnalysis(params, frame);
			ImagePlus.removeImageListener(this);
			gui.removeAll();
			image.close();
			image.flush();
			imageIni.show();
		} else if (gui.wasOKed()) {
			params = paramTemp;
			// updateAnalysis(paramTemp, frame);
			ImagePlus.removeImageListener(this);
			gui.removeAll();
			image.close();
			image.flush();
			imageIni.show();
		}
	}

	@Override
	public boolean dialogItemChanged(GenericDialog gui, AWTEvent e) {

		paramTemp.tagName = gui.getNextString();
		paramTemp.pixelSizeNm = gui.getNextNumber();
		paramTemp.frameLengthS = gui.getNextNumber();

		paramTemp.autoThreshold = gui.getNextBoolean();
		paramTemp.greyThreshold = gui.getNextNumber();
		paramTemp.smoothingContour = gui.getNextNumber();
		paramTemp.minCellSurface = gui.getNextNumber() / Math.pow(paramTemp.pixelSizeNm / 1000.0D, 2);
		paramTemp.maxCellSurface = gui.getNextNumber() / Math.pow(paramTemp.pixelSizeNm / 1000.0D, 2);

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
							updateAnalysis(paramtemp, frame);
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

	private void updateAnalysis(Params paramTemp, int frame) {
		UserVariables uv = paramTemp.getUV();
		uv = paramTemp.updateUV(uv);
		uv.setAnalyseProtrusions(true);

		ImagePlus bp = new ImagePlus("", new ByteProcessor(1, 1));
		new ImageWindow(bp).setVisible(false);

		AnalyseMovieMP previewAnalyser = new AnalyseMovieMP(new ImageStack[] { stack8bit, null }, false, false, uv,
				null, null);
		previewAnalyser.preparePreview(frame + 1, uv);
		previewAnalyser.doWork();
		ArrayList<CellData> cellData = previewAnalyser.getCellData();

		Overlay ov = null;
		ov = new Overlay();
		for (int i = 0; i < cellData.size(); i++) {
			double[] contourX = cellData.get(i).getCurveMap().getxCoords()[0];
			double[] contourY = cellData.get(i).getCurveMap().getyCoords()[0];
			PolygonRoi roi = new PolygonRoi(Utils.buildFloatPolygon(contourX, contourY), Roi.POLYGON);
			double area = Utils.area(roi.getFloatPolygon());

			if (area > paramTemp.minCellSurface && area < paramTemp.maxCellSurface) {
				roi.setStrokeColor(Color.BLUE);
			} else {
				roi.setStrokeColor(Color.BLUE.darker().darker());
			}
			roi.setStrokeWidth(1.5);
			roi.setPosition(frame + 1);
			ov.add(roi);
			image.setOverlay(ov);
		}
		image.setSliceWithoutUpdate(frame + 1);
		canvas.repaint();
		image.draw();
		image.setHideOverlay(false);
	}

	public void kill() {
		paramTemp = null;
	}

	@Override
	public void imageClosed(ImagePlus imp) {
		if (imp == image)
			imp.show();
	}

	@Override
	public void imageOpened(ImagePlus imp) {
		image.getWindow().requestFocus();
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
		if (imp == image && image.getCurrentSlice() - 1 != frame) {
			frame = image.getCurrentSlice() - 1;
			updateAnalysis(paramTemp, frame);
		}
	}

}
