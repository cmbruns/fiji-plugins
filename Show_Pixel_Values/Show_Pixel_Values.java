import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import java.awt.event.MouseEvent;
import ij.measure.Calibration;

// Prints pixel values as numbers at high zoom levels.
// Christopher Bruns, November 1, 2010
public class Show_Pixel_Values implements PlugIn {

    // Possible label colors
    Color darkGray;
    Color lightGray;
    Color darkRed;
    Color lightRed;
    Color darkGreen;
    Color lightGreen;
    Color darkBlue;
    Color lightBlue;
    
    public void run(String arg) {
        ImagePlus imp = IJ.getImage();
        CanvasWithValueLabels cc = new CanvasWithValueLabels(imp);
        if (imp.getStackSize()>1)
            new StackWindow(imp, cc);
        else
           new ImageWindow(imp, cc);
    }

    // Extend ImageCanvas class to show pixel intensity labels at high magnification
    class CanvasWithValueLabels extends ImageCanvas 
    {
    	
        long myWinSetMaxBoundsTime; // hack to match parent class internals
        private boolean maxBoundsReset;
        Rectangle maxWindowBounds;
       
        CanvasWithValueLabels(ImagePlus imp) 
        {
            super(imp);
            darkGray   = new Color( 30,  30,  30);
            lightGray  = new Color(230, 230, 230);
            darkRed    = new Color( 80,   0,   0);
            lightRed   = new Color(255, 120, 120);
            darkGreen  = new Color(  0,  80,   0);
            lightGreen = new Color(180, 255, 180);
            darkBlue   = new Color(  0,   0,  80);
            lightBlue  = new Color(120, 120, 255);
            
            myWinSetMaxBoundsTime = 0;
	    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    Rectangle maxWindow = ge.getMaximumWindowBounds();
	    maxWindowBounds = maxWindow;
        }

        // override paint() to display pixel value labels at high magnification
        public void paint(Graphics g) {
            super.paint(g);
            drawPixelValues(g);
       }

       // drawPixelValues places text labels over each pixel showing the numeric intensity
       void drawPixelValues(Graphics g) {
           // IJ.log("Magnification = " + magnification);
           // if (magnification < 12)
           //     return;  // Too fine for pixel numbers
           // Font size depends on how many digits we need to cram into pixel area
           float fontSize;
           boolean bIsRGB;
           switch (imp.getType()) {
           	case ImagePlus.COLOR_256:
           	case ImagePlus.COLOR_RGB:
           	    fontSize = (float)magnification / 4.0f;
           	    bIsRGB = true;
           	    break;
           	case ImagePlus.GRAY8:
           	case ImagePlus.GRAY16:
           	case ImagePlus.GRAY32:
           	default:
           	    fontSize = (float)magnification / 2.5f;
           	    bIsRGB = false;
           	    break;
           }

           if (fontSize < 4) // too small to read
               return;
           
           Font numberFont = g.getFont().deriveFont(fontSize);
           g.setFont(numberFont);
           // IJ.log("srcRect.x = " + srcRect.x);
           // For speed, just loop over the visible area only
           for (float x = srcRect.x - 1; x <= srcRect.x + srcRect.width + 1; ++x) {
               for (float y = srcRect.y - 1; y <= srcRect.y + srcRect.height + 1; ++y) {
                   if (bIsRGB) {
                       drawIntensity(g, (int)x, (int)y, 0, 0.40); // red                   	
                       drawIntensity(g, (int)x, (int)y, 1, 0.65); // green            	
                       drawIntensity(g, (int)x, (int)y, 2, 0.90); // blue
                   }
                   else {
                       drawIntensity(g, (int)x, (int)y, 0, 0.65); // gray
                   }
               }
           }
       }

       // drawIntensity(...) overlays one text string showing the intensity value of a particular pixel and channel
       void drawIntensity(Graphics g, int x, int y, int channel, double yOffset) 
       {
           int val = imp.getPixel(x, y)[channel];
           double calVal = imp.getCalibration().getCValue(val);
           String label = Integer.toString((int)calVal);
           // if ((int)calVal < 10) // indent single digits
           //     label = " " + label;
           g.setColor(getLabelColor(calVal, channel));
   	   g.drawString(label, screenXD(x + 0.10), screenYD(y + yOffset));
       }

