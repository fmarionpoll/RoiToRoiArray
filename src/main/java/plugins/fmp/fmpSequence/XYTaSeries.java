package plugins.fmp.fmpSequence;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import icy.file.xml.XMLPersistent;
import icy.roi.ROI2D;
import icy.util.XMLUtil;
import plugins.fmp.fmpTools.EnumArrayListType;
import plugins.kernel.roi.roi2d.*;

public class XYTaSeries implements XMLPersistent {
	
	public ROI2DPolygon 	roi;
	public Double 			threshold 			= 50.;
	public int 				lastTimeAlive 		= 0;
	public int 				lastIntervalAlive 	= 0;
	public ArrayList<XYTaValue> pointsList 		= new ArrayList<XYTaValue>();

	
	public XYTaSeries(ROI2D roi) {
		this.roi = (ROI2DPolygon) roi;
	}
	
	public XYTaSeries() {
		this.roi = new ROI2DPolygon();
	}
	
	public void ensureCapacity(int minCapacity) {
		pointsList.ensureCapacity(minCapacity);
	}

	public Point2D getPoint(int i) {
		return pointsList.get(i).point;
	}
	
	public int getTime(int i) {
		return pointsList.get(i).time;
	}

	public String getName() {
		return roi.getName();
	}
	
	public void add(Point2D point, int frame) {
		XYTaValue pos = new XYTaValue(point, frame);
		pointsList.add(pos);
	}

	@Override
	public boolean loadFromXML(Node node) {
		if (node == null)
			return false;
		
		Element node_roi = XMLUtil.getElement(node, "roi");
		roi.loadFromXML(node_roi);
		
		Element node_lastime = XMLUtil.getElement(node, "lastTimeItMoved");
		lastTimeAlive = XMLUtil.getAttributeIntValue(node_lastime, "tlast", -1);
		lastIntervalAlive = XMLUtil.getAttributeIntValue(node_lastime, "ilast", -1);

		Element node_position_list = XMLUtil.getElement(node, "PositionsList");
		if (node_position_list == null) 
			return false;
		
		pointsList.clear();
		int nb_items =  XMLUtil.getAttributeIntValue(node_position_list, "nb_items", 0);
		for (int i=0; i< nb_items; i++) {
			String elementi = "i"+i;
			Element node_position_i = XMLUtil.getElement(node_position_list, elementi);
			XYTaValue pos = new XYTaValue();
			pos.loadFromXML(node_position_i);
			pointsList.add(pos);
		}
		return true;
	}

	@Override
	public boolean saveToXML(Node node) {
		if (node == null)
			return false;
		
		Element node_roi = XMLUtil.addElement(node, "roi");
		roi.saveToXML(node_roi);
		
		Element node_lastime = XMLUtil.addElement(node, "lastTimeAlive");
		XMLUtil.setAttributeIntValue(node_lastime, "tlast", lastTimeAlive);
		XMLUtil.setAttributeIntValue(node_lastime, "ilast", lastIntervalAlive);
		
		Element node_position_list = XMLUtil.addElement(node, "PositionsList");
		XMLUtil.setAttributeIntValue(node_position_list, "nb_items", pointsList.size());
		
		int i = 0;
		for (XYTaValue pos: pointsList) {
			String elementi = "i"+i;
			Element node_position_i = XMLUtil.addElement(node_position_list, elementi);
			pos.saveToXML(node_position_i);
			i++;
		}
		return true;
	}
	
	public ArrayList<Double> getDoubleArrayList (EnumArrayListType option) {
		
		if (pointsList.size() == 0)
			return null;
		ArrayList<Double> datai = null;
		
		switch (option) {
		case distance:
			datai = getDistanceBetweenPoints();
			break;
		case isalive:
			datai = getDistanceBetweenPoints();
			computeIsAlive(datai, threshold);
			datai = getIsAliveAsDoubleArray();
			break;
		case xyPosition:
		default:
			datai = getXYPositions();
			break;
		}
		return datai;
	}
	
	public int computeLastIntervalAlive() {
		computeIsAlive(getDistanceBetweenPoints(), threshold);
		return lastIntervalAlive;
	}
	
	private ArrayList<Double> getDistanceBetweenPoints() {
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(pointsList.size());
		Point2D previous = new Point2D.Double();
		previous = pointsList.get(0).point;
		for (XYTaValue pos: pointsList) {
			double distance = pos.point.distance(previous); 
			dataArray.add(distance);
			previous = pos.point;
		}
		return dataArray;
	}
	
	public ArrayList<Double> getIsAliveAsDoubleArray() {
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(pointsList.size());
		for (XYTaValue pos: pointsList) {
			dataArray.add(pos.alive ? 1.0: 0.0);
		}
		return dataArray;
	}
	
	public ArrayList<Integer> getIsAliveAsIntegerArray() {
		ArrayList<Integer> dataArray = new ArrayList<Integer>();
		dataArray.ensureCapacity(pointsList.size());
		for (XYTaValue pos: pointsList) {
			dataArray.add(pos.alive ? 1: 0);
		}
		return dataArray;
	}
	
	public void computeIsAlive(ArrayList<Double> data, Double threshold) {
		this.threshold = threshold;
		lastIntervalAlive = 0;
		boolean isalive = false;
		for (int i= data.size() - 1; i >= 0; i--) {
			if (data.get(i) > threshold && !isalive) {
				lastIntervalAlive = i;
				lastTimeAlive = pointsList.get(i).time;
				isalive = true;				
			}
			pointsList.get(i).alive = isalive;
		}
	}
	
	private ArrayList<Double> getXYPositions() {
		ArrayList<Double> dataArray = new ArrayList<Double>();
		dataArray.ensureCapacity(pointsList.size()*2);

		for (XYTaValue pos: pointsList) {
			double x = pos.point.getX(); 
			double y = pos.point.getY();
			dataArray.add(x);
			dataArray.add(y);
		}
		return dataArray;
	}

	public int getLastIntervalAlive() {
		if (lastIntervalAlive >= 0)
			return lastIntervalAlive;
		return computeLastIntervalAlive();
	}
}
