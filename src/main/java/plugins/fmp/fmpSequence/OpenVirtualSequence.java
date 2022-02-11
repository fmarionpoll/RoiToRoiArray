package plugins.fmp.fmpSequence;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import icy.file.Loader;
import icy.file.SequenceFileImporter;
import icy.gui.dialog.LoaderDialog;
import icy.gui.dialog.MessageDialog;
import ome.xml.meta.OMEXMLMetadata;
import plugins.fmp.fmpTools.StringSorter;
import plugins.stef.importer.xuggler.VideoImporter;

public class OpenVirtualSequence {
	
	public static EnumStatus statusSequenceVirtual = EnumStatus.REGULAR;
	protected static VideoImporter importer 		= null;
	private final static String[] acceptedTypes = {".jpg", ".jpeg", ".bmp", "tiff"};
	private String directory 		= null;
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
	
	static public SequenceVirtual loadInputVirtualStack(String path) 
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

		SequenceVirtual sequenceVirtual = null;
		String [] list;
		if (selectedFiles.length == 1) 
		{
			list = (new File(directory)).list();
			if (list ==null)
				return null;
			
			if (!(selectedFiles[0].isDirectory()) && selectedFiles[0].getName().toLowerCase().contains(".avi")) 
			{
				sequenceVirtual = loadSequenceVirtualAVI(selectedFiles[0].getAbsolutePath());
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
			sequenceVirtual = loadV2SequenceFromImagesList(imagesList);
		}
		
		return sequenceVirtual;
	}
	
	private static SequenceVirtual loadSequenceVirtualAVI(String fileName) 
	{
		SequenceVirtual sequenceVirtual = null;
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
			statusSequenceVirtual = EnumStatus.AVIFILE;
			importer.open( fileName, 0 );
			OMEXMLMetadata metaData = importer.getOMEXMLMetaData();
			//nTotalFrames = MetaDataUtil.getSizeT( metaData, 0 ) - 2 ; 
			// get one frame less as there is a little bug in the decompression of the video in h264
		}
		catch (Exception exc)
		{
			MessageDialog.showDialog( "File type or video-codec not supported.", MessageDialog.ERROR_MESSAGE );
			statusSequenceVirtual = EnumStatus.FAILURE;
		}
		return sequenceVirtual;
	}
	
	private static String[] getAcceptedNamesFromImagesList(String[] list, String directory) 
	{
		statusSequenceVirtual = EnumStatus.FAILURE;
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
		statusSequenceVirtual = EnumStatus.FILESTACK;
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
	
	private void loadSequenceVirtualStackFromName(String name) 
	{
		File filename = new File (name);
		if (filename.isDirectory())
	    	directory = filename.getAbsolutePath();
	    else {
	    	directory = filename.getParentFile().getAbsolutePath();
	    }
		if (directory == null) {
			statusSequenceVirtual = EnumStatus.FAILURE;
			return;
		}
		String [] list;
		File fdir = new File(directory);
		boolean flag = fdir.isDirectory();
		if (!flag)
			return;
		list = fdir.list();
		// TODO: change directory into a pathname
		if (list != null)
			getAcceptedNamesFromImagesList(list, directory);
	}
	
	public void loadSequenceVirtualFromName(String name)
	{
		if (name.toLowerCase().contains(".avi"))
			loadSequenceVirtualAVI(name);
		else
			loadSequenceVirtualStackFromName(name);
	}
	
	public static SequenceVirtual loadV2SequenceFromImagesList(List <String> imagesList) 
	{
		SequenceFileImporter seqFileImporter = Loader.getSequenceFileImporter(imagesList.get(0), true);
		SequenceVirtual seq = (SequenceVirtual) Loader.loadSequence(seqFileImporter, imagesList, false);
		return seq;
	}
	
	public SequenceVirtual loadVirtualStackAt(String textPath) 
	{
		if (textPath == null) 
			return loadInputVirtualStack(null); 
		
		File filepath = new File(textPath); 
	    if (filepath.isDirectory())
	    	directory = filepath.getAbsolutePath();
	    else
	    	directory = filepath.getParentFile().getAbsolutePath();
		if (directory == null )
			return null;

		String [] list;
		list = (new File(directory)).list();
		if (list ==null)
			return null;
		
		SequenceVirtual sequenceVirtual = null;
		if (!(filepath.isDirectory()) && filepath.getName().toLowerCase().contains(".avi")) 
		{
			sequenceVirtual = loadSequenceVirtualAVI(filepath.getAbsolutePath());
		}
		else 
		{
			String[] imagesArray = getAcceptedNamesFromImagesList(list, directory);
			List<String> imagesList = Arrays.asList(imagesArray);
			sequenceVirtual = loadV2SequenceFromImagesList(imagesList);
		}
		return sequenceVirtual;
	}
	
	public String loadInputVirtualFromNameSavedInRoiXML(String csFileName)
	{
		if (csFileName != null)
			loadSequenceVirtualFromName(csFileName);
		return csFileName;
	}
}
