package MP;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ij.plugin.PlugIn;

public class Combine_Results extends JFrame implements PlugIn, ActionListener, ListSelectionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8288301039264353779L;
	private JLabel lblFoldersToBe;
	private JButton btnAddFolders;
	private JButton btnDeleteFolders;
	private JButton btnOk;
	private JButton btnCancel;
	private JScrollPane scrollPane;
	private JList<String> list;
	private DefaultListModel<String> listmodel = new DefaultListModel<String>();
	DefaultListModel<String> resultList = new DefaultListModel<String>();
	private static final Font font = new Font("Segoe UI", Font.PLAIN, 13);

	public Combine_Results() {
		super("Combine results...");
		this.setBounds(20, 20, 600, 300);
		getContentPane().setFont(font);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 4, 551, 103, 2, 0 };
		gridBagLayout.rowHeights = new int[] { 4, 22, 31, 31, 31, 2, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		getContentPane().setLayout(gridBagLayout);

		lblFoldersToBe = new JLabel("Folders to be combined:");
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
		gbc_scrollPane.gridheight = 2;
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
		list.addListSelectionListener(this);
		scrollPane.setViewportView(list);

		btnAddFolders = new JButton("Add folders");
		btnAddFolders.setFont(font);
		GridBagConstraints gbc_btnAddFolders = new GridBagConstraints();
		gbc_btnAddFolders.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnAddFolders.insets = new Insets(0, 0, 5, 5);
		gbc_btnAddFolders.gridx = 2;
		gbc_btnAddFolders.gridy = 2;
		btnAddFolders.addActionListener(this);
		getContentPane().add(btnAddFolders, gbc_btnAddFolders);

		btnDeleteFolders = new JButton("Delete folders");
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

		btnCancel = new JButton("Cancel");
		btnCancel.setFont(font);
		GridBagConstraints gbc_btnCancel = new GridBagConstraints();
		gbc_btnCancel.ipadx = 40;
		gbc_btnCancel.anchor = GridBagConstraints.EAST;
		gbc_btnCancel.insets = new Insets(0, 0, 5, 5);
		gbc_btnCancel.gridx = 1;
		gbc_btnCancel.gridy = 4;
		btnCancel.addActionListener(this);
		getContentPane().add(btnCancel, gbc_btnCancel);

		btnOk = new JButton("OK");
		btnOk.setFont(font);
		GridBagConstraints gbc_btnOk = new GridBagConstraints();
		gbc_btnOk.insets = new Insets(0, 0, 5, 5);
		gbc_btnOk.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnOk.gridx = 2;
		gbc_btnOk.gridy = 4;
		btnOk.addActionListener(this);
		getContentPane().add(btnOk, gbc_btnOk);

		this.setVisible(true);
	}

	@Override
	public void run(String arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == btnAddFolders) {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("Select folders to be combined:");
			chooser.setMultiSelectionEnabled(true);

			int returnVal = chooser.showOpenDialog(null);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				for (int i = 0; i < chooser.getSelectedFiles().length; i++) {
					String path = chooser.getSelectedFiles()[i].getAbsolutePath();
					if (!listmodel.contains(path))
						listmodel.addElement(path);
				}
			}
		} else if (source == btnDeleteFolders) {
			int[] index = list.getSelectedIndices();
			if (index == null) {
				btnDeleteFolders.setEnabled(false);
			} else {
				for (int i = index.length - 1; i >= 0; i--) {
					listmodel.remove(index[i]);
				}

				int size = listmodel.getSize();

				if (size == 0) { // Nobody's left, disable firing.
					btnDeleteFolders.setEnabled(false);
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

			} else {
				// Selection, enable the fire button.
				btnDeleteFolders.setEnabled(true);
			}

		}
	}

	void doCombine() {
		// Use the resultList to read results one by one, combine them, and save them
		ResultsTableMt rt = new ResultsTableMt();
		String newColumnTitle = "Original File";
		int newColumn = rt.addNewColumn(newColumnTitle);

		int avgNumberOfProtrusions = 0, avgDistOfTheProt = 0, closestProt = 0, avgSizeOfProt = 0, avgAreaOfTheUro = 0;

		for (int i = 0; i < resultList.getSize(); i++) {
			ResultsTableMt rtTemp = ResultsTableMt.open2(resultList.get(i) + File.separator + "2-Results.csv");
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

				if (rt.getValueAsDouble(avgNumberOfProtrusions, j) == 0) {
					rt.setValue(avgDistOfTheProt, j, "");
					rt.setValue(closestProt, j, "");
					rt.setValue(avgSizeOfProt, j, "");
					rt.setValue(avgAreaOfTheUro, j, "");
				}
			}
		}

		rt.saveAsPrecise(new File(resultList.get(0)).getParent() + File.separator + "2-Combined_Results.csv", 5);
	}
}
