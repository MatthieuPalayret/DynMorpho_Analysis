package MP;

import java.awt.Color;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import ij.gui.Plot;
import ij.measure.ResultsTable;

public class Histogram {

	double max;
	private int yMax;
	int bins;
	private boolean LogScale = false;
	private boolean Refreshing = false;

	double[] x;
	LinkedList<double[]> Hists = new LinkedList<double[]>();
	LinkedList<double[]> list = new LinkedList<double[]>();
	LinkedList<double[]> fit = new LinkedList<double[]>();
	LinkedList<String> legend = new LinkedList<String>();
	Plot plot = new Plot("Histogram", "bins", "");

	public Histogram(double max, int bins, boolean logScale) {
		this.max = max;
		this.bins = bins;
		this.LogScale = logScale;

		initialiseX();
	}

	public Histogram(double max, int bins) {
		this(max, bins, false);
	}

	public Histogram(double[] values, int bins, boolean DiscardZeros, boolean LogScale, String Legend) {
		this.bins = bins;
		this.LogScale = LogScale;
		if (values != null && values.length > 0) {
			max = Utils.getMean(values, DiscardZeros) + 3.0D * Utils.getIQR(values, DiscardZeros);

			initialiseX();
			addHistogram(values, DiscardZeros, Legend);
		}
	}

	public Histogram(double[] values, int bins, boolean DiscardZeros, boolean LogScale) {
		this(values, bins, DiscardZeros, LogScale, "");
	}

	public Histogram(double[] values, int bins, boolean DiscardZeros, String Legend) {
		this(values, bins, DiscardZeros, false, Legend);
	}

	public Histogram(double[] values, int bins, boolean DiscardZeros) {
		this(values, bins, DiscardZeros, "");
	}

	private void initialiseX() {
		x = new double[bins];
		for (int i = 0; i < bins; i++) {
			x[i] = (i) * max / (bins);
		}
	}

	public double[] put(double[] histogram, int histogramNumber, String legend) {
		double[] hist;
		boolean updateHist = histogramNumber >= 0 && histogramNumber < Hists.size();
		if (updateHist)
			hist = Hists.get(histogramNumber);
		else
			hist = new double[bins];

		for (int i = 0; i < histogram.length; i++) {
			if (i < hist.length)
				hist[i] += histogram[i];
			else
				hist[hist.length - 1] += histogram[i];
		}

		if (!updateHist)
			Hists.add(hist);

		if (!updateHist && !Refreshing) {
			list.add(new double[0]);
			this.legend.add(legend);
		} else
			this.legend.set(histogramNumber, legend);
		yMax = (int) Math.max(yMax, Utils.getMax(hist));
		return hist;
	}

	public double[] put(double[] histogram, int histogramNumber) {
		return put(histogram, histogramNumber, "");
	}

	public double[] addToHistogram(double[] values, boolean DiscardZeros, int histogramNumber, String legend) {
		double[] Hist;
		boolean UpdateHist = histogramNumber >= 0 && histogramNumber < Hists.size();
		if (UpdateHist)
			Hist = Hists.get(histogramNumber);
		else
			Hist = new double[bins];

		for (int i = 0; i < values.length; i++) {
			if (!DiscardZeros || values[i] != 0) {
				if (LogScale)
					Hist[Math.max(Math.min((int) Math.log(Math.max((values[i] * (bins) / max), 0.00000001)), bins - 1),
							0)]++;
				else
					Hist[Math.max(Math.min((int) (values[i] * (bins) / max), bins - 1), 0)]++;
			}
		}

		if (!UpdateHist)
			Hists.add(Hist);

		if (!Refreshing) {
			if (UpdateHist) {
				list.set(histogramNumber, Utils.concatenate(list.get(histogramNumber), values));
			} else {
				list.add(values);
			}
		}
		yMax = (int) Math.max(yMax, Utils.getMax(Hist));

		if (UpdateHist)
			this.legend.set(histogramNumber, legend);
		else
			this.legend.add(legend);

		return Hist;
	}

	public double[] addToHistogram(double[] values, boolean DiscardZeros, int histogramNumber) {
		return addToHistogram(values, DiscardZeros, histogramNumber, "");
	}

	public double[] addHistogram(double[] values, boolean DiscardZeros, String legend) {
		return addToHistogram(values, DiscardZeros, -1, legend);
	}

	public double[] addHistogram(double[] values, boolean DiscardZeros) {
		return addToHistogram(values, DiscardZeros, -1);
	}

	public Plot plot() {
		plot.setFrameSize(512, 256);
		double halfBox = 0.5D * (max) / (bins);
		plot.setLimits(0 - halfBox, max + halfBox, 0, yMax + 2);
		plot.setAxisXLog(LogScale);

		int histNumber = 0;
		ListIterator<double[]> it = Hists.listIterator();
		ListIterator<String> itLeg = legend.listIterator();
		while (it.hasNext()) {
			double[] add = it.next();
			String addLegend = itLeg.next();

			if (add != null) {
				plot.setColor(Utils.getGradientColor(Color.RED, Color.BLUE, Hists.size(), histNumber));
				histNumber++;
				for (int i = 0; i < add.length; i++)
					drawBox(x[i], add[i]);
				if (addLegend != null && addLegend != "") {
					plot.addLabel(0.5, (0.85 - (histNumber - 1) * 0.1), addLegend);
				}
				plot.draw();
			}
		}

		int fitNumber = 0;
		it = fit.listIterator();
		while (it.hasNext()) {
			double[] add = it.next();

			if (add != null) {
				plot.setColor(Utils.getGradientColor(Color.RED, Color.BLUE, fit.size(), fitNumber));
				fitNumber++;
				plot.addPoints(x, add, Plot.LINE);
				plot.draw();
			}
		}

		return plot;
	}

