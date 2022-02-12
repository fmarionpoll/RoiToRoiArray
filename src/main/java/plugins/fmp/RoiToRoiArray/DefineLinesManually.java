package plugins.fmp.RoiToRoiArray;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import icy.gui.frame.progress.AnnounceFrame;
import icy.image.IcyBufferedImage;
import icy.roi.ROI2D;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;
import icy.type.geom.GeomUtil;
import plugins.fmp.fmpTools.FmpTools;
import plugins.kernel.roi.roi2d.ROI2DEllipse;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;

public class DefineLinesManually 
{
	public Line2D adjustLine (IcyBufferedImage image, Line2D line, int checksize, int deltainside) 
	{	
		Rectangle rect = line.getBounds();	
		Line2D bestline = new Line2D.Double();
		
		// horizontal lines
		if (rect.getWidth() >= rect.getHeight()) {
			// check profile of a line perpendicular to this line at one end (linetest1) and then at the other end (linetest2)
			Line2D linetest1 = new Line2D.Double (line.getX1()+deltainside, line.getY1()-checksize, line.getX1()+deltainside, line.getY2()+checksize);
			int iy1min = getIndexMinimumValue(getProfile(image, linetest1))-checksize;
			Line2D linetest2 = new Line2D.Double (line.getX2()-deltainside, line.getY2()-checksize, line.getX2()-deltainside, line.getY2()+checksize);
			int iy2min = getIndexMinimumValue(getProfile(image, linetest2))-checksize;
			bestline.setLine(line.getX1(), line.getY1()-iy1min, line.getX2(), line.getY2()-iy2min);
		}
		// vertical lines
		else {
			Line2D linetest1 = new Line2D.Double (line.getX1()-checksize, line.getY1()+deltainside, line.getX1()+checksize, line.getY2()+deltainside);
			int iy1min = getIndexMinimumValue(getProfile(image, linetest1))-checksize;
			Line2D linetest2 = new Line2D.Double (line.getX2()-checksize, line.getY2()-deltainside, line.getX2()+checksize, line.getY2()-deltainside);
			int iy2min = getIndexMinimumValue(getProfile(image, linetest2))-checksize;
			bestline.setLine(line.getX1(), line.getY1()-iy1min, line.getX2(), line.getY2()-iy2min);
		}
		return bestline;
	}
	
	private double [][] getProfile (IcyBufferedImage image, Line2D line) 
	{
		List<Point2D> pointslist = getAllPointsAlongLine (line);		
		double [][] profile = getValueForPointList(pointslist, image);
		return profile;
	}
	
	public double[][] getValueForPointList( List<Point2D> pointList, IcyBufferedImage image ) 
	{
		int sizeX = image.getSizeX();
		int sizeY = image.getSizeY();
		
		int nchannels = image.getSizeC();
		int len = pointList.size();
		double[][] value = new double[len][nchannels];
//		System.out.println( "create double array len=" + len  + " nchannels="+ nchannels);
		
		for (int chan=0; chan < nchannels; chan++) {
			double [] sourceValues = Array1DUtil.arrayToDoubleArray(image.getDataXY(chan), image.isSignedDataType());
			int len_sourceValues = sourceValues.length -1;
			//System.out.println("len = "+len_sourceValues);
			for (int i=0; i<len; i++) {
				Point2D point = pointList.get(i);
				if (point.getX() < 0)
					point.setLocation(0, point.getY());
				if (point.getY() < 0)
					point.setLocation(point.getX(), 0);
//					System.out.println( "i= " + i  + " point x:"+ point.getX() + " point.y="+ point.getY());
				if (point.getX() >= sizeX ) 
					point.setLocation(sizeX -1, point.getY());
				if (point.getX() >= sizeX || point.getY() >= sizeY) 
					point.setLocation(point.getX(), sizeY -1);
	
				int index = (int)point.getX() + ((int) point.getY() * sizeX);
				if (index >= len_sourceValues)
					index = len_sourceValues -1;
				if (index < 0) {
					System.out.println( "i= " + i  + " point x:"+ point.getX() + " point.y="+ point.getY() + " index=" + index);
				}

				value[i][chan] = sourceValues [index];
			}
		}
		return value;
	}

