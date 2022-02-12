package plugins.fmp.roiToArray;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import icy.gui.viewer.Viewer;
import icy.gui.viewer.ViewerEvent;
import icy.gui.viewer.ViewerListener;
import icy.main.Icy;
import icy.plugin.PluginLauncher;
import icy.plugin.PluginLoader;
import icy.gui.viewer.ViewerEvent.ViewerEventType;
import icy.preferences.XMLPreferences;
import icy.roi.ROI;
import icy.sequence.DimensionId;
import icy.sequence.Sequence;

import plugins.adufour.ezplug.EzButton;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzGroup.FoldListener;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVar;
import plugins.adufour.ezplug.EzVarEnum;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarListener;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.ezplug.EzVarText;

import plugins.fmp.fmpSequence.OpenVirtualSequence;
import plugins.fmp.fmpSequence.SequenceVirtual;
import plugins.fmp.fmpTools.EnumImageOp;



public class RoiToRoiArray extends EzPlug implements ViewerListener, FoldListener
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
	
	EzGroup groupDetectFromSTD;
	EzGroup groupDefineManually;
	EzGroup groupDetectDisks;
	
	private SequenceVirtual sequenceVirtual = null;
	private boolean [] foldingStatus = new boolean[3];
	
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
		
		adjustAndCenterEllipsesButton = new EzButton("Find leaf disks within ROIs", new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				DetectLeafDisks.findLeafDiskIntoRectangles(sequenceVirtual); 
				}});
		ezFindLinesButton = new EzButton("Build histograms",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				sequenceVirtual = OpenVirtualSequence.initVirtualSequence(ezSequence.getValue());
				Sequence seq = ezSequence.getValue();
				DetectLinesSTD.findLines(seq, sequenceVirtual.currentFrame); 
				}});
		ezOpenFileButton = new EzButton("Open file or sequence",  new ActionListener() { 
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
				int t = sequenceVirtual.currentFrame;
				String choice = thresholdSTDFromChanComboBox.getValue();
				int threshold = thresholdSTD.getValue();
				DetectLinesSTD.buildAutoGrid(seq, t, choice, threshold); 
				}});
		convertLinesToSquaresButton = new EzButton("Convert lines to squares",  new ActionListener() { 
			public void actionPerformed(ActionEvent e) { 
				int areaShrinkPCT = areaShrink.getValue();
				String rootname = ezRootnameComboBox.getValue();
				DefineLinesManually.convertLinesToSquares(sequenceVirtual.seq, rootname, areaShrinkPCT); 
				}});
		changeGridNameButton = new EzButton("Set names of ROIs", new ActionListener () {
			public void actionPerformed(ActionEvent e) { 
				changeGridName(); 
				}});
//		overlayCheckBox = new EzVarBoolean("build from overlay", false);
//		overlayCheckBox.addVarChangeListener(new EzVarListener<Boolean>() {
//             @Override
//             public void variableChanged(EzVar<Boolean> source, Boolean newValue) {
//            	 DetectLeafDisks.displayOverlay(sequenceVirtual, newValue, thresholdOv);
//             }});

		filterComboBox = new EzVarEnum <EnumImageOp>("Filter as ", EnumImageOp.values(), 7);
		filterComboBox.addVarChangeListener(new EzVarListener<EnumImageOp>() {
			@Override
			public void variableChanged(EzVar<EnumImageOp> source, EnumImageOp newOp) {
				EnumImageOp transformop = filterComboBox.getValue();
				DetectLeafDisks.updateOverlay(sequenceVirtual, transformop);
				}});
		
		thresholdOv = new EzVarInteger("threshold ", 70, 1, 255, 1);
		thresholdSTD = new EzVarInteger("threshold / selected filter", 500, 1, 10000, 1);
		thresholdOv.addVarChangeListener(new EzVarListener<Integer>() {
            @Override
            public void variableChanged(EzVar<Integer> source, Integer newValue) { 
            	DetectLeafDisks.updateThreshold(sequenceVirtual, newValue); 
            	}});

		// 2) add variables to the interface

		EzGroup groupSequence = new EzGroup("Source data", ezSequence, ezOpenFileButton, openXMLButton);
		super.addEzComponent (groupSequence);

		groupDetectFromSTD = new EzGroup("Detect lines using STD", ezFindLinesButton, /*exportSTDButton,*/ 
				thresholdSTD, thresholdSTDFromChanComboBox, generateAutoGridButton, 
				areaShrink, convertLinesToSquaresButton);
		super.addEzComponent (groupDetectFromSTD);
		foldingStatus[0] = false;
		groupDetectFromSTD.setFoldedState(foldingStatus[0]);
		groupDetectFromSTD.addFoldListener(this);
	
		groupDefineManually = new EzGroup("Define lines manually", splitAsComboBox, 
				ezNumberOfColumns, columnSize, columnSpan, 
				ezNumberOfRows, rowWidth, rowInterval, generateGridButton);
		super.addEzComponent (groupDefineManually);
		foldingStatus[1] = true;
		groupDefineManually.setFoldedState(foldingStatus[1]);
		groupDefineManually.addFoldListener(this);

		groupDetectDisks = new EzGroup("Detect leaf disks", 
				filterComboBox, thresholdOv, adjustAndCenterEllipsesButton);
		super.addEzComponent (groupDetectDisks);
		foldingStatus[2] = true;
		groupDetectDisks.setFoldedState(foldingStatus[2]);
		groupDetectDisks.addFoldListener(this);
		
		EzGroup outputParameters = new EzGroup("Output data",  ezRootnameComboBox, changeGridNameButton, saveXMLButton);
		super.addEzComponent (outputParameters);
	}
	
