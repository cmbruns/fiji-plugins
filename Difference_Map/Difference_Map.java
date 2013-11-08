import ij.plugin.PlugIn;
import ij.*;
import ij.gui.*;
import ij.process.*;

/**
 * This is a template for a plugin that does not require one image
 * (be it that it does not require any, or that it lets the user
 * choose more than one image in a dialog).
 */
public class Difference_Map implements PlugIn {
	/**
	 * This method gets called by ImageJ / Fiji.
	 *
	 * @param arg can be specified in plugins.config
	 */
	public void run(String arg) 
	{
		int nImages = WindowManager.getImageCount();
		if (nImages < 1) {
			IJ.error("No images are open!");
		}
		
		int[] imageWindowIDs = WindowManager.getIDList();
		String[] titles = new String[nImages];
		for (int win=0; win < nImages; win++) {
			titles[win] = WindowManager.getImage(imageWindowIDs[win]).getTitle();
		}
		
		GenericDialog dlg = new GenericDialog("Difference Map");
		dlg.addMessage("Choose two images to compare:");
		dlg.addChoice("Image1:", titles, titles[0]);
		int ix = 1;
		if (nImages < 2) {ix = 0;}
		dlg.addChoice("Image2:", titles, titles[ix]);
		
		dlg.showDialog();
		if (dlg.wasCanceled()) return;

		ImagePlus imp1 = WindowManager.getImage(dlg.getNextChoice());
		ImagePlus imp2 = WindowManager.getImage(dlg.getNextChoice());
		Object[] result = exec(imp1, 1, imp2, 1);
		if (null != result) {
			ImagePlus diff = (ImagePlus) result[0];
			diff.show();
		}	
	}

	public Object[] exec(ImagePlus imp1, int channel1, ImagePlus imp2, int channel2) {
		int width = Math.min(imp1.getWidth(), imp2.getWidth());
		int height = Math.min(imp1.getHeight(), imp2.getHeight());
		int nSlices = Math.min(imp1.getNSlices(), imp2.getNSlices());
		ImagePlus diffImg = NewImage.createRGBImage(
				"Difference of " + imp1.getTitle() + " and " + imp2.getTitle(), 
				width, height, nSlices, NewImage.FILL_BLACK);

		// per-slice intensity normalization factors
		double[] m = new double[nSlices + 1];
		double[] b = new double[nSlices + 1];
		double maxDiff = 0; // for scaling entire stack
		
		for (int slice = 1; slice <= nSlices; ++slice) 
		{
			IJ.showProgress(slice, nSlices * 2);
			ImageProcessor ip1 = imp1.getStack().getProcessor(slice);
			ImageProcessor ip2 = imp2.getStack().getProcessor(slice);
			// 1 - compute scale factor for this slice
			// in log space, to include gamma correction
			// and to reduce effect of saturated pixels
			double xSum = 0;
			double ySum = 0;
			double xySum = 0;
			double xSqSum = 0;
			int n = 0;
			for (int x = 0; x < width; ++x) {
				for (int y = 0; y < height; ++y) {
					int val1 = ip1.get(x, y);
					int val2 = ip2.get(x, y);
					// Skip saturated pixels
					if (val1 == 255) continue;
					if (val2 == 255) continue;
					if (val1 == 4095) continue;
					if (val2 == 4095) continue;
					double logVal1 = Math.log(val1 + 1);
					double logVal2 = Math.log(val2 + 1);
					xSum += logVal1;
					ySum += logVal2;
					xySum += logVal1 * logVal2;
					xSqSum += logVal1 * logVal1;
					n++;
				}
			}
			// Best fit line parameters in log space
			// logY = m * logX + b
			m[slice] = (n*xySum - xSum*ySum) / (n*xSqSum - xSum*xSum);
			b[slice] = (ySum - m[slice]*xSum) / n;
			
			// 2) Scale to +- 255
			for (int x = 0; x < width; ++x) {
				for (int y = 0; y < height; ++y) 
				{
					// Apply per-slice scaling
					double x0 = Math.log(ip1.get(x, y) + 1);
					x0 = m[slice] * x0 + b[slice]; // scale to y range
					x0 = Math.exp(x0) - 1;
					int scaledX = (int)Math.round(x0);
					
					int diff = scaledX - ip2.get(x, y);
					maxDiff = Math.max(Math.abs(diff), maxDiff);
				}
			}
		}
		double scale = 1.0;
		if (maxDiff > 0) {
			scale = 255.0 / maxDiff;
		}

		for (int slice = 1; slice <= nSlices; ++slice) 
		{
			IJ.showProgress(nSlices + slice, nSlices * 2);
			ImageProcessor ip1 = imp1.getStack().getProcessor(slice);
			ImageProcessor ip2 = imp2.getStack().getProcessor(slice);
			ImageProcessor ipDiff = diffImg.getStack().getProcessor(slice);
			// 3 - apply difference
			int[] rgb = new int[3];
			for (int x = 0; x < width; ++x) {
				for (int y = 0; y < height; ++y) 
				{
					// Apply per-slice scaling
					double x0 = Math.log(ip1.get(x, y) + 1);
					x0 = m[slice] * x0 + b[slice]; // scale to y range
					x0 = Math.exp(x0) - 1;
					double scaledX = x0 * scale;
					double scaledY = ip2.get(x, y) * scale;
					
					int diff = (int)Math.round(scaledX - scaledY);
					if (diff < 0) {
						// green for negative values
						rgb[1] = -diff;
						rgb[0] = rgb[2] = 0;
					}
					else {
						// magenta for positive values
						rgb[0] = rgb[2] = diff;
						rgb[1] = 0;
					}
					ipDiff.putPixel(x, y, rgb);
				}
				
			}
		}
		return new Object[]{diffImg};
	}
}
