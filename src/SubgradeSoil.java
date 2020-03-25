
import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.TexturePaint;

// Этот класс - слой грунта
// Описывается координатами точек, являющихся вершинами многоугольника
// Также содержит характеристики грунта
public class SubgradeSoil {
	protected static final double PARSE_ERROR = -999.0;
	
	private Path2D.Double SoilLayer = new Path2D.Double(); // точки многоугольника, задающего очертания слоя грунта 
	private double f; // угол внутреннего трения, градусы
	private double c; // удельное сцепление, кПа
	private double g; // удельный вес, кН/куб. м 

	private String soilName = "";
	private int hatchIndex = -1;
	private Subgrade parentSubgrade;
	
	public SubgradeSoil()
	{
		f = 20.5/180 * Math.PI;
		c = 40;
		g = 19.9;
	}
	
	public SubgradeSoil(Path2D.Double SoilPath)
	{
		f = 20.5/180 * Math.PI;
		c = 40;
		g = 19.9;
		SoilLayer = SoilPath;
	}
	
	public void setSubgrade(Subgrade subgrade)
	{
		parentSubgrade = subgrade;
	}
	
	public double getF() {
		return f;
	}
	
	public double getC() {
		return c;
	}
	
	public double getG() {
		return g;
	}
	
	public void setF(double f) {
		// TODO: угол внутреннего трения - сделать проверку правильности присваиваемого значения
		this.f = f;
	}
	
	public void setC(double c) {
		// TODO: удельное сцепление - сделать проверку правильности присваиваемого значения
		this.c = c;
	}
	
	public void setG(double g) {
		// TODO: удельный вес - сделать проверку правильности присваиваемого значения
		this.g = g;
	}
	
	public void AddPoint(Point2D.Double pt, Point2D.Double origin)
	{
		AddPoint(pt.x - origin.x, pt.y - origin.y);
	}
	
	public void AddPoint(Point2D.Double pt)
	{
		AddPoint(pt.x, pt.y);
	}
	
	public void AddPoint(double x, double y)
	{
		// TODO: Проверка возможности размещения точки с заданными координатами
		Point2D.Double new_pt = new Point2D.Double(x, y);
		Point2D.Double last_pt = (Point2D.Double)SoilLayer.getCurrentPoint();
		if (last_pt == null) 
		{
			SoilLayer.moveTo(x, y);
		} else if (!TwinPoints(new_pt, last_pt)) SoilLayer.lineTo(x, y);
	}
	
	public void Close()
	{
		SoilLayer.closePath();
	}
	
	// Максимальная координата по горизонтали
	public double MaxX()
	{
		return SoilLayer.getBounds().getMaxX();
	}
	
	// Максимальная координата по вертикали
	public double MaxY()
	{
		return SoilLayer.getBounds().getMaxY();
	}
	
	// Минимальная координата по горизонтали
	public double MinX()
	{
		return SoilLayer.getBounds().getMinX();
	}
		
	// Минимальная координата по вертикали
	public double MinY()
	{
		return SoilLayer.getBounds().getMinY();
	}
	
	public Point2D.Double LastPoint()
	{
		return (Point2D.Double) SoilLayer.getCurrentPoint();
	}

	public boolean PointInside(Point2D.Double pt)
	{
		Area a = new Area(SoilLayer);
		return a.contains(pt);
	}
	
	public Area SoilArea()
	{
		return new Area(SoilLayer);
	}
	
	private Point2D.Double ClosestLinePoint(Point2D.Double LineStart, Point2D.Double LineEnd, Point2D.Double pt)
	{
		Line2D.Double line = new Line2D.Double(LineStart, LineEnd);
		Line2D.Double purp = Purpendicular(line, pt);
		Point2D.Double Closest = IntersectionPoint(line, purp, 0, -1, false);
		
		if (Closest == null) return null;
		if (TwinPoints(Closest, LineStart)) return LineStart;
		if (TwinPoints(Closest, LineEnd)) return LineEnd;
		
		return Closest;
	}
	
