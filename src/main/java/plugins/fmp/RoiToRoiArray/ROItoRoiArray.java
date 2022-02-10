package plugins.fmp.RoiToRoiArray;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolygon;
import plugins.kernel.roi.roi2d.ROI2DEllipse;

import icy.gui.frame.IcyFrame;
import icy.gui.frame.progress.AnnounceFrame;
import icy.gui.util.GuiUtil;
import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.main.Icy;
import icy.plugin.PluginLauncher;
import icy.plugin.PluginLoader;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.preferences.XMLPreferences;
import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.DimensionId;
import icy.sequence.Sequence;
import icy.type.collection.array.Array1DUtil;
import icy.type.geom.GeomUtil;

import plugins.adufour.ezplug.EzButton;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarListener;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.ezplug.EzVarText;

import plugins.fmp.fmpSequence.SequenceVirtual;
import plugins.fmp.fmpTools.FmpTools;
import plugins.fmp.fmpTools.EnumImageOp;
import plugins.fmp.fmpTools.OverlayThreshold;


public class ROItoRoiArray extends EzPlug implements ViewerListener 
{
	// -------------------------------------- interface 
	
	EzButton		ezOpenFileButton;
	EzVarSequence   ezSequence;
	EzVarText		ezRootnameComboBox;
	EzButton		ezFindLinesButton;
	EzVarInteger 	ezNumberOfRows;
	EzVarInteger 	ezNumberOfColumns;
	EzVarInteger 	columnSize;
	EzVarInteger 	columnSpan;
	EzVarInteger 	rowWidth; 
	EzVarInteger 	rowInterval; 
	EzVarText 		splitAsComboBox;
	EzVarText 		thresholdSTDFromChanComboBox;
	EzButton		adjustAndCenterEllipsesButton;
	EzVarBoolean 	overlayCheckBox;
	EzVarEnum<EnumImageOp> filterComboBox;
	EzVarInteger 	thresholdOv;
	EzVarInteger 	thresholdSTD;
	EzButton		openXMLButton;
	EzButton		saveXMLButton;
	EzButton		generateGridButton;
	EzButton		generateAutoGridButton;
	EzButton		convertLinesToSquaresButton;
	EzVarInteger 	areaShrink;
	EzButton		changeGridNameButton;
	
	private OverlayThreshold thresholdOverlay = null;
	private SequenceVirtual virtualSequence = null;
	private IcyFrame mainChartFrame = null;
	private double [][] stdXArray = null;
	private double [][] stdYArray = null;
	
	// ----------------------------------
	
