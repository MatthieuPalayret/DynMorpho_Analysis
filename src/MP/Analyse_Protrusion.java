package MP;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.ListIterator;

import MP.modifs.AnalyseMovieMP;
import MP.modifs.RegionGrowerMP;
import MP.objects.Results;
import MP.params.ParamPreview;
import MP.params.ParamVisualisation;
import MP.params.Params;
import MP.utils.Utils;
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
import net.calm.iaclasslibrary.Cell.CellData;
import net.calm.iaclasslibrary.DateAndTime.Time;
import net.calm.iaclasslibrary.IAClasses.Region;
import net.calm.iaclasslibrary.IO.PropertyWriter;
import net.calm.iaclasslibrary.Revision.Revision;
import net.calm.iaclasslibrary.TimeAndDate.TimeAndDate;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;
import net.calm.iaclasslibrary.UtilClasses.Utilities;

/**
 * 
 * @author matth
 *
 *         The Analyse_Protrusion plugin, based on the net.calm.adapt.Adapt by
 *         David Barry, analyses the protrusions of multiple cells in a ".tif"
 *         movie. It takes as input a movie (if none is open when the plugin is
 *         started, the user is asked to open one). NB: the plugin can also
 *         manage the analysis of a single frame (i.e. not a movie).
 * 
 *         First, an interactive window allows to choose correct parameters to
 *         determine the contours of the cells. In blue are plotted detected and
 *         selected cell contours ; in dark blue, those which are discarded
 *         (because of a too big area). Then cell which contours are overlapping
 *         in two consecutive frames are linked in trajectories.
 * 
 *         Then, a second interactive window offers to perform two different
 *         actions:
 * 
 *         (1) Choose a set of parameters to correctly detect the protrusions of
 *         the cells: (a) the minimal change of curvature in the contour of the
 *         cell for a protrusion to be detected (in °) ; (b) the maximal surface
 *         ratio between the protrusion and the corresponding cell (in % - the
 *         protrusion is supposed to be only a small portion of the cell) ; (c)
 *         a smoothing factor (to take into account only average changes of
 *         curvature in the contour of the cell, and not tiny accidental ones) ;
 *         (d) whether to try to detect the uropod of the cell (corresponding to
 *         the protrusion of the cell in the opposite direction from the
 *         direction the cell is moving towards, if at least one protrusion is
 *         detected).
 *
 *         (2a) Choose some parameters to refine the selection of the cell
 *         trajectories: (a) the minimal length of a trajectory (in s. - if
 *         trajectories are shorter, they are discarded) ; (b) the dramatic cell
 *         area in-/de-crease factor (in % - if a massive increase or decrease
 *         is observed in the surface of a cell during its trajectory, it is
 *         probably meeting or separating from another cell ; the plugin thus
 *         split the trajectory in two around that massive change).
 * 
 *         (2b) Interactively rejecting some cells or trajectories by selecting
 *         the correct button ("No rejection" ; "Reject cell in a single
 *         frame?"; "Reject a whole cell trajectory?") and directly clicking on
 *         the corresponding cell / trajectory on the movie. All initially
 *         detected trajectories are plotted, with colours depending on their
 *         status: Dark red: cell rejected in a single frame because of area
 *         in-/de-crease; Dark blue: cell rejected in a single frame; Dark
 *         green: trajectory length shorter than the defined parameter; Dark
 *         purple: whole cell rejected; [not used except for unknown reason]
 *         Dark yellow: cell rejected in a single frame because of 'technical'
 *         issue. NB: No uropod is detected in the 1st frame of a trajectory.
 * 
 *         Finally, the final movie is saved in "stack-ini.tif" and
 *         "stack-RoiSet.zip" (in order to be easy re-open thanks to the Open_MP
 *         plugin), and as a GIF movie in "stack.gif" (with a rate of 3
 *         frames/s.). Final chosen parameters are saved in "Parameters.csv" ; a
 *         list of the trajectories of all the cells in "0-Trajectories.csv"
 *         (with for each cell and each frame, the frame number, the cell
 *         number, the status of the trajectory -rejected or not-, and the (x,
 *         y) position of the center of mass of this cell in that frame) ; the
 *         contours of all protrusions of all cells in
 *         "1-Protrusion_contours.csv" ; the trajectory of all detected
 *         protrusions (in terms of their centres of mass) in
 *         "1-Protrusion_center_of_mass_positions.csv".
 * 
 *         Results from additional analyses are saved:
 * 
 *         - In "2-Results_per_frame.csv", for each cell and frame (cf. 1st and
 *         3rd columns), it outputs: (4th column) the average distance of the
 *         protrusions (but not the uropod, if it is detected) to the leading
 *         edge of the cell (defined as the opposite of the uropod) and (5th)
 *         the distance of the closest protrusion to the uropod (both distances
 *         are divided by the perimeter of the cell and given as %) ; (6th) a
 *         measure of the circularity of the cell (the ratio of the real area of
 *         the cell over the area of a circular cell which would have the same
 *         perimeter as the cell) ; (7th) the number of protrusions (but not the
 *         uropod, if it is detected) in the cell in the considered frame ;
 *         (8th) the average size of the protrusions (but not the uropod, if it
 *         is detected) in the cell in the considered frame ; (9th) the area of
 *         the uropod of this cell in that frame, if it is detected ; (10th) the
 *         area of that cell in that frame.
 * 
 *         - In "2-Results.csv", for each cell trajectory (cf. 1st column), it
 *         outputs: (3rd column) the linearity of the trajectory (the trajectory
 *         is perfectly linear for a result of 100%) ; (4th) the real distance
 *         the cell travelled ; (5th) the corresponding average speed of the
 *         cell (averaged over the number of frames) ; (6th) the global distance
 *         the cell travelled (distance between its last and first positions) ;
 *         (7th) the corresponding global average speed of the cell ; (8th-14th)
 *         the average (4th-10th) values of "2-Results_per_frame.csv" averaged
 *         over the whole cell trajectory ; (15th) the temporal duration of the
 *         trajectory.
 */
