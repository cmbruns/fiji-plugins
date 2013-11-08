import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import ij.*;

/**
 * This is a template for a plugin that requires one image to
 * be opened, and takes it as parameter.
 */
public class Circle_Blur_Mask implements PlugInFilter {
	protected ImagePlus image;

	/**
	 * This method gets called by ImageJ / Fiji to determine
	 * whether the current image is of an appropriate type.
	 *
	 * @param arg can be specified in plugins.config
	 * @param image is the currently opened image
	 */
	public int setup(String arg, ImagePlus image) {
		this.image = image;
		/*
		 * The current return value accepts all gray-scale
		 * images (if you access the pixels with ip.getf(x, y)
		 * anyway, that works quite well.
		 *
		 * It could also be DOES_ALL; you can add "| NO_CHANGES"
		 * to indicate that the current image will not be
		 * changed by this plugin.
		 *
		 * Beware of DOES_STACKS: this will call the run()
		 * method with all slices of the current image
		 * (channels, z-slices and frames, all). Most likely
		 * not what you want.
		 */
		return DOES_8G | DOES_16 | DOES_32;
	}

	/**
	 * This method is run when the current image was accepted.
	 *
	 * @param ip is the current slice (typically, plugins use
	 * the ImagePlus set above instead).
	 */
	public void run(ImageProcessor ip) {
	    ImageStatistics stats = ip.getStatistics();
	    // IJ.log("Mean intensity = " + stats.mean);
	    double avg = stats.mean;
	    int intAvg = (int)avg;
	    double centerX = ip.getWidth() / 2.0;
	    double centerY = ip.getHeight() / 2.0;
	    double blurRadiusMax = Math.min(centerX, centerY);
	    double blurRadiusMin = blurRadiusMax - 50;	    
	    double blurRadiusMaxSquared = blurRadiusMax * blurRadiusMax;
	    double blurRadiusMinSquared = blurRadiusMin * blurRadiusMin;	    
	    for (int x = 0; x < ip.getWidth(); ++x) {
			for (int y = 0; y < ip.getHeight(); ++y) {
				int val = ip.getPixel(x, y);
				double dx = x - centerX;
				double dy = y - centerY;
				double radiusSquared = dx*dx + dy*dy;
				// Outside of region, color everything by average value
				if (radiusSquared > blurRadiusMaxSquared) {
					ip.putPixelValue(x, y, intAvg);
				}
				else if (radiusSquared > blurRadiusMinSquared) {
					double radius = Math.sqrt(radiusSquared);
					double alpha1 = (radius - blurRadiusMin) / (blurRadiusMax - blurRadiusMin);
					// smooth derivative using cosine
					double alpha2 = -0.5 * Math.cos(alpha1 * Math.PI) + 0.5;
					double dVal = alpha2 * avg + (1 - alpha2) * ip.getPixelValue(x, y);
					ip.putPixelValue(x, y, (int)dVal);
				}
			}
	    }
	}
}