	private List<Point2D> getAllPointsAlongLine(Line2D line) 
	{
        List<Point2D> pointslist = new ArrayList<Point2D>();
        int x1 = (int) line.getX1();
        int y1 = (int) line.getY1();
        int x2 = (int) line.getX2();
        int y2 = (int) line.getY2();
        
        int deltax = Math.abs(x2 - x1);
        int deltay = Math.abs(y2 - y1);
        int error = 0;
        if (deltax > deltay) {
	        int y = y1;
	        for (int x = x1; x< x2; x++) 
	        {
	        	pointslist.add(new Point2D.Double(x, y));
	        	error = error + deltay;
	            if( 2*error >= deltax ) {
	                y = y + 1;
	                error=error - deltax;
	            	}
	        }
        }
        else 
        {
        	int x = x1;
	        for (int y = y1; y< y2; y++) 
	        {
	        	pointslist.add(new Point2D.Double(x, y));
	        	error = error + deltax;
	            if( 2*error >= deltay ) {
	                x = x + 1;
	                error=error - deltay;
	            	}
	        }
        }
        return pointslist;
	}
	
	public ArrayList<Line2D> getVerticalLinesFromIntervals(Polygon roiPolygon, List<Integer> listofX) 
	{
		ArrayList<Line2D> verticallines = new ArrayList<Line2D>();
		double deltaYTop = roiPolygon.ypoints[3] - roiPolygon.ypoints[0];
		double deltaXTop = roiPolygon.xpoints[3] - roiPolygon.xpoints[0];
		double deltaYBottom = roiPolygon.ypoints[2] - roiPolygon.ypoints[1];
		double deltaXBottom = roiPolygon.xpoints[2] - roiPolygon.xpoints[1];
		double lastX = listofX.get(listofX.size() -1);
		
		for (int i = 0; i < listofX.size(); i++) {
			int index = listofX.get(i);
			int ixtop = (int) (index*deltaXTop/lastX);
			int ixbottom = (int) (index*deltaXBottom/lastX);
			Point2D.Double top 		= new Point2D.Double(roiPolygon.xpoints[0] + ixtop, 	roiPolygon.ypoints[0] + index*deltaYTop/lastX);
			Point2D.Double bottom 	= new Point2D.Double(roiPolygon.xpoints[1] + ixbottom,	roiPolygon.ypoints[1] + index*deltaYBottom/lastX);
			Line2D line = new Line2D.Double (top, bottom);
			verticallines.add(line);
		}
		return verticallines;
	}

	public ArrayList<Line2D> getHorizontalLinesFromIntervals(Polygon roiPolygon, List<Integer> listofY) 
	{
		ArrayList<Line2D> horizontallines = new ArrayList<Line2D>();
		double deltaYLeft = roiPolygon.ypoints[1] - roiPolygon.ypoints[0];
		double deltaXLeft = roiPolygon.xpoints[1] - roiPolygon.xpoints[0];
		double deltaYRight = roiPolygon.ypoints[2] - roiPolygon.ypoints[3];
		double deltaXRight = roiPolygon.xpoints[2] - roiPolygon.xpoints[3];
		double lastX = listofY.get(listofY.size() -1);
		
		for (int i = 0; i < listofY.size(); i++) {
			int index = listofY.get(i);
			int iyleft = (int) (index*deltaYLeft/lastX);
			int iyright = (int) (index*deltaYRight/lastX);
			Point2D.Double left = new Point2D.Double(roiPolygon.xpoints[0] + index*deltaXLeft/lastX, 	roiPolygon.ypoints[0] + iyleft); 
			Point2D.Double right = new Point2D.Double(roiPolygon.xpoints[3] + index*deltaXRight/lastX,	roiPolygon.ypoints[3] + iyright); 
			Line2D line = new Line2D.Double (left, right);
			horizontallines.add(line);
		}
		return horizontallines;
	}
	
	public void buildROIsFromLines (Sequence seq, List<List<Line2D>> linesArray) 
	{
		// build dummy lines
		String [] type = new String [] {"vertical", "horizontal"};  
		int itype = 0;
		for (List<Line2D> firstarray : linesArray) {
			int i=0;
			for (Line2D line: firstarray) {
				ROI2DLine roiL1 = new ROI2DLine (line);
				roiL1.setName(type[itype]+i);
				roiL1.setReadOnly(false);
				roiL1.setColor(Color.RED);
				seq.addROI(roiL1, true);
				i++;
			}
			itype++;
		}
	}
	
