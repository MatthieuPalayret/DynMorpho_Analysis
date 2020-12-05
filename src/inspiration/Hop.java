package inspiration;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

public class Hop extends JFrame {
	private JLabel lblFoldersToBe;
	private JScrollPane scrollPane;
	private JButton btnAddFolders;
	private JButton btnDeleteFolders;
	private JButton btnCancel;
	private JButton btnOk;
	private JRadioButton rdbtnAnalyseOnlyGreen;
	private JRadioButton rdbtnAnalyseOnlyRed;
	private JRadioButton rdbtnAnalyseAllCells;

	public Hop() {
		this.setBounds(20, 20, 600, 400);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 4, 551, 103, 2, 0 };
		gridBagLayout.rowHeights = new int[] { 4, 22, 31, 31, 0, 0, 0, 0, 31, 2, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		getContentPane().setLayout(gridBagLayout);

		lblFoldersToBe = new JLabel("to be combined:");
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

		btnAddFolders = new JButton("Add");
		GridBagConstraints gbc_btnAddFolders = new GridBagConstraints();
		gbc_btnAddFolders.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnAddFolders.insets = new Insets(0, 0, 5, 5);
		gbc_btnAddFolders.gridx = 2;
		gbc_btnAddFolders.gridy = 2;
		getContentPane().add(btnAddFolders, gbc_btnAddFolders);

		btnDeleteFolders = new JButton("Delete");
		GridBagConstraints gbc_btnDeleteFolders = new GridBagConstraints();
		gbc_btnDeleteFolders.anchor = GridBagConstraints.NORTH;
		gbc_btnDeleteFolders.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnDeleteFolders.insets = new Insets(0, 0, 5, 5);
		gbc_btnDeleteFolders.gridx = 2;
		gbc_btnDeleteFolders.gridy = 3;
		getContentPane().add(btnDeleteFolders, gbc_btnDeleteFolders);

		rdbtnAnalyseOnlyGreen = new JRadioButton("Analyse only green cells");
		GridBagConstraints gbc_rdbtnAnalyseOnlyGreen = new GridBagConstraints();
		gbc_rdbtnAnalyseOnlyGreen.anchor = GridBagConstraints.WEST;
		gbc_rdbtnAnalyseOnlyGreen.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnAnalyseOnlyGreen.gridx = 2;
		gbc_rdbtnAnalyseOnlyGreen.gridy = 4;
		getContentPane().add(rdbtnAnalyseOnlyGreen, gbc_rdbtnAnalyseOnlyGreen);

		rdbtnAnalyseOnlyRed = new JRadioButton("Analyse only red cells");
		GridBagConstraints gbc_rdbtnAnalyseOnlyRed = new GridBagConstraints();
		gbc_rdbtnAnalyseOnlyRed.anchor = GridBagConstraints.WEST;
		gbc_rdbtnAnalyseOnlyRed.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnAnalyseOnlyRed.gridx = 2;
		gbc_rdbtnAnalyseOnlyRed.gridy = 5;
		getContentPane().add(rdbtnAnalyseOnlyRed, gbc_rdbtnAnalyseOnlyRed);

		rdbtnAnalyseAllCells = new JRadioButton("Analyse all cells");
		GridBagConstraints gbc_rdbtnAnalyseAllCells = new GridBagConstraints();
		gbc_rdbtnAnalyseAllCells.anchor = GridBagConstraints.WEST;
		gbc_rdbtnAnalyseAllCells.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnAnalyseAllCells.gridx = 2;
		gbc_rdbtnAnalyseAllCells.gridy = 6;
		getContentPane().add(rdbtnAnalyseAllCells, gbc_rdbtnAnalyseAllCells);

		btnCancel = new JButton("Cancel");
		GridBagConstraints gbc_btnCancel = new GridBagConstraints();
		gbc_btnCancel.ipadx = 40;
		gbc_btnCancel.anchor = GridBagConstraints.EAST;
		gbc_btnCancel.insets = new Insets(0, 0, 5, 5);
		gbc_btnCancel.gridx = 1;
		gbc_btnCancel.gridy = 8;
		getContentPane().add(btnCancel, gbc_btnCancel);

		btnOk = new JButton("OK");
		GridBagConstraints gbc_btnOk = new GridBagConstraints();
		gbc_btnOk.insets = new Insets(0, 0, 5, 5);
		gbc_btnOk.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnOk.gridx = 2;
		gbc_btnOk.gridy = 8;
		getContentPane().add(btnOk, gbc_btnOk);

		this.setVisible(true);
	}

}
