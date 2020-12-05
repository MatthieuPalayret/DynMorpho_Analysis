package MP;

import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFileChooser;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import MP.objects.ResultsTableMt;
import MP.params.Params;
import MP.utils.Utils;
import ij.IJ;
import ij.gui.Plot;

public class Plot_Trajectories extends Combine_Imaris_Results {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6687219155912852110L;
	private static final String SHEET_NAME = "Position";
	private static int PositionX = -1, PositionY = -1, TrackID = -1;
	private Plot plot = new Plot("Trajectories", "x (" + IJ.micronSymbol + "m)", "y (" + IJ.micronSymbol + "m)");
	private int plotNumber = 0;
	private double[][] MaxMinXY = new double[2][2];
	private boolean fixedRange = true;

	public Plot_Trajectories() {
		super("Imaris or analysed file", JFileChooser.FILES_AND_DIRECTORIES);
	}

	@Override
	void doCombine() {
		IJ.log("Plugin: MP v." + Params.version);
		IJ.log("HaPpY Easter Nora!");
		IJ.log("  (\\_/)");
		IJ.log("  (o.o)");
		IJ.log("  (___) 0");

		plot.setLineWidth(2);

		// Go through the files thanks to resultList
		for (int i = 0; i < resultList.getSize(); i++) {
			String pathFile = resultList.get(i);
			IJ.log("Reading file: " + pathFile);

			if (new File(pathFile).isDirectory()) {
				String pathTemp = pathFile + File.separator + "0-Trajectories.csv";
				if (new File(pathTemp).isFile()) {
					ResultsTableMt rtTemp = ResultsTableMt.open2(pathTemp);
					addRtFromAnalysedFileToPlot(rtTemp);
				} else {
					IJ.log(pathFile + " does not countain any 0-Trajectories.csv file to analyse. It is thus ignored.");
				}
			} else if (pathFile.endsWith(".csv")) {
				ResultsTableMt rtTemp = ResultsTableMt.open2(pathFile);
				addRtFromAnalysedFileToPlot(rtTemp);
			} else if (pathFile.endsWith(".xls")) {
				try {
					ExcelHolder holder = new ExcelHolder(pathFile);
					if (holder.excelFile.exists()) {
						holder.fileIn = new FileInputStream(holder.excelFile);
						holder.wb = WorkbookFactory.create(holder.fileIn);
						Iterator<Sheet> itSheet = holder.wb.sheetIterator();
						while (itSheet.hasNext()) {
							String sheetName = itSheet.next().getSheetName();
							if (sheetName.equalsIgnoreCase(SHEET_NAME)) {
								addToResults(holder, sheetName);
								addRtFromImarisFileToPlot(hm.get(sheetName));
							}
						}
					}

					// Close the Excel file
					closeExcelHolder(holder);
				} catch (EncryptedDocumentException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else
				IJ.log(pathFile + " is not a correct file. It is thus ignored.");
		}

		plot.setSize(512, 512);
		plot.setFormatFlags(Plot.X_TICKS + Plot.X_MINOR_TICKS + Plot.X_NUMBERS + Plot.Y_TICKS + Plot.Y_MINOR_TICKS
				+ Plot.Y_NUMBERS);
		if (fixedRange) {
			plot.setLimits(-200, 200, -200, 200);
		} else {
			double margin = 10;
			plot.setLimits(MaxMinXY[1][0] - margin, MaxMinXY[0][0] + margin, MaxMinXY[1][1] - margin,
					MaxMinXY[0][1] + margin);
		}
		plot.show();
		IJ.log("" + plotNumber + " trajectories plotted.");
	}

	private void addRtFromAnalysedFileToPlot(ResultsTableMt rt) {
		if (rt == null || rt.getCounter() <= 1)
			return;
		int CELL_NUMBER = rt.getColumnIndex("Cell number");
		int cellNumber = (int) rt.getValueAsDouble(CELL_NUMBER, 0);
		int row = 1;
		while (row < rt.getCounter()) {
			ResultsTableMt rtTemp = new ResultsTableMt();
			while (row < rt.getCounter() && rt.getValueAsDouble(CELL_NUMBER, row) == cellNumber) {
				rtTemp.incrementCounter();
				rtTemp.addValue(ResultsTableMt.X, rt.getValueAsDouble(ResultsTableMt.X, row));
				rtTemp.addValue(ResultsTableMt.Y, rt.getValueAsDouble(ResultsTableMt.Y, row));
				row++;
			}
			addTrajToPlot(rtTemp.getColumnAsDoubles(ResultsTableMt.X), rtTemp.getColumnAsDoubles(ResultsTableMt.Y));

			if (row < rt.getCounter())
				cellNumber = (int) rt.getValueAsDouble(CELL_NUMBER, row);
		}
	}

	private void addRtFromImarisFileToPlot(ResultsTableMt rt) {
		if (rt == null || rt.getCounter() <= 1)
			return;
		HashMap<Integer, ResultsTableMt> hashMap = new HashMap<Integer, ResultsTableMt>();
		PositionX = rt.getColumnIndex("Position X");
		PositionY = rt.getColumnIndex("Position Y");
		TrackID = rt.getColumnIndex("TrackID");
		for (int row = 0; row < rt.getCounter(); row++) {
			ResultsTableMt temp = hashMap.get((int) rt.getValueAsDouble(TrackID, row));
			if (temp == null) {
				temp = new ResultsTableMt();
				hashMap.put((int) rt.getValueAsDouble(TrackID, row), temp);
			}
			temp.incrementCounter();
			temp.addValue(ResultsTableMt.X, rt.getValueAsDouble(PositionX, row));
			temp.addValue(ResultsTableMt.Y, rt.getValueAsDouble(PositionY, row));
		}

		List<Integer> listTrackID = new ArrayList<Integer>(hashMap.keySet());
		java.util.Collections.sort(listTrackID);
		Iterator<Integer> itTrackID = listTrackID.iterator();
		while (itTrackID.hasNext()) {
			int traj = itTrackID.next();
			ResultsTableMt temp = hashMap.get(traj);
			addTrajToPlot(temp.getColumnAsDoubles(ResultsTableMt.X), temp.getColumnAsDoubles(ResultsTableMt.Y));
		}
	}

	private void addTrajToPlot(double[] xvalues, double[] yvalues) {
		plot.setColor(Utils.getGradientColor(Color.RED, Color.BLUE, 10, plotNumber));
		double[] x = Utils.minus(xvalues, xvalues[0]);
		double[] y = Utils.minus(yvalues, yvalues[0]);
		double[] temp = Utils.maxMin(x);

		if (!fixedRange) {
			// Update MaxMin for the plot dimensions
			MaxMinXY[0][0] = Math.max(temp[0], MaxMinXY[0][0]);
			MaxMinXY[1][0] = Math.min(temp[1], MaxMinXY[1][0]);
			temp = Utils.maxMin(y);
			MaxMinXY[0][1] = Math.max(temp[0], MaxMinXY[0][1]);
			MaxMinXY[1][1] = Math.min(temp[1], MaxMinXY[1][1]);
		}

		plot.addPoints(x, y, Plot.LINE);
		plot.draw();
		plotNumber++;
	}

}
