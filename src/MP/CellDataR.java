package MP;

import java.awt.Color;

import net.calm.iaclasslibrary.Cell.CellData;

public class CellDataR {

	protected static final int NOT_REJECTED = 0, REJECT_MANUAL = 1, REJECT_DRAMATIC_CHANGE = 2, REJECT_WHOLE_TRAJ = 3,
			REJECT_TRAJ_LENGTH = 4, REJECT_404 = 5, GREEN = 6, RED = 7;
	protected static final Color[] COLOR = new Color[] { Color.BLUE, Color.BLUE.darker().darker(),
			Color.RED.darker().darker().darker(), new Color(128, 0, 128).darker(),
			Color.GREEN.darker().darker().darker(), Color.YELLOW.darker().darker().darker(), Color.GREEN.darker(),
			Color.RED };
	private int originalCellColour = CellDataR.GREEN;
	private boolean[] changeInGreenRedChannel;
	private int[] rejectFrame;
	private CellData cellData;

	private int stopRejectWholeCellFrame = -1;

	public CellDataR(CellData cellData, int frameNumber) {
		this.cellData = cellData;
		rejectFrame = new int[frameNumber];

		changeInGreenRedChannel = new boolean[frameNumber];
		for (int i = 0; i < frameNumber; i++) {
			changeInGreenRedChannel[i] = false;
		}
	}

	public void setStoreGreenRedChannel(int frame) {
		if (frame >= 0 && frame < changeInGreenRedChannel.length)
			changeInGreenRedChannel[frame] = !changeInGreenRedChannel[frame];
	}

	public boolean getStoreGreenRedChannel(int frame) {
		if (frame >= 0 && frame < changeInGreenRedChannel.length)
			return changeInGreenRedChannel[frame];
		return false;
	}

	public void stopWholeCellRejection(int frame) {
		stopRejectWholeCellFrame = frame;
	}

	public int getStopWholeCellRejection() {
		return stopRejectWholeCellFrame;
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

	public int whichOriginalCellColour() {
		return originalCellColour;
	}

	public void setOriginalCellColour(int cellCOLOUR) {
		this.originalCellColour = cellCOLOUR;
	}

}
