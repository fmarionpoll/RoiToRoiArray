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
import plugins.fmp.fmpSequence.OpenVirtualSequence;
import plugins.fmp.fmpSequence.SequenceVirtual;
import plugins.fmp.fmpTools.FmpTools;
import plugins.fmp.fmpTools.EnumImageOp;
import plugins.fmp.fmpTools.OverlayThreshold;


public class RoiToRoiArray extends EzPlug implements ViewerListener 
{
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

	
	// ----------------------------------
	
	@Override
	protected void initialize() {

		// 1) init variables
		splitAsComboBox 	= new EzVarText("Split polygon as ", new String[] {"vertical lines", "polygons", "circles"}, 1, false);
		ezRootnameComboBox	= new EzVarText("Names of ROIS begin with", new String[] {"gridA", "gridB", "gridC"}, 0, true);
		thresholdSTDFromChanComboBox = new EzVarText("Filter from", new String[] {"R", "G", "B", "R+B-2G"}, 3, false);
		ezSequence 			= new EzVarSequence("Select data from");
		
		ezNumberOfColumns	= new EzVarInteger("N columns ", 5, 1, 1000, 1);
		columnSize			= new EzVarInteger("column width ", 10, 0, 1000, 1);
		columnSpan			= new EzVarInteger("space btw. col. ", 0, 0, 1000, 1);
		ezNumberOfRows 		= new EzVarInteger("N rows ", 10, 1, 1000, 1); 
		rowWidth			= new EzVarInteger("row height ", 10, 0, 1000, 1);
		rowInterval 		= new EzVarInteger("space btw. row ", 0, 0, 1000, 1);
		areaShrink			= new EzVarInteger("area shrink (%)", 5, -100, 100, 1);
		
		adjustAndCenterEllipsesButton = new EzButton("Find leaf disks", new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				findLeafDiskIntoRectangles(); 
				}});
		ezFindLinesButton = new EzButton("Build histograms",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				virtualSequence = OpenVirtualSequence.initVirtualSequence(ezSequence.getValue());
				Sequence seq = ezSequence.getValue();
				BuildLinesFromSTD.findLines(seq); 
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
				Sequence seq = ezSequence.getValue();
				int t = virtualSequence.currentFrame;
				BuildLinesFromSTD.buildAutoGrid(seq, t); 
				}});
		convertLinesToSquaresButton = new EzButton("Convert lines to squares",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				int areaShrinkPCT = areaShrink.getValue();
				String rootname = ezRootnameComboBox.getValue();
				BuildROIsFromLines.convertLinesToSquares(virtualSequence.seq, rootname, areaShrinkPCT); 
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
	
// -----------------------------------	
	
	private void findLeafDiskIntoRectangles() {
		if (!overlayCheckBox.getValue())
			return;
		if (virtualSequence.cacheThresholdedImage == null)
			return;
		// get byte image (0, 1) that has been thresholded
		ArrayList<ROI2D> roiList = virtualSequence.seq.getROI2Ds();
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
				virtualSequence.seq.addOverlay(thresholdOverlay);
			}
			virtualSequence.cagesRoi2RoiArray.detect.threshold = thresholdOv.getValue();
			virtualSequence.seq.addOverlay(thresholdOverlay);
			updateOverlay();
		}
		else  {
			if (virtualSequence == null)
				return;
			if (thresholdOverlay != null) 
				virtualSequence.seq.removeOverlay(thresholdOverlay);
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
			virtualSequence.seq.addOverlay(thresholdOverlay);
		}
		EnumImageOp transformop = filterComboBox.getValue();

		thresholdOverlay.setSequence (virtualSequence);
		thresholdOverlay.setTransform(transformop);
		thresholdOverlay.setThresholdSingle(virtualSequence.cagesRoi2RoiArray.detect.threshold);
			
		if (thresholdOverlay != null) {
			thresholdOverlay.painterChanged();
		}
	}
	
	private void openFile() 
	{
		if (virtualSequence != null) 
			virtualSequence.close();
		
		Sequence seq = OpenVirtualSequence.openImagesOrAvi(null);
		virtualSequence = OpenVirtualSequence.initVirtualSequence(seq);
		String path = virtualSequence.getDirectory();
		if (path != null) 
		{
			XMLPreferences guiPrefs = this.getPreferences("gui");
			guiPrefs.put("lastUsedPath", path);
		}
		addSequenceToEzSequenceCombo(seq);
	}
	
	private void addSequenceToEzSequenceCombo (Sequence seq) 
	{
		ezSequence.setValue(seq);
		Viewer v = seq.getFirstViewer();
		v.addListener(RoiToRoiArray.this);
	}
	
	// ----------------------------------
	
	@Override
	public void clean() 
	{
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected void execute() 
	{
		String choice = splitAsComboBox.getValue();
		Sequence seq = ezSequence.getValue();
		double colSpan = columnSpan.getValue();
		double colSize = columnSize.getValue();
		double nbcols = ezNumberOfColumns.getValue(); 
		double rowSpan = rowInterval.getValue();
		double rowSize = rowWidth.getValue();
		double nbrows = ezNumberOfRows.getValue();
		String rootName = ezRootnameComboBox.getValue();
		
		if (choice == "vertical lines") 
		{
			BuildROIsFromLines.createROISFromSelectedPolygon(seq, 0, rootName, colSpan, colSize, nbcols, rowSpan, rowSize, nbrows);
		}
		else if (choice == "polygons") 
		{
			BuildROIsFromLines.createROISFromSelectedPolygon(seq, 1, rootName, colSpan, colSize, nbcols, rowSpan, rowSize, nbrows);
		}
		else if (choice == "circles") 
		{
			BuildROIsFromLines.createROISFromSelectedPolygon(seq, 2, rootName, colSpan, colSize, nbcols, rowSpan, rowSize, nbrows);
		}		
	}
	
	@Override
	public void viewerChanged(ViewerEvent event) 
	{
		if ((event.getType() == ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T))        
			virtualSequence.currentFrame = event.getSource().getPositionT() ; 
	}
	
	@Override
	public void viewerClosed(Viewer viewer) 
	{
		viewer.removeListener(this);
		virtualSequence = null;
	}
	
	private void openXMLFile() 
	{
		virtualSequence.seq.removeAllROI();
		virtualSequence.capillariesRoi2RoiArray.xmlReadROIsAndData(virtualSequence);
//		vSequence.cages.xmlReadCagesFromFile(vSequence);
	}
	
	private void saveXMLFile() 
	{
		virtualSequence.capillariesRoi2RoiArray.grouping = 1;
		virtualSequence.capillariesRoi2RoiArray.xmlWriteROIsAndDataNoFilter("roisarray.xml", virtualSequence);
	}
	
	private void changeGridName() 
	{
		List<ROI> roisList = virtualSequence.seq.getROIs(true);
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
		PluginLauncher.start(PluginLoader.getPlugin(RoiToRoiArray.class.getName()));
	}
}

