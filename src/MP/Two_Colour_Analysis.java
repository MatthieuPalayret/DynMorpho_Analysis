package MP;

import java.io.File;

import javax.swing.JFileChooser;

import MP.objects.Results;
import MP.params.ParamPreview;
import MP.params.ParamVisualisation;
import MP.params.ParamVisualisationTwoColour;
import MP.params.Params;
import MP.utils.Utils;
import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;
import net.calm.iaclasslibrary.UtilClasses.GenUtils;

/**
 * 
 * @author matth
 *
 *         The Two_Colour_Analysis plugin is a version of the Analyse_Protrusion
 *         plugin adapted for a two-channel movie with two populations of cells
 *         to compare: some cells only emit in one channel (they are called the
 *         "green-only" or "green" cells) and others emit in both channels (they
 *         are called the "red-and-green" or "red" cells).
 * 
 *         Basically, both channels are first analysed independently to detect
 *         cells and their contours (in an interactive way). At the same time,
 *         the plugin matches cells that emit in both channels, to detect the
 *         two populations of cells (the "green-only" and the "red-and-green"
 *         cells).
 * 
 *         The cell trajectory analyses and the protrusion analyses are then
 *         performed, as in the Analyse_Protrusion plugin, and the outputs are
 *         given: (1) without distinction between the two population of cells ;
 *         (2) for the "green-only" cells only (with the "_green.csv" suffix) ;
 *         and (3) for the "red-and-green" cells only (with the "_red.csv"
 *         suffix).
 * 
 *
 */
public class Two_Colour_Analysis extends Analyse_Protrusion {

	public Two_Colour_Analysis() {
		super("");
		IJ.log("Plugin: MP v." + Params.version);
		imp = IJ.getImage();

		params = new Params();
		uv = params.getUV();
		params.twoColourAnalysis = true;
		directory = new File(imp.getOriginalFileInfo().directory);

		// Do a z-stack projection
		IJ.run("Z Project...", "projection=[Max Intensity] all");
		imp.close();
		imp.flush();
		imp = IJ.getImage();
		imp.setTitle(directory.getName());

		// Ask where to save the files
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("Select the directory where the result folder will be created:");
		chooser.setMultiSelectionEnabled(false);
		chooser.setCurrentDirectory(directory);

		int returnVal = chooser.showOpenDialog(null);
		File pathTemp = null;
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			pathTemp = chooser.getSelectedFile();
		} else
			pathTemp = directory;

		parDir = new File(GenUtils.openResultsDirectory(pathTemp + File.separator + directory.getName()));
		parDir.mkdir();

		// Save the file
		// new File(directory + File.separator + directory.getName()).mkdir();
		if (params.test)
			Utils.saveTiff(imp, parDir + File.separator + directory.getName() + ".tiff", false);
	}

	@Override
	public void run(String subClass) {
		IJ.resetMinAndMax(imp);

		IJ.log("Choose parameters to determine the cell contours... Legend:");
		IJ.log("- Blue: selected cell contours.");
		IJ.log("- Dark blue: cell which area is > "
				+ IJ.d2s(params.maxCellSurface * Math.pow(params.pixelSizeNm / 1000.0, 2)) + " �m�.");
		int finished = params.getNewParameters(IJ.getImage());
		if (finished == ParamPreview.CANCEL) {
			IJ.log("Plugin cancelled!");
			return;
		}
		uv = params.updateUV(uv);

		ImagePlus image = new Duplicator().run(imp, params.channel1, params.channel1, 1, 1, 1, imp.getNFrames());
		imp.hide();
		IJ.resetMinAndMax(image);
		image.show();
		Params paramMemo = params.clone();
		params.twoColourAnalysis = false;
		uv = params.updateUV(uv);
		batchMode = true;
		runFromAnalyseMovieMP("");
		params = paramMemo;
		image.close();

		image = new Duplicator().run(imp, params.channel2, params.channel2, 1, 1, 1, imp.getNFrames());
		IJ.resetMinAndMax(image);
		image.show();
		Analyse_Protrusion channel2_Analysis = new Analyse_Protrusion();
		channel2_Analysis.params = params.clone();
		channel2_Analysis.params.twoColourAnalysis = false;
		channel2_Analysis.params.greyThreshold = params.greyThreshold2;
		channel2_Analysis.uv = channel2_Analysis.params.updateUV(uv);
		channel2_Analysis.batchMode = true;
		channel2_Analysis.parDir = new File(parDir + File.separator + "RedChannel");
		channel2_Analysis.parDir.mkdirs();
		channel2_Analysis.runFromAnalyseMovieMP("");

		params.setChildDir(parDir);

		if (subClass == null || subClass.isEmpty()) {
			IJ.log("Building protrusions...");
			Results res = new Results(this.getCellData(), this.stacks[0].getSize(), params);
			res.buildProtrusions(image, false);
			Results resRed = new Results(channel2_Analysis.getCellData(), channel2_Analysis.stacks[0].getSize(),
					params);
			resRed.buildProtrusions(image, false, true);

			IJ.log("Protrusions built.");

			IJ.log("Let's play with the parameters... Legend:");
			IJ.log(" - Dark red: cell rejected in a single frame because of area in-/de-crease >"
					+ IJ.d2s(params.dramaticAreaIncrease, 0) + " %.");
			IJ.log(" - Dark blue: cell rejected in a single frame.");
			IJ.log(" - Dark green: trajectory < " + IJ.d2s(params.minTrajLength * params.frameLengthS, 2) + "s.");
			IJ.log(" - Dark purple: whole cell rejected.");
			IJ.log("[- Dark yellow: cell rejected in a single frame because of 'technical' issue.]");
			IJ.log("NB: No uropod is detected in the 1st frame of a trajectory.");
			ParamVisualisation pm = new ParamVisualisationTwoColour(params, res, stacks[0], resRed,
					channel2_Analysis.stacks[0]);
			pm.run();
			if (pm.finished == ParamVisualisation.CANCEL) {
				imp.show();
				IJ.log("Back to step 1...");
				batchMode = true;
				directory = parDir;
				this.run(subClass);
				return;
			}

			IJ.selectWindow("Visualisation - Green channel");
			ImagePlus temp = IJ.getImage();
			if (temp != null && temp.getTitle().startsWith("Visualisation - Green channel"))
				temp.close();

			new Open_MP(parDir.getAbsolutePath(), new ImagePlus()).run("");
			res.kill();
			IJ.log("End of MP Protrusion analyses.");
		}

	}

}
