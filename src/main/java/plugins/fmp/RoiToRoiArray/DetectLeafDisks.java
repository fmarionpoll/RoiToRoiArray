package plugins.fmp.RoiToRoiArray;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import plugins.fmp.fmpSequence.SequenceVirtual;
import plugins.fmp.fmpTools.EnumImageOp;
import plugins.fmp.fmpTools.FmpTools;
import plugins.fmp.fmpTools.OverlayThreshold;
import plugins.kernel.roi.roi2d.ROI2DEllipse;

public class DetectLeafDisks {
	
	private static OverlayThreshold overlayThreshold = null;
	
	public static void findLeafDiskIntoRectangles(SequenceVirtual sequenceVirtual) 
	{
		if (sequenceVirtual.cacheThresholdedImage == null)
			return;
		// get byte image (0, 1) that has been thresholded
		ArrayList<ROI2D> roiList = sequenceVirtual.seq.getROI2Ds();
		Collections.sort(roiList, new FmpTools.ROI2DNameComparator());
		
		for (ROI2D roi:roiList) {
			if (!roi.getName().contains("grid"))
				continue;

			Rectangle rectGrid = roi.getBounds();
			IcyBufferedImage img = IcyBufferedImageUtil.getSubImage(sequenceVirtual.cacheThresholdedImage, rectGrid);
			byte [] binaryData = img.getDataXYAsByte(0);
			int sizeX = img.getSizeX();
			int sizeY = img.getSizeY();

			getPixelsConnected (sizeX, sizeY, binaryData);
			getBlobsConnected(sizeX, sizeY, binaryData);
			byte leafBlob = getLargestBlob(binaryData);
			eraseAllBlobsExceptOne(leafBlob, binaryData);
			Rectangle leafBlobRect = getBlobRectangle( leafBlob, sizeX, sizeY, binaryData);
			
			addLeafROIinGridRectangle(sequenceVirtual.seq, leafBlobRect, roi);
		}
		System.out.println("Done");
	}
	
	private static void addLeafROIinGridRectangle (Sequence seq, Rectangle leafBlobRect, ROI2D roi) {

		Rectangle rectGrid = roi.getBounds();
		double xleft = rectGrid.getX()+ leafBlobRect.getX();
		double xright = xleft + leafBlobRect.getWidth();
		double ytop = rectGrid.getY() + leafBlobRect.getY();
		double ybottom = ytop + leafBlobRect.getHeight();
		
		Point2D.Double point0 = new Point2D.Double (xleft , ytop);
		Point2D.Double point1 = new Point2D.Double (xleft , ybottom);
		Point2D.Double point2 = new Point2D.Double (xright , ybottom);
		Point2D.Double point3 = new Point2D.Double (xright , ytop);
		
		List<Point2D> points = new ArrayList<>();
		points.add(point0);
		points.add(point1);
		points.add(point2);
		points.add(point3);
		ROI2DEllipse roiP = new ROI2DEllipse (points.get(0), points.get(2));
		roiP.setName("leaf"+roi.getName());
		roiP.setColor(Color.RED);
		seq.addROI(roiP);
	}
	
	private static Rectangle getBlobRectangle(byte blobNumber, int sizeX, int sizeY, byte [] binaryData) {
		Rectangle rect = new Rectangle(0, 0, 0, 0);
		int [] arrayX = new int [sizeX];
		int [] arrayY = new int [sizeY];
		for (int iy= 0; iy < sizeY; iy++) {
			for (int ix = 0; ix < sizeX; ix++) {					
				if (binaryData[ix + sizeX*iy] != blobNumber) 
					continue;
				arrayX[ix] ++;
				arrayY[iy]++;
			}
		}
		for (int i=0; i< sizeX; i++)
			if (arrayX[i] > 0) {
				rect.x = i;
				break;
			}
		for (int i = sizeX-1; i >=0; i--)
			if (arrayX[i] > 0) {
				rect.width = i-rect.x +1;
				break;
			}
		
		for (int i=0; i< sizeY; i++)
			if (arrayY[i] > 0) {
				rect.y = i;
				break;
			}
		for (int i = sizeY-1; i >=0; i--)
			if (arrayY[i] > 0) {
				rect.height = i-rect.y +1;
				break;
			}
		return rect;
	}
	
	private static int getPixelsConnected (int sizeX, int sizeY, byte [] binaryData) 
	{
		byte blobnumber = 1;
		for (int iy= 0; iy < sizeY; iy++) {
			for (int ix = 0; ix < sizeX; ix++) {					
				if (binaryData[ix + sizeX*iy] < 0) 
					continue;
				
				int ioffset = ix + sizeX*iy;
				int ioffsetpreviousrow = ix + sizeX*(iy-1);
				
				if ((iy > 0) && (ix > 0) && (binaryData[ioffsetpreviousrow-1] > 0)) 
					binaryData[ioffset] = binaryData[ioffsetpreviousrow-1];
				
				else if ((iy > 0) && (binaryData[ioffsetpreviousrow] > 0))
					binaryData[ioffset] = binaryData[ioffsetpreviousrow];
				
				else if ((iy > 0) && ((ix+1) < sizeX) &&  (binaryData[ioffsetpreviousrow+1] > 0))
					binaryData[ioffset] = binaryData[ioffsetpreviousrow+1];
				
				else if ((ix > 0) && (binaryData[ioffset-1] > 0))
					binaryData[ioffset] = binaryData[ioffset-1];
				
				else { // new blob number
					binaryData[ioffset] = blobnumber;
					blobnumber++;
				}						
			}
		}
		return (int) blobnumber -1;
	}
	