       // The color of the text displaying the intensity depends on several factors.
       // If the background is light, the text should be dark.
       // If the background is dark, the text should be light.
       // If the image is grayscale, the text should be grayscale.
       // If the image is rgb, the text color should depend on the intensity channel.
       Color getLabelColor(double val, int channel) 
       {
           // default to gray
           Color lightColor = lightGray;
           Color darkColor = darkGray;
           if ( (imp.getType() == ImagePlus.COLOR_RGB) 
             || (imp.getType() == ImagePlus.COLOR_256) ) 
           {
              	if (channel == 0) {
              	    lightColor = lightRed;
              	    darkColor = darkRed;
              	}
              	else if (channel == 1) {
              	    lightColor = lightGreen;
              	    darkColor = darkGreen;
              	}
              	else {
              	    lightColor = lightBlue;
              	    darkColor = darkBlue;
              	}
           }
           double minVal = imp.getDisplayRangeMin();
           double maxVal = imp.getDisplayRangeMax();
           double alpha = (val - minVal) / (maxVal - minVal);
           if (alpha < 0.5)
               return lightColor;
           else
               return darkColor;
       }

       @Override
       public void setMagnification(double magnification) {
		// if (magnification>32.0) magnification = 32.0;
		if (magnification<0.03125) magnification = 0.03125;
		this.magnification = magnification;
		imp.setTitle(imp.getTitle());
	}

       @Override
	public void zoomIn(int sx, int sy) {
		// IJ.log("Zoom in");
		// Remove zoom limit
		// if (magnification>=32) return;
		double newMag = getMyHigherZoomLevel(magnification);
		int newWidth = (int)(imageWidth*newMag);
		int newHeight = (int)(imageHeight*newMag);
		Dimension newSize = canEnlarge(newWidth, newHeight);
		if (newSize!=null) {
			setDrawingSize(newSize.width, newSize.height);
			if (newSize.width!=newWidth || newSize.height!=newHeight)
				adjustSourceRect(newMag, sx, sy);
			else
				setMagnification(newMag);
			imp.getWindow().pack();
		} else
			adjustSourceRect(newMag, sx, sy);
		repaint();
		if (srcRect.width<imageWidth || srcRect.height<imageHeight)
			resetMaxBounds();
	}
	
