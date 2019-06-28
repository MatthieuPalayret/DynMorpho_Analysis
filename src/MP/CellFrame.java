package MP;

import java.awt.Color;
import java.awt.Polygon;
import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import Cell.CellData;
import Curvature.CurveAnalyser;
import UserVariables.UserVariables;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.FloatPolygon;

public class CellFrame {

	LinkedList<FloatPolygon> pols = new LinkedList<FloatPolygon>();
	int[] thisPolIsUropod = new int[4];
	double[] contourX;
	double[] contourY;
	private double[] curvature;
	private int frame;
	private int[] trinary;
	private Cell cell;
	int reject = CellDataR.NOT_REJECTED;
	private ResultsTableMt rt;
	private ResultsTableMt linearityPrecised;
	private Params params;

	public CellFrame(CellData celldata, int frame, Cell cell, ResultsTableMt rt, ResultsTableMt linearityPrecised,
			Params params) {
		this.params = params;

		if (frame >= cell.startFrame && frame <= cell.endFrame) {
			if (celldata.getCurveMap() != null && celldata.getCurveMap().getxCoords() != null) {
				contourX = celldata.getCurveMap().getxCoords()[frame - celldata.getStartFrame() + 1];
				contourY = celldata.getCurveMap().getyCoords()[frame - celldata.getStartFrame() + 1];
				curvature = celldata.getCurveMap().getzVals()[frame - celldata.getStartFrame() + 1];
			} else {
				rejectCell404();
			}
		} else {
			rejectCell404();
		}
		this.frame = frame;
		this.cell = cell;
		this.rt = rt;
		this.linearityPrecised = linearityPrecised;
	}

	private void rejectCell404() {
		if (cell.rejectCell != CellDataR.NOT_REJECTED)
			reject = cell.rejectCell;
		else if (reject == CellDataR.NOT_REJECTED)
			reject = CellDataR.REJECT_404;
	}

	void setParams(Params paramsTemp) {
		params = paramsTemp;
	}

	public void buildProtrusions(ImagePlus imp, boolean save) {
		if (params.test)
			IJ.log("Building protrusions...");
		if (contourX == null) {
			rejectCell404();
		}
		if (contourX != null) {
			if (params.test)
				IJ.log("Building trinary perimeter...");
			buildTrinary(save);

			if (params.test && save && frame - cell.startFrame < 2 && cell.cellNumber < 2) {
				ByteProcessor bp = new ByteProcessor(imp.getWidth(), imp.getHeight());
				buildContour(bp, curvature);
				IJ.saveAs(new ImagePlus("", bp), "TIF",
						params.childDir + File.separator + "curvature-" + frame + ".tif");

				buildContour(bp,
						Utils.derivative(Utils.runningAverage(curvature, params.smoothingCoeffInPixels), true));
				IJ.saveAs(new ImagePlus("", bp), "TIF",
						params.childDir + File.separator + "derivative-" + frame + ".tif");
			}
			if (params.test)
				IJ.log("Building contour...");
			buildContour(imp, frame + 1);
			if (reject == CellDataR.NOT_REJECTED && cell.rejectCell == CellDataR.NOT_REJECTED
					&& !(params.detectUropod && frame == cell.startFrame)) {
				if (params.test)
					IJ.log("Properly building protrusions...");
				buildProtrusions();
				if (params.test)
					IJ.log("Detecting the uropod...");
				detectUropod();
				if (params.test)
					IJ.log("Drawing protrusions...");
				drawProtrusions(imp, frame + 1);

				if (save) {
					updateRt();
					updateLinearityPrecised();
				}
			}
		}

	}

	void updateRt() {

		// [frame cell polNumber polsX polsY areaPol]
		ListIterator<FloatPolygon> it = pols.listIterator();
		int iteration = 0;
		while (it.hasNext()) {
			FloatPolygon polygon = it.next();

			for (int i = 0; i < polygon.npoints; i++) {
				if (iteration != thisPolIsUropod[3]) {
					rt.incrementCounter();
					rt.addValue(ResultsTableMt.FRAME, frame);
					rt.addValue(Results.CELL, cell.cellNumber);
					rt.addValue(Results.PROTRUSION, iteration);
					rt.addValue(ResultsTableMt.X, polygon.xpoints[i]);
					rt.addValue(ResultsTableMt.Y, polygon.ypoints[i]);
					rt.addValue(Results.AREA, Utils.area(polygon) * Math.pow(params.pixelSizeNm / 1000.0D, 2));
				}
			}

			iteration++;
		}
	}

