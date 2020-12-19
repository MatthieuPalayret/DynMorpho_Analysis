package MP.utils;

import java.awt.Color;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;

import MP.objects.ResultsTableMt;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.io.OpenDialog;
import ij.process.FloatPolygon;

public class Utils {

	public static double scalar(double[] vector1, double[] vector2) {
		if (vector1.length != vector2.length)
			return 0;
		double result = 0;
		for (int i = 0; i < vector1.length; i++) {
			result += vector1[i] * vector2[i];
		}
		return result;
	}

	public static boolean intersect(PolygonRoi pol1, PolygonRoi pol2) {
		// Look first at the intersection of the Rectangle in which they fit.
		if (!pol1.getBounds().intersects(pol2.getBounds()))
			return false;

		float[] x = pol1.getFloatPolygon().xpoints;
		float[] y = pol1.getFloatPolygon().ypoints;
		for (int pnt = 0; pnt < pol1.getFloatPolygon().npoints; pnt++) {
			if (pol2.getFloatPolygon().contains(x[pnt], y[pnt]))
				return true;
		}
		x = pol2.getFloatPolygon().xpoints;
		y = pol2.getFloatPolygon().ypoints;
		for (int pnt = 0; pnt < pol2.getFloatPolygon().npoints; pnt++) {
			if (pol1.getFloatPolygon().contains(x[pnt], y[pnt]))
				return true;
		}
		return false;
	}

	public static double distance(PolygonRoi pol1, PolygonRoi pol2) {
		return distance(getCentreOfMass(pol1.getFloatPolygon()), getCentreOfMass(pol2.getFloatPolygon()));
	}

	public static double[] normalise(double[] vector) {
		return divide(vector, norm(vector));
	}

	public static double[] divide(double[] vector, double value) {
		if (value == 0)
			return null;
		double[] result = new double[vector.length];
		for (int i = 0; i < vector.length; i++) {
			result[i] = vector[i] / value;
		}
		return result;
	}

	public static double norm(double[] vector) {
		double result = 0;
		if (vector == null)
			return 0;
		for (int i = 0; i < vector.length; i++) {
			result += Math.pow(vector[i], 2);
		}
		return Math.sqrt(result);
	}

