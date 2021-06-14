package MP;

import java.awt.Color;
import java.awt.Font;
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
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.plugin.filter.Analyzer;

/**
 * This plugin plot the trajectories (of cells or dots within the cells) from
 * pre-processed data.
 * 
 * It accepts both (1) Imaris-analysed data in a ".xls" format with a sheet
 * called "Position" (with a 1st column called "Position X", a 2nd column
 * "Position Y", a 7th column "Time" and an 8th column "TrackID"), and (2) data
 * analysed with one of the MP plugins (e.g. Get_Trajectories, or
 * Analyse_Protrusion) (".csv" or ".txt" files).
 * 
 * Several datasets (from potentially both different types of analyses) may be
 * plotted in the same round.
 * 
 * If a folder (and not a file) is fed in the dialogue box, the plugin will
 * search for a file called "0-Trajectories.csv" in the folder (or it will
 * ignore this folder).
 * 
 * If no movie is open when the plugin is started, all the trajectories are
 * plotted in a new white window.
 * 
 * If the original ".tif" file - which was used to detect the detected
 * trajectories which data is fed to the plugin - is open before starting the
 * plugin, the trajectories are plotted over this open movie (the trajectories
 * from the first dataset are coloured blue, then those of the following dataset
 * red, then green, cyan, magenta, orange, and pink).
 */
public class Plot_Trajectories extends Combine_Excel_Results {

	private static final long serialVersionUID = -6687219155912852110L;
	private static final String SHEET_NAME = "Position";
	private static int PositionX = -1, PositionY = -1, TrackID = -1, Frame = -1;
	private Plot plot = new Plot("Trajectories", "x (" + IJ.micronSymbol + "m)", "y (" + IJ.micronSymbol + "m)");
	private int plotNumber = 0;
	private double[][] MaxMinXY = new double[2][2];
	private boolean fixedRange = true;
	private ImagePlus initialMovie = null;
	private int fileAnalysed = 0;

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