	private static void getBlobsConnected (int sizeX, int sizeY, byte[] binaryData) {
		for (int iy= 0; iy < sizeY; iy++) {
			for (int ix = 0; ix < sizeX; ix++) {					
				if (binaryData[ix + sizeX*iy] < 0) 
					continue;
				
				int ioffset = ix + sizeX*iy;
				int ioffsetpreviousrow = ix + sizeX*(iy-1);
				byte val = binaryData[ioffset];
				
				if ((iy > 0) && (ix > 0) && (binaryData[ioffsetpreviousrow-1] > 0)) 
					if (binaryData[ioffsetpreviousrow-1] > val)
						changeAllBlobNumber1Into2 (binaryData[ioffsetpreviousrow-1], val, binaryData) ;
				
				else if ((iy > 0) && (binaryData[ioffsetpreviousrow] > 0))
					if (binaryData[ioffsetpreviousrow] > val)
						changeAllBlobNumber1Into2 (binaryData[ioffsetpreviousrow], val, binaryData) ;
				
				else if ((iy > 0) && ((ix+1) < sizeX) &&  (binaryData[ioffsetpreviousrow+1] > 0))
					if (binaryData[ioffsetpreviousrow+1] > val)
						changeAllBlobNumber1Into2 (binaryData[ioffsetpreviousrow+1], val, binaryData) ;
				
				else if ((ix>0) && (binaryData[ioffset-1] > 0))
					if (binaryData[ioffset-1] > val)
						changeAllBlobNumber1Into2 (binaryData[ioffset-1], val, binaryData) ;					
			}
		}
	}
	
	private static byte getLargestBlob(byte[] binaryData) 
	{
		byte maxblob = getMaximumBlobNumber(binaryData);
		int maxpixels = 0;
		byte largestblob = 0;
		for (byte i=0; i <= maxblob; i++) {
			int npixels = getNumberOfPixelEqualToValue (i, binaryData);
			if (npixels > maxpixels) {
				maxpixels = npixels;
				largestblob = i;
			}
		}
		return largestblob;
	}
	
	private static void eraseAllBlobsExceptOne(byte blobIDToKeep, byte [] binaryData) {
		for (int i=0; i< binaryData.length; i++) {
			if (binaryData[i] != blobIDToKeep)
				binaryData[i] = -1;
			else
				binaryData[i] = 1;
		}
	}
	
	private static void changeAllBlobNumber1Into2 (byte oldvalue, byte newvalue, byte [] binaryData) 
	{
		for (int i=0; i< binaryData.length; i++)
			if (binaryData[i] == oldvalue)
				binaryData[i] = newvalue;
	}
	
	private static int getNumberOfPixelEqualToValue (byte value, byte [] binaryData) 
	{
		int sum = 0;
		for (int i=0; i< binaryData.length; i++)
			if (binaryData[i] == value)
				sum++;
		return sum;
	}
	
	private static byte getMaximumBlobNumber (byte [] binaryData) 
	{
		byte max = 0;
		for (int i=0; i< binaryData.length; i++)
			if (binaryData[i] > max)
				max = binaryData[i];
		return max;
	}
	
	public static void displayOverlay (SequenceVirtual sequenceVirtual, boolean newValue, int thresholdOv) {
		if (sequenceVirtual == null)
			return;

		if (newValue) {
			
			if (overlayThreshold == null) {
				overlayThreshold = new OverlayThreshold(sequenceVirtual);
				sequenceVirtual.seq.addOverlay(overlayThreshold);
			}
			sequenceVirtual.cagesRoi2RoiArray.detect.threshold = thresholdOv;
			sequenceVirtual.seq.addOverlay(overlayThreshold);
			updateOverlay(sequenceVirtual, null);
		}
		else  
		{
			if (overlayThreshold != null) 
				sequenceVirtual.seq.removeOverlay(overlayThreshold);
			overlayThreshold = null;
		}
	}
	
	public static void updateThreshold (SequenceVirtual sequenceVirtual, int newValue) {
		if (sequenceVirtual == null)
			return;
		
		sequenceVirtual.cagesRoi2RoiArray.detect.threshold = newValue;
		updateOverlay(sequenceVirtual, null);
	}
	
	public static void updateOverlay (SequenceVirtual sequenceVirtual, EnumImageOp transformop) {
		if (sequenceVirtual == null)
			return;
		if (overlayThreshold == null) {
			overlayThreshold = new OverlayThreshold(sequenceVirtual);
			sequenceVirtual.seq.addOverlay(overlayThreshold);
		}

		overlayThreshold.setSequence (sequenceVirtual);
		if (transformop != null)
			overlayThreshold.setTransform(transformop);
		overlayThreshold.setThresholdSingle(sequenceVirtual.cagesRoi2RoiArray.detect.threshold);
			
		if (overlayThreshold != null) {
			overlayThreshold.painterChanged();
		}
	}
	
}