	public int getIndexMinimumValue (double [][] profile) 
	{
		int n= profile.length;
		int imin = 0;
		double valuemin = profile[0][0] + profile[0][1] + profile[0][2];
		for (int chan= 0; chan < 3; chan++) {
			
			for (int i=0; i< n; i++) {
				double value = profile[i][0] + profile[i][1] + profile[i][2];
				if (value < valuemin) {
					valuemin = value; 
					imin = i;
				}
			}
		}
		return imin;
	}
	
	public List<List<Line2D>> buildLinesFromSTDProfile(IcyBufferedImage image, Polygon roiPolygon, double [][] stdXArray, double [][] stdYArray, int threshold, int channel) 
	{
		//get points > threshold
		List<Integer> listofX = getTransitions (stdXArray, threshold, channel);
		List<Integer> listofY = getTransitions (stdYArray, threshold, channel);
		
		ArrayList<Line2D> vertlines = getVerticalLinesFromIntervals(roiPolygon, listofX);
		ArrayList<Line2D> horzlines = getHorizontalLinesFromIntervals(roiPolygon, listofY);
		
		int averagewidth = (int) (roiPolygon.getBounds().getWidth() / (vertlines.size()-1));
		int checksize = averagewidth / 3;
		int deltainside = averagewidth / 8;
		for (Line2D line: vertlines) {
			line = adjustLine (image, line, checksize, deltainside);
		}
		
		averagewidth = (int) (roiPolygon.getBounds().getHeight() / (horzlines.size()-1));
		checksize = averagewidth / 3;
		deltainside = averagewidth / 8;
		for (Line2D line: horzlines) {
			line = adjustLine (image, line, checksize, deltainside);
		}

		List<List<Line2D>> linesArray = new ArrayList<List<Line2D>> ();
		linesArray.add(vertlines);
		linesArray.add(horzlines);
		
		return linesArray;
	}
	
	private List<Integer> getTransitions (double [][] arrayWithSTDvalues, int userSTDthreshold, int channel) 
	{
		List<Integer> listofpoints = new ArrayList<Integer> ();
		listofpoints.add(0);
		
		// assume that we look at the first point over threshold starting from the border
		boolean bDetectGetDown = true;
		double duserSTDthreshold = userSTDthreshold;
		double minSTDvalue = arrayWithSTDvalues[0][channel];
		double previousSTDvalue = minSTDvalue;
		int iofminSTDvalue = 0;
		
		for (int ix=1; ix < arrayWithSTDvalues.length; ix++) {
			double value = arrayWithSTDvalues[ix][channel];
			if (bDetectGetDown && ((previousSTDvalue>duserSTDthreshold) && (value < duserSTDthreshold))) {
				bDetectGetDown = false;
				iofminSTDvalue = ix;
				minSTDvalue = value;
			}
			else if (!bDetectGetDown) {
				if ((value > duserSTDthreshold) && (previousSTDvalue < duserSTDthreshold)) {
					bDetectGetDown = true;
					listofpoints.add(iofminSTDvalue);
				}
				else if (value < minSTDvalue) {
					minSTDvalue = value;
					iofminSTDvalue = ix;
				}
			}
			previousSTDvalue = value;
		}
		iofminSTDvalue = arrayWithSTDvalues.length-1;
		listofpoints.add(iofminSTDvalue);

		return listofpoints;
	}