	void buildTrinary(boolean save) {
		double[] derivative = new double[curvature.length];

		if (params.method == 1) {
			double[] smoothedCurvature = Utils.runningAverage(curvature, params.smoothingCoeffInPixels);
			trinary = Utils.trinarise(smoothedCurvature, params.curvatureMinLevel);

		} else if (params.method == 3) {
			double[] smoothedCurvature = Utils.runningAverage(curvature, params.smoothingCoeffInPixels);

			// Calculate equivalent curvature for a circle of same surface
			double areaEq = Utils.area(Utils.buildFloatPolygon(contourX, contourY));
			double rEq = Math.sqrt(areaEq / Math.PI); // areaEq = PI * rEq²
			double angle = (2.0D * new UserVariables().getCurveRange() + 1.0D) / rEq * 360.0D / (2.0D * Math.PI);
			// rEq * angle = (2*n+1) -> angle = (2*n+1) / rEq -> angle° = (2*n+1) / rEq *
			// 360 / (2 PI)
			double theta = 180.0D - angle / 2.0D;

			int n = new UserVariables().getCurveRange();
			OvalRoi roi = new OvalRoi(rEq + 2.0D, rEq + 2.0D, rEq, rEq);
			Polygon polRoi = roi.getPolygon();
			short[][] pix = new short[polRoi.npoints][2];
			for (int i = 0; i < pix.length; i++) {
				pix[i][0] = (short) polRoi.xpoints[i];
				pix[i][1] = (short) polRoi.ypoints[i];
			}
			double[] temp = CurveAnalyser.calcCurvature(pix, n); // , null);
			double theta4 = temp[Math.min(n, temp.length - 1)];

			IJ.log("Average curvature : " + IJ.d2s(Utils.maxMin(curvature)[2]) + " / Equivalent curvature : "
					+ IJ.d2s(theta) + " / Theta4 : " + IJ.d2s(theta4));

			trinary = Utils.trinarise(Utils.minus(smoothedCurvature, params.reducingCoeff * theta4),
					params.curvatureMinLevel);

		} else if (params.method == 4) {
			double[] smoothedCurvature = Utils.runningAverage(curvature, params.smoothingCoeffInPixels);

			// Calculate equivalent curvature for a circle of same surface
			double areaEq = Utils.area(Utils.buildFloatPolygon(contourX, contourY));
			double rEq = Math.sqrt(areaEq / Math.PI); // areaEq = PI * rEq²
			int n = new UserVariables().getCurveRange();

			double x1 = rEq * Math.cos(n / rEq) - rEq;
			double y1 = rEq * Math.sin(n / rEq) - 0;
			double theta1 = IAClasses.Utils.arcTan(x1, y1);
			double theta2 = IAClasses.Utils.arcTan(x1, -y1);
			if (Math.abs(theta1 - theta2) >= 180.0) {
				if (theta2 > theta1) {
					theta2 -= 360.0;
				} else {
					theta1 -= 360.0;
				}
			}
			double theta3 = theta1 - theta2;

			IJ.log("Average curvature : " + IJ.d2s(Utils.maxMin(curvature)[2]) + " / Theta3 : " + IJ.d2s(theta3));

			trinary = Utils.trinarise(Utils.minus(smoothedCurvature, params.reducingCoeff * theta3),
					params.curvatureMinLevel);

		} else if (params.method == 2) {

			// (Lightly smoothed) Derivative of the curvature -> to select the sides of
			// protrusions
			// Join two consecutive sides if the curvatures of the in-between points are
			// positive.
			derivative = Utils.derivative(Utils.runningAverage(curvature, params.smoothingCoeffInPixels), true);
			int[] maxima = Utils.trinarise(derivative, params.curvatureMinLevel);
			boolean closed = true;
			int i = 0;
			int iMod = 0;
			int lastMax = 0;
			int lastMin = -1;
			int firstMax = -1;
			Arrays.fill(trinary, Utils.LOWER);

			while (i < curvature.length + firstMax) {
				iMod = i % curvature.length;
				if (closed) {
					if (maxima[iMod] == Utils.UPPER) {
						closed = false;
						lastMax = i;
						if (firstMax == -1) {
							firstMax = i;
						}
					} else if (lastMin > 0 && maxima[iMod] == Utils.LOWER
							&& maxima[(i + 1) % curvature.length] > Utils.LOWER) {
						for (int j = lastMin; j <= i; j++) {
							trinary[j % curvature.length] = Utils.UPPER;
						}
						lastMin = i;
					}
				} else {
					if (maxima[iMod] == Utils.LOWER && maxima[(i + 1) % curvature.length] > Utils.LOWER) {
						closed = true;
						lastMin = i;
						for (int j = lastMax; j <= i; j++) {
							trinary[j % curvature.length] = Utils.UPPER;
						}
					}
				}
				i++;
			}
			if (!closed) {
				for (int j = lastMax; j <= curvature.length + firstMax; j++) {
					trinary[j % curvature.length] = Utils.UPPER;
				}
			}
		}

		if (params.test && save && frame - cell.startFrame < 2 && cell.cellNumber < 2) {
			Plot plot = new Plot("Curvature " + frame, "pixels", "curvature");
			if (params.method == 1 || params.method == 3 || params.method == 4)
				plot.add("line", curvature);
			if (params.method == 2)
				plot.add("line", derivative);
			plot.draw();
			if (params.test) {
				plot.show();
			}
			IJ.saveAs(plot.getImagePlus(), "TIF", params.childDir + File.separator + "CurvaturePlot-Cell"
					+ cell.cellNumber + "-Frame" + frame + ".tif");
		}
	}