public class Analyse_Protrusion extends AnalyseMovieMP {

	private boolean selectiveOutput = false;
	protected Params params;
	protected ImagePlus imp;

	public Analyse_Protrusion() {
		super();
		IJ.log("Plugin: MP v." + Params.version);
		imp = IJ.getImage();

		boolean tempFinalAddedSlice = false;
		if (!imp.isStack()) {
			ImageProcessor newEmptySlice = imp.getProcessor().createProcessor(imp.getWidth(), imp.getHeight());
			imp.getStack().addSlice(newEmptySlice);
			tempFinalAddedSlice = true;
		}

		if (!(imp == null || imp.getOriginalFileInfo() == null)) {
			if (imp.getOriginalFileInfo().directory == "") {
				Utils.saveTiff(imp, IJ.getFilePath("Choose a place to save this image:"), false);
			} else {
				String initPath = imp.getOriginalFileInfo().directory + File.separator
						+ imp.getOriginalFileInfo().fileName;
				if (imp.getStack().isVirtual()) {
					new Opener().open(initPath);
					imp.close();
					imp = IJ.getImage();
				}
			}
		}

		params = new Params();
		params.finalAddedSlice = tempFinalAddedSlice;
		uv = params.getUV();
	}

	Analyse_Protrusion(String str) {
		super();
	}

	@Override
	public void run(String subClass) {
		IJ.resetMinAndMax(imp);

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

		runFromAnalyseMovieMP("");

		params.setChildDir(parDir);

		if (subClass == null || subClass.isEmpty()) {
			IJ.log("Building protrusions...");
			Results res = new Results(this.getCellData(), this.stacks[0].getSize(), params);
			res.buildProtrusions(imp, false);
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
				batchMode = true;
				directory = parDir;
				this.run(subClass);
				return;
			}

			new Open_MP(parDir.getAbsolutePath(), new ImagePlus()).run("");
			res.kill();
			IJ.log("End of MP Protrusion analyses.");
		}

	}

	public void runFromAnalyseMovieMP(String arg) {
		LocalDateTime startTime = LocalDateTime.now();

		TITLE = TITLE + "_v" + Revision.VERSION + "." + numFormat.format(Revision.revisionNumber);
		IJ.log(TITLE);
		IJ.log(TimeAndDate.getCurrentTimeAndDate());
		if (IJ.getInstance() != null && WindowManager.getIDList() == null) {
			IJ.error("No Images Open.");
			return;
		}
		try {
			if (!batchMode) {
				directory = Utilities.getFolder(directory, "Specify directory for output files...", true);
				// Specify directory for output
			}
		} catch (Exception e) {
			IJ.log(e.toString());
		}
		if (directory == null) {
			return;
		}
		IJ.log(String.format("Using %d parallel processes.\n", Runtime.getRuntime().availableProcessors()));
		analyse(arg);

		try {
			PropertyWriter.saveProperties(props, parDir.getAbsolutePath(), TITLE, true);
		} catch (IOException e) {
			IJ.log("Failed to create properties file.");
		}
		IJ.showStatus(TITLE + " done.");
		IJ.log(Time.getDurationAsString(startTime));
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
		if (parDir == null) {
			String addTag = "";
			if (params.tagName.length() > 0)
				addTag = "-" + params.tagName;
			String parDirName = GenUtils
					.openResultsDirectory(directory + delimiter + cytoImp.getOriginalFileInfo().fileName + addTag);
			parDir = new File(parDirName);
		}
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
		int threshold = RegionGrowerMP.getThreshold(cytoProc, uv.isAutoThreshold(), uv.getGreyThresh(),
				uv.getThreshMethod());
		RegionGrowerMP.initialiseROIs(null, -1, sliceIndex, cytoProc, roi, stacks[0].getWidth(), stacks[0].getHeight(),
				stacks[0].getSize(), cellData, uv, protMode, selectiveOutput);
		return RegionGrowerMP.findCellRegions(cytoProc, threshold, cellData);
	}

}
