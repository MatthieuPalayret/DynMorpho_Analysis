/*----------------------------------------------------------------------------- 
 * vLS Software
 * 
 * Copyright (C) 2014 Matthieu Palayret
 * Department of Chemistry
 * University of Cambridge, UK
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *---------------------------------------------------------------------------*/

package MP.objects;

import java.io.IOException;

import ij.IJ;
import ij.measure.ResultsTable;
import ij.util.Tools;

public class ResultsTableMt extends ResultsTable {

	public static final int FRAME = 0, X = 1, Y = 2, Z = 3, INTENSITY = 4, NUMBER = 5, GROUP = 6, JD = 7;

	private static final String[] defaultHeadings = { "Frame", "X", "Y", "Z", "Intensity", "Number", "Group", "JD" };

	private int freeColumn = defaultHeadings.length;

	public ResultsTableMt() {
		super();
	}

	@SuppressWarnings("deprecation")
	public int addNewColumn(String column) {
		int column_ = getColumnIndex(column);
		if (column_ == COLUMN_NOT_FOUND) {
			setHeading(freeColumn, column);
			freeColumn++;
			return freeColumn - 1;
		}
		return column_;
	}

	/** Adds a value to the end of the given column. Counter must be >0. */
	@Override
	public void addValue(int column, double value) {
		setValue(column, getCounter() - 1, value);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void addValue(String column, double value) {
		int column_ = getColumnIndex(column);
		if (column_ == COLUMN_NOT_FOUND) {
			setHeading(freeColumn, column);
			setValue(freeColumn, getCounter() - 1, value);
			freeColumn++;
			return;
		}

		setValue(column_, getCounter() - 1, value);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void addValue(String column, String value) {
		int column_ = getColumnIndex(column);
		if (column_ == COLUMN_NOT_FOUND) {
			setHeading(freeColumn, column);
			setValue(freeColumn, getCounter() - 1, value);
			freeColumn++;
			return;
		}

		setValue(column_, getCounter() - 1, value);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setValue(int column, int row, double value) {
		if (column < defaultHeadings.length && !columnExists(column))
			setHeading(column, defaultHeadings[column]);
		super.setValue(column, row, value);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setValue(String column, int row, String value) {
		int column_ = getColumnIndex(column);
		if (column_ == COLUMN_NOT_FOUND) {
			setHeading(freeColumn, column);
			super.setValue(freeColumn, row, value);
			freeColumn++;
			return;
		}

		super.setValue(column_, row, value);
	}

	@Override
	public void setValue(int column, int row, String value) {
		super.setValue(column, row, value);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void setValue(String column, int row, double value) {
		int column_ = getColumnIndex(column);
		if (column_ == COLUMN_NOT_FOUND) {
			setHeading(freeColumn, column);
			setValue(freeColumn, row, value);
			freeColumn++;
			return;
		}

		setValue(column_, row, value);
	}

	@Override
	public double getValueAsDouble(int column, int row) {
		if (columnExists(column))
			return super.getValueAsDouble(column, row);
		else
			return 0;
	}

	@Override
	public double getValue(String column, int row) {
		int column_ = getColumnIndex(column);
		return getValueAsDouble(column_, row);
	}

	/**
	 * Returns the string value of the given column and row, where row must be
	 * greater than or equal zero and less than the value returned by getCounter().
	 */
	@Override
	public String getStringValue(String column, int row) {
		int column_ = getColumnIndex(column);
		return getStringValue(column_, row);
	}

	@Override
	public String getStringValue(int column, int row) {
		if (columnExists(column))
			return super.getStringValue(column, row);
		else
			return "";
	}

	/**
	 * Returns the index of the first column with the given heading. heading. If not
	 * found, returns COLUMN_NOT_FOUND.
	 */
	@Override
	public int getColumnIndex(String column) {
		int column_ = 0;
		for (; column_ < defaultHeadings.length; column_++) {
			if (column.equals(defaultHeadings[column_])) {
				return column_;
			}
		}

		if (column_ == defaultHeadings.length) {
			for (; column_ < freeColumn; column_++) {
				if (columnExists(column_) && getColumnHeading(column_).equals(column))
					return column_;
			}
		}

		return COLUMN_NOT_FOUND;

	}

	public double[] getColumn(String columnString) {
		return this.getColumnAsDoubles(this.getColumnIndex(columnString));
	}

	/**
	 * Opens a tab or comma delimited text file and returns it as a ResultsTable,
	 * without requiring a try/catch statement. Displays a file open dialog if
	 * 'path' is empty or null.
	 */
	public static ResultsTableMt open2(String path) {
		ResultsTableMt rt = null;
		try {
			rt = open(path);
		} catch (IOException e) {
			IJ.error("Open Results", e.getMessage());
			rt = null;
		}
		return rt;
	}

	/**
	 * Opens a tab or comma delimited text file and returns it as a ResultsTable.
	 * Displays a file open dialog if 'path' is empty or null.
	 * 
	 * @see #open2(String)
	 */
	@SuppressWarnings("deprecation")
	public static ResultsTableMt open(String path) throws IOException {
		final String lineSeparator = "\n";
		final String cellSeparator = ",\t";
		String text = IJ.openAsString(path);
		if (text == null)
			return null;
		if (text.startsWith("Error:"))
			throw new IOException("Error opening " + path);
		String[] lines = Tools.split(text, lineSeparator);
		if (lines.length == 0)
			throw new IOException("Table is empty or invalid");
		String[] headings = Tools.split(lines[0], cellSeparator);
		if (headings.length == 1)
			throw new IOException("This is not a tab or comma delimited text file.");
		int numbersInHeadings = 0;
		for (int i = 0; i < headings.length; i++) {
			if (headings[i].equals("NaN") || !Double.isNaN(Tools.parseDouble(headings[i])))
				numbersInHeadings++;
		}
		boolean allNumericHeadings = numbersInHeadings == headings.length;
		if (allNumericHeadings) {
			for (int i = 0; i < headings.length; i++)
				headings[i] = "C" + (i + 1);
		}
		int firstColumn = headings[0].equals(" ") ? 1 : 0;
		for (int i = 0; i < headings.length; i++)
			headings[i] = headings[i].trim();
		int firstRow = allNumericHeadings ? 0 : 1;
		boolean labels = firstColumn == 1 && headings[1].equals("Label");
		int type = getTableType(path, lines, firstRow, cellSeparator);
		if (!labels && (type == 1 || type == 2))
			labels = true;
		int labelsIndex = (type == 2) ? 0 : 1;
		if (lines[0].startsWith("\t")) {
			String[] headings2 = new String[headings.length + 1];
			headings2[0] = " ";
			for (int i = 0; i < headings.length; i++)
				headings2[i + 1] = headings[i];
			headings = headings2;
			firstColumn = 1;
		}
		ResultsTableMt rt = new ResultsTableMt();
		for (int i = firstRow; i < lines.length; i++) {
			rt.incrementCounter();
			String[] items = Tools.split(lines[i], cellSeparator);
			for (int j = firstColumn; j < items.length; j++) {
				if (j == labelsIndex && labels)
					rt.addLabel(headings[labelsIndex], items[labelsIndex]);
				else if (j < headings.length) {
					double value = Tools.parseDouble(items[j]);
					if (Double.isNaN(value))
						rt.addValue(headings[j], items[j]);
					else
						rt.addValue(headings[j], value);
				}
			}
		}
		return rt;
	}

	private static int getTableType(String path, String[] lines, int firstRow, String cellSeparator) {
		if (lines.length < 2)
			return 0;
		String[] items = Tools.split(lines[1], cellSeparator);
		int nonNumericCount = 0;
		int nonNumericIndex = 0;
		for (int i = 0; i < items.length; i++) {
			if (!items[i].equals("NaN") && Double.isNaN(Tools.parseDouble(items[i]))) {
				nonNumericCount++;
				nonNumericIndex = i;
			}
		}
		if (nonNumericCount == 0)
			return 0; // assume this is all-numeric table
		if (nonNumericCount == 1 && nonNumericIndex == 1)
			return 1; // assume this is an ImageJ Results table with row numbers and row labels
		if (nonNumericCount == 1 && nonNumericIndex == 0)
			return 2; // assume this is an ImageJ Results table without row numbers and with row
						// labels
		return 3;
	}

	public void saveAsPrecise(String path, int decimalPlaces) {
		setPrecision(decimalPlaces);
		try {
			saveAs(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public ResultsTableMt clone() {
		ResultsTableMt retour = new ResultsTableMt();
		for (int row = 0; row < getCounter(); row++)
			addRow(this, retour, row);

		return retour;
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

	public static ResultsTableMt concatenate(ResultsTableMt from, ResultsTableMt to) {
		if (from == null) {
			if (to == null) {
				return null;
			} else
				return to.clone();
		}

		if (to == null)
			return from.clone();

		IJ.showStatus("Combining results...");
		for (int i = 0; i < from.getCounter(); i++) {
			if (i % 10 == 0)
				IJ.showProgress(i, from.getCounter());
			addRow(from, to, i);
		}

		IJ.showProgress(1);
		return to;
	}
}
