package MP;
// Work with Adapt from CALM distribution plugins (https://github.com/djpbarry/CALM/wiki/Installation)

import java.io.File;

import UserVariables.UserVariables;
import ij.ImagePlus;

public class Params implements Cloneable {

	public static final double version = 1.472;
	public static final boolean officialVersion = true;

	// For getNewParameters1()
	public String tagName = "";
	public double pixelSizeNm = 320.5;
	public double frameLengthS = 20;
	public boolean autoThreshold = false;
	public int greyThreshold = 90;
	public int greyThreshold2 = 90;
	public double smoothingContour = 1; // Gaussian radius to smooth the image before detecting the contour
	public double minCellSurface = 505; // in pixel² (~25 µm²)
	public double maxCellSurface = 10000; // in pixel²

	// For ParamVisualisation
	public double curvatureMinLevel = -0; // trinary = [smoothedCurvatures - average(smoothedCurvatures)] > level
	public int minTrajLength = 6; // in frames
	public double dramaticAreaIncrease = 150; // ratio max of the areas of a cell in two adjacent frames
	public double minAreaDetection = 30; // in pixel²
	public double maxProtrusionToCellAreaRatio = 0.3;
	public int smoothingCoeffInPixels = 6;
	public boolean detectUropod = true;

	public boolean postRejectCellFrame = false;
	public boolean postRejectWholeCell = false;
	public boolean postExchangeGreenRedCell = false;

	// Parameters not shown in official version
	public int method = 1;
	public double reducingCoeff = 0.5;
	// For method = 3 or 4, select curvature for (theta - reducingCoeff * thetaEq) >
	// level

	boolean test = false;
	File childDir;

	// Internal hidden parameter
	boolean finalAddedSlice = false;
	public boolean twoColourAnalysis = false;
	public int channel1 = 1;
	public int channel2 = 2;

	public Params() {
		super();
	}

	public void setChildDir(File childDir) {
		this.childDir = childDir;
	}

	public double getPixelSizeUmSquared() {
		return Math.pow(pixelSizeNm / 1000.0, 2);
	}

	public UserVariables updateUV(UserVariables uv) {
		uv.setTimeRes(frameLengthS != 0 ? 60.0 / frameLengthS : 0);
		uv.setSpatialRes(pixelSizeNm / 1000.0);
		uv.setMorphSizeMin(minCellSurface * Math.pow(pixelSizeNm / 1000.0, 2.0));
		uv.setMinLength(minTrajLength);
		uv.setGaussRad(smoothingContour);
		uv.setAutoThreshold(autoThreshold);
		uv.setGreyThresh(greyThreshold);
		return uv;
	}

	public UserVariables getUV() {
		UserVariables uv = new UserVariables();

		uv.setBlebDetect(false);
		uv.setAutoThreshold(false);
		uv.setGreyThresh(90);
		uv.setAnalyseProtrusions(false);
		uv.setDisplayPlots(false);
		uv.setUseSigThresh(false);
		uv.setGenVis(false);
		uv.setMorphSizeMin(0);

		return uv;
	}

	public void updateParamsFromUV(UserVariables uv) {
		pixelSizeNm = 1000.0 * uv.getSpatialRes();
		frameLengthS = uv.getTimeRes() != 0 ? 60.0 / uv.getTimeRes() : 0;
		smoothingContour = (int) uv.getSpatFiltRad();
		minCellSurface = uv.getMorphSizeMin() / Math.pow(uv.getSpatialRes(), 2.0);
		minTrajLength = uv.getMinLength();
	}

	public int getNewParameters(ImagePlus img) {
		ParamPreview pp;
		if (twoColourAnalysis)
			pp = new ParamPreviewTwoColour(this, img);
		else
			pp = new ParamPreview(this, img);
		pp.run();
		tagName = pp.params.tagName;
		pixelSizeNm = pp.params.pixelSizeNm;
		frameLengthS = pp.params.frameLengthS;
		autoThreshold = pp.params.autoThreshold;
		greyThreshold = pp.params.greyThreshold;
		greyThreshold2 = pp.params.greyThreshold2;
		smoothingContour = pp.params.smoothingContour;
		minCellSurface = pp.params.minCellSurface;
		maxCellSurface = pp.params.maxCellSurface;
		channel1 = pp.params.channel1;
		channel2 = pp.params.channel2;
		return pp.finished;
	}

	public void save() {
		if (childDir != null) {
			save(childDir + File.separator + "Parameters.csv");
		}
	}