	public Line2D.Double LineNearby(double x, double y, double tolerance)
	{
		PathIterator pathi = SoilLayer.getPathIterator(null);   
		double coords[] = new double[6];
		Point2D.Double pt1 = null, pt2 = null, pt = null;
		
		while (!pathi.isDone())
		{
			pathi.currentSegment(coords);
			pt1 = new Point2D.Double(coords[0], coords[1]);
			pathi.next();
			if (!pathi.isDone())
			{
				pathi.currentSegment(coords);
				pt2 = new Point2D.Double(coords[0], coords[1]);
				pt = new Point2D.Double(x, y);
				double h = Line2D.ptSegDist(pt1.x, pt1.y, pt2.x, pt2.y, pt.x, pt.y);
				if ((h > 0) && (h <= tolerance))
				{ 
					if (ClosestLinePoint(pt1, pt2, pt) != null) return new Line2D.Double(pt1, pt2);
				}
			}
		}
		return null;
	}
	
	public Point2D.Double PointNearby(double x, double y, double tolerance)
	{
		PathIterator pathi = SoilLayer.getPathIterator(null);   
		double coords[] = new double[6];
		Point2D.Double pt = null;
		
		while (!pathi.isDone())
		{
			pathi.currentSegment(coords);
			double X = coords[0];
			double Y = coords[1];
			if (Point2D.distance(X, Y, x, y) <= tolerance)
			{
				pt = new Point2D.Double(X, Y);
				return pt;
			}
			pathi.next();
		}
		return pt;
	}
	
	public Point2D.Double SplitAt(Point2D.Double pt)
	{
		if (pt == null) return null;
		
		Path2D.Double first_part = new Path2D.Double();
		PathIterator pathi = SoilLayer.getPathIterator(null);   
		Line2D.Double line = GetNextSegment(pathi);
		if (line == null) return null;
		
		first_part.moveTo(line.getX1(), line.getY1()); 
		
		while (line != null)
		{
			Point2D.Double pt1 = (Point2D.Double)line.getP1();
			Point2D.Double pt2 = (Point2D.Double)line.getP2();
			
			double h = Line2D.ptSegDist(pt1.x, pt1.y, pt2.x, pt2.y, pt.x, pt.y);
			if ((h >= 0) && (h <= SubgradeMain.NEAR_TOLERANCE)) 
			{ 
				// Берём ближайшую к указанной пользователем точку, лежащую на выбранной линии - 
				// если только это не точка начала или конца линии
				Point2D.Double closest = ClosestLinePoint(pt1, pt2, pt);
				if ((closest != null) && (!TwinPoints(pt1, pt)) && (!TwinPoints(pt2, pt)))
				{
					first_part.lineTo(closest.x, closest.y);
					// добавляем оставшуюся геометрию
					first_part.append(pathi, true);
					SoilLayer = first_part;
					return closest;
				}
			} else first_part.lineTo(pt2.x, pt2.y);
			line = GetNextSegment(pathi);
		}
		
		return null;
	}
	
	private void DrawPoints(Graphics2D g2d, double dx, double dy)
	{
		// Отрисовка точек (кружки пожирнее)
		PathIterator pathi = SoilLayer.getPathIterator(null);   
		double coords[] = new double[6];
		double cr = Math.max(dx, dy) * 0.005;
				
		while (!pathi.isDone())
		{
			pathi.currentSegment(coords);
			double X = coords[0];
			double Y = coords[1];
			g2d.fill(new Ellipse2D.Double(X-cr, Y-cr, 2*cr, 2*cr));
			pathi.next();
		}
	}
	
	public void Paint(Graphics2D g2d, boolean draw_points, double dx, double dy)
	{
		g2d.setPaint(Color.black);
		
		
		if ((parentSubgrade != null) && (hatchIndex > 0))
		{
			java.awt.Paint savedPaint = g2d.getPaint();
			BufferedImage hatch = parentSubgrade.getPattern(hatchIndex);
			if (hatch != null)
			{
				// TODO Разобраться как размеры текстуры соотносятся с масштабом чертежа
				AffineTransform at = g2d.getTransform();
				int w = hatch.getWidth();
				int h = hatch.getHeight();
				TexturePaint tp = new TexturePaint(hatch, new Rectangle2D.Double(0, 0, w / at.getScaleX(), h / Math.abs(at.getScaleX())));
				g2d.setPaint(tp);
				g2d.fill(SoilLayer);
			}
			g2d.setPaint(savedPaint); 
		}
		g2d.draw(SoilLayer);
		if (draw_points) DrawPoints(g2d, dx, dy);
	}

	// Отрисовка слоя грунта на экране
	public void Paint(Graphics2D g2d, double dx, double dy)
	{
		Paint(g2d, true, dx, dy);
	}
	
