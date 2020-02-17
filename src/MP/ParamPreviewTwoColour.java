package MP;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;

import UtilClasses.GenUtils;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.Duplicator;
import ij.process.StackStatistics;

public class ParamPreviewTwoColour extends ParamPreview {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7032598160292949111L;
	protected ImagePlus image2;
	protected ImageStack stack8bit2;
	protected int channel1 = 1;
	protected int channel2 = 2;
	protected ij.gui.ImageCanvas canvas2;

	private JLabel lblChannels;
	private JComboBox<String> comboboxChannel1;
	private JComboBox<String> comboboxChannel2;
	private JSlider sliderContourIntensityThreshold2;
	private JTextField lblContourIntensityThreshold_2;

	public ParamPreviewTwoColour(Params params, ImagePlus img) {
		super("Parameter preview...");

		this.params = params;
		params.twoColourAnalysis = true;
		paramTemp = params.clone();
		imageIni = img;

		img.setC(channel1);
		ImagePlus impTemp1 = new Duplicator().run(img, channel1, channel1, 1, 1, 1, img.getNFrames());
		stack8bit = GenUtils.convertStack(impTemp1.getImageStack(), 8);
		image = new ImagePlus("Previsualisation - Red channel", GenUtils.convertStack(impTemp1, 32).getImageStack());
		frame = image.getCurrentSlice() - 1;
		ImagePlus impTemp2 = new Duplicator().run(img, channel2, channel2, 1, 1, 1, img.getNFrames());
		stack8bit2 = GenUtils.convertStack(impTemp2.getImageStack(), 8);
		image2 = new ImagePlus("Previsualisation - Green channel", GenUtils.convertStack(impTemp2, 32).getImageStack());
		image2.setSlice(frame + 1);

		image.updateAndDraw();
		image.show();
		image.getWindow().setLocation(505, 20);
		canvas = image.getCanvas();
		IJ.run("Enhance Contrast", "saturated=0.35");
		image2.updateAndDraw();
		image2.show();
		image2.getWindow().setLocation(1010, 20);
		canvas2 = image2.getCanvas();
		IJ.run("Enhance Contrast", "saturated=0.35");
		ImagePlus.addImageListener(this);

		this.setBounds(20, 20, 500, 400);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 139, 0, 53, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 1.0, 1.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 1.0, 0.0, Double.MIN_VALUE };
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

		lblChannels = new JLabel("Channels:");
		lblChannels.setFont(font);
		GridBagConstraints gbc_lblChannels = new GridBagConstraints();
		gbc_lblChannels.anchor = GridBagConstraints.EAST;
		gbc_lblChannels.insets = new Insets(0, 0, 5, 5);
		gbc_lblChannels.gridx = 1;
		gbc_lblChannels.gridy = 6;
		getContentPane().add(lblChannels, gbc_lblChannels);

		String[] channels = new String[imageIni.getNChannels()];
		for (int i = 0; i < channels.length; i++)
			channels[i] = IJ.d2s(i + 1);
		comboboxChannel1 = new JComboBox<String>(new DefaultComboBoxModel<String>(channels));
		comboboxChannel1.setSelectedIndex(channel1 - 1);
		comboboxChannel1.setFont(font);
		comboboxChannel1.setEditable(false);
		comboboxChannel1.setBackground(new Color(255, 99, 71));
		GridBagConstraints gbc_comboboxChannel1 = new GridBagConstraints();
		gbc_comboboxChannel1.insets = new Insets(0, 0, 5, 5);
		gbc_comboboxChannel1.gridx = 2;
		gbc_comboboxChannel1.gridy = 6;
		comboboxChannel1.addActionListener(this);
		getContentPane().add(comboboxChannel1, gbc_comboboxChannel1);

		comboboxChannel2 = new JComboBox<String>(new DefaultComboBoxModel<String>(channels));
		comboboxChannel2.setSelectedIndex(channel2 - 1);
		comboboxChannel2.setFont(font);
		comboboxChannel2.setEditable(false);
		comboboxChannel2.setBackground(new Color(144, 238, 144));
		GridBagConstraints gbc_comboboxChannel2 = new GridBagConstraints();
		gbc_comboboxChannel2.insets = new Insets(0, 0, 5, 5);
		gbc_comboboxChannel2.gridx = 3;
		gbc_comboboxChannel2.gridy = 6;
		comboboxChannel2.addActionListener(this);
		getContentPane().add(comboboxChannel2, gbc_comboboxChannel2);

