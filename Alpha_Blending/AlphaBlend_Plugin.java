import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import java.lang.Number;

public class AlphaBlend_Plugin implements ExtendedPlugInFilter {
	private static int FLAGS =
        DOES_ALL |              //this plugin processes 8-bit, 16-bit, 32-bit gray & 24-bit/pxl RGB
        STACK_REQUIRED;
	ImagePlus imp;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return FLAGS;
	}

	@Override
	public void run(ImageProcessor ip) {
		ImageStack stack = imp.getStack();
		byte[] pixels;
		int dimension = ip.getWidth() * ip.getHeight();
		byte[] mipPixels = new byte[dimension];
		BlendFunc<Byte> blendFunc = new MipFunc<Byte>();
		for (int i = stack.getSize(); i >= 1; --i) {
			pixels = (byte[]) stack.getPixels(i);
			for (int j = 0; j < dimension; ++j) {
				mipPixels[j] = blendFunc.compute(pixels[j], mipPixels[j]);
			}
		}
		imp.setSlice(stack.getSize());
		stack.addSlice("Blended", mipPixels);
		imp.setSlice(stack.getSize());
	}

	@Override
	public void setNPasses(int nPasses) {}

	@Override
	public int showDialog (ImagePlus imp, String command, PlugInFilterRunner pfr) {
		return FLAGS;
	}
	
	interface BlendFunc<E extends Number> {
		E compute(E src, E dest);
	}

	class MipFunc<E extends Number> implements BlendFunc<E> {
		@Override
		public E compute(E src, E dest) {
			return src.doubleValue() > dest.doubleValue() ? src : dest;
		}
	}

}
