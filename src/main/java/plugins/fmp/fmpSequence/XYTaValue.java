package plugins.fmp.fmpSequence;

import java.awt.geom.Point2D;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.util.XMLUtil;

public class XYTaValue implements XMLPersistent {
	public Point2D point = new Point2D.Double();
	public int time = 0;
	public boolean alive = false;
	
	public XYTaValue() {
	}
	
	public XYTaValue(Point2D point, int time) {
		this.point = point;
		this.time = time;
	}
	
	public XYTaValue(Point2D point, int time, boolean alive) {
		this.point = point;
		this.time = time;
		this.alive = alive;
	}

	@Override
	public boolean loadFromXML(Node node) {
		if (node == null)
			return false;
		
		Element node_XYTa = XMLUtil.getElement(node, "XYTa");
		
		double x =  XMLUtil.getAttributeDoubleValue( node_XYTa, "x", 0);
		double y =  XMLUtil.getAttributeDoubleValue( node_XYTa, "y", 0);
		point.setLocation(x, y);
		time =  XMLUtil.getAttributeIntValue(node_XYTa, "t", 0);
		alive = XMLUtil.getAttributeBooleanValue(node_XYTa, "a", false);
		
		return false;
	}

	@Override
	public boolean saveToXML(Node node) {
		if (node == null)
			return false;
		
		Element node_XYTa = XMLUtil.addElement(node, "XYTa");
		XMLUtil.setAttributeDoubleValue(node_XYTa, "x", point.getX());
		XMLUtil.setAttributeDoubleValue(node_XYTa, "y", point.getY());
		XMLUtil.setAttributeIntValue(node_XYTa, "t", time);
		XMLUtil.setAttributeBooleanValue(node_XYTa, "a", alive);

		return false;
	}
}