		lblContourIntensityThreshold = new JLabel("Contour intensity threshold (photon counts):");
		lblContourIntensityThreshold.setFont(font);
		GridBagConstraints gbc_lblContourIntensityThreshold = new GridBagConstraints();
		gbc_lblContourIntensityThreshold.anchor = GridBagConstraints.EAST;
		gbc_lblContourIntensityThreshold.gridheight = 2;
		gbc_lblContourIntensityThreshold.insets = new Insets(0, 0, 5, 5);
		gbc_lblContourIntensityThreshold.gridx = 1;
		gbc_lblContourIntensityThreshold.gridy = 7;
		getContentPane().add(lblContourIntensityThreshold, gbc_lblContourIntensityThreshold);

		sliderContourIntensityThreshold = new JSlider(0, (int) (new StackStatistics(image).max + 0.5),
				paramTemp.greyThreshold);
		sliderContourIntensityThreshold.setFont(font);
		GridBagConstraints gbc_sliderContourIntensityThreshold = new GridBagConstraints();
		gbc_sliderContourIntensityThreshold.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderContourIntensityThreshold.insets = new Insets(0, 0, 5, 5);
		gbc_sliderContourIntensityThreshold.gridx = 2;
		gbc_sliderContourIntensityThreshold.gridy = 7;
		sliderContourIntensityThreshold.addChangeListener(this);
		getContentPane().add(sliderContourIntensityThreshold, gbc_sliderContourIntensityThreshold);

		sliderContourIntensityThreshold2 = new JSlider(0, (int) (new StackStatistics(image2).max + 0.5),
				paramTemp.greyThreshold2);
		sliderContourIntensityThreshold2.setFont(font);
		GridBagConstraints gbc_sliderContourIntensityThreshold2 = new GridBagConstraints();
		gbc_sliderContourIntensityThreshold2.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderContourIntensityThreshold2.insets = new Insets(0, 0, 5, 5);
		gbc_sliderContourIntensityThreshold2.gridx = 3;
		gbc_sliderContourIntensityThreshold2.gridy = 7;
		sliderContourIntensityThreshold2.addChangeListener(this);
		getContentPane().add(sliderContourIntensityThreshold2, gbc_sliderContourIntensityThreshold2);

		lblContourIntensityThreshold2 = new JTextField(IJ.d2s(sliderContourIntensityThreshold.getValue(), 0));
		lblContourIntensityThreshold2.setFont(font);
		GridBagConstraints gbc_lblContourIntensityThreshold2 = new GridBagConstraints();
		gbc_lblContourIntensityThreshold2.insets = new Insets(0, 0, 5, 5);
		gbc_lblContourIntensityThreshold2.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblContourIntensityThreshold2.gridx = 2;
		gbc_lblContourIntensityThreshold2.gridy = 8;
		lblContourIntensityThreshold2.addActionListener(this);
		lblContourIntensityThreshold2.addFocusListener(this);
		getContentPane().add(lblContourIntensityThreshold2, gbc_lblContourIntensityThreshold2);
		lblContourIntensityThreshold2.setColumns(10);

		lblContourIntensityThreshold_2 = new JTextField(IJ.d2s(sliderContourIntensityThreshold2.getValue(), 0));
		lblContourIntensityThreshold_2.setFont(font);
		GridBagConstraints gbc_lblContourIntensityThreshold2_2 = new GridBagConstraints();
		gbc_lblContourIntensityThreshold2_2.insets = new Insets(0, 0, 5, 5);
		gbc_lblContourIntensityThreshold2_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblContourIntensityThreshold2_2.gridx = 3;
		gbc_lblContourIntensityThreshold2_2.gridy = 8;
		lblContourIntensityThreshold_2.addActionListener(this);
		lblContourIntensityThreshold_2.addFocusListener(this);
		getContentPane().add(lblContourIntensityThreshold_2, gbc_lblContourIntensityThreshold2_2);
		lblContourIntensityThreshold_2.setColumns(10);

