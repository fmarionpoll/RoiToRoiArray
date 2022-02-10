package plugins.fmp.fmpSequence;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import icy.canvas.Canvas2D;
import icy.common.exception.UnsupportedFormatException;
import icy.gui.dialog.LoaderDialog;
import icy.gui.dialog.MessageDialog;
import icy.gui.viewer.Viewer;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.image.ImageUtil;
import icy.main.Icy;
import icy.math.ArrayMath;
import icy.sequence.MetaDataUtil;
import icy.sequence.Sequence;
import icy.system.thread.ThreadUtil;
import icy.type.collection.array.Array1DUtil;

import ome.xml.meta.OMEXMLMetadata;
import plugins.fmp.fmpTools.EnumImageOp;
import plugins.fmp.fmpTools.ImageOperationsStruct;
import plugins.fmp.fmpTools.StringSorter;
import plugins.stef.importer.xuggler.VideoImporter;




public class SequenceVirtual extends Sequence 
{
	protected VideoImporter importer 		= null;
	private String [] 		listFiles 		= null;
	private String 			csFileName 		= null;
	private final static String[] acceptedTypes = {".jpg", ".jpeg", ".bmp", "tiff"};
	private String			directory 		= null;
	public IcyBufferedImage refImage 		= null;
	
	public long				analysisStart 	= 0;
	public long 			analysisEnd		= 99999999;
	public int 				analysisStep 	= 1;
	public int 				currentFrame 	= 0;
	public int				nTotalFrames 	= 0;
	
	public VImageBufferThread bufferThread 	= null;
	public boolean			bBufferON 		= false;
	public EnumStatus 		status			= EnumStatus.REGULAR;
	
	public Capillaries 		capillaries 	= new Capillaries();
	public Cages			cages 			= new Cages();
	
	public String [] 		seriesname 		= null;
	public int [][] 		data_raw 		= null;
	public double [][] 		data_filtered 	= null;
	
	// image cache
	public IcyBufferedImage 		cacheTransformedImage = null;
	public ImageOperationsStruct 	cacheTransformOp = new ImageOperationsStruct();
	public IcyBufferedImage 		cacheThresholdedImage = null;
	public ImageOperationsStruct 	cacheThresholdOp = new ImageOperationsStruct();
	
	// ----------------------------------------
	public SequenceVirtual () {
		status = EnumStatus.REGULAR;
	}
	
	
	public SequenceVirtual(String name, IcyBufferedImage image) {
		super (name, image);
	}
	
	public SequenceVirtual (Sequence seq) {
		int nfiles = seq.getSizeT();
		String fileList[] = new String[nfiles];
		String directory = null;
		for(int i=0; i < nfiles; i++) {
			String firstfile = seq.getFilename (0, 0, 0);
			Path path = Paths.get(firstfile);
			if (i == 0) 
				directory = path.getParent().toString();
			String filename = path.getFileName().toString();
			fileList[i] = filename;
		}
		
		loadSequenceVirtual(fileList, directory);
		filename = directory + ".xml";		
	}

	public SequenceVirtual (String csFile) {
		loadSequenceVirtualAVI(csFile);
	}

	public SequenceVirtual (String [] list, String directory) {
		loadSequenceVirtual(list, directory);
		filename = directory + ".xml";
	}

	public static boolean acceptedFileType(String name) {
		/* 
		 * Returns true if 'name' includes one of the accepted types stored in the "accepted" list 
		 */
		if (name==null) return false;
		for (int i=0; i<acceptedTypes.length; i++) {
			if (name.endsWith(acceptedTypes[i]))
				return true;
		}
		return false;
	}	

	@Override
	public void close() {
		vImageBufferThread_STOP();
		super.close();
	}
	
	public void displayRelativeFrame( int nbFrame ) {
		int currentTime = getT()+ nbFrame ;
		if (currentTime < 0)
			currentTime = 0;
		if (currentTime > nTotalFrames-1)
			currentTime = (int) (nTotalFrames -1);

		final Viewer v = Icy.getMainInterface().getFirstViewer(this);
		if (v != null) 
			v.setPositionT(currentTime);
		displayImageAt(currentTime);
	}

	public void displayImageAt(int t) {
		currentFrame = t;
		if (getImage(t, 0) == null)
		{
			final boolean wasEmpty = getNumImage() == 0;
			setCurrentVImage (t);
			if (wasEmpty)
			{
				for (Viewer viewer : getViewers())
				{
					if (viewer.getCanvas() instanceof Canvas2D)
						((Canvas2D) viewer.getCanvas()).fitCanvasToImage();
				}
			}
		}
	}

