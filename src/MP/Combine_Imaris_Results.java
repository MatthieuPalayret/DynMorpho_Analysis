package MP;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import javax.swing.JFileChooser;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import ij.IJ;

public class Combine_Imaris_Results extends Combine_Results {
	public Combine_Imaris_Results() {
		super("Imaris file", JFileChooser.FILES_ONLY);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -3621134683008902745L;
	private static final String TRAJ_SPEED_MEAN = "Track Speed Mean";

	// A hashmap listing the files of resultList having an Excel sheet of a specific
	// name:
	HashMap<String, ResultsTableMt> hm = new HashMap<String, ResultsTableMt>();
	HashMap<String, LinkedList<String>> headers = new HashMap<String, LinkedList<String>>();

	@Override
	void doCombine() {
		IJ.log("Merry Xmas Nora!     o<]:-{)}");

		// Go through the files thanks to resultList
		for (int i = 0; i < resultList.getSize(); i++) {
			String pathFile = resultList.get(i);
			IJ.log("Combining results from file: " + pathFile);

			try {
				ExcelHolder holder = new ExcelHolder(pathFile);
				if (holder.excelFile.exists()) {
					holder.fileIn = new FileInputStream(holder.excelFile);
					holder.wb = WorkbookFactory.create(holder.fileIn);
					Iterator<Sheet> itSheet = holder.wb.sheetIterator();
					while (itSheet.hasNext()) {
						String sheetName = itSheet.next().getSheetName();
						addToResults(holder, sheetName);
					}
				}

				// Close the Excel file
				closeExcelHolder(holder);
			} catch (EncryptedDocumentException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		ExcelHolder holder = null;
		boolean firstRound = true;

		List<String> listSheet = new ArrayList<String>(hm.keySet());
		java.util.Collections.sort(listSheet);
		Iterator<String> itSheet = listSheet.iterator();
		while (itSheet.hasNext()) {
			String sheetName = itSheet.next();

			// At first, initialise the Excel file (delete any previous one, and create a
			// new fresh one)
			if (firstRound) {
				firstRound = false;
				String resultPath = resultList.get(0);
				holder = newExcelHolder(resultPath.substring(0, resultPath.lastIndexOf(File.separatorChar))
						+ File.separator + "CombinedResults.xls", sheetName);
			}
			// Create a new sheet
			Sheet sheet = openExcelSheet(holder, sheetName);
			// IJ.log("Writing sheet: " + sheetName);

			// Add results from headers and hm
			addResultsToExcel(holder, sheet, headers.get(sheetName), hm.get(sheetName));
		}

		// Write and close the Excel file
		writeAndCloseExcelHolder(holder);
	}

	private ExcelHolder newExcelHolder(String filePath, String sheetName) {
		// Open the Excel file and the proper sheet
		ExcelHolder holder = null;
		try {
			holder = new ExcelHolder(filePath);
			holder.newWorkbook(sheetName);
		} catch (IOException e) {
			IJ.handleException(e);
		} catch (InvalidFormatException e) {
			IJ.handleException(e);
		}

		return holder;
	}

	private Sheet openExcelSheet(ExcelHolder holder, String sheetName) {
		Workbook wb = holder.wb;
		// Get sheet in workbook. See method for details.
		return openSheetInWorkbook(wb, sheetName);
	}

	private void closeExcelHolder(ExcelHolder holder) {
		// Close the Excel file
		try {
			holder.closeWorkbook();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeAndCloseExcelHolder(ExcelHolder holder) {
		// Close the Excel file
		try {
			holder.writeOutAndCloseWorkbook();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getFileNameFromPath(String filePath) {
		if (filePath == null)
			return null;
		return filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1);
	}

	public void addToResults(ExcelHolder holder, String sheetName) {
		Sheet sheet = holder.wb.getSheet(sheetName);

		// Read the Excel sheet and fill rt (and headers if it is empty)
		Iterator<Row> it = sheet.rowIterator();
		if (it.hasNext())
			it.next();
		else
			return;

		if (it.hasNext()) {
			Row headerRow = it.next();
			if (!headers.containsKey(sheetName)) {
				LinkedList<String> sheetHeaders = new LinkedList<String>();
				hm.put(sheetName, new ResultsTableMt());

				Iterator<org.apache.poi.ss.usermodel.Cell> it2 = headerRow.cellIterator();
				while (it2.hasNext()) {
					org.apache.poi.ss.usermodel.Cell cell = it2.next();
					if (cell.getCellType() != CellType.BLANK && cell.getCellType() == CellType.STRING)
						sheetHeaders.add(cell.getStringCellValue());
				}
				sheetHeaders.add("Origin");
				if (sheetName.equalsIgnoreCase(TRAJ_SPEED_MEAN))
					sheetHeaders.add("AColumnx60");
				headers.put(sheetName, sheetHeaders);
			}
		} else
			return;

		while (it.hasNext()) {
			Row row = it.next();
			ResultsTableMt rt = hm.get(sheetName);
			rt.incrementCounter();
			Iterator<org.apache.poi.ss.usermodel.Cell> it2 = row.cellIterator();
			int column = 0;
			while (it2.hasNext() && column < headers.get(sheetName).size() - 1
					- ((sheetName.equalsIgnoreCase(TRAJ_SPEED_MEAN)) ? 1 : 0)) {
				org.apache.poi.ss.usermodel.Cell cell = it2.next();
				CellType cellType = cell.getCellType();
				if (cellType == CellType.STRING) {
					rt.setValue(column, rt.getCounter() - 1, cell.getStringCellValue());
				} else if (cellType == CellType.NUMERIC) {
					rt.setValue(column, rt.getCounter() - 1, cell.getNumericCellValue());
				}
				column++;
			}
			rt.setValue(column, rt.getCounter() - 1, getFileNameFromPath(holder.excelFile.getAbsolutePath()));
			column++;
			if (sheetName.equalsIgnoreCase(TRAJ_SPEED_MEAN))
				rt.setValue(column, rt.getCounter() - 1, rt.getValueAsDouble(0, rt.getCounter() - 1) * 60.0);
		}
	}

	public void addResultsToExcel(ExcelHolder holder, Sheet sheet, LinkedList<String> headers, ResultsTableMt rt) {

		// Create a bold font style
		Workbook wb = holder.wb;
		CellStyle boldStyle = wb.createCellStyle();
		Font f = wb.createFont();
		f.setColor(Font.COLOR_NORMAL);
		f.setBold(true);
		boldStyle.setFont(f);

		// Prepare the first rows with the headers
		for (int row = 0; row < rt.getCounter() + 2; row++) {
			sheet.createRow(row);
		}

		{
			ListIterator<String> itHeaders = headers.listIterator();
			int column = 0;
			Row headerRow = sheet.getRow(1);
			while (itHeaders.hasNext()) {
				org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(column);
				column++;
				cell.setCellValue(itHeaders.next());
			}
		}

		// Add the results, row by row
		for (int rowRt = 0; rowRt < rt.getCounter(); rowRt++) {
			Row row = sheet.getRow(rowRt + 2);
			for (int column = 0; column < headers.size(); column++) {
				if (headers.getLast().equalsIgnoreCase(TRAJ_SPEED_MEAN) && column == headers.size() - 1) {
					String strFormula = "A" + (rowRt + 3) + "*60";
					row.createCell(column).setCellFormula(strFormula);
				} else
					row.createCell(column).setCellValue(rt.getStringValue(column, rowRt));
			}
		}
	}

	/**
	 * Opens the sheet called {@link #sheetName} in the given workbook. If
	 * {@link #sheetName} is equal to {@link #DEFAULT_SHEET_NAME}, then we ignore it
	 * and open the last sheet in the workbook (if there are any existing sheets).
	 * If a sheet called {@link #sheetName} doesn't already exist, then we'll create
	 * a sheet with the name.
	 * 
	 * @param wb Workbook to open sheet in.
	 * @return Sheet called {@link #sheetName}.
	 */
	private Sheet openSheetInWorkbook(Workbook wb, String sheetName) {
		if (wb.getNumberOfSheets() == 0) {
			// If there are no sheets, just make a sheet with our current sheetName.
			return wb.createSheet(sheetName);
		} else {
			// We know there are existing sheets, either open or create the one
			// called sheetName.
			Sheet sheet = wb.getSheet(sheetName);
			// This is a shorthand if-statement: "[boolean expression] ? [return if true] :
			// [return if false];"
			return (sheet != null) ? sheet : wb.createSheet(sheetName);
		}
	}

	private static class ExcelHolder {
		private File excelFile;
		private FileInputStream fileIn;
		Workbook wb;

		public ExcelHolder(String filePath) {
			this.excelFile = new File(filePath);
		}

		void readFileAndOpenWorkbook(String sheetName) throws IOException, InvalidFormatException {
			ensureExcelFileExists(excelFile, sheetName);
			fileIn = new FileInputStream(excelFile);
			wb = WorkbookFactory.create(fileIn);
		}

		void newWorkbook(String sheetName) throws IOException, InvalidFormatException {
			if (excelFile.exists())
				excelFile.delete();
			readFileAndOpenWorkbook(sheetName);
		}

		void writeOutAndCloseWorkbook() throws IOException {
			FileOutputStream fileOut = new FileOutputStream(excelFile);
			wb.write(fileOut);
			if (fileIn != null)
				fileIn.close();
			fileOut.close();
		}

		void closeWorkbook() throws IOException {
			if (fileIn != null)
				fileIn.close();
		}

		private void ensureExcelFileExists(File excelFile, String defaultSheetName) throws IOException {
			if (!excelFile.exists()) {
				Workbook wb = new HSSFWorkbook();
				Sheet sheet = wb.createSheet(defaultSheetName);
				Row row = sheet.createRow(0);
				row.createCell(0);

				FileOutputStream tempOut = new FileOutputStream(excelFile);
				wb.write(tempOut);
				tempOut.close();
				wb.close();
			}
		}
	}
}