		initialMovie = IJ.getImage();
		if (initialMovie != null) {
			if (initialMovie.getOverlay() == null) {
				initialMovie.setOverlay(new Overlay());
				initialMovie.getOverlay().drawLabels(true);
				Analyzer.drawLabels(true);
				initialMovie.getOverlay().drawNames(true);
				initialMovie.getOverlay().drawBackgrounds(false);
				initialMovie.getOverlay().setLabelColor(Color.WHITE);
				initialMovie.getOverlay().setLabelFont(new Font("SansSerif", Font.BOLD, 18), false);
			} else
				initialMovie.getOverlay().clear();
		}

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
			} else if (pathFile.endsWith(".csv") || pathFile.endsWith(".txt")) {
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
			fileAnalysed++;
		}

		if (initialMovie != null) {
			initialMovie.setHideOverlay(false);
			initialMovie.show();
		} else {
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
		}
		IJ.log("" + plotNumber + " trajectories plotted and " + fileAnalysed + " files analysed.");
	}

	private void addRtFromAnalysedFileToPlot(ResultsTableMt rt) {
		if (rt == null || rt.getCounter() <= 1)
			return;
		int CELL_NUMBER = rt.getColumnIndex("Cell number");
		if (CELL_NUMBER < 0)
			CELL_NUMBER = rt.getColumnIndex("Group");
		rt = Utils.sortRt(rt, CELL_NUMBER);

		int cellNumber = (int) rt.getValueAsDouble(CELL_NUMBER, 0);
		int row = 0;
		while (row < rt.getCounter() && rt.getValueAsDouble(CELL_NUMBER, row) == cellNumber) {
			row++;
		}

		while (row < rt.getCounter()) {
			ResultsTableMt rtTemp = new ResultsTableMt();
			while (row < rt.getCounter() && rt.getValueAsDouble(CELL_NUMBER, row) == cellNumber) {
				rtTemp.incrementCounter();
				rtTemp.addValue(ResultsTableMt.X, rt.getValueAsDouble(ResultsTableMt.X, row));
				rtTemp.addValue(ResultsTableMt.Y, rt.getValueAsDouble(ResultsTableMt.Y, row));
				rtTemp.addValue(ResultsTableMt.FRAME, rt.getValueAsDouble(ResultsTableMt.FRAME, row));
				row++;
			}
			if (rtTemp.getCounter() > 1)
				addTrajToPlot(rtTemp.getColumnAsDoubles(ResultsTableMt.X), rtTemp.getColumnAsDoubles(ResultsTableMt.Y),
						rtTemp.getColumnAsDoubles(ResultsTableMt.FRAME));

			if (row < rt.getCounter())
				cellNumber = (int) rt.getValueAsDouble(CELL_NUMBER, row);
		}
	}

	private void addRtFromImarisFileToPlot(ResultsTableMt rt) {
		if (rt == null || rt.getCounter() <= 1)
			return;
		HashMap<Integer, ResultsTableMt> hashMap = new HashMap<Integer, ResultsTableMt>();
		PositionX = rt.getColumnIndex("Position X");
		if (PositionX == -1)
			PositionX = 0;
		PositionY = rt.getColumnIndex("Position Y");
		if (PositionY == -1)
			PositionY = 1;
		TrackID = rt.getColumnIndex("TrackID");
		if (TrackID == -1)
			TrackID = 7;
		Frame = rt.getColumnIndex("Time");
		if (Frame == -1)
			Frame = 6;

		double pixelSize = 0.107; // µm/pix TODO
		for (int row = 0; row < rt.getCounter(); row++) {
			ResultsTableMt temp = hashMap.get((int) rt.getValueAsDouble(TrackID, row));
			if (temp == null) {
				temp = new ResultsTableMt();
				hashMap.put((int) rt.getValueAsDouble(TrackID, row), temp);
			}
			temp.incrementCounter();
			temp.addValue(ResultsTableMt.X, rt.getValueAsDouble(PositionX, row) / pixelSize);
			temp.addValue(ResultsTableMt.Y, initialMovie.getHeight() - rt.getValueAsDouble(PositionY, row) / pixelSize); // TODO
			temp.addValue(ResultsTableMt.FRAME, rt.getValueAsDouble(Frame, row));
		}

		List<Integer> listTrackID = new ArrayList<Integer>(hashMap.keySet());
		java.util.Collections.sort(listTrackID);
		Iterator<Integer> itTrackID = listTrackID.iterator();
		while (itTrackID.hasNext()) {
			int traj = itTrackID.next();
			ResultsTableMt temp = hashMap.get(traj);
			addTrajToPlot(temp.getColumnAsDoubles(ResultsTableMt.X), temp.getColumnAsDoubles(ResultsTableMt.Y),
					temp.getColumnAsDoubles(ResultsTableMt.FRAME));
		}
	}

	private void addTrajToPlot(double[] xvalues, double[] yvalues, double[] framevalues) {
		if (isOriginalMovieOpened((int) Utils.getMax(framevalues))) {

			Color[] colors = new Color[] { Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.ORANGE,
					Color.PINK };

			Overlay ov = null;
			ImageCanvas ic = initialMovie.getCanvas();
			if (ic != null)
				ov = ic.getShowAllList();
			if (ov == null)
				ov = initialMovie.getOverlay();
			if (ov == null)
				ov = new Overlay();

			for (int row = 0; row < framevalues.length; row++) {
				PointRoi ptroi = new PointRoi(xvalues[row], yvalues[row]);
				ptroi.setPosition((int) framevalues[row]);
				ptroi.setStrokeColor(colors[fileAnalysed]);
				ptroi.setStrokeWidth(1.5);

				ov.add(ptroi);

				if (row > 0) {

					PolygonRoi roi = new PolygonRoi(
							Utils.buildFloatPolygon(Utils.trunc(xvalues, 0, row), Utils.trunc(yvalues, 0, row)),
							Roi.POLYLINE);

					roi.setStrokeColor(colors[fileAnalysed]);
					// roi.setName("c" + cell.cellNumber);
					roi.setStrokeWidth(1.5);
					roi.setPosition((int) framevalues[row]);
					ov.add(roi);
				}
			}

			initialMovie.draw();
			initialMovie.setHideOverlay(false);

		} else {

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
		}

		plotNumber++;
	}

	private boolean isOriginalMovieOpened(int maxFrame) {
		if (initialMovie == null || initialMovie.getImageStackSize() == 0) {
			initialMovie = null;
			return false;
		}
		if (initialMovie.getImageStackSize() >= maxFrame)
			return true;
		initialMovie = null;
		return false;
	}

}