	public void save(String filePath) {

		ResultsTableMt params = new ResultsTableMt();
		final double pixelSizeMm2 = Math.pow(pixelSizeNm / 1000.0D, 2);
		params.incrementCounter();
		params.addValue("Version", version);
		params.addValue("TagName", tagName);
		params.addValue("PixelSize (nm/pix)", pixelSizeNm);
		params.addValue("Frame length (s/frame)", frameLengthS);
		params.addValue("Automatic intensity threshold?", autoThreshold ? 1 : 0);
		params.addValue("Contour intensity threshold", greyThreshold);
		if (twoColourAnalysis)
			params.addValue("Contour intensity threshold2", greyThreshold2);
		params.addValue("Smoothing coefficient for the contour", smoothingContour);
		params.addValue("Minimal area of a cell (µm²)", minCellSurface * pixelSizeMm2);
		params.addValue("Maximal area of a cell (µm²)", maxCellSurface * pixelSizeMm2);

		params.addValue("Minimal curvature for protrusions (°)", -curvatureMinLevel);
		params.addValue("Minimal length of a trajectory (s)", minTrajLength * frameLengthS);
		params.addValue("Dramatic cell area in-/de-crease (%)", dramaticAreaIncrease);
		params.addValue("Minimal area of a protrusion (µm²)", minAreaDetection * pixelSizeMm2);
		params.addValue("Maximal protrusion to cell surface ratio", maxProtrusionToCellAreaRatio);
		params.addValue("Smoothing window (pix)", smoothingCoeffInPixels);
		params.addValue("Detect uropod?", detectUropod ? 1 : 0);

		if (twoColourAnalysis) {
			params.addValue("Two colour analysis", 1);
			params.addValue("Red channel", channel1);
			params.addValue("Green channel", channel2);
		}

		if (!officialVersion) {
			params.addValue("Reducing coefficient (if applicable)", reducingCoeff);
			params.addValue("Method", method);
		}

		params.saveAsPrecise(filePath, 3);
	}

	@Override
	public Params clone() {
		Params output = new Params();
		output.tagName = "" + this.tagName;
		output.pixelSizeNm = this.pixelSizeNm;
		output.frameLengthS = this.frameLengthS;
		output.autoThreshold = this.autoThreshold;
		output.greyThreshold = this.greyThreshold;
		output.greyThreshold2 = this.greyThreshold2;
		output.smoothingContour = this.smoothingContour;
		output.minCellSurface = this.minCellSurface;
		output.maxCellSurface = this.maxCellSurface;

		output.curvatureMinLevel = this.curvatureMinLevel;
		output.minTrajLength = this.minTrajLength;
		output.dramaticAreaIncrease = this.dramaticAreaIncrease;
		output.minAreaDetection = this.minAreaDetection;
		output.maxProtrusionToCellAreaRatio = this.maxProtrusionToCellAreaRatio;
		output.smoothingCoeffInPixels = this.smoothingCoeffInPixels;
		output.detectUropod = this.detectUropod;

		output.postRejectCellFrame = this.postRejectCellFrame;
		output.postRejectWholeCell = this.postRejectWholeCell;
		output.postExchangeGreenRedCell = this.postExchangeGreenRedCell;

		output.method = this.method;
		output.reducingCoeff = this.reducingCoeff;

		output.test = this.test;
		if (childDir != null)
			output.childDir = new File(this.childDir.getAbsolutePath());

		output.finalAddedSlice = this.finalAddedSlice;
		output.twoColourAnalysis = this.twoColourAnalysis;
		output.channel1 = this.channel1;
		output.channel2 = this.channel2;
		return output;
	}

	public boolean compare(Params params2) {
		boolean identical = (this.tagName.equals(params2.tagName)) && (this.pixelSizeNm == params2.pixelSizeNm)
				&& (this.frameLengthS == params2.frameLengthS) && (this.autoThreshold == params2.autoThreshold)
				&& (this.greyThreshold == params2.greyThreshold) && (this.greyThreshold2 == params2.greyThreshold2)
				&& (this.smoothingContour == params2.smoothingContour)
				&& (this.minCellSurface == params2.minCellSurface) && (this.maxCellSurface == params2.maxCellSurface)

				&& (this.curvatureMinLevel == params2.curvatureMinLevel)
				&& (this.minTrajLength == params2.minTrajLength)
				&& (this.dramaticAreaIncrease == params2.dramaticAreaIncrease)
				&& (this.minAreaDetection == params2.minAreaDetection)
				&& (this.maxProtrusionToCellAreaRatio == params2.maxProtrusionToCellAreaRatio)
				&& (this.smoothingCoeffInPixels == params2.smoothingCoeffInPixels)
				&& (this.detectUropod == params2.detectUropod)

				&& (this.postRejectCellFrame == params2.postRejectCellFrame) && (this.test == params2.test)
				&& (this.postRejectWholeCell == params2.postRejectWholeCell)
				&& (this.postExchangeGreenRedCell == params2.postExchangeGreenRedCell)

				&& (this.method == params2.method) && (this.reducingCoeff == params2.reducingCoeff)
				&& (this.method == params2.method)
				&& ((this.childDir == null && params2.childDir == null)
						|| this.childDir.getAbsolutePath().equals(params2.childDir.getAbsolutePath()))
				&& (this.twoColourAnalysis == params2.twoColourAnalysis) && (this.channel1 == params2.channel1)
				&& (this.channel2 == params2.channel2);
		return identical;
	}
}
