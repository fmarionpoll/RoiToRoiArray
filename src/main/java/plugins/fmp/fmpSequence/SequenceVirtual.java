package plugins.fmp.fmpSequence;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.math.ArrayMath;

import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;


import plugins.fmp.fmpTools.EnumImageOp;
import plugins.fmp.fmpTools.ImageOperationsStruct;
import plugins.stef.importer.xuggler.VideoImporter;




public class SequenceVirtual extends Sequence 
{
	protected VideoImporter importer 		= null;
	private String [] 		imagesList 		= null;
	private String 			csFileName 		= null;
	private String			directory 		= null;
	public IcyBufferedImage refImage 		= null;
	
	public long				analysisStart 	= 0;
	public long 			analysisEnd		= 99999999;
	public int 				analysisStep 	= 1;
	public int 				currentFrame 	= 0;
	public int				nTotalFrames 	= 0;
	
	public boolean			bBufferON 		= false;
	public EnumStatus 		statusSequenceVirtual = EnumStatus.REGULAR;
	
	public Capillaries 		capillariesRoi2RoiArray 	= new Capillaries();
	public Cages			cagesRoi2RoiArray 			= new Cages();
	
	public String [] 		seriesname 		= null;
	public int [][] 		data_raw 		= null;
	public double [][] 		data_filtered 	= null;
	
	// image cache
	public IcyBufferedImage 		cacheTransformedImage = null;
	public ImageOperationsStruct 	cacheTransformOp = new ImageOperationsStruct();
	public IcyBufferedImage 		cacheThresholdedImage = null;
	public ImageOperationsStruct 	cacheThresholdOp = new ImageOperationsStruct();
	
	// ----------------------------------------
	public SequenceVirtual () 
	{
		statusSequenceVirtual = EnumStatus.REGULAR;
	}
	
	public SequenceVirtual(String name, IcyBufferedImage image) 
	{
		super (name, image);
	}
	
	public SequenceVirtual (Sequence seq) 
	{
		int nfiles = seq.getSizeT();
		String fileList[] = new String[nfiles];
		String directory = null;
		for(int t=0; t < nfiles; t++) 
		{
			String firstfile = seq.getFilename (0, t, 0);
			if (firstfile == null)
				continue;
			Path path = Paths.get(firstfile);
			if (t == 0) 
				directory = path.getParent().toString();
			String filename = path.getFileName().toString();
			fileList[t] = filename;
		}
		
		filename = directory + ".xml";		
	}

	@Override
	public void close() 
	{
		super.close();
	}

	public String getDirectory () 
	{
		return directory;
	}

	@Override
	public IcyBufferedImage getImage(int t, int z, int c) 
	{
		IcyBufferedImage image =  super.getImage(t, z, c);
		currentFrame = t;
		return image;
	}

	@Override
	public IcyBufferedImage getImage(int t, int z) 
	{
		IcyBufferedImage image = super.getImage(t, z);
		currentFrame = t;
		return image;
	}
	
	public IcyBufferedImage getImageTransf(int t, int z, int c, EnumImageOp transformop) 
	{
		IcyBufferedImage image =  loadVImageAndSubtractReference(t, transformop);
		if (image != null && c != -1)
			image = IcyBufferedImageUtil.extractChannel(image, c);
		return image;
	}
	
	public IcyBufferedImage loadVImage(int t, int z) 
	{
		return super.getImage(t, z);
	}
	
	public IcyBufferedImage loadVImage(int t) 
	{
		return super.getImage(t, 0);
	}
	
