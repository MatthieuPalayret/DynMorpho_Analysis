package MP;

import ij.plugin.ZProjector;

public class Two_Colour_Analysis extends Analyse_Protrusion_MP {

	public Two_Colour_Analysis() {
		super();

		params.twoColourAnalysis = true;

		ZProjector zproj = new ZProjector(imp);
		zproj.setMethod(ZProjector.MAX_METHOD);
		zproj.setStartSlice(1);
		zproj.setStopSlice(imp.getNSlices());
		zproj.doHyperStackProjection(true);
		imp = zproj.getProjection();
	}
}