	public Plot plotAndSave(String path, boolean flush) {
		Plot plotImp = plot();
		Utils.saveTiff(plotImp.getImagePlus(), path, flush);

		ResultsTable data = new ResultsTable(), hist = new ResultsTable();
		hist.incrementCounter();
		hist.setLabel("Bin", hist.getCounter() - 1);
		for (int column = 0; column < x.length; column++)
			hist.addValue(column, x[column]);
		ListIterator<double[]> it = Hists.listIterator();
		ListIterator<String> itLegend = legend.listIterator();
		while (it.hasNext()) {
			double[] add = it.next();
			String addLegend = itLegend.next();
			if (add != null) {
				hist.incrementCounter();
				for (int column = 0; column < add.length; column++)
					hist.addValue(column, add[column]);
				if (addLegend != null && addLegend != "")
					hist.setLabel(addLegend, hist.getCounter() - 1);
			}
		}

		it = list.listIterator();
		itLegend = legend.listIterator();
		if (list.size() > 1) {
			while (it.hasNext()) {
				double[] add = it.next();
				String addLegend = itLegend.next();
				if (add != null) {
					data.incrementCounter();
					for (int column = 0; column < add.length; column++)
						data.addValue(column, add[column]);
					if (addLegend != null && addLegend != "")
						data.setLabel(addLegend, data.getCounter() - 1);
				}
			}
		} else if (list.size() == 1) {
			double[] add = it.next();
			String addLegend = itLegend.next();
			for (int row = 0; row < add.length; row++) {
				data.incrementCounter();
				data.addValue(addLegend, add[row]);
			}
		}

		try {
			data.saveAs(path.substring(0, path.indexOf(".tif")) + "-Data.txt");
			hist.saveAs(path.substring(0, path.indexOf(".tif")) + "-Hist.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}

		return plotImp;
	}

	public double[] getHistogram() {
		return Hists.getFirst();
	}

	private void drawBox(double x, double y) {
		double halfBox = 0.5D * (max) / (bins);
		plot.drawLine(x - halfBox, 0, x - halfBox, y); // Left
		plot.drawLine(x - halfBox, y, x + halfBox, y); // Top
		plot.drawLine(x + halfBox, y, x + halfBox, 0); // Right
	}

	public double[] buildCumulativeFunction(double[] values) {

		// TODO To be done if needed!
		return null;
	}

	public boolean isEmpty() {
		return Hists.size() == 0;
	}

	public void refresh(int histogramNumber, boolean DiscardZeros, boolean LogScale) {
		if (histogramNumber >= 0 && histogramNumber < Hists.size()) {
			Hists = new LinkedList<double[]>();

			this.LogScale = LogScale;
			double[] values = list.get(histogramNumber);
			max = Utils.getMean(values) + 3.0D * Utils.getIQR(values);

			initialiseX();

			Refreshing = true;
			ListIterator<double[]> it = list.listIterator();
			while (it.hasNext()) {
				double[] add = it.next();

				if (add != null) {
					addHistogram(add, DiscardZeros);
				}
			}
			Refreshing = false;
		}
	}

	public void refresh(double max, boolean DiscardZeros, boolean LogScale) {
		Hists = new LinkedList<double[]>();

		this.LogScale = LogScale;
		this.max = max;

		initialiseX();

		Refreshing = true;
		ListIterator<double[]> it = list.listIterator();
		while (it.hasNext()) {
			double[] add = it.next();

			if (add != null) {
				addHistogram(add, DiscardZeros);
			}
		}
		Refreshing = false;
	}

	public int getBinPeak() {
		return getBinPeak(0);
	}

	public int getBinPeak(int histogramNumber) {
		double max = Double.NEGATIVE_INFINITY;
		int binMax = 0;
		double[] histo = Hists.get(histogramNumber);
		for (int i = 0; i < histo.length; i++) {
			if (histo[i] > max) {
				max = histo[i];
				binMax = i;
			}
		}
		return binMax;
	}

	public double getPeak() {
		return getPeak(0);
	}

	public double getPeak(int histogramNumber) {
		return x[getBinPeak(histogramNumber)];
	}

	public double getValueAtPeak() {
		return getValueAtPeak(0);
	}

	public double getValueAtPeak(int histogramNumber) {
		return Hists.get(histogramNumber)[getBinPeak(histogramNumber)];
	}

	public void updateLegend(int histogramNumber, String newLegend) {
		if (histogramNumber < legend.size())
			legend.set(histogramNumber, newLegend);
		else
			legend.add(histogramNumber, newLegend);
	}

	public void updateLegend(String newLegend) {
		updateLegend(0, newLegend);
	}

	public boolean addFit(double[] fitValues) {
		if (fitValues.length != x.length)
			return false;
		else
			fit.add(fitValues);
		return true;
	}

	public Histogram duplicate() {
		Histogram retour = new Histogram(this.max, this.bins, this.LogScale);
		retour.yMax = this.yMax;
		retour.Refreshing = this.Refreshing;

		retour.x = this.x.clone();
		retour.Hists = duplicate(this.Hists);
		retour.list = duplicate(this.list);
		retour.fit = duplicate(this.fit);
		retour.legend = this.legend;

		return retour;
	}

	private static LinkedList<double[]> duplicate(LinkedList<double[]> item) {
		LinkedList<double[]> retour = new LinkedList<double[]>();
		ListIterator<double[]> it = item.listIterator();
		while (it.hasNext()) {
			retour.add(it.next().clone());
		}
		return retour;
	}

}
