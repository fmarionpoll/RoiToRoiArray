package plugins.fmp.fmpSequence;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.math.ArrayMath;

import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;


import plugins.fmp.fmpTools.EnumImageOp;
import plugins.fmp.fmpTools.ImageOperationsStruct;




public class SequenceVirtual 
{
	public Sequence			seq				= null;
	private List <String> 	imagesList 		= null;
	private String 			csFileName 		= null;
	private String			directory 		= null;
	protected String 		csCamFileName 	= null;
	public IcyBufferedImage refImage 		= null;
	
	public long				analysisStart 	= 0;
	public long 			analysisEnd		= 99999999;
	public int 				analysisStep 	= 1;
	public int 				currentFrame 	= 0;
	public int				nTotalFrames 	= 0;
	
	public boolean			bBufferON 		= false;
	public EnumStatus 		statusSequenceVirtual = EnumStatus.REGULAR;
	
	public Capillaries 		capillariesRoi2RoiArray = new Capillaries();
	public Cages			cagesRoi2RoiArray 		= new Cages();
	
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
		seq = new Sequence();
		statusSequenceVirtual = EnumStatus.REGULAR;
	}
	
	public SequenceVirtual (Sequence seq) 
	{
		this.seq = seq;
		statusSequenceVirtual = EnumStatus.REGULAR;
	}
	
	public SequenceVirtual(String name, IcyBufferedImage image) 
	{
		seq = new Sequence (name, image);
		statusSequenceVirtual = EnumStatus.FILESTACK;
	}
	
	public SequenceVirtual(List<String> listNames) 
	{
		setV2ImagesList(listNames);
		statusSequenceVirtual = EnumStatus.FILESTACK;
	}
	
	public void setV2ImagesList(List <String> extImagesList) 
	{
		imagesList.clear();
		imagesList.addAll(extImagesList);
		nTotalFrames = imagesList.size();
		statusSequenceVirtual = EnumStatus.FILESTACK;
	}

	public void close() 
	{
		seq.close();
	}

	public String getDirectory () 
	{
		return directory;
	}

	public IcyBufferedImage getSeqImage(int t, int z) 
	{
		currentFrame = t;
		return seq.getImage(t, z);
	}
	
	public IcyBufferedImage getImageTransf(int t, int z, int c, EnumImageOp transformop) 
	{
		IcyBufferedImage image =  loadVImageAndSubtractReference(t, transformop);
		if (image != null && c != -1)
			image = IcyBufferedImageUtil.extractChannel(image, c);
		return image;
	}
		
	public IcyBufferedImage loadVImageAndSubtractReference(int t, EnumImageOp transformop) 
	{
		IcyBufferedImage ibufImage = seq.getImage(t, 0);
		switch (transformop) 
		{
			case REF_PREVIOUS: // subtract image n-analysisStep
			{
				int t0 = t-analysisStep;
				if (t0 <0)
					t0 = 0;
				IcyBufferedImage ibufImage0 = seq.getImage(t0, 0);
				ibufImage = subtractImages (ibufImage, ibufImage0);
			}	
				break;
			case REF_T0: // subtract reference image
			case REF:
				if (refImage == null)
					refImage = seq.getImage((int) analysisStart, 0);
				ibufImage = subtractImages (ibufImage, refImage);
				break;

			case NONE:
			default:
				break;
		}
		return ibufImage;
	}
		
	public List<String> getListofFiles() 
	{
		return imagesList;
	}

	/*
	 * getSizeT (non-Javadoc)
	 * @see icy.sequence.Sequence#getSizeT()
	 * getSizeT is used to evaluate if volumetric images are stored in the sequence
	 * SequenceVirtual does not support volumetric images 
	 */

	public int getSizeT() 
	{
		if (statusSequenceVirtual == EnumStatus.REGULAR)
			return seq.getSizeT();
		else 
			return (int) nTotalFrames;
	}

	public int getT() 
	{
		return currentFrame;
	}

	public double getVData(int t, int z, int c, int y, int x) 
	{
		final IcyBufferedImage img = seq.getImage(t, 0);
		if (img != null)
			return img.getData(x, y, c);
		return 0d;
	}

	public String getDecoratedImageName(int t) 
	{
		currentFrame = t; 
		if (seq!= null)
			return getCSCamFileName() + " ["+(t)+ "/" + (seq.getSizeT()-1) + "]";
		else
			return getCSCamFileName() + "[]";
	}
	
	private String getCSCamFileName() 
	{
		if (csCamFileName == null) 
		{
			Path path = Paths.get(imagesList.get(0));
			csCamFileName = path.subpath(path.getNameCount()-4, path.getNameCount()-1).toString();
		}
		return csCamFileName;		
	}
	
	public String getFileName(int t) 
	{
		String csName = null;
		if (statusSequenceVirtual == EnumStatus.FILESTACK) 
			csName = imagesList.get(t);
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
		IcyBufferedImage bimage = seq.getImage(t, 0);
		seq.setImage(t, 0, bimage);		
		currentFrame = t;
		return true;
	}

	public void setImage(int t, int z, BufferedImage bimage) throws IllegalArgumentException 
	{
		seq.setImage(t, z, bimage);
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