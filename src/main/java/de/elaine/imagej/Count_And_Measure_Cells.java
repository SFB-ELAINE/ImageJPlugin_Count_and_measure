package de.elaine.imagej;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.Thresholder;
import ij.plugin.filter.Binary;
import ij.plugin.filter.EDM;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;

import java.awt.*;

public class Count_And_Measure_Cells implements PlugIn {

	// Main Class called by ImageJ
	public void run(String arg) {

		// Open Image
		ImagePlus originalImage = IJ.getImage();
		
		// Reduce to Red CH
		ImagePlus redChannelImage = getRedChannel(originalImage);
		//redChannelImage.show();

		// Get all red pixels with an intensity higher than 9 (out of 0-255)
		ImagePlus maskedRedChannelImage = applyThreshold(redChannelImage);
		//maskedRedChannelImage.show();
		
		// Fill holes
		IJ.run(maskedRedChannelImage, "Options...", "iterations=1 count=1 black");
		ImagePlus filledMaskedRedChannelImage = applyFillHoles(maskedRedChannelImage);
		//filledMaskedRedChannelImage.show();
		
		// Apply watersheding
		ImagePlus finalRedChannelImage = applyWaterShed(filledMaskedRedChannelImage);
		//finalRedChannelImage.show();

		// Table with results
		ImageWithResultsTable imageWithResultsTable = analyzeParticles(finalRedChannelImage);

		// used to override the close dialog
		//imageWithResultsTable.getImage().changes = false;
		//imageWithResultsTable.getImage().close();
		//imageWithResultsTable.getImage().flush();
		
        //ImagePlus finalImage = printRoiMarkers(originalImage);

        
        //maskedRedChannelImage.flush();
        //filledMaskedRedChannelImage.flush();
        //finalRedChannelImage.flush();
        //originalImage.flush();
        
        //finalImage.show();

        imageWithResultsTable.getResultsTable().show("My Results");

	}

	private ImagePlus getRedChannel(ImagePlus img) {
		ImagePlus[] channels = ChannelSplitter.split(img);
		return channels[0];
	}

	private ImagePlus applyThreshold(ImagePlus img) {
		final int minThreshold = 9;
		final int maxThreshold = 255;

		ByteProcessor byteProcessor = (ByteProcessor) img.getProcessor();
		byte[] pixels = (byte[]) byteProcessor.getPixels();

		for (int i = 0; i < pixels.length; i++) {
			int pixel = pixels[i] & 0xff;
			if (pixel >= minThreshold && pixel <= maxThreshold)
				pixels[i] = (byte) 255;
			else {
				pixels[i] = (byte) 0;
			}
		}

		byteProcessor.setPixels(pixels);
		return img;
	}

	private ImagePlus applyFillHoles(ImagePlus img) { // IJ.run(img, "Fill Holes", "");
		Binary b = new Binary();
		b.setup("fill", null);
		b.run(img.getProcessor());
		return img;
	}

	private ImagePlus applyWaterShed(ImagePlus img) {// IJ.run(img, "Watershed", "");
		EDM e = new EDM();
		e.toWatershed(img.getProcessor());
		return img;
	}
	
	private ImagePlus applyFillHoles2(ImagePlus img) {
        //Can fill larger holes then applyFillHoles

        Binary b = new Binary();
        b.setup("fill", null);
        b.run(img.getProcessor());

        ImagePlus tempImage = img.duplicate();


        b.setup("dilate", null);
        b.run(img.getProcessor());

        b.setup("fill", null);
        b.run(img.getProcessor());

        b.setup("erode", null);
        b.run(img.getProcessor());

        tempImage.setRoi(1, 1, tempImage.getWidth() - 2, tempImage.getHeight() - 2);

        //reevaluate usage of IJ class

        IJ.run(tempImage, "Make Inverse", "");
        IJ.run(tempImage, "Add Selection...", "");

        IJ.run(tempImage, "Copy", "");
        IJ.run(img, "Paste", "");

        img.deleteRoi();
        return img;
    }

	private ImageWithResultsTable analyzeParticles(ImagePlus img) {// IJ.run(img, "Analyze Particles...",
																	// "size=1000-Infinity show=Nothing exclude clear
																	// summarize add");
		ResultsTable resultsTable = new ResultsTable();

		ParticleAnalyzer particleAnalyzer = new ParticleAnalyzer(
				ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.CLEAR_WORKSHEET | ParticleAnalyzer.DISPLAY_SUMMARY
						| ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES | ParticleAnalyzer.SHOW_NONE,
				0, resultsTable, 1000.0, Double.POSITIVE_INFINITY);
		particleAnalyzer.analyze(img);

		return new ImageWithResultsTable(img, resultsTable);
	}
	
	private ImagePlus printRoiMarkers(ImagePlus img) {
        RoiManager rm = RoiManager.getInstance2();
        if (rm == null) rm = new RoiManager(true);
        //rm.setVisible(false);

        Overlay overlay = new Overlay();
        Roi[] rois = rm.getRoisAsArray();
        for (Roi points : rois) {
            overlay.add(points);
        }
        //overlay.setLabelFontSize(30, "background");
        overlay.setLabelColor(Color.YELLOW);
        overlay.drawLabels(true);
        IJ.showMessage("Test");
        img.setOverlay(overlay);

        ImagePlus resultImage = img.flatten();

        img.close();
        img.flush();

        return resultImage;
    }

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */

	public static void main(String[] args) throws Exception {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		// see: https://stackoverflow.com/a/7060464/1207769
		Class<?> clazz = Count_And_Measure_Cells.class;
		java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
		java.io.File file = new java.io.File(url.toURI());
		System.setProperty("plugins.dir", file.getAbsolutePath());

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage(
				"https://github.com/SFB-ELAINE/ImageJPlugin_Count_and_measure/raw/master/images/Spreading3.tif");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}

}