	public static double distance(double[] point1, double[] point2) {
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
	public static double area(FloatPolygon polygon) {
		return Math.abs(signedArea(polygon));
	}

	// return signed area of polygon
	public static double signedArea(FloatPolygon polygon) {
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

	public static double average(double[] vector) {
		double result = 0;
		for (int i = 0; i < vector.length; i++) {
			result += vector[i];
		}
		return result / vector.length;
	}

	public static double[] runningAverage(double[] vector, int smoothingCoeffInPixels) {
		double[] result = new double[vector.length];
		for (int i = 0; i < vector.length; i++) {
			for (int j = i - smoothingCoeffInPixels; j <= i + smoothingCoeffInPixels; j++) {
				result[i] += vector[mod(j, vector.length)];
			}
			result[i] /= (2.0 * (smoothingCoeffInPixels) + 1.0);
		}
		return result;
	}

	public static double[] derivative(double[] vector, boolean circular) {
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

	public static double[] minus(double[] vector, double value) {
		double[] result = new double[vector.length];
		for (int i = 0; i < vector.length; i++) {
			result[i] = vector[i] - value;
		}
		return result;
	}

	/**
	 * 
	 * @param vector
	 * @return [max; min; average; index at max; index at min]
	 */
	public static double[] maxMin(double[] vector) {
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

	public static ResultsTableMt sortRt(ResultsTableMt rt, String column) {
		return sortRt(rt, rt.getColumnIndex(column));
	}

	public static ResultsTableMt sortRt(ResultsTableMt rt, int column) {
		if (!rt.columnExists(column))
			return null;

		IJ.showStatus("Sorting results by " + rt.getColumnHeading(column));

		double[] rows = getSortedIndexes(rt.getColumnAsDoubles(column));

		ResultsTableMt rt_Sorted = new ResultsTableMt();
		rt_Sorted.incrementCounter();
		rt_Sorted.addValue("OriginalFit", 0);
		rt_Sorted.deleteRow(0);
		int originalFit = rt_Sorted.getColumnIndex("OriginalFit");
		for (int row = 0; row < rows.length; row++) {
			addRow(rt, rt_Sorted, (int) rows[row]);
			rt_Sorted.addValue(originalFit, rows[row]);
		}

		return rt_Sorted;
	}

	public static double[] getSortedIndexes(double[] vector) {
		double[] Rows = new double[vector.length];
		for (int row = 0; row < Rows.length; row++)
			Rows[row] = row;

		// Sort Rt following the "frame" column
		double[][] RtDouble = new double[2][vector.length];
		RtDouble[0] = Rows;
		RtDouble[1] = vector;

		RtDouble = transpose(RtDouble);
		class Comp implements Comparator<double[]> {
			@Override
			public int compare(double[] o1, double[] o2) {
				return ((Double) o1[1]).compareTo(o2[1]);
			}
		}
		Comp comp = new Comp();
		Arrays.sort(RtDouble, comp);

		RtDouble = transpose(RtDouble);
		return RtDouble[0];
	}

	public static double[][] transpose(double[][] M) {
		return new Array2DRowRealMatrix(M).transpose().getData();
	}

	public static void addRow(ResultsTableMt from, ResultsTableMt to, int row) {
		to.incrementCounter();
		for (int column = 0; column <= from.getLastColumn(); column++) {
			if (from.columnExists(column) && from.getColumnHeading(column) != null)
				if (Double.isNaN(from.getValueAsDouble(column, row)))
					to.addValue(from.getColumnHeading(column), from.getStringValue(column, row));
				else
					to.addValue(from.getColumnHeading(column), from.getValueAsDouble(column, row));
		}
	}

	public static double getDistance(ResultsTableMt rt, int rowInRt1, int rowInRt2) {
		return getDistance(rt, rowInRt1, rt, rowInRt2);
	}

	public static double getDistance(ResultsTableMt rt1, int rowInRt1, ResultsTableMt rt2, int rowInRt2) {
		return Math.sqrt(Math
				.pow(rt1.getValueAsDouble(ResultsTableMt.X, rowInRt1)
						- rt2.getValueAsDouble(ResultsTableMt.X, rowInRt2), 2.0)
				+ Math.pow(rt1.getValueAsDouble(ResultsTableMt.Y, rowInRt1)
						- rt2.getValueAsDouble(ResultsTableMt.Y, rowInRt2), 2.0));
	}

	public static double[][] getColumnsFrom(ResultsTableMt rt, String[] headings) {
		if (rt == null)
			return null;

		double[][] values = new double[headings.length][];
		int[] indexes = getIndexOf(rt, headings);
		for (int column = 0; column < headings.length; column++) {
			if (indexes[column] != ResultsTableMt.COLUMN_NOT_FOUND)
				values[column] = rt.getColumnAsDoubles(indexes[column]);
		}

		return values;
	}

	public static double[][] getColumnsFrom(ResultsTableMt rt, int[] indexes) {
		if (rt == null)
			return null;

		double[][] values = new double[indexes.length][];
		for (int column = 0; column < indexes.length; column++)
			values[column] = rt.getColumnAsDoubles(indexes[column]);

		return values;
	}

	public static double[] getColumnFromRt(ResultsTableMt rt, String x0) {
		if (rt == null)
			return null;

		int index = rt.getColumnIndex(x0);
		if (index == ResultsTableMt.COLUMN_NOT_FOUND)
			return null;

		return rt.getColumnAsDoubles(index);
	}

	public static int[] getIndexOf(ResultsTableMt rt, String[] headings) {
		if (rt == null)
			return null;

		int[] indexes = new int[headings.length];
		for (int column = 0; column < headings.length; column++)
			indexes[column] = rt.getColumnIndex(headings[column]);

		return indexes;
	}

	public static int[] addHeadingsTo(ResultsTableMt rt, String[] headings) {
		if (rt == null)
			return null;

		int[] indexes = getIndexOf(rt, headings);
		boolean deleteRow0 = false;
		if (rt.getCounter() == 0) {
			rt.incrementCounter();
			deleteRow0 = true;
		}

		for (int column = 0; column < headings.length; column++) {
			rt.addValue(headings[column], 0);
			indexes[column] = rt.getColumnIndex(headings[column]);
		}

		if (deleteRow0)
			rt.deleteRow(0);

		return indexes;
	}

	public static ResultsTableMt concatenate(ResultsTableMt rt1, ResultsTableMt rt2) {
		if (rt2 == null) {
			if (rt1 == null) {
				return null;
			} else
				return rt1.clone();
		}

		if (rt1 == null)
			return rt2.clone();

		IJ.showStatus("Combining results...");
		String[] rt1Headings = rt1.getHeadings();
		int[] rt1ColumnIndex = new int[rt1Headings.length];
		for (int i = 0; i < rt1Headings.length; i++)
			rt1ColumnIndex[i] = rt1.getColumnIndex(rt1Headings[i]);
		double[][] rt2Values = getColumnsFrom(rt2, rt1Headings);

		for (int i = 0; i < rt2.getCounter(); i++) {
			if (i % 100 == 0)
				IJ.showProgress(i, rt2.getCounter());

			rt1.incrementCounter();
			for (int j = 0; j < rt1Headings.length; j++)
				if (rt2Values[j] != null)
					rt1.addValue(rt1ColumnIndex[j], rt2Values[j][i]);
		}

		IJ.showProgress(1);

		return rt1;
	}

	public static double[] concatenate(double[] vector1, double[] vector2) {
		if (vector1 == null)
			return vector2.clone();
		if (vector2 == null)
			return vector1.clone();

		double[] result = new double[vector1.length + vector2.length];
		for (int i = 0; i < vector1.length; i++)
			result[i] = vector1[i];
		for (int i = 0; i < vector2.length; i++)
			result[vector1.length + i] = vector2[i];
		return result;
	}

	public static double getIQR(double[] vector, boolean discardZeros) {
		double[] ordered = vector.clone();
		Arrays.sort(ordered);

		if (discardZeros) {
			int row = 0;
			while (row < ordered.length && ordered[row] == 0)
				row++;
			if (row == ordered.length)
				return vector.length;
			else
				return ordered[Math.min((int) (0.75 * (ordered.length - row)) + row + 1, ordered.length - 1)]
						- ordered[(int) (0.25 * (ordered.length - row)) + row];
		} else
			return ordered[Math.min((int) (0.75 * ordered.length) + 1, ordered.length - 1)]
					- ordered[(int) (0.25 * ordered.length)];
	}

	public static double getIQR(double[] vector) {
		return getIQR(vector, false);
	}

	public static double getIQR(ResultsTableMt rt, String x0) {
		if (rt.getColumnIndex(x0) != -1)
			return getIQR(rt.getColumnAsDoubles(rt.getColumnIndex(x0)));
		else
			return -1;
	}

	public static double getIQR(ResultsTableMt rt, int x0) {
		return getIQR(rt.getColumnAsDoubles(x0));
	}

	public static double[] removeZeros(double[] jd) {
		int i = 0;
		while (i < jd.length && jd[i] == 0)
			i++;

		int ii = jd.length - 1;
		while (ii >= i && jd[ii] == 0)
			ii--;
		double[] retour = new double[Math.max(0, ii - i + 1)];
		for (int j = 0; j < retour.length; j++)
			retour[j] = jd[i + j];
		return retour;
	}

	public static double[] removeFullyZeros(double[] vector) {
		if (vector == null || vector.length == 0)
			return null;

		ResultsTableMt rt = new ResultsTableMt();
		for (int i = 0; i < vector.length; i++)
			if (vector[i] != 0) {
				rt.incrementCounter();
				rt.addValue(ResultsTableMt.FRAME, vector[i]);
			}
		return rt.getColumnAsDoubles(ResultsTableMt.FRAME);
	}

	public static double[] plus(double[] vector, double cte) {
		double[] retour = new double[vector.length];
		for (int i = 0; i < vector.length; i++)
			retour[i] = vector[i] + cte;
		return retour;
	}

	private static double[] plus(double[] vector, double[] vector2, boolean minus) {
		if (vector == null) {
			if (vector2 == null)
				return null;
			return vector2.clone();
		}
		if (vector2 == null)
			return vector.clone();
		int length = Math.min(vector.length, vector2.length);
		double[] retour = new double[length];
		for (int i = 0; i < length; i++)
			retour[i] = vector[i] + (minus ? (-1) : 1) * vector2[i];
		return retour;
	}

	public static double[] plus(double[] vector, double[] vector2) {
		return plus(vector, vector2, false);
	}

	public static double[] minus(double[] vector, double[] vector2) {
		return plus(vector, vector2, true);
	}

	public static double getMax(ResultsTableMt rt, String x0) {
		if (rt.getColumnIndex(x0) != -1)
			return getMinMax(rt.getColumnAsDoubles(rt.getColumnIndex(x0)))[1];
		else
			return -1;
	}

	public static double getMax(ResultsTableMt rt, int x0) {
		if (x0 >= 0)
			return getMinMax(rt.getColumnAsDoubles(x0))[1];
		else
			return -1;
	}

	public static double getMin(ResultsTableMt rt, String x0) {
		if (rt.getColumnIndex(x0) != -1)
			return getMinMax(rt.getColumnAsDoubles(rt.getColumnIndex(x0)))[0];
		else
			return -1;
	}

	public static double getMin(ResultsTableMt rt, int x0) {
		if (x0 >= 0)
			return getMinMax(rt.getColumnAsDoubles(x0))[0];
		else
			return -1;
	}

	public static double[] getMinMax(ResultsTableMt rt, String x0) {
		if (rt.getColumnIndex(x0) != -1)
			return getMinMax(rt.getColumnAsDoubles(rt.getColumnIndex(x0)));
		else
			return null;
	}

	public static double[] getMinMax(ResultsTableMt rt, int x0) {
		return getMinMax(rt.getColumnAsDoubles(x0));
	}

	public static double getMean(ResultsTableMt rt, String x0) {
		if (rt.getColumnIndex(x0) != -1)
			return getMean(rt.getColumnAsDoubles(rt.getColumnIndex(x0)));
		else
			return -1;
	}

	public static double getMean(ResultsTableMt rt, int x0) {
		return getMean(rt.getColumnAsDoubles(x0));
	}

	public static double[] getMeanAndStdev(ResultsTableMt rt, String x0) {
		return getMeanAndStdev(rt, x0, false);
	}

	public static double[] getMeanAndStdev(ResultsTableMt rt, int x0) {
		return getMeanAndStdev(rt, x0, false);
	}

	public static double[] getMeanAndStdev(ResultsTableMt rt, int x0, boolean dontCountZeros) {
		if (rt.getColumnHeading(x0) != null)
			return getMeanAndStdev(rt.getColumnAsDoubles(x0), dontCountZeros);
		else
			return null;
	}

	public static double[] getMeanAndStdev(ResultsTableMt rt, String x0, boolean dontCountZeros) {
		if (rt.getColumnIndex(x0) != -1)
			return getMeanAndStdev(rt.getColumnAsDoubles(rt.getColumnIndex(x0)), dontCountZeros);
		else
			return null;
	}

	public static double getMean(double[] vector, boolean discardZeros) {
		double mean = 0;
		int tot = 0;
		for (int row = 0; row < vector.length; row++) {
			if (!discardZeros || vector[row] != 0) {
				mean += vector[row];
				tot++;
			}
		}
		return mean / tot;
	}

	public static double getMean(double[] vector) {
		return getMean(vector, false);
	}

	public static double[] getMeanAndStdev(double[] vector, boolean dontCountZeros) {
		double[] meanAndStdev = new double[2];
		meanAndStdev[0] = getMean(vector, dontCountZeros);
		int sum = 0;
		for (int row = 0; row < vector.length; row++) {
			if (!dontCountZeros || vector[row] != 0) {
				meanAndStdev[1] += Math.pow(vector[row] - meanAndStdev[0], 2);
				sum++;
			}
		}
		meanAndStdev[1] = Math.sqrt(meanAndStdev[1] / sum);
		return meanAndStdev;
	}

	public static double[] getMeanAndStdev(double[] vector) {
		return getMeanAndStdev(vector, false);
	}

	public static double getMedian(double[] vector, boolean dontCountZeros) {
		ResultsTableMt tempRt = new ResultsTableMt();
		for (int i = 0; i < vector.length; i++)
			if (!dontCountZeros || vector[i] != 0) {
				tempRt.incrementCounter();
				tempRt.addValue(ResultsTableMt.X, vector[i]);
			}
		double[] temp = tempRt.getColumnAsDoubles(ResultsTableMt.X);
		Arrays.sort(temp);
		return temp[(int) (temp.length / 2.0 + 0.5)];
	}

	public static double getMedian(double[] vector) {
		return getMedian(vector, false);
	}

	public static double getMedian(ResultsTableMt rt, String x0) {
		return getMedian(rt, x0, false);
	}

	public static double getMedian(ResultsTableMt rt, String x0, boolean dontCountZeros) {
		if (rt.getColumnIndex(x0) != -1)
			return getMedian(rt.getColumnAsDoubles(rt.getColumnIndex(x0)), dontCountZeros);
		else
			return -1;
	}

	public static double getMedian(ResultsTableMt rt, int x0) {
		if (rt.columnExists(x0))
			return getMedian(rt.getColumnAsDoubles(x0));
		else
			return -1;
	}

	public static double[] getMinMax(double[] vector) {
		double[] minMax = { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY };
		for (int row = 0; row < vector.length; row++) {
			if (vector[row] > minMax[1])
				minMax[1] = vector[row];
			if (vector[row] < minMax[0])
				minMax[0] = vector[row];
		}
		return minMax;
	}

	public static double getMax(double[] vector) {
		return getMinMax(vector)[1];
	}

	public static double getMin(double[] vector) {
		return getMinMax(vector)[0];
	}

	public static double[] pow(double[] vector, double cte) {
		double[] retour = new double[vector.length];
		for (int i = 0; i < vector.length; i++)
			retour[i] = Math.pow(vector[i], cte);
		return retour;
	}

	public static double[] pow(double[] vector, double[] vector2) {
		int length = Math.min(vector.length, vector2.length);
		double[] retour = new double[length];
		for (int i = 0; i < length; i++)
			retour[i] = Math.pow(vector[i], vector2[i]);
		return retour;
	}

	public static double[] sqrt(double[] vector) {
		return pow(vector, 0.5);
	}

	public static double sum(double[] vector) {
		double sum = 0;
		for (int i = 0; i < vector.length; i++)
			sum += vector[i];
		return sum;
	}

	public static double sumOfAbs(double[] vector) {
		double sum = 0;
		for (int i = 0; i < vector.length; i++)
			sum += Math.abs(vector[i]);
		return sum;
	}

	public static double sumOfSquares(double[] vector) {
		double sum = 0;
		for (int i = 0; i < vector.length; i++)
			sum += Math.pow(vector[i], 2);
		return sum;
	}

	public static double[] exp(double[] vector) {
		double[] retour = new double[vector.length];
		for (int i = 0; i < vector.length; i++)
			retour[i] = Math.exp(vector[i]);
		return retour;
	}

	public static double[] trunc(double[] vector, int begin, int end) {
		double[] retour = new double[end - begin + 1];
		for (int i = Math.max(0, begin); i <= Math.min(vector.length - 1, end); i++)
			retour[i - Math.max(0, begin)] = vector[i];
		return retour;
	}

	public static class Time {
		private long start, end;

		public Time() {
			start = System.currentTimeMillis();
		}

		public void start() {
			start = System.currentTimeMillis();
		}

		public long stop(boolean print) {
			end = System.currentTimeMillis();
			if (print)
				IJ.log("Time taken by the algorithm: " + (int) ((end - start) / 1000.0) + " s.");
			return end - start;
		}
	}

	public static double[] reverse(double[] vector) {
		double[] retour = new double[vector.length];
		for (int i = 0; i < vector.length; i++) {
			retour[i] = vector[vector.length - 1 - i];
		}
		return retour;
	}

	public static double[] times(double[] v1, double[] v2) {
		int length = Math.min(v1.length, v2.length);
		double[] retour = new double[length];
		for (int i = 0; i < length; i++)
			retour[i] = v1[i] * v2[i];
		return retour;
	}

	public static double[] times(double[] v1, double cte) {
		double[] retour = new double[v1.length];
		for (int i = 0; i < v1.length; i++)
			retour[i] = v1[i] * cte;
		return retour;
	}

	public static double[] divide(double[] up, double[] down) {
		int length = Math.min(up.length, down.length);
		double[] retour = new double[length];
		for (int i = 0; i < length; i++)
			retour[i] = up[i] / ((down[i] == 0) ? 1 : down[i]);
		return retour;
	}

	public static void saveVector(double[] vector, String path) {
		ResultsTableMt rt = new ResultsTableMt();
		for (int i = 0; i < vector.length; i++) {
			rt.incrementCounter();
			rt.addValue(0, vector[i]);
		}

		try {
			rt.saveAs(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	final static public int LOWER = 100;
	final static public int UPPER = 255;
	final static public int MIDDLE = 150;

	public static int[] trinarise(double[] vector, double level) {
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

	public static String[][] getFiles(String title, String initialRoot, String initialFile) {
		if (title == null || title == "")
			title = "Choose files";
		if (initialRoot == null || initialRoot == "")
			initialRoot = "E:\\Data";

		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Tiff files", "tif", "tiff");
		chooser.setFileFilter(filter);
		chooser.setDialogTitle(title);
		chooser.setCurrentDirectory(new File(initialRoot));
		chooser.setMultiSelectionEnabled(true);

		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String[][] files = new String[chooser.getSelectedFiles().length][2];
			for (int i = 0; i < files.length; i++) {
				files[i][0] = chooser.getSelectedFiles()[i].getParent();
				files[i][1] = chooser.getSelectedFiles()[i].getName();
			}
			return files;
		} else
			return null;
	}

	public static File getADir(String title, String initialRoot, String initialFile) {
		if (title == null || title == "")
			title = "Choose a directory";
		if (initialRoot == null || initialRoot == "")
			initialRoot = "E:\\Data";

		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(false);
		chooser.setDialogTitle(title);
		chooser.setCurrentDirectory(new File(initialRoot));
		chooser.setMultiSelectionEnabled(true);

		int returnVal = chooser.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			return chooser.getSelectedFile().getAbsoluteFile();
		} else
			return null;
	}

	public static LinkedList<File> getFilesInDir(String dirPath, String fileType) {
		File folder = new File(dirPath);
		File[] listOfFiles = folder.listFiles();
		LinkedList<File> list = new LinkedList<File>();

		// Count the number of sub-directories
		for (int i = 0; i < listOfFiles.length; i++) {
			if (!listOfFiles[i].isDirectory() && listOfFiles[i].getName().endsWith(fileType))
				list.add(listOfFiles[i]);
		}

		return list;
	}

	public static Color getGradientColor(Color ini, Color fin, int numberOfSteps, int step) {
		if (numberOfSteps == 0)
			return ini;
		if (step % numberOfSteps == 0)
			return ini;
		if (step % numberOfSteps == numberOfSteps - 1)
			return fin;
		else {
			float[] hsbini = Color.RGBtoHSB(ini.getRed(), ini.getGreen(), ini.getBlue(), null);
			float[] hsbfin = Color.RGBtoHSB(fin.getRed(), fin.getGreen(), fin.getBlue(), null);
			float h = ((numberOfSteps - step % numberOfSteps) * hsbini[0] + (step % numberOfSteps) * hsbfin[0])
					/ numberOfSteps;
			float s = ((numberOfSteps - step % numberOfSteps) * hsbini[1] + (step % numberOfSteps) * hsbfin[1])
					/ numberOfSteps;
			float b = ((numberOfSteps - step % numberOfSteps) * hsbini[2] + (step % numberOfSteps) * hsbfin[2])
					/ numberOfSteps;
			return Color.getHSBColor(h, s, b);
		}
	}

	public static Color getDarkGradient(Color color, int numberOfSteps, int step) {
		if (numberOfSteps == 0)
			return color;
		else {
			double factor = Math.pow(0.8, 5.0 / numberOfSteps * step);
			return new Color(Math.max((int) (color.getRed() * factor), 0),
					Math.max((int) (color.getGreen() * factor), 0), Math.max((int) (color.getBlue() * factor), 0),
					color.getAlpha());
		}
	}

}
