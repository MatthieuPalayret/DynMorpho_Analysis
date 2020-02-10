package MP;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;

import Cell.CellData;
import IAClasses.Region;
import Segmentation.RegionGrower;
import UtilClasses.GenUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.io.Opener;
import ij.plugin.filter.GaussianBlur;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class Analyse_Protrusion_MP extends AnalyseMovieMP {

	private boolean selectiveOutput = false;
	protected Params params;
	protected ImagePlus imp;

	public Analyse_Protrusion_MP() {
		super();
		IJ.log("Plugin: MP v." + Params.version);
		imp = IJ.getImage();

		boolean tempFinalAddedSlice = false;
		if (!imp.isStack()) {
			ImageProcessor newEmptySlice = imp.getProcessor().createProcessor(imp.getWidth(), imp.getHeight());
			imp.getStack().addSlice(newEmptySlice);
			tempFinalAddedSlice = true;
		}

		if (imp.getOriginalFileInfo().directory == "") {
			Utils.saveTiff(imp, IJ.getFilePath("Choose a place to save this image:"), false);
		} else {
			String initPath = imp.getOriginalFileInfo().directory + File.separator + imp.getOriginalFileInfo().fileName;
			if (imp.getStack().isVirtual()) {
				new Opener().open(initPath);
				imp.close();
				imp = IJ.getImage();
			}
		}

		params = new Params();
		params.finalAddedSlice = tempFinalAddedSlice;
		uv = params.getUV();
	}

	@Override
	public void run(String subClass) {
		IJ.run("Enhance Contrast", "saturated=0.35");

		IJ.log("Choose parameters to determine the cell contours... Legend:");
		IJ.log("- Blue: selected cell contours.");
		IJ.log("- Dark blue: cell which area is > "
				+ IJ.d2s(params.maxCellSurface * Math.pow(params.pixelSizeNm / 1000.0, 2)) + " µm².");
		int finished = params.getNewParameters(IJ.getImage());
		if (finished == ParamPreview.CANCEL) {
			IJ.log("Plugin cancelled!");
			return;
		}
		uv = params.updateUV(uv);

		super.run("");

		params.setChildDir(parDir);

		if (subClass == null || subClass.isEmpty()) {
			IJ.log("Building protrusions...");
			Results res = new Results(this.getCellData(), params, this.stacks[0].getSize());
			res.buildProtrusions(false);
			IJ.log("Protrusions built.");

			IJ.log("Let's play with the parameters... Legend:");
			IJ.log(" - Dark red: cell rejected in a single frame because of area in-/de-crease >"
					+ IJ.d2s(params.dramaticAreaIncrease, 0) + " %.");
			IJ.log(" - Dark blue: cell rejected in a single frame.");
			IJ.log(" - Dark green: trajectory < " + IJ.d2s(params.minTrajLength * params.frameLengthS, 2) + "s.");
			IJ.log(" - Dark purple: whole cell rejected.");
			IJ.log("[- Dark yellow: cell rejected in a single frame because of 'technical' issue.]");
			IJ.log("NB: No uropod is detected in the 1st frame of a trajectory.");
			ParamVisualisation pm = new ParamVisualisation(params, res, stacks[0]);
			pm.run();
			if (pm.finished == ParamVisualisation.CANCEL) {
				imp.show();
				IJ.log("Back to step 1...");
				super.batchMode = true;
				super.directory = parDir;
				this.run(subClass);
				return;
			}

			new Open_MP(parDir.getAbsolutePath(), new ImagePlus()).run("");
			res.kill();
			IJ.log("End of MP Protrusion analyses.");
		}

	}

	protected static void addNewCell(ArrayList<CellData> cellData, int i, Region region, int size, int height,
			int width) {
		CellData celld = new CellData(i + 1);
		celld.setEndFrame(i + 1);
		celld.setCellRegions(new Region[size]);
		celld.getCellRegions()[i] = region;
		celld.setImageHeight(height);
		celld.setImageWidth(width);
		cellData.add(celld);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void analyse(String subClass) {
		int cytoSize;
		ImageStack cytoStack;
		ImagePlus cytoImp = new ImagePlus();
		if (IJ.getInstance() == null) {
			cytoStack = IJ.openImage(directory.getAbsolutePath()).getImageStack();
			cytoSize = cytoStack.getSize();
			stacks[0] = cytoStack;
		} else {
			int windIDs[] = WindowManager.getIDList();
			if (windIDs == null) {
				return;
			}
			cytoImp = WindowManager.getImage(windIDs[0]);
			roi = (PointRoi) cytoImp.getRoi(); // Points specified by the user indicate cells of interest

			cytoStack = cytoImp.getImageStack();
			cytoSize = cytoImp.getImageStackSize();

			stacks[0] = cytoStack;
		}

		if (stacks[0].getProcessor(1) instanceof ColorProcessor
				|| (stacks[1] != null && stacks[1].getProcessor(1) instanceof ColorProcessor)) {
			IJ.showMessage("Warning: greyscale images should be used for optimal results.");
		}

		/*
		 * Create new parent output directory - make sure directory name is unique so
		 * old results are not overwritten
		 */
		String addTag = "";
		if (params.tagName.length() > 0)
			addTag = "-" + params.tagName;
		String parDirName = GenUtils.openResultsDirectory(directory + delimiter + cytoImp.getShortTitle() + addTag);
		parDir = new File(parDirName);
		popDir = parDir;
		visDir = parDir;
		cellsDir = parDir;

		/*
		 * Convert cyto channel to 8-bit for faster segmentation
		 */
		cytoStack = GenUtils.convertStack(stacks[0], 8);
		stacks[0] = cytoStack;

		minLength = uv.getMinLength();
		String pdLabel = "Segmenting cells...";
		cellData = new ArrayList<>();
		roi = null;

		/*
		 * Cycle through all images in stack and detect cells in each. All detected
		 * regions are stored (in order) in stackRegions.
		 */

		IJ.log(pdLabel);
		for (int i = 0; i < cytoSize; i++) {
			IJ.showStatus(String.format("Segmenting %d%%", (int) Math.round(i * 100.0 / cytoSize)));
			ArrayList<Region> regions = generatePreview(i + 1, cytoStack);

			for (int cell = 0; cell < regions.size(); cell++) {
				double area = Utils
						.area(regions.get(cell).getPolygonRoi(regions.get(cell).getMask()).getFloatPolygon());
				if (area < params.maxCellSurface && area > params.minCellSurface) {
					if (i == 0) {
						addNewCell(cellData, i, regions.get(cell), cytoSize, cytoStack.getHeight(),
								cytoStack.getWidth());
					} else {
						// Search the closest cell in the previous frame

						// TODO This can be meliorated in the case of two cells encountering each
						// other...
						ListIterator<CellData> it = cellData.listIterator();
						PolygonRoi newCell = regions.get(cell).getPolygonRoi(regions.get(cell).getMask());
						double closestDistance = Double.MAX_VALUE;
						CellData toLink = null;
						while (it.hasNext()) {
							CellData oldCell = it.next();
							if (oldCell.getStartFrame() <= i && oldCell.getEndFrame() >= i) {
								PolygonRoi oldCellpol = oldCell.getCellRegions()[i - 1]
										.getPolygonRoi(oldCell.getCellRegions()[i - 1].getMask());
								if (Utils.intersect(newCell, oldCellpol)
										&& Utils.distance(newCell, oldCellpol) < closestDistance) {
									if (oldCell.getEndFrame() == i) {
										toLink = oldCell;
										closestDistance = Utils.distance(newCell, oldCellpol);
									} else if (oldCell.getEndFrame() == i + 1) {
										toLink = oldCell;
										closestDistance = Utils.distance(newCell, oldCellpol);
									}
								}
							}
						}
						if (toLink != null) {
							toLink.setEndFrame(i + 1);
							toLink.getCellRegions()[i] = regions.get(cell);
						} else {
							addNewCell(cellData, i, regions.get(cell), cytoSize, cytoStack.getHeight(),
									cytoStack.getWidth());
						}
					}
				}
			}
		}
		IJ.log(String.format("%d cells found.\n", cellData.size()));

		/*
		 * Analyse the dynamics of each cell, represented by a series of detected
		 * regions.
		 */
		try {
			getMorphologyData(cellData, false, -1, null, 0.0);
			for (int i = 0; i < cellData.size(); i++) {
				buildOutput(i, cellData.get(i).getLength(), true);
			}
		} catch (IOException e) {
			GenUtils.logError(e, "Could not save morphological data file.");
		}
		try {
			if (subClass == null || subClass.isEmpty() || !params.finalAddedSlice)
				generateCellTrajectories(cellData);
		} catch (Exception e) {
			GenUtils.logError(e, "Error: Failed to create cell trajectories file.");
		}

	}

	// Override the function in AnalyseMovieMP
	public ArrayList<Region> generatePreview(int sliceIndex, ImageStack stack) {
		ArrayList<CellData> cellData = new ArrayList<>();
		ImageProcessor cytoProc = stack.getProcessor(sliceIndex).duplicate();
		(new GaussianBlur()).blurGaussian(cytoProc, uv.getGaussRad(), uv.getGaussRad(), 0.01);
		int threshold = RegionGrower.getThreshold(cytoProc, uv.isAutoThreshold(), uv.getGreyThresh(),
				uv.getThreshMethod());
		RegionGrower.initialiseROIs(null, -1, sliceIndex, cytoProc, roi, stacks[0].getWidth(), stacks[0].getHeight(),
				stacks[0].getSize(), cellData, uv, protMode, selectiveOutput);
		return RegionGrower.findCellRegions(cytoProc, threshold, cellData);
	}

}
