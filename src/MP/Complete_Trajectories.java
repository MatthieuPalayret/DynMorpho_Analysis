package MP;

import java.io.File;
import java.util.HashMap;

import MP.objects.ResultsTableMt;
import MP.params.Params;
import MP.utils.Utils;
import ij.IJ;

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
		final int GROUPSIZE_Sorted = rtFit.addNewColumn("GroupSize");
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

		calculateAvgTrajDirection(trajs, (int) Utils.getMax(rtFit, ResultsTableMt.FRAME), true, true);

		// Analyse trajectories...
		// Link with Align_Trajectories, by making Get_Trajectories an extension of
		// Align_Trajectories.
		// TODO : cell tracking to properly analyse REAR and FRONT tracks...
		analyseTheTracks(false, false, false, true);

		IJ.log("All done!");
	}

}