	public boolean DeletePoint(Point2D.Double pt) 
	{
		// Обходим геометрию до тех пор, пока не встретим указанную линию
		// Встретив, удаляем
		Path2D.Double first_part = new Path2D.Double();
		PathIterator pathi = SoilLayer.getPathIterator(null);   
		double coords[] = new double[6];
		int seg;
		
		while (!pathi.isDone())
		{
			seg = pathi.currentSegment(coords);
			// Если точка совпадает с заданной - удаляем
			if (pt.distanceSq(coords[0], coords[1]) <= Math.pow(SubgradeMain.NEAR_TOLERANCE, 2))
			{
				pathi.next();
				if (!pathi.isDone())
				{
					pathi.currentSegment(coords);
					// Если удаляемая точка - первая, то 
					if (first_part.getCurrentPoint() == null)
					{
						first_part.moveTo(coords[0], coords[1]);
						pathi.next();
						first_part.append(pathi, true);
					} else first_part.append(pathi, true);
				}
				SoilLayer = first_part;
				return true;
			} else 
			{
				switch (seg) 
				{
					case PathIterator.SEG_MOVETO: first_part.moveTo(coords[0], coords[1]); break;
					case PathIterator.SEG_LINETO: first_part.lineTo(coords[0], coords[1]); break;
				};
				pathi.next();
			}
		}
		return false;
	}
	
	public void MovePoint(Point2D.Double pt_from, Point2D.Double pt_to) 
	{
		// Обходим геометрию до тех пор, пока не встретим указанную точку
		Path2D.Double PreviousGeom = new Path2D.Double(); 
		PathIterator pathi = SoilLayer.getPathIterator(null);   
		double coords[] = new double[6];
		int seg;
				
		while (!pathi.isDone())
		{
			seg = pathi.currentSegment(coords);	
			
			if (pt_from.distanceSq(coords[0], coords[1]) <= Math.pow(SubgradeMain.NEAR_TOLERANCE, 2))
			{
				// Нашли точку.
				// Всю геометрию до неё сохраняем, всю геометрию после - тоже, а данную точку - перемещаем
				if (PreviousGeom.getCurrentPoint() == null) 
				{
					PreviousGeom.moveTo(pt_to.x, pt_to.y);
				} else PreviousGeom.lineTo(pt_to.x, pt_to.y);
				pathi.next();
				PreviousGeom.append(pathi, true);
				SoilLayer = PreviousGeom;
				return;
			} else
			{
				switch (seg) 
				{
					case PathIterator.SEG_MOVETO: PreviousGeom.moveTo(coords[0], coords[1]); break;
					case PathIterator.SEG_LINETO: PreviousGeom.lineTo(coords[0], coords[1]); break;
				};
			}
			pathi.next();
		}
	}
	
	public boolean MyLine(Line2D.Double line) 
	{
		if (line == null) return false;
		Point2D.Double pt1 = (Point2D.Double)line.getP1();
		Point2D.Double pt2 = (Point2D.Double)line.getP2();
		
		PathIterator p = GetPathIterator();
		Line2D.Double my_line = GetNextSegment(p);
		while (my_line != null)
		{
			Point2D.Double my_pt1 = (Point2D.Double)my_line.getP1();
			Point2D.Double my_pt2 = (Point2D.Double)my_line.getP2();
			
			if ((TwinPoints(my_pt1, pt1) && TwinPoints(my_pt2, pt2)) ||
				(TwinPoints(my_pt1, pt2) && TwinPoints(my_pt2, pt1))) return true;	
			my_line = GetNextSegment(p);
		}
		return false;
	}
	
	public boolean DeleteLine(Line2D.Double line) 
	{
		// Обходим геометрию до тех пор, пока не встретим указанную линию
		// Встретив, удаляем
		
		
		Path2D.Double first_part = new Path2D.Double();
		PathIterator pathi = SoilLayer.getPathIterator(null);   
		double coords[] = new double[6];
		Point2D.Double pt1 = null, pt2 = null;
		
		while (!pathi.isDone())
		{
			switch (pathi.currentSegment(coords)) 
			{
				case PathIterator.SEG_MOVETO: first_part.moveTo(coords[0], coords[1]); break;
				case PathIterator.SEG_LINETO: first_part.lineTo(coords[0], coords[1]); break;
			};
			pt1 = new Point2D.Double(coords[0], coords[1]);
						
			pathi.next();
			if (!pathi.isDone())
			{
				pathi.currentSegment(coords);
				pt2 = new Point2D.Double(coords[0], coords[1]);
				
				// Полученные координаты начала и конца сегмента сравниваем
				// с точками начала и конца заданной линии
				if ((line.ptLineDistSq(pt1) <= Math.pow(SubgradeMain.NEAR_TOLERANCE, 2)) &&
					(line.ptLineDistSq(pt2) <= Math.pow(SubgradeMain.NEAR_TOLERANCE, 2)))
				{
					// Если начальная и конечная точки сегмента обе лежат практически на линии,
					// то считаем - нашли
					// И вместо линии - просто перемещаем 
					pathi.next();
					if (!pathi.isDone())
					{
						first_part.moveTo(coords[0], coords[1]);
						first_part.append(pathi, false);
					}
					SoilLayer = first_part;
					return true;
				}
			}
		}
		return false;
	}
	
