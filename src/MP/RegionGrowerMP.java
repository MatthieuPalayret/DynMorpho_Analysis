package MP;

import java.util.ArrayList;

import Cell.CellData;
import IAClasses.Region;
import IAClasses.Utils;
import Segmentation.RegionGrower;
import UserVariables.UserVariables;
import ij.IJ;
import ij.Prefs;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.process.AutoThresholder;
import ij.process.Blitter;
import ij.process.ByteBlitter;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class RegionGrowerMP extends RegionGrower {

	public static int initialiseROIs(ByteProcessor masks, int threshold, int start, ImageProcessor input, PointRoi roi,
			int width, int height, int size, ArrayList<CellData> cellData, UserVariables uv, boolean protMode,
			boolean selectiveOutput) {
		ArrayList<short[]> initP = new ArrayList<>();
		int n;
		if (roi != null) {
			if (roi.getType() == Roi.POINT) {
				n = roi.getNCoordinates();
			} else {
				IJ.error("Point selection required.");
				return -1;
			}
		} else {
			if (threshold < 0) {
				threshold = getThreshold(input, uv.isAutoThreshold(), uv.getGreyThresh(), uv.getThreshMethod());
			}
			ByteProcessor binary = input.convertToByteProcessor(true);
			// TODO
			binary.threshold((int) (threshold * 255.0 / (input.getStatistics().max - input.getStatistics().min)));
			if (masks != null) {
				ByteBlitter bb = new ByteBlitter(binary);
				bb.copyBits(masks, 0, 0, Blitter.SUBTRACT);
			}
//	            IJ.saveAs(new ImagePlus("", binary), "PNG", String.format("D:\\debugging\\adapt_debug\\output\\%s_%d.png", "Residuals", (start - 2)));
			double minArea = getMinCellArea(uv); // protMode ? getMinFilArea(uv) : getMinCellArea(uv);
			getSeedPoints(binary, initP, minArea);
			n = initP.size();
		}
		if (cellData == null) {
			cellData = new ArrayList<>();
		}
		int s = cellData.size();
		int N = s + n;
		for (int i = s; i < N; i++) {
			CellData cell = new CellData(start, selectiveOutput ? start == 1 : true);
			cell.setImageWidth(width);
			cell.setImageHeight(height);
			short[] init;
			if (roi != null) {
				init = new short[] { (short) (roi.getXCoordinates()[i] + roi.getBounds().x),
						(short) (roi.getYCoordinates()[i] + roi.getBounds().y) };
			} else {
				init = initP.get(i - s);
			}
			if (!Utils.isEdgePixel(init[0], init[1], width, height, 1)) {
				ByteProcessor mask = new ByteProcessor(width, height);
				mask.setColor(Region.MASK_BACKGROUND);
				mask.fill();
				mask.setColor(Region.MASK_FOREGROUND);
				mask.drawPixel(init[0], init[1]);
				cell.setInitialRegion(new Region(mask, init));
				cell.setEndFrame(size);
			} else {
				cell.setInitialRegion(null);
				cell.setEndFrame(0);
			}
			cellData.add(cell);
		}
		return n;
	}

	/**
	 * Find cell regions in the given image
	 * 
	 * @param inputProc Input image
	 * @param rois      Regions of interest on which to initialise the regions
	 * @param t         Percentile to use in manual threshold calculation
	 * @param method    Method to use for automatic threshold calculation
	 * @return List of regions
	 */
	public static ArrayList<Region> findCellRegions(ImageProcessor inputProc, Roi[] rois, double t, String method) {
		PointRoi proi = null;
		for (Roi r : rois) {
			double[] centroid = r.getContourCentroid();
			if (proi == null) {
				proi = new PointRoi(centroid[0], centroid[1]);
			} else {
				proi.addPoint(centroid[0], centroid[1]);
			}
		}
		ArrayList<CellData> cells = new ArrayList<>();
		RegionGrower.initialiseROIs(null, -1, 0, inputProc, proi, inputProc.getWidth(), inputProc.getHeight(), 1, cells,
				null, false, false);
		return findCellRegions(inputProc, getThreshold(inputProc, true, t, method), cells);
	}

	static void getSeedPoints(ByteProcessor binary, ArrayList<short[]> pixels, double minArea) {
		binary.invert();
		if (binary.isInvertedLut()) {
			binary.invertLut();
		}
		ResultsTable rt = Analyzer.getResultsTable();
		rt.reset();
		Prefs.blackBackground = false;
		ParticleAnalyzer analyzer = new ParticleAnalyzer(ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES, Measurements.CENTROID,
				rt, minArea, Double.POSITIVE_INFINITY);
		analyzeDetections(null, binary, analyzer);
		int count = rt.getCounter();
		if (count > 0) {
			float[] x = rt.getColumn(rt.getColumnIndex("X"));
			float[] y = rt.getColumn(rt.getColumnIndex("Y"));
			for (int i = 0; i < count; i++) {
				pixels.add(new short[] { (short) Math.round(x[i]), (short) Math.round(y[i]) });
			}
		}
	}

	// To redefine how to measure the threshold
	public static int getThreshold(ImageProcessor image, boolean auto, double thresh, String method) {
		if (auto) {
			return (new AutoThresholder()).getThreshold(method, image.getStatistics().histogram);
		} else {
			// The threshold was calculated for each frame as the i-th top pixels
			// return (int) Math.round(Utils.getPercentileThresh(image, thresh));
			return (int) thresh;
		}
	}

}