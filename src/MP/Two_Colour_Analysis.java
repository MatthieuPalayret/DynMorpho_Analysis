package MP;

import java.io.File;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.Duplicator;

public class Two_Colour_Analysis extends Analyse_Protrusion_MP {

	public Two_Colour_Analysis() {
		super();

		params.twoColourAnalysis = true;

		/**
		 * ZProjector zproj = new ZProjector(imp);
		 * zproj.setMethod(ZProjector.MAX_METHOD); zproj.setStartSlice(1);
		 * zproj.setStopSlice(imp.getNSlices()); zproj.doHyperStackProjection(true); imp
		 * = zproj.getProjection();
		 **/

	}

	@Override
	public void run(String subClass) {
		IJ.resetMinAndMax(imp);
		directory = new File(imp.getOriginalFileInfo().directory);

		IJ.log("Choose parameters to determine the cell contours... Legend:");
		IJ.log("- Blue: selected cell contours.");
		IJ.log("- Dark blue: cell which area is > "
				+ IJ.d2s(params.maxCellSurface * Math.pow(params.pixelSizeNm / 1000.0, 2)) + " µm².");
		int finished = params.getNewParameters(IJ.getImage());
		if (finished == ParamPreview.CANCEL) {
			IJ.log("Plugin cancelled!");
			return;
		}
		uv = params.updateUV(uv);

		ImagePlus image = new Duplicator().run(imp, params.channel2, params.channel2, 1, 1, 1, imp.getNFrames());
		imp.hide();
		IJ.resetMinAndMax(image);
		image.show();
		Params paramMemo = params.clone();
		params.greyThreshold = paramMemo.greyThreshold2;
		params.twoColourAnalysis = false;
		uv = params.updateUV(uv);
		batchMode = true;
		runFromAnalyseMovieMP("");
		params = paramMemo;
		image.close();

		image = new Duplicator().run(imp, params.channel1, params.channel1, 1, 1, 1, imp.getNFrames());
		IJ.resetMinAndMax(image);
		image.show();
		// Utils.saveTiff(image, directory + File.separator + "Channel1.tiff", false);
		Analyse_Protrusion_MP channel1_Analysis = new Analyse_Protrusion_MP();
		channel1_Analysis.params = params.clone();
		channel1_Analysis.params.twoColourAnalysis = false;
		channel1_Analysis.uv = params.updateUV(uv);
		channel1_Analysis.batchMode = true;
		channel1_Analysis.parDir = new File(parDir + File.separator + "RedChannel");
		channel1_Analysis.parDir.mkdirs();
		channel1_Analysis.runFromAnalyseMovieMP("");

		params.setChildDir(parDir);

		if (subClass == null || subClass.isEmpty()) {
			IJ.log("Building protrusions...");
			Results res = new Results(this.getCellData(), this.stacks[0].getSize(), params);
			res.buildProtrusions(image, false);
			Results resRed = new Results(channel1_Analysis.getCellData(), channel1_Analysis.stacks[0].getSize(),
					params);
			resRed.buildProtrusions(image, false);

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
					channel1_Analysis.stacks[0]);
			pm.run();
			if (pm.finished == ParamVisualisation.CANCEL) {
				imp.show();
				IJ.log("Back to step 1...");
				batchMode = true;
				directory = parDir;
				this.run(subClass);
				return;
			}

			new Open_MP(parDir.getAbsolutePath(), new ImagePlus()).run("");
			res.kill();
			IJ.log("End of MP Protrusion analyses.");
		}

	}

}