	public static Line2D.Double Purpendicular(Line2D.Double line, Point2D.Double pt)
	{
		double Eq[] = LineEquation(line);
		return Purpendicular(Math.atan(-Eq[0]/Eq[1]), pt);
	}
	
	public static Line2D.Double Purpendicular(double line_angle, Point2D.Double pt)
	{
		double a, b, c;
		if (Math.abs(line_angle) == 0) 
		{
			// Если линия будет вертикальной, то 
			a = 1; b = 0; c = -pt.x;
			return new Line2D.Double(pt.x, pt.y, pt.x, pt.y + 10);
		} else 
			// Если горизонтальной - то
			if (Math.abs(line_angle) == Math.PI/2)
			{
				a = 0; b = 1; c = -pt.y;
				return new Line2D.Double(pt.x, pt.y, pt.x + 10, pt.y);
			} else
			{
				a = Math.tan(line_angle + Math.PI/2);
				b = -1;
				c = pt.y - pt.x * a;
				return new Line2D.Double(pt.x, pt.y, pt.x + 10, (-a * (pt.x + 10) - c) / b);
			}
	}

	public static double[] LineEquation(Line2D.Double line)
	{
		// Используем уравнение прямой в отрезках на осях
		double d[] = new double[3];
		
		// Если у1 = y2, то с осью х прямая не пересекается, т.е. a = бесконечность
		// Если x1 = x2, то с осью y нет пересечения и b = бесконечности
		if (line.y1 == line.y2) { d[0] = 0; d[1] = 1; d[2] = -line.y1; return d; };
		if (line.x1 == line.x2) { d[0] = 1; d[1] = 0; d[2] = -line.x1; return d; };
		
		// Если ничто из вышеупомянутого не выполняется, то 
		// прямая наклонена к обеим осям
		double k, b;
		k = (line.y2 - line.y1) / (line.x2 - line.x1);
		b = line.y2 - (line.x2 * k);
		
		d[0] = k; d[1] = -1; d[2] = b;
		return d;
	}
	
	public static Point2D.Double IntersectionPoint(Line2D.Double line1, Line2D.Double line2)
	{
		return IntersectionPoint(line1, line2, -1, -1, true);
	}
	
	public static boolean RayLineInt(Point2D.Double pt, Line2D.Double ray, Line2D.Double line, int n_pt)
	{
		Point2D.Double pt_start, pt_end;
		if (n_pt == 1) 
		{ 
			pt_start = (Point2D.Double)ray.getP1();
			pt_end = (Point2D.Double)ray.getP2();
		} else
		{
			pt_start = (Point2D.Double)ray.getP2();
			pt_end = (Point2D.Double)ray.getP1();
		}
		double ray_signX = Math.signum(pt_start.x - pt_end.x);
		double ray_signY = Math.signum(pt_start.y - pt_end.y);
		
		if (!PointOnLine(line, pt) || 
			((ray_signX != Math.signum(pt_start.x - pt.x)) ||
			 (ray_signY != Math.signum(pt_start.y - pt.y)))) return false;
		
		return true;
	}
	
	public static double RaySign(Line2D.Double ray, int n_pt, int xy)
	{
		Point2D.Double pt_start, pt_end;
		if (n_pt == 1) 
		{ 
			pt_start = (Point2D.Double)ray.getP1();
			pt_end = (Point2D.Double)ray.getP2();
		} else
		{
			pt_start = (Point2D.Double)ray.getP2();
			pt_end = (Point2D.Double)ray.getP1();
		}
		switch (xy)
		{
			case 1: return Math.signum(pt_start.x - pt_end.x); 
			default: return Math.signum(pt_start.y - pt_end.y);
		}
	}
	
	public static double RayPointSign(Point2D.Double pt, Line2D.Double ray, int n_pt, int xy)
	{
		Point2D.Double pt_start;
		if (n_pt == 1) pt_start = (Point2D.Double)ray.getP1(); else pt_start = (Point2D.Double)ray.getP2();
		switch (xy)
		{
			case 1: return Math.signum(pt_start.x - pt.x); 
			default: return Math.signum(pt_start.y - pt.y);
		}
	}
	
	public static boolean RayRayInt(Point2D.Double pt, Line2D.Double ray1, Line2D.Double ray2, int n_pt1, int n_pt2)
	{
		double ray1_signX = RaySign(ray1, n_pt1, 1);
		double ray1_signY = RaySign(ray1, n_pt1, 2);
		double ray2_signX = RaySign(ray2, n_pt2, 1);
		double ray2_signY = RaySign(ray2, n_pt2, 2);
		
		double ray1_pt_signX = RayPointSign(pt, ray1, n_pt1, 1);
		double ray1_pt_signY = RayPointSign(pt, ray1, n_pt1, 2);
		
		double ray2_pt_signX = RayPointSign(pt, ray2, n_pt2, 1);
		double ray2_pt_signY = RayPointSign(pt, ray2, n_pt2, 2);
		
	    return (ray1_signX == ray1_pt_signX) && 
	    	   (ray2_signX == ray2_pt_signX) && 
	    	   (ray1_signY == ray1_pt_signY) && 
	    	   (ray2_signY == ray2_pt_signY);
	}
	
	public static boolean PointOnLine(Line2D.Double line, Point2D.Double pt)
	{
		double Eq[] = LineEquation(line);
		double zero = Eq[0] * pt.x + Eq[1] * pt.y + Eq[2];
				
		return (zero >= -0.001) && (zero <= 0.001) && 
			   (pt.x > Math.min(line.x1, line.x2) - 0.025) &&
			   (pt.y < Math.max(line.y1, line.y2) + 0.025) &&
			   (pt.x < Math.max(line.x1, line.x2) + 0.025) &&
			   (pt.y > Math.min(line.y1, line.y2) - 0.025);
	}
	
	public static boolean TwinPoints(Point2D.Double pt1, Point2D.Double pt2)
	{
		if ((pt1 != null) && (pt2 != null))
		{
			double dist = Point2D.distance(pt1.x, pt1.y, pt2.x, pt2.y);
			return (dist <= 0.01);
		} else return false;
	}
	
	// Поиск точки перечения двух линий. 
	// Линии могут считаться бесконечными прямыми,
	// либо лучами, либо отрезками
	// если линия1 - луч, то параметр pt1 задаёт начало луча
	// аналогично для линии2
	// если линия1 - это отрезок, то задано to_pt1
	// если соответствующие объекты - Null, то линия не ограничена с соответствующей стороны 
	// check_ends означает, принимать ли конечные и начальные точки отрезков во внимание
	public static Point2D.Double IntersectionPoint(Line2D.Double line1, Line2D.Double line2, int l_one, int l_two, boolean check_ends)
	{
		Point2D.Double pt = new Point2D.Double();
		double Eq1[], Eq2[];
		
		Eq1 = LineEquation(line1);
		Eq2 = LineEquation(line2);
		
		// Если линии параллельны - нет пересечения
		if (Eq1[0] == Eq2[0]) return null; 
		
		if (Eq1[0] != 0)
		{
			pt.y = (Eq2[2] - Eq1[2] * Eq2[0] / Eq1[0]) / (Eq2[0] / Eq1[0] * Eq1[1] - Eq2[1]);
			pt.x = (-Eq1[2] - Eq1[1] * pt.y) / Eq1[0];
		} else
		{
			pt.y = -Eq1[2] / Eq1[1];
			pt.x = (-Eq2[2] - Eq2[1] * pt.y) / Eq2[0];
		}
		
		// Отрихтуем
		double delta1 = Eq1[0] * pt.x + Eq1[1] * pt.y + Eq1[2];
		double delta2 = Eq2[0] * pt.x + Eq2[1] * pt.y + Eq2[2];
		while ((Math.abs(delta1) > 0.05) || Math.abs(delta2) > 0.05)
		{
			if ((Eq1[0] != 0) && (Eq1[1] != 0)) 
			{ 
				pt.x = pt.x - 0.5 * delta1 / Eq1[0]; 
				pt.y = pt.y - 0.5 * delta1 / Eq1[1]; 
			} else
				if (Eq1[0] != 0) pt.x = pt.x - delta1 / Eq1[0];
				else pt.y = pt.y - delta1 / Eq1[1];
			
			delta1 = Eq1[0] * pt.x + Eq1[1] * pt.y + Eq1[2];
			delta2 = Eq2[0] * pt.x + Eq2[1] * pt.y + Eq2[2];
			
			if (delta2 != 0)
			{
				if ((Eq2[0] != 0) && (Eq2[1] != 0)) 
				{ 
					pt.x = pt.x - 0.5 * delta2 / Eq2[0]; 
					pt.y = pt.y - 0.5 * delta2 / Eq2[1]; 
				} else
					if (Eq2[0] != 0) pt.x = pt.x - delta2 / Eq2[0];
					else pt.y = pt.y - delta2 / Eq2[1];
			}
		} 
		
		// Если обе линии - отрезки, то необходимо, чтобы точка принадлежала им обоим
		if ((l_one == 0) && !PointOnLine(line1, pt)) return null; 
		if ((l_two == 0) && !PointOnLine(line2, pt)) return null; 
			
		// Если первая - отрезок, а вторая - луч
		// то обязательно точка должна лежать на первом отрезке и 
		// знак разности между координатами точки пересечения и координатами точки начала луча
		// должен совпадать со знаком разности между координатами начала луча и второй точки линии 2
		if ((l_one == 0) && (l_two > 0) && !RayLineInt(pt, line2, line1, l_two)) return null;
		if ((l_one > 0) && (l_two == 0) && !RayLineInt(pt, line1, line2, l_one)) return null;
		
		// Если обе - лучи, то знаки разностей должны совпадать у всех 
		if ((l_one > 0) && (l_two > 0) && !RayRayInt(pt, line1, line2, l_one, l_two)) return null;
					
		if (check_ends)
		{
			if (pt.equals(line1.getP1()) || pt.equals(line1.getP2()) || 
				pt.equals(line2.getP1()) || pt.equals(line2.getP2())
			   ) return null;
		}
		
		return pt;		
	}
	
	public Line2D.Double ClosestLine(Point2D.Double pt, double angle)
	{
		PathIterator pathi = GetPathIterator();
		Line2D.Double line = GetNextSegment(pathi);
		Line2D.Double ray = new Line2D.Double(pt.x, pt.y, pt.x + Math.cos(angle), pt.y + Math.sin(angle));
		ArrayList<Line2D.Double> lines = new ArrayList<Line2D.Double>();
		
		// Найдём все пересечения
		while (line != null)
		{
			Point2D.Double ipt = IntersectionPoint(line, ray, 0, 1, false);
			if (ipt != null) lines.add(ray);
			line = GetNextSegment(pathi);
		}
		
		// Теперь среди них найдём ближайшее
		double min_dist = 1000;
		int min_index = -1;
		for (int i = 0; i < lines.size(); i++)
		{
			line = lines.get(i);
			double dist = Line2D.ptSegDist(line.x1, line.y1, line.x2, line.y2, pt.x, pt.y);
			if (dist <= min_dist)
			{
				min_dist = dist;
				min_index = i;
			}
		}
		if (min_index > -1) return lines.get(min_index); else return null;
	}
	
	public void CreatePointsAtIntersection(SubgradeSoil subgradeSoil, Line2D.Double line)
	{
		// Ищем сегмент, пересекающийся с заданной линией
		// Если находим, то создаём узел в точке пересечения
		// Линия может пересекаться с более, чем двумя сегментами - поэтому продолжаем до последнего сегмента
		PathIterator pathi = GetPathIterator();
		Line2D.Double my_line = GetNextSegment(pathi);
		while (my_line != null)
		{
			if (line.intersectsLine(my_line))
			{
				Point2D.Double pt = IntersectionPoint(line, my_line);
				pt = SplitAt(pt);
				subgradeSoil.SplitAt(pt);
			}
			my_line = GetNextSegment(pathi);
		}
	}
	
	public PathIterator GetPathIterator()
	{
		return SoilLayer.getPathIterator(null);
	}
	
	public ArrayList<Point2D.Double> LayerPoints()
	{
		ArrayList<Point2D.Double> points = new ArrayList<Point2D.Double>();
		PathIterator p = GetPathIterator();
		Line2D.Double line = GetNextSegment(p);
		while (line != null)
		{
			points.add((Point2D.Double)line.getP1());
			points.add((Point2D.Double)line.getP2());
			line = GetNextSegment(p);
		}
		return points;
	}
	
	public Line2D.Double GetNextSegment(PathIterator pathi) 
	{
		if (pathi == null) return null;
		
		double coords[] = new double[6];
		
		pathi.currentSegment(coords);
		Point2D.Double pt1 = new Point2D.Double(coords[0], coords[1]);
		Point2D.Double pt2 = null;
		pathi.next();
		if (!pathi.isDone())
		{
			if (pathi.currentSegment(coords) != PathIterator.SEG_CLOSE)
			{
				pt2 = new Point2D.Double(coords[0], coords[1]);
			} else 
				{
					PathIterator p = GetPathIterator();
					p.currentSegment(coords);
					pt2 = new Point2D.Double(coords[0], coords[1]);
				}
		
			return new Line2D.Double(pt1, pt2);
		} return null;
	}
	
	public boolean isEmpty() 
	{
		PathIterator p = SoilLayer.getPathIterator(null);
		p.next();
		return p.isDone();
	}

	public void toXML(Element e2) {
	    e2.setAttribute("Weight", Double.toString(getG()));
	    e2.setAttribute("Cohesion", Double.toString(getC()));
	    e2.setAttribute("FrictionAngle", Double.toString(getF()));
	    e2.setAttribute("SoilName", soilName);
	    e2.setAttribute("Hatch", Integer.toString(hatchIndex));
		
	    Element geo = e2.getOwnerDocument().createElement("Geometry");
	    e2.appendChild(geo);
	    PathIterator path = SoilLayer.getPathIterator(null);
	    double c[] = new double[6];
	    int seg; int i = 0;
	    while (!path.isDone()) 
	    {
	    	seg = path.currentSegment(c);
	    	Element node = e2.getOwnerDocument().createElement("point"+Integer.toString(i));
		    geo.appendChild(node);
		    node.setAttribute("op", Integer.toString(seg));
		    
	    	switch (seg)
	    	{
	    		case PathIterator.SEG_MOVETO: case PathIterator.SEG_LINETO: 
	    		{
	    			node.setAttribute("X1", Double.toString(c[0]));
	    			node.setAttribute("Y1", Double.toString(c[1]));
	    			break;
	    		}
	    		case PathIterator.SEG_QUADTO: 
	    		{
	    			node.setAttribute("X1", Double.toString(c[0]));
	    			node.setAttribute("Y1", Double.toString(c[1]));
	    			node.setAttribute("X2", Double.toString(c[2]));
	    			node.setAttribute("Y2", Double.toString(c[3]));
	    		}
	    		case PathIterator.SEG_CUBICTO: 
	    		{
	    			node.setAttribute("X1", Double.toString(c[0]));
	    			node.setAttribute("Y1", Double.toString(c[1]));
	    			node.setAttribute("X2", Double.toString(c[2]));
	    			node.setAttribute("Y2", Double.toString(c[3]));
	    			node.setAttribute("X3", Double.toString(c[4]));
	    			node.setAttribute("Y3", Double.toString(c[5]));
	    			break;
	    		}
	    	}
	    	i++;
	    	path.next();
	    }
	}
	
	public static double myParseDouble(String text) 
	{
		DecimalFormat format = (DecimalFormat)DecimalFormat.getInstance();
		DecimalFormatSymbols symbols=format.getDecimalFormatSymbols();
		String sep = String.valueOf(symbols.getDecimalSeparator());
		
		text = text.replace(".", sep);
		text = text.replace(",", sep);
		
		NumberFormat nf = NumberFormat.getInstance();	
		try {
			return nf.parse(text).doubleValue();
		} catch (ParseException e1) {
			return PARSE_ERROR;
		}
	}

