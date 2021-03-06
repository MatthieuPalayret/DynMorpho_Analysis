package MP.modifs;

import java.awt.image.ColorModel;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;

/**
 * Performs a walking average on a stack.
 * 
 * Please sent comments to: Arne Seitz <seitz@embl.de>
 * 
 */

public class WalkingAverageMP implements PlugIn {

	@Override
	public void run(String averageNumber) {
		ImagePlus imp = WindowManager.getCurrentImage();

		if (imp == null) {
			IJ.noImage();
			return;
		}
		IJ.run("8-bit");
		int numStacks = imp.getStackSize();
		int average = 1;

		if (numStacks == 1) {
			IJ.error("Must call this plugin on image stack.");
			return;
		}

		if (averageNumber == "")
			return;
		average = Integer.parseInt(averageNumber);
		if (average >= numStacks) {
			IJ.error("Sorry. This is not possible");
			return;
		}

		if (!imp.lock())
			return; // exit if in use

		walkav(imp, numStacks, average);

		imp.unlock();

	}

	protected void walkav(ImagePlus imp, int numImages, int numSubStacks) {

		ImageStack stack = imp.getStack();

		ColorModel cm = imp.createLut().getColorModel();

		ImageStack ims = new ImageStack(stack.getWidth(), stack.getHeight(), cm);
		String sStackName = "walkAv";

		byte[] pixels;
		int dimension = stack.getWidth() * stack.getHeight();
		int[] sum = new int[dimension];
		int stop = stack.getSize();

		for (int s = 0; s <= stop - numSubStacks; s++) {

			for (int j = 0; j < dimension; j++) {
				sum[j] = 0;
			}

			for (int i = 1 + s; i <= numSubStacks + s; i++) {
				pixels = (byte[]) stack.getPixels(i);

				for (int j = 0; j < dimension; j++) {
					sum[j] += 0xff & pixels[j];
				}
			}
			byte[] average = new byte[dimension];

			for (int j = 0; j < dimension; j++) {
				average[j] = (byte) ((sum[j] / numSubStacks) & 0xff);
			}
			ims.addSlice("RollAverage" + s, average);

		}

		ImagePlus nimp = new ImagePlus(sStackName, ims);
		nimp.setStack(sStackName, ims);
		nimp.show();

	}
}
