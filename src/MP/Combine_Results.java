package MP;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ij.IJ;
import ij.plugin.PlugIn;

public class Combine_Results extends JFrame implements PlugIn, ActionListener, ListSelectionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8288301039264353779L;
	private JLabel lblFoldersToBe;
	private JButton btnAddFolders;
	private JButton btnDeleteFolders;
	private JRadioButton rdbtnAnalyseOnlyGreen;
	private JRadioButton rdbtnAnalyseOnlyRed;
	private JRadioButton rdbtnAnalyseAllCells;
	final ButtonGroup buttonGroup = new ButtonGroup();
	private JButton btnOk;
	private JButton btnCancel;
	private JScrollPane scrollPane;
	private JList<String> list;
	private HashMap<String, Integer> hmWhichCells = new HashMap<String, Integer>();
	private static final int GREEN_CELLS = 0, RED_CELLS = 1, ALL_CELLS = 2;
	private static final String[] cellTag = { "_green", "_red", "" };
	private static final String[] buttonLabel = { "Analyse only green cells", "Analyse only red cells",
			"Analyse all cells" };
	private static final Color LIGHT_GREEN = new Color(179, 255, 179), GREEN = new Color(128, 255, 128),
			RED = new Color(255, 128, 128), LIGHT_RED = new Color(255, 179, 179);
	private DefaultListModel<String> listmodel = new DefaultListModel<String>();
	DefaultListModel<String> resultList = new DefaultListModel<String>();
	private static final Font font = new Font("Segoe UI", Font.PLAIN, 13);
	String fileType = "folder";
	int fileChooserType = JFileChooser.DIRECTORIES_ONLY;
	File currentDirectory = null;

	public Combine_Results() {
		super("Combine results...");
	}

	public Combine_Results(String fileType, int fileChooserType) {
		this();
		this.fileType = fileType;
		this.fileChooserType = fileChooserType;
	}

	@Override
	public void run(String arg0) {
		this.setBounds(20, 20, 600, 400);
		getContentPane().setFont(font);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 4, 551, 103, 2, 0 };
		gridBagLayout.rowHeights = new int[] { 4, 22, 31, 31, 0, 0, 0, 0, 31, 2, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		getContentPane().setLayout(gridBagLayout);

		lblFoldersToBe = new JLabel(
				fileType.substring(0, 1).toUpperCase() + fileType.substring(1) + "s to be combined:");
		lblFoldersToBe.setFont(font);
		GridBagConstraints gbc_lblFoldersToBe = new GridBagConstraints();
		gbc_lblFoldersToBe.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblFoldersToBe.insets = new Insets(0, 0, 5, 5);
		gbc_lblFoldersToBe.gridx = 1;
		gbc_lblFoldersToBe.gridy = 1;
		getContentPane().add(lblFoldersToBe, gbc_lblFoldersToBe);

		scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane.gridheight = 6;
		gbc_scrollPane.gridx = 1;
		gbc_scrollPane.gridy = 2;
		getContentPane().add(scrollPane, gbc_scrollPane);

		list = new JList<String>(listmodel);
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setLayoutOrientation(JList.VERTICAL);
		list.setVisibleRowCount(-1);
		list.setBorder(new LineBorder(new Color(0, 0, 0)));
		list.setVisibleRowCount(10);
		list.setFont(font);
		list.setCellRenderer(new MyCellRenderer());
		list.addListSelectionListener(this);
		scrollPane.setViewportView(list);

		btnAddFolders = new JButton("Add " + fileType + "s");
		btnAddFolders.setFont(font);
		GridBagConstraints gbc_btnAddFolders = new GridBagConstraints();
		gbc_btnAddFolders.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnAddFolders.insets = new Insets(0, 0, 5, 5);
		gbc_btnAddFolders.gridx = 2;
		gbc_btnAddFolders.gridy = 2;
		btnAddFolders.addActionListener(this);
		getContentPane().add(btnAddFolders, gbc_btnAddFolders);

		btnDeleteFolders = new JButton("Delete " + fileType + "s");
		btnDeleteFolders.setFont(font);
		GridBagConstraints gbc_btnDeleteFolders = new GridBagConstraints();
		gbc_btnDeleteFolders.anchor = GridBagConstraints.NORTH;
		gbc_btnDeleteFolders.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnDeleteFolders.insets = new Insets(0, 0, 5, 5);
		gbc_btnDeleteFolders.gridx = 2;
		gbc_btnDeleteFolders.gridy = 3;
		btnDeleteFolders.addActionListener(this);
		btnDeleteFolders.setEnabled(false);
		getContentPane().add(btnDeleteFolders, gbc_btnDeleteFolders);

		rdbtnAnalyseOnlyGreen = new JRadioButton(buttonLabel[GREEN_CELLS]);
		buttonGroup.add(rdbtnAnalyseOnlyGreen);
		GridBagConstraints gbc_rdbtnAnalyseOnlyGreen = new GridBagConstraints();
		gbc_rdbtnAnalyseOnlyGreen.anchor = GridBagConstraints.WEST;
		gbc_rdbtnAnalyseOnlyGreen.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnAnalyseOnlyGreen.gridx = 2;
		gbc_rdbtnAnalyseOnlyGreen.gridy = 4;
		rdbtnAnalyseOnlyGreen.setBackground(GREEN);
		rdbtnAnalyseOnlyGreen.setSelected(false);
		rdbtnAnalyseOnlyGreen.addActionListener(this);
		getContentPane().add(rdbtnAnalyseOnlyGreen, gbc_rdbtnAnalyseOnlyGreen);

		rdbtnAnalyseOnlyRed = new JRadioButton(buttonLabel[RED_CELLS]);
		buttonGroup.add(rdbtnAnalyseOnlyRed);
		GridBagConstraints gbc_rdbtnAnalyseOnlyRed = new GridBagConstraints();
		gbc_rdbtnAnalyseOnlyRed.anchor = GridBagConstraints.WEST;
		gbc_rdbtnAnalyseOnlyRed.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnAnalyseOnlyRed.gridx = 2;
		gbc_rdbtnAnalyseOnlyRed.gridy = 5;
		rdbtnAnalyseOnlyRed.setBackground(RED);
		rdbtnAnalyseOnlyRed.setSelected(false);
		rdbtnAnalyseOnlyRed.addActionListener(this);
		getContentPane().add(rdbtnAnalyseOnlyRed, gbc_rdbtnAnalyseOnlyRed);

		rdbtnAnalyseAllCells = new JRadioButton(buttonLabel[ALL_CELLS]);
		buttonGroup.add(rdbtnAnalyseAllCells);
		GridBagConstraints gbc_rdbtnAnalyseAllCells = new GridBagConstraints();
		gbc_rdbtnAnalyseAllCells.anchor = GridBagConstraints.WEST;
		gbc_rdbtnAnalyseAllCells.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnAnalyseAllCells.gridx = 2;
		gbc_rdbtnAnalyseAllCells.gridy = 6;
		rdbtnAnalyseAllCells.setSelected(true);
		rdbtnAnalyseAllCells.addActionListener(this);
		getContentPane().add(rdbtnAnalyseAllCells, gbc_rdbtnAnalyseAllCells);

		btnCancel = new JButton("Cancel");
		btnCancel.setFont(font);
		GridBagConstraints gbc_btnCancel = new GridBagConstraints();
		gbc_btnCancel.ipadx = 40;
		gbc_btnCancel.anchor = GridBagConstraints.EAST;
		gbc_btnCancel.insets = new Insets(0, 0, 5, 5);
		gbc_btnCancel.gridx = 1;
		gbc_btnCancel.gridy = 8;
		btnCancel.addActionListener(this);
		getContentPane().add(btnCancel, gbc_btnCancel);

		btnOk = new JButton("OK");
		btnOk.setFont(font);
		GridBagConstraints gbc_btnOk = new GridBagConstraints();
		gbc_btnOk.insets = new Insets(0, 0, 5, 5);
		gbc_btnOk.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnOk.gridx = 2;
		gbc_btnOk.gridy = 8;
		btnOk.addActionListener(this);
		getContentPane().add(btnOk, gbc_btnOk);

		this.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == btnAddFolders) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(fileChooserType);
			chooser.setDialogTitle("Select " + fileType + "s to be combined:");
			chooser.setMultiSelectionEnabled(true);
			if (currentDirectory != null)
				chooser.setCurrentDirectory(currentDirectory);

			int returnVal = chooser.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				for (int i = 0; i < chooser.getSelectedFiles().length; i++) {
					String path = chooser.getSelectedFiles()[i].getAbsolutePath();
					if (!listmodel.contains(path)) {
						hmWhichCells.put(path, ALL_CELLS);
						rdbtnAnalyseAllCells.setSelected(true);
						listmodel.addElement(path);
					}
				}
			}

			currentDirectory = chooser.getCurrentDirectory();
		} else if (source == btnDeleteFolders) {
			int[] index = list.getSelectedIndices();
			if (index == null) {
				btnDeleteFolders.setEnabled(false);
			} else {
				for (int i = index.length - 1; i >= 0; i--) {
					String temp = listmodel.get(index[i]);
					listmodel.remove(index[i]);
					hmWhichCells.remove(temp);
				}

				int size = listmodel.getSize();

				if (size == 0) { // Nobody's left, disable firing.
					btnDeleteFolders.setEnabled(false);
				}
			}
		} else if (source == rdbtnAnalyseOnlyGreen) {
			if (rdbtnAnalyseOnlyGreen.isSelected()) {
				int[] index = list.getSelectedIndices();
				if (index != null) {
					for (int i = index.length - 1; i >= 0; i--) {
						hmWhichCells.replace(listmodel.get(index[i]), GREEN_CELLS);
						listmodel.set(index[i], listmodel.get(index[i])); // To force a change of background colour.
					}
				}
			}
		} else if (source == rdbtnAnalyseOnlyRed) {
			if (rdbtnAnalyseOnlyRed.isSelected()) {
				int[] index = list.getSelectedIndices();
				if (index != null) {
					for (int i = index.length - 1; i >= 0; i--) {
						hmWhichCells.replace(listmodel.get(index[i]), RED_CELLS);
						listmodel.set(index[i], listmodel.get(index[i])); // To force a change of background colour.
					}
				}
			}
		} else if (source == rdbtnAnalyseAllCells) {
			if (rdbtnAnalyseAllCells.isSelected()) {
				int[] index = list.getSelectedIndices();
				if (index != null) {
					for (int i = index.length - 1; i >= 0; i--) {
						hmWhichCells.replace(listmodel.get(index[i]), ALL_CELLS);
						listmodel.set(index[i], listmodel.get(index[i])); // To force a change of background colour.
					}
				}
			}
		} else if (source == btnCancel) {
			resultList = null;
			disposeThis();
		} else if (source == btnOk) {
			resultList = listmodel;
			disposeThis();
			doCombine();
		}
	}

	private void disposeThis() {
		this.setVisible(false);
		btnAddFolders.removeActionListener(this);
		btnDeleteFolders.removeActionListener(this);
		btnOk.removeActionListener(this);
		btnCancel.removeActionListener(this);
		list.removeListSelectionListener(this);
		this.dispose();
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {

			if (list.getSelectedIndex() == -1) {
				// No selection, disable fire button.
				btnDeleteFolders.setEnabled(false);
				rdbtnAnalyseOnlyGreen.setEnabled(false);
				rdbtnAnalyseOnlyGreen.setBackground(LIGHT_GREEN);
				rdbtnAnalyseOnlyRed.setEnabled(false);
				rdbtnAnalyseOnlyRed.setBackground(LIGHT_RED);
				rdbtnAnalyseAllCells.setEnabled(false);

			} else {
				// Selection, enable the fire button.
				btnDeleteFolders.setEnabled(true);

				boolean[] allFoldersWithTwoColorResults = new boolean[cellTag.length];
				for (int j = 0; j < cellTag.length; j++)
					allFoldersWithTwoColorResults[j] = true;
				int[] index = list.getSelectedIndices();
				for (int i = index.length - 1; i >= 0; i--) {
					File file = new File(listmodel.get(index[i]));
					if (file.isDirectory()) {
						for (int j = 0; j < cellTag.length; j++)
							allFoldersWithTwoColorResults[j] = allFoldersWithTwoColorResults[j]
									&& new File(file + File.separator + "2-Results" + cellTag[j] + ".csv").exists()
									&& ResultsTableMt.open2(file + File.separator + "2-Results" + cellTag[j] + ".csv")
											.getCounter() > 0;
					} else {
						allFoldersWithTwoColorResults[GREEN_CELLS] = false;
						allFoldersWithTwoColorResults[RED_CELLS] = false;
					}
				}

				rdbtnAnalyseOnlyGreen.setEnabled(allFoldersWithTwoColorResults[GREEN_CELLS]);
				rdbtnAnalyseOnlyGreen.setBackground(allFoldersWithTwoColorResults[GREEN_CELLS] ? GREEN : LIGHT_GREEN);
				rdbtnAnalyseOnlyRed.setEnabled(allFoldersWithTwoColorResults[RED_CELLS]);
				rdbtnAnalyseOnlyRed.setBackground(allFoldersWithTwoColorResults[RED_CELLS] ? RED : LIGHT_RED);
				rdbtnAnalyseAllCells.setEnabled(allFoldersWithTwoColorResults[ALL_CELLS]);
				if (index.length == 1) {
					File file = new File(
							listmodel.get(index[0]) + File.separator + "2-Results" + cellTag[GREEN_CELLS] + ".csv");
					if (file.exists())
						rdbtnAnalyseOnlyGreen.setText(buttonLabel[GREEN_CELLS] + " ("
								+ ResultsTableMt.open2(file.getAbsolutePath()).getCounter() + ")");
					else
						rdbtnAnalyseOnlyGreen.setText(buttonLabel[GREEN_CELLS]);
					file = new File(
							listmodel.get(index[0]) + File.separator + "2-Results" + cellTag[RED_CELLS] + ".csv");
					if (file.exists())
						rdbtnAnalyseOnlyRed.setText(buttonLabel[RED_CELLS] + " ("
								+ ResultsTableMt.open2(file.getAbsolutePath()).getCounter() + ")");
					else
						rdbtnAnalyseOnlyRed.setText(buttonLabel[RED_CELLS]);
					file = new File(
							listmodel.get(index[0]) + File.separator + "2-Results" + cellTag[ALL_CELLS] + ".csv");
					if (file.exists())
						rdbtnAnalyseAllCells.setText(buttonLabel[ALL_CELLS] + " ("
								+ ResultsTableMt.open2(file.getAbsolutePath()).getCounter() + ")");
					else
						rdbtnAnalyseAllCells.setText(buttonLabel[ALL_CELLS]);
				} else {
					rdbtnAnalyseOnlyGreen.setText(buttonLabel[GREEN_CELLS]);
					rdbtnAnalyseOnlyRed.setText(buttonLabel[RED_CELLS]);
					rdbtnAnalyseAllCells.setText(buttonLabel[ALL_CELLS]);
				}

				int sum = 0;
				int indexTrue = -1;
				for (int i = 0; i < cellTag.length; i++) {
					sum += allFoldersWithTwoColorResults[i] ? 1 : 0;
					if (sum == 1 && indexTrue == -1)
						indexTrue = i;
				}
				if (sum == 1) {
					if (indexTrue == GREEN_CELLS)
						rdbtnAnalyseOnlyGreen.setSelected(true);
					else if (indexTrue == RED_CELLS)
						rdbtnAnalyseOnlyRed.setSelected(true);
					else if (indexTrue == ALL_CELLS)
						rdbtnAnalyseAllCells.setSelected(true);
				}
			}

		}
	}

	void doCombine() {
		// Use the resultList to read results one by one, combine them, and save them
		ResultsTableMt rt = new ResultsTableMt();
		String newColumnTitle = "Original File";
		int newColumn = rt.addNewColumn(newColumnTitle);
		int newColumn2 = rt.addNewColumn("Green- red- or all-cells?");

		int avgNumberOfProtrusions = 0, avgDistOfTheProt = 0, closestProt = 0, avgSizeOfProt = 0, avgAreaOfTheUro = 0;

		for (int i = 0; i < resultList.getSize(); i++) {
			int whichCells = hmWhichCells.get(resultList.get(i));
			if (new File(resultList.get(i) + File.separator + "2-Results" + cellTag[whichCells] + ".csv").exists()) {
				ResultsTableMt rtTemp = ResultsTableMt
						.open2(resultList.get(i) + File.separator + "2-Results" + cellTag[whichCells] + ".csv");
				int firstRowValue = rt.getCounter();
				ResultsTableMt.concatenate(rtTemp, rt);

				if (i == 0) {
					avgNumberOfProtrusions = rt.addNewColumn("AvgNumberOfProtrusions");
					avgDistOfTheProt = rt.addNewColumn("AvDistOfTheProtrusionToTheLeadingEdge (%)");
					closestProt = rt.addNewColumn("ClosestProtrusionToTheUropod (%)");
					avgSizeOfProt = rt.addNewColumn("AvgSizeOfProtrusions (µm²)");
					avgAreaOfTheUro = rt.addNewColumn("AvgAreaOfTheUropod (µm²)");
				}

				for (int j = firstRowValue; j < rt.getCounter(); j++) {
					rt.setValue(newColumn, j, resultList.get(i));
					rt.setValue(newColumn2, j, whichCells);

					if (rt.getValueAsDouble(avgNumberOfProtrusions, j) == 0) {
						rt.setValue(avgDistOfTheProt, j, "");
						rt.setValue(closestProt, j, "");
						rt.setValue(avgSizeOfProt, j, "");
						rt.setValue(avgAreaOfTheUro, j, "");
					}
				}
			} else {
				IJ.log(resultList.get(i) + " is not a directory containing a 2-Results.csv file. It is thus ignored.");
			}
		}

		rt.saveAsPrecise(new File(resultList.get(0)).getParent() + File.separator + "2-Combined_Results.csv", 5);
	}

	// Display an icon and a string for each object in the list.
	class MyCellRenderer extends JLabel implements ListCellRenderer<Object> {

		/**
		 * 
		 */
		private static final long serialVersionUID = 17542363519142932L;
		// This is the only method defined by ListCellRenderer.
		// We just reconfigure the JLabel each time we're called.

		public MyCellRenderer() {
			setOpaque(true);
		}

		@Override
		public Component getListCellRendererComponent(JList<?> list, // the list
				Object value, // value to display
				int index, // cell index
				boolean isSelected, // is the cell selected
				boolean cellHasFocus) // does the cell have focus
		{
			setText((String) value);
			int cell = hmWhichCells.get(value);

			if (isSelected) {
				Color cellColour = (cell == GREEN_CELLS ? GREEN
						: (cell == RED_CELLS ? RED : list.getSelectionBackground()));
				setBackground(cellColour);
				setForeground(list.getSelectionForeground());
			} else {
				Color cellColour = (cell == GREEN_CELLS ? LIGHT_GREEN
						: (cell == RED_CELLS ? LIGHT_RED : list.getBackground()));
				setBackground(cellColour);
				setForeground(list.getForeground());
			}
			setEnabled(list.isEnabled());
			setFont(list.getFont());
			return this;
		}

	}

}