	public ImagePlus buildContour(ImagePlus imp, int frame) {
		Overlay ov = null;
		ImageCanvas ic = imp.getCanvas();
		if (ic != null)
			ov = ic.getShowAllList();
		if (ov == null)
			ov = imp.getOverlay();
		if (ov == null)
			ov = new Overlay();

		PolygonRoi roi = new PolygonRoi(Utils.buildFloatPolygon(contourX, contourY), Roi.POLYGON);

		roi.setStrokeColor(CellDataR.COLOR[cell.rejectCell]);
		if (reject != CellDataR.NOT_REJECTED)
			roi.setStrokeColor(CellDataR.COLOR[reject]);
		if (reject == CellDataR.REJECT_WHOLE_TRAJ)
			roi.setStrokeColor(CellDataR.COLOR[CellDataR.REJECT_WHOLE_TRAJ].brighter());

		roi.setName("c" + cell.cellNumber);
		if (reject != CellDataR.NOT_REJECTED)
			roi.setName(" ");
		roi.setStrokeWidth(1.5);
		roi.setPosition(frame);
		ov.add(roi);
		imp.draw();

		return imp;
	}

	public ByteProcessor buildContour(ByteProcessor bp, double[] contour) {
		double[] maxMin = Utils.maxMin(contour);
		double[] contour2 = null;
		if (contour != null) {
			contour2 = Utils.divide(Utils.minus(contour, maxMin[1]), (maxMin[0] - maxMin[1]) / Utils.UPPER);
		}

		for (int pixela = 0; pixela < trinary.length; pixela++) {
			if (contour == null) {
				bp.set((int) contourX[pixela], (int) contourY[pixela],
						reject != CellDataR.NOT_REJECTED ? Utils.LOWER / 2 : Utils.LOWER);
			} else {
				bp.set((int) contourX[pixela], (int) contourY[pixela], (int) contour2[pixela]);
			}
		}
		if (params.test) {
			bp.set(0, 0, 200);
			bp.set((int) contourX[trinary.length - 1], (int) contourY[trinary.length - 1], 200);
		}

		bp.setColor(Color.LIGHT_GRAY);
		bp.drawString("c" + cell.cellNumber, (int) Utils.average(contourX), (int) Utils.average(contourY));

		return bp;
	}