	@Override
	protected void initialize() {

		// 1) init variables
		splitAsComboBox = new EzVarText("Split polygon as ", new String[] {"vertical lines", "polygons", "circles"}, 1, false);
		ezRootnameComboBox= new EzVarText("Names of ROIS begin with", new String[] {"gridA", "gridB", "gridC"}, 0, true);
		thresholdSTDFromChanComboBox = new EzVarText("Filter from", new String[] {"R", "G", "B", "R+B-2G"}, 3, false);
		ezSequence = new EzVarSequence("Select data from");
		
		ezNumberOfColumns		= new EzVarInteger("N columns ", 5, 1, 1000, 1);
		columnSize		= new EzVarInteger("column width ", 10, 0, 1000, 1);
		columnSpan		= new EzVarInteger("space btw. col. ", 0, 0, 1000, 1);
		ezNumberOfRows 			= new EzVarInteger("N rows ", 10, 1, 1000, 1); 
		rowWidth		= new EzVarInteger("row height ", 10, 0, 1000, 1);
		rowInterval 	= new EzVarInteger("space btw. row ", 0, 0, 1000, 1);
		areaShrink		= new EzVarInteger("area shrink (%)", 5, -100, 100, 1);
		
		adjustAndCenterEllipsesButton = new EzButton("Find leaf disks", new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				findLeafDiskIntoRectangles(); 
				}});
		ezFindLinesButton = new EzButton("Build histograms",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				initVirtualSequence();
				findLines(); 
				}});
		ezOpenFileButton = new EzButton("Open file or sequence",  new ActionListener() 
		{ 
			public void actionPerformed(ActionEvent e) { 
				openFile(); 
				}});
		openXMLButton = new EzButton("Load XML file with ROIs",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				openXMLFile(); 
				}});
		saveXMLButton = new EzButton("Save ROIs to XML file",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				saveXMLFile(); 
				}});
		generateGridButton = new EzButton("Create grid",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				execute(); 
				}});
		generateAutoGridButton = new EzButton("Create lines / histograms > threshold",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				buildAutoGrid(); 
				}});
		convertLinesToSquaresButton = new EzButton("Convert lines to squares",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				convertLinesToSquares(); 
				}});
		changeGridNameButton = new EzButton("Set names of ROIs", new ActionListener () {
			public void actionPerformed(ActionEvent e) { 
				changeGridName(); 
				}});
		overlayCheckBox = new EzVarBoolean("build from overlay", false);
		overlayCheckBox.addVarChangeListener(new EzVarListener<Boolean>() {
             @Override
             public void variableChanged(EzVar<Boolean> source, Boolean newValue) {
            	 displayOverlay(newValue);
             }});

		filterComboBox = new EzVarEnum <EnumImageOp>("Filter as ", EnumImageOp.values(), 7);
		filterComboBox.addVarChangeListener(new EzVarListener<EnumImageOp>() {
			@Override
			public void variableChanged(EzVar<EnumImageOp> source, EnumImageOp newOp) {
				updateOverlay();
				}});
		
		thresholdOv = new EzVarInteger("threshold ", 70, 1, 255, 10);
		thresholdSTD = new EzVarInteger("threshold / selected filter", 500, 1, 10000, 10);
		thresholdOv.addVarChangeListener(new EzVarListener<Integer>() {
            @Override
            public void variableChanged(EzVar<Integer> source, Integer newValue) { updateThreshold(newValue); }
        });

		// 2) add variables to the interface

		EzGroup groupSequence = new EzGroup("Source data", ezSequence, ezOpenFileButton, openXMLButton);
		super.addEzComponent (groupSequence);

		EzGroup groupAutoDetect = new EzGroup("Automatic detection from lines", ezFindLinesButton, /*exportSTDButton,*/ 
				thresholdSTD, thresholdSTDFromChanComboBox, generateAutoGridButton, 
				areaShrink, convertLinesToSquaresButton);
		super.addEzComponent (groupAutoDetect);
	
		EzGroup groupManualDetect = new EzGroup("Manual definition of lines", splitAsComboBox, 
				ezNumberOfColumns, columnSize, columnSpan, 
				ezNumberOfRows, rowWidth, rowInterval, generateGridButton);
		super.addEzComponent (groupManualDetect);
		groupManualDetect.setFoldedState(true);

		EzGroup groupDetectDisks = new EzGroup("Detect leaf disks", overlayCheckBox, 
				filterComboBox, thresholdOv, adjustAndCenterEllipsesButton);
		super.addEzComponent (groupDetectDisks);
		groupDetectDisks.setFoldedState(true);
		
		EzGroup outputParameters = new EzGroup("Output data",  ezRootnameComboBox, changeGridNameButton, saveXMLButton);
		super.addEzComponent (outputParameters);
	}
	
	private void initVirtualSequence() {
		Sequence seq = ezSequence.getValue();
		if (seq == null || virtualSequence != null)
			return;
		
		virtualSequence = new SequenceVirtual(seq);
		seq.getFirstViewer().close();
		Icy.getMainInterface().addSequence(virtualSequence);
	}
	
	private void findLines() {
		Sequence seq = ezSequence.getValue(true);
		ROI2D roi = seq.getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("The frame must be a ROI2D POLYGON");
			return;
		}
		Polygon roiPolygon = FmpTools.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
		seq.removeAllROI();
		seq.addROI(roi, true);

		getSTD(roiPolygon.getBounds());
		getSTDRBminus2G();
		graphDisplay2Panels(stdXArray, stdYArray);
	}

	private double [][] getProfile (Line2D line) {
		List<Point2D> pointslist = getAllPointsAlongLine (line);		
		IcyBufferedImage image = virtualSequence.loadVImage(virtualSequence.currentFrame);
		double [][] profile = getValueForPointList(pointslist, image);
		return profile;
	}
	
	private List<Point2D> getAllPointsAlongLine(Line2D line) {
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
        else {
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
	
	public double[][] getValueForPointList( List<Point2D> pointList, IcyBufferedImage image ) {

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

	private void getSTD (Rectangle rect) {

		Point2D.Double [] refpoint = new Point2D.Double [4];
		refpoint [0] = new Point2D.Double (rect.x, 					rect.y);
		refpoint [1] = new Point2D.Double (rect.x, 					rect.y + rect.height - 1);
		refpoint [2] = new Point2D.Double (rect.x + rect.width - 1, rect.y + rect.height - 1);
		refpoint [3] = new Point2D.Double (rect.x + rect.width - 1, rect.y );
		
		int nYpoints = (int) (refpoint[1].y - refpoint[0].y +1); 
		int nXpoints = (int) (refpoint[3].x - refpoint[0].x +1); 
		double [][] sumXArray = new double [nXpoints][3];
		double [][] sum2XArray = new double [nXpoints][3];
		double [][] countXArray = new double [nXpoints][3];
		stdXArray = new double [nXpoints][4];
		double [][] sumYArray = new double [nYpoints][3];
		double [][] sum2YArray = new double [nYpoints][3];
		double [][] countYArray = new double [nYpoints][3];
		stdYArray = new double [nYpoints][4];
		
		for (int chan= 0; chan< 3; chan++) 
		{
			IcyBufferedImage virtualImage = virtualSequence.getImage(virtualSequence.currentFrame, 0, chan) ;
			if (virtualImage == null) 
			{
				System.out.println("An error occurred while reading image: " + virtualSequence.currentFrame );
				return;
			}
			int widthImage = virtualImage.getSizeX();
			double [] image1DArray = Array1DUtil.arrayToDoubleArray(virtualImage.getDataXY(0), virtualImage.isSignedDataType());
			
			double deltaXUp 	= (refpoint[3].x - refpoint[0].x +1);
			double deltaXDown 	= (refpoint[2].x - refpoint[1].x +1);
			double deltaYUp 	= (refpoint[3].y - refpoint[0].y +1);
			double deltaYDown 	= (refpoint[2].y - refpoint[1].y +1);
			
			for (int ix = 0; ix < nXpoints; ix++) {
				
				double xUp 		= refpoint[0].x + deltaXUp * ix / nXpoints;
				double yUp 		= refpoint[0].y + deltaYUp * ix / nXpoints;
				double xDown 	= refpoint[1].x + deltaXDown * ix / nXpoints;
				double yDown 	= refpoint[1].y + deltaYDown * ix / nXpoints;

				for (int iy = 0; iy < nYpoints; iy++) {
					double x = xUp + (xDown - xUp +1) * iy / nYpoints;
					double y = yUp + (yDown - yUp +1) * iy / nYpoints;
					
					int index = (int) x + ((int) y* widthImage);
					double value = image1DArray[index];
					double value2 = value*value;
					
					sumXArray[ix][chan] = sumXArray[ix][chan] + value;
					sum2XArray[ix][chan] = sum2XArray[ix][chan] + value2;
					countXArray[ix][chan] = countXArray[ix][chan] + 1;
					
					sumYArray[iy][chan] = sumYArray[iy][chan] + value;
					sum2YArray[iy][chan] = sum2YArray[iy][chan] + value2;
					countYArray[iy][chan] = countYArray[iy][chan] +1;
				}
			}
		}
		
		// compute variance
		for (int chan = 0; chan <3; chan++) {
			for (int ix = 0; ix < nXpoints; ix++) {
				double n 		= countXArray[ix][chan];
				double sum2 	= sum2XArray[ix][chan];
				double sumsum 	= sumXArray[ix][chan];
				sumsum 			= sumsum*sumXArray[ix][chan]/n;
				stdXArray[ix][chan] = (sum2 - sumsum)/(n-1);
			}
			
			for (int iy = 0; iy < nYpoints; iy++) {
				double n 		= countYArray[iy][chan];
				double sum2 	= sum2YArray[iy][chan];
				double sumsum 	= sumYArray[iy][chan];
				sumsum 			= sumsum*sumYArray[iy][chan]/n;
				stdYArray[iy][chan] = (sum2 - sumsum)/(n-1);
			}
		}
	}

	private void getSTDRBminus2G() {
		
		for (int i=0; i < stdXArray.length; i++) 
			stdXArray[i][3] = stdXArray [i][0]+stdXArray[i][2]-2*stdXArray[i][1];
			
		for (int i=0; i < stdYArray.length; i++) 
			stdYArray[i][3] = stdYArray[i][0]+stdYArray[i][2]-2*stdYArray[i][1];	
	}
	
	private XYSeriesCollection graphCreateXYDataSet(double [][] array, String rootName) {

		XYSeriesCollection xyDataset = new XYSeriesCollection();
		for (int chan = 0; chan < 4; chan++) 
		{
			XYSeries seriesXY = new XYSeries(rootName+chan);
			if (chan == 3)
				seriesXY.setDescription("1-2 + 3-2");
			int len = array.length;
			for ( int i = 0; i < len;  i++ )
			{
				double value = array[i][chan];
				seriesXY.add( i, value);
			}
			xyDataset.addSeries(seriesXY );
		}
		return xyDataset;
	}
	
	private void graphDisplay2Panels (double [][] arrayX, double [][] arrayY) {

		if (mainChartFrame != null) {
			mainChartFrame.removeAll();
			mainChartFrame.close();
		}

		final JPanel mainPanel = new JPanel(); 
		mainPanel.setLayout( new BoxLayout( mainPanel, BoxLayout.LINE_AXIS ) );
		String localtitle = "Variance along X and Y";
		mainChartFrame = GuiUtil.generateTitleFrame(localtitle, 
				new JPanel(), new Dimension(1400, 800), true, true, true, true);	

		int totalpoints = 0;
		ArrayList<XYSeriesCollection> xyDataSetList = new ArrayList <XYSeriesCollection>();
		XYSeriesCollection xyDataset = graphCreateXYDataSet(arrayX, "X chan ");
		xyDataSetList.add(xyDataset);
		totalpoints += xyDataset.getSeries(0).getItemCount();
		xyDataset = graphCreateXYDataSet(arrayY, "Y chan ");
		xyDataSetList.add(xyDataset);
		totalpoints += xyDataset.getSeries(0).getItemCount();
		
		for (int i=0; i<xyDataSetList.size(); i++) {
			xyDataset = xyDataSetList.get(i);
			int npoints = xyDataset.getSeries(0).getItemCount();
			JFreeChart xyChart = ChartFactory.createXYLineChart(null, null, null, xyDataset, PlotOrientation.VERTICAL, true, true, true);
			xyChart.setAntiAlias( true );
			xyChart.setTextAntiAlias( true );
			int drawWidth =  npoints * 800 / totalpoints;
			int drawHeight = 400;
			ChartPanel xyChartPanel = new ChartPanel(xyChart, drawWidth, drawHeight, drawWidth, drawHeight, drawWidth, drawHeight, false, false, true, true, true, true);
			mainPanel.add(xyChartPanel);
		}

		mainChartFrame.add(mainPanel);
		mainChartFrame.pack();
		Viewer v = virtualSequence.getFirstViewer();
		Rectangle rectv = v.getBounds();
		Point pt = new Point((int) rectv.getX(), (int) rectv.getY()+30);
		mainChartFrame.setLocation(pt);

		mainChartFrame.setVisible(true);
		mainChartFrame.addToDesktopPane ();
		mainChartFrame.requestFocus();
	}
	
	private List<List<Line2D>> buildLinesFromSTDProfile(Polygon roiPolygon, double [][] stdXArray, double [][] stdYArray, int threshold, int channel) {
		//get points > threshold
		List<Integer> listofX = getTransitions (stdXArray, threshold, channel);
		List<Integer> listofY = getTransitions (stdYArray, threshold, channel);
		
		ArrayList<Line2D> vertlines = getVerticalLinesFromIntervals(roiPolygon, listofX);
		ArrayList<Line2D> horzlines = getHorizontalLinesFromIntervals(roiPolygon, listofY);
		
		int averagewidth = (int) (roiPolygon.getBounds().getWidth() / (vertlines.size()-1));
		int checksize = averagewidth / 3;
		int deltainside = averagewidth / 8;
		for (Line2D line: vertlines) {
			line = adjustLine (line, checksize, deltainside);
		}
		
		averagewidth = (int) (roiPolygon.getBounds().getHeight() / (horzlines.size()-1));
		checksize = averagewidth / 3;
		deltainside = averagewidth / 8;
		for (Line2D line: horzlines) {
			line = adjustLine (line, checksize, deltainside);
		}

		List<List<Line2D>> linesArray = new ArrayList<List<Line2D>> ();
		linesArray.add(vertlines);
		linesArray.add(horzlines);
		
		return linesArray;
	}
	
	private int getIndexMinimumValue (double [][] profile) {
		
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
	
	private Line2D adjustLine (Line2D line, int checksize, int deltainside) {
			
		Rectangle rect = line.getBounds();	
		Line2D bestline = new Line2D.Double();
		
		// horizontal lines
		if (rect.getWidth() >= rect.getHeight()) {
			// check profile of a line perpendicular to this line at one end (linetest1) and then at the other end (linetest2)
			Line2D linetest1 = new Line2D.Double (line.getX1()+deltainside, line.getY1()-checksize, line.getX1()+deltainside, line.getY2()+checksize);
			int iy1min = getIndexMinimumValue(getProfile(linetest1))-checksize;
			Line2D linetest2 = new Line2D.Double (line.getX2()-deltainside, line.getY2()-checksize, line.getX2()-deltainside, line.getY2()+checksize);
			int iy2min = getIndexMinimumValue(getProfile(linetest2))-checksize;
			bestline.setLine(line.getX1(), line.getY1()-iy1min, line.getX2(), line.getY2()-iy2min);
		}
		// vertical lines
		else {
			Line2D linetest1 = new Line2D.Double (line.getX1()-checksize, line.getY1()+deltainside, line.getX1()+checksize, line.getY2()+deltainside);
			int iy1min = getIndexMinimumValue(getProfile(linetest1))-checksize;
			Line2D linetest2 = new Line2D.Double (line.getX2()-checksize, line.getY2()-deltainside, line.getX2()+checksize, line.getY2()-deltainside);
			int iy2min = getIndexMinimumValue(getProfile(linetest2))-checksize;
			bestline.setLine(line.getX1(), line.getY1()-iy1min, line.getX2(), line.getY2()-iy2min);
		}
		return bestline;
	}
	
	private ArrayList<Line2D> getVerticalLinesFromIntervals(Polygon roiPolygon, List<Integer> listofX) {

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
	
	private ArrayList<Line2D> getHorizontalLinesFromIntervals(Polygon roiPolygon, List<Integer> listofY) {

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
	
	private List<Integer> getTransitions (double [][] arrayWithSTDvalues, int userSTDthreshold, int channel) {

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

	private void buildROIsFromLines (List<List<Line2D>> linesArray) {
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
				ezSequence.getValue(true).addROI(roiL1, true);
				i++;
			}
			itype++;
		}
	}
	
	private void buildAutoGrid () {
		ROI2D roi = ezSequence.getValue(true).getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("The frame must be a ROI 2D POLYGON");
			return;
		}
		Polygon roiPolygon = FmpTools.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
		ezSequence.getValue(true).removeAllROI();
		ezSequence.getValue(true).addROI(roi, true);
		int channel = 0;
		String choice = thresholdSTDFromChanComboBox.getValue();
		if (choice.contains("R+B-2G"))
			channel = 3;
		else if (choice.contains("R"))
			channel = 0;
		else if (choice.contains("G"))
			channel = 1;
		else
			channel = 2;
		//buildROIsFromSTDProfile( roiPolygon, thresholdSTD.getValue(), channel);
		List<List<Line2D>> linesArray = buildLinesFromSTDProfile( roiPolygon, stdXArray, stdYArray, thresholdSTD.getValue(), channel);
		
		buildROIsFromLines(linesArray);
		//expandROIsToMinima(0);
	}
	
	private void convertLinesToSquares() {
		ArrayList<ROI2D> list = ezSequence.getValue(true).getROI2Ds();
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
		ezSequence.getValue(true).removeAllROI();
		
		ROI2DLine roih1 = null;
		int row = 0;
		int areaShrinkPCT = areaShrink.getValue();
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
				addPolygonROI (listpoints, ezRootnameComboBox.getValue(), col, row);
				
				roiv1 = roiv2;
				col++;
			}
			roih1 = roih2;
			row++;
		}
	}
	
	private void areaShrink(List <Point2D> listpoints, int areaShrinkPCT) {
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
	
// -----------------------------------	
	private void findLeafDiskIntoRectangles() {
		if (!overlayCheckBox.getValue())
			return;
		if (virtualSequence.cacheThresholdedImage == null)
			return;
		// get byte image (0, 1) that has been thresholded
		ArrayList<ROI2D> roiList = virtualSequence.getROI2Ds();
		Collections.sort(roiList, new FmpTools.ROI2DNameComparator());
		
		for (ROI2D roi:roiList) {
			if (!roi.getName().contains("grid"))
				continue;

			Rectangle rectGrid = roi.getBounds();
			IcyBufferedImage img = IcyBufferedImageUtil.getSubImage(virtualSequence.cacheThresholdedImage, rectGrid);
			byte [] binaryData = img.getDataXYAsByte(0);
			int sizeX = img.getSizeX();
			int sizeY = img.getSizeY();

			getPixelsConnected (sizeX, sizeY, binaryData);
			getBlobsConnected(sizeX, sizeY, binaryData);
			byte leafBlob = getLargestBlob(binaryData);
			eraseAllBlobsExceptOne(leafBlob, binaryData);
			Rectangle leafBlobRect = getBlobRectangle( leafBlob, sizeX, sizeY, binaryData);
			
			addLeafROIinGridRectangle(leafBlobRect, roi);
		}
		System.out.println("Done");
	}
	
	private void addLeafROIinGridRectangle (Rectangle leafBlobRect, ROI2D roi) {

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
		ezSequence.getValue(true).addROI(roiP);
	}
	
	private Rectangle getBlobRectangle(byte blobNumber, int sizeX, int sizeY, byte [] binaryData) {
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
	
	private int getPixelsConnected (int sizeX, int sizeY, byte [] binaryData) 
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
	
	private void getBlobsConnected (int sizeX, int sizeY, byte[] binaryData) {
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
	
	private byte getLargestBlob(byte[] binaryData) 
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
	
	private void eraseAllBlobsExceptOne(byte blobIDToKeep, byte [] binaryData) {
		for (int i=0; i< binaryData.length; i++) {
			if (binaryData[i] != blobIDToKeep)
				binaryData[i] = -1;
			else
				binaryData[i] = 1;
		}
	}
	
	private void changeAllBlobNumber1Into2 (byte oldvalue, byte newvalue, byte [] binaryData) 
	{
		for (int i=0; i< binaryData.length; i++)
			if (binaryData[i] == oldvalue)
				binaryData[i] = newvalue;
	}
	
	private int getNumberOfPixelEqualToValue (byte value, byte [] binaryData) 
	{
		int sum = 0;
		for (int i=0; i< binaryData.length; i++)
			if (binaryData[i] == value)
				sum++;
		return sum;
	}
	
	private byte getMaximumBlobNumber (byte [] binaryData) 
	{
		byte max = 0;
		for (int i=0; i< binaryData.length; i++)
			if (binaryData[i] > max)
				max = binaryData[i];
		return max;
	}
	
	private void displayOverlay (Boolean newValue) {
		if (virtualSequence == null)
			return;

		if (newValue) {
			
			if (thresholdOverlay == null) {
				thresholdOverlay = new OverlayThreshold(virtualSequence);
				virtualSequence.addOverlay(thresholdOverlay);
			}
			virtualSequence.cagesRoi2RoiArray.detect.threshold = thresholdOv.getValue();
			virtualSequence.addOverlay(thresholdOverlay);
			updateOverlay();
		}
		else  {
			if (virtualSequence == null)
				return;
			if (thresholdOverlay != null) 
				virtualSequence.removeOverlay(thresholdOverlay);
			thresholdOverlay = null;
		}
	}
	
	private void updateThreshold (int newValue) {
		if (virtualSequence == null)
			return;
		
		virtualSequence.cagesRoi2RoiArray.detect.threshold = thresholdOv.getValue();
		updateOverlay();
	}
	
	private void updateOverlay () {
		if (virtualSequence == null)
			return;
		if (thresholdOverlay == null) {
			thresholdOverlay = new OverlayThreshold(virtualSequence);
			virtualSequence.addOverlay(thresholdOverlay);
		}
		EnumImageOp transformop = filterComboBox.getValue();

		thresholdOverlay.setSequence (virtualSequence);
		thresholdOverlay.setTransform(transformop);
		thresholdOverlay.setThresholdSingle(virtualSequence.cagesRoi2RoiArray.detect.threshold);
			
		if (thresholdOverlay != null) {
			thresholdOverlay.painterChanged();
		}
	}
	
	private void openFile() {

		if (virtualSequence != null) 
		{
			virtualSequence.close();
		}
		
		virtualSequence = new SequenceVirtual();
		String path = virtualSequence.loadInputVirtualStack(null);
		if (path != null) 
		{
			XMLPreferences guiPrefs = this.getPreferences("gui");
			guiPrefs.put("lastUsedPath", path);
			initInputSeq();
		}
	}
	
	// ----------------------------------
	private void addEllipseROI (List<Point2D> points, String baseName, int i, int j) {
		
		ROI2DEllipse roiP = new ROI2DEllipse (points.get(0), points.get(2));
		roiP.setName(baseName+ String.format("_r%02d", j) + String.format("_c%02d", i));
		roiP.setColor(Color.YELLOW);
		ezSequence.getValue(true).addROI(roiP);
	}
	
	private void addPolygonROI (List<Point2D> points, String baseName, int columnnumber, int rownumber) {
		
		ROI2DPolygon roiP = new ROI2DPolygon (points);
		roiP.setName(baseName+ String.format("_R%02d", rownumber) + String.format("_C%02d", columnnumber));
		roiP.setColor(Color.YELLOW);
		ezSequence.getValue(true).addROI(roiP);
	}
	
	private void addLineROI (List<Point2D> points, String baseName, int i, int j) {
		
		ROI2DLine roiL1 = new ROI2DLine (points.get(0), points.get(1));
		roiL1.setName(baseName+ String.format("%02d", i/2)+"L");
		roiL1.setReadOnly(false);
		roiL1.setColor(Color.YELLOW);
		ezSequence.getValue(true).addROI(roiL1, true);
		
		ROI2DLine roiL2 = new ROI2DLine (points.get(2), points.get(3));
		roiL2.setName(baseName+ String.format("%02d", i/2)+"R");
		roiL2.setReadOnly(false);
		roiL2.setColor(Color.YELLOW);
		ezSequence.getValue(true).addROI(roiL2, true);
	}
	
	private void createROISFromSelectedPolygon(int ioption) {
		
		ROI2D roi = ezSequence.getValue(true).getSelectedROI2D();
		if ( ! ( roi instanceof ROI2DPolygon ) ) {
			new AnnounceFrame("Select a 2D ROI polygon");
			return;
		}

		Polygon roiPolygon = FmpTools.orderVerticesofPolygon (((ROI2DPolygon) roi).getPolygon());
		ezSequence.getValue(true).removeAllROI();
		ezSequence.getValue(true).addROI(roi, true);
		
		double colSpan = columnSpan.getValue();
		double colSize = columnSize.getValue();
		double nbcols = ezNumberOfColumns.getValue(); 
		double colsSum = nbcols * (colSize + colSpan) + colSpan;
		
		double rowSpan = rowInterval.getValue();
		double rowSize = rowWidth.getValue();
		double nbrows = ezNumberOfRows.getValue();
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
			
			for (int row=0; row < ezNumberOfRows.getValue(); row++) {
				
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
						baseName = ezRootnameComboBox.getValue()+"_line ";
					addLineROI (points, baseName, column, row);
					break;
				case 1:
					if (baseName == null)
						baseName = ezRootnameComboBox.getValue()+"_area ";
					addPolygonROI (points, baseName, column, row);
					break;
				case 2:
				default:
					if (baseName == null)
						baseName = ezRootnameComboBox.getValue()+"_circle ";
					addEllipseROI (points, baseName, column, row);
					break;
				}
			}
		}

		ArrayList<ROI2D> list = ezSequence.getValue(true).getROI2Ds();
		Collections.sort(list, new FmpTools.ROI2DNameComparator());
	}
	
	@Override
	public void clean() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void execute() {
		String choice = splitAsComboBox.getValue();
		if (choice == "vertical lines") {
			createROISFromSelectedPolygon(0);
		}
		else if (choice == "polygons") {
			createROISFromSelectedPolygon(1);
		}
		else if (choice == "circles") {
			createROISFromSelectedPolygon(2);
		}		
	}
	
	private void initInputSeq () 
	{
		addSequence(virtualSequence);
		ezSequence.setValue(virtualSequence);
		Viewer v = virtualSequence.getFirstViewer();
		v.addListener(ROItoRoiArray.this);

		virtualSequence.removeAllImages();
	}
	
	@Override
	public void viewerChanged(ViewerEvent event) {
		if ((event.getType() == ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T))        
			virtualSequence.currentFrame = event.getSource().getPositionT() ; 
	}
	
	@Override
	public void viewerClosed(Viewer viewer) {
		viewer.removeListener(this);
		virtualSequence = null;
	}
	
	private void openXMLFile() {
		virtualSequence.removeAllROI();
		virtualSequence.capillariesRoi2RoiArray.xmlReadROIsAndData(virtualSequence);
//		vSequence.cages.xmlReadCagesFromFile(vSequence);
	}
	
	private void saveXMLFile() {
		virtualSequence.capillariesRoi2RoiArray.grouping = 1;
		virtualSequence.capillariesRoi2RoiArray.xmlWriteROIsAndDataNoFilter("roisarray.xml", virtualSequence);
	}
	
	private void changeGridName() {
		List<ROI> roisList = virtualSequence.getROIs(true);
		String baseName = ezRootnameComboBox.getValue();
		
		for (ROI roi : roisList) {
			String cs = roi.getName();
			int firstunderscore = cs.indexOf("_");
			cs = baseName + cs.substring(firstunderscore);
			roi.setName(cs);
		}
	}
	
	public static void main(String[] args)
	{
		Icy.main(args);
		PluginLauncher.start(PluginLoader.getPlugin(ROItoRoiArray.class.getName()));
	}
}

