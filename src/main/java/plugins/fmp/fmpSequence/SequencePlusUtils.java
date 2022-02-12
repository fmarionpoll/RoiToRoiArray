package plugins.fmp.fmpSequence;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import icy.common.exception.UnsupportedFormatException;
import icy.file.Loader;
import icy.gui.frame.progress.ProgressFrame;
import icy.image.IcyBufferedImage;
import plugins.kernel.roi.roi2d.ROI2DShape;



public class SequencePlusUtils {
	public static boolean isInterrupted = false;
	public static boolean isRunning = false;
	
	public static ArrayList<SequencePlus> openFiles (String directory) {
		
		isRunning = true;
		ArrayList<SequencePlus> arrayKymos = new ArrayList<SequencePlus> ();	
		String[] list = (new File(directory)).list();
		if (list == null)
			return arrayKymos;
		
		Arrays.sort(list, String.CASE_INSENSITIVE_ORDER);
		ProgressFrame progress = new ProgressFrame("Load kymographs");
		progress.setLength(list.length);
		
		for (String filename: list) {
			if (!filename.contains(".tiff"))
				continue;
			if (isInterrupted) {
				isInterrupted = false;
				isRunning = false;
				progress.close();
				return null;
			}
			 
			SequencePlus kymographSeq = new SequencePlus();
			final String name =  directory + File.separator+ filename;
			progress.setMessage( "Load "+filename);
			
			IcyBufferedImage ibufImage = null;
			try {
				ibufImage = Loader.loadImage(name);

			} catch (UnsupportedFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			kymographSeq.seq.addImage(0, ibufImage);
			
			int index1 = filename.indexOf(".tiff");
			int index0 = filename.lastIndexOf(File.separator)+1;
			String title = filename.substring(index0, index1);
			kymographSeq.seq.setName(title);
			kymographSeq.loadXMLKymographAnalysis(directory);
			arrayKymos.add(kymographSeq);
			
			progress.incPosition();
		}
		progress.close();
		isRunning = false;
		return arrayKymos;
	}
	
	public static ArrayList<SequencePlus> openFiles (String directory, Capillaries cap) {
		
		isRunning = true;
		ArrayList<SequencePlus> arrayKymos = new ArrayList<SequencePlus> ();	

		ProgressFrame progress = new ProgressFrame("Load kymographs");
		progress.setLength(cap.capillariesArrayList.size());
		
		for (ROI2DShape roi: cap.capillariesArrayList) {
			
			if (isInterrupted) {
				isInterrupted = false;
				isRunning = false;
				progress.close();
				return null;
			}
			 
			SequencePlus kymographSeq = new SequencePlus();
			final String name =  directory + File.separator+ roi.getName() + ".tiff";
			progress.setMessage( "Load "+name);
			
			IcyBufferedImage ibufImage = null;
			try {
				ibufImage = Loader.loadImage(name);

			} catch (UnsupportedFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			kymographSeq.seq.addImage(0, ibufImage);
			
			String title = roi.getName();
			kymographSeq.seq.setName(title);
			kymographSeq.loadXMLKymographAnalysis(directory);
			arrayKymos.add(kymographSeq);
			
			progress.incPosition();
		}
		progress.close();
		isRunning = false;
		return arrayKymos;
	}
	
	public static void saveKymosMeasures (ArrayList<SequencePlus> kymographArrayList, String directory) {
		
		isRunning = true;
		ProgressFrame progress = new ProgressFrame("Save kymograph measures");
		progress.setLength(kymographArrayList.size());
		
		for (SequencePlus seq : kymographArrayList) {
			if (isInterrupted) {
				isInterrupted = false;
				isRunning = false;
				progress.close();
			}

			if (!seq.saveXMLKymographAnalysis(directory))
				System.out.println(" -> failed - in directory: " + directory);
			
			progress.incPosition();
		}
		progress.close();
		isRunning = false;
	}

	public static void transferSequenceInfoToKymos (ArrayList<SequencePlus> kymographArrayList, SequenceVirtual vSequence) {
				
		for (int kymo=0; kymo < kymographArrayList.size(); kymo++) {
			SequencePlus seq = kymographArrayList.get(kymo);
			seq.analysisStart = vSequence.analysisStart; 
			seq.analysisEnd  = vSequence.analysisEnd;
			seq.analysisStep = vSequence.analysisStep;
		}
	}
}