	public void buildProtrusions() {
		FloatPolygon polygon = new FloatPolygon();

		for (int pixela = 0; pixela < trinary.length; pixela++) {
			if (trinary[pixela] > Utils.LOWER) {
				polygon.addPoint(contourX[pixela], contourY[pixela]);
			} else {
				if (polygon.npoints > 0) {
					polygon.addPoint(polygon.xpoints[0], polygon.ypoints[0]);
					double areaProtrusion = Utils.area(polygon);
					if (areaProtrusion > params.minAreaDetection
							&& areaProtrusion / cellSurface() < params.maxProtrusionToCellAreaRatio) {
						pols.add(polygon);
					}
					polygon = new FloatPolygon();
				}
			}
		}
		if (polygon.npoints > 0) {
			if (trinary[0] > Utils.LOWER && pols.size() >= 1
					&& Math.abs(pols.getFirst().xpoints[0] - (float) contourX[0]) < 0.001
					&& Math.abs(pols.getFirst().ypoints[0] - (float) contourY[0]) < 0.001) {
				FloatPolygon firstPol = pols.getFirst();
				for (int i = 0; i < firstPol.npoints - 1; i++) {
					polygon.addPoint(firstPol.xpoints[i], firstPol.ypoints[i]);
				}
				pols.removeFirst();
			}
			polygon.addPoint(polygon.xpoints[0], polygon.ypoints[0]);

			double areaProtrusion = Utils.area(polygon);
			if (areaProtrusion > params.minAreaDetection
					&& areaProtrusion / cellSurface() < params.maxProtrusionToCellAreaRatio) {
				pols.add(polygon);
			}
		}
	}

	public final int NO_UROPOD = -1;

	public int detectUropod() {
		thisPolIsUropod = new int[4];

		if (!params.detectUropod) {
			thisPolIsUropod[3] = NO_UROPOD;
			return NO_UROPOD;
		}

		detectUropod(1);
		detectUropod(2);
		detectUropodBis();

		if (frame - cell.startFrame <= 1) {
			thisPolIsUropod[3] = thisPolIsUropod[0];
		} else if (thisPolIsUropod[0] == thisPolIsUropod[1] && thisPolIsUropod[0] == thisPolIsUropod[2]) {
			thisPolIsUropod[3] = thisPolIsUropod[0];
		} else if (thisPolIsUropod[0] == thisPolIsUropod[1] || thisPolIsUropod[0] == thisPolIsUropod[2]) {
			thisPolIsUropod[3] = thisPolIsUropod[0];
		} else if (thisPolIsUropod[1] == thisPolIsUropod[2]) {
			thisPolIsUropod[3] = thisPolIsUropod[1];
		} else {
			thisPolIsUropod[3] = thisPolIsUropod[0];
		}

		return thisPolIsUropod[3];
	}

	protected void updateCell(Cell newCell) {
		cell = newCell;
	}

	public int detectUropod(int frameBackwards) {
		if (frame - cell.startFrame < frameBackwards) {
			thisPolIsUropod[frameBackwards - 1] = NO_UROPOD;
			return NO_UROPOD;
		}

		double[] cell1 = getCentreOfMass();
		double[] cell2 = cell.cellFrame[frame - frameBackwards].getCentreOfMass();
		double[] speedVector = Utils.normalise(Utils.minus(cell1, cell2));
		if (params.test)
			IJ.log("Deplacement vector = [" + speedVector[0] + ", " + speedVector[1] + "]");

		ListIterator<FloatPolygon> it = pols.listIterator();
		LinkedList<Double> scalarVal = new LinkedList<Double>();
		while (it.hasNext()) {
			FloatPolygon polygon = it.next();
			if (polygon.npoints > 1 && Utils.signedArea(polygon) > params.minAreaDetection) {
				double[] poly = getCentreOfMass(polygon);
				double[] speedVector2 = Utils.normalise(Utils.minus(cell1, poly));
				double scalar = Utils.scalar(speedVector, speedVector2);
				if (params.test)
					IJ.log("" + (scalarVal.size() - 1) + " - Scalar = " + scalar);
				scalarVal.add(scalar);

				if (scalar > scalarVal.get(thisPolIsUropod[frameBackwards - 1])) {
					thisPolIsUropod[frameBackwards - 1] = scalarVal.size() - 1;
				}
			} else {
				scalarVal.add(0.0);
			}
		}
		return thisPolIsUropod[frameBackwards - 1];
	}

