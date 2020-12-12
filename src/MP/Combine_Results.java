package MP;

import java.io.File;
import java.util.LinkedList;

import javax.swing.JFileChooser;

import MP.objects.ResultsTableMt;
import MP.params.Params;
import ij.IJ;

public class Combine_Results extends Combine_Excell_Results {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6687219155912852110L;
	private ResultsTableMt rtCSV = new ResultsTableMt();

	public Combine_Results() {
		super("Imaris .xls or MP .csv analysed file", JFileChooser.FILES_AND_DIRECTORIES);
	}

	@Override
	void doCombine() {
		IJ.log("Plugin: MP v." + Params.version);
		IJ.log("Hello from Cairo!");
		IJ.log("                  ,,__");
		IJ.log("        ..  ..   / o._)");
		IJ.log("       /--'/--\\  \\-'||");
		IJ.log("      /        \\_/ / |");
		IJ.log("    .'\\  \\_\\  _.'.'");
		IJ.log("      )\\ |  )\\ |");
		IJ.log("     // \\\\ // \\\\");
		IJ.log("    ||_  \\\\|_  \\\\");
		IJ.log("    '--' '--'' '--'");

		// Go through the files thanks to resultList
		int countCSV = 0;
		int countXLS = 0;
		for (int i = 0; i < resultList.getSize(); i++) {
			String pathFile = resultList.get(i);
			IJ.log("Reading file: " + pathFile);

			if (new File(pathFile).isDirectory()) {
				LinkedList<File> list = MP.utils.Utils.getFilesInDir(pathFile, ".csv");
				while (!list.isEmpty()) {
					addRtFromCSV(list.removeFirst().getAbsolutePath());
					countCSV++;
				}
				list = MP.utils.Utils.getFilesInDir(pathFile, ".xls");
				while (!list.isEmpty()) {
					addXLSFile(list.removeFirst().getAbsolutePath());
					countXLS++;
				}
			} else if (pathFile.endsWith(".csv")) {
				addRtFromCSV(pathFile);
				countCSV++;
			} else if (pathFile.endsWith(".xls")) {
				addXLSFile(pathFile);
				countXLS++;
			}

		}

		String resultPath = resultList.get(0);
		String pathFile = resultPath;
		if (!new File(resultPath).isDirectory())
			pathFile = resultPath.substring(0, resultPath.lastIndexOf(File.separatorChar));
		if (!hm.isEmpty())
			writeAndCloseTheCombinedXLSFile(pathFile + File.separator + "CombinedResults.xls");
		if (rtCSV.getCounter() > 1)
			rtCSV.saveAsPrecise(pathFile + File.separator + "CombinedResults.csv", 6);

		IJ.log("Finished! - " + countCSV + " .csv files and " + countXLS + " .xls files were (separately) combined.");
		IJ.log("The combined files are save in: " + pathFile);
	}

	private void addRtFromCSV(String pathFile) {
		rtCSV = ResultsTableMt.concatenate(rtCSV, ResultsTableMt.open2(pathFile));
	}

}