		lblParametersForAnalysis = new JLabel("Parameters for analysis:");
		lblParametersForAnalysis.setFont(font);
		GridBagConstraints gbc_lblParametersForAnalysis = new GridBagConstraints();
		gbc_lblParametersForAnalysis.gridwidth = 2;
		gbc_lblParametersForAnalysis.insets = new Insets(0, 0, 5, 5);
		gbc_lblParametersForAnalysis.gridx = 1;
		gbc_lblParametersForAnalysis.gridy = 10;
		getContentPane().add(lblParametersForAnalysis, gbc_lblParametersForAnalysis);

		lblAutomaticIntensityThreshold = new JLabel("Automatic intensity threshold?");
		lblAutomaticIntensityThreshold.setFont(font);
		GridBagConstraints gbc_lblAutomaticIntensityThreshold = new GridBagConstraints();
		gbc_lblAutomaticIntensityThreshold.anchor = GridBagConstraints.EAST;
		gbc_lblAutomaticIntensityThreshold.insets = new Insets(0, 0, 5, 5);
		gbc_lblAutomaticIntensityThreshold.gridx = 1;
		gbc_lblAutomaticIntensityThreshold.gridy = 11;
		getContentPane().add(lblAutomaticIntensityThreshold, gbc_lblAutomaticIntensityThreshold);

		chckbxAutomaticIntensityThreshold = new JCheckBox();
		chckbxAutomaticIntensityThreshold.setFont(font);
		chckbxAutomaticIntensityThreshold.setSelected(paramTemp.autoThreshold);
		GridBagConstraints gbc_chckbxNewCheckBox = new GridBagConstraints();
		gbc_chckbxNewCheckBox.anchor = GridBagConstraints.WEST;
		gbc_chckbxNewCheckBox.gridwidth = 2;
		gbc_chckbxNewCheckBox.insets = new Insets(0, 0, 5, 5);
		gbc_chckbxNewCheckBox.gridx = 2;
		gbc_chckbxNewCheckBox.gridy = 11;
		chckbxAutomaticIntensityThreshold.addActionListener(this);
		getContentPane().add(chckbxAutomaticIntensityThreshold, gbc_chckbxNewCheckBox);

		lblSmoothingContourCoefficient = new JLabel("Smoothing contour coefficient:");
		lblSmoothingContourCoefficient.setFont(font);
		GridBagConstraints gbc_lblSmoothingContourCoefficient = new GridBagConstraints();
		gbc_lblSmoothingContourCoefficient.anchor = GridBagConstraints.EAST;
		gbc_lblSmoothingContourCoefficient.insets = new Insets(0, 0, 5, 5);
		gbc_lblSmoothingContourCoefficient.gridx = 1;
		gbc_lblSmoothingContourCoefficient.gridy = 12;
		getContentPane().add(lblSmoothingContourCoefficient, gbc_lblSmoothingContourCoefficient);

		sliderSmoothingContourCoefficient = new JSlider();
		sliderSmoothingContourCoefficient.setFont(font);
		sliderSmoothingContourCoefficient.setValue((int) ((paramTemp.smoothingContour) / 5.0 * 100.0));
		GridBagConstraints gbc_slider_1 = new GridBagConstraints();
		gbc_slider_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_1.insets = new Insets(0, 0, 5, 5);
		gbc_slider_1.gridx = 2;
		gbc_slider_1.gridy = 12;
		sliderSmoothingContourCoefficient.addChangeListener(this);
		getContentPane().add(sliderSmoothingContourCoefficient, gbc_slider_1);

		lblSmoothingContourCoefficient2 = new JTextField(
				IJ.d2s((sliderSmoothingContourCoefficient.getValue()) / 100.0D * 5.0D, 3));
		lblSmoothingContourCoefficient2.setFont(font);
		GridBagConstraints gbc_label_1 = new GridBagConstraints();
		gbc_label_1.fill = GridBagConstraints.HORIZONTAL;
		gbc_label_1.insets = new Insets(0, 0, 5, 5);
		gbc_label_1.gridx = 3;
		gbc_label_1.gridy = 12;
		lblSmoothingContourCoefficient2.addActionListener(this);
		lblSmoothingContourCoefficient2.addFocusListener(this);
		getContentPane().add(lblSmoothingContourCoefficient2, gbc_label_1);