	public String getDirectory () {
		return directory;
	}

	@Override
	public IcyBufferedImage getImage(int t, int z, int c) {
		setVImageName(t);
		IcyBufferedImage image =  loadVImage(t, z);
		if (image != null && c != -1)
			image = IcyBufferedImageUtil.extractChannel(image, c);
		return image;
	}

	@Override
	public IcyBufferedImage getImage(int t, int z) {
		IcyBufferedImage image;
		if (t == currentFrame) {
			image = super.getImage(t, z);
		}
		else {
		  	image =  loadVImage(t, z);
		}
		setVImageName(t);
		return image;
	}
	
	public IcyBufferedImage getImageTransf(int t, int z, int c, EnumImageOp transformop) {
		IcyBufferedImage image =  loadVImageAndSubtractReference(t, transformop);
		if (image != null && c != -1)
			image = IcyBufferedImageUtil.extractChannel(image, c);
		return image;
	}
	
	public IcyBufferedImage loadVImageAndSubtractReference(int t, EnumImageOp transformop) {
		IcyBufferedImage ibufImage = loadVImage(t);
		switch (transformop) {
			case REF_PREVIOUS: // subtract image n-analysisStep
			{
				int t0 = t-analysisStep;
				if (t0 <0)
					t0 = 0;
				IcyBufferedImage ibufImage0 = loadVImage(t0);
				ibufImage = subtractImages (ibufImage, ibufImage0);
			}	
				break;
			case REF_T0: // subtract reference image
			case REF:
				if (refImage == null)
					refImage = loadVImage((int) analysisStart);
				ibufImage = subtractImages (ibufImage, refImage);
				break;

			case NONE:
			default:
				break;
		}
		return ibufImage;
	}
		
	public String[] getListofFiles() {
		return listFiles;
	}

	/*
	 * getSizeT (non-Javadoc)
	 * @see icy.sequence.Sequence#getSizeT()
	 * getSizeT is used to evaluate if volumetric images are stored in the sequence
	 * the "current" virtual version of SequenceVirtual (as of 18-oct-2018) does not support volumetric images 
	 */
	@Override
	public int getSizeT() {
		int nframes = 0;
		if (status == EnumStatus.REGULAR)
			nframes = super.getSizeT();
		else 
			nframes = (int) nTotalFrames;
		return nframes;
	}

	public int getT() {
		return currentFrame;
	}

	public double getVData(int t, int z, int c, int y, int x) {
		final IcyBufferedImage img = loadVImage(t);
		if (img != null)
			return img.getData(x, y, c);
		return 0d;
	}

	public String getDecoratedImageName(int t) {
		String csTitle = "["+t+ "/" + nTotalFrames + " V] : ";
		if (status == EnumStatus.FILESTACK) 
			csTitle += listFiles[t];
		else //  if ((status == EnumStatus.AVIFILE))
			csTitle += csFileName;
		return csTitle;
	}

	public String getFileName(int t) {
		String csName = null;
		if (status == EnumStatus.FILESTACK) 
			csName = listFiles[t];
		else if (status == EnumStatus.AVIFILE)
			csName = csFileName;
		return csName;
	}
	
	public boolean isFileStack() {
		return (status == EnumStatus.FILESTACK);
	}
		
	public IcyBufferedImage loadVImage(int t, int z) {
		IcyBufferedImage ibufImage = super.getImage(t, z);
		// not found : load from file
		if (ibufImage == null) 
			ibufImage = loadVImageFromFile (t);

		return ibufImage;
	}
	
	public IcyBufferedImage loadVImage(int t) {
		IcyBufferedImage ibufImage = super.getImage(t, 0);
		// not found : load from file
		if (ibufImage == null)
			return loadVImageFromFile (t);
		return ibufImage;
	}
	