	public int detectUropodBis() {
		if (frame <= cell.startFrame) {
			thisPolIsUropod[2] = NO_UROPOD;
			return NO_UROPOD;
		}

		int lastUropod = cell.cellFrame[frame - 1].thisPolIsUropod[3];
		if (lastUropod < 0 || cell.cellFrame[frame - 1].pols.size() == 0) {
			thisPolIsUropod[2] = NO_UROPOD;
			return NO_UROPOD;
		}
		double[] lastUropodCenterOfMass = getCentreOfMass(cell.cellFrame[frame - 1].pols.get(lastUropod));
		double minDistance = Double.MAX_VALUE;
		int counter = 0;
		ListIterator<FloatPolygon> it = pols.listIterator();
		while (it.hasNext()) {
			FloatPolygon polygon = it.next();
			if (polygon.npoints > 1 && Utils.signedArea(polygon) > params.minAreaDetection) {
				double[] poly = getCentreOfMass(polygon);
				double distance = Utils.norm(Utils.minus(lastUropodCenterOfMass, poly));

				if (distance < minDistance) {
					minDistance = distance;
					thisPolIsUropod[2] = counter;
				}
			}
			counter++;
		}
		return thisPolIsUropod[2];
	}

	private double[] getCentreOfMass() {
		double[] centreOfMass = new double[2];
		for (int i = 0; i < contourX.length; i++) {
			centreOfMass[0] += contourX[i];
			centreOfMass[1] += contourY[i];
		}
		centreOfMass[0] /= (contourX.length);
		centreOfMass[1] /= (contourY.length);
		return centreOfMass;
	}

	private double[] getCentreOfMass(FloatPolygon polygon) {
		double[] centreOfMass = new double[2];
		double Cte = 0;
		for (int i = 0; i < polygon.npoints - 1; i++) {
			Cte = (polygon.xpoints[i] * polygon.ypoints[i + 1] - polygon.xpoints[i + 1] * polygon.ypoints[i]);
			centreOfMass[0] += (polygon.xpoints[i] + polygon.xpoints[i + 1]) * Cte;
			centreOfMass[1] += (polygon.ypoints[i] + polygon.ypoints[i + 1]) * Cte;
		}
		return Utils.divide(centreOfMass, 6.0 * Utils.signedArea(polygon));
	}

	public ImagePlus drawProtrusions(ImagePlus imp, int frame) {
		ListIterator<FloatPolygon> it = pols.listIterator();
		int iteration = 0;
		String letter = "";
		Color color = Color.CYAN;

		Overlay ov = null;
		ImageCanvas ic = imp.getCanvas();
		if (ic != null)
			ov = ic.getShowAllList();
		if (ov == null)
			ov = imp.getOverlay();
		if (ov == null)
			ov = new Overlay();

		while (it.hasNext()) {
			FloatPolygon polygon = it.next();
			if (Utils.area(polygon) > params.minAreaDetection) {
				if (thisPolIsUropod[3] == iteration) {
					color = Color.GREEN;
					letter = "u";
				} else {
					color = Color.CYAN;
					letter = "p" + iteration;
				}

				PolygonRoi roi = new PolygonRoi(polygon, Roi.POLYGON);
				roi.setStrokeColor(color);
				roi.setStrokeWidth(1.5);
				roi.setPosition(frame);
				roi.setName(letter);
				ov.add(roi);
			}
			iteration++;
		}
		imp.draw();
		return imp;
	}

