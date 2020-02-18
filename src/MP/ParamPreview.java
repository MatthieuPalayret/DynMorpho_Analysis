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
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import Cell.CellData;
import UserVariables.UserVariables;
import UtilClasses.GenUtils;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;

public class ParamPreview extends JFrame
		implements ActionListener, ImageListener, ChangeListener, FocusListener, KeyListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -216989449160564702L;
	protected Params params, paramTemp;
	protected ImagePlus image;
	protected ImageStack stack8bit;
	protected ImagePlus imageIni;
	protected int frame = 0;

	protected static final Font font = new Font("Segoe UI", Font.PLAIN, 13);
	protected JLabel lblGeneralParam;
	protected JLabel lblAdditionalTagTo;
	protected JTextField fieldAdditionalTag;
	protected JLabel lblPixelSizenm;
	protected JTextField fieldPixelSizenm;
	protected JLabel lblFrameLengths;
	protected JTextField fieldFrameLengths;
	protected JLabel lblParametersForAnalysis;
	protected JLabel lblAutomaticIntensityThreshold;
	protected JCheckBox chckbxAutomaticIntensityThreshold;
	protected JLabel lblContourIntensityThreshold;
	protected JSlider sliderContourIntensityThreshold;
	protected JTextField lblContourIntensityThreshold2;
	protected JLabel lblSmoothingContourCoefficient;
	protected JSlider sliderSmoothingContourCoefficient;
	protected JTextField lblSmoothingContourCoefficient2;
	protected JLabel lblMinimalAreaOf;
	protected JSlider sliderMinimalAreaOf;
	protected JTextField lblMinimalAreaOf2;
	protected JLabel lblMaximalAreaOf;
	protected JSlider sliderMaximalAreaOf;
	protected JTextField labelMaximalAreaOf2;
	protected ij.gui.ImageCanvas canvas;
	protected JButton btnOk;
	protected JButton btnReset;
	protected JButton btnCancel;

	final static int CANCEL = 1;
	final static int FINISHED = 2;
	final static int RUNNING = 0;
	int finished = RUNNING;

	public ParamPreview(String title) {
		super(title);
	}

	public ParamPreview(Params params, ImagePlus img) {
		super("Parameter preview...");

		this.params = params;
		paramTemp = params.clone();
		imageIni = img;
		stack8bit = GenUtils.convertStack(img.getImageStack(), 8); // TODO
		image = new ImagePlus("Previsualisation", GenUtils.convertStack(img, 32).getImageStack());
		frame = image.getCurrentSlice() - 1;

		image.updateAndDraw();
		image.show();
		image.getWindow().setLocation(505, 20);
		canvas = image.getCanvas();
		ImagePlus.addImageListener(this);

		this.setBounds(20, 20, 500, 350);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 139, 0, 53, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0,
				Double.MIN_VALUE };
		getContentPane().setLayout(gridBagLayout);

		addKeyListener(this);

		lblGeneralParam = new JLabel("General parameters:");
		lblGeneralParam.setFont(font);
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.gridwidth = 3;
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbc_lblNewLabel.gridx = 1;
		gbc_lblNewLabel.gridy = 1;
		getContentPane().add(lblGeneralParam, gbc_lblNewLabel);

		lblAdditionalTagTo = new JLabel("Additional tag to the name:");
		lblAdditionalTagTo.setFont(font);
		GridBagConstraints gbc_lblAdditionalTagTo = new GridBagConstraints();
		gbc_lblAdditionalTagTo.anchor = GridBagConstraints.EAST;
		gbc_lblAdditionalTagTo.insets = new Insets(0, 0, 5, 5);
		gbc_lblAdditionalTagTo.gridx = 1;
		gbc_lblAdditionalTagTo.gridy = 2;
		getContentPane().add(lblAdditionalTagTo, gbc_lblAdditionalTagTo);

		fieldAdditionalTag = new JTextField();
		fieldAdditionalTag.setFont(font);
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.gridwidth = 2;
		gbc_textField.insets = new Insets(0, 0, 5, 5);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 2;
		gbc_textField.gridy = 2;
		getContentPane().add(fieldAdditionalTag, gbc_textField);
		fieldAdditionalTag.addActionListener(this);
		fieldAdditionalTag.addFocusListener(this);
		fieldAdditionalTag.setColumns(10);

		lblPixelSizenm = new JLabel("Pixel size (nm):");
		lblPixelSizenm.setFont(font);
		GridBagConstraints gbc_lblPixelSizenm = new GridBagConstraints();
		gbc_lblPixelSizenm.anchor = GridBagConstraints.EAST;
		gbc_lblPixelSizenm.insets = new Insets(0, 0, 5, 5);
		gbc_lblPixelSizenm.gridx = 1;
		gbc_lblPixelSizenm.gridy = 3;
		getContentPane().add(lblPixelSizenm, gbc_lblPixelSizenm);

		fieldPixelSizenm = new JTextField(IJ.d2s(paramTemp.pixelSizeNm, 1));
		fieldPixelSizenm.setFont(font);
		GridBagConstraints gbc_textField_1 = new GridBagConstraints();
		gbc_textField_1.gridwidth = 2;
		gbc_textField_1.insets = new Insets(0, 0, 5, 5);
		gbc_textField_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_1.gridx = 2;
		gbc_textField_1.gridy = 3;
		getContentPane().add(fieldPixelSizenm, gbc_textField_1);
		fieldPixelSizenm.addActionListener(this);
		fieldPixelSizenm.addFocusListener(this);
		fieldPixelSizenm.setColumns(10);

		lblFrameLengths = new JLabel("Frame length (s):");
		lblFrameLengths.setFont(font);
		GridBagConstraints gbc_lblFrameLengths = new GridBagConstraints();
		gbc_lblFrameLengths.anchor = GridBagConstraints.EAST;
		gbc_lblFrameLengths.insets = new Insets(0, 0, 5, 5);
		gbc_lblFrameLengths.gridx = 1;
		gbc_lblFrameLengths.gridy = 4;
		getContentPane().add(lblFrameLengths, gbc_lblFrameLengths);

		fieldFrameLengths = new JTextField(IJ.d2s(paramTemp.frameLengthS, 2));
		fieldFrameLengths.setFont(font);
		GridBagConstraints gbc_textField_2 = new GridBagConstraints();
		gbc_textField_2.gridwidth = 2;
		gbc_textField_2.insets = new Insets(0, 0, 5, 5);
		gbc_textField_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_2.gridx = 2;
		gbc_textField_2.gridy = 4;
		getContentPane().add(fieldFrameLengths, gbc_textField_2);
		fieldFrameLengths.addActionListener(this);
		fieldFrameLengths.addFocusListener(this);
		fieldFrameLengths.setColumns(10);

		lblParametersForAnalysis = new JLabel("Parameters for analysis:");
		lblParametersForAnalysis.setFont(font);
		GridBagConstraints gbc_lblParametersForAnalysis = new GridBagConstraints();
		gbc_lblParametersForAnalysis.gridwidth = 2;
		gbc_lblParametersForAnalysis.insets = new Insets(0, 0, 5, 5);
		gbc_lblParametersForAnalysis.gridx = 1;
		gbc_lblParametersForAnalysis.gridy = 6;
		getContentPane().add(lblParametersForAnalysis, gbc_lblParametersForAnalysis);

		lblAutomaticIntensityThreshold = new JLabel("Automatic intensity threshold?");
		lblAutomaticIntensityThreshold.setFont(font);
		GridBagConstraints gbc_lblAutomaticIntensityThreshold = new GridBagConstraints();
		gbc_lblAutomaticIntensityThreshold.anchor = GridBagConstraints.EAST;
		gbc_lblAutomaticIntensityThreshold.insets = new Insets(0, 0, 5, 5);
		gbc_lblAutomaticIntensityThreshold.gridx = 1;
		gbc_lblAutomaticIntensityThreshold.gridy = 7;
		getContentPane().add(lblAutomaticIntensityThreshold, gbc_lblAutomaticIntensityThreshold);

		chckbxAutomaticIntensityThreshold = new JCheckBox();
		chckbxAutomaticIntensityThreshold.setFont(font);
		chckbxAutomaticIntensityThreshold.setSelected(paramTemp.autoThreshold);
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.WEST;
		gbc_chckbxNewCheckBox.gridwidth = 2;
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox.gridx = 2;
		gbc_chckbxNewCheckBox.gridy = 7;
		chckbxAutomaticIntensityThreshold.addActionListener(this);
		getContentPane().add(chckbxAutomaticIntensityThreshold, gbc_chckbxNewCheckBox);

		lblContourIntensityThreshold = new JLabel("Contour intensity threshold [0 .. 255]:");
		lblContourIntensityThreshold.setFont(font);
		GridBagConstraints gbc_lblContourIntensityThreshold = new GridBagConstraints();
		gbc_lblContourIntensityThreshold.anchor = GridBagConstraints.EAST;
		gbc_lblContourIntensityThreshold.insets = new Insets(0, 0, 5, 5);
		gbc_lblContourIntensityThreshold.gridx = 1;
		gbc_lblContourIntensityThreshold.gridy = 8;
		getContentPane().add(lblContourIntensityThreshold, gbc_lblContourIntensityThreshold);

		sliderContourIntensityThreshold = new JSlider(0, 255, paramTemp.greyThreshold);
		sliderContourIntensityThreshold.setFont(font);
		GridBagConstraints gbc_slider = new GridBagConstraints();
		gbc_slider.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider.insets = new Insets(0, 0, 5, 5);
		gbc_slider.gridx = 2;
		gbc_slider.gridy = 8;
		sliderContourIntensityThreshold.addChangeListener(this);
		getContentPane().add(sliderContourIntensityThreshold, gbc_slider);

		// TODO (int) (new StackStatistics(image).max + 0.5)
		lblContourIntensityThreshold2 = new JTextField(IJ.d2s(sliderContourIntensityThreshold.getValue(), 0));
		lblContourIntensityThreshold2.setFont(font);
		GridBagConstraints gbc_label = new GridBagConstraints();
		gbc_label.fill = GridBagConstraints.HORIZONTAL;
		gbc_label.insets = new Insets(0, 0, 5, 5);
		gbc_label.gridx = 3;
		gbc_label.gridy = 8;
		lblContourIntensityThreshold2.addActionListener(this);
		lblContourIntensityThreshold2.addFocusListener(this);
		getContentPane().add(lblContourIntensityThreshold2, gbc_label);

		lblSmoothingContourCoefficient = new JLabel("Smoothing contour coefficient:");
		lblSmoothingContourCoefficient.setFont(font);
		GridBagConstraints gbc_lblSmoothingContourCoefficient = new GridBagConstraints();
		gbc_lblSmoothingContourCoefficient.anchor = GridBagConstraints.EAST;
		gbc_lblSmoothingContourCoefficient.insets = new Insets(0, 0, 5, 5);
		gbc_lblSmoothingContourCoefficient.gridx = 1;
		gbc_lblSmoothingContourCoefficient.gridy = 9;
		getContentPane().add(lblSmoothingContourCoefficient, gbc_lblSmoothingContourCoefficient);

		sliderSmoothingContourCoefficient = new JSlider();
		sliderSmoothingContourCoefficient.setFont(font);
		sliderSmoothingContourCoefficient.setValue((int) ((paramTemp.smoothingContour) / 5.0 * 100.0));
		GridBagConstraints gbc_slider_1 = new GridBagConstraints();
		gbc_slider_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_1.insets = new Insets(0, 0, 5, 5);
		gbc_slider_1.gridx = 2;
		gbc_slider_1.gridy = 9;
		sliderSmoothingContourCoefficient.addChangeListener(this);
		getContentPane().add(sliderSmoothingContourCoefficient, gbc_slider_1);

		lblSmoothingContourCoefficient2 = new JTextField(
				IJ.d2s((sliderSmoothingContourCoefficient.getValue()) / 100.0D * 5.0D, 3));
		lblSmoothingContourCoefficient2.setFont(font);
		GridBagConstraints gbc_label_1 = new GridBagConstraints();
		gbc_label_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_label_1.insets = new Insets(0, 0, 5, 5);
		gbc_label_1.gridx = 3;
		gbc_label_1.gridy = 9;
		lblSmoothingContourCoefficient2.addActionListener(this);
		lblSmoothingContourCoefficient2.addFocusListener(this);
		getContentPane().add(lblSmoothingContourCoefficient2, gbc_label_1);

		lblMinimalAreaOf = new JLabel("Minimal area of a cell (\u00B5m\u00B2):");
		lblMinimalAreaOf.setFont(font);
		GridBagConstraints gbc_lblMinimalAreaOf = new GridBagConstraints();
		gbc_lblMinimalAreaOf.anchor = GridBagConstraints.EAST;
		gbc_lblMinimalAreaOf.insets = new Insets(0, 0, 5, 5);
		gbc_lblMinimalAreaOf.gridx = 1;
		gbc_lblMinimalAreaOf.gridy = 10;
		getContentPane().add(lblMinimalAreaOf, gbc_lblMinimalAreaOf);

		sliderMinimalAreaOf = new JSlider();
		sliderMinimalAreaOf.setFont(font);
		sliderMinimalAreaOf.setValue(20);
		GridBagConstraints gbc_slider_2 = new GridBagConstraints();
		gbc_slider_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_2.insets = new Insets(0, 0, 5, 5);
		gbc_slider_2.gridx = 2;
		gbc_slider_2.gridy = 10;
		sliderMinimalAreaOf.addChangeListener(this);
		getContentPane().add(sliderMinimalAreaOf, gbc_slider_2);

		lblMinimalAreaOf2 = new JTextField(IJ.d2s(paramTemp.minCellSurface * paramTemp.getPixelSizeUmSquared(), 1));
		lblMinimalAreaOf2.setFont(font);
		GridBagConstraints gbc_label_2 = new GridBagConstraints();
		gbc_label_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_label_2.insets = new Insets(0, 0, 5, 5);
		gbc_label_2.gridx = 3;
		gbc_label_2.gridy = 10;
		lblMinimalAreaOf2.addActionListener(this);
		lblMinimalAreaOf2.addFocusListener(this);
		getContentPane().add(lblMinimalAreaOf2, gbc_label_2);

		lblMaximalAreaOf = new JLabel("Maximal area of a cell (\u00B5m\u00B2):");
		lblMaximalAreaOf.setFont(font);
		GridBagConstraints gbc_lblMaximalAreaOf = new GridBagConstraints();
		gbc_lblMaximalAreaOf.anchor = GridBagConstraints.EAST;
		gbc_lblMaximalAreaOf.insets = new Insets(0, 0, 5, 5);
		gbc_lblMaximalAreaOf.gridx = 1;
		gbc_lblMaximalAreaOf.gridy = 11;
		getContentPane().add(lblMaximalAreaOf, gbc_lblMaximalAreaOf);

		sliderMaximalAreaOf = new JSlider();
		sliderMaximalAreaOf.setFont(font);
		sliderMaximalAreaOf.setValue(10);
		GridBagConstraints gbc_slider_3 = new GridBagConstraints();
		gbc_slider_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_3.insets = new Insets(0, 0, 5, 5);
		gbc_slider_3.gridx = 2;
		gbc_slider_3.gridy = 11;
		sliderMaximalAreaOf.addChangeListener(this);
		getContentPane().add(sliderMaximalAreaOf, gbc_slider_3);

		labelMaximalAreaOf2 = new JTextField(IJ.d2s(paramTemp.maxCellSurface * paramTemp.getPixelSizeUmSquared(), 0));
		labelMaximalAreaOf2.setFont(font);
		GridBagConstraints gbc_label_3 = new GridBagConstraints();
		gbc_label_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_label_3.insets = new Insets(0, 0, 5, 5);
		gbc_label_3.gridx = 3;
		gbc_label_3.gridy = 11;
		labelMaximalAreaOf2.addActionListener(this);
		labelMaximalAreaOf2.addFocusListener(this);
		getContentPane().add(labelMaximalAreaOf2, gbc_label_3);

		btnOk = new JButton("OK");
		btnOk.setFont(font);
		GridBagConstraints gbc_btnOk = new GridBagConstraints();
		gbc_btnOk.insets = new Insets(0, 0, 0, 5);
		gbc_btnOk.gridx = 1;
		gbc_btnOk.gridy = 13;
		btnOk.addActionListener(this);
		getContentPane().add(btnOk, gbc_btnOk);

		btnReset = new JButton("Reset");
		btnReset.setFont(font);
		GridBagConstraints gbc_btnReset = new GridBagConstraints();
		gbc_btnReset.insets = new Insets(0, 0, 0, 5);
		gbc_btnReset.gridx = 2;
		gbc_btnReset.gridy = 13;
		btnReset.addActionListener(this);
		getContentPane().add(btnReset, gbc_btnReset);

		btnCancel = new JButton("Cancel");
		btnCancel.setFont(font);
		GridBagConstraints gbc_btnCancel = new GridBagConstraints();
		gbc_btnCancel.insets = new Insets(0, 0, 0, 5);
		gbc_btnCancel.gridx = 3;
		gbc_btnCancel.gridy = 13;
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

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		somethingHappened(source);
	}

	@Override
	public void focusGained(FocusEvent e) {

	}

	@Override
	public void focusLost(FocusEvent e) {
		Object source = e.getSource();
		somethingHappened(source);
	}

	protected boolean sliderMoveAllowed = true;

	protected void somethingHappened(Object source) {
		if (source == fieldAdditionalTag) {
			paramTemp.tagName = fieldAdditionalTag.getText();
		} else if (source == fieldPixelSizenm) {
			paramTemp.pixelSizeNm = Double.parseDouble(fieldPixelSizenm.getText());
		} else if (source == fieldFrameLengths) {
			paramTemp.frameLengthS = Double.parseDouble(fieldFrameLengths.getText());
		} else if (source == chckbxAutomaticIntensityThreshold) {
			paramTemp.autoThreshold = chckbxAutomaticIntensityThreshold.isSelected();
			updateImage();
		} else if (source == lblContourIntensityThreshold2) {
			paramTemp.greyThreshold = (int) Double.parseDouble(lblContourIntensityThreshold2.getText());
			sliderMoveAllowed = false;
			sliderContourIntensityThreshold.setValue(paramTemp.greyThreshold);
			updateImage();
			sliderMoveAllowed = true;
		} else if (source == lblSmoothingContourCoefficient2) {
			paramTemp.smoothingContour = Double.parseDouble(lblSmoothingContourCoefficient2.getText());
			sliderMoveAllowed = false;
			sliderSmoothingContourCoefficient.setValue((int) (paramTemp.smoothingContour * 100.0 / 5.0D));
			updateImage();
			sliderMoveAllowed = true;
		} else if (source == lblMinimalAreaOf2) {
			paramTemp.minCellSurface = Double.parseDouble(lblMinimalAreaOf2.getText())
					/ paramTemp.getPixelSizeUmSquared();
			sliderMoveAllowed = false;
			sliderMinimalAreaOf.setValue((int) (paramTemp.minCellSurface * 100.0 / (5.0D * params.minCellSurface)));
			updateImage();
			sliderMoveAllowed = true;
		} else if (source == labelMaximalAreaOf2) {
			paramTemp.maxCellSurface = Double.parseDouble(labelMaximalAreaOf2.getText())
					/ paramTemp.getPixelSizeUmSquared();
			sliderMoveAllowed = false;
			sliderMaximalAreaOf.setValue((int) (paramTemp.maxCellSurface * 100.0 / (10.0D * params.maxCellSurface)));
			updateImage();
			sliderMoveAllowed = true;
		} else if (source == btnOk) {
			params = paramTemp;
			ImagePlus.removeImageListener(this);
			image.close();
			imageIni.show();
			disposeThis();
			finished = FINISHED;
		} else if (source == btnReset) {
			paramTemp = new Params();
			fieldPixelSizenm.setText(IJ.d2s(paramTemp.pixelSizeNm, 1));
			fieldFrameLengths.setText(IJ.d2s(paramTemp.frameLengthS, 2));
			chckbxAutomaticIntensityThreshold.setSelected(paramTemp.autoThreshold);
			sliderContourIntensityThreshold.setValue(paramTemp.greyThreshold);
			lblContourIntensityThreshold2.setText(IJ.d2s(sliderContourIntensityThreshold.getValue(), 0));
			sliderSmoothingContourCoefficient.setValue((int) ((paramTemp.smoothingContour) / 5.0 * 100.0));
			lblSmoothingContourCoefficient2
					.setText(IJ.d2s((sliderSmoothingContourCoefficient.getValue()) / 100.0D * 5.0D, 3));
			sliderMinimalAreaOf.setValue(20);
			lblMinimalAreaOf2.setText(IJ.d2s(paramTemp.minCellSurface * paramTemp.getPixelSizeUmSquared(), 1));
			sliderMaximalAreaOf.setValue(10);
			labelMaximalAreaOf2.setText(IJ.d2s(paramTemp.maxCellSurface * paramTemp.getPixelSizeUmSquared(), 0));
			updateImage();
		} else if (source == btnCancel) {
			ImagePlus.removeImageListener(this);
			image.close();
			imageIni.show();
			disposeThis();
			finished = CANCEL;
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		Object source = e.getSource();
		if (sliderMoveAllowed) {
			if (source == sliderContourIntensityThreshold) {
				paramTemp.greyThreshold = sliderContourIntensityThreshold.getValue();
				lblContourIntensityThreshold2.setText(IJ.d2s(paramTemp.greyThreshold, 0));
				updateImage();
			} else if (source == sliderSmoothingContourCoefficient) {
				paramTemp.smoothingContour = sliderSmoothingContourCoefficient.getValue() / 100.0D * 5.0D;
				lblSmoothingContourCoefficient2.setText(IJ.d2s(paramTemp.smoothingContour, 3));
				updateImage();
			} else if (source == sliderMinimalAreaOf) {
				paramTemp.minCellSurface = sliderMinimalAreaOf.getValue() / 100.0D * 5.0D * params.minCellSurface;
				lblMinimalAreaOf2.setText(IJ.d2s(paramTemp.minCellSurface * paramTemp.getPixelSizeUmSquared(), 1));
				updateImage();
			} else if (source == sliderMaximalAreaOf) {
				paramTemp.maxCellSurface = sliderMaximalAreaOf.getValue() / 100.0D * 10.0D * params.maxCellSurface;
				labelMaximalAreaOf2.setText(IJ.d2s(paramTemp.maxCellSurface * paramTemp.getPixelSizeUmSquared(), 0));
				updateImage();
			}
		}
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
		} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
			btnOk.setSelected(true);
		} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			btnCancel.setSelected(true);
		}
		updateImage();
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	protected void disposeThis() {
		this.setVisible(false);
		this.removeKeyListener(this);
		fieldAdditionalTag.removeActionListener(this);
		fieldAdditionalTag.removeFocusListener(this);
		fieldPixelSizenm.removeActionListener(this);
		fieldPixelSizenm.removeFocusListener(this);
		fieldFrameLengths.removeActionListener(this);
		fieldFrameLengths.removeFocusListener(this);
		chckbxAutomaticIntensityThreshold.removeActionListener(this);
		chckbxAutomaticIntensityThreshold.removeFocusListener(this);
		sliderContourIntensityThreshold.removeChangeListener(this);
		lblContourIntensityThreshold2.removeActionListener(this);
		lblContourIntensityThreshold2.removeFocusListener(this);
		sliderSmoothingContourCoefficient.removeChangeListener(this);
		lblSmoothingContourCoefficient2.removeActionListener(this);
		lblSmoothingContourCoefficient2.removeFocusListener(this);
		sliderMinimalAreaOf.removeChangeListener(this);
		lblMinimalAreaOf2.removeActionListener(this);
		lblMinimalAreaOf2.removeFocusListener(this);
		sliderMaximalAreaOf.removeChangeListener(this);
		labelMaximalAreaOf2.removeActionListener(this);
		labelMaximalAreaOf2.removeFocusListener(this);
		canvas.removeKeyListener(this);
		btnOk.removeChangeListener(this);
		btnReset.removeChangeListener(this);
		btnCancel.removeChangeListener(this);
		this.dispose();
	}

	private boolean imageLock = false;

	private synchronized boolean aquireImageLock() {
		if (imageLock)
			return false;
		return imageLock = true;
	}

	protected void updateImage() {
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
							int frameTemp = frame;
							// Do something with parameters
							updateAnalysis(paramtemp, frame, stack8bit, image);
							// Check if the parameters have changed again
							parametersChanged = !paramtemp.compare(paramTemp) || !(frameTemp == frame);
						}
					} finally {
						// Ensure the running flag is reset
						updateView(frame);
						imageLock = false;
					}
				}
			}).start();
		}
	}

	static protected void updateAnalysis(Params paramTemp, int frame, ImageStack stack8bit, ImagePlus image) {
		UserVariables uv = paramTemp.getUV();
		uv = paramTemp.updateUV(uv);
		uv.setAnalyseProtrusions(true);

		ImagePlus bp = new ImagePlus("", new ByteProcessor(1, 1));
		new ImageWindow(bp).setVisible(false);

		AnalyseMovieMP previewAnalyser = new AnalyseMovieMP(new ImageStack[] { stack8bit, null }, false, false, uv,
				null, null);
		previewAnalyser.preparePreview(frame + 1, uv);
		previewAnalyser.doWork();
		ArrayList<CellData> cellData = previewAnalyser.getCellData();

		Overlay ov = null;
		ov = new Overlay();
		for (int i = 0; i < cellData.size(); i++) {
			double[] contourX = cellData.get(i).getCurveMap().getxCoords()[0];
			double[] contourY = cellData.get(i).getCurveMap().getyCoords()[0];
			PolygonRoi roi = new PolygonRoi(Utils.buildFloatPolygon(contourX, contourY), Roi.POLYGON);
			double area = Utils.area(roi.getFloatPolygon());

			if (area > paramTemp.minCellSurface && area < paramTemp.maxCellSurface) {
				roi.setStrokeColor(Color.BLUE);
			} else {
				roi.setStrokeColor(Color.BLUE.darker().darker());
			}
			roi.setStrokeWidth(1.5);
			roi.setPosition(frame + 1);
			ov.add(roi);
			image.setOverlay(ov);
		}
		image.setOverlay(ov);
	}

	protected void updateView(int frame) {
		image.setSliceWithoutUpdate(frame + 1);
		canvas.repaint();
		image.draw();
		image.setHideOverlay(false);
	}

	public void kill() {
		paramTemp = null;
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
		if (imp == image && imp.getCurrentSlice() - 1 != frame) {
			frame = imp.getCurrentSlice() - 1;
			updateImage();
		}
	}

}