	private IcyBufferedImage loadVImageFromFile(int t) {
		BufferedImage buf =null;
		if (status == EnumStatus.FILESTACK) {
			buf = ImageUtil.load(listFiles[t]);
			ImageUtil.waitImageReady(buf);
			if (buf == null)
				return null;
							
		}
		else if (status == EnumStatus.AVIFILE) {
			try {
				buf = importer.getImage(0, t);
			} catch (UnsupportedFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// --------------------------------
//		setImage(t, 0, buf);
		return IcyBufferedImage.createFrom(buf);
	}
	
	public boolean setCurrentVImage(int t) {
		BufferedImage bimage = loadVImage(t);
		if (bimage == null)
			return false;

		super.setImage(t, 0, bimage);
		setVImageName(t);		
		currentFrame = t;
		return true;
	}

	@Override
	public void setImage(int t, int z, BufferedImage bimage) throws IllegalArgumentException {
		/* setImage overloaded
		 * caveats: 
		 * (1) this routine deals only with 2D images i.e. z is not used (z= 0), 
		 * (2) the virtual stack is left untouched - no mechanism is provided to "save" modified images to the disk - so actually
		 * 	   setImage here is equivalent to "load image" from disk - the buffered image parameter is not used if the stack is virtual
		 * @see icy.sequence.Sequence#setImage(int, int, java.awt.image.BufferedImage)
		 */
		
		if ((status == EnumStatus.FILESTACK) || (status == EnumStatus.AVIFILE) )
			setCurrentVImage(t);
		else 
			super.setImage(t, z, bimage);
	}

	public void setVImage(int t) {
		IcyBufferedImage ibuf = loadVImage(t);
		if (ibuf != null)
			super.setImage(t, 0, ibuf);
	}

	public String[] keepOnlyAcceptedNames(String[] rawlist) {
		// -----------------------------------------------
		// subroutines borrowed from FolderOpener
		/* Keep only "accepted" names (file extension)*/
		int count = 0;
		for (int i=0; i< rawlist.length; i++) {
			String name = rawlist[i];
			if ( !acceptedFileType(name) )
				rawlist[i] = null;
			else
				count++;
		}
		if (count==0) return null;

		String[] list = rawlist;
		if (count<rawlist.length) {
			list = new String[count];
			int index = 0;
			for (int i=0; i< rawlist.length; i++) {
				if (rawlist[i]!=null)
					list[index++] = rawlist[i];
			}
		}
		return list;
	}

	public void vImageBufferThread_START (int numberOfImageForBuffer) {
		vImageBufferThread_STOP();

		bufferThread = new VImageBufferThread(this, numberOfImageForBuffer);
		bufferThread.setName("Buffer Thread");
		bufferThread.setPriority(Thread.NORM_PRIORITY);
		bufferThread.start();
	}
	
	public void vImageBufferThread_STOP() {
		if (bufferThread != null)
		{
			bufferThread.interrupt();
			try {
				bufferThread.join();
			}
			catch (final InterruptedException e1) { e1.printStackTrace(); }
		}
		// TODO clean buffer by removing images?
	}

	public void cleanUpBufferAndRestart() {
		if (bufferThread == null)
			return;
		int depth = bufferThread.getFenetre();
		vImageBufferThread_STOP();
		for (int t = 0; t < nTotalFrames-1 ; t++) {
			removeImage(t, 0);
		}
		vImageBufferThread_START(depth);
	}
	
	public class VImageBufferThread extends Thread {

		/**
		 * pre-fetch files / companion to SequenceVirtual
		 */

		private int fenetre = 20; // 100;
		private int span = fenetre/2;

		public VImageBufferThread() {
			bBufferON = true;
		}

		public VImageBufferThread(SequenceVirtual vseq, int depth) {
			fenetre = depth;
			span = fenetre/2 * analysisStep;
			bBufferON = true;
		}
		
		public void setFenetre (int depth) {
			fenetre = depth;
			span = fenetre/2 * analysisStep;
		}

		public int getFenetre () {
			return fenetre;
		}
		public int getStep() {
			return analysisStep;
		}

		public int getCurrentBufferLoadPercent()
		{
			int currentBufferPercent = 0;
			int frameStart = currentFrame-span; 
			int frameEnd = currentFrame + span;
			if (frameStart < 0) 
				frameStart = 0;
			if (frameEnd >= (int) nTotalFrames) 
				frameEnd = (int) nTotalFrames-1;

			float nbImage = 1;
			float nbImageLoaded = 1;
			for (int t = frameStart; t <= frameEnd; t+= analysisStep) {
				nbImage++;
				if (getImage(t, 0) != null)
					nbImageLoaded++;
			}
			currentBufferPercent = (int) (nbImageLoaded * 100f / nbImage);
			return currentBufferPercent;
		}

		@Override
		public void run()
		{
			try
			{
				while (!isInterrupted())
				{
					ThreadUtil.sleep(100);

					int frameStart 	= currentFrame - span;
					int frameEnd 	= currentFrame + span;
					if (frameStart < 0) 
						frameStart = 0;
					if (frameEnd > nTotalFrames) 
						frameEnd = nTotalFrames;
			
					// clean all images except those within the buffer 
					for (int t = 0; t < nTotalFrames-1 ; t+= analysisStep) { // t++) {
						if (t < frameStart || t > frameEnd)
							removeImage(t, 0);
						
						if (isInterrupted())
							return;
					}
					
					for (int t = frameStart; t < frameEnd ; t+= analysisStep) {	
						setVImage(t);
						if (isInterrupted())
							return;
					}
				}			
			}
			catch (final Exception e) 
			{ e.printStackTrace(); }
		}
	}

	public IcyBufferedImage subtractImages (IcyBufferedImage image1, IcyBufferedImage image2) 
	{
	
		IcyBufferedImage result = new IcyBufferedImage(image1.getSizeX(), image1.getSizeY(), image1.getSizeC(), image1.getDataType_());
		for (int c = 0; c < image1.getSizeC(); c++) {
			
			double[] img1DoubleArray = Array1DUtil.arrayToDoubleArray(image1.getDataXY(c), image1.isSignedDataType());
			double[] img2DoubleArray = Array1DUtil.arrayToDoubleArray(image2.getDataXY(c), image2.isSignedDataType());
			ArrayMath.subtract(img1DoubleArray, img2DoubleArray, img1DoubleArray);

			double[] dummyzerosArray = Array1DUtil.arrayToDoubleArray(result.getDataXY(c), result.isSignedDataType());
			ArrayMath.max(img1DoubleArray, dummyzerosArray, img1DoubleArray);
			Array1DUtil.doubleArrayToSafeArray(img1DoubleArray, result.getDataXY(c), false);
			result.setDataXY(c, result.getDataXY(c));
		}
		result.dataChanged();
		return result;
	}
	
	public String loadInputVirtualStack(String path) 
	{
		LoaderDialog dialog = new LoaderDialog(false);
		if (path != null) 
			dialog.setCurrentDirectory(new File(path));
	    File[] selectedFiles = dialog.getSelectedFiles();
	    if (selectedFiles.length == 0)
	    	return null;
	    
	    if (selectedFiles[0].isDirectory())
	    	directory = selectedFiles[0].getAbsolutePath();
	    else
	    	directory = selectedFiles[0].getParentFile().getAbsolutePath();
		if (directory == null )
			return null;

		String [] list;
		if (selectedFiles.length == 1) {
			list = (new File(directory)).list();
			if (list ==null)
				return null;
			
			if (!(selectedFiles[0].isDirectory()) && selectedFiles[0].getName().toLowerCase().contains(".avi")) {
				loadSequenceVirtualAVI(selectedFiles[0].getAbsolutePath());
				return directory;
			}
		}
		else
		{
			list = new String[selectedFiles.length];
			  for (int i = 0; i < selectedFiles.length; i++) {
				if (selectedFiles[i].getName().toLowerCase().contains(".avi"))
					continue;
			    list[i] = selectedFiles[i].getAbsolutePath();
			}
		}
		loadSequenceVirtual(list, directory);
		return directory;
	}
	
	public String loadVirtualStackAt(String textPath) {

		if (textPath == null) 
			return loadInputVirtualStack(null); 
		
		File filepath = new File(textPath); 
	    if (filepath.isDirectory())
	    	directory = filepath.getAbsolutePath();
	    else
	    	directory = filepath.getParentFile().getAbsolutePath();
		if (directory == null )
			return null;

		String [] list;
		list = (new File(directory)).list();
		if (list ==null)
			return null;
		
		if (!(filepath.isDirectory()) && filepath.getName().toLowerCase().contains(".avi")) {
			loadSequenceVirtualAVI(filepath.getAbsolutePath());
			return directory;
		}
		
		loadSequenceVirtual(list, directory);
		return directory;
	}

	public String loadInputVirtualFromNameSavedInRoiXML()
	{
		if (csFileName != null)
			loadInputVirtualFromName(csFileName);
		return csFileName;
	}
	
	public void loadInputVirtualFromName(String name)
	{
		if (name.toLowerCase().contains(".avi"))
			loadSequenceVirtualAVI(name);
		else
			loadSequenceVirtualFromName(name);
	}

	private void loadSequenceVirtualFromName(String name) 
	{
		File filename = new File (name);
		if (filename.isDirectory())
	    	directory = filename.getAbsolutePath();
	    else {
	    	directory = filename.getParentFile().getAbsolutePath();
	    }
		if (directory == null) {
			status = EnumStatus.FAILURE;
			return;
		}
		String [] list;
		File fdir = new File(directory);
		boolean flag = fdir.isDirectory();
		if (!flag)
			return;
		list = fdir.list();
		// TODO: change directory into a pathname
		if (list != null)
			loadSequenceVirtual(list, directory);
	}
	
	private void loadSequenceVirtualAVI(String fileName) {
		if (importer != null )
		{
			try {
				importer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try
		{
			importer = new VideoImporter();
			status = EnumStatus.AVIFILE;
			importer.open( fileName, 0 );
			OMEXMLMetadata metaData = importer.getOMEXMLMetaData();
			nTotalFrames = MetaDataUtil.getSizeT( metaData, 0 ) - 2 ; // get one frame less as there is a little bug in the decompression of the video in h264
			csFileName = fileName;
		}
		catch (Exception exc)
		{
			MessageDialog.showDialog( "File type or video-codec not supported.", MessageDialog.ERROR_MESSAGE );
			status = EnumStatus.FAILURE;
		}
	}
	
	private void loadSequenceVirtual(String[] list, String directory) {
		status = EnumStatus.FAILURE;
		list = keepOnlyAcceptedNames(list);
		if (list==null) 
			return;

		listFiles = new String [list.length];
		int j = 0;
		for (int i=0; i<list.length; i++) {
			if (list[i]!=null)
				listFiles [j++] = directory + '/'+ list[i];
		}
		listFiles = StringSorter.sortNumerically(listFiles);
		nTotalFrames = listFiles.length;
		status = EnumStatus.FILESTACK;		
	}

	private void setVImageName(int t)
	{
		if (status == EnumStatus.FILESTACK)
			setName(getDecoratedImageName(t));
	}

	public String getFileName() {
		String fileName;
		if (status == EnumStatus.FILESTACK) 
			fileName = listFiles[0];
		else //  if ((status == EnumStatus.AVIFILE))
			fileName = csFileName;
		return fileName;		
	}
	
	public void setFileName(String name) {
		csFileName = name;		
	}
	
	public void storeAnalysisParametersToCages() {
		cages.detect.analysisEnd = analysisEnd;
		cages.detect.analysisStart = analysisStart;
		cages.detect.analysisStep = analysisStep;
	}
	
	public void storeAnalysisParametersToCapillaries () {
		capillaries.analysisStart = analysisStart;
		capillaries.analysisEnd = analysisEnd;
		capillaries.analysisStep = analysisStep;
	}
	
	public boolean xmlReadCapillaryTrackDefault() {
		return xmlReadCapillaryTrack(getDirectory()+File.separator+"capillarytrack.xml");
	}
	
	public boolean xmlReadCapillaryTrack(String filename) {
		boolean flag = capillaries.xmlReadROIsAndData(filename, this);
		if (flag) {
			analysisStart = capillaries.analysisStart;
			analysisEnd = capillaries.analysisEnd;
			analysisStep = capillaries.analysisStep;
		}
		return flag;
	}
		
	public boolean xmlReadDrosoTrackDefault() {
		return cages.xmlReadCagesFromFileNoQuestion(getDirectory() + File.separator+"drosotrack.xml", this);
	}
	
	public boolean xmlReadDrosoTrack(String filename) {
		boolean flag = cages.xmlReadCagesFromFileNoQuestion(filename, this);
		if (flag) {
			analysisStart = cages.detect.analysisStart;
			analysisEnd = cages.detect.analysisEnd;
			analysisStep = cages.detect.analysisStep;
		}
		return flag;
	}
	
	public boolean xmlWriteDrosoTrackDefault() {
		return cages.xmlWriteCagesToFile("drosotrack.xml", getDirectory());
	}
	
	public FileTime getImageModifiedTime (int t) {
		String name = getFileName(t);
		Path path = Paths.get(name);
		FileTime fileTime;
		try { fileTime = Files.getLastModifiedTime(path); }
		catch (IOException e) {
			System.err.println("Cannot get the last modified time - "+e+ "image "+ t+ " -- file "+ name);
			return null;
		}
//		LocalDateTime loc = LocalDateTime.ofInstant(fileTime.toInstant(), ZoneOffset.UTC);
		return fileTime;
	}
}