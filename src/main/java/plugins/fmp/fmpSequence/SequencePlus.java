package plugins.fmp.fmpSequence;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;

import org.w3c.dom.Node;

import icy.image.IcyBufferedImage;
import icy.roi.ROI2D;
import icy.type.geom.Polyline2D;
import icy.util.XMLUtil;
import plugins.fmp.fmpTools.FmpTools;
import plugins.fmp.fmpTools.EnumArrayListType;
import plugins.fmp.fmpTools.EnumImageOp;
import plugins.fmp.fmpTools.OverlayThreshold;
import plugins.fmp.fmpTools.OverlayTrapMouse;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;

public class SequencePlus extends SequenceVirtual  {
	
	public 	ArrayList<Integer> derivedValuesArrayList= new ArrayList<Integer>(); // (derivative) result of the detection of the capillary level
	public 	boolean 		hasChanged 				= false;
	public 	boolean 		bStatusChanged 			= false;
	public 	boolean 		detectTop 				= true;
	public 	boolean 		detectBottom 			= true;
	public 	boolean 		detectAllLevel 			= true;
	public 	boolean 		detectAllGulps 			= true;
	public 	int 			direction 				= 0;
	public 	int				detectLevelThreshold 	= 35;
	public 	int 			detectGulpsThreshold 	= 90;
	public	EnumImageOp		transformForLevels 		= EnumImageOp.R2MINUS_GB;
	public	EnumImageOp 	transformForGulps 		= EnumImageOp.XDIFFN;
	
	public 	LocalDateTime	startDate				= null;
	public 	LocalDateTime	endDate					= null;
	public 	long			minutesBetweenImages	= 1;
	public 	OverlayThreshold thresholdOverlay 		= null;
	public 	OverlayTrapMouse trapOverlay 			= null;
	
	public 	KymoMeasures	kymoMeasures			= new KymoMeasures ();
	
	// -----------------------------------------------------
	
	public SequencePlus() {
		super ();
	}
	
	public SequencePlus(String name, IcyBufferedImage image) {
		super (name, image);
		kymoMeasures.imageWidth = image.getWidth();
		kymoMeasures.imageHeight = image.getHeight();
	}
	
