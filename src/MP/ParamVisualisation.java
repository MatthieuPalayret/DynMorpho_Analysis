package MP;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import Cell.CellData;
import UtilClasses.GenUtils;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;

public class ParamVisualisation extends JFrame
		implements ActionListener, ChangeListener, ImageListener, FocusListener, KeyListener, MouseListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3108969450503939630L;
	protected Params params, paramTemp;
	private ImagePlus image;
	private int frame = 0;

	private static final Font font = new Font("Segoe UI", Font.PLAIN, 13);
	private JLabel lblParam;
	private JLabel lblMinCurvProtrusions;
	private JSlider sliderMinCurvProtrusions;
	private JTextField txtMinCurvProtrusions;
	private JLabel lblMinLengthTraj;
	private JSlider sliderMinLengthTraj;
	private JTextField txtMinLengthTraj;
	private JLabel lblDramaticIncrease;
	private JSlider sliderDramaticIncrease;
	private JTextField txtDramaticIncrease;
	private JLabel lblMinAreaProtrusion;
	private JSlider sliderMinAreaProtrusion;
	private JTextField txtMinAreaProtrusion;
	private JLabel lblMaxProtrusionToCellSurfaceRatio;
	private JSlider sliderMaxProtrusionToCellSurfaceRatio;
	private JTextField txtMaxProtrusionToCellSurfaceRatio;
	private JLabel lblSmoothingWdw;
	private JSlider sliderSmoothingWdw;
	private JTextField txtSmoothingWdw;
	private JLabel lblDetectUropods;
	private JCheckBox chckbxDetectUropods;
	private JLabel lblInteractiveRejectionOfCells;
	private JRadioButton rdbtnNoRejection;
	private JRadioButton rdbtnRejectCellIn;
	private JRadioButton rdbtnRejectAWhole;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private ij.gui.ImageCanvas canvas_1;
	private JButton btnOk;
	private JButton btnReset;
	private JButton btnCancel;

	final static int CANCEL = 1;
	final static int FINISHED = 2;
	final static int RUNNING = 0;
	int finished = RUNNING;
	private Results results;

	public ParamVisualisation(Params params, Results results, ImageStack stack) {
		super("Parameter visualisation...");

		this.params = params;
		paramTemp = params.clone();
		this.results = results;
		results.stack = stack;
		image = new ImagePlus("Visualisation", results.stack);
		results.imp = image;
		image = new ImagePlus("Visualisation", GenUtils.convertStack(image, 32).getImageStack());
		frame = image.getCurrentSlice() - 1;

		image.updateAndDraw();
		image.show();
		image.getWindow().setLocation(505, 20);
		canvas_1 = image.getCanvas();
		ImagePlus.addImageListener(this);
		canvas_1.addKeyListener(this);
		canvas_1.addMouseListener(this);

		this.setBounds(20, 20, 500, 430);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 139, 0, 53, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				1.0, Double.MIN_VALUE };
		getContentPane().setLayout(gridBagLayout);

		addKeyListener(this);

		lblParam = new JLabel("Parameters:");
		lblParam.setFont(font);
		GridBagConstraints gbc_lblParam = new GridBagConstraints();
		gbc_lblParam.gridwidth = 3;
		gbc_lblParam.insets = new Insets(0, 0, 5, 5);
		gbc_lblParam.gridx = 1;
		gbc_lblParam.gridy = 1;
		getContentPane().add(lblParam, gbc_lblParam);

		lblMinCurvProtrusions = new JLabel("Minimal curvature for protrusions (°):");
		lblMinCurvProtrusions.setFont(font);
		GridBagConstraints gbc_lblMinCurvProtrusions = new GridBagConstraints();
		gbc_lblMinCurvProtrusions.anchor = GridBagConstraints.EAST;
		gbc_lblMinCurvProtrusions.insets = new Insets(0, 0, 5, 5);
		gbc_lblMinCurvProtrusions.gridx = 1;
		gbc_lblMinCurvProtrusions.gridy = 2;
		getContentPane().add(lblMinCurvProtrusions, gbc_lblMinCurvProtrusions);

		sliderMinCurvProtrusions = new JSlider(-180, 180, (int) (-paramTemp.curvatureMinLevel));
		sliderMinCurvProtrusions.setFont(font);
		GridBagConstraints gbc_sliderMinCurvProtrusions = new GridBagConstraints();
		gbc_sliderMinCurvProtrusions.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderMinCurvProtrusions.insets = new Insets(0, 0, 5, 5);
		gbc_sliderMinCurvProtrusions.gridx = 2;
		gbc_sliderMinCurvProtrusions.gridy = 2;
		sliderMinCurvProtrusions.addChangeListener(this);
		getContentPane().add(sliderMinCurvProtrusions, gbc_sliderMinCurvProtrusions);

		txtMinCurvProtrusions = new JTextField(IJ.d2s(-paramTemp.curvatureMinLevel, 0));
		txtMinCurvProtrusions.setFont(font);
		GridBagConstraints gbc_txtMinCurvProtrusions = new GridBagConstraints();
		gbc_txtMinCurvProtrusions.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMinCurvProtrusions.insets = new Insets(0, 0, 5, 5);
		gbc_txtMinCurvProtrusions.gridx = 3;
		gbc_txtMinCurvProtrusions.gridy = 2;
		txtMinCurvProtrusions.addActionListener(this);
		txtMinCurvProtrusions.addFocusListener(this);
		getContentPane().add(txtMinCurvProtrusions, gbc_txtMinCurvProtrusions);

		lblMinLengthTraj = new JLabel("Minimal length of a trajectory (s):");
		lblMinLengthTraj.setFont(font);
		GridBagConstraints gbc_lblMinLengthTraj = new GridBagConstraints();
		gbc_lblMinLengthTraj.anchor = GridBagConstraints.EAST;
		gbc_lblMinLengthTraj.insets = new Insets(0, 0, 5, 5);
		gbc_lblMinLengthTraj.gridx = 1;
		gbc_lblMinLengthTraj.gridy = 3;
		getContentPane().add(lblMinLengthTraj, gbc_lblMinLengthTraj);

		sliderMinLengthTraj = new JSlider(0, image.getStackSize(), paramTemp.minTrajLength);
		sliderMinLengthTraj.setFont(font);
		GridBagConstraints gbc_sliderMinLengthTraj = new GridBagConstraints();
		gbc_sliderMinLengthTraj.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderMinLengthTraj.insets = new Insets(0, 0, 5, 5);
		gbc_sliderMinLengthTraj.gridx = 2;
		gbc_sliderMinLengthTraj.gridy = 3;
		sliderMinLengthTraj.addChangeListener(this);
		getContentPane().add(sliderMinLengthTraj, gbc_sliderMinLengthTraj);

		txtMinLengthTraj = new JTextField(IJ.d2s(paramTemp.minTrajLength * paramTemp.frameLengthS, 3));
		txtMinLengthTraj.setFont(font);
		GridBagConstraints gbc_txtMinLengthTraj = new GridBagConstraints();
		gbc_txtMinLengthTraj.insets = new Insets(0, 0, 5, 5);
		gbc_txtMinLengthTraj.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMinLengthTraj.gridx = 3;
		gbc_txtMinLengthTraj.gridy = 3;
		getContentPane().add(txtMinLengthTraj, gbc_txtMinLengthTraj);
		txtMinLengthTraj.addActionListener(this);
		txtMinLengthTraj.addFocusListener(this);

		lblDramaticIncrease = new JLabel("Dramatic cell area in-/de-crease (%):");
		lblDramaticIncrease.setFont(font);
		GridBagConstraints gbc_lblDramaticIncrease = new GridBagConstraints();
		gbc_lblDramaticIncrease.anchor = GridBagConstraints.EAST;
		gbc_lblDramaticIncrease.insets = new Insets(0, 0, 5, 5);
		gbc_lblDramaticIncrease.gridx = 1;
		gbc_lblDramaticIncrease.gridy = 4;
		getContentPane().add(lblDramaticIncrease, gbc_lblDramaticIncrease);

		sliderDramaticIncrease = new JSlider(101, 200, (int) paramTemp.dramaticAreaIncrease);
		sliderDramaticIncrease.setFont(font);
		GridBagConstraints gbc_sliderDramaticIncrease = new GridBagConstraints();
		gbc_sliderDramaticIncrease.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderDramaticIncrease.insets = new Insets(0, 0, 5, 5);
		gbc_sliderDramaticIncrease.gridx = 2;
		gbc_sliderDramaticIncrease.gridy = 4;
		sliderDramaticIncrease.addChangeListener(this);
		getContentPane().add(sliderDramaticIncrease, gbc_sliderDramaticIncrease);

		txtDramaticIncrease = new JTextField(IJ.d2s(paramTemp.dramaticAreaIncrease, 0));
		txtDramaticIncrease.setFont(font);
		GridBagConstraints gbc_txtDramaticIncrease = new GridBagConstraints();
		gbc_txtDramaticIncrease.insets = new Insets(0, 0, 5, 5);
		gbc_txtDramaticIncrease.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtDramaticIncrease.gridx = 3;
		gbc_txtDramaticIncrease.gridy = 4;
		getContentPane().add(txtDramaticIncrease, gbc_txtDramaticIncrease);
		txtDramaticIncrease.addActionListener(this);
		txtDramaticIncrease.addFocusListener(this);

		lblMinAreaProtrusion = new JLabel("Minimal area of a protrusion (\u00B5m\u00B2):");
		lblMinAreaProtrusion.setFont(font);
		GridBagConstraints gbc_lblMinAreaProtrusion = new GridBagConstraints();
		gbc_lblMinAreaProtrusion.anchor = GridBagConstraints.EAST;
		gbc_lblMinAreaProtrusion.insets = new Insets(0, 0, 5, 5);
		gbc_lblMinAreaProtrusion.gridx = 1;
		gbc_lblMinAreaProtrusion.gridy = 5;
		getContentPane().add(lblMinAreaProtrusion, gbc_lblMinAreaProtrusion);

		sliderMinAreaProtrusion = new JSlider(0,
				(int) (10.0D * paramTemp.minAreaDetection * paramTemp.getPixelSizeUmSquared() * 10.0),
				(int) (paramTemp.minAreaDetection * paramTemp.getPixelSizeUmSquared() * 10.0));
		sliderMinAreaProtrusion.setFont(font);
		GridBagConstraints gbc_sliderMinAreaProtrusion = new GridBagConstraints();
		gbc_sliderMinAreaProtrusion.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderMinAreaProtrusion.insets = new Insets(0, 0, 5, 5);
		gbc_sliderMinAreaProtrusion.gridx = 2;
		gbc_sliderMinAreaProtrusion.gridy = 5;
		sliderMinAreaProtrusion.addChangeListener(this);
		getContentPane().add(sliderMinAreaProtrusion, gbc_sliderMinAreaProtrusion);

		txtMinAreaProtrusion = new JTextField(
				IJ.d2s(paramTemp.minAreaDetection * paramTemp.getPixelSizeUmSquared(), 1));
		txtMinAreaProtrusion.setFont(font);
		GridBagConstraints gbc_txtMinAreaProtrusion = new GridBagConstraints();
		gbc_txtMinAreaProtrusion.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMinAreaProtrusion.insets = new Insets(0, 0, 5, 5);
		gbc_txtMinAreaProtrusion.gridx = 3;
		gbc_txtMinAreaProtrusion.gridy = 5;
		txtMinAreaProtrusion.addActionListener(this);
		txtMinAreaProtrusion.addFocusListener(this);
		getContentPane().add(txtMinAreaProtrusion, gbc_txtMinAreaProtrusion);

		lblMaxProtrusionToCellSurfaceRatio = new JLabel("Maximal protrusion to cell surface ratio (%):");
		lblMaxProtrusionToCellSurfaceRatio.setFont(font);
		GridBagConstraints gbc_lblMaxProtrusionToCellSurfaceRatio = new GridBagConstraints();
		gbc_lblMaxProtrusionToCellSurfaceRatio.anchor = GridBagConstraints.EAST;
		gbc_lblMaxProtrusionToCellSurfaceRatio.insets = new Insets(0, 0, 5, 5);
		gbc_lblMaxProtrusionToCellSurfaceRatio.gridx = 1;
		gbc_lblMaxProtrusionToCellSurfaceRatio.gridy = 6;
		getContentPane().add(lblMaxProtrusionToCellSurfaceRatio, gbc_lblMaxProtrusionToCellSurfaceRatio);

		sliderMaxProtrusionToCellSurfaceRatio = new JSlider(0, 100,
				(int) (paramTemp.maxProtrusionToCellAreaRatio * 100.0));
		sliderMaxProtrusionToCellSurfaceRatio.setFont(font);
		GridBagConstraints gbc_sliderMaxProtrusionToCellSurfaceRatio = new GridBagConstraints();
		gbc_sliderMaxProtrusionToCellSurfaceRatio.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderMaxProtrusionToCellSurfaceRatio.insets = new Insets(0, 0, 5, 5);
		gbc_sliderMaxProtrusionToCellSurfaceRatio.gridx = 2;
		gbc_sliderMaxProtrusionToCellSurfaceRatio.gridy = 6;
		sliderMaxProtrusionToCellSurfaceRatio.addChangeListener(this);
		getContentPane().add(sliderMaxProtrusionToCellSurfaceRatio, gbc_sliderMaxProtrusionToCellSurfaceRatio);

		txtMaxProtrusionToCellSurfaceRatio = new JTextField(IJ.d2s(paramTemp.maxProtrusionToCellAreaRatio * 100.0, 0));
		txtMaxProtrusionToCellSurfaceRatio.setFont(font);
		GridBagConstraints gbc_txtMaxProtrusionToCellSurfaceRatio = new GridBagConstraints();
		gbc_txtMaxProtrusionToCellSurfaceRatio.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMaxProtrusionToCellSurfaceRatio.insets = new Insets(0, 0, 5, 5);
		gbc_txtMaxProtrusionToCellSurfaceRatio.gridx = 3;
		gbc_txtMaxProtrusionToCellSurfaceRatio.gridy = 6;
		txtMaxProtrusionToCellSurfaceRatio.addActionListener(this);
		txtMaxProtrusionToCellSurfaceRatio.addFocusListener(this);
		getContentPane().add(txtMaxProtrusionToCellSurfaceRatio, gbc_txtMaxProtrusionToCellSurfaceRatio);

		lblSmoothingWdw = new JLabel("Smoothing window (pixel):");
		lblSmoothingWdw.setFont(font);
		GridBagConstraints gbc_lblSmoothingWdw = new GridBagConstraints();
		gbc_lblSmoothingWdw.anchor = GridBagConstraints.EAST;
		gbc_lblSmoothingWdw.insets = new Insets(0, 0, 5, 5);
		gbc_lblSmoothingWdw.gridx = 1;
		gbc_lblSmoothingWdw.gridy = 7;
		getContentPane().add(lblSmoothingWdw, gbc_lblSmoothingWdw);

		sliderSmoothingWdw = new JSlider(0, 20, paramTemp.smoothingCoeffInPixels);
		sliderSmoothingWdw.setFont(font);
		GridBagConstraints gbc_sliderSmoothingWdw = new GridBagConstraints();
		gbc_sliderSmoothingWdw.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderSmoothingWdw.insets = new Insets(0, 0, 5, 5);
		gbc_sliderSmoothingWdw.gridx = 2;
		gbc_sliderSmoothingWdw.gridy = 7;
		sliderSmoothingWdw.addChangeListener(this);
		getContentPane().add(sliderSmoothingWdw, gbc_sliderSmoothingWdw);

		txtSmoothingWdw = new JTextField(IJ.d2s(paramTemp.smoothingCoeffInPixels, 0));
		txtSmoothingWdw.setFont(font);
		GridBagConstraints gbc_txtSmoothingWdw = new GridBagConstraints();
		gbc_txtSmoothingWdw.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtSmoothingWdw.insets = new Insets(0, 0, 5, 5);
		gbc_txtSmoothingWdw.gridx = 3;
		gbc_txtSmoothingWdw.gridy = 7;
		txtSmoothingWdw.addActionListener(this);
		txtSmoothingWdw.addFocusListener(this);
		getContentPane().add(txtSmoothingWdw, gbc_txtSmoothingWdw);

		lblDetectUropods = new JLabel("Detect uropods?");
		lblDetectUropods.setFont(font);
		GridBagConstraints gbc_lblDetectUropods = new GridBagConstraints();
		gbc_lblDetectUropods.anchor = GridBagConstraints.EAST;
		gbc_lblDetectUropods.insets = new Insets(0, 0, 5, 5);
		gbc_lblDetectUropods.gridx = 1;
		gbc_lblDetectUropods.gridy = 8;
		getContentPane().add(lblDetectUropods, gbc_lblDetectUropods);

		chckbxDetectUropods = new JCheckBox();
		chckbxDetectUropods.setFont(font);
		chckbxDetectUropods.setSelected(paramTemp.detectUropod);
		GridBagConstraints gbc_chckbxDetectUropods = new GridBagConstraints();
		gbc_chckbxDetectUropods.anchor = GridBagConstraints.WEST;
		gbc_chckbxDetectUropods.gridwidth = 2;
		gbc_chckbxDetectUropods.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxDetectUropods.gridx = 2;
		gbc_chckbxDetectUropods.gridy = 8;
		chckbxDetectUropods.addActionListener(this);
		getContentPane().add(chckbxDetectUropods, gbc_chckbxDetectUropods);

		lblInteractiveRejectionOfCells = new JLabel("Interactive rejection of cells:");
		lblInteractiveRejectionOfCells.setFont(font);
		GridBagConstraints gbc_lblInteractiveRejectionOfCells = new GridBagConstraints();
		gbc_lblInteractiveRejectionOfCells.gridwidth = 3;
		gbc_lblInteractiveRejectionOfCells.insets = new Insets(0, 0, 5, 5);
		gbc_lblInteractiveRejectionOfCells.gridx = 1;
		gbc_lblInteractiveRejectionOfCells.gridy = 10;
		getContentPane().add(lblInteractiveRejectionOfCells, gbc_lblInteractiveRejectionOfCells);

		rdbtnNoRejection = new JRadioButton("No rejection");
		buttonGroup.add(rdbtnNoRejection);
		GridBagConstraints gbc_rdbtnNoRejection = new GridBagConstraints();
		gbc_rdbtnNoRejection.gridwidth = 2;
		gbc_rdbtnNoRejection.anchor = GridBagConstraints.WEST;
		gbc_rdbtnNoRejection.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnNoRejection.gridx = 1;
		gbc_rdbtnNoRejection.gridy = 11;
		if (!paramTemp.postRejectCellFrame && !paramTemp.postRejectWholeCell)
			rdbtnNoRejection.setSelected(true);
		rdbtnNoRejection.addActionListener(this);
		getContentPane().add(rdbtnNoRejection, gbc_rdbtnNoRejection);

		rdbtnRejectCellIn = new JRadioButton("Reject cell in a single frame?");
		buttonGroup.add(rdbtnRejectCellIn);
		GridBagConstraints gbc_rdbtnRejectCellIn = new GridBagConstraints();
		gbc_rdbtnRejectCellIn.gridwidth = 2;
		gbc_rdbtnRejectCellIn.anchor = GridBagConstraints.WEST;
		gbc_rdbtnRejectCellIn.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnRejectCellIn.gridx = 1;
		gbc_rdbtnRejectCellIn.gridy = 12;
		if (paramTemp.postRejectCellFrame && !paramTemp.postRejectWholeCell)
			rdbtnRejectCellIn.setSelected(true);
		rdbtnRejectCellIn.addActionListener(this);
		getContentPane().add(rdbtnRejectCellIn, gbc_rdbtnRejectCellIn);

		rdbtnRejectAWhole = new JRadioButton("Reject a whole cell trajectory?");
		buttonGroup.add(rdbtnRejectAWhole);
		GridBagConstraints gbc_rdbtnRejectAWhole = new GridBagConstraints();
		gbc_rdbtnRejectAWhole.gridwidth = 2;
		gbc_rdbtnRejectAWhole.anchor = GridBagConstraints.WEST;
		gbc_rdbtnRejectAWhole.insets = new Insets(0, 0, 5, 5);
		gbc_rdbtnRejectAWhole.gridx = 1;
		gbc_rdbtnRejectAWhole.gridy = 13;
		if (!paramTemp.postRejectCellFrame && paramTemp.postRejectWholeCell)
			rdbtnRejectAWhole.setSelected(true);
		rdbtnRejectAWhole.addActionListener(this);
		getContentPane().add(rdbtnRejectAWhole, gbc_rdbtnRejectAWhole);

		btnOk = new JButton("OK");
		btnOk.setFont(font);
		GridBagConstraints gbc_btnOk = new GridBagConstraints();
		gbc_btnOk.insets = new Insets(0, 0, 0, 5);
		gbc_btnOk.gridx = 1;
		gbc_btnOk.gridy = 14;
		btnOk.addActionListener(this);
		getContentPane().add(btnOk, gbc_btnOk);

		btnReset = new JButton("Reset");
		btnReset.setFont(font);
		GridBagConstraints gbc_btnReset = new GridBagConstraints();
		gbc_btnReset.insets = new Insets(0, 0, 0, 5);
		gbc_btnReset.gridx = 2;
		gbc_btnReset.gridy = 14;
		btnReset.addActionListener(this);
		getContentPane().add(btnReset, gbc_btnReset);

		btnCancel = new JButton("Cancel");
		btnCancel.setFont(font);
		GridBagConstraints gbc_btnCancel = new GridBagConstraints();
		gbc_btnCancel.insets = new Insets(0, 0, 0, 5);
		gbc_btnCancel.gridx = 3;
		gbc_btnCancel.gridy = 14;
		btnCancel.addActionListener(this);
		getContentPane().add(btnCancel, gbc_btnCancel);

		updateImage();

		this.setVisible(true);
	}

	public void run() {
		while (finished == RUNNING) {
			IJ.wait(1000);
		}
	}

	private void cancelAllRejections() {
		for (int i = 0; i < results.cellData.size(); i++) {
			CellDataR cell = results.cellData.get(i);
			cell.rejectCell(CellDataR.NOT_REJECTED);
			for (int j = 0; j < cell.getFrameNumber(); j++)
				cell.rejectFrame(j, CellDataR.NOT_REJECTED);
		}
	}

	private void disposeThis() {
		this.setVisible(false);
		this.removeKeyListener(this);
		sliderMinCurvProtrusions.removeChangeListener(this);
		txtMinCurvProtrusions.removeActionListener(this);
		txtMinCurvProtrusions.removeFocusListener(this);
		sliderMinLengthTraj.removeChangeListener(this);
		txtMinLengthTraj.removeActionListener(this);
		txtMinLengthTraj.removeFocusListener(this);
		sliderDramaticIncrease.removeChangeListener(this);
		txtDramaticIncrease.removeActionListener(this);
		txtDramaticIncrease.removeFocusListener(this);
		sliderMinAreaProtrusion.removeChangeListener(this);
		txtMinAreaProtrusion.removeActionListener(this);
		txtMinAreaProtrusion.removeFocusListener(this);
		sliderMaxProtrusionToCellSurfaceRatio.removeChangeListener(this);
		txtMaxProtrusionToCellSurfaceRatio.removeActionListener(this);
		txtMaxProtrusionToCellSurfaceRatio.removeFocusListener(this);
		sliderSmoothingWdw.removeChangeListener(this);
		txtSmoothingWdw.removeActionListener(this);
		txtSmoothingWdw.removeFocusListener(this);
		chckbxDetectUropods.removeChangeListener(this);
		rdbtnNoRejection.removeChangeListener(this);
		rdbtnRejectCellIn.removeChangeListener(this);
		rdbtnRejectAWhole.removeChangeListener(this);
		canvas_1.removeKeyListener(this);
		canvas_1.removeMouseListener(this);
		btnOk.removeChangeListener(this);
		btnReset.removeChangeListener(this);
		btnCancel.removeChangeListener(this);
		this.dispose();

		ImagePlus.removeImageListener(this);
		ImagePlus temp = IJ.getImage();
		if (temp != null && temp.getTitle().equalsIgnoreCase("Visualisation"))
			temp.close();
	}

	private boolean imageLock = false;

	private synchronized boolean aquireImageLock() {
		if (imageLock)
			return false;
		return imageLock = true;
	}

	private void updateImage() {
		if (aquireImageLock()) {
			// Run in a new thread to allow the GUI to continue updating
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// Continue while the parameter is changing
						boolean parametersChanged = true;
						while (parametersChanged) {
							// Store the parameters to be processed
							Params paramtemp = paramTemp.clone();
							// Do something with parameters
							updateAnalysis(paramtemp, true);
							// Check if the parameters have changed again
							parametersChanged = !paramtemp.compare(paramTemp);
						}
					} finally {
						// Ensure the running flag is reset
						imageLock = false;
					}
				}
			}).start();
		}
	}

	private void updateAnalysis(Params paramTemp, boolean previsualisation) {
		results = new Results(results.cellData, paramTemp);
		if (image.getOverlay() == null) {
			image.setOverlay(new Overlay());
			image.getOverlay().drawLabels(true);
			Analyzer.drawLabels(true);
			image.getOverlay().drawNames(true);
			image.getOverlay().drawBackgrounds(false);
			image.getOverlay().setLabelColor(Color.WHITE);
			image.getOverlay().setLabelFont(new Font("SansSerif", Font.BOLD, 18), false);
		} else
			image.getOverlay().clear();
		results.imp = image;
		results.stack = image.getStack();
		results.buildProtrusions(!previsualisation);
		image.setHideOverlay(false);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (paramTemp.postRejectCellFrame || paramTemp.postRejectWholeCell) {
			int x = canvas_1.offScreenX(e.getX());
			int y = canvas_1.offScreenY(e.getY());
			int frame = image.getCurrentSlice() - 1;
			for (int i = 0; i < results.cellData.size(); i++) {
				CellDataR cell = results.cellData.get(i);
				CellData cellD = cell.getCellData();
				if (cellD.getStartFrame() - 1 <= frame && frame <= cellD.getEndFrame() - 1
						&& cellD.getCellRegions()[frame].getPolygonRoi(cellD.getCellRegions()[frame].getMask())
								.contains(x, y)) {
					int cellNumber = findCellNumber(frame, x, y);
					if (paramTemp.postRejectWholeCell) {
						if (results.cells.get(cellNumber).rejectCell == CellDataR.REJECT_WHOLE_TRAJ) {
							IJ.log("Addition of cell #" + cellNumber + " for all frames.");
							cell.stopWholeCellRejection(frame);
						} else if (results.cells.get(cellNumber).rejectCell == CellDataR.NOT_REJECTED
								&& results.cells.get(cellNumber).cellFrame[frame].reject == CellDataR.NOT_REJECTED) {
							IJ.log("Suppression of cell #" + cellNumber + " for all frames.");
							cell.rejectFrame(frame, CellDataR.REJECT_WHOLE_TRAJ);
						} else {
							IJ.log("The cell #" + cellNumber + " in frame #" + (frame + 1) + " is already rejected ("
									+ (cell.isFrameRejected(frame) ? cell.whichRejectionInFrame(frame)
											: results.cells.get(cellNumber).rejectCell)
									+ ").");
						}
					} else if (paramTemp.postRejectCellFrame) {
						if (cell.whichRejectionInFrame(frame) == CellDataR.REJECT_MANUAL) {
							IJ.log("Addition of cell #" + cellNumber + " in frame #" + (frame + 1) + ".");
							cell.rejectFrame(frame, CellDataR.NOT_REJECTED);
						} else if (!cell.isFrameRejected(frame)
								&& results.cells.get(cellNumber).rejectCell == CellDataR.NOT_REJECTED) {
							IJ.log("Supression of cell #" + cellNumber + " in frame #" + (frame + 1) + ".");
							cell.rejectFrame(frame, CellDataR.REJECT_MANUAL);
						} else {
							IJ.log("The cell #" + cellNumber + " in frame #" + (frame + 1) + " is already rejected ("
									+ (cell.isFrameRejected(frame) ? cell.whichRejectionInFrame(frame)
											: results.cells.get(cellNumber).rejectCell)
									+ ").");
						}
					}
				}
			}
			updateAnalysis(paramTemp.clone(), true);
		}

	}

	private int findCellNumber(int frame, int x, int y) {
		for (int i = 0; i < results.cells.size(); i++) {
			Cell cell = results.cells.get(i);
			if (cell.startFrame <= frame && frame <= cell.endFrame && cell.cellFrame[frame].contains(x, y)) {
				return cell.cellNumber;
			}
		}
		return -1;
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	public void kill() {
		paramTemp = null;
		results.kill();
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
			frame = Math.min(frame + 1, image.getStackSize() - 1);
		} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
			frame = Math.max(frame - 1, 0);
		}
		updateImage();
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void focusGained(FocusEvent e) {
	}

	@Override
	public void focusLost(FocusEvent e) {
		Object source = e.getSource();
		somethingHappened(source);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		Object source = e.getSource();
		if (sliderMoveAllowed) {
			if (source == sliderMinCurvProtrusions) {
				paramTemp.curvatureMinLevel = -sliderMinCurvProtrusions.getValue();
				txtMinCurvProtrusions.setText(IJ.d2s(-paramTemp.curvatureMinLevel, 0));
				updateImage();
			} else if (source == sliderMinLengthTraj) {
				int temp = sliderMinLengthTraj.getValue();
				paramTemp.minTrajLength = temp;
				txtMinLengthTraj.setText(IJ.d2s(temp * paramTemp.frameLengthS, 3));
				updateImage();
			} else if (source == sliderDramaticIncrease) {
				paramTemp.dramaticAreaIncrease = sliderDramaticIncrease.getValue();
				txtDramaticIncrease.setText(IJ.d2s(paramTemp.dramaticAreaIncrease, 0));
				updateImage();
			} else if (source == sliderMinAreaProtrusion) {
				double temp = sliderMinAreaProtrusion.getValue() / 10.0;
				paramTemp.minAreaDetection = temp / paramTemp.getPixelSizeUmSquared();
				txtMinAreaProtrusion.setText(IJ.d2s(temp, 1));
				updateImage();
			} else if (source == sliderMaxProtrusionToCellSurfaceRatio) {
				double temp = sliderMaxProtrusionToCellSurfaceRatio.getValue();
				paramTemp.maxProtrusionToCellAreaRatio = temp / 100.0D;
				txtMaxProtrusionToCellSurfaceRatio.setText(IJ.d2s(temp, 0));
				updateImage();
			} else if (source == sliderSmoothingWdw) {
				double temp = sliderSmoothingWdw.getValue();
				paramTemp.smoothingCoeffInPixels = (int) temp;
				txtSmoothingWdw.setText(IJ.d2s(temp, 0));
				updateImage();
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		somethingHappened(source);
	}

	private boolean sliderMoveAllowed = true;

	private void somethingHappened(Object source) {
		if (source == txtMinCurvProtrusions) {
			paramTemp.curvatureMinLevel = -Double.parseDouble(txtMinCurvProtrusions.getText());
			sliderMoveAllowed = false;
			sliderMinCurvProtrusions.setValue((int) (-paramTemp.curvatureMinLevel));
			updateImage();
			sliderMoveAllowed = true;
		} else if (source == txtMinLengthTraj) {
			double temp = Double.parseDouble(txtMinLengthTraj.getText());
			paramTemp.minTrajLength = (int) (temp / paramTemp.frameLengthS);
			sliderMoveAllowed = false;
			sliderMinLengthTraj.setValue(paramTemp.minTrajLength);
			updateImage();
			sliderMoveAllowed = true;
		} else if (source == txtDramaticIncrease) {
			paramTemp.dramaticAreaIncrease = Double.parseDouble(txtDramaticIncrease.getText());
			sliderMoveAllowed = false;
			sliderDramaticIncrease.setValue((int) (paramTemp.dramaticAreaIncrease));
			updateImage();
			sliderMoveAllowed = true;
		} else if (source == txtMinAreaProtrusion) {
			double temp = Double.parseDouble(txtMinAreaProtrusion.getText());
			paramTemp.minAreaDetection = temp / paramTemp.getPixelSizeUmSquared();
			sliderMoveAllowed = false;
			sliderMinAreaProtrusion.setValue((int) (temp * 10.0));
			updateImage();
			sliderMoveAllowed = true;
		} else if (source == txtMaxProtrusionToCellSurfaceRatio) {
			double temp = Double.parseDouble(txtMaxProtrusionToCellSurfaceRatio.getText());
			paramTemp.maxProtrusionToCellAreaRatio = temp / 100.0D;
			sliderMoveAllowed = false;
			sliderMaxProtrusionToCellSurfaceRatio.setValue((int) temp);
			updateImage();
			sliderMoveAllowed = true;
		} else if (source == txtSmoothingWdw) {
			paramTemp.smoothingCoeffInPixels = (int) Double.parseDouble(txtSmoothingWdw.getText());
			sliderMoveAllowed = false;
			sliderSmoothingWdw.setValue(paramTemp.smoothingCoeffInPixels);
			updateImage();
			sliderMoveAllowed = true;
		} else if (source == chckbxDetectUropods) {
			paramTemp.detectUropod = chckbxDetectUropods.isSelected();
			updateImage();
		} else if (source == rdbtnNoRejection) {
			if (rdbtnNoRejection.isSelected()) {
				paramTemp.postRejectCellFrame = false;
				paramTemp.postRejectWholeCell = false;
			}
		} else if (source == rdbtnRejectCellIn) {
			if (rdbtnRejectCellIn.isSelected()) {
				paramTemp.postRejectCellFrame = true;
				paramTemp.postRejectWholeCell = false;
			}
		} else if (source == rdbtnRejectAWhole) {
			if (rdbtnRejectAWhole.isSelected()) {
				paramTemp.postRejectCellFrame = false;
				paramTemp.postRejectWholeCell = true;
			}
		} else if (source == btnOk) {
			updateAnalysis(paramTemp, false);
			params = paramTemp;
			disposeThis();
			RoiManager.getInstance().setVisible(false);
			finished = FINISHED;
		} else if (source == btnReset) {
			Params paramReset = new Params();
			paramTemp.curvatureMinLevel = paramReset.curvatureMinLevel;
			sliderMinCurvProtrusions.setValue((int) (-paramTemp.curvatureMinLevel));
			txtMinCurvProtrusions.setText(IJ.d2s(-paramTemp.curvatureMinLevel, 0));
			paramTemp.minTrajLength = paramReset.minTrajLength;
			sliderMinLengthTraj.setValue(paramTemp.minTrajLength);
			txtMinLengthTraj.setText(IJ.d2s(paramTemp.minTrajLength * paramTemp.frameLengthS, 3));
			paramTemp.dramaticAreaIncrease = paramReset.dramaticAreaIncrease;
			sliderDramaticIncrease.setValue((int) paramTemp.dramaticAreaIncrease);
			txtDramaticIncrease.setText(IJ.d2s(paramTemp.dramaticAreaIncrease, 0));
			paramTemp.minAreaDetection = paramReset.minAreaDetection;
			sliderMinAreaProtrusion
					.setValue((int) (paramTemp.minAreaDetection * paramTemp.getPixelSizeUmSquared() * 10.0));
			txtMinAreaProtrusion.setText(IJ.d2s(paramTemp.minAreaDetection * paramTemp.getPixelSizeUmSquared(), 1));
			paramTemp.maxProtrusionToCellAreaRatio = paramReset.maxProtrusionToCellAreaRatio;
			sliderMaxProtrusionToCellSurfaceRatio.setValue((int) (paramTemp.maxProtrusionToCellAreaRatio * 100.0));
			txtMaxProtrusionToCellSurfaceRatio.setText(IJ.d2s(paramTemp.maxProtrusionToCellAreaRatio * 100.0, 0));
			paramTemp.smoothingCoeffInPixels = paramReset.smoothingCoeffInPixels;
			sliderSmoothingWdw.setValue(paramTemp.smoothingCoeffInPixels);
			txtSmoothingWdw.setText(IJ.d2s(paramTemp.smoothingCoeffInPixels, 0));
			paramTemp.detectUropod = paramReset.detectUropod;
			chckbxDetectUropods.setSelected(paramTemp.detectUropod);
			paramTemp.postRejectCellFrame = paramReset.postRejectCellFrame;
			paramTemp.postRejectWholeCell = paramReset.postRejectWholeCell;
			if (!paramTemp.postRejectCellFrame && !paramTemp.postRejectWholeCell)
				rdbtnNoRejection.setSelected(true);
			else if (paramTemp.postRejectCellFrame && !paramTemp.postRejectWholeCell)
				rdbtnRejectCellIn.setSelected(true);
			else if (!paramTemp.postRejectCellFrame && paramTemp.postRejectWholeCell)
				rdbtnRejectAWhole.setSelected(true);
			cancelAllRejections();
			updateImage();
		} else if (source == btnCancel) {
			cancelAllRejections();
			disposeThis();
			finished = CANCEL;
			image.close();
		}
	}

	@Override
	public void imageClosed(ImagePlus imp) {
		if (imp == image)
			image.show();
	}

	@Override
	public void imageOpened(ImagePlus arg0) {
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
		if (imp == image) {
			frame = image.getCurrentSlice() - 1;
			updateImage();
		}
	}

}
