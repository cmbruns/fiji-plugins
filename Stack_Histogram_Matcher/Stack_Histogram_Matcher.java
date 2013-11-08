// Histogram matching plugin
// Match pixel value distribution of all images in a stack
// to the distribution in the first image.

import ij.ImagePlus;
import ij.ImageStack;
import ij.IJ;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

public class Stack_Histogram_Matcher implements PlugIn 
{
	public void run(String arg) 
	{
		ImagePlus image = IJ.getImage();
		int currentChannel = image.getChannel();
		int currentFrame = image.getFrame();
		// Get reference histogram from first slice
		int stackIx = image.getStackIndex(currentChannel, 1, currentFrame);
		ImageStack stack = image.getStack();
		ImageProcessor ip = stack.getProcessor(stackIx);
		int[] referenceHistogram = ip.getHistogram();
		// Apply histogram to subsequent slices
		for (int sliceIx = 2; sliceIx <= image.getNSlices(); ++sliceIx) {
			stackIx = image.getStackIndex(currentChannel, sliceIx, currentFrame);
			ip = stack.getProcessor(stackIx);
			int[] sliceHistogram = ip.getHistogram();
			int[] intensityMap = new int[sliceHistogram.length];
			int refVal = -1;
			int sliceVal = 0;
			int refCount = 0; // cumulative count of pixels in reference image
			int sliceCount = 0; // cumulative count of pixels in subject image
			while (sliceVal < sliceHistogram.length) {
				sliceCount += sliceHistogram[sliceVal];
				while ((refCount < sliceCount) && (refVal < referenceHistogram.length)) {
					refVal++;
					refCount += referenceHistogram[refVal];
				}
				intensityMap[sliceVal] = refVal;
				sliceVal++;
			}
			// Apply intensity mapping to subject slice
			for (int x = 0; x < ip.getWidth(); ++x) {
				for (int y = 0; y < ip.getHeight(); ++y) {
					int val = ip.getPixel(x, y);
					ip.putPixel(x, y, intensityMap[val]);
				}
			}
			IJ.showProgress(sliceIx, image.getNSlices());
		}
		IJ.showMessage("Stack_Histogram_Matcher", "Histogram matching applied!");
	}

}
