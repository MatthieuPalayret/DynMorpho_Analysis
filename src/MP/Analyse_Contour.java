package MP;

import java.awt.Color;
import java.io.File;

import MP.objects.ResultsTableMt;
import MP.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

/**
 * 
 * @author matth
 *
 *         The Analyse_Contour plugin is a shorter version of the
 *         Analyse-Protrusion plugin: it only analyses the contour of the cells
 *         of the movie that it is fed with (but not their protrusions).
 *
 *         It takes as input a movie (if none is open when the plugin is
 *         started, the user is asked to open one). NB: the plugin can also
 *         manage the analysis of a single frame (i.e. not a movie).
 * 
 *         An interactive window allows to choose correct parameters to
 *         determine the contours of the cells. In blue are plotted detected and
 *         selected cell contours ; in dark blue, those which are discarded
 *         (because of a too big area). Then cell which contours are overlapping
 *         in two consecutive frames are linked in trajectories.
 * 
 *         The final movie is saved in "0-Contours.gif" as a GIF movie (only)
 *         (with a rate of 3 frames/s.). Final chosen parameters are saved in
 *         "Parameters.csv" ; and all detected contours (or cells) are saved in
 *         "1-Contour-analysis.csv": (1st column) the frame number ; (2nd-3rd)
 *         the (x, y) position of the center of mass of this cell in that frame
 *         ; (4th) the cell number (the same number than in the GIF movie) ;
 *         (5th) the area of this cell in that frame ; (6th) the perimeter of
 *         this cell in that frame.
 */
public class Analyse_Contour extends Analyse_Protrusion {

	public Analyse_Contour() {
		super();
	}

	@Override
	public void run(String arg0) {
		super.run("Contour");

		IJ.log("End of MP Contour analyses.");
	}

	@Override
	public void analyse(String arg0) {
		super.analyse("Contour");

		{
			ImagePlus image = new ImagePlus("Contours", stacks[0]);

			ResultsTableMt rt = new ResultsTableMt();
			int cellindex = rt.addNewColumn("Cell number");
			int areaindex = rt.addNewColumn("Area (µm²)");
			int perimeterindex = rt.addNewColumn("Perimeter (µm)");

			Overlay ov = null;
			ov = new Overlay();
			int frameLength = stacks[0].getSize();
			if (params.finalAddedSlice)
				frameLength--;
			for (int frame = 0; frame < frameLength; frame++) {
				for (int i = 0; i < cellData.size(); i++) {
					int startFrame = cellData.get(i).getStartFrame() - 1;
					int endFrame = cellData.get(i).getEndFrame() - 1;
					if (frame >= startFrame && frame <= endFrame) {
						double[] contourX = cellData.get(i).getCurveMap().getxCoords()[frame - startFrame];
						double[] contourY = cellData.get(i).getCurveMap().getyCoords()[frame - startFrame];
						FloatPolygon floatPolygon = Utils.buildFloatPolygon(contourX, contourY);
						PolygonRoi roi = new PolygonRoi(floatPolygon, Roi.POLYGON);
						double area = Utils.area(roi.getFloatPolygon());

						rt.incrementCounter();
						rt.addValue(ResultsTableMt.FRAME, frame);
						rt.addValue(cellindex, i);
						rt.addValue(ResultsTableMt.X, Utils.average(contourX));
						rt.addValue(ResultsTableMt.Y, Utils.average(contourY));
						rt.addValue(areaindex, area * Math.pow(params.pixelSizeNm / 1000.0D, 2));
						rt.addValue(perimeterindex, Utils.perimeter(floatPolygon) * params.pixelSizeNm / 1000.0D);

						if (area > params.minCellSurface && area < params.maxCellSurface) {
							roi.setStrokeColor(Color.BLUE);
						} else {
							roi.setStrokeColor(Color.BLUE.darker().darker());
						}
						roi.setStrokeWidth(1.5);
						roi.setPosition(frame + 1);
						ov.add(roi);
						image.setOverlay(ov);
					}
				}
			}
			image.flattenStack();
			image.hide();
			image.getCalibration().fps = 3;
			if (params.finalAddedSlice)
				image.getImageStack().deleteLastSlice();
			Utils.saveGif(image, this.popDir.getAbsolutePath() + File.separator + "0-Contours.gif", true);

			rt.saveAsPrecise(this.popDir.getAbsolutePath() + File.separator + "1-Contour-analysis.csv", 3);
		}

		params.save(this.popDir.getAbsolutePath() + File.separator + "Parameters.csv");
	}

}