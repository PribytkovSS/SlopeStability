
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SubgradeLoad {
	private Line2D.Double loadedLine = null;
	private double lineLoad = 0;
	
	public SubgradeLoad(Line2D.Double line, double load)
	{
		loadedLine = line;
		lineLoad = load;
	}
	
	public SubgradeLoad(Node xml_node)
	{
		double fromX = SubgradeSoil.myParseDouble(xml_node.getAttributes().getNamedItem("FromX").getNodeValue());
		double fromY = SubgradeSoil.myParseDouble(xml_node.getAttributes().getNamedItem("FromY").getNodeValue());
		double toX = SubgradeSoil.myParseDouble(xml_node.getAttributes().getNamedItem("ToX").getNodeValue());
		double toY = SubgradeSoil.myParseDouble(xml_node.getAttributes().getNamedItem("ToY").getNodeValue());
		lineLoad = SubgradeSoil.myParseDouble(xml_node.getAttributes().getNamedItem("LoadValue").getNodeValue());
		loadedLine = new Line2D.Double(fromX, fromY, toX, toY);
	}
	
	public Line2D.Double GetLine()
	{
		return loadedLine;
	}
	
	public double GetLoad()
	{
		return lineLoad;
	}
	
	public void SetLoad(double load)
	{
		lineLoad = Math.abs(load);
	}
	
	public void Relocate(Point2D.Double pt_from, Point2D.Double pt_to)
	{
		if (loadedLine != null)
		{
			Point2D.Double pt1 = (Point2D.Double)loadedLine.getP1();
			Point2D.Double pt2 = (Point2D.Double)loadedLine.getP2();
			if (SubgradeSoil.TwinPoints(pt1, pt_from)) loadedLine.setLine(pt_to, pt2);
			else
				if (SubgradeSoil.TwinPoints(pt2, pt_from)) loadedLine.setLine(pt1, pt_to);
		}
	}
	
	public SubgradeLoad Split(Point2D.Double split_point)
	{
		if (SubgradeSoil.PointOnLine(loadedLine, split_point))
		{
			Point2D.Double pt2 = (Point2D.Double)loadedLine.getP2();
			Relocate(pt2, split_point);
			return new SubgradeLoad(new Line2D.Double(split_point, pt2), lineLoad);
		}
		return null;
	}
	
	public boolean ItsMe(Line2D.Double line)
	{
		// ¬озвращаем true, если Line €вл€етс€ отрезком в loadedLine. 
		// Ёто так, если обе точки line принадлежат loadedLine
		if (loadedLine != null)
		{
			return SubgradeSoil.PointOnLine(loadedLine, (Point2D.Double)line.getP1()) &&
				   SubgradeSoil.PointOnLine(loadedLine, (Point2D.Double)line.getP2());	
		} else return false;
	}
	
	public void Paint(Graphics2D g2d, double size_x, double size_y)
	{
		// Ќагрузка €вл€етс€ равномерно распределЄнной
		if (loadedLine != null)
		{
			if (loadedLine.x2 == loadedLine.x1) return;
			if (lineLoad == 0) return;
			
			g2d.setColor(Color.orange);
			double h = 0.05 * size_y;
			double length = Math.abs(loadedLine.x1 - loadedLine.x2);
			double dx = 0.01 * size_x * Math.signum(loadedLine.x2 - loadedLine.x1);
			
			int steps_count = (int)Math.round(length / dx);
			if (steps_count == 0) steps_count = 1;
			
			dx = length / steps_count;
			double tan = (loadedLine.y2 - loadedLine.y1) / (loadedLine.x2 - loadedLine.x1);
			double dy = dx * tan;
			double arrow = 0.005 * size_x;
		
			Point2D.Double pt = (Point2D.Double)loadedLine.getP1();
			Point2D.Double pt1 = (Point2D.Double)pt.clone();
			pt1.y = pt.y + h;
			Point2D.Double a_pt1 = (Point2D.Double)pt.clone(); 
			Point2D.Double a_pt2 = (Point2D.Double)pt.clone();
			a_pt1.x = a_pt1.x - arrow * 0.707;
			a_pt2.x = a_pt2.x + arrow * 0.707;
			a_pt1.y = a_pt1.y + arrow * 0.707;
			a_pt2.y = a_pt1.y;
			
			// ƒвига€сь с шагом dx в направлении второй точки, рисуем стрелки
			for (int i = 0; i <= steps_count; i++)
			{
				g2d.draw(new Line2D.Double(pt, pt1));
				g2d.draw(new Line2D.Double(pt, a_pt1));
				g2d.draw(new Line2D.Double(pt, a_pt2));
				pt.x = pt.x + dx;
				pt1.x = pt1.x + dx;
				a_pt1.x = a_pt1.x + dx;
				a_pt2.x = a_pt2.x + dx;
				pt.y = pt.y + dy;
				pt1.y = pt1.y + dy;
				a_pt1.y = a_pt1.y + dy;
				a_pt2.y = a_pt2.y + dy;
			}
		}
	}

	public void toXML(Element e) {
		e.setAttribute("FromX", Double.toString(loadedLine.getX1()));
		e.setAttribute("FromY", Double.toString(loadedLine.getY1()));
		e.setAttribute("ToX", Double.toString(loadedLine.getX2()));
		e.setAttribute("ToY", Double.toString(loadedLine.getY2()));
		e.setAttribute("LoadValue", Double.toString(lineLoad));
	}
}