		lblMinimalAreaOf = new JLabel("Minimal area of a cell (\u00B5m\u00B2):");
		lblMinimalAreaOf.setFont(font);
		GridBagConstraints gbc_lblMinimalAreaOf = new GridBagConstraints();
		gbc_lblMinimalAreaOf.anchor = GridBagConstraints.EAST;
		gbc_lblMinimalAreaOf.insets = new Insets(0, 0, 5, 5);
		gbc_lblMinimalAreaOf.gridx = 1;
		gbc_lblMinimalAreaOf.gridy = 13;
		getContentPane().add(lblMinimalAreaOf, gbc_lblMinimalAreaOf);

		sliderMinimalAreaOf = new JSlider();
		sliderMinimalAreaOf.setFont(font);
		sliderMinimalAreaOf.setValue(20);
		GridBagConstraints gbc_slider_2 = new GridBagConstraints();
		gbc_slider_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_2.insets = new Insets(0, 0, 5, 5);
		gbc_slider_2.gridx = 2;
		gbc_slider_2.gridy = 13;
		sliderMinimalAreaOf.addChangeListener(this);
		getContentPane().add(sliderMinimalAreaOf, gbc_slider_2);

		lblMinimalAreaOf2 = new JTextField(IJ.d2s(paramTemp.minCellSurface * paramTemp.getPixelSizeUmSquared(), 1));
		lblMinimalAreaOf2.setFont(font);
		GridBagConstraints gbc_label_2 = new GridBagConstraints();
		gbc_label_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_label_2.insets = new Insets(0, 0, 5, 5);
		gbc_label_2.gridx = 3;
		gbc_label_2.gridy = 13;
		lblMinimalAreaOf2.addActionListener(this);
		lblMinimalAreaOf2.addFocusListener(this);
		getContentPane().add(lblMinimalAreaOf2, gbc_label_2);

		lblMaximalAreaOf = new JLabel("Maximal area of a cell (\u00B5m\u00B2):");
		lblMaximalAreaOf.setFont(font);
		GridBagConstraints gbc_lblMaximalAreaOf = new GridBagConstraints();
		gbc_lblMaximalAreaOf.anchor = GridBagConstraints.EAST;
		gbc_lblMaximalAreaOf.insets = new Insets(0, 0, 5, 5);
		gbc_lblMaximalAreaOf.gridx = 1;
		gbc_lblMaximalAreaOf.gridy = 14;
		getContentPane().add(lblMaximalAreaOf, gbc_lblMaximalAreaOf);

		sliderMaximalAreaOf = new JSlider();
		sliderMaximalAreaOf.setFont(font);
		sliderMaximalAreaOf.setValue(10);
		GridBagConstraints gbc_slider_3 = new GridBagConstraints();
		gbc_slider_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_3.insets = new Insets(0, 0, 5, 5);
		gbc_slider_3.gridx = 2;
		gbc_slider_3.gridy = 14;
		sliderMaximalAreaOf.addChangeListener(this);
		getContentPane().add(sliderMaximalAreaOf, gbc_slider_3);

		labelMaximalAreaOf2 = new JTextField(IJ.d2s(paramTemp.maxCellSurface * paramTemp.getPixelSizeUmSquared(), 0));
		labelMaximalAreaOf2.setFont(font);
		GridBagConstraints gbc_label_3 = new GridBagConstraints();
		gbc_label_3.fill = GridBagConstraints.HORIZONTAL;
		gbc_label_3.insets = new Insets(0, 0, 5, 5);
		gbc_label_3.gridx = 3;
		gbc_label_3.gridy = 14;
		labelMaximalAreaOf2.addActionListener(this);
		labelMaximalAreaOf2.addFocusListener(this);
		getContentPane().add(labelMaximalAreaOf2, gbc_label_3);

		btnOk = new JButton("OK");
		btnOk.setFont(font);
		GridBagConstraints gbc_btnOk = new GridBagConstraints();
		gbc_btnOk.insets = new Insets(0, 0, 0, 5);
		gbc_btnOk.gridx = 1;
		gbc_btnOk.gridy = 16;
		btnOk.addActionListener(this);
		getContentPane().add(btnOk, gbc_btnOk);

		btnReset = new JButton("Reset");
		btnReset.setFont(font);
		GridBagConstraints gbc_btnReset = new GridBagConstraints();
		gbc_btnReset.insets = new Insets(0, 0, 0, 5);
		gbc_btnReset.gridx = 2;
		gbc_btnReset.gridy = 16;
		btnReset.addActionListener(this);
		getContentPane().add(btnReset, gbc_btnReset);