	/**
	 * 
	 * @return double[2] : returns [0] the average distance of the protrusions to
	 *         the leading edge of the cell (defined as the opposite of the uropod)
	 *         and [1] the distance of the closest protrusion to the uropod. Both
	 *         distances are divided by the perimeter length and given as
	 *         percentages
	 */
	double[] linearDensityOfProtrusion() {
		/*
		 * Open up the perimeter of the cell from the uropod : openedPerim = {first
		 * pixel of the perimeter after the uropod, next pixel, ..., last pixel of the
		 * perimeter before the uropod} * {0 if there is no protrusion on this pixel of
		 * the perimeter, 1 otherwise}
		 */
		if (numberOfProtrusions() <= 0 || thisPolIsUropod[3] < 0) {
			return new double[] { -1, -1 };
		}

		FloatPolygon uropod = pols.get(thisPolIsUropod[3]);
		boolean[] openedPerim = new boolean[contourX.length - uropod.npoints];

		// Search first point fp of openedPerim after xUro
		float[] uro = new float[] { uropod.xpoints[uropod.npoints - 2], uropod.ypoints[uropod.npoints - 2] };
		int fp = 0;
		while (Math.abs((float) contourX[fp] - uro[0]) > 0.001 || Math.abs((float) contourY[fp] - uro[1]) > 0.001) {
			fp++;
		}
		fp = Math.floorMod(fp + 1, contourX.length);

		// Update binary with the post-selected pols
		boolean[] binary = new boolean[trinary.length];
		ListIterator<FloatPolygon> it = pols.listIterator();
		while (it.hasNext()) {
			FloatPolygon pol = it.next();
			int i = 0;
			while (Math.abs((float) contourX[i] - pol.xpoints[0]) > 0.001
					|| Math.abs((float) contourY[i] - pol.ypoints[0]) > 0.001) {
				i++;
			}
			for (int j = 0; j <= pol.npoints - 2; j++) {
				binary[Math.floorMod(i + j, binary.length)] = true;
			}
		}

		// Build openedPerim
		for (int i = 0; i < openedPerim.length; i++) {
			openedPerim[i] = binary[Math.floorMod(fp + i, contourX.length)];
		}
		if (params.test)
			IJ.log("OpenedPerim: [ " + openedPerim[openedPerim.length - 2] + " - " + openedPerim[openedPerim.length - 1]
					+ " // " + openedPerim[0] + " - " + openedPerim[1] + " ]");

		// Average distance of the protrusions to the leading edge of the cell (opposite
		// to the uropod)
		double avDist = 0;
		for (int i = 0; i < openedPerim.length; i++) {
			if (openedPerim[i]) {
				int i0 = i;
				int length = 1;
				i++;
				while (i < openedPerim.length && openedPerim[i]) {
					length++;
					i++;
				}
				// Distance between the centre of the protrusion (on the perimeter) and the
				// centre of the openedPerim
				avDist += Math.abs(i0 + length / 2.0D - openedPerim.length / 2.0D);
			}
		}
		avDist /= numberOfProtrusions();

		// Closest protrusion to the uropod
		double closestDist = 0;
		while (closestDist < openedPerim.length && !openedPerim[(int) closestDist]) {
			closestDist++;
		}
		double length = 1;
		while (closestDist + length < openedPerim.length && openedPerim[(int) (closestDist + length)]) {
			length++;
		}

		double closestDist2 = openedPerim.length - 1;
		while (closestDist2 >= 0 && !openedPerim[(int) closestDist2]) {
			closestDist2--;
		}
		double length2 = 1;
		while (closestDist2 - length2 >= 0 && openedPerim[(int) (closestDist2 - length2)]) {
			length2++;
		}

		double midUroLength = (uropod.npoints - 1) / 2.0D;
		closestDist = Math.min(closestDist + length / 2.0D + midUroLength,
				(openedPerim.length - 1) - closestDist2 + length2 / 2.0D + midUroLength);

		return Utils.divide(new double[] { avDist, closestDist }, (binary.length / 2.0D) / 100.0D);
	}