	public IcyBufferedImage loadVImageAndSubtractReference(int t, EnumImageOp transformop) 
	{
		IcyBufferedImage ibufImage = loadVImage(t);
		switch (transformop) 
		{
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
		
	public String[] getListofFiles() 
	{
		return imagesList;
	}

	/*
	 * getSizeT (non-Javadoc)
	 * @see icy.sequence.Sequence#getSizeT()
	 * getSizeT is used to evaluate if volumetric images are stored in the sequence
	 * SequenceVirtual does not support volumetric images 
	 */
	@Override
	public int getSizeT() 
	{
		if (statusSequenceVirtual == EnumStatus.REGULAR)
			return super.getSizeT();
		else 
			return (int) nTotalFrames;
	}

	public int getT() 
	{
		return currentFrame;
	}

	public double getVData(int t, int z, int c, int y, int x) 
	{
		final IcyBufferedImage img = loadVImage(t);
		if (img != null)
			return img.getData(x, y, c);
		return 0d;
	}

	public String getDecoratedImageName(int t) 
	{
		currentFrame = t; 
		return getFileName() + " ["+(t)+ "/" + (getSizeT()-1) + "]";
		
	}
	
	public String getFileName(int t) 
	{
		String csName = null;
		if (statusSequenceVirtual == EnumStatus.FILESTACK) 
			csName = imagesList[t];
		else if (statusSequenceVirtual == EnumStatus.AVIFILE)
			csName = csFileName;
		return csName;
	}
	
	public boolean isFileStack() 
	{
		return (statusSequenceVirtual == EnumStatus.FILESTACK);
	}

	public boolean setCurrentVImage(int t) 
	{
		IcyBufferedImage bimage = super.getImage(t, 0);
		super.setImage(t, 0, bimage);
		setVImageName(t);		
		currentFrame = t;
		return true;
	}

	@Override
	public void setImage(int t, int z, BufferedImage bimage) throws IllegalArgumentException 
	{
		super.setImage(t, z, bimage);
		currentFrame = t;
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
	
	// --------------------------------------------------------------------
		
	private void setVImageName(int t)
	{
		if (statusSequenceVirtual == EnumStatus.FILESTACK)
			setName(getDecoratedImageName(t));
	}

	public String getFileName() 
	{
		String fileName;
		if (statusSequenceVirtual == EnumStatus.FILESTACK) 
			fileName = imagesList[0];
		else //  if ((status == EnumStatus.AVIFILE))
			fileName = csFileName;
		return fileName;		
	}
	
	public void setFileName(String name) 
	{
		csFileName = name;		
	}
	
	public void storeAnalysisParametersToCages() 
	{
		cagesRoi2RoiArray.detect.analysisEnd = analysisEnd;
		cagesRoi2RoiArray.detect.analysisStart = analysisStart;
		cagesRoi2RoiArray.detect.analysisStep = analysisStep;
	}
	
	public void storeAnalysisParametersToCapillaries () 
	{
		capillariesRoi2RoiArray.analysisStart = analysisStart;
		capillariesRoi2RoiArray.analysisEnd = analysisEnd;
		capillariesRoi2RoiArray.analysisStep = analysisStep;
	}
	
	public boolean xmlReadCapillaryTrackDefault() 
	{
		return xmlReadCapillaryTrack(getDirectory()+File.separator+"capillarytrack.xml");
	}
	
	public boolean xmlReadCapillaryTrack(String filename) 
	{
		boolean flag = capillariesRoi2RoiArray.xmlReadROIsAndData(filename, this);
		if (flag) {
			analysisStart = capillariesRoi2RoiArray.analysisStart;
			analysisEnd = capillariesRoi2RoiArray.analysisEnd;
			analysisStep = capillariesRoi2RoiArray.analysisStep;
		}
		return flag;
	}
		
	public boolean xmlReadDrosoTrackDefault() 
	{
		return cagesRoi2RoiArray.xmlReadCagesFromFileNoQuestion(getDirectory() + File.separator+"drosotrack.xml", this);
	}
	
	public boolean xmlReadDrosoTrack(String filename) 
	{
		boolean flag = cagesRoi2RoiArray.xmlReadCagesFromFileNoQuestion(filename, this);
		if (flag) {
			analysisStart = cagesRoi2RoiArray.detect.analysisStart;
			analysisEnd = cagesRoi2RoiArray.detect.analysisEnd;
			analysisStep = cagesRoi2RoiArray.detect.analysisStep;
		}
		return flag;
	}
	
	public boolean xmlWriteDrosoTrackDefault() 
	{
		return cagesRoi2RoiArray.xmlWriteCagesToFile("drosotrack.xml", getDirectory());
	}
	
	public FileTime getImageModifiedTime (int t) 
	{
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