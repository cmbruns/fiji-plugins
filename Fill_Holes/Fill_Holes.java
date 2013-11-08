import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;

public class Fill_Holes implements PlugInFilter {
	ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		ImageStack stack = imp.getStack();
		// Keep three slices in memory at one time
		ImageProcessor previousSlice = null;
		ImageProcessor currentSlice = null;
		ImageProcessor nextSlice = null;
		// Read one slice at a time
		for (int i=1;i<=stack.getSize();i++) {
			nextSlice = stack.getProcessor(i);
			fillOneSlice(previousSlice, currentSlice, nextSlice);
			// Update for next iteration
			previousSlice = currentSlice;
			currentSlice = nextSlice;
			// if (i > 1) return; // stop after one slice for debugging
		}
		nextSlice = null;
		fillOneSlice(previousSlice, currentSlice, nextSlice); // Finish final slice
	}

	private void fillOneSlice(ImageProcessor previousSlice, ImageProcessor currentSlice, ImageProcessor nextSlice)
	{
		if (currentSlice == null)
			return;
		int w = currentSlice.getWidth(), h = currentSlice.getHeight();
		int offset[] = new int[w]; // store offset to closest non-zero pixel in scan line
		float intensities[] = new float[w];
		for (int i = 0; i < h; i++) {
		    // Multiple passes over scan line
		    // TODO - the problem with this approach is that, while we want to fill holes
		    // from the outside in, this techniques does so in one dimension only.
		    // Perhaps we could mark each zero value by distance to closest non-zero value,
		    // then fill them in so that smaller distances are always filled in first.
		    //
		    // PASS 1
		    // store offset to closest non-zero pixel in scan line, in two passes
		    int currentOffset = -w; // start extreme, because we have not seen any non-zero pixels yet
		    boolean hasZeros = false;
		    for (int j = 0; j < w; j++) { // left to right first pass
		    	intensities[j] = currentSlice.getf(j, i);
		    	if (intensities[j] == 0) {
		    		currentOffset -= 1;
		    		hasZeros = true;
		    	}
		    	else
		    		currentOffset = 0; // no offset, this is a non-zero pixel
		    	offset[j] = currentOffset;
		    	// if (currentOffset != 0) IJ.log(""+j+", "+offset[j]);
		    }
		    if (! hasZeros)
		    	continue;
		    // PASS 2
		    currentOffset = w; // extreme value again, until we find a non-zero pixel
		    for (int j = w - 1; j >= 0; j--) { // right to left second pass
		    	if (offset[j] == 0) {
		    		currentOffset = 0; // non-zero pixel
		    		continue;
		    	}
		    	currentOffset += 1; // another zero pixel
		    	// Only update offset if non-zero pixel to right is closer than one to left
		    	if (currentOffset >= -offset[j])
		    		continue; // Left to right was better than this way
		    	offset[j] = currentOffset;
		    	// if (currentOffset != 0) IJ.log(""+j+", "+offset[j]);
		    	if (j >= w-1)
		    		continue; // should not happen...
			intensities[j] = fillOnePixel(j, i, intensities[j+1], previousSlice, currentSlice, nextSlice);
		    }
		    // PASS 3
		    // Left to right intensity update
		    for (int j = 0; j < w; j++) { // left to right first pass
		    	if (offset[j] >= 0)
		    		continue; // Should be handled right to left
		    	if (j <= 1)
		    		continue; // Should not happen...
		    	intensities[j] = fillOnePixel(j, i, intensities[j-1], previousSlice, currentSlice, nextSlice);
		    }
		    // return; // stop after one scan line for debugging
		}
		
		// currentSlice.invert(); // debugging, just for showing that plugin runs
	}

	private float fillOnePixel(int x, int y, float neighborIntensity, ImageProcessor previousSlice, ImageProcessor currentSlice, ImageProcessor nextSlice)
	{
		// TODO - average values of adjacent non-zero pixels
		// TODO - blend in median intensity
		float result = neighborIntensity; // TODO
		currentSlice.setf(x, y, result);
		return result;
	}

}