	public ArrayList<Integer> getArrayListFromRois (EnumArrayListType option) {
		
		ArrayList<ROI2D> listRois = seq.getROI2Ds();
		if (listRois == null)
			return null;
		ArrayList<Integer> datai = null;
		
		switch (option) {
		case derivedValues:
			datai = derivedValuesArrayList;
			break;
		case cumSum:
			datai = new ArrayList<Integer>(Collections.nCopies(seq.getWidth(), 0));
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
	
	public ArrayList<Integer> subtractTi(ArrayList<Integer > array) {
		if (array == null)
			return null;
		int item0 = array.get(0);
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
			item0 = value;
		}
		return array;
	}
	
	public ArrayList<Integer> subtractT0 (ArrayList<Integer> array) {

		if (array == null)
			return null;
		int item0 = array.get(0);
		for (int index= 0; index < array.size(); index++) {
			int value = array.get(index);
			array.set(index, value-item0);
		}
		return array;
	}
	
	private ArrayList<Integer> copyFirstRoiMatchingFilterToDataArray (String filter) {
		
		ArrayList<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName().contains(filter)) { 
				interpolateMissingPointsAlongXAxis ((ROI2DPolyLine)roi);
				return transfertRoiYValuesToDataArray((ROI2DPolyLine)roi);
			}
		}
		return null;
	}
	
	private void addRoisMatchingFilterToCumSumDataArray (String filter, ArrayList<Integer> cumSumArray) {
		
		ArrayList<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi: listRois) {
			if (roi.getName().contains(filter)) 
				addRoitoCumulatedSumArray((ROI2DPolyLine) roi, cumSumArray);
		}
		return ;
	}

	private ArrayList<Integer> transfertRoiYValuesToDataArray(ROI2DPolyLine roiLine) {

		Polyline2D line = roiLine.getPolyline2D();
		ArrayList<Integer> intArray = new ArrayList<Integer> (line.npoints);
		for (int i=0; i< line.npoints; i++) 
			intArray.add((int) line.ypoints[i]);

		return intArray;
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
	
	public void validateRois() {

		ArrayList<ROI2D> listRois = seq.getROI2Ds();
		for (ROI2D roi: listRois) {

			// interpolate missing points if necessary
			if (roi.getName().contains("level") || roi.getName().contains("gulp")) {
				interpolateMissingPointsAlongXAxis ((ROI2DPolyLine) roi);
				continue;
			}

			// if gulp not found - add an index to it
			if (roi instanceof ROI2DPolyLine) {
				ROI2DPolyLine roiLine = (ROI2DPolyLine) roi;
				Polyline2D line = roiLine.getPolyline2D();
				roi.setName("gulp"+String.format("%07d", (int) line.xpoints[0]));
				roi.setColor(Color.red);						// set color to red
			}
		}
		Collections.sort(listRois, new FmpTools.ROI2DNameComparator());
	}
	
	public boolean loadXMLKymographAnalysis (String directory) {
	
		if (directory == null)
			return false;
		
		String resultsDirectory = directory;
		String subDirectory = File.separator+"results";
		if (!resultsDirectory.contains (subDirectory))
			resultsDirectory += subDirectory;
		Path resultsPath = Paths.get(resultsDirectory);
		if (Files.notExists(resultsPath)) 
			return false; 
		
		seq.setFilename(resultsDirectory+File.separator+seq.getName()+".xml");
		String filename = resultsDirectory+File.separator+seq.getName()+".xml";
		Path filenamePath = Paths.get(filename);
		if (Files.notExists(filenamePath)) 
			return false; 
		
		seq.removeAllROI();
		boolean flag = seq.loadXMLData();
		if (flag) {
			ArrayList<ROI2D> listRois = seq.getROI2Ds();
			for (ROI2D roi: listRois) {
				seq.addROI(roi);
			}
		}
		
		Node myNode = seq.getNode(seq.getName()+"_parameters");
		detectTop = XMLUtil.getElementBooleanValue(myNode, "detectTop", true);
		detectBottom = XMLUtil.getElementBooleanValue(myNode, "detectBottom", false);
		detectAllLevel = XMLUtil.getElementBooleanValue(myNode, "detectAllLevel", true);
		detectAllGulps = XMLUtil.getElementBooleanValue(myNode, "detectAllGulps", true); 
		bStatusChanged = XMLUtil.getElementBooleanValue(myNode, "bStatusChanged", false);

		int dummy = XMLUtil.getElementIntValue(myNode, "transformForLevels", 0);
		transformForLevels = EnumImageOp.values()[dummy];
		direction = XMLUtil.getElementIntValue(myNode, "direction", 0);
		detectLevelThreshold = XMLUtil.getElementIntValue(myNode, "detectLevelThreshold", 35);
		detectGulpsThreshold = XMLUtil.getElementIntValue(myNode, "detectGulpsThreshold", 75);
		int dummy2 = XMLUtil.getElementIntValue(myNode, "transformForGulps", 3);
		transformForGulps = EnumImageOp.values()[dummy2];
		
		analysisStart = XMLUtil.getElementIntValue(myNode, "analysisStart", 0);
		analysisEnd = XMLUtil.getElementIntValue(myNode, "analysisEnd", -1);
		analysisStep = XMLUtil.getElementIntValue(myNode, "analysisStep", 1);
		
		kymoMeasures.imageWidth = XMLUtil.getElementIntValue(myNode, "imageWidth", 1);
		kymoMeasures.imageHeight = XMLUtil.getElementIntValue(myNode, "imageHeight", 1);

		return flag;
	}

	public boolean saveXMLKymographAnalysis(String directory) {

		// check if directory is present. If not, create it
		String resultsDirectory = directory;
		String subDirectory = File.separator+"results";
		if (!resultsDirectory.contains (subDirectory))
			resultsDirectory += subDirectory;
		resultsDirectory += File.separator;
		Path resultsPath = Paths.get(resultsDirectory);
		if (Files.notExists(resultsPath)) {
			try {
				resultsPath = Files.createDirectories(resultsPath);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		Node myNode = seq.getNode(seq.getName()+"_parameters");
		XMLUtil.setElementBooleanValue(myNode, "detectTop", detectTop);
		XMLUtil.setElementBooleanValue(myNode, "detectBottom", detectBottom);
		XMLUtil.setElementBooleanValue(myNode, "detectAllLevel", detectAllLevel);
		XMLUtil.setElementBooleanValue(myNode, "detectAllGulps", detectAllGulps); 
		XMLUtil.setElementBooleanValue(myNode, "bStatusChanged", bStatusChanged);

		int dummy1 = transformForLevels.ordinal(); 
		XMLUtil.setElementIntValue(myNode, "transformForLevels", dummy1);
		XMLUtil.setElementIntValue(myNode, "direction", direction);
		XMLUtil.setElementIntValue(myNode, "detectLevelThreshold", detectLevelThreshold);
		XMLUtil.setElementIntValue(myNode, "detectGulpsThreshold", detectGulpsThreshold);
		int dummy2 = transformForGulps.ordinal();
		XMLUtil.setElementIntValue(myNode, "transformForGulps", dummy2);
		
		XMLUtil.setElementIntValue(myNode, "analysisStart", (int) analysisStart);
		XMLUtil.setElementIntValue(myNode, "analysisEnd", (int) analysisEnd);
		XMLUtil.setElementIntValue(myNode, "analysisStep", analysisStep);
		
		XMLUtil.setElementIntValue(myNode, "imageWidth", kymoMeasures.imageWidth);
		XMLUtil.setElementIntValue(myNode, "imageHeight", kymoMeasures.imageHeight);
		
		seq.setFilename(resultsDirectory+seq.getName()+".xml");
		return seq.saveXMLData();
	}

	// ----------------------------
	public void setThresholdOverlay(boolean bActive) {
		if (bActive) {
			if (thresholdOverlay == null) 
				thresholdOverlay = new OverlayThreshold(this);
			if (!seq.contains(thresholdOverlay)) 
				seq.addOverlay(thresholdOverlay);
			thresholdOverlay.setSequence (this);
		}
		else {
			if (thresholdOverlay != null && seq.contains(thresholdOverlay) )
				seq.removeOverlay(thresholdOverlay);
			thresholdOverlay = null;
		}
	}
	
	public void setThresholdOverlayParametersSingle(EnumImageOp transf, int threshold) {
		thresholdOverlay.setTransform(transf);
		thresholdOverlay.setThresholdSingle(threshold);
		thresholdOverlay.painterChanged();
	}
	
	public void setThresholdOverlayParametersColors(EnumImageOp transf, ArrayList <Color> colorarray, int colordistancetype, int colorthreshold) {
		thresholdOverlay.setTransform(transf);
		thresholdOverlay.setThresholdColor(colorarray, colordistancetype, colorthreshold);
		thresholdOverlay.painterChanged();
	}

	public void setMouseTrapOverlay (boolean bActive, JButton pickColorButton, JComboBox<Color> colorPickCombo) {
		if (bActive) {
			if (trapOverlay == null)
				trapOverlay = new OverlayTrapMouse (pickColorButton, colorPickCombo);
			if (!seq.contains(trapOverlay))
				seq.addOverlay(trapOverlay);
		}
		else {
			if (trapOverlay != null && seq.contains(trapOverlay))
				seq.removeOverlay(trapOverlay);
			trapOverlay = null;
		}
	}
	

}
