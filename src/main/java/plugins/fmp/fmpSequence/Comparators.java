package plugins.fmp.fmpSequence;

import java.util.Comparator;

import icy.roi.ROI;
import icy.roi.ROI2D;
import icy.sequence.Sequence;



public class Comparators 
{
	public static class ROI_Name_Comparator implements Comparator<ROI> {
		@Override
		public int compare(ROI o1, ROI o2) 
		{
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	public static class ROI2D_Name_Comparator implements Comparator<ROI2D> 
	{
		@Override
		public int compare(ROI2D o1, ROI2D o2) 
		{
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	public static class ROI2D_T_Comparator implements Comparator<ROI2D> 
	{
		@Override
		public int compare(ROI2D o1, ROI2D o2) 
		{
			return o1.getT()-o2.getT();
		}
	}

	public static class Sequence_Name_Comparator implements Comparator<Sequence> 
	{
		@Override
		public int compare(Sequence o1, Sequence o2) 
		{
			return o1.getName().compareTo(o2.getName());
		}
	}
	
	
	
}

