package MP;

import java.awt.Polygon;
import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.io.OpenDialog;
import ij.process.FloatPolygon;

public class Utils {

	static double scalar(double[] vector1, double[] vector2) {
		if (vector1.length != vector2.length)
			return 0;
		double result = 0;
		for (int i = 0; i < vector1.length; i++) {
			result += vector1[i] * vector2[i];
		}
		return result;
	}

	static boolean intersect(PolygonRoi pol1, PolygonRoi pol2) {
		return pol1.getBounds().intersects(pol2.getBounds());
	}

	static double distance(PolygonRoi pol1, PolygonRoi pol2) {
		return distance(getCentreOfMass(pol1.getFloatPolygon()), getCentreOfMass(pol2.getFloatPolygon()));
	}

	static double[] normalise(double[] vector) {
		return divide(vector, norm(vector));
	}

	static double[] divide(double[] vector, double value) {
		if (value == 0)
			return null;
		double[] result = new double[vector.length];
		for (int i = 0; i < vector.length; i++) {
			result[i] = vector[i] / value;
		}
		return result;
	}

	static double norm(double[] vector) {
		double result = 0;
		if (vector == null)
			return 0;
		for (int i = 0; i < vector.length; i++) {
			result += Math.pow(vector[i], 2);
		}
		return Math.sqrt(result);
	}

	static double distance(double[] point1, double[] point2) {
		if (point1.length != point2.length) {
			IJ.log(String.format(
					"A distance cannot be calculated between two vectors of different dimensions (%d and %d)",
					point1.length, point2.length));
			return 0;
		}
		return norm(minus(point1, point2));
	}

	static double distance(float[] point1, float[] point2) {
		if (point1.length != point2.length) {
			IJ.log(String.format(
					"A distance cannot be calculated between two vectors of different dimensions (%d and %d)",
					point1.length, point2.length));
			return 0;
		}

		double[] pt1 = new double[point1.length], pt2 = new double[point1.length];
		for (int i = 0; i < pt1.length; i++) {
			pt1[i] = point1[i];
			pt2[i] = point2[i];
		}
		return distance(pt1, pt2);
	}

	// Cf. http://introcs.cs.princeton.edu/java/35purple/Polygon.java.html &&
	// http://mathworld.wolfram.com/PolygonArea.html
	// return area of polygon
	static double area(FloatPolygon polygon) {
		return Math.abs(signedArea(polygon));
	}

	// return signed area of polygon
	static double signedArea(FloatPolygon polygon) {
		if (polygon == null || polygon.npoints == 0)
			return 0;
		double sum = 0;
		for (int i = 0; i < polygon.npoints - 1; i++) {
			sum += (polygon.xpoints[i] * polygon.ypoints[i + 1]) - (polygon.ypoints[i] * polygon.xpoints[i + 1]);
		}
		sum += (polygon.xpoints[polygon.npoints - 1] * polygon.ypoints[0])
				- (polygon.ypoints[polygon.npoints - 1] * polygon.xpoints[0]);
		return 0.5 * sum;
	}

	static int[] floatToInt(float[] vector) {
		int[] sol = new int[vector.length];
		for (int i = 0; i < vector.length; i++) {
			sol[i] = Math.round(vector[i]);
		}
		return sol;
	}

	static Polygon floatPolygonToPolygon(FloatPolygon fp) {
		int[] x = floatToInt(fp.xpoints);
		int[] y = floatToInt(fp.ypoints);
		return new Polygon(x, y, fp.npoints);
	}

	static int mod(int x, int y) {
		int result = x % y;
		if (result < 0) {
			result += y;
		}
		return result;
	}

	static double average(double[] vector) {
		double result = 0;
		for (int i = 0; i < vector.length; i++) {
			result += vector[i];
		}
		return result / vector.length;
	}

	static double[] runningAverage(double[] vector, int smoothingCoeffInPixels) {
		double[] result = new double[vector.length];
		for (int i = 0; i < vector.length; i++) {
			for (int j = i - smoothingCoeffInPixels; j <= i + smoothingCoeffInPixels; j++) {
				result[i] += vector[mod(j, vector.length)];
			}
			result[i] /= (2.0 * (smoothingCoeffInPixels) + 1.0);
		}
		return result;
	}

	static double[] derivative(double[] vector, boolean circular) {
		double[] result = new double[vector.length];
		for (int i = 0; i < vector.length; i++) {
			if (i == 0 && vector.length > 1) {
				if (circular) {
					result[i] = (vector[i + 1] - vector[vector.length - 1]) / 2;
				} else {
					result[i] = (vector[i + 1] - vector[0]);
				}
			} else if (i == vector.length - 1) {
				if (circular) {
					result[i] = (vector[0] - vector[i - 1]) / 2;
				} else {
					result[i] = (vector[i] - vector[i - 1]);
				}
			} else {
				result[i] = (vector[i + 1] - vector[i - 1]) / 2;
			}
		}
		return result;
	}