	public static void convertLinesToSquares(Sequence seq, String baseName, int areaShrinkPCT) 
	{
		ArrayList<ROI2D> list = seq.getROI2Ds();
//		Collections.sort(list, new Tools.ROI2DNameComparator());
		List <ROI2DLine> vertRoiLines = new ArrayList <ROI2DLine> ();
		List <ROI2DLine> horizRoiLines = new ArrayList <ROI2DLine> ();
		for (ROI2D roi: list) {
			String name = roi.getName();
			if (name.contains("vertical"))
				vertRoiLines.add((ROI2DLine)roi);
			else if (name.contains("horizontal"))
				horizRoiLines.add((ROI2DLine) roi);
		}
		Collections.sort(vertRoiLines, new FmpTools.ROI2DLineLeftXComparator());
		Collections.sort(horizRoiLines, new FmpTools.ROI2DLineLeftYComparator());
		seq.removeAllROI();
		
		ROI2DLine roih1 = null;
		int row = 0;
		
		for (ROI2DLine roih2: horizRoiLines) {
			if (roih1 == null) {
				roih1 = roih2;
				continue;
			}
			ROI2DLine roiv1 = null;
			int col = 0;
			for (ROI2DLine roiv2: vertRoiLines) {
				if (roiv1 == null) {
					roiv1 = roiv2;
					continue;
				}
				List <Point2D> listpoints = new ArrayList<Point2D> ();
				listpoints.add(GeomUtil.getIntersection(roiv1.getLine(), roih1.getLine()));
				listpoints.add(GeomUtil.getIntersection(roiv1.getLine(), roih2.getLine()));
				listpoints.add(GeomUtil.getIntersection(roiv2.getLine(), roih2.getLine()));
				listpoints.add(GeomUtil.getIntersection(roiv2.getLine(), roih1.getLine()));
				
				areaShrink (listpoints, areaShrinkPCT);
				addPolygonROI (seq, listpoints, baseName, col, row);
				
				roiv1 = roiv2;
				col++;
			}
			roih1 = roih2;
			row++;
		}
	}
	
	private static void areaShrink(List <Point2D> listpoints, int areaShrinkPCT) {
		// assume 4 ordered points 0 (topleft), 1 (bottomleft), 2 (bottomright), 3 (topright)
		double xdeltatop = (listpoints.get(3).getX()-listpoints.get(0).getX() +1)*areaShrinkPCT/200 ;
		double xdeltabottom = (listpoints.get(2).getX()-listpoints.get(1).getX() +1)*areaShrinkPCT/200;
		double ydeltaleft = (listpoints.get(1).getY()-listpoints.get(0).getY() +1)*areaShrinkPCT/200;
		double ydeltaright = (listpoints.get(2).getY()-listpoints.get(3).getY() +1)*areaShrinkPCT/200;
		int i=0;
		listpoints.get(i).setLocation(listpoints.get(i).getX() + xdeltatop, listpoints.get(i).getY() + ydeltaleft); 	
		i=1;
		listpoints.get(i).setLocation(listpoints.get(i).getX() + xdeltabottom, listpoints.get(i).getY()-ydeltaleft);
		i=2;
		listpoints.get(i).setLocation(listpoints.get(i).getX() - xdeltabottom, listpoints.get(i).getY() - ydeltaright);
		i=3;
		listpoints.get(i).setLocation(listpoints.get(i).getX() - xdeltatop, listpoints.get(i).getY() + ydeltaright);
	}
	
	private static void addPolygonROI (Sequence seq, List<Point2D> points, String baseName, int columnnumber, int rownumber) 
	{
		ROI2DPolygon roiP = new ROI2DPolygon (points);
		roiP.setName(baseName+ String.format("_R%02d", rownumber) + String.format("_C%02d", columnnumber));
		roiP.setColor(Color.YELLOW);
		seq.addROI(roiP);
	}
	
	public static void createROISFromSelectedPolygon(Sequence seq, int ioption,
			String rootName,
			double colSpan, double colSize, double nbcols,
			double rowSpan, double rowSize, double nbrows) 
	{
		ROI2D roi = seq.getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("Select a 2D ROI polygon");
			return;
		}

		Polygon roiPolygon = FmpTools.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
		seq.removeAllROI();
		seq.addROI(roi, true);
		
		double colsSum = nbcols * (colSize + colSpan) + colSpan;
		double rowsSum = nbrows * (rowSize + rowSpan) + rowSpan;

		String baseName = null;

