package MP;

import ij.IJ;
import ij.ImagePlus;

public class Cell {

	CellFrame[] cellFrame;
	int cellNumber;
	CellDataR celldata;
	int startFrame, endFrame;
	private double[][] trajectory;
	@SuppressWarnings("unused")
	private ResultsTableMt linearity, linearityPrecised;
	private Params params;
	int rejectCell = CellDataR.NOT_REJECTED;

	public Cell(int frameNumber, int cellNumber, CellDataR celldata, ResultsTableMt linearity,
			ResultsTableMt linearityPrecised, Params params) {
		this.params = params;

		cellFrame = new CellFrame[frameNumber];
		this.cellNumber = cellNumber - 1;
		this.celldata = celldata;
		startFrame = celldata.getCellData().getStartFrame() - 1;
		endFrame = celldata.getCellData().getEndFrame() - 1;
		this.linearity = linearity;
		this.linearityPrecised = linearityPrecised;
		trajectory = new double[endFrame - startFrame + 1][2];
	}

	void setParams(Params paramsTemp) {
		params = paramsTemp;
	}

	public void buildProtrusions(ImagePlus imp, boolean save) {
		for (int frame = startFrame; frame <= endFrame; frame++) {
			cellFrame[frame].buildProtrusions(imp, save);
		}

		if (save && rejectCell == CellDataR.NOT_REJECTED)
			updateLinearity();
	}

	public void buildTrajectory() {
		if (trajectory.length != endFrame - startFrame + 1)
			trajectory = new double[endFrame - startFrame + 1][2];
		for (int frame = startFrame; frame <= endFrame; frame++) {
			if (cellFrame[frame].contourX != null) {
				trajectory[frame - startFrame][0] = Utils.average(cellFrame[frame].contourX);
				trajectory[frame - startFrame][1] = Utils.average(cellFrame[frame].contourY);
			}
		}
	}

	/**
	 * Percentage measuring the linearity of the cell trajectory: the trajectory is
	 * perfectly linear for a result of 100%
	 * 
	 * @return double : percentage corresponding to the ratio between the unrolled
	 *         length of the trajectory and the plain length (endPoint - startPoint)
	 *         of the trajectory. If the trajectory has a single point, the function
	 *         returns 0.
	 */
	double linearCoefficientOfTheTrajectory() {
		if (trajectory.length <= 1 || numberOfNonRejectedFrames() <= 1) {
			return 0;
		}
		return 100.0D * translatedDistanceOfTheTrajectory() / realTravelledDistanceOfTheTrajectory();
	}

	double realTravelledDistanceOfTheTrajectory() {
		if (trajectory.length <= 1 || numberOfNonRejectedFrames() <= 1)
			return 0;

		double realTravelledDistance = 0;
		int frame = getFirstNonRejectedFrame();
		while (cellFrame[frame].hasNext()) {
			int frameTemp = cellFrame[frame].nextFrame();
			if (params.test)
				IJ.log("frameTemp = " + frameTemp + " / frame = " + frame + " / startFrame = " + startFrame);
			realTravelledDistance += Utils.distance(trajectory[frameTemp - startFrame], trajectory[frame - startFrame]);
			frame = frameTemp;
		}
		return realTravelledDistance;
	}

	/**
	 * This gives the length (endPoint - startPoint) of the trajectory
	 * 
	 * @return double : the length of the trajectory
	 **/
	double translatedDistanceOfTheTrajectory() {
		if (numberOfNonRejectedFrames() <= 1)
			return 0;
		return Utils.distance(trajectory[getFirstNonRejectedFrame() - startFrame],
				trajectory[getLastNonRejectedFrame() - startFrame]);
	}

	/**
	 * 
	 * @return double[2] : it returns [0] the average speed (in pixel/frame) of the
	 *         trajectory steps; and [1] the global speed of the trajectory
	 *         (endPoint - startPoint)/frameNumber
	 */
	double[] averageSpeed() {
		if (numberOfNonRejectedFrames() <= 1)
			return new double[] { -1, -1 };
		double[] result = new double[2];
		result[0] = realTravelledDistanceOfTheTrajectory() / (numberOfNonRejectedFrames());
		result[1] = translatedDistanceOfTheTrajectory()
				/ ((double) getLastNonRejectedFrame() - getFirstNonRejectedFrame() + 1);
		return result;
	}

	double[] linearDensityOfProtrusion() {
		double[] result = new double[2];
		if (numberOfNonRejectedFrames() <= 0)
			return new double[] { -1, -1 };
		double iteration = 0;
		for (int i = startFrame; i <= endFrame; i++) {
			if (cellFrame[i].reject == CellDataR.NOT_REJECTED) {
				double[] temp = cellFrame[i].linearDensityOfProtrusion();
				if (temp[0] > 0) {
					result = Utils.plus(result, cellFrame[i].linearDensityOfProtrusion());
					iteration++;
				}
			}
		}
		if (iteration == 0)
			return new double[] { -1, -1 };
		return Utils.divide(result, iteration);
	}