	double circularityCoeff() {
		FloatPolygon polygon = Utils.buildFloatPolygon(contourX, contourY);
		return 4.0D * Math.PI * Utils.area(polygon) / Math.pow(Utils.perimeter(polygon), 2) * 100.0D;
	}

	double numberOfProtrusions() {
		if (pols == null || pols.size() <= ((thisPolIsUropod[3] < 0) ? 0 : 1))
			return 0;
		return pols.size() - ((thisPolIsUropod[3] < 0) ? 0 : 1);
	}

	double avgSizeOfProtrusions() {
		if (numberOfProtrusions() == 0)
			return 0;
		FloatPolygon polygon = new FloatPolygon();
		double result = 0;
		int iteration = 0;
		Iterator<FloatPolygon> it = pols.iterator();
		while (it.hasNext()) {
			polygon = it.next();
			if (iteration != thisPolIsUropod[3]) {
				result += Utils.area(polygon);
			}
			iteration++;
		}
		return result / numberOfProtrusions();
	}

	double areaOfTheCell() {
		return Utils.area(Utils.buildFloatPolygon(contourX, contourY));
	}

	double areaOfTheUropod() {
		return (reject == CellDataR.NOT_REJECTED && params.detectUropod && thisPolIsUropod != null
				&& thisPolIsUropod[3] != NO_UROPOD && numberOfProtrusions() > 0)
						? Utils.area(pols.get(thisPolIsUropod[3]))
						: 0;
	}

	void updateLinearityPrecised() {
		double pixelSizeUm2 = Math.pow(params.pixelSizeNm / 1000.0D, 2);
		linearityPrecised.incrementCounter();
		linearityPrecised.addValue(Results.CELL, cell.cellNumber);
		linearityPrecised.addValue(ResultsTableMt.FRAME, frame);
		double[] linearDensity = linearDensityOfProtrusion();
		linearityPrecised.addValue("DistOfTheProtrusionToTheLeadingEdge (%)", linearDensity[0]);
		linearityPrecised.addValue("ClosestProtrusionToTheUropod (%)", linearDensity[1]);
		linearityPrecised.addValue("Circularity coefficient", circularityCoeff());
		linearityPrecised.addValue("NumberOfProtrusions", numberOfProtrusions());
		linearityPrecised.addValue("AvgSizeOfProtrusions (µm²)", avgSizeOfProtrusions() * pixelSizeUm2);
		linearityPrecised.addValue("Area of the uropod (µm²)", areaOfTheUropod() * pixelSizeUm2);
		linearityPrecised.addValue("Area of the cell (µm²)", areaOfTheCell() * pixelSizeUm2);
	}

	boolean contains(int x, int y) {
		Polygon pol = new Polygon();
		for (int i = 0; i < contourX.length; i++) {
			pol.addPoint((int) contourX[i], (int) contourY[i]);
		}
		return pol.contains(x, y);
	}

	double cellSurface() {
		FloatPolygon pol = new FloatPolygon();
		for (int i = 0; i < contourX.length; i++) {
			pol.addPoint((int) contourX[i], (int) contourY[i]);
		}
		return Utils.area(pol);
	}

	boolean hasNext() {
		if (nextFrame() < 0) {
			return false;
		} else {
			return true;
		}
	}

	CellFrame nextCellFrame() {
		int i = nextFrame();
		if (i < 0)
			return null;
		return cell.cellFrame[i];
	}

	int nextFrame() {
		int i = frame + 1;
		while (i <= cell.endFrame && cell.cellFrame[i].reject != CellDataR.NOT_REJECTED)
			i++;
		if (i > cell.endFrame)
			return -1;
		return i;
	}

	boolean hasPrevious() {
		if (previousFrame() < 0)
			return false;
		return true;
	}

	CellFrame previousCellFrame() {
		int i = previousFrame();
		if (i < 0)
			return null;
		return cell.cellFrame[i];
	}

	int previousFrame() {
		int i = frame - 1;
		while (i >= cell.startFrame && cell.cellFrame[i].reject != CellDataR.NOT_REJECTED)
			i--;
		if (i < cell.startFrame)
			return -1;
		return i;
	}

}
