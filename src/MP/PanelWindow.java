package MP;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * Adds a panel containing "Invert" and "Flip" buttons to the current image. If
 * no images are open, creates a blank 400x300 byte image and adds the panel to
 * it.
 */
public class PanelWindow implements PlugIn {

	static final int WIDTH = 400;
	static final int HEIGHT = 300;

	@Override
	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			ImageProcessor ip = new ByteProcessor(WIDTH, HEIGHT);
			ip.setColor(Color.white);
			ip.fill();
			imp = new ImagePlus("Panel Window", ip);
		}
		CustomCanvas cc = new CustomCanvas(imp);
		if (imp.getStackSize() > 1)
			new CustomStackWindow(imp, cc);
		else
			new CustomWindow(imp, cc);
		cc.requestFocus();
	}

	class CustomCanvas extends ImageCanvas {

		/**
		 * 
		 */
		private static final long serialVersionUID = -9115778804562252997L;

		CustomCanvas(ImagePlus imp) {
			super(imp);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			super.mousePressed(e);
			IJ.log("mousePressed: (" + offScreenX(e.getX()) + "," + offScreenY(e.getY()) + ")");
		}

	} // CustomCanvas inner class

	class CustomWindow extends ImageWindow implements ActionListener {

		/**
		 * 
		 */
		private static final long serialVersionUID = -902908606714118520L;
		private Button button1, button2;

		CustomWindow(ImagePlus imp, ImageCanvas ic) {
			super(imp, ic);
			addPanel();
		}

		void addPanel() {
			Panel panel = new Panel();
			panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			button1 = new Button(" Invert ");
			button1.addActionListener(this);
			panel.add(button1);
			button2 = new Button(" Flip ");
			button2.addActionListener(this);
			panel.add(button2);
			add(panel, 0);
			pack();
			validate();
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			Point loc = getLocation();
			Dimension size = getSize();
			if (loc.y + size.height > screen.height)
				getCanvas().zoomOut(0, 0);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object b = e.getSource();
			if (b == button1) {
				imp.getProcessor().invert();
				imp.updateAndDraw();
			} else {
				imp.getProcessor().flipVertical();
				imp.updateAndDraw();
			}
			ImageCanvas ic = imp.getCanvas();
			if (ic != null)
				ic.requestFocus();
		}

	} // CustomWindow inner class

	class CustomStackWindow extends StackWindow implements ActionListener {

		/**
		 * 
		 */
		private static final long serialVersionUID = 4337442636053434421L;
		private Button button1, button2;

		CustomStackWindow(ImagePlus imp, ImageCanvas ic) {
			super(imp, ic);
			addPanel();
		}

		void addPanel() {
			Panel panel = new Panel();
			panel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			button1 = new Button(" Invert ");
			button1.addActionListener(this);
			panel.add(button1);
			button2 = new Button(" Flip ");
			button2.addActionListener(this);
			panel.add(button2);
			add(panel, 0);
			pack();
			validate();
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			Point loc = getLocation();
			Dimension size = getSize();
			if (loc.y + size.height > screen.height)
				getCanvas().zoomOut(0, 0);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Object b = e.getSource();
			if (b == button1) {
				imp.getProcessor().invert();
				imp.updateAndDraw();
			} else {
				imp.getProcessor().flipVertical();
				imp.updateAndDraw();
			}
			ImageCanvas ic = imp.getCanvas();
			if (ic != null)
				ic.requestFocus();
		}

	} // CustomStackWindow inner class

} // Panel_Window class