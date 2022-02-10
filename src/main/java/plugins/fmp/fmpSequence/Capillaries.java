package plugins.fmp.fmpSequence;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.edit.ROIAddsSequenceEdit;
import icy.util.XMLUtil;
import plugins.fmp.fmpTools.FmpTools;
import plugins.kernel.roi.roi2d.ROI2DLine;
import plugins.kernel.roi.roi2d.ROI2DPolyLine;
import plugins.kernel.roi.roi2d.ROI2DShape;

public class Capillaries {
	
	public double 	volume 				= 5.;
	public double 	pixels 				= 300.;
	public int		grouping 			= 2;
	public String 	sourceName 			= null;
	public ArrayList <ROI2DShape> capillariesArrayList 	= new ArrayList <ROI2DShape>();	
	public long 	analysisStart 		= 0;
	public long 	analysisEnd 		= 0;
	public int 		analysisStep 		= 1;
	
	public String 	boxID				= new String("boxID");
	public String	experiment			= new String("experiment");
	public String 	comment				= new String("...");
	public String 	stimulusR			= new String("stimulusR");
	public String 	concentrationR		= new String("xmMR");
	public String 	stimulusL			= new String("stimulusL");
	public String 	concentrationL		= new String("xmML");
	
	public boolean xmlReadCapillaryParameters (Document doc) {
		String nodeName = "capillaryTrack";
		// read local parameters
		Node node = XMLUtil.getElement(XMLUtil.getRootElement(doc), nodeName);
		if (node == null)
			return false;

		Element xmlElement = XMLUtil.getElement(node, "Parameters");
		if (xmlElement == null) 
			return false;

		Element xmlVal = XMLUtil.getElement(xmlElement, "file");
		sourceName = XMLUtil.getAttributeValue(xmlVal, "ID", null);
		
		xmlVal = XMLUtil.getElement(xmlElement, "Grouping");
		grouping = XMLUtil.getAttributeIntValue(xmlVal, "n", 2);
		
		xmlVal = XMLUtil.getElement(xmlElement, "capillaryVolume");
		volume = XMLUtil.getAttributeDoubleValue(xmlVal, "volume_ul", Double.NaN);

		xmlVal = XMLUtil.getElement(xmlElement, "capillaryPixels");
		pixels = XMLUtil.getAttributeDoubleValue(xmlVal, "npixels", Double.NaN);

		xmlVal = XMLUtil.getElement(xmlElement, "analysis");
		if (xmlVal != null) {
			analysisStart 	= XMLUtil.getAttributeLongValue(xmlVal, "start", 0);
			analysisEnd 	= XMLUtil.getAttributeLongValue(xmlVal, "end", 0);
			analysisStep 	= XMLUtil.getAttributeIntValue(xmlVal, "step", 1);
		}

		xmlVal = XMLUtil.getElement(xmlElement, "LRstimulus");
		if (xmlVal != null) {
			stimulusR 		= XMLUtil.getAttributeValue(xmlVal, "stimR", "stimR");
			concentrationR 	= XMLUtil.getAttributeValue(xmlVal, "concR", "concR");
			stimulusL 		= XMLUtil.getAttributeValue(xmlVal, "stimL", "stimL");
			concentrationL 	= XMLUtil.getAttributeValue(xmlVal, "concL", "concL");
		}
		
		xmlVal = XMLUtil.getElement(xmlElement, "Experiment");
		if (xmlVal != null) {
			boxID 		= XMLUtil.getAttributeValue(xmlVal, "boxID", "boxID");
			experiment 	= XMLUtil.getAttributeValue(xmlVal, "expt", "experiment");
			comment 	= XMLUtil.getAttributeValue(xmlVal, "comment", ".");
		}
		return true;
	}
	
	private boolean xmlWriteCapillaryParameters (Document doc, SequenceVirtual seq) {
		String nodeName = "capillaryTrack";
		Node node = XMLUtil.addElement(XMLUtil.getRootElement(doc), nodeName);
		if (node == null)
			return false;
		
		Element xmlElement = XMLUtil.addElement(node, "Parameters");
		
		Element xmlVal = XMLUtil.addElement(xmlElement, "file");
		sourceName = seq.getFileName();
		XMLUtil.setAttributeValue(xmlVal, "ID", sourceName);
		
		xmlVal = XMLUtil.addElement(xmlElement, "Grouping");
		XMLUtil.setAttributeIntValue(xmlVal, "n", grouping);
		
		xmlVal = XMLUtil.addElement(xmlElement, "capillaryVolume");
		XMLUtil.setAttributeDoubleValue(xmlVal, "volume_ul", volume);

		xmlVal = XMLUtil.addElement(xmlElement, "capillaryPixels");
		XMLUtil.setAttributeDoubleValue(xmlVal, "npixels", pixels);

		xmlVal = XMLUtil.addElement(xmlElement, "analysis");
		XMLUtil.setAttributeLongValue(xmlVal, "start", seq.analysisStart);
		XMLUtil.setAttributeLongValue(xmlVal, "end", seq.analysisEnd); 
		XMLUtil.setAttributeIntValue(xmlVal, "step", seq.analysisStep); 
		
		xmlVal = XMLUtil.addElement(xmlElement,  "LRstimulus");
		XMLUtil.setAttributeValue(xmlVal, "stimR", stimulusR);
		XMLUtil.setAttributeValue(xmlVal, "concR", concentrationR);
		XMLUtil.setAttributeValue(xmlVal, "stimL", stimulusL);
		XMLUtil.setAttributeValue(xmlVal, "concL", concentrationL);

		xmlVal = XMLUtil.addElement(xmlElement,  "Experiment");
		XMLUtil.setAttributeValue(xmlVal, "boxID", boxID);
		XMLUtil.setAttributeValue(xmlVal, "expt", experiment);
		XMLUtil.setAttributeValue(xmlVal, "comment", comment);

		return true;
	}
	
