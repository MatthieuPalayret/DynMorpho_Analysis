package MP;

import java.awt.Color;

import Cell.CellData;

public class CellDataR {

	protected static final int NOT_REJECTED = 0, REJECT_MANUAL = 1, REJECT_DRAMATIC_CHANGE = 2, REJECT_WHOLE_TRAJ = 3,
			REJECT_TRAJ_LENGTH = 4, REJECT_404 = 5;
	protected static final Color[] COLOR = new Color[] { Color.BLUE, Color.BLUE.darker().darker(),
			Color.RED.darker().darker().darker(), new Color(128, 0, 128).darker(),
			Color.GREEN.darker().darker().darker(), Color.YELLOW.darker().darker().darker() };
	private int rejectCell = NOT_REJECTED;
	private int[] rejectFrame;
	private CellData cellData;

	private int stopRejectWholeCellFrame = -1;

	public CellDataR(CellData cellData, int frameNumber) {
		this.cellData = cellData;
		rejectFrame = new int[frameNumber];
	}

	public void stopWholeCellRejection(int frame) {
		stopRejectWholeCellFrame = frame;
	}

	public int getStopWholeCellRejection() {
		return stopRejectWholeCellFrame;
	}

	public void rejectCell(int reject) {
		rejectCell = reject;
	}

	public boolean isCellRejected() {
		return rejectCell != NOT_REJECTED;
	}

	public int whichCellRejection() {
		return rejectCell;
	}

	public boolean rejectFrame(int i, int reject) {
		if (i < 0 || i >= rejectFrame.length)
			return false;
		rejectFrame[i] = reject;
		return true;
	}

	public boolean isFrameRejected(int i) {
		if (i < 0 || i >= rejectFrame.length)
			return false;
		return rejectFrame[i] != NOT_REJECTED;
	}

	public int whichRejectionInFrame(int i) {
		if (i < 0 || i >= rejectFrame.length)
			return -1;
		return rejectFrame[i];
	}

	public int getFrameNumber() {
		return rejectFrame.length;
	}

	public CellData getCellData() {
		return cellData;
	}

}