	/**Zooms out by making the source rectangle (srcRect)  
		larger and centering it on (x,y). If we can't make it larger,  
		then make the window smaller.*/
	@Override
	public void zoomOut(int x, int y) {
		if (magnification<=0.03125)
			return;
		double oldMag = magnification;
		double newMag = getMyLowerZoomLevel(magnification);
		double srcRatio = (double)srcRect.width/srcRect.height;
		double imageRatio = (double)imageWidth/imageHeight;
		double initialMag = imp.getWindow().getInitialMagnification();
		if (Math.abs(srcRatio-imageRatio)>0.05) {
			double scale = oldMag/newMag;
			int newSrcWidth = (int)Math.round(srcRect.width*scale);
			int newSrcHeight = (int)Math.round(srcRect.height*scale);
			if (newSrcWidth>imageWidth) newSrcWidth=imageWidth;
			if (newSrcHeight>imageHeight) newSrcHeight=imageHeight;
			int newSrcX = srcRect.x - (newSrcWidth - srcRect.width)/2;
			int newSrcY = srcRect.y - (newSrcHeight - srcRect.height)/2;
			if (newSrcX<0) newSrcX = 0;
			if (newSrcY<0) newSrcY = 0;
			srcRect = new Rectangle(newSrcX, newSrcY, newSrcWidth, newSrcHeight);
			//IJ.log(newMag+" "+srcRect+" "+dstWidth+" "+dstHeight);
			int newDstWidth = (int)(srcRect.width*newMag);
			int newDstHeight = (int)(srcRect.height*newMag);
			setMagnification(newMag);
			setMaxBounds();
			//IJ.log(newDstWidth+" "+dstWidth+" "+newDstHeight+" "+dstHeight);
			if (newDstWidth<dstWidth || newDstHeight<dstHeight) {
				//IJ.log("pack");
				setDrawingSize(newDstWidth, newDstHeight);
				imp.getWindow().pack();
			} else
				repaint();
			return;
		}
		if (imageWidth*newMag>dstWidth) {
			int w = (int)Math.round(dstWidth/newMag);
			if (w*newMag<dstWidth) w++;
			int h = (int)Math.round(dstHeight/newMag);
			if (h*newMag<dstHeight) h++;
			x = offScreenX(x);
			y = offScreenY(y);
			Rectangle r = new Rectangle(x-w/2, y-h/2, w, h);
			if (r.x<0) r.x = 0;
			if (r.y<0) r.y = 0;
			if (r.x+w>imageWidth) r.x = imageWidth-w;
			if (r.y+h>imageHeight) r.y = imageHeight-h;
			srcRect = r;
		} else {
			srcRect = new Rectangle(0, 0, imageWidth, imageHeight);
			setDrawingSize((int)(imageWidth*newMag), (int)(imageHeight*newMag));
			//setDrawingSize(dstWidth/2, dstHeight/2);
			imp.getWindow().pack();
		}
		//IJ.write(newMag + " " + srcRect.x+" "+srcRect.y+" "+srcRect.width+" "+srcRect.height+" "+dstWidth + " " + dstHeight);
		setMagnification(newMag);
		//IJ.write(srcRect.x + " " + srcRect.width + " " + dstWidth);
		setMaxBounds();
		repaint();
	}


        // hide static method of parent class
        double getMyHigherZoomLevel(double magnification) {
            if (magnification < 32)
                return ImageCanvas.getHigherZoomLevel(magnification);
            return (double)(int)(1.60 * magnification);
        }
        
        // hide static method of parent class
        double getMyLowerZoomLevel(double magnification) {
            if (magnification <= 32)
                return ImageCanvas.getLowerZoomLevel(magnification);
            return (double)(int)(0.625 * magnification);
        }
        
        // copy private methods of parent class
	void adjustSourceRect(double newMag, int x, int y) {
		//IJ.log("adjustSourceRect1: "+newMag+" "+dstWidth+"  "+dstHeight);
		int w = (int)Math.round(dstWidth/newMag);
		if (w*newMag<dstWidth) w++;
		int h = (int)Math.round(dstHeight/newMag);
		if (h*newMag<dstHeight) h++;
		x = offScreenX(x);
		y = offScreenY(y);
		Rectangle r = new Rectangle(x-w/2, y-h/2, w, h);
		if (r.x<0) r.x = 0;
		if (r.y<0) r.y = 0;
		if (r.x+w>imageWidth) r.x = imageWidth-w;
		if (r.y+h>imageHeight) r.y = imageHeight-h;
		srcRect = r;
		setMagnification(newMag);
		//IJ.log("adjustSourceRect2: "+srcRect+" "+dstWidth+"  "+dstHeight);
	}
	void resetMaxBounds() {
		ImageWindow win = imp.getWindow();
		if (win!=null && (System.currentTimeMillis()-myWinSetMaxBoundsTime)>500L) {
			win.setMaximizedBounds(maxWindowBounds);
			maxBoundsReset = true;
			myWinSetMaxBoundsTime = System.currentTimeMillis();
		}
	}
	void setMaxBounds() {
		if (maxBoundsReset) {
			maxBoundsReset = false;
			ImageWindow win = imp.getWindow();
			if (win!=null && !IJ.isLinux() && maxWindowBounds!=null) {
				win.setMaximizedBounds(maxWindowBounds);
				myWinSetMaxBoundsTime = System.currentTimeMillis();
			}
		}
	}
	
    } // CanvasWithValueLabels inner class

}
