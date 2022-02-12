package plugins.fmp.fmpSequence;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;


import icy.file.Loader;
import icy.file.SequenceFileImporter;
import icy.gui.dialog.LoaderDialog;
import icy.gui.dialog.MessageDialog;
import icy.gui.viewer.Viewer;
import icy.main.Icy;
import icy.sequence.Sequence;

import plugins.fmp.fmpTools.StringSorter;
import plugins.stef.importer.xuggler.VideoImporter;



public class OpenVirtualSequence {
	
	public static EnumStatus statusSequence = EnumStatus.REGULAR;
	protected static VideoImporter importer = null;
	private static final String[] acceptedTypes = {".jpg", ".jpeg", ".bmp", "tiff"};
	private String directory  = null;
	/*
	public SequenceVirtual (String csFile) 
	{
		loadSequenceVirtualAVIFromName(csFile);
	}

	public SequenceVirtual (String [] list, String directory) 
	{
		getAcceptedNamesFromImagesList(list, directory);
		filename = directory + ".xml";
	}
	*/
	public static SequenceVirtual initVirtualSequence(Sequence seq) 
	{
		if (seq == null)
			return null;
		
		Viewer v = seq.getFirstViewer();
		if (v != null) 
			v.close();
		Icy.getMainInterface().addSequence(seq);
		
		return new SequenceVirtual(seq);
	}
	
	public static Sequence openImagesOrAvi(String path) 
	{
		LoaderDialog dialog = new LoaderDialog(false);
		if (path != null) 
			dialog.setCurrentDirectory(new File(path));
	    File[] selectedFiles = dialog.getSelectedFiles();
	    if (selectedFiles.length == 0)
	    	return null;
	    
	    String directory;
	    if (selectedFiles[0].isDirectory())
	    	directory = selectedFiles[0].getAbsolutePath();
	    else
	    	directory = selectedFiles[0].getParentFile().getAbsolutePath();
		if (directory == null )
			return null;

		Sequence seq = null;
		String [] list;
		if (selectedFiles.length == 1) 
		{
			list = (new File(directory)).list();
			if (list ==null)
				return null;
			
			if (!selectedFiles[0].isDirectory())  
			{
				if (selectedFiles[0].getName().toLowerCase().contains(".avi"))
					seq = loadSequenceAVI(selectedFiles[0].getAbsolutePath());
				else
				{
					String[] imagesArray = getAcceptedNamesFromImagesList(list, directory);
					List <String> imagesList = Arrays.asList(imagesArray);
					seq = loadV2SequenceFromImagesList(imagesList);
				}
			}
			
		}
		else
		{
			list = new String[selectedFiles.length];
			  for (int i = 0; i < selectedFiles.length; i++) {
				if (selectedFiles[i].getName().toLowerCase().contains(".avi"))
					continue;
			    list[i] = selectedFiles[i].getAbsolutePath();
			}
			String[] imagesArray = getAcceptedNamesFromImagesList(list, directory);
			List <String> imagesList = Arrays.asList(imagesArray);
			seq = loadV2SequenceFromImagesList(imagesList);
		}
		
		return seq;
	}
	
	private static Sequence loadSequenceAVI(String fileName) 
	{
		Sequence sequenceVirtual = null;
		if (importer != null )
		{
			try 
			{
				importer.close();
			} catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		
		try
		{
			importer = new VideoImporter();
			statusSequence = EnumStatus.AVIFILE;
			importer.open( fileName, 0 );
//			OMEXMLMetadata metaData = importer.getOMEXMLMetaData();
//			nTotalFrames = MetaDataUtil.getSizeT( metaData, 0 ) - 2 ; 
			// get one frame less as there is a little bug in the decompression of the video in h264
		}
		catch (Exception exc)
		{
			MessageDialog.showDialog( "File type or video-codec not supported.", MessageDialog.ERROR_MESSAGE );
			statusSequence = EnumStatus.FAILURE;
		}
		return sequenceVirtual;
	}
	
	private static String[] getAcceptedNamesFromImagesList(String[] list, String directory) 
	{
		statusSequence = EnumStatus.FAILURE;
		String[] imagesList = keepOnlyAcceptedNames(list);
		if (list==null) 
			return null;

		int j = 0;
		for (int i=0; i<list.length; i++) 
		{
			if (list[i]!= null)
				imagesList [j++] = directory + '/'+ list[i];
		}
		imagesList = StringSorter.sortNumerically(imagesList);
		statusSequence = EnumStatus.FILESTACK;
		return imagesList;
	}
	
	public static String[] keepOnlyAcceptedNames(String[] rawlist) 
	{
		// -----------------------------------------------
		// subroutines borrowed from FolderOpener
		/* Keep only "accepted" names (file extension)*/
		int count = 0;
		for (int i=0; i< rawlist.length; i++) {
			String name = rawlist[i];
			if ( !acceptedFileType(name) )
				rawlist[i] = null;
			else
				count++;
		}
		if (count==0) return null;

		String[] list = rawlist;
		if (count<rawlist.length) {
			list = new String[count];
			int index = 0;
			for (int i=0; i< rawlist.length; i++) {
				if (rawlist[i]!=null)
					list[index++] = rawlist[i];
			}
		}
		return list;
	}

	public static boolean acceptedFileType(String name) 
	{
		if (name==null) 
			return false;
		for (int i=0; i<acceptedTypes.length; i++) 
		{
			if (name.endsWith(acceptedTypes[i]))
				return true;
		}
		return false;
	}	
	
	private Sequence loadSequenceStackFromName(String name) 
	{
		File filename = new File (name);
		Sequence seq = null;
		if (filename.isDirectory())
	    	directory = filename.getAbsolutePath();
	    else {
	    	directory = filename.getParentFile().getAbsolutePath();
	    }
		if (directory == null) {
			statusSequence = EnumStatus.FAILURE;
			return seq;
		}
		String [] imagesArray;
		File fdir = new File(directory);
		boolean flag = fdir.isDirectory();
		if (!flag)
			return seq;
		imagesArray = fdir.list();
		// TODO: change directory into a pathname
		if (imagesArray != null) {
			getAcceptedNamesFromImagesList(imagesArray, directory);
			List <String> imagesList = Arrays.asList(imagesArray);
			seq = loadV2SequenceFromImagesList(imagesList);
		}
		return seq;
	}
	
	public Sequence loadSequenceVirtualFromName(String name)
	{
		Sequence seq = null;
		if (name.toLowerCase().contains(".avi"))
			seq = loadSequenceAVI(name);
		else
			seq = loadSequenceStackFromName(name);
		return seq;
	}
	
	public static Sequence loadV2SequenceFromImagesList(List <String> imagesList) 
	{
		SequenceFileImporter seqFileImporter = Loader.getSequenceFileImporter(imagesList.get(0), true);
		Sequence seq = Loader.loadSequence(seqFileImporter, imagesList, false);
		return seq;
	}
	
	
	public String loadInputVirtualFromNameSavedInRoiXML(String csFileName)
	{
		if (csFileName != null)
			loadSequenceVirtualFromName(csFileName);
		return csFileName;
	}
}
