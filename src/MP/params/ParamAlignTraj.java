package MP.params;

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
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import MP.Get_Trajectories;
import MP.objects.ResultsTableMt;
import MP.utils.FittingPeakFit;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.plugin.filter.Analyzer;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;

public class ParamAlignTraj extends JFrame
		implements ActionListener, ChangeListener, ImageListener, FocusListener, KeyListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3108969450503939630L;

	public class Params {
		public double minIntensity = 200; // 1000;
		public double minSigma = 1.25;
		public double maxSigma = 2.75;
		public double maxStepPix = 3;// 1.5;
		public int maxDarkTimeFrame = 4;
		public int minNumberOfLocPerTraj = 3;

		@Override
		public Params clone() {
			Params res = new Params();
			res.minIntensity = this.minIntensity;
			res.minSigma = this.minSigma;
			res.maxSigma = this.maxSigma;
			res.maxStepPix = this.maxStepPix;
			res.maxDarkTimeFrame = this.maxDarkTimeFrame;
			res.minNumberOfLocPerTraj = this.minNumberOfLocPerTraj;
			return res;
		}

		public boolean compare(Params comp) {
			return (comp.minIntensity == this.minIntensity) || (comp.minSigma == this.minSigma)
					|| (comp.maxSigma == this.maxSigma) || (comp.maxStepPix == this.maxStepPix)
					|| (comp.maxDarkTimeFrame == this.maxDarkTimeFrame)
					|| (comp.minNumberOfLocPerTraj == this.minNumberOfLocPerTraj);
		}

		public ResultsTableMt save(String dirPath) {
			ResultsTableMt rt = new ResultsTableMt();
			rt.incrementCounter();
			rt.addValue("Minimal intensity (a.u.)", minIntensity);
			rt.addValue("Minimal width sigma (pix.)", minSigma);
			rt.addValue("Maximal width sigma (pix.)", maxSigma);
			rt.addValue("Maximal distance between fits (pix.)", maxStepPix);
			rt.addValue("Maximal dark time (frames)", maxDarkTimeFrame);
			rt.addValue("Reject trajectory w/ fewer than (fits)", minNumberOfLocPerTraj);

			rt.saveAsPrecise(dirPath + File.separator + "parameters_AlignTrajectories.txt", 3);

			return rt;
		}
	}

	public Params params;

	protected Params paramTemp;

	protected ImagePlus image;
	protected int frame = 0;

	protected static final Font font = new Font("Segoe UI", Font.PLAIN, 13);
	protected JLabel lblParam;
	protected JLabel lblMinIntensity;
	protected JSlider sliderMinIntensity;
	protected JTextField txtMinIntensity;
	protected JLabel lblMinSigma;
	protected JSlider sliderMinSigma;
	protected JTextField txtMinSigma;
	protected JLabel lblMaxSigma;
	protected JSlider sliderMaxSigma;
	protected JTextField txtMaxSigma;

	protected JLabel lblTraj;
	protected JLabel lblMaxStepPix;
	protected JSlider sliderMaxStepPix;
	protected JTextField txtMaxStepPix;
	protected JLabel lblMaxDarkTimeFrame;
	protected JSlider sliderMaxDarkTimeFrame;
	protected JTextField txtMaxDarkTimeFrame;
	protected JLabel lblMinNumberOfLocPerTraj;
	protected JSlider sliderMinNumberOfLocPerTraj;
	protected JTextField txtMinNumberOfLocPerTraj;

	protected ij.gui.ImageCanvas canvas;
	protected JButton btnOk;
	protected JButton btnReset;
	protected JButton btnCancel;

	final static int CANCEL = 1;
	final static int FINISHED = 2;
	final static int RUNNING = 0;
	int finished = RUNNING;
	protected ResultsTableMt rtFit;

	protected ParamAlignTraj thisClass = this;

	public ParamAlignTraj() {
		super("Parameter visualisation...");
	}

	public ParamAlignTraj(ResultsTableMt rtFit, ImageStack stack) {
		super("Parameter visualisation...");

		params = new Params();
		paramTemp = params.clone();
		this.rtFit = rtFit;
		image = new ImagePlus("Visualisation", stack);
		image = new ImagePlus("Visualisation", GenUtils.convertStack(image, 32).getImageStack());
		frame = image.getCurrentSlice() - 1;

		image.updateAndDraw();
		image.show();
		image.getWindow().setLocation(505, 20);
		canvas = image.getCanvas();
		ImagePlus.addImageListener(this);
		canvas.addKeyListener(this);

		this.setBounds(20, 20, 500, 330);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 139, 0, 53, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		getContentPane().setLayout(gridBagLayout);

		addKeyListener(this);

		setMainFeatures();
		setFinalButtons(9);

		updateImage();

		this.setVisible(true);
	}

	public void setMainFeatures() {
		lblParam = new JLabel("Parameters to filter fits:");
		lblParam.setFont(font);
		GridBagConstraints gbc_lblParam = new GridBagConstraints();
		gbc_lblParam.gridwidth = 3;
		gbc_lblParam.insets = new Insets(0, 0, 5, 5);
		gbc_lblParam.gridx = 1;
		gbc_lblParam.gridy = 1;
		getContentPane().add(lblParam, gbc_lblParam);

		lblMinIntensity = new JLabel("Minimal intensity (a.u.):");
		lblMinIntensity.setFont(font);
		GridBagConstraints gbc_lblMinIntensity = new GridBagConstraints();
		gbc_lblMinIntensity.anchor = GridBagConstraints.EAST;
		gbc_lblMinIntensity.insets = new Insets(0, 0, 5, 5);
		gbc_lblMinIntensity.gridx = 1;
		gbc_lblMinIntensity.gridy = 2;
		getContentPane().add(lblMinIntensity, gbc_lblMinIntensity);

		sliderMinIntensity = new JSlider(0, 1000, (int) paramTemp.minIntensity);
		sliderMinIntensity.setFont(font);
		GridBagConstraints gbc_sliderMinIntensity = new GridBagConstraints();
		gbc_sliderMinIntensity.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderMinIntensity.insets = new Insets(0, 0, 5, 5);
		gbc_sliderMinIntensity.gridx = 2;
		gbc_sliderMinIntensity.gridy = 2;
		sliderMinIntensity.addChangeListener(this);
		getContentPane().add(sliderMinIntensity, gbc_sliderMinIntensity);

		txtMinIntensity = new JTextField(IJ.d2s(paramTemp.minIntensity, 0));
		txtMinIntensity.setFont(font);
		GridBagConstraints gbc_txtMinIntensity = new GridBagConstraints();
		gbc_txtMinIntensity.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMinIntensity.insets = new Insets(0, 0, 5, 5);
		gbc_txtMinIntensity.gridx = 3;
		gbc_txtMinIntensity.gridy = 2;
		txtMinIntensity.addActionListener(this);
		txtMinIntensity.addFocusListener(this);
		getContentPane().add(txtMinIntensity, gbc_txtMinIntensity);

		lblMinSigma = new JLabel("Minimal width \u03C3 (pix.):");
		lblMinSigma.setFont(font);
		GridBagConstraints gbc_lblMinSigma = new GridBagConstraints();
		gbc_lblMinSigma.anchor = GridBagConstraints.EAST;
		gbc_lblMinSigma.insets = new Insets(0, 0, 5, 5);
		gbc_lblMinSigma.gridx = 1;
		gbc_lblMinSigma.gridy = 3;
		getContentPane().add(lblMinSigma, gbc_lblMinSigma);

		sliderMinSigma = new JSlider(0, 500, (int) (paramTemp.minSigma * 100.0));
		sliderMinSigma.setFont(font);
		GridBagConstraints gbc_sliderMinSigma = new GridBagConstraints();
		gbc_sliderMinSigma.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderMinSigma.insets = new Insets(0, 0, 5, 5);
		gbc_sliderMinSigma.gridx = 2;
		gbc_sliderMinSigma.gridy = 3;
		sliderMinSigma.addChangeListener(this);
		getContentPane().add(sliderMinSigma, gbc_sliderMinSigma);

		txtMinSigma = new JTextField(IJ.d2s(paramTemp.minSigma, 2));
		txtMinSigma.setFont(font);
		GridBagConstraints gbc_txtMinSigma = new GridBagConstraints();
		gbc_txtMinSigma.insets = new Insets(0, 0, 5, 5);
		gbc_txtMinSigma.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMinSigma.gridx = 3;
		gbc_txtMinSigma.gridy = 3;
		getContentPane().add(txtMinSigma, gbc_txtMinSigma);
		txtMinSigma.addActionListener(this);
		txtMinSigma.addFocusListener(this);

		lblMaxSigma = new JLabel("Maximal width \u03C3 (pix.):");
		lblMaxSigma.setFont(font);
		GridBagConstraints gbc_lblMaxSigma = new GridBagConstraints();
		gbc_lblMaxSigma.anchor = GridBagConstraints.EAST;
		gbc_lblMaxSigma.insets = new Insets(0, 0, 5, 5);
		gbc_lblMaxSigma.gridx = 1;
		gbc_lblMaxSigma.gridy = 4;
		getContentPane().add(lblMaxSigma, gbc_lblMaxSigma);

		sliderMaxSigma = new JSlider(0, 1000, (int) (paramTemp.maxSigma * 100.0));
		sliderMaxSigma.setFont(font);
		GridBagConstraints gbc_sliderMaxSigma = new GridBagConstraints();
		gbc_sliderMaxSigma.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderMaxSigma.insets = new Insets(0, 0, 5, 5);
		gbc_sliderMaxSigma.gridx = 2;
		gbc_sliderMaxSigma.gridy = 4;
		sliderMaxSigma.addChangeListener(this);
		getContentPane().add(sliderMaxSigma, gbc_sliderMaxSigma);

		txtMaxSigma = new JTextField(IJ.d2s(paramTemp.maxSigma, 2));
		txtMaxSigma.setFont(font);
		GridBagConstraints gbc_txtMaxSigma = new GridBagConstraints();
		gbc_txtMaxSigma.insets = new Insets(0, 0, 5, 5);
		gbc_txtMaxSigma.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMaxSigma.gridx = 3;
		gbc_txtMaxSigma.gridy = 4;
		getContentPane().add(txtMaxSigma, gbc_txtMaxSigma);
		txtMaxSigma.addActionListener(this);
		txtMaxSigma.addFocusListener(this);

		lblTraj = new JLabel("Parameters to link fits in trajectories:");
		lblTraj.setFont(font);
		GridBagConstraints gbc_lblTraj = new GridBagConstraints();
		gbc_lblTraj.gridwidth = 3;
		gbc_lblTraj.insets = new Insets(0, 0, 5, 5);
		gbc_lblTraj.gridx = 1;
		gbc_lblTraj.gridy = 5;
		getContentPane().add(lblTraj, gbc_lblTraj);

		lblMaxStepPix = new JLabel("Maximal distance between fits (pix.):");
		lblMaxStepPix.setFont(font);
		GridBagConstraints gbc_lblMaxStepPix = new GridBagConstraints();
		gbc_lblMaxStepPix.anchor = GridBagConstraints.EAST;
		gbc_lblMaxStepPix.insets = new Insets(0, 0, 5, 5);
		gbc_lblMaxStepPix.gridx = 1;
		gbc_lblMaxStepPix.gridy = 6;
		getContentPane().add(lblMaxStepPix, gbc_lblMaxStepPix);

		sliderMaxStepPix = new JSlider(0, (int) (4 * 100.0), (int) (paramTemp.maxStepPix * 100.0));
		sliderMaxStepPix.setFont(font);
		GridBagConstraints gbc_sliderMaxStepPix = new GridBagConstraints();
		gbc_sliderMaxStepPix.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderMaxStepPix.insets = new Insets(0, 0, 5, 5);
		gbc_sliderMaxStepPix.gridx = 2;
		gbc_sliderMaxStepPix.gridy = 6;
		sliderMaxStepPix.addChangeListener(this);
		getContentPane().add(sliderMaxStepPix, gbc_sliderMaxStepPix);

		txtMaxStepPix = new JTextField(IJ.d2s(paramTemp.maxStepPix, 2));
		txtMaxStepPix.setFont(font);
		GridBagConstraints gbc_txtMaxStepPix = new GridBagConstraints();
		gbc_txtMaxStepPix.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMaxStepPix.insets = new Insets(0, 0, 5, 5);
		gbc_txtMaxStepPix.gridx = 3;
		gbc_txtMaxStepPix.gridy = 6;
		txtMaxStepPix.addActionListener(this);
		txtMaxStepPix.addFocusListener(this);
		getContentPane().add(txtMaxStepPix, gbc_txtMaxStepPix);

		lblMaxDarkTimeFrame = new JLabel("Maximal dark time (frames):");
		lblMaxDarkTimeFrame.setFont(font);
		GridBagConstraints gbc_lblMaxDarkTimeFrame = new GridBagConstraints();
		gbc_lblMaxDarkTimeFrame.anchor = GridBagConstraints.EAST;
		gbc_lblMaxDarkTimeFrame.insets = new Insets(0, 0, 5, 5);
		gbc_lblMaxDarkTimeFrame.gridx = 1;
		gbc_lblMaxDarkTimeFrame.gridy = 7;
		getContentPane().add(lblMaxDarkTimeFrame, gbc_lblMaxDarkTimeFrame);

		sliderMaxDarkTimeFrame = new JSlider(0, 5, paramTemp.maxDarkTimeFrame);
		sliderMaxDarkTimeFrame.setFont(font);
		GridBagConstraints gbc_sliderMaxDarkTimeFrame = new GridBagConstraints();
		gbc_sliderMaxDarkTimeFrame.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderMaxDarkTimeFrame.insets = new Insets(0, 0, 5, 5);
		gbc_sliderMaxDarkTimeFrame.gridx = 2;
		gbc_sliderMaxDarkTimeFrame.gridy = 7;
		sliderMaxDarkTimeFrame.addChangeListener(this);
		getContentPane().add(sliderMaxDarkTimeFrame, gbc_sliderMaxDarkTimeFrame);

		txtMaxDarkTimeFrame = new JTextField(IJ.d2s(paramTemp.maxDarkTimeFrame, 0));
		txtMaxDarkTimeFrame.setFont(font);
		GridBagConstraints gbc_txtMaxDarkTimeFrame = new GridBagConstraints();
		gbc_txtMaxDarkTimeFrame.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMaxDarkTimeFrame.insets = new Insets(0, 0, 5, 5);
		gbc_txtMaxDarkTimeFrame.gridx = 3;
		gbc_txtMaxDarkTimeFrame.gridy = 7;
		txtMaxDarkTimeFrame.addActionListener(this);
		txtMaxDarkTimeFrame.addFocusListener(this);
		getContentPane().add(txtMaxDarkTimeFrame, gbc_txtMaxDarkTimeFrame);

		lblMinNumberOfLocPerTraj = new JLabel("Reject trajectory w/ fewer than (fits):");
		lblMinNumberOfLocPerTraj.setFont(font);
		GridBagConstraints gbc_MinNumberOfLocPerTraj = new GridBagConstraints();
		gbc_MinNumberOfLocPerTraj.anchor = GridBagConstraints.EAST;
		gbc_MinNumberOfLocPerTraj.insets = new Insets(0, 0, 5, 5);
		gbc_MinNumberOfLocPerTraj.gridx = 1;
		gbc_MinNumberOfLocPerTraj.gridy = 8;
		getContentPane().add(lblMinNumberOfLocPerTraj, gbc_MinNumberOfLocPerTraj);

		sliderMinNumberOfLocPerTraj = new JSlider(0, 10, paramTemp.minNumberOfLocPerTraj);
		sliderMinNumberOfLocPerTraj.setFont(font);
		GridBagConstraints gbc_sliderMinNumberOfLocPerTraj = new GridBagConstraints();
		gbc_sliderMinNumberOfLocPerTraj.fill = GridBagConstraints.HORIZONTAL;
		gbc_sliderMinNumberOfLocPerTraj.insets = new Insets(0, 0, 5, 5);
		gbc_sliderMinNumberOfLocPerTraj.gridx = 2;
		gbc_sliderMinNumberOfLocPerTraj.gridy = 8;
		sliderMinNumberOfLocPerTraj.addChangeListener(this);
		getContentPane().add(sliderMinNumberOfLocPerTraj, gbc_sliderMinNumberOfLocPerTraj);

		txtMinNumberOfLocPerTraj = new JTextField(IJ.d2s(paramTemp.minNumberOfLocPerTraj, 0));
		txtMinNumberOfLocPerTraj.setFont(font);
		GridBagConstraints gbc_txtMinNumberOfLocPerTraj = new GridBagConstraints();
		gbc_txtMinNumberOfLocPerTraj.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMinNumberOfLocPerTraj.insets = new Insets(0, 0, 5, 5);
		gbc_txtMinNumberOfLocPerTraj.gridx = 3;
		gbc_txtMinNumberOfLocPerTraj.gridy = 8;
		txtMinNumberOfLocPerTraj.addActionListener(this);
		txtMinNumberOfLocPerTraj.addFocusListener(this);
		getContentPane().add(txtMinNumberOfLocPerTraj, gbc_txtMinNumberOfLocPerTraj);
	}

	public void setFinalButtons(int line) {
		btnOk = new JButton("OK");
		btnOk.setFont(font);
		GridBagConstraints gbc_btnOk = new GridBagConstraints();
		gbc_btnOk.insets = new Insets(0, 0, 0, 5);
		gbc_btnOk.gridx = 1;
		gbc_btnOk.gridy = line;
		btnOk.addActionListener(this);
		getContentPane().add(btnOk, gbc_btnOk);

		btnReset = new JButton("Reset");
		btnReset.setFont(font);
		GridBagConstraints gbc_btnReset = new GridBagConstraints();
		gbc_btnReset.insets = new Insets(0, 0, 0, 5);
		gbc_btnReset.gridx = 2;
		gbc_btnReset.gridy = line;
		btnReset.addActionListener(this);
		getContentPane().add(btnReset, gbc_btnReset);

		btnCancel = new JButton("Cancel");
		btnCancel.setFont(font);
		GridBagConstraints gbc_btnCancel = new GridBagConstraints();
		gbc_btnCancel.insets = new Insets(0, 0, 0, 5);
		gbc_btnCancel.gridx = 3;
		gbc_btnCancel.gridy = line;
		btnCancel.addActionListener(this);
		getContentPane().add(btnCancel, gbc_btnCancel);
	}

	public void run() {
		while (finished == RUNNING) {
			IJ.wait(1000);
		}
	}

	protected void disposeThis() {
		this.setVisible(false);
		this.removeKeyListener(this);
		sliderMinIntensity.removeChangeListener(this);
		txtMinIntensity.removeActionListener(this);
		txtMinIntensity.removeFocusListener(this);
		sliderMinSigma.removeChangeListener(this);
		txtMinSigma.removeActionListener(this);
		txtMinSigma.removeFocusListener(this);
		sliderMaxSigma.removeChangeListener(this);
		txtMaxSigma.removeActionListener(this);
		txtMaxSigma.removeFocusListener(this);
		sliderMaxStepPix.removeChangeListener(this);
		txtMaxStepPix.removeActionListener(this);
		txtMaxStepPix.removeFocusListener(this);
		sliderMaxDarkTimeFrame.removeChangeListener(this);
		txtMaxDarkTimeFrame.removeActionListener(this);
		txtMaxDarkTimeFrame.removeFocusListener(this);
		sliderMinNumberOfLocPerTraj.removeChangeListener(this);
		txtMinNumberOfLocPerTraj.removeActionListener(this);
		txtMinNumberOfLocPerTraj.removeFocusListener(this);
		canvas.removeKeyListener(this);
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
							// Do something with parameters
							updateAnalysis(paramtemp, rtFit, image);
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

	protected static void updateAnalysis(Params paramTemp, ResultsTableMt rtFit, ImagePlus image) {
		Get_Trajectories GT = new Get_Trajectories();
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

		rtFit = Get_Trajectories.filterLoc(rtFit, paramTemp.minIntensity, paramTemp.minSigma, paramTemp.maxSigma);
		ResultsTableMt[] trajs = GT.groupInTrajectories(rtFit, paramTemp.maxStepPix, paramTemp.maxDarkTimeFrame,
				paramTemp.minNumberOfLocPerTraj, FittingPeakFit.pixelSize, false);
		Get_Trajectories.plotTrajs(image, trajs);

		image.setHideOverlay(false);
	}

	public void kill() {
		paramTemp = null;
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
			thisClass.updateImage();
		} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
			btnCancel.setSelected(true);
			thisClass.updateImage();
		}
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
		thisClass.somethingHappened(source);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		Object source = e.getSource();
		if (sliderMoveAllowed) {
			if (source == sliderMinIntensity) {
				paramTemp.minIntensity = sliderMinIntensity.getValue();
				txtMinIntensity.setText(IJ.d2s(paramTemp.minIntensity, 0));
				thisClass.updateImage();
			} else if (source == sliderMinSigma) {
				paramTemp.minSigma = sliderMinSigma.getValue() / 100.0;
				txtMinSigma.setText(IJ.d2s(paramTemp.minSigma, 2));
				thisClass.updateImage();
			} else if (source == sliderMaxSigma) {
				paramTemp.maxSigma = sliderMaxSigma.getValue() / 100.0;
				txtMaxSigma.setText(IJ.d2s(paramTemp.maxSigma, 2));
				thisClass.updateImage();
			} else if (source == sliderMaxStepPix) {
				paramTemp.maxStepPix = sliderMaxStepPix.getValue() / 100.0;
				txtMaxStepPix.setText(IJ.d2s(paramTemp.maxStepPix, 2));
				thisClass.updateImage();
			} else if (source == sliderMaxDarkTimeFrame) {
				paramTemp.maxDarkTimeFrame = sliderMaxDarkTimeFrame.getValue();
				txtMaxDarkTimeFrame.setText(IJ.d2s(paramTemp.maxDarkTimeFrame, 0));
				thisClass.updateImage();
			} else if (source == sliderMinNumberOfLocPerTraj) {
				paramTemp.minNumberOfLocPerTraj = sliderMinNumberOfLocPerTraj.getValue();
				txtMinNumberOfLocPerTraj.setText(IJ.d2s(paramTemp.minNumberOfLocPerTraj, 0));
				thisClass.updateImage();
			}
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		thisClass.somethingHappened(source);
	}

	protected boolean sliderMoveAllowed = true;

	protected void somethingHappened(Object source) {
		if (source == txtMinIntensity) {
			paramTemp.minIntensity = Double.parseDouble(txtMinIntensity.getText());
			sliderMoveAllowed = false;
			sliderMinIntensity.setValue((int) paramTemp.minIntensity);
			thisClass.updateImage();
			sliderMoveAllowed = true;
		} else if (source == txtMinSigma) {
			paramTemp.minSigma = Double.parseDouble(txtMinSigma.getText());
			sliderMoveAllowed = false;
			sliderMinSigma.setValue((int) (paramTemp.minSigma * 100.0));
			thisClass.updateImage();
			sliderMoveAllowed = true;
		} else if (source == txtMaxSigma) {
			paramTemp.maxSigma = Double.parseDouble(txtMaxSigma.getText());
			sliderMoveAllowed = false;
			sliderMaxSigma.setValue((int) (paramTemp.maxSigma * 100.0));
			thisClass.updateImage();
			sliderMoveAllowed = true;
		} else if (source == txtMaxStepPix) {
			paramTemp.maxStepPix = Double.parseDouble(txtMaxStepPix.getText());
			sliderMoveAllowed = false;
			sliderMaxStepPix.setValue((int) (paramTemp.maxStepPix * 100.0));
			thisClass.updateImage();
			sliderMoveAllowed = true;
		} else if (source == txtMaxDarkTimeFrame) {
			paramTemp.maxDarkTimeFrame = Integer.parseInt(txtMaxDarkTimeFrame.getText());
			sliderMoveAllowed = false;
			sliderMaxDarkTimeFrame.setValue(paramTemp.maxDarkTimeFrame);
			thisClass.updateImage();
			sliderMoveAllowed = true;
		} else if (source == txtMinNumberOfLocPerTraj) {
			paramTemp.minNumberOfLocPerTraj = Integer.parseInt(txtMinNumberOfLocPerTraj.getText());
			sliderMoveAllowed = false;
			sliderMinNumberOfLocPerTraj.setValue(paramTemp.minNumberOfLocPerTraj);
			thisClass.updateImage();
			sliderMoveAllowed = true;
		} else if (source == btnOk) {
			updateAnalysis(paramTemp, rtFit, image);
			params = paramTemp;
			disposeThis();
			// RoiManager.getInstance().setVisible(false);
			finished = FINISHED;
		} else if (source == btnReset) {
			Params paramReset = new Params();
			paramTemp.minIntensity = paramReset.minIntensity;
			sliderMinIntensity.setValue((int) paramTemp.minIntensity);
			txtMinIntensity.setText(IJ.d2s(paramTemp.minIntensity, 0));
			paramTemp.minSigma = paramReset.minSigma;
			sliderMinSigma.setValue((int) (paramTemp.minSigma * 100.0));
			txtMinSigma.setText(IJ.d2s(paramTemp.minSigma, 2));
			paramTemp.maxSigma = paramReset.maxSigma;
			sliderMaxSigma.setValue((int) (paramTemp.maxSigma * 100.0));
			txtMaxSigma.setText(IJ.d2s(paramTemp.maxSigma, 2));
			paramTemp.maxStepPix = paramReset.maxStepPix;
			sliderMaxStepPix.setValue((int) (paramTemp.maxStepPix * 100.0));
			txtMaxStepPix.setText(IJ.d2s(paramTemp.maxStepPix, 2));
			paramTemp.maxDarkTimeFrame = paramReset.maxDarkTimeFrame;
			sliderMaxDarkTimeFrame.setValue(paramTemp.maxDarkTimeFrame);
			txtMaxDarkTimeFrame.setText(IJ.d2s(paramTemp.maxDarkTimeFrame, 0));
			paramTemp.minNumberOfLocPerTraj = paramReset.minNumberOfLocPerTraj;
			sliderMinNumberOfLocPerTraj.setValue(paramTemp.minNumberOfLocPerTraj);
			txtMinNumberOfLocPerTraj.setText(IJ.d2s(paramTemp.minNumberOfLocPerTraj, 0));
			thisClass.updateImage();
		} else if (source == btnCancel) {
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
			// thisClass.updateImage();
		}
	}

}
