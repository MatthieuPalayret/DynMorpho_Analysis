package MP;

import java.io.File;
import java.util.HashMap;

import MP.objects.ResultsTableMt;
import MP.params.Params;
import MP.utils.Utils;
import ij.IJ;

/**
 * 
 * @author matth
 *
 *         The Complete_Trajectories plugin should not be used anymore. It
 *         extract trajectories from a pre-processed result file from the
 *         Get_Trajectories plugin (a "Table_Trajectories.txt" file), and
 *         re-analyse them.
 * 
 *         It process the same analyses (1a), (1b), (2) and (3) (but not (4)) as
 *         the Get_Trajectories plugin (see the corresponding explanation).
 * 
 *         It may be used to re-analyse some fitted data whose analyses were
 *         lost or perverted, without redoing the relatively long process of
 *         fitting and tracking the particles.
 */
public class Complete_Trajectories extends Get_Trajectories {

	public Complete_Trajectories() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run(String arg0) {
		IJ.log("MP plugin v." + Params.version);
		IJ.log("Covid-19 vaccination time!");
		IJ.log("            _____________");
		IJ.log("        |--|\"'\"'\"'\"'\"'\"'\"'\"'\"'\"'\"'\"'\"'|-.______________");
		IJ.log("        |--|_____________|-'");

		// Ask for the preanalysed Table_Trajectories.txt file
		String[] temp = Utils.getAFile("Select a preanalised Table_Trajectories.txt file...", "",
				"Table_Trajectories.txt");
		fileDirName = temp[0];
		ResultsTableMt rtFit = ResultsTableMt.open2(fileDirName + File.separatorChar + temp[1]);
		setDirectory(new File(fileDirName), TRAJ);

		rtFit = Utils.sortRt(rtFit, ResultsTableMt.FRAME);
		rtFit = Utils.sortRt(rtFit, ResultsTableMt.GROUP);

		// Extract the trajectories.
		ResultsTableMt[] trajs = new ResultsTableMt[(int) rtFit.getValueAsDouble(ResultsTableMt.GROUP,
				rtFit.getCounter() - 1) + 3];
		ResultsTableMt groupSizes = new ResultsTableMt();
		groupSizes.incrementCounter();
		final int GROUPSIZE_groupSizes = groupSizes.addNewColumn("GroupSize");

		finalHashMap = new HashMap<String, CellContainer>();
		CellContainer allTrajs = new CellContainer(new ResultsTableMt());
		finalHashMap.put("SingleCell", allTrajs);

		int row = 0;
		while (row < rtFit.getCounter() && rtFit.getValueAsDouble(ResultsTableMt.GROUP, row) == 0)
			row++;
		int traj = 0;
		while (row < rtFit.getCounter()) {
			trajs[traj + 3] = new ResultsTableMt();
			while (row < rtFit.getCounter() && rtFit.getValueAsDouble(ResultsTableMt.GROUP, row) == traj + 1) {
				Utils.addRow(rtFit, trajs[traj + 3], row);
				row++;
			}
			while (groupSizes.getCounter() <= traj)
				groupSizes.incrementCounter();
			groupSizes.setValue(GROUPSIZE_groupSizes, traj, trajs[traj + 3].getCounter());

			allTrajs.trajTracks.put(traj, trajs[traj + 3]);
			traj++;
		}

		trajs[0] = groupSizes;

		int frameNumber = (int) Utils.getMax(rtFit, ResultsTableMt.FRAME);
		calculateAvgTrajDirection(trajs, frameNumber, true, true);

		// Analyse trajectories...
		// Link with Align_Trajectories, by making Get_Trajectories an extension of
		// Align_Trajectories.
		// TODO : cell tracking to properly analyse REAR and FRONT tracks...
		analyseTheTracks(false, false, false, true);

		// correctForCellDisplacement(trajs, frameNumber, true);
		pairwiseDistance(trajs, true);

		IJ.log("All done!");
	}

}
