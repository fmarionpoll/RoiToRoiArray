package plugins.fmp.fmpTools;

import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import icy.file.FileUtil;
import icy.gui.dialog.ConfirmDialog;
import icy.gui.frame.progress.AnnounceFrame;
import icy.plugin.abstract_.Plugin;
import icy.sequence.Sequence;
import icy.roi.ROI;
import icy.roi.ROI2D;
import plugins.kernel.roi.roi2d.ROI2DLine;



public class FmpTools  extends Plugin {

	public static String saveFileAs(String defaultName, String directory, String csExt)
	{		
		// load last preferences for loader
		String csFile = null;
		final JFileChooser fileChooser = new JFileChooser();
		if (directory != null) {
			fileChooser.setCurrentDirectory(new File(directory));
		}
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY );
		FileNameExtensionFilter xlsFilter = new FileNameExtensionFilter(csExt+" files",  csExt, csExt);
		fileChooser.addChoosableFileFilter(xlsFilter);
		fileChooser.setFileFilter(xlsFilter);
		if (defaultName != null)
			fileChooser.setSelectedFile(new File(defaultName));

		final int returnValue = fileChooser.showSaveDialog(null);
		if (returnValue == JFileChooser.APPROVE_OPTION)
		{
			File f = fileChooser.getSelectedFile();
			csFile = f.getAbsolutePath();
			int dotOK = csExt.indexOf(".");
			if (dotOK < 0)
				csExt = "." + csExt;
			int extensionOK = csFile.indexOf(csExt);
			if (extensionOK < 0)
			{
				csFile += csExt;
				f = new File(csFile);
			}

			if(f.exists())
				if (ConfirmDialog.confirm("Overwrite existing file ?"))
					FileUtil.delete(f, true);
				else 
					csFile = null;
		}
		return csFile;
	}

	// TODO use LoaderDialog from Icy
	public static String[] selectFiles(String directory, String csExt)
	{
		// load last preferences for loader
		final JFileChooser fileChooser = new JFileChooser();

		final String path = directory;
		fileChooser.setCurrentDirectory(new File(path));
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY );
		fileChooser.setMultiSelectionEnabled(true);
		FileNameExtensionFilter csFilter = new FileNameExtensionFilter(csExt+" files",  csExt, csExt);
		fileChooser.addChoosableFileFilter(csFilter);
		fileChooser.setFileFilter(csFilter);

		final int returnValue = fileChooser.showDialog(null, "Load");
		String[] liststrings = null;
		if (returnValue == JFileChooser.APPROVE_OPTION)
		{
			File[] files = fileChooser.getSelectedFiles();
			liststrings = new String[files.length];
			
			for (int i=0; i< files.length; i++) {
				liststrings[i] = files[i].getAbsolutePath();
			}
		}
		return liststrings;
	}

	public static class ROI2DLineLeftXComparator implements Comparator<ROI2DLine> {
		@Override
		public int compare(ROI2DLine o1, ROI2DLine o2) {
			if (o1.getBounds().x == o2.getBounds().x)
				return 0;
			else if (o1.getBounds().x > o2.getBounds().x)
				return 1;
			else 
				return -1;
		}
	}
	
	public static class ROI2DLineLeftYComparator implements Comparator<ROI2DLine> {
		@Override
		public int compare(ROI2DLine o1, ROI2DLine o2) {
			if (o1.getBounds().y == o2.getBounds().y)
				return 0;
			else if (o1.getBounds().y > o2.getBounds().y)
				return 1;
			else 
				return -1;
		}
	}
	
	public static class ROI2DNameComparator implements Comparator<ROI2D> {
		@Override
		public int compare(ROI2D o1, ROI2D o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}

	public static class ROINameComparator implements Comparator<ROI> {
		@Override
		public int compare(ROI o1, ROI o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}

	public static class SequenceNameComparator implements Comparator<Sequence> {
		@Override
		public int compare(Sequence o1, Sequence o2) {
			return o1.getName().compareTo(o2.getName());
		}
	}

	public static int[] rank(double[] values) {
		/** Returns a sorted list of indices of the specified double array.
		Modified from: http://stackoverflow.com/questions/951848 by N.Vischer.
		 */
		int n = values.length;
		final Integer[] indexes = new Integer[n];
		final Double[] data = new Double[n];
		for (int i=0; i<n; i++) {
			indexes[i] = new Integer(i);
			data[i] = new Double(values[i]);
		}
		Arrays.sort(indexes, new Comparator<Integer>() {
			public int compare(final Integer o1, final Integer o2) {
				return data[o1].compareTo(data[o2]);
			}
		});
		int[] indexes2 = new int[n];
		for (int i=0; i<n; i++)
			indexes2[i] = indexes[i].intValue();
		return indexes2;
	}

	public static int[] rank(final String[] data) {
		/** Returns a sorted list of indices of the specified String array. */
		int n = data.length;
		final Integer[] indexes = new Integer[n];
		for (int i=0; i<n; i++)
			indexes[i] = new Integer(i);
		Arrays.sort(indexes, new Comparator<Integer>() {
			public int compare(final Integer o1, final Integer o2) {
				return data[o1].compareToIgnoreCase(data[o2]);
			}
		});
		int[] indexes2 = new int[n];
		for (int i=0; i<n; i++)
			indexes2[i] = indexes[i].intValue();
		return indexes2;
	}
	
	public static Polygon orderVerticesofPolygon(Polygon roiPolygon) {
		
		if (roiPolygon.npoints > 4)
			new AnnounceFrame("Only the first 4 points of the polygon will be used...");
		
		Polygon extFrame = new Polygon();
		Rectangle rect = roiPolygon.getBounds();
		Rectangle rect1 = new Rectangle(rect);
	
		// find upper left
		rect1.setSize(rect.width/2, rect.height/2);
		for (int i = 0; i< roiPolygon.npoints; i++) {
			if (rect1.contains(roiPolygon.xpoints[i], roiPolygon.ypoints[i])) {
				extFrame.addPoint(roiPolygon.xpoints[i], roiPolygon.ypoints[i]);
				break;
			}
		}
		// find lower left
		rect1.translate(0, rect.height/2 +2);
		for (int i = 0; i< roiPolygon.npoints; i++) {
			if (rect1.contains(roiPolygon.xpoints[i], roiPolygon.ypoints[i])) {
				extFrame.addPoint(roiPolygon.xpoints[i], roiPolygon.ypoints[i]);
				break;
			}
		}
		// find lower right
		rect1.translate(rect.width/2+2, 0);
		for (int i = 0; i< roiPolygon.npoints; i++) {
			if (rect1.contains(roiPolygon.xpoints[i], roiPolygon.ypoints[i])) {
				extFrame.addPoint(roiPolygon.xpoints[i], roiPolygon.ypoints[i]);
				break;
			}
		}
		// find upper right
		rect1.translate(0, -rect.height/2 - 2);
		for (int i = 0; i< roiPolygon.npoints; i++) {
			if (rect1.contains(roiPolygon.xpoints[i], roiPolygon.ypoints[i])) {
				extFrame.addPoint(roiPolygon.xpoints[i], roiPolygon.ypoints[i]);
				break;
			}
		}
		
		return extFrame;
	}

	public static File chooseDirectory(String rootdirectory) {

		File dummy_selected = null;
				
		JFileChooser fc = new JFileChooser(); 
		if (rootdirectory != null)
			fc.setCurrentDirectory(new File(rootdirectory));
	    fc.setDialogTitle("Select a root directory...");
	    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    fc.setAcceptAllFileFilterUsed(false);
	    if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) { 
	      dummy_selected = fc.getSelectedFile();
	    }
	    else {
	      System.out.println("No directory selected ");
	    }
		return dummy_selected;
	}
	
}