		btnCancel = new JButton("Cancel");
		btnCancel.setFont(font);
		GridBagConstraints gbc_btnCancel = new GridBagConstraints();
		gbc_btnCancel.insets = new Insets(0, 0, 0, 5);
		gbc_btnCancel.gridx = 3;
		gbc_btnCancel.gridy = 16;
		btnCancel.addActionListener(this);
		getContentPane().add(btnCancel, gbc_btnCancel);

		updateImage();

		this.setVisible(true);
	}

	@Override
	protected void disposeThis() {
		comboboxChannel1.removeActionListener(this);
		comboboxChannel2.removeActionListener(this);
		sliderContourIntensityThreshold2.removeChangeListener(this);
		lblContourIntensityThreshold_2.removeActionListener(this);
		lblContourIntensityThreshold_2.removeFocusListener(this);
		super.disposeThis();
	}

	@Override
	protected void somethingHappened(Object source) {
		if (source == comboboxChannel1) {
			int newChannel = comboboxChannel1.getSelectedIndex() + 1;
			if (newChannel > 0 && newChannel != channel1) {
				channel1 = newChannel;
				ImagePlus impTemp = new Duplicator().run(imageIni, channel1, channel1, 1, 1, 1, imageIni.getNFrames());
				stack8bit = GenUtils.convertStack(impTemp.getImageStack(), 8);
				image.setStack(GenUtils.convertStack(impTemp, 32).getImageStack());
				IJ.run("Enhance Contrast", "saturated=0.35");
				updateImage();
			}
		} else if (source == comboboxChannel2) {
			int newChannel = comboboxChannel2.getSelectedIndex() + 1;
			if (newChannel > 0 && newChannel != channel2) {
				channel2 = newChannel;
				ImagePlus impTemp = new Duplicator().run(imageIni, channel2, channel2, 1, 1, 1, imageIni.getNFrames());
				stack8bit2 = GenUtils.convertStack(impTemp.getImageStack(), 8);
				image2.setStack(GenUtils.convertStack(impTemp, 32).getImageStack());
				IJ.run("Enhance Contrast", "saturated=0.35");
				updateImage();
			}
		} else if (source == lblContourIntensityThreshold_2) {
			paramTemp.greyThreshold2 = (int) Double.parseDouble(lblContourIntensityThreshold_2.getText());
			sliderMoveAllowed = false;
			sliderContourIntensityThreshold2.setValue(paramTemp.greyThreshold2);
			updateImage();
			sliderMoveAllowed = true;
		} else {
			super.somethingHappened(source);
		}
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		Object source = e.getSource();
		if (sliderMoveAllowed) {
			if (source == sliderContourIntensityThreshold2) {
				paramTemp.greyThreshold2 = sliderContourIntensityThreshold2.getValue();
				lblContourIntensityThreshold_2.setText(IJ.d2s(paramTemp.greyThreshold2, 0));
				updateImage();
			} else {
				super.stateChanged(e);
			}
		}
	}

	private boolean imageLock = false;

	private synchronized boolean aquireImageLock() {
		if (imageLock)
			return false;
		return imageLock = true;
	}

	@Override
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
							Params paramtempBis = paramtemp.clone();
							paramtempBis.greyThreshold = paramtempBis.greyThreshold2;
							int frameTemp = frame;
							// Do something with parameters
							updateAnalysis(paramtemp, frame, stack8bit, image);
							updateAnalysis(paramtempBis, frame, stack8bit2, image2);
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

	@Override
	protected void updateView(int frame) {
		image2.setSliceWithoutUpdate(frame + 1);
		canvas2.repaint();
		image2.draw();
		image2.setHideOverlay(false);

		super.updateView(frame);
	}

	@Override
	public void imageClosed(ImagePlus imp) {
		if (imp == image2)
			image2.show();
		else
			super.imageClosed(imp);
	}

	@Override
	public void imageUpdated(ImagePlus imp) {
		if (imp == image2 && imp.getCurrentSlice() - 1 != frame) {
			frame = imp.getCurrentSlice() - 1;
			updateImage();
		} else
			super.imageUpdated(imp);
	}

}