	double circularityCoeff() {
		double result = 0;
		if (numberOfNonRejectedFrames() <= 0)
			return -1;
		for (int i = startFrame; i <= endFrame; i++) {
			if (cellFrame[i].reject == CellDataR.NOT_REJECTED)
				result += cellFrame[i].circularityCoeff();
		}
		return result / (numberOfNonRejectedFrames());
	}

	double numberOfProtrusions() {
		double result = 0;
		for (int i = startFrame; i <= endFrame; i++) {
			if (cellFrame[i].reject == CellDataR.NOT_REJECTED)
				result += cellFrame[i].numberOfProtrusions();
		}
		return result / (numberOfNonRejectedFrames());
	}

	double avgSizeOfProtrusions() {
		double result = 0;
		double iteration = 0;
		for (int i = startFrame; i <= endFrame; i++) {
			double add = cellFrame[i].avgSizeOfProtrusions();
			if (add != 0 && cellFrame[i].reject == CellDataR.NOT_REJECTED) {
				result += add;
				iteration++;
			}
		}
		if (iteration == 0)
			return 0;
		return result / iteration;
	}

	double avgAreaOfTheCell() {
		double result = 0;
		for (int i = startFrame; i <= endFrame; i++) {
			if (cellFrame[i].reject == CellDataR.NOT_REJECTED)
				result += cellFrame[i].areaOfTheCell();
		}
		return result / (numberOfNonRejectedFrames());
	}

	// This only returns a non-null value if another protrusion (than the uropod) is
	// detected. This is to prevent false uropod detection
	double avgAreaOfTheUropod() {
		if (!params.detectUropod)
			return 0;
		double result = 0;
		double iteration = 0;
		for (int i = startFrame; i <= endFrame; i++) {
			if (cellFrame[i].reject == CellDataR.NOT_REJECTED) {
				double temp = cellFrame[i].areaOfTheUropod();
				if (temp > 0) {
					result += temp;
					iteration++;
				}
			}
		}
		if (iteration == 0)
			return 0;
		return result / iteration;
	}

	int numberOfNonRejectedFrames() {
		int result = 0;
		for (int i = startFrame; i <= endFrame; i++) {
			if (cellFrame[i].reject == CellDataR.NOT_REJECTED) {
				result++;
			}
		}
		return result;
	}

	int getFirstNonRejectedFrame() {
		if (cellFrame[startFrame].reject == CellDataR.NOT_REJECTED)
			return startFrame;
		return cellFrame[startFrame].nextFrame();
	}

	int getLastNonRejectedFrame() {
		if (cellFrame[endFrame].reject == CellDataR.NOT_REJECTED)
			return endFrame;
		return cellFrame[endFrame].previousFrame();
	}

	void updateLinearity() {
		buildTrajectory();

		linearity.incrementCounter();
		linearity.addValue(Results.CELL, cellNumber);
		linearity.addValue("LinearCoeffTraj", linearCoefficientOfTheTrajectory());
		double[] avSp = averageSpeed();
		double pixelSizeUm = params.pixelSizeNm / 1000.0D;
		double pixelSizeUm2 = Math.pow(pixelSizeUm, 2);
		linearity.addValue("SumOfDisplacements (µm)", realTravelledDistanceOfTheTrajectory() * pixelSizeUm);
		linearity.addValue("AverageSpeed (µm/s)", avSp[0] * pixelSizeUm / params.frameLengthS);
		linearity.addValue("GlobalDisplacement (µm)", translatedDistanceOfTheTrajectory() * pixelSizeUm);
		linearity.addValue("GlobalAverageSpeed (µm/s)", avSp[1] * pixelSizeUm / params.frameLengthS);
		double[] linearDensity = linearDensityOfProtrusion();
		linearity.addValue("AvDistOfTheProtrusionToTheLeadingEdge (%)", linearDensity[0]);
		linearity.addValue("ClosestProtrusionToTheUropod (%)", linearDensity[1]);
		linearity.addValue("Circularity coefficient", circularityCoeff());
		linearity.addValue("AvgNumberOfProtrusions", numberOfProtrusions());
		linearity.addValue("AvgSizeOfProtrusions (µm²)", avgSizeOfProtrusions() * pixelSizeUm2);
		linearity.addValue("AvgAreaOfTheUropod (µm²)", avgAreaOfTheUropod() * pixelSizeUm2);
		linearity.addValue("AvgAreaOfTheCell (µm²)", avgAreaOfTheCell() * pixelSizeUm2);
	}
}
