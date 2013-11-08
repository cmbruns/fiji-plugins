/**
 * The goal of the Bilateral_Symmetry plugin is to identify the major
 * plane of bilateral symmetry in a three dimensional image stack, such
 * as an image of a fly brain.
 *
 * It is expected that finding the plane of bilateral symmetry will
 * dramatically simplify the Fourier space registration of 3D volumes.
 * Naturally, this approach can only be used for images that possess
 * strong bilateral symmetry, such as whole-brain or whole-animal images.
 *
 * But it looks like the result is not clean enough to identify the mirror plane.  Oh well.
 */

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

import mpicbg.imglib.image.Image;
import mpicbg.imglib.image.ImagePlusAdapter;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.ComplexType;
import mpicbg.imglib.type.numeric.complex.ComplexFloatType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.algorithm.fft.FourierTransform;
import mpicbg.imglib.algorithm.fft.InverseFourierTransform;
import mpicbg.imglib.image.display.imagej.ImageJFunctions;
import mpicbg.imglib.cursor.Cursor;
import mpicbg.imglib.cursor.LocalizableByDimCursor;
import mpicbg.imglib.type.Type;

public class Bilateral_Symmetry<T extends RealType<T> > implements PlugIn {

	protected Image<T> img;

	public void run(String arg) 
	{
		ImagePlus imp = WindowManager.getCurrentImage();
		img = ImagePlusAdapter.wrap(imp);

		// Fourier Transform
		final FourierTransform<T, ComplexFloatType> fft
			= new FourierTransform<T, ComplexFloatType>(img, new ComplexFloatType() );
		final Image<ComplexFloatType> kernelFFT;
		if ( fft.checkInput() && fft.process() )
			kernelFFT = fft.getResult();
		else {
			IJ.write("Cannot complete Fourier transform: " + fft.getErrorMessage() );
			return;
		}

		// Double dimensions of derived FFT
		int[] bilateralDims = new int[kernelFFT.getNumDimensions()];
		kernelFFT.getDimensions(bilateralDims);
		// for (int d = 0; d < kernelFFT.getNumDimensions(); ++d)
		// 	bilateralDims[d] *= 2;
		final Image<ComplexFloatType> bilateralFFT = kernelFFT.createNewImage(bilateralDims);
		// TODO - need a cursor that counts monotonically downward in absolute value of h,k,l Fourier indices.
		LocalizableByDimCursor<ComplexFloatType> cursor = bilateralFFT.createLocalizableByDimCursor();
		LocalizableByDimCursor<ComplexFloatType> cursor2 = kernelFFT.createLocalizableByDimCursor();
		int[] hkl1 = new int[cursor.getNumDimensions()];
		int[] hkl2 = new int[cursor.getNumDimensions()];
		ComplexFloatType val = new ComplexFloatType();
		ComplexFloatType val2 = new ComplexFloatType();
		while ( cursor.hasNext() )
		{
			cursor.fwd();

			val = cursor.getType();
			val.set(0f,0f);

			// TODO - set higher order coefficient
			// TODO - generalize to 3 or 2 dimensions
			
			cursor.getPosition(hkl1);
			// Transform image indices to fourier indices
			boolean bZero = false;
			for (int d = 0; d < cursor.getNumDimensions(); ++d) {
				if (d == 0) // x axis is a half plane
					hkl1[d] -= (cursor.getDimensions()[d] - 1);
				else
					hkl1[d] -= cursor.getDimensions()[d]/2;

				// Zero out coefficients with odd indices
				if (hkl1[d] % 2 != 0)
					bZero = true;
			}
			if (!bZero) {
				// TODO set from other image
				for (int d = 0; d < cursor.getNumDimensions(); ++d) {
					hkl2[d] = hkl1[d] / 2; // result = double input frequency
					// convert back to image coordinates
					if (d == 0)
						hkl2[d] += (cursor2.getDimensions()[d] - 1);
					else
						hkl2[d] += cursor2.getDimensions()[d]/2;
					cursor2.setPosition(hkl2);
					val2 = cursor2.getType();
					double real2 = val2.getRealDouble();
					double imag2 = val2.getComplexDouble();
					double phase = Math.atan2(imag2, real2);
					double amplitude = Math.sqrt(real2*real2 + imag2*imag2);
					phase *= 2;
					double real = Math.cos(phase) * amplitude;
					double imag = Math.sin(phase) * amplitude;
					val.set((float)real, (float)imag);
				}
			}
		}

		// ImageJFunctions.copyToImagePlus( bilateralFFT ).show();
		
		// Inverse FFT
	        // compute inverse fourier transform of the kernel
	        final InverseFourierTransform< FloatType, ComplexFloatType > ifft = 
	        	new InverseFourierTransform< FloatType, ComplexFloatType >( bilateralFFT, fft, new FloatType() );
	        final Image< FloatType > kernelInverse;
	        if ( ifft.checkInput() && ifft.process() )
	                kernelInverse = ifft.getResult();
	        else
	        {
				IJ.write("Cannot complete inverse Fourier transform: " + 
						ifft.getErrorMessage() );
				return;
	        }
		
		ImageJFunctions.copyToImagePlus( kernelInverse ).show();
	}
}
