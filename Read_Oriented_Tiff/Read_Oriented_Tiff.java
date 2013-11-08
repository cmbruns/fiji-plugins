import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.io.*;
import java.io.*;

public class Read_Oriented_Tiff implements PlugIn {

	public void run(String arg) 
	{
		// IJ.showMessage("My_Plugin","Hello world!");
		
		OpenDialog od = new OpenDialog("Open oriented Tiff image...", arg);
		String dir = od.getDirectory();
		String name = od.getFileName();
		if (name == null)
			return;
		IJ.showStatus("Opening: " + dir + name);
		try {
			FileInfo[] fi =  getFileInfo(dir, name);
			IJ.showMessage(fi[0].info);
			openStacks(fi);
		} catch (IOException e) {
			String msg = e.getMessage();
			if (msg==null || msg.equals(""))
				msg = ""+e;
			IJ.showMessage("Read Oriented Tiff", msg);
		}
	}

	FileInfo[] getFileInfo(String directory, String name) throws IOException 
	{
		OrientedTiffDecoder td = new OrientedTiffDecoder(directory, name);
		if (IJ.debugMode) td.enableDebugging();
		FileInfo[] info = td.getTiffInfo();
		if (info==null)
			throw new IOException("This file does not appear to be in TIFF format.");
		if (IJ.debugMode) // dump tiff tags
			IJ.write(info[0].info);
		return info;
	}

	void openStacks(FileInfo[] fi) throws IOException 
	{
		int nImages = 0;
		for (FileInfo fileInfo : fi) {
			// IJ.showMessage("frameInterval = " + fileInfo.frameInterval);
			IJ.showMessage("nImages = " + fileInfo.nImages);
			if (fileInfo.nImages > 1) {
				nImages += fileInfo.nImages;
				break;
			}
			nImages ++;
		}
	}

	class OrientedTiffDecoder extends TiffDecoder {
		public OrientedTiffDecoder(String directory, String name) {
			super(directory, name);
		}
	}
}