	public void extractLinesFromSequence(SequenceVirtual seq) {

		capillariesArrayList.clear();
		ArrayList<ROI2D> list = seq.getROI2Ds();
		 
		for (ROI2D roi:list)
		{
			if ((roi instanceof ROI2DShape) == false)
				continue;
			if (!roi.getName().contains("line"))
				continue;
			if (roi instanceof ROI2DLine || roi instanceof ROI2DPolyLine)
				capillariesArrayList.add((ROI2DShape)roi);
		}
		Collections.sort(capillariesArrayList, new FmpTools.ROI2DNameComparator()); 
	}
	
	public boolean xmlWriteROIsAndData(String name, SequenceVirtual seq) {

		String csFile = FmpTools.saveFileAs(name, seq.getDirectory(), "xml");
		csFile.toLowerCase();
		if (!csFile.contains(".xml")) {
			csFile += ".xml";
		}
		return xmlWriteROIsAndDataNoQuestion(csFile, seq);
	}
	
	public boolean xmlWriteROIsAndDataNoQuestion(String csFile, SequenceVirtual seq) {

		if (csFile != null) 
		{
			extractLinesFromSequence(seq);
			if (capillariesArrayList.size() > 0)
			{
				final Document doc = XMLUtil.createDocument(true);
				if (doc != null)
				{
					List<ROI> roisList = new ArrayList<ROI>();
					for (ROI roi: capillariesArrayList)
						roisList.add(roi);
					ROI.saveROIsToXML(XMLUtil.getRootElement(doc), roisList);
					xmlWriteCapillaryParameters (doc, seq);
					XMLUtil.saveDocument(doc, csFile);
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean xmlWriteROIsAndDataNoFilter(String name, SequenceVirtual seq) {

		String csFile = FmpTools.saveFileAs(name, seq.getDirectory(), "xml");
		csFile.toLowerCase();
		if (!csFile.contains(".xml")) {
			csFile += ".xml";
		}
		
		final Document doc = XMLUtil.createDocument(true);
		if (doc != null)
		{
			List<ROI> roisList = seq.getROIs();
			ROI.saveROIsToXML(XMLUtil.getRootElement(doc), roisList);
			xmlWriteCapillaryParameters (doc, seq);
			XMLUtil.saveDocument(doc, csFile);
			return true;
		}
		
		return false;
	}
	
	public boolean xmlReadROIsAndData(SequenceVirtual seq) {

		String [] filedummy = null;
		String filename = seq.getFileName();
		File file = new File(filename);
		String directory = file.getParentFile().getAbsolutePath();
		filedummy = FmpTools.selectFiles(directory, "xml");
		boolean wasOk = false;
		if (filedummy != null) {
			for (int i= 0; i< filedummy.length; i++) {
				String csFile = filedummy[i];
				wasOk &= xmlReadROIsAndData(csFile, seq);
			}
		}
		return wasOk;
	}
	
	public boolean xmlReadROIsAndData(String csFileName, SequenceVirtual seq) {
		
		if (csFileName != null)  {
			final Document doc = XMLUtil.loadDocument(csFileName);
			if (doc != null) {
				xmlReadCapillaryParameters(doc);
				List<ROI> listOfROIs = ROI.loadROIsFromXML(XMLUtil.getRootElement(doc));
				capillariesArrayList.clear();
				for (ROI roi: listOfROIs)
					capillariesArrayList.add((ROI2DShape) roi);
				Collections.sort(capillariesArrayList, new FmpTools.ROINameComparator()); 
				try  {  
					for (ROI roi : capillariesArrayList)  {
						seq.addROI(roi);
					}
				}
				finally {
				}
				// add to undo manager
				seq.addUndoableEdit(new ROIAddsSequenceEdit(seq, listOfROIs) {
					@Override
					public String getPresentationName() {
						return getROIs().size() + " ROI(s) loaded from XML file"; };
				});
				return true;
			}
		}
		return false;
	}

	public Capillaries copy (Capillaries cap) {
		volume = cap.volume;
		pixels = cap.pixels;
		grouping = cap.grouping;
		analysisStart = cap.analysisStart;
		analysisEnd = cap.analysisEnd;
		analysisStep = cap.analysisStep;
		stimulusR = cap.stimulusR;
		stimulusL = cap.stimulusL;
		concentrationR = cap.concentrationR;
		concentrationL = cap.concentrationL;
		boxID = cap.boxID;
		experiment = cap.experiment;
		comment = cap.comment;
		
		return cap;
	}
	
	public boolean isChanged (Capillaries cap) {
		boolean flag = false; 
		flag = (cap.volume != volume) || flag;
		flag = (cap.pixels != pixels) || flag;
		flag = (cap.analysisStart != analysisStart) || flag;
		flag = (cap.analysisEnd != analysisEnd) || flag;
		flag = (cap.analysisStep != analysisStep) || flag;
		flag = (stimulusR != null && !cap.stimulusR .equals(stimulusR)) || flag;
		flag = (concentrationR != null && !cap.concentrationR .equals(concentrationR)) || flag;
		flag = (stimulusL != null && !cap.stimulusL .equals(stimulusL)) || flag;
		flag = (concentrationL != null && !cap.concentrationL .equals(concentrationL)) || flag;
		flag = (cap.boxID != null && !cap.boxID .equals(boxID)) || flag;
		flag = (cap.experiment != null && !cap.experiment .equals(experiment)) || flag;
		flag = (cap.comment != null && !cap.comment .equals(comment)) || flag;
		return flag;
	}

}
