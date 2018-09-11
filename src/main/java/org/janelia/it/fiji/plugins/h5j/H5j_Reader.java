/*
 * Copyright 2018 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms ( http://license.janelia.org/license/jfrc_copyright_1_1.html ).
 */

package org.janelia.it.fiji.plugins.h5j;

import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.janelia.it.h5j.fiji.adapter.FijiAdapter;

/**
 * Reader for HHMI HDF 5 reader.  Consumes H.265-compressed data.
 *
 * @author fosterl
 */



public class H5j_Reader extends ImagePlus implements PlugIn {
    
    private static final String MESSAGE_PREFIX = "HHMI_H5J_Reader: ";
    private static final String EXTENSION = ".h5j";
    public static final String INFO_PROPERTY = "Info";
    private static final boolean HYPERSTACK = true;
    
    private boolean asImage = false;
    private boolean asHyperstack = HYPERSTACK;

    @Override
    public void run(String string) {
        // Do nothing, if this is a headless environment.
    	/*
    	try {
    		String ffmpeg_dir = IJ.getDirectory("startup") + "ffmpeg/";
    		URLClassLoader cl = (URLClassLoader) ClassLoader.getSystemClassLoader();
        	Class<?> ccl = URLClassLoader.class;
        	Method methodAddUrl = ccl.getDeclaredMethod("addURL", URL.class);
        	methodAddUrl.setAccessible(true);
        	methodAddUrl.invoke(cl, new File(ffmpeg_dir+"javacpp-1.4.1").toURI().toURL());
        	methodAddUrl.invoke(cl, new File(ffmpeg_dir+"ffmpeg-4.0-1.4.2-macosx-x86_64.jar").toURI().toURL());
        	methodAddUrl.invoke(cl, new File(ffmpeg_dir+"ffmpeg-4.0-1.4.2.jar").toURI().toURL());
        	URL[] urls = cl.getURLs();
        	for (URL u : urls) {
        		System.out.println(u.toString());
        	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	*/
        if (GraphicsEnvironment.isHeadless()) {
            IJ.showMessage(MESSAGE_PREFIX + "this plugin will only display a graphical file.  Using it in headless mode is meaningless.");
            return;
        }
        else {
            try {
                File infile = ensureFileAvailable(string);
                if (infile != null) {
                    FijiAdapter adapter = new FijiAdapter();
                    ImagePlus infileImage = null;
                    if (asHyperstack) {
                        infileImage = adapter.getMultiChannelImagePlus(infile);
                    } else {
                        infileImage = adapter.getImagePlus(infile);
                    }
                    if (asImage) {
                        // This reader will act as the image. Its 'show' will
                        // be the natural flow of this type of plugin.
                        cloneStack(infileImage, this, asHyperstack);
                    } else {
                        infileImage.show();
                    }
                }
                
            } catch (Exception ex) {
                ex.printStackTrace();
                IJ.showMessage(MESSAGE_PREFIX + "Problem reading file data.  Messsage is '" + ex.getLocalizedMessage() + "'.");
            }
        }
    }
    
    /**
     * Tell this image to be produced as a separation of channels, or
     * as a merging of all channels.
     * 
     * @param asHyperstack T=separate/F=merge
     */
    public void setAsHyperStack(boolean asHyperstack) {
        this.asHyperstack = asHyperstack;
    }
    
    /**
     * This can be used to clone one stack into another.
     * 
     * @param infileImage
     * @param target 
     */
    private void cloneStack(ImagePlus infileImage, ImagePlus target, boolean hyperStack) {
        // Now, 'clone' this image plus back to the one implemented
        // by this plugin.
        target.setStack(infileImage.getTitle(), infileImage.getStack());
        target.setCalibration(infileImage.getCalibration());
        Object objInfo = infileImage.getProperty(INFO_PROPERTY);
        if (objInfo != null) {
            target.setProperty(INFO_PROPERTY, objInfo);
        }
        target.setFileInfo(infileImage.getFileInfo());
        if (hyperStack) {
            target.setDimensions(infileImage.getNChannels(), infileImage.getNSlices(), infileImage.getNFrames());
        }
        target.setOpenAsHyperStack(hyperStack);

    }
    
    private File ensureFileAvailable( String putativeFilePath ) {        
        File rtnVal = null;
        try {
            if (putativeFilePath == null || putativeFilePath.trim().equals("")) {
                rtnVal = showFileChooser();
                asImage = false;
            }
            else {
                rtnVal = new File(putativeFilePath);
                if (rtnVal.canRead()) {
                    asImage = true;
                } else {
                    rtnVal = showFileChooser();
                    asImage = false;
                }
            }
            
        } catch ( Exception ex ) {
            IJ.showMessage(MESSAGE_PREFIX+"unable to open " + putativeFilePath);
        }
        
        return rtnVal;
    }
    
    /**
     * Capture user's file choice.  Forces the hyperstack option off, so that
     * fully-merged files are shown.
     * 
     * @return user-chosen file.
     * @throws HeadlessException 
     */
    private File showFileChooser() throws HeadlessException {
        // Will replace the file with a user input.
        try {
            OpenDialog od = new OpenDialog("H5J Reader");
            String dir = od.getDirectory();
    		String filename = od.getFileName();
    		if (filename == null)
    			return null;
    		String path = dir + filename;
            File rtnVal = new File(path);
            return rtnVal;
        } catch (Exception ex) {
            IJ.error("Exception encountered: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }
    
}