	// Загрузка слоя грунта из xml
	public void Load(Node item) 
	{
		double F = myParseDouble(item.getAttributes().getNamedItem("FrictionAngle").getNodeValue());
		if (F == PARSE_ERROR) F = 20.5/180 * Math.PI;
		
		double G = myParseDouble(item.getAttributes().getNamedItem("Weight").getNodeValue());
		if (G == PARSE_ERROR) G = 19.9;
		
		double C = myParseDouble(item.getAttributes().getNamedItem("Cohesion").getNodeValue());
		if (C == PARSE_ERROR) C = 40;
		
		soilName = item.getAttributes().getNamedItem("SoilName").getNodeValue();
		hatchIndex = Integer.parseInt(item.getAttributes().getNamedItem("Hatch").getNodeValue());
		
		f = F;
		c = C;
		g = G;
		
		for (int i = 0; i < item.getChildNodes().getLength(); i++)
		{
			Node geo = item.getChildNodes().item(i);
			if (geo.getNodeName() == "Geometry")
			{
				for (int j = 0; j < geo.getChildNodes().getLength(); j++)
				{
					Node point = geo.getChildNodes().item(j);
					int seg = Integer.parseInt(point.getAttributes().getNamedItem("op").getNodeValue());
					switch (seg)
					{
						case PathIterator.SEG_CLOSE: 
						{
							SoilLayer.closePath();
							break;
						}
						case PathIterator.SEG_MOVETO: 
						{
							double x = myParseDouble(point.getAttributes().getNamedItem("X1").getNodeValue());
							double y = myParseDouble(point.getAttributes().getNamedItem("Y1").getNodeValue());
							SoilLayer.moveTo(x, y);
							break;
						}
						case PathIterator.SEG_LINETO: 
						{
							double x = myParseDouble(point.getAttributes().getNamedItem("X1").getNodeValue());
							double y = myParseDouble(point.getAttributes().getNamedItem("Y1").getNodeValue());
							SoilLayer.lineTo(x, y);
							break;
						}
						case PathIterator.SEG_QUADTO: 
						{
							double x1 = myParseDouble(point.getAttributes().getNamedItem("X1").getNodeValue());
							double y1 = myParseDouble(point.getAttributes().getNamedItem("Y1").getNodeValue());
							double x2 = myParseDouble(point.getAttributes().getNamedItem("X2").getNodeValue());
							double y2 = myParseDouble(point.getAttributes().getNamedItem("Y2").getNodeValue());
							SoilLayer.quadTo(x1, y1, x2, y2);
							break;
						}
						case PathIterator.SEG_CUBICTO: 
						{
							double x1 = myParseDouble(point.getAttributes().getNamedItem("X1").getNodeValue());
							double y1 = myParseDouble(point.getAttributes().getNamedItem("Y1").getNodeValue());
							double x2 = myParseDouble(point.getAttributes().getNamedItem("X2").getNodeValue());
							double y2 = myParseDouble(point.getAttributes().getNamedItem("Y2").getNodeValue());
							double x3 = myParseDouble(point.getAttributes().getNamedItem("X3").getNodeValue());
							double y3 = myParseDouble(point.getAttributes().getNamedItem("Y3").getNodeValue());
							SoilLayer.curveTo(x1, y1, x2, y2, x3, y3);
							break;
						}
					}
				}
			}
		}
	}

	public String getSoilName() {
		return soilName;
	}
	
	public void setSoilName(String newSoilName) {
		soilName = newSoilName;
	}
	
	public int getHatchIndex() {
		return hatchIndex;
	}
	
	public void setHatchIndex(int newHatchIndex) {
		hatchIndex = newHatchIndex;
	}

	public double Area() {
		// Площадь вычисляем по алгоритму Гаусса
		ArrayList<Point2D.Double> pts = new ArrayList<Point2D.Double>();
		
		PathIterator pi = GetPathIterator();
		Line2D.Double line = GetNextSegment(pi);
		double A = 0;
		while (line != null)
		{
			pts.add((Point2D.Double) line.getP1());
			line = GetNextSegment(pi);
		}
		for (int i = 0; i < pts.size(); i++)
		{
			int j = i - 1; if (j < 0) j = pts.size() - 1;
			int k = i + 1; if (k == pts.size()) k = 0;
			
			A = A + pts.get(i).x*(pts.get(k).y - pts.get(j).y);
		}
		return Math.abs(A / 2);
		/*double side_len[] = new double[4];
		Point2D.Double pts[] = new Point2D.Double[4];
		int i = 0;
		while ((line != null) && (i < 4))
		{
			side_len[i] = Point2D.distance(line.x1, line.y1, line.x2, line.y2);
			if (side_len[i] <= 0.01)
			{
				line = GetNextSegment(pi);
				continue;
			}
			pts[i] = (Point2D.Double) line.getP1();
			line = GetNextSegment(pi);
			i++;
		}
		switch (i)
		{
			case 3: {
				double det = (pts[0].x - pts[2].x) * (pts[1].y - pts[2].y) - (pts[1].x - pts[2].x) * (pts[0].y - pts[2].y);
				return Math.abs(det / 2);
			}
			case 4:
			{
				double half_p = 0;
				for (double sl: side_len) half_p = half_p + sl;
				half_p = half_p / 2;
				double S = 1;
				for (double sl: side_len) S = S * (half_p - sl);
				return Math.sqrt(S);
			}
		}*/
	}
}
