package plugins.fmp.fmpTools;


import java.awt.Color;
import java.util.ArrayList;




public class ImageOperationsStruct 
{
	
	int 				fromFrame		= -1;
	int 				colordistanceType 	= 0;
	int					simplethreshold = 255;
	int 				colorthreshold	= 0;
	ArrayList <Color> 	colorarray 		= null;
	EnumImageOp 	transformop		= EnumImageOp.NONE;
	EnumThresholdType 		thresholdtype 	= EnumThresholdType.NONE;
		
	// -----------------------------------
	
	public ImageOperationsStruct () {
		this.fromFrame = -1;
		this.transformop = EnumImageOp.NONE;
		this.thresholdtype = EnumThresholdType.NONE;
		this.colorthreshold = 0;
	}
	
	public ImageOperationsStruct (int framenumber, EnumImageOp transformop, EnumThresholdType thresholdtype, int thresholdvalue) {
		this.fromFrame = framenumber;
		this.transformop = transformop;
		this.thresholdtype = thresholdtype;
		this.colorthreshold = thresholdvalue;
	}
	
	public ImageOperationsStruct (int framenumber, EnumImageOp transformop) {
		this.fromFrame = framenumber;
		this.transformop = transformop;
		this.thresholdtype = EnumThresholdType.NONE;
		this.colorthreshold = 0;
	}
	
	public boolean isValidTransformCache(ImageOperationsStruct op) {
		if (op.fromFrame != this.fromFrame)
			return false;
		
		if (op.transformop != this.transformop)
			return false;
		return true;
	}
	
	public void copyTransformOpTo (ImageOperationsStruct op) {
		op.transformop = transformop;
		op.fromFrame = fromFrame;
	}
	
	public void copyThresholdOpTo (ImageOperationsStruct op) {
		
		op.thresholdtype = thresholdtype;
		if (thresholdtype == EnumThresholdType.SINGLE) {
			op.simplethreshold = simplethreshold;
		}
		else if (thresholdtype == EnumThresholdType.COLORARRAY) {
			op.colorthreshold = colorthreshold;
			if (op.colorarray == null)
				op.colorarray = new ArrayList <Color> ();
			else
				op.colorarray.clear();
			for (Color c: colorarray)
				op.colorarray.add(c);
			op.colordistanceType = colordistanceType;
		}
		op.fromFrame = fromFrame;
	}
	
	public boolean isValidThresholdCache(ImageOperationsStruct op) {
		if (op.fromFrame != this.fromFrame)
			return false;
		
		if (op.thresholdtype != this.thresholdtype)
			return false;
		
		if (op.thresholdtype == EnumThresholdType.COLORARRAY) {
			if (op.colorthreshold != this.colorthreshold)
				return false;
			if (op.colordistanceType != this.colordistanceType)
				return false;
			if (op.colorarray.size() != this.colorarray.size())
				return false;
		}
		else {
			if (op.simplethreshold != this.simplethreshold)
				return false;
		}
		return true;
	}
}
