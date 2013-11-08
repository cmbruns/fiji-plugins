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
        float[][] slicePixels;
        int dimension = ip.getWidth() * ip.getHeight();
        int sx = ip.getWidth();
        int sy = ip.getHeight();
        int sc = imp.getNChannels();
        int sz = stack.getSize()/sc;
        IJ.log("nChannels = "+sc+"; nSlices = "+sz);

        BlendFunc blendFunc = 
            // new MipFunc();
            new LimitedAlphaFunc();
            // new AlphaFunc();

        ImagePlus blended = NewImage.createImage(
            "Blended", 
            sx, sy, sc, 
            16,
            NewImage.FILL_BLACK);
        if (sc > 1) {
            blended.setDimensions(sc, 1, 1);
            blended = new CompositeImage(blended, CompositeImage.COMPOSITE);
            blended.setOpenAsHyperStack(true);
        }
        
        FloatProcessor scratch = new FloatProcessor(sx, sy);
        // start at back slice, for painters' algorithm
        for (int z = sz; z >= 1; --z) { // start at back
        // for (int z = 1; z <= sz; ++z) { // start at front
            for (int c = 0; c < sc; ++c) {
                int i = sc*(z-1) + c;
                ImageProcessor srcSlice = stack.getProcessor(i+1);
                FloatProcessor srcChannel = srcSlice.toFloat(1, scratch);
                blended.setPosition(c+1,1,1); // one-based
                ImageProcessor destChannel = blended.getChannelProcessor();
                for (int y = 0; y < sy; ++y) {
                    for (int x = 0; x < sx; ++x) {
                        boolean debug = true;
                        if (x != 364) debug = false;
                        if (y != 153) debug = false;
                        if (c != 1) debug = false;
                        float src = srcChannel.getf(x, y);
                        float dest = destChannel.getf(x, y);
                        destChannel.setf(x, y, blendFunc.compute(src, dest, debug));
                    }
                }
            }
            IJ.showProgress(sz-z+1, sz-1+1);
        }
        blended.show();
        // imp.setSlice(stack.getSize());
        // stack.addSlice("Blended", mipPixels);
        // imp.setSlice(stack.getSize());
    }

    @Override
    public void setNPasses(int nPasses) {}

    @Override
    public int showDialog (ImagePlus imp, String command, PlugInFilterRunner pfr) {
        return FLAGS;
    }
    
    interface BlendFunc {
        float compute(float src, float dest, boolean debug);
    }

    class MipFunc implements BlendFunc {
        @Override
        public float compute(float src, float dest, boolean debug) {
            return src > dest ? src : dest;
        }
    }

    class LimitedAlphaFuncWrong implements BlendFunc {
        @Override
        public float compute(float src, float dest, boolean debug) {
            float m = 2.0f;
            float z = 1.0f/m;
            float o = z*src;
            if (dest > 0)
                o += (dest/(dest+(z/(m-1))*src))*dest;
            if (debug) 
                IJ.log("s,d,o = "+src+","+dest+","+o);
            return o;
        }
    }

    class LimitedAlphaFunc implements BlendFunc {
        @Override
        public float compute(float src, float dest, boolean debug) {
            float m = 10.0f;
            float z = 1.0f/m;
            float o = z*src;
            if (dest > 0)
                o += (dest/(dest+src*m*z/(m-1)))*dest;
            return o;
        }
    }

    class AlphaFunc implements BlendFunc {
	    @Override
	    public float compute(float src, float dest, boolean debug) {
	        float max = 255.0f;
	        float fog = 1.0f;
	        float k = fog * (1.0f - src/max);
	        float o = src + k*k * dest;
	        return o;
	    }
    }

}
