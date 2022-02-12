package plugins.fmp.fmpSequence;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import plugins.fmp.fmpTools.EnumArrayListType;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class KymoMeasures {

	public int imageWidth = 1;
	public int imageHeight = 1;
	public ArrayList <ROI2DShape> measuresArrayList = new ArrayList <ROI2DShape>();	

	
	public ArrayList<Integer> getArrayList (EnumArrayListType option) {

		if (measuresArrayList.size() == 0)
			return null;
		ArrayList<Integer> datai = null;
		
		switch (option) {
//		case derivedValues:
//			datai = derivedValuesArrayList;
//			break;
		case cumSum:
			datai = new ArrayList<Integer>(Collections.nCopies(imageWidth, 0));
			addRoisMatchingFilterToCumSumDataArray("gulp", datai);
			break;
		case bottomLevel:
			datai = copyFirstRoiMatchingFilterToDataArray("bottomlevel");
			break;
		case topLevel:
		default:
			datai = copyFirstRoiMatchingFilterToDataArray("toplevel");
			break;
		}
		return datai;
	}
	
	private void addRoisMatchingFilterToCumSumDataArray (String filter, ArrayList<Integer> cumSumArray) {
		
		for (ROI2D roi: measuresArrayList) {
			if (roi.getName().contains(filter)) 
				addRoitoCumulatedSumArray((ROI2DPolyLine) roi, cumSumArray);
		}
		return ;
	}
	
	private void addRoitoCumulatedSumArray(ROI2DPolyLine roi, ArrayList<Integer> sumArrayList) {
		
		interpolateMissingPointsAlongXAxis (roi);
		ArrayList<Integer> intArray = transfertRoiYValuesToDataArray(roi);
		Polyline2D line = roi.getPolyline2D();
		int jstart = (int) line.xpoints[0];

		int previousY = intArray.get(0);
		for (int i=0; i< intArray.size(); i++) {
			int val = intArray.get(i);
			int deltaY = val - previousY;
			previousY = val;
			for (int j = jstart+i; j< sumArrayList.size(); j++) {
				sumArrayList.set(j, sumArrayList.get(j) +deltaY);
			}
		}
	}
	
	private boolean interpolateMissingPointsAlongXAxis (ROI2DPolyLine roiLine) {
		// interpolate points so that each x step has a value	
		// assume that points are ordered along x
	
		Polyline2D line = roiLine.getPolyline2D();
		int roiLine_npoints = line.npoints;
		// exit if the length of the segment is the same
		int roiLine_nintervals =(int) line.xpoints[roiLine_npoints-1] - (int) line.xpoints[0] +1;  
		
		if (roiLine_npoints == roiLine_nintervals)
			return true;
		else if (roiLine_npoints > roiLine_nintervals)
			return false;
		
		List<Point2D> pts = new ArrayList <Point2D>(roiLine_npoints);
		double ylast = line.ypoints[roiLine_npoints-1];
		for (int i=1; i< roiLine_npoints; i++) {
			
			int xfirst = (int) line.xpoints[i-1];
			int xlast = (int) line.xpoints[i];
			double yfirst = line.ypoints[i-1];
			ylast = line.ypoints[i];
			for (int j = xfirst; j< xlast; j++) {
				
				int val = (int) (yfirst + (ylast-yfirst)*(j-xfirst)/(xlast-xfirst));
				Point2D pt = new Point2D.Double(j, val);
				pts.add(pt);
			}
		}
		Point2D pt = new Point2D.Double(line.xpoints[roiLine_npoints-1], ylast);
		pts.add(pt);
		
		roiLine.setPoints(pts);
		return true;
	}
	
	private ArrayList<Integer> transfertRoiYValuesToDataArray(ROI2DPolyLine roiLine) {

		Polyline2D line = roiLine.getPolyline2D();
		ArrayList<Integer> intArray = new ArrayList<Integer> (line.npoints);
		for (int i=0; i< line.npoints; i++) 
			intArray.add((int) line.ypoints[i]);

		return intArray;
	}
	
	private ArrayList<Integer> copyFirstRoiMatchingFilterToDataArray (String filter) {
		
		for (ROI2D roi: measuresArrayList) {
			if (roi.getName().contains(filter)) { 
				interpolateMissingPointsAlongXAxis ((ROI2DPolyLine)roi);
				return transfertRoiYValuesToDataArray((ROI2DPolyLine)roi);
			}
		}
		return null;
	}
}
