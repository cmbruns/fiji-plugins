import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;

public class Pearson_Correlation implements PlugIn {
	public String pluginName = "Pearson Correlation";
	static boolean ignoreZerosOption = true;
	boolean macro = false;
	ImagePlus image1;
	ImagePlus image2;

	public void run(String arg) {
	        macro = ! arg.equals("");
		if (! showDialog()) return;
		float result = exec(image1, image2, ignoreZerosOption);
		IJ.log("Pearson Correlation = "+result);
	}

	public float exec(ImagePlus i1, ImagePlus i2, boolean ignoreZeros) {
		ImageStack stack1 = i1.getStack();
		ImageStack stack2 = i2.getStack();
		int sz = Math.min(stack1.getSize(), stack2.getSize());
		int sy = Math.min(stack1.getHeight(), stack2.getHeight());
		int sx = Math.min(stack1.getWidth(), stack2.getWidth());
		
		// First pass compute mean intensity for each image
		double intensitySum1 = 0;
		double intensitySum2 = 0;
		int count = 0;
		for (int z = 1; z <= sz; z++) {
			ImageProcessor slice1 = stack1.getProcessor(z);
			ImageProcessor slice2 = stack2.getProcessor(z);
			for (int y = 0; y < sy; ++y) {
				for (int x = 0; x < sx; ++x) {
					float intensity1 = slice1.getf(x, y);
					if (ignoreZeros && intensity1 == 0)
						continue;
					float intensity2 = slice2.getf(x, y);
					if (ignoreZeros && intensity2 == 0)
						continue;
					intensitySum1 += intensity1;
					intensitySum2 += intensity2;
					count += 1;
				}
			}
		}
		double mean1 = 0;
		double mean2 = 0;
		if (count > 0) {
			mean1 = intensitySum1 / count;
			mean2 = intensitySum2 / count;
		}

		// Second pass compute correlation coefficient
		double numerator = 0.0;
		double denom1 = 0.0;
		double denom2 = 0.0;
		for (int z = 1; z <= sz; z++) {
			ImageProcessor slice1 = stack1.getProcessor(z);
			ImageProcessor slice2 = stack2.getProcessor(z);
			for (int y = 0; y < sy; ++y) {
				for (int x = 0; x < sx; ++x) {
					float intensity1 = slice1.getf(x, y);
					if (ignoreZeros && intensity1 == 0)
						continue;
					float intensity2 = slice2.getf(x, y);
					if (ignoreZeros && intensity2 == 0)
						continue;
					double d1 = intensity1 - mean1;
					double d2 = intensity2 - mean2;
					numerator += d1 * d2;
					denom1 += d1 * d1;
					denom2 += d2 * d2;
				}
			}
		}
		double r = numerator / Math.sqrt(denom1 * denom2);		
		
		return (float) r; // TODO 
	}
	
	boolean showDialog() {
		int[] wList = WindowManager.getIDList();
	        if (wList==null) {
	            IJ.error("No windows are open.");
	            return false;
	        } else if (wList.length < 2) {
	            IJ.error("Two or more windows must be open");
	            return false;
	        }
	        int nImages = wList.length;
	        
	        String[] titles = new String[nImages];
	        for (int i=0; i<nImages; i++) {
	            ImagePlus imp = WindowManager.getImage(wList[i]);
	            if (imp!=null) {
	                titles[i] = imp.getTitle();
	            } else {
	                titles[i] = "";
	            }
	        }

	        GenericDialog gd = new GenericDialog(pluginName);
	        gd.addChoice("Image1:", titles, titles[0]);
	        gd.addChoice("Image2:", titles, titles[1]);
		gd.addCheckbox("Ignore zero intensities", ignoreZerosOption);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		ignoreZerosOption = gd.getNextBoolean();
		image1 = WindowManager.getImage(wList[gd.getNextChoiceIndex()]);
		image2 = WindowManager.getImage(wList[gd.getNextChoiceIndex()]);
		// TODO
		return true;
	}

}
