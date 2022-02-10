package plugins.fmp.fmpTools;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import icy.canvas.IcyCanvas;
import icy.canvas.IcyCanvas2D;
import icy.image.IcyBufferedImage;
import icy.image.IcyBufferedImageUtil;
import icy.painter.Overlay;
import icy.sequence.Sequence;
import plugins.fmp.fmpSequence.SequenceVirtual;


public class OverlayThreshold extends Overlay
{
	private ImageOperations 	imgOp 	= null;
	private float 				opacity = 0.3f;
	private OverlayColorMask	map 	= new OverlayColorMask ("", new Color(0x00FF0000, true));
	
	// ---------------------------------------------
	
	public OverlayThreshold()
	{
		super("ThresholdOverlay");	
	}
	
	public OverlayThreshold(SequenceVirtual seq) {
		super("ThresholdOverlay");
		setSequence(seq);
	}
	
	public void setSequence (SequenceVirtual seq)
	{
		if (seq == null)
			return;
		if (imgOp == null)
			imgOp = new ImageOperations (seq);
		imgOp.setSequence(seq);
	}
	
	public void setTransform (EnumImageOp transf) {
		imgOp.setTransform( transf);
	}
	
	public void setThresholdSingle (int threshold)
	{
		imgOp.setThresholdSingle(threshold);
	}
	
	public void setThresholdColor (ArrayList <Color> colorarray, int distancetype, int threshold)
	{
		imgOp.setColorArrayThreshold(colorarray, distancetype, threshold);
	}
	
	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas)
	{
		// check if we are dealing with a 2D canvas and if we have a valid Graphics object
		if ((canvas instanceof IcyCanvas2D) && (g != null))
		{
			IcyBufferedImage thresholdedImage = imgOp.run();
			if (thresholdedImage != null) {
				thresholdedImage.setColorMap(0, map);
				BufferedImage bufferedImage = IcyBufferedImageUtil.getARGBImage(thresholdedImage);
				Composite bck = g.getComposite();
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
				g.drawImage(bufferedImage, 0, 0, null);
				g.setComposite(bck);

			}
		}
	}

}