	static double[] minus(double[] vector, double value) {
		double[] result = new double[vector.length];
		for (int i = 0; i < vector.length; i++) {
			result[i] = vector[i] - value;
		}
		return result;
	}

	static double[] minus(double[] vector1, double[] vector2) {
		if (vector1 == null || vector2 == null || vector1.length != vector2.length)
			return null;
		double[] result = new double[vector1.length];
		for (int i = 0; i < vector1.length; i++) {
			result[i] = vector1[i] - vector2[i];
		}
		return result;
	}

	static double[] plus(double[] vector1, double[] vector2) {
		return minus(vector1, divide(vector2, -1.0D));
	}

	/**
	 * 
	 * @param vector
	 * @return [max; min; average; index at max; index at min]
	 */
	static double[] maxMin(double[] vector) {
		double[] results = new double[5]; // [max; min; average; index at max; index at min]
		if (vector != null && vector.length > 0) {
			results[0] = vector[0];
			results[1] = vector[0];
			results[2] = vector[0];
			for (int i = 1; i < vector.length; i++) {
				results[2] += vector[i];
				if (vector[i] > results[0]) {
					results[0] = vector[i];
					results[3] = i;
				} else if (vector[i] < results[1]) {
					results[1] = vector[i];
					results[4] = i;
				}
			}
			results[2] /= vector.length;
		}
		return results;
	}

	final static public int LOWER = 100;
	final static public int UPPER = 255;
	final static public int MIDDLE = 150;

	static int[] trinarise(double[] vector, double level) {
		int[] trinary = new int[vector.length];
		for (int i = 0; i < vector.length; i++) {
			if (vector[i] < -level) {
				trinary[i] = LOWER;
			} else if (Math.abs(vector[i]) < level) {
				trinary[i] = MIDDLE;
			} else {
				trinary[i] = UPPER;
			}
		}
		return trinary;
	}

	public static FloatPolygon buildFloatPolygon(double[] contourX, double[] contourY) {
		FloatPolygon pol = new FloatPolygon();
		for (int i = 0; i < contourX.length; i++) {
			pol.addPoint(contourX[i], contourY[i]);
		}
		return pol;
	}

	// This works for a continuous evenly distributed contour.
	public static double[] getCenterOfMassEvenlyDistributedPolygon(FloatPolygon polygon) {
		double[] results = new double[2];
		for (int i = 0; i < polygon.npoints; i++) {
			results[0] += polygon.xpoints[i];
			results[1] += polygon.ypoints[i];
		}
		return divide(results, polygon.npoints);
	}

	// This work for any unevenly distributed polygon.
	public static double[] getCentreOfMass(FloatPolygon polygon) {
		double[] centreOfMass = new double[2];
		double Cte = 0;
		for (int i = 0; i < polygon.npoints - 1; i++) {
			Cte = (polygon.xpoints[i] * polygon.ypoints[i + 1] - polygon.xpoints[i + 1] * polygon.ypoints[i]);
			centreOfMass[0] += (polygon.xpoints[i] + polygon.xpoints[i + 1]) * Cte;
			centreOfMass[1] += (polygon.ypoints[i] + polygon.ypoints[i + 1]) * Cte;
		}
		return Utils.divide(centreOfMass, 6.0 * Utils.signedArea(polygon));
	}

	public static double perimeter(FloatPolygon polygon) {
		if (polygon == null || polygon.npoints == 0)
			return 0;
		double result = 0;
		for (int i = 1; i < polygon.npoints; i++) {
			result += Math.sqrt(Math.pow(polygon.xpoints[i] - polygon.xpoints[i - 1], 2)
					+ Math.pow(polygon.ypoints[i] - polygon.ypoints[i - 1], 2));
		}
		result += Math.sqrt(Math.pow(polygon.xpoints[0] - polygon.xpoints[polygon.npoints - 1], 2)
				+ Math.pow(polygon.ypoints[0] - polygon.ypoints[polygon.npoints - 1], 2));
		return result;
	}

	public static void saveTiff(ImagePlus imp, String fullName, boolean flush) {
		ij.io.FileSaver fs = new ij.io.FileSaver(imp);
		if (imp.getImageStackSize() > 1)
			fs.saveAsTiffStack(fullName);
		else
			fs.saveAsTiff(fullName);
		if (flush)
			imp.flush();
	}

	public static void saveGif(ImagePlus imp, String fullName, boolean flush) {
		ij.io.FileSaver fs = new ij.io.FileSaver(imp);
		fs.saveAsGif(fullName);
		if (flush)
			imp.flush();
	}

	public static String[] getAFile(String title, String initialRoot, String initialFile) {
		if (title == null || title == "")
			title = "Choose an analysed table:";
		if (initialRoot == null || initialRoot == "")
			initialRoot = "E:\\Data";
		OpenDialog dialog = new OpenDialog(title, initialRoot, initialFile);

		if (!new File(dialog.getDirectory() + File.separator + dialog.getFileName()).isFile())
			return null;

		String[] retour = { dialog.getDirectory(), dialog.getFileName() };
		return retour;
	}

}