// -----------------------------------	
	
	private void openFile() 
	{
		if (sequenceVirtual != null) 
			sequenceVirtual.close();
		
		Sequence seq = OpenVirtualSequence.openImagesOrAvi(null);
		sequenceVirtual = OpenVirtualSequence.initVirtualSequence(seq);
		String path = sequenceVirtual.getDirectory();
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
			DefineLinesManually.createROISFromSelectedPolygon(seq, 0, rootName, colSpan, colSize, nbcols, rowSpan, rowSize, nbrows);
		}
		else if (choice == "polygons") 
		{
			DefineLinesManually.createROISFromSelectedPolygon(seq, 1, rootName, colSpan, colSize, nbcols, rowSpan, rowSize, nbrows);
		}
		else if (choice == "circles") 
		{
			DefineLinesManually.createROISFromSelectedPolygon(seq, 2, rootName, colSpan, colSize, nbcols, rowSpan, rowSize, nbrows);
		}		
	}
	
	@Override
	public void viewerChanged(ViewerEvent event) 
	{
		if ((event.getType() == ViewerEventType.POSITION_CHANGED) && (event.getDim() == DimensionId.T))        
			sequenceVirtual.currentFrame = event.getSource().getPositionT() ; 
	}
	
	@Override
	public void viewerClosed(Viewer viewer) 
	{
		viewer.removeListener(this);
		sequenceVirtual = null;
	}
	
	private void openXMLFile() 
	{
		sequenceVirtual.seq.removeAllROI();
		sequenceVirtual.capillariesRoi2RoiArray.xmlReadROIsAndData(sequenceVirtual);
//		vSequence.cages.xmlReadCagesFromFile(vSequence);
	}
	
	private void saveXMLFile() 
	{
		sequenceVirtual.capillariesRoi2RoiArray.grouping = 1;
		sequenceVirtual.capillariesRoi2RoiArray.xmlWriteROIsAndDataNoFilter("roisarray.xml", sequenceVirtual);
	}
	
	private void changeGridName() 
	{
		List<ROI> roisList = sequenceVirtual.seq.getROIs(true);
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

	@Override
	public void foldStateChanged(boolean state) {
		
		boolean [] status = new boolean [3];
		status[0] = groupDetectFromSTD.getFoldedState();
		status[1] = groupDefineManually.getFoldedState();
		status[2] = groupDetectDisks.getFoldedState();
		
		makeSureOnlyOneGroupIsUnfolded(status, foldingStatus);
		foldGroups (status);
		
		int threshold = thresholdOv.getValue();
		DetectLeafDisks.displayOverlay(sequenceVirtual, !foldingStatus[2], threshold);
	}
	
	private void foldGroups(boolean [] status) {
		foldingStatus[0] = status[0];
		foldingStatus[1] = status[1];
		foldingStatus[2] = status[2];
		
		groupDetectFromSTD.setFoldedState(status[0]);
		groupDefineManually.setFoldedState(status[1]);
		groupDetectDisks.setFoldedState(status[2]);
	}
	
	private void makeSureOnlyOneGroupIsUnfolded(boolean [] statusNew, boolean [] statusOld) {
		int size = 3;
		for (int i= 0; i < size; i++) {
			if (!statusNew[i] && statusNew[i] != statusOld[i]) {
				for (int j = 0; j < size; j++ ) {
					if (j != i) statusNew[j] = true;
				}
			}
		}
	}
	
	
}

