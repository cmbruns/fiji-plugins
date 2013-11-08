import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import java.util.HashMap;
import java.text.DecimalFormat;

// Compute the Shannon entropy of one channel of the current image.
// Result in bits is shown in the log window.
public class Shannon_Entropy2 implements PlugIn {

	public void run(String arg) {
		ImagePlus image = IJ.getImage();
		int currentChannel = image.getChannel();
		int currentSlice = image.getSlice();
		int currentFrame = image.getFrame();
		ImageStack stack = image.getStack();
		HashMap<Integer, Integer> histogram = new HashMap<Integer, Integer>();
		int totalPixels = 0;
		// Count how many pixels there are of each intensity
		for (int sx = 1; sx <= image.getNSlices(); ++sx) {
			
			// IJ.log("Processing slice number " + sx);
			int stackIx = image.getStackIndex(currentChannel, sx, currentFrame);
			ImageProcessor ip = stack.getProcessor(stackIx);
			for (int x = 0; x < ip.getWidth(); ++x) {
				for (int y = 0; y < ip.getHeight(); ++y) {
					int val = ip.getPixel(x, y);
					totalPixels++;
					if (histogram.containsKey(val)) {
						histogram.put(val, histogram.get(val) + 1);
					} else {
						histogram.put(val, 1);
					}
				}
			}
			IJ.showProgress(sx, image.getNSlices());
		}
		// Convert counts to probabilities and compute Shannon entropy
		double totalProbability = 0.0;
		double entropy = 0.0;
		int minIntensity = 1000000;
		int maxIntensity = -1;
		Integer modeIntensity = null;
		int modeCount = -1;
		for (Integer intensity : histogram.keySet()) {
			int count = histogram.get(intensity);
			// While we're here, store the mode statistic too
			if (count > modeCount) {
				modeCount = count;
				modeIntensity = intensity;
			}
			// And min and max
			if (minIntensity > intensity) {
				minIntensity = intensity;
			}
			if (maxIntensity < intensity) {
				maxIntensity = intensity;
			}
			double p = (double)count / (double)totalPixels;
			totalProbability += p;
			entropy -= p * Math.log(p) / Math.log(2.0); // bits
		}

		DecimalFormat entFmt = new DecimalFormat("0.00");
		DecimalFormat countFmt = new DecimalFormat("#,###,###,###,##0");
		
		String msg = "Shannon entropy = " + entFmt.format(entropy) + " bits per pixel";
		msg += "\nNumber of slices = " + image.getNSlices();
		msg += "\nNumber of voxels = " + countFmt.format(totalPixels);
		msg += "\nMost common intensity (mode) = " + modeIntensity + " (" + countFmt.format(modeCount) + " times)";
		msg += "\nMaximum intensity = " + maxIntensity;
		msg += "\nMinimum intensity = " + minIntensity;
		IJ.showMessage("Shannon Entropy", msg);
	}

}