		for (int column=0; column< nbcols; column++) {
			
			double ratioX0 = ((colSize + colSpan)*column + colSpan) /colsSum;
			
			double x = roiPolygon.xpoints[0] + (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * ratioX0;
			double y = roiPolygon.ypoints[0] + (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * ratioX0;
			Point2D.Double ipoint0 = new Point2D.Double (x, y);
			
			x = roiPolygon.xpoints[1] + (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * ratioX0 ;
			y = roiPolygon.ypoints[1] + (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * ratioX0 ;
			Point2D.Double ipoint1 = new Point2D.Double (x, y);

			double ratioX1 = ((colSize + colSpan)*(column+1)) / colsSum;

			x = roiPolygon.xpoints[1]+ (roiPolygon.xpoints[2]-roiPolygon.xpoints[1]) * ratioX1;
			y = roiPolygon.ypoints[1]+ (roiPolygon.ypoints[2]-roiPolygon.ypoints[1]) * ratioX1;
			Point2D.Double ipoint2 = new Point2D.Double (x, y);
			
			x = roiPolygon.xpoints[0]+ (roiPolygon.xpoints[3]-roiPolygon.xpoints[0]) * ratioX1;
			y = roiPolygon.ypoints[0]+ (roiPolygon.ypoints[3]-roiPolygon.ypoints[0]) * ratioX1;
			Point2D.Double ipoint3 = new Point2D.Double (x, y);
			
			for (int row=0; row < nbrows; row++) {
				
				double ratioY0 = ( (rowSize + rowSpan)*row + rowSpan)/rowsSum;

				x = ipoint0.x + (ipoint1.x - ipoint0.x) * ratioY0;
				y = ipoint0.y + (ipoint1.y - ipoint0.y) * ratioY0;
				Point2D.Double point0 = new Point2D.Double (x, y);
				
				x = ipoint3.x + (ipoint2.x - ipoint3.x) * ratioY0;
				y = ipoint3.y + (ipoint2.y - ipoint3.y) * ratioY0;
				Point2D.Double point3 = new Point2D.Double (x, y);
				
				double ratioY1 = ( (rowSize + rowSpan)*(row+1)) / rowsSum;
				x = ipoint0.x + (ipoint1.x - ipoint0.x) * ratioY1;
				y = ipoint0.y + (ipoint1.y - ipoint0.y) * ratioY1;
				Point2D.Double point1 = new Point2D.Double (x, y);
				
				x = ipoint3.x + (ipoint2.x - ipoint3.x) * ratioY1;
				y = ipoint3.y + (ipoint2.y - ipoint3.y) * ratioY1;
				Point2D.Double point2 = new Point2D.Double (x, y);
				
				List<Point2D> points = new ArrayList<>();
				points.add(point0);
				points.add(point1);
				points.add(point2);
				points.add(point3);
				
				switch (ioption)
				{
				case 0:
					if (baseName == null)
						baseName = rootName + "_line ";
					addLineROI (seq, points, baseName, column, row);
					break;
				case 1:
					if (baseName == null)
						baseName = rootName + "_area ";
					addPolygonROI (seq, points, baseName, column, row);
					break;
				case 2:
				default:
					if (baseName == null)
						baseName = rootName + "_circle ";
					addEllipseROI (seq, points, baseName, column, row);
					break;
				}
			}
		}

		ArrayList<ROI2D> list = seq.getROI2Ds();
		Collections.sort(list, new FmpTools.ROI2DNameComparator());
	}
	
	private static void addEllipseROI (Sequence seq, List<Point2D> points, String baseName, int i, int j) 
	{
		ROI2DEllipse roiP = new ROI2DEllipse (points.get(0), points.get(2));
		roiP.setName(baseName+ String.format("_r%02d", j) + String.format("_c%02d", i));
		roiP.setColor(Color.YELLOW);
		seq.addROI(roiP);
	}
	
	private static void addLineROI (Sequence seq, List<Point2D> points, String baseName, int i, int j) 
	{
		ROI2DLine roiL1 = new ROI2DLine (points.get(0), points.get(1));
		roiL1.setName(baseName+ String.format("%02d", i/2)+"L");
		roiL1.setReadOnly(false);
		roiL1.setColor(Color.YELLOW);
		seq.addROI(roiL1, true);
		
		ROI2DLine roiL2 = new ROI2DLine (points.get(2), points.get(3));
		roiL2.setName(baseName+ String.format("%02d", i/2)+"R");
		roiL2.setReadOnly(false);
		roiL2.setColor(Color.YELLOW);
		seq.addROI(roiL2, true);
	}


}
