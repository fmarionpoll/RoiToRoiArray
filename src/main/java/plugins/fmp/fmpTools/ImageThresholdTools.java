package plugins.fmp.fmpTools;

import java.awt.Color;
import java.util.ArrayList;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.type.DataType;
import icy.type.collection.array.Array1DUtil;


public class ImageThresholdTools {

	// parameters passed by caller
	private int colorthreshold;
	private int colordistanceType;
	private int simplethreshold = 255;
	
	// local variables
	private final byte byteFALSE = 0;
	private final byte byteTRUE = (byte) 0xFF;
	private ArrayList<Color> colorarray = null;
	
	// ---------------------------------------------
	
	public void setSingleThreshold (int simplethreshold)
	{
		this.simplethreshold = simplethreshold;
	}
	
	public void setColorArrayThreshold (int colordistanceType, int colorthreshold, ArrayList<Color> colorarray)
	{
		this.colordistanceType = colordistanceType;
		this.colorthreshold = colorthreshold;
		this.colorarray = colorarray;
	}

	public IcyBufferedImage getBinaryInt_FromThreshold(IcyBufferedImage sourceImage) 
	{	
		IcyBufferedImage binaryMap = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(), 1, DataType.UBYTE);
		byte[] binaryMapDataBuffer = binaryMap.getDataXYAsByte(0);

		int [] imageSourceDataBuffer = null;
		DataType datatype = sourceImage.getDataType_();
		if (datatype != DataType.INT) {
			Object sourceArray = sourceImage.getDataXY(0);
			imageSourceDataBuffer = Array1DUtil.arrayToIntArray(sourceArray, sourceImage.isSignedDataType());
		}
		else
			imageSourceDataBuffer = sourceImage.getDataXYAsInt(0);
		
		for (int x = 0; x < binaryMapDataBuffer.length; x++)  {
			int val = imageSourceDataBuffer[x] & 0xFF;
			if (val > simplethreshold)
				binaryMapDataBuffer[x] = byteFALSE;
			else
				binaryMapDataBuffer[x] = byteTRUE;
		}
		return binaryMap;
	}
	
	public IcyBufferedImage getBinaryInt_FromColorsThreshold(IcyBufferedImage sourceImage)  
	{
		if (colorarray.size() == 0)
			return null;

		if (sourceImage.getSizeC() < 3 ) {
			System.out.print("Failed operation: attempt to compute threshold from image with less than 3 color channels");
			return null;
		}
		
		NHColorDistance distance; 
		if (colordistanceType == 1)
			distance = new NHColorDistanceL1();
		else
			distance = new NHColorDistanceL2();
			
		IcyBufferedImage binaryResultBuffer = new IcyBufferedImage(sourceImage.getSizeX(), sourceImage.getSizeY(), 1, DataType.UBYTE);	
		
		IcyBufferedImage dummy = sourceImage;
		if (sourceImage.getDataType_() == DataType.DOUBLE) {
			dummy = IcyBufferedImageUtil.convertToType(sourceImage, DataType.BYTE, false);
		}
		byte [][] sourceBuffer = dummy.getDataXYCAsByte(); // [C][XY]
		byte [] binaryResultArray = binaryResultBuffer.getDataXYAsByte(0);
		
		int npixels = binaryResultArray.length;
		Color pixel = new Color(0,0,0);
		for (int ipixel = 0; ipixel < npixels; ipixel++) {
			
			byte val = byteFALSE; 
			pixel = new Color(sourceBuffer[0][ipixel] & 0xFF, sourceBuffer[1][ipixel]  & 0xFF, sourceBuffer[2][ipixel]  & 0xFF);
			
			for (int k = 0; k < colorarray.size(); k++) {
				Color color = colorarray.get(k);
				if (distance.computeDistance(pixel, color) <= colorthreshold) {
					val = byteTRUE; 
					break;
				}
			}
			binaryResultArray[ipixel] = val;
		}
		return binaryResultBuffer;
	}
	
	public boolean[] getBoolMap_FromBinaryInt(IcyBufferedImage img) 
	{
		boolean[]	boolMap = new boolean[ img.getSizeX() * img.getSizeY() ];
		byte [] imageSourceDataBuffer = null;
		DataType datatype = img.getDataType_();
		
		if (datatype != DataType.BYTE && datatype != DataType.UBYTE) {
			Object sourceArray = img.getDataXY(0);
			imageSourceDataBuffer = Array1DUtil.arrayToByteArray(sourceArray);
		}
		else
			imageSourceDataBuffer = img.getDataXYAsByte(0);
		
		for (int x = 0; x < boolMap.length; x++)  {
			if (imageSourceDataBuffer[x] == byteFALSE)
				boolMap[x] =  false;
			else
				boolMap[x] =  true;
		}
		return boolMap;
	}
	
}
