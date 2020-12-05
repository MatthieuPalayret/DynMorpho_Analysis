package MP;

import java.awt.Color;
import java.awt.Font;
import java.io.File;

import MP.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;

public class Open_MP implements PlugIn {

	String path;
	ImagePlus imp = null;

	public Open_MP() {
		path = Utils.getAFile("Select the \"stack-ini.tif\"", path, "stack-ini.tif")[0];
	}

	public Open_MP(String path, ImagePlus imp) {
		this.path = path;
		this.imp = imp;
	}

	@Override
	public void run(String arg0) {
		imp = IJ.openImage(path + File.separator + "stack-ini.tif");
		imp.show();
		// IJ.resetMinAndMax(imp);
		IJ.run("Enhance Contrast", "saturated=0.35");

		RoiManager rm = ij.plugin.frame.RoiManager.getRoiManager();
		rm.runCommand("Open", path + File.separator + "stack-RoiSet.zip");
		rm.setVisible(true);
		while (!imp.isVisible())
			IJ.wait(100);

		IJ.run("From ROI Manager");
		rm.moveRoisToOverlay(imp);
		rm.runCommand("Show All with labels");

		Overlay ov = null;
		ImageCanvas ic = imp.getCanvas();
		if (ic != null)
			ov = ic.getShowAllList();
		if (ov == null)
			ov = imp.getOverlay();

		ov.drawLabels(true);
		Analyzer.drawLabels(true);
		ov.drawNames(true);
		ov.drawBackgrounds(false);
		ov.setLabelColor(Color.WHITE);
		ov.setLabelFont(new Font("SansSerif", Font.BOLD, 18), false);
		imp.draw();
	}

}
