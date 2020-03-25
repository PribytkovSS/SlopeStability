
import java.util.ArrayList;

import javax.swing.DefaultListModel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;


// Этот класс - земляное полотно
// Описывает кусочек земполотна, устойчивость которого надо рассчитать
public class Subgrade {
	private ArrayList<SubgradeSoil> SoilLayers; // Слои грунта, включая основание земполотна
	private ArrayList<SubgradeSoil> RefinedLayers; // Временное хранилище слоёв для процедуры преобразования
	private ArrayList<SubgradeLoad> Loads; // Нагрузки на поверхности грунта, включая постоянные от веса пути и временную от поезда
	
	public ArrayList<SubgradeLoad> getLoads() {
		return Loads;
	}

	private Path2D.Double SearchPath;
	private ArrayList<Line2D.Double> Closed;
	protected ArrayList<SoilBlock> blocks; 
	
	protected static final double INVALID_INTERFACE = 9999; 
	
	public ArrayList<Point2D.Double> SlopePoints;
	private DefaultListModel<SubgradeSoilHatch> patternList;
	
	public void BasicInitialization()
	{
		// Изначальная конфигурация земляного полотна включает насыпи и 
		// её основания - это два слоя грунта c характеристиками по умолчанию
		SoilLayers = new ArrayList<SubgradeSoil>();
		RefinedLayers = new ArrayList<SubgradeSoil>();
		Loads = new ArrayList<SubgradeLoad>();

		SlopePoints = new ArrayList<Point2D.Double>();
				
		SearchPath = new Path2D.Double();
		Closed = new ArrayList<Line2D.Double>();
		blocks = new ArrayList<SoilBlock>();
		
		patternList = null;
	}
	
	// Конструктор "по умолчанию"
	public Subgrade()
	{
		BasicInitialization();
				
		SubgradeSoil f = new SubgradeSoil();
		f.setSubgrade(this);
		
		// Основания
		SoilLayers.add(f);
		f.AddPoint(0, 0);
		f.AddPoint(0, 10);
		f.AddPoint(10, 10);
		f.AddPoint(34, 10);
		f.AddPoint(44, 10);
		f.AddPoint(44, 0);
		f.Close();
		
		// Насыпь
		f = new SubgradeSoil();
		f.setSubgrade(this);
		SoilLayers.add(f);
		f.AddPoint(10, 10);
		f.AddPoint(19, 16);
		f.AddPoint(25, 16);
		f.AddPoint(34, 10);
		f.Close();
	}
	
	// Конструктор земполотна из xml-описания
	public Subgrade(Document xml_doc)
	{
		BasicInitialization();
		Load(xml_doc);
	}

	// Конструктор по базовым данным о поперечном профиле земполотна
	public Subgrade(double LeftB, double RightB, double CenterB, double LeftHeight, 
			        double LeftTrackGroundHeight, double Inclination)
	{
		BasicInitialization();
		
		// 1. Расстояние от оси левого пути до левой бровки ("LeftB")
		// 2. Расстояние между осями путей ("CenterB")
		// 3. Расстояние от оси правого пути до правой бровки ("RightB")
		// 4. Высотная отметка левой бровки ("LeftHeight")
		// 5. Высотная отметка земли по оси левого пути ("LeftTrackGroundHeight")
		// 6. Заложение откоса (расстояние, через которое отметка земли уменьшится на 1 м)
		//    Если заложение положительное, то отметки земли понижаются слева направо.  
		//    Если отрицательное, то повышаются слева направо.
					
				
		// Прямая, описывающая основную площадку
		// В качестве точки отсчёта принимаем точку, расположенную на 
		// оси левого пути в уровне основной площадки
		Point2D.Double pt_leftb = new Point2D.Double(-LeftB, LeftHeight);
		Point2D.Double pt_rightb = new Point2D.Double(-LeftB + CenterB + RightB, LeftHeight);
		Line2D.Double base_line = new Line2D.Double(pt_leftb, pt_rightb);
		// Линия, описывающая естественную поверхность грунта
		Point2D.Double pt_gound1 = new Point2D.Double(0, LeftTrackGroundHeight);
		Point2D.Double pt_gound2 = new Point2D.Double(Inclination, LeftTrackGroundHeight - 1);
		Line2D.Double ground_line = new Line2D.Double(pt_gound1, pt_gound2);
		// Теперь откосы
		// Ищем точку пересечения отрезка основной площадки и прямой естественной поверхности
		// Если эта точка существует, то мы имеем дело с переходным типом земполотна - 
		// т.е. с той стороны, где естественная поверхность выше основной площадки 
		// будет выемка, а значит и кювет, а с той стороны, где насыпь - просто откос
		
		Point2D.Double ip = SubgradeSoil.IntersectionPoint(base_line, ground_line, 0, -1, false);
		Point2D.Double left_slope = null, right_slope = null;
		Point2D.Double pt_ditch1L = null, pt_ditch2L = null, pt_ditch1R = null, pt_ditch2R = null;
		Line2D.Double leftSlope, rightSlope;
		
		if (ip != null)
		{
			// Точка пересечения есть
			// Значит, если Inclination положительная, то выемка - слева
			// если отрицательная, то справа
			if (Inclination > 0)
			{
				pt_ditch1L = new Point2D.Double(pt_leftb.x - 0.75, pt_leftb.y - 0.5);
				pt_ditch2L = new Point2D.Double(pt_ditch1L.x - 0.5, pt_ditch1L.y);
				left_slope = new Point2D.Double(pt_ditch2L.x - 1.5, pt_ditch2L.y + 1); 
				leftSlope = new Line2D.Double(pt_ditch2L.x, pt_ditch2L.y, left_slope.x, left_slope.y);
				left_slope = SubgradeSoil.IntersectionPoint(leftSlope, ground_line, -1, -1, false);
				right_slope = new Point2D.Double(pt_rightb.x + 0.75, pt_rightb.y - 0.5);
				rightSlope = new Line2D.Double(pt_rightb.x, pt_rightb.y, right_slope.x, right_slope.y);
				right_slope = SubgradeSoil.IntersectionPoint(rightSlope, ground_line, -1, -1, false);
			} else 
			{
				pt_ditch1R = new Point2D.Double(pt_rightb.x + 0.75, pt_rightb.y - 0.5);
				pt_ditch2R = new Point2D.Double(pt_ditch1R.x + 0.5, pt_ditch1R.y);
				right_slope = new Point2D.Double(pt_ditch2R.x + 1.5, pt_ditch2R.y + 1); 
				rightSlope = new Line2D.Double(pt_ditch2R.x, pt_ditch2R.y, right_slope.x, right_slope.y);
				right_slope = SubgradeSoil.IntersectionPoint(rightSlope, ground_line, -1, -1, false);
				
				left_slope = new Point2D.Double(pt_leftb.x - 1.5, pt_leftb.y - 1); 
				leftSlope = new Line2D.Double(pt_leftb.x, pt_leftb.y, left_slope.x, left_slope.y);
				left_slope = SubgradeSoil.IntersectionPoint(leftSlope, ground_line, -1, -1, false);
			}
		} else
			if (LeftTrackGroundHeight > LeftHeight)
			{
				// Чистая выемка
				// От основной площадки слева и справа под уколном 1:1,5 делаем 
				// откосы до дна кюветов, расположенного на 1 метр ниже основной площадки
				// Затем - горизонтальные линии дна на 0,5 м
				// и уже от этого дна откос под уклоном 1,5 до точек пересечения с 
				// линией естественной поверхности грунта	
				pt_ditch1L = new Point2D.Double(pt_leftb.x - 0.75, pt_leftb.y - 0.5);
				pt_ditch2L = new Point2D.Double(pt_ditch1L.x - 0.5, pt_ditch1L.y);
				left_slope = new Point2D.Double(pt_ditch2L.x - 1.5, pt_ditch2L.y + 1); 
				leftSlope = new Line2D.Double(pt_ditch2L.x, pt_ditch2L.y, left_slope.x, left_slope.y);
				left_slope = SubgradeSoil.IntersectionPoint(leftSlope, ground_line, -1, -1, false);
				// Теперь с правой стороны
				pt_ditch1R = new Point2D.Double(pt_rightb.x + 0.75, pt_rightb.y - 0.5);
				pt_ditch2R = new Point2D.Double(pt_ditch1R.x + 0.5, pt_ditch1R.y);
				right_slope = new Point2D.Double(pt_ditch2R.x + 1.5, pt_ditch2R.y + 1); 
				rightSlope = new Line2D.Double(pt_ditch2R.x, pt_ditch2R.y, right_slope.x, right_slope.y);
				right_slope = SubgradeSoil.IntersectionPoint(rightSlope, ground_line, -1, -1, false);
			} 
			else 
			{
				// Натуральная насыпь
				left_slope = new Point2D.Double(pt_leftb.x - 1.5, pt_leftb.y - 1); 
				right_slope = new Point2D.Double(pt_rightb.x + 1.5, pt_rightb.y - 1);
				leftSlope = new Line2D.Double(pt_leftb.x, pt_leftb.y, left_slope.x, left_slope.y);
				rightSlope = new Line2D.Double(pt_rightb.x, pt_rightb.y, right_slope.x, right_slope.y);
				// Точки пересечения откосов с естественной поверхностью грунта
				left_slope = SubgradeSoil.IntersectionPoint(leftSlope, ground_line, -1, -1, false);
				right_slope = SubgradeSoil.IntersectionPoint(rightSlope, ground_line, -1, -1, false);
			}
		
		// Расстояние между этими точками по горизонтали надо поделить на четыре 
		// и по этой четверти отступить влево и вправо, чтобы найти "край земли"
		double dx = (right_slope.x - left_slope.x) / 4;
		double dy = 0;
		double h = Math.abs(LeftTrackGroundHeight - LeftHeight);
		if (Inclination != 0) dy = dx / Inclination;
		
		Point2D.Double left_end = new Point2D.Double(left_slope.x - dx, left_slope.y + dy); 
		Point2D.Double right_end = new Point2D.Double(right_slope.x + dx, right_slope.y - dy); 
		// Глубина грунта от естественной поверхности - в половину высоты насыпи / глубины выемки
		// Но не менее 10 метров
		
		double deep_y = (Math.min(LeftTrackGroundHeight, LeftHeight) - Math.max(h/2, 10));
		deep_y = Math.min(deep_y, Math.min(left_end.y - 5, right_end.y - 5));
		
		Point2D.Double left_deep = new Point2D.Double(left_end.x, deep_y);
		Point2D.Double right_deep = new Point2D.Double(right_end.x, deep_y);
		
		// Теперь создаём основание насыпи
		SubgradeSoil f = new SubgradeSoil();
		f.setSubgrade(this);
		f.AddPoint(left_deep, left_deep);
		f.AddPoint(left_end, left_deep);
		f.AddPoint(left_slope, left_deep);
		if (pt_ditch1L != null) 
		{
			f.AddPoint(pt_ditch2L, left_deep);
			f.AddPoint(pt_ditch1L, left_deep);
		}
		if (ip != null) 
		{
			 if (Inclination > 0) f.AddPoint(pt_leftb, left_deep);
			 f.AddPoint(ip, left_deep);
			 if (Inclination < 0) f.AddPoint(pt_rightb, left_deep);
		} else 
			if ((pt_ditch1L != null) && (pt_ditch1R != null)) 
			{
				// Если чистая выемка, то между кюветами - основная площадка
				f.AddPoint(pt_leftb, left_deep);
				f.AddPoint(pt_rightb, left_deep);
			}
		
		
		if (pt_ditch1R != null) 
		{
			f.AddPoint(pt_ditch1R, left_deep);
			f.AddPoint(pt_ditch2R, left_deep);
		}
		f.AddPoint(right_slope, left_deep);
		f.AddPoint(right_end, left_deep);
		f.AddPoint(right_deep, left_deep);
		f.Close();
		
		SoilLayers.add(f);
		
		// Теперь земполотно
		f = new SubgradeSoil();
		f.setSubgrade(this);
		if (pt_ditch1L == null)
		{
			f.AddPoint(left_slope, left_deep);
			f.AddPoint(pt_leftb, left_deep);
		};
		if (ip != null)
		{
			f.AddPoint(ip, left_deep);
		};
		if (pt_ditch1R == null) 
		{
			f.AddPoint(pt_rightb, left_deep);
			f.AddPoint(right_slope, left_deep);
		};
		if ((pt_ditch1L == null) || (pt_ditch1R == null))
		{
			f.Close();
			SoilLayers.add(f);
		}
	}
	
	public ArrayList<SubgradeSoil> Layers()
	{
		return SoilLayers;
	}
	
	public SubgradeSoil AddNewSoilLayer(Point2D.Double pt)
	{
		SubgradeSoil NewSoil = new SubgradeSoil();
		NewSoil.setSubgrade(this);
		NewSoil.AddPoint(pt);

		SoilLayers.add(NewSoil);
		
		return NewSoil;
	}
	
	public double MaxX()
	{
		double X = -1000;
		for (int i = 0; i<SoilLayers.size(); i++) 
		{
			double x = SoilLayers.get(i).MaxX();
			if (X < x) X = x; 
		}
		return X;
	}
	
	public double MinX()
	{
		double X = 1000;
		for (int i = 0; i<SoilLayers.size(); i++) 
		{
			double x = SoilLayers.get(i).MinX();
			if (X > x) X = x; 
		}
		return X;
	}
	
	public double MaxY()
	{
		double Y = -1000;
		for (int i = 0; i<SoilLayers.size(); i++) 
		{
			double y = SoilLayers.get(i).MaxY();
			if (Y < y) Y = y; 
		}
		return Y;
	}
	
	public double MinY()
	{
		double Y = 1000;
		for (int i = 0; i<SoilLayers.size(); i++) 
		{
			double y = SoilLayers.get(i).MinY();
			if (Y > y) Y = y; 
		}
		return Y;
	}
	
	public double GetScale(Point2D.Double ScreenSize)
	{
		double DrawingWidth = MaxX() - MinX();
		double DrawingHeight = MaxY() - MinY();
		
		if ((DrawingHeight <= 0) || (DrawingWidth <= 0)) return 0;
		
		return Double.min(ScreenSize.x / (DrawingWidth), 
						  ScreenSize.y / (DrawingHeight));
	}
	
	public Point2D.Double PointNearby(double x, double y, double tolerance)
	{
		Point2D.Double pt = null;
		for (int i = 0; i<SoilLayers.size(); i++) 
		{
			pt = SoilLayers.get(i).PointNearby(x,  y, tolerance);
			if (pt != null) return pt;
		}
		return pt;
	}
	
	// Возвращает слой грунта, в границах которого находится точка Pt
	public SubgradeSoil SoilByPoint(Point2D.Double pt)
	{
		for (int i = 0; i<SoilLayers.size(); i++) 
		{
			if (SoilLayers.get(i).PointInside(pt)) return SoilLayers.get(i);
		}
		return null; 
	}
	
	public Line2D.Double LineNearby(double x, double y, double tolerance)
	{
		Line2D.Double line = null;
		for (int i = 0; i<SoilLayers.size(); i++) 
		{
			line = SoilLayers.get(i).LineNearby(x,  y, tolerance);
			if (line != null) return line;
		}
		return line;
	}
	
	public Point2D.Double SplitLineAt(Line2D.Double line, Point2D.Double pt)
	{
		Point2D.Double split_pt, NewPoint = null;
		for (int i = 0; i<SoilLayers.size(); i++) 
		{
			split_pt = SoilLayers.get(i).SplitAt(pt);
			if (NewPoint == null) NewPoint = split_pt;
		}
		return NewPoint;
	}
	
	public void MovePoint(Point2D.Double pt_from, Point2D.Double pt_to) 
	{
		for (int i = 0; i<SoilLayers.size(); i++) 
		{
			SoilLayers.get(i).MovePoint(pt_from, pt_to);
		}
	}
	
	public void DeleteLine(Line2D.Double line)
	{
		for (int i = 0; i<SoilLayers.size(); i++) 
		{
			if (SoilLayers.get(i).DeleteLine(line))
			{
				// После удаления точки или линии может получиться так,
				// что слой перестаёт существовать (т.е. если удаляем точку или линию из
				// слоя, состоящего из одной линии)
				if (SoilLayers.get(i).isEmpty()) SoilLayers.remove(i);
				return;
			}
		}
	}
	
	public SubgradeSoil FindSoilByLine(Line2D.Double line)
	{
		for (int i = 0; i < SoilLayers.size(); i++) 
		{
			if (SoilLayers.get(i).MyLine(line)) return SoilLayers.get(i);
		}
		return null;
	}
	
	public void DeletePoint(Point2D.Double pt)
	{
		for (int i = 0; i<SoilLayers.size(); i++) 
		{
			if (SoilLayers.get(i).DeletePoint(pt)) 
			{
				// После удаления точки или линии может получиться так,
				// что слой перестаёт существовать (т.е. если удаляем точку или линию из
				// слоя, состоящего из одной линии)
				if (SoilLayers.get(i).isEmpty()) SoilLayers.remove(i);
				return;
			}
		}
	}
	
	public void RefineLayers(Point2D.Double StartPoint)
	{
		// 1. Создадим узлы на пересечении линий
		RefinedLayers.clear();
				
		for (int i = 0; i < SoilLayers.size(); i++) 
		{
			// Взяв первый из слоёв SoilLayers.get(i),
			// берём каждый из остальных и для каждого их сегмента
			// выполняем проверку на пересечение с нашим
			for (int j = 0; j < SoilLayers.size(); j++) 
			{
				if (i == j) continue;
				
				PathIterator pathi = SoilLayers.get(j).GetPathIterator();
				Line2D.Double line = SoilLayers.get(j).GetNextSegment(pathi);
				while (line != null)
				{
					SoilLayers.get(i).CreatePointsAtIntersection(SoilLayers.get(j), line);
					line = SoilLayers.get(j).GetNextSegment(pathi);
				}
			}	
		}
		
		// 2. Выделим замкнутые контуры и реорганизуем слои грунта
		FindClosedPaths(StartPoint);
		
		// Заменяем существующие слои на улучшенные
		SoilLayers.clear();
		SoilLayers.addAll(RefinedLayers);
		for (SubgradeSoil soil: SoilLayers) soil.setSubgrade(this);
	}
	
	public Line2D.Double FindClosestLine(Point2D.Double pt, double angle, ArrayList<Line2D.Double> all_lines)
	{
		Line2D.Double ray = new Line2D.Double(pt.x, pt.y, pt.x + 10 * Math.cos(angle), pt.y + 10 * Math.sin(angle));
		ArrayList<Line2D.Double> i_lines = new ArrayList<Line2D.Double>();
		ArrayList<Point2D.Double> i_points = new ArrayList<Point2D.Double>();
		Line2D.Double line;
		Point2D.Double ipt;
		
		// Найдём все пересечения
		for (int i = 0; i < all_lines.size(); i++)
		{
			line = all_lines.get(i);
			ipt = SubgradeSoil.IntersectionPoint(line, ray, 0, 1, false);
 			if (ipt != null) 
 			{
 				i_lines.add(line);
 				i_points.add(ipt);
 			}
		}
		
		// Теперь среди них найдём ближайшее
		double min_dist = 1000;
		int min_index = -1;
		for (int i = 0; i < i_lines.size(); i++)
		{
			line = i_lines.get(i);
			ipt = i_points.get(i);
			double dist = Point2D.distance(ray.x1, ray.y1, ipt.x, ipt.y);
			// Если только точка pt не лежит на этой прямой
			if ((dist > 0.0001) && (dist <= min_dist))
			{	
				min_dist = dist;
				min_index = i;
			}
		}
		if (min_index > -1) return i_lines.get(min_index); else return null;
	}
	
	public ArrayList<Line2D.Double> CreateLinesArray()
	{
		ArrayList<Line2D.Double> lines = new ArrayList<Line2D.Double>();
		for (int i = 0; i < SoilLayers.size(); i++)
		{
			PathIterator pathi = SoilLayers.get(i).GetPathIterator();
			Line2D.Double line = SoilLayers.get(i).GetNextSegment(pathi);
			while (line != null)
			{
				AddLine(lines, line);
				line = SoilLayers.get(i).GetNextSegment(pathi);
			}
		}
		return lines;
	}
	
	// Применяет перестроенные слои грунта взамен исходных
	public void UseRefinedLayers()
	{
		SoilLayers.clear();
		SoilLayers.addAll(RefinedLayers);
	}
	
	public void FindClosedPaths(Point2D.Double StartPoint)
	{
		SearchPath.reset();
		// Алгоритм следующий: 
		// 1. Берём произвольную точку, если она не попадает в один из уже найденных контуров.
		// 2. Из точки под случайным углом проводим луч до пересечения с ближайшей линией
		// 3. Если пересечение не найдено - берём следующую точку.
		// 4. Если найдено - запоминаем эту линию и "отскакиваем" от неё под случайным углом - и снова в пункт 2
		// 5. Заканчиваем, когда: 
		//		5.1 у каждой из найденных линий начало и конец совпадают с точками, принадлежащими какой-либо другой 
		//		линии из найденных.
		// Когда один из контуров сформирован - запоминаем его. После переходим к п.1
		// Заканчиваем, когда: 
		
		// Зададим область поиска
		double size_x = MaxX();
		double size_y = MaxY();
		
		ArrayList<Line2D.Double> lines = CreateLinesArray();
		ArrayList<Line2D.Double> touched_lines1 = new ArrayList<Line2D.Double>();
		ArrayList<Line2D.Double> touched_lines2 = new ArrayList<Line2D.Double>();
		
		ArrayList<Line2D.Double> path = new ArrayList<Line2D.Double>();
		ArrayList<Line2D.Double> path1 = new ArrayList<Line2D.Double>(); 
		ArrayList<Line2D.Double> path2 = new ArrayList<Line2D.Double>();
		Point2D.Double pt = StartPoint;
		
		while ((touched_lines1.size() < lines.size()) && (touched_lines2.size() < lines.size()))
		{
			// Произвольная точка и угол	
			if (pt == null) while (InsideFoundPaths(pt))
								pt = new Point2D.Double(-5 + (size_x + 10) * Math.random(), -5 + (size_y + 10) * Math.random());
		
			path.clear();
			path1.clear();
			path2.clear();
			
			SearchPath.reset();
			SearchPath.moveTo(pt.x, pt.y);			
			
			double min_angle = 0, max_angle = 2 * Math.PI;
			
			while (!ClosedPath(path, path1, path2))
			{				
				double angle = min_angle + (max_angle - min_angle) * Math.random();
				if (angle < 0) angle = 2 * Math.PI + angle;
				
				Line2D.Double line = FindClosestLine(pt, angle, lines);
				// Если улетели в бесконечность - прерываемся
				if (line == null) 
				{ 
					SearchPath.lineTo(pt.x + size_x * Math.cos(angle), pt.y + size_y * Math.sin(angle));
					break;
				}
								
				// Луч
				Line2D.Double ray = new Line2D.Double(pt.x, pt.y, pt.x + 10*Math.cos(angle), pt.y + 10*Math.sin(angle));
				// Точка пересечения луча с линией
				pt = SubgradeSoil.IntersectionPoint(line, ray, 0, 1, false);
				// Уравнение линии и её угол наклона к горизонтали
				double Eq[] = SubgradeSoil.LineEquation(line);
				
				// Теперь определяем диапазон углов, расположенных по разные стороны от линии
				// Угол пересчитываем в диапазоне от -pi/2 до pi/2
				double line_angle = Math.atan(-Eq[0]/Eq[1]);			
		        // Определяем уравнение перпендикуляра к этой линии,
				// проходящего через точку начала луча
				
				// Найдём теперь точку пересечения перпендикуляра и линии
				Line2D.Double purpendicular = SubgradeSoil.Purpendicular(line_angle, (Point2D.Double)ray.getP1());
				Point2D.Double purple_point = SubgradeSoil.IntersectionPoint(line, purpendicular, -1, -1, false);
				// Пусть точка ray.P1 - это центр системы координат
				// Определим четверть плоскости, в которой расположена purple_point
				if (purple_point.x - ray.getX1() > 0)
				{
					// Справа
					AddLine(touched_lines1, line);
					AddLine(path1, line);
					if (purple_point.y - ray.getY1() >= 0)
					{
						// Выше - первая четверть
						min_angle = line_angle - Math.PI;
						max_angle = line_angle;
					} else
					{
						// Ниже - четвёртая четверть
						min_angle = line_angle;
						max_angle = line_angle + Math.PI;
					}
				} else
				{
					// Слева
					AddLine(touched_lines2, line);
					AddLine(path2, line);
					if (purple_point.y - ray.getY1() > 0)
					{
						// Выше - первая четверть
						min_angle = line_angle - Math.PI;
						max_angle = line_angle;
					} else
					{
						// Ниже - четвёртая четверть
						min_angle = line_angle;
						max_angle = line_angle + Math.PI;
					}
				}
									
				AddLine(path, line);						
				min_angle = min_angle + 0.01; max_angle = max_angle - 0.01;
				
				SearchPath.lineTo(pt.x, pt.y);
			}
			// Формируем слой грунта
			if (ClosedPath(path, path1, path2)) RefinedLayers.add(new SubgradeSoil(MakeLayer(path)));
			pt = null;
		}
	}
	
	// Если точка внутри уже обнаруженного замкнутого контура - возвращаем true
	public boolean InsideFoundPaths(Point2D.Double pt)
	{
		if (pt == null) return true;
		
		for (int i = 0; i < RefinedLayers.size(); i++)
		{
			if (RefinedLayers.get(i).PointInside(pt)) return true;
		}
		return false;
	}
	
	public static void AddLine(ArrayList<Line2D.Double> path, Line2D.Double line)
	{
		// Помещаем линии, стараясь их вставлять последовательно
		// Сначала проверим, нет ли линии с такими же координатами точек в начале или в конце
		// Если есть уже линия с точно такими же точками начала и конца - не вставляем линию
		
		for (int i = 0; i < path.size(); i++)
		{
			Line2D.Double line1 = path.get(i);
			Point2D.Double pt1_1 = (Point2D.Double)line.getP1();
			Point2D.Double pt1_2 = (Point2D.Double)line.getP2();
			Point2D.Double pt2_1 = (Point2D.Double)line1.getP1();
			Point2D.Double pt2_2 = (Point2D.Double)line1.getP2();
			
			if ((SubgradeSoil.TwinPoints(pt1_1, pt2_1) && SubgradeSoil.TwinPoints(pt1_2, pt2_2)) ||
				(SubgradeSoil.TwinPoints(pt1_1, pt2_2) && SubgradeSoil.TwinPoints(pt1_2, pt2_1))) return; 
		}
		path.add(line);
	}
		
	public static Line2D.Double GetNextLine(ArrayList<Line2D.Double> path, Point2D.Double pt)
	{
		Line2D.Double line;
		for (int i = 0; i < path.size(); i++)
		{
			line = path.get(i);
			Point2D.Double pt1 = (Point2D.Double)line.getP1();
			Point2D.Double pt2 = (Point2D.Double)line.getP2();
			if (SubgradeSoil.TwinPoints(pt1, pt) || SubgradeSoil.TwinPoints(pt2, pt)) return line; 
		}
		return null;
	}
	
	// Создадим слой грунта из замкнутого контура
	public static Path2D.Double MakeLayer(ArrayList<Line2D.Double> path)
	{
		// Поскольку в процессе поиска замкнутых контуров линии добавлялись в список "как придётся",
		// то нужно теперь их соединить в последовательные цепочки
		Path2D.Double new_path = new Path2D.Double();	
		
		// Теперь все остальные соединяем в цепочку
		int i = 0;
		Line2D.Double line1 = null; 
		Point2D.Double last_point = null;
		while (path.size() > 0)
		{
			// Если ещё не начинали замкнутый контур - берём первую линию
			if (line1 == null)
			{
				line1 = path.get(i);
				new_path.moveTo(line1.x1, line1.y1);
				new_path.lineTo(line1.x2, line1.y2);
				last_point = (Point2D.Double)line1.getP2();
			} else
			{
				// Если уже начинали то - двигаемся последней точке следующей линии,
				// соединённой с предыдущей. 
				Point2D.Double pt1 = (Point2D.Double)line1.getP1();
				Point2D.Double pt2 = (Point2D.Double)line1.getP2();
				// Если линии соединены через первую точку - двигаемся ко второй
				if (SubgradeSoil.TwinPoints(pt1, last_point)) 
				{
					new_path.lineTo(pt2.x, pt2.y); 
					last_point = pt2;
				} else
				{
					// Если через вторую - то к первой
					new_path.lineTo(pt1.x, pt1.y);
					last_point = pt1;
				}
			}
			// Учтённую линию из списка удаляем
			path.remove(line1);
			// Берём следующую линию, соединённую с данной через last_point
			line1 = GetNextLine(path, last_point);
		}
		return new_path;
	}
	
	
	// Проверка того, является ли путь замкнутым контуром
	public boolean ClosedPath(ArrayList<Line2D.Double> path, ArrayList<Line2D.Double> side1, ArrayList<Line2D.Double> side2)
	{
		// Для каждой линии контура должно найтись две линии,
		// начало или конец которых совпадают с этой точкой
		boolean pt1_done = false, pt2_done = false;
		Line2D.Double line1, line2;
		
		// Если линия зафиксирована с обеих сторон, то удаляем её - она точно не ограничивает 
		// замкнутый контур
		for (int i = 0; i < path.size(); i++)
		{
			line1 = path.get(i);
			if (side1.contains(line1) && side2.contains(line1)) 
			{
				path.remove(i);
				return ClosedPath(path, side1, side2);
			}
		}
		
		// Далее проверяем, все ли линии соединены с другими обоими концами
		for (int i = 0; i < path.size(); i++)
		{
			line1 = path.get(i);
			Point2D.Double pt1_1 = (Point2D.Double)line1.getP1();
			Point2D.Double pt1_2 = (Point2D.Double)line1.getP2();
			
			pt1_done = false; pt2_done = false;
			
			for (int j = 0; j < path.size(); j++)
			{
				if (j == i) continue;
				
				line2 = path.get(j);
				Point2D.Double pt2_1 = (Point2D.Double)line2.getP1();
				Point2D.Double pt2_2 = (Point2D.Double)line2.getP2();
						
				// Ищем совпадение для каждой точки в отдельности
				// Если нашли  - далее всё равно, сколько точек ещё с ней же совпадают 
				if (!pt1_done && (SubgradeSoil.TwinPoints(pt1_1, pt2_1) || SubgradeSoil.TwinPoints(pt1_1, pt2_2)))
				{
					pt1_done = true;
				}
				
				if (!pt2_done && (SubgradeSoil.TwinPoints(pt1_2, pt2_1) || SubgradeSoil.TwinPoints(pt1_2, pt2_2)))
				{
					pt2_done = true;
				}
				
				if (pt2_done && pt1_done) break;
			}
			// Если есть линии, не соединённые обоими концами с другими линиями -
			// пытаемся обойтись без них - удаляем из списка и снова проверяем на замкнутость
			if (!(pt2_done && pt1_done)) return false;
		}
		// Минимум три стороны у контура
		if (path.size() > 2)
		{
			Closed.clear();
			Closed.addAll(path);
			return true;
		} else return false;
	}
	
	public void Analyse()
	{
		// Расчёт коэффициента устойчивости
		// В процессе расчёта нужно найти положение критической поверхности сдвига.
		// Критическая - это та, для которой коэффициент устойчивости имееет наименьшее значение
		// 
		// Сначала ищем среди всех возможных кривых, проходящх через нижнюю точку откоса
		blocks.clear();
		Line2D.Double slope = SlopeLine();
		if (slope == null) return;
		double xmin = Math.min(slope.getX1(), slope.getX2());
		double xmax = Math.max(slope.getX1(), slope.getX2());
		double ymin = Math.max(slope.getY1(), slope.getY2());
		double ymax = Math.max(slope.getY1(), slope.getY2()) + Math.abs(slope.getY1() - slope.getY2());
		SoilBlock block1 = MethodTwo(xmin, xmax, ymin, ymax);
		//SoilBlock block1 = new SoilBlock(this, new Point2D.Double(60, 67), 46, -1);
		//block1.FactorOfSafety(block1.meshDirection);
		//SoilBlock block2 = new SoilBlock(this, block1.blockCenter, block1.blockRadius, block1.meshDirection);
		//block2.FactorOfSafety(block1.meshDirection);
		blocks.add(block1);
		//blocks.add(block2);
		/*SlopePoints.clear();
		SlopePoints.add(new Point2D.Double(11.37, 22.36));
		SlopePoints.add(new Point2D.Double(30.66, 9.50));
		SoilBlock block1 = new SoilBlock(this, new Point2D.Double(11.37+14, 22.36+2), 18, -1);
		
		SlopePoints.clear();
		SlopePoints.add(new Point2D.Double(56.85, 22.36));
		SlopePoints.add(new Point2D.Double(37.56, 9.50));
		SoilBlock block2 = new SoilBlock(this, new Point2D.Double(56.85-14, 22.36+2), 18, 1);
		
		double k1 = block1.FactorOfSafety(-1);
		double k2 = block2.FactorOfSafety(1);
		blocks.add(block1);
		blocks.add(block2);*/
	}
	
	public Line2D.Double SlopeLine()
	{
		if (SlopePoints.size() != 2) return null;
		// Ищем точку, с которой начнём поиск
		// Она находится на пересечении линии, отстоящей на 25
		Point2D.Double pt1 = SlopePoints.get(0);
		Point2D.Double pt2 = SlopePoints.get(1);
		Line2D.Double slopeMidLine;
		
		if (pt1.y < pt2.y) 
			 slopeMidLine = new Line2D.Double(pt1.x, pt1.y, pt2.x, pt2.y); 
		else slopeMidLine = new Line2D.Double(pt2.x, pt2.y, pt1.x, pt1.y);
		
		return slopeMidLine;
	}
		
	public static double LineAngle(Line2D.Double line)
	{
		return Math.atan((line.y2 - line.y1)/(line.x2 - line.x1));
	}
	
	public Rectangle2D.Double SubgradeBounds()
	{
		Area a = SubgradeArea();
		Rectangle r = a.getBounds();
		return new Rectangle2D.Double(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}
	
	public static Line2D.Double LowerBound(Rectangle2D.Double rect)
	{
		return new Line2D.Double(rect.x, rect.y, rect.x + rect.width, rect.y);
	}
	
	public SoilBlock MethodTwo(double xmin, double xmax, double ymin, double ymax)
	{
		// Это коэффициент устойчивости, зависящий от координат x, y центра кривой скольжения и её радиуса R
		// k[x][y][R]
		double k[][][] = new double[10][10][10];
		double x[] = new double[10];
		double y[] = new double[10];
		double R[][][] = new double[10][10][10];
		SoilBlock b[][][] = new SoilBlock[10][10][10];
		// На первом шаге выбираем область поиска экстремума
		// поскольку диапазон радиусов зависит от координат центра,
		// сначала задаём диапазон для координат
		for (int i = 0; i < 10; i++)
		{
			x[i] = xmin + (xmax - xmin) / 9 * i;
			y[i] = ymin + (ymax - ymin) / 9 * i;
 		}
		// Теперь определяем максимальное и минимальное расстояние от всех точек x, y
		// до линии откоса
		// Минимальный радиус - это расстояние от центра до нижней точки откоса
		// Максимальный радиус - это расстояние от центра до нижней стороны прямоугольника, описанного вокруг
		// всего земляного полотна
		Line2D.Double lower = LowerBound(SubgradeBounds());
		Line2D.Double slope = SlopeLine();
		int direction = (int)Math.signum(LineAngle(slope));
		for (int i = 0; i < 10; i++)
		{
			for (int j = 0; j < 10; j++)
			{
				Point2D.Double pt = new Point2D.Double(x[i], y[j]);	
				//double rmin = Line2D.ptLineDist(slope.x1, slope.y1, slope.x2, slope.y2, pt.x, pt.y);
				double rmin = Math.max(Point2D.distance(slope.x1, slope.y1, pt.x, pt.y),
									   Point2D.distance(slope.x2, slope.y2, pt.x, pt.y));
				double rmax = 0.98 * Line2D.ptLineDist(lower.x1, lower.y1, lower.x2, lower.y2, pt.x, pt.y);
				
				for (int r = 0; r < 10; r++)
				{
					if (rmax < rmin) 
					{
						k[i][j][r] = INVALID_INTERFACE;
						continue;
					}
					
					R[i][j][r] = rmin + (rmax - rmin) / 9 * r;
					// Теперь произведём расчёты для сочетания
					b[i][j][r] = new SoilBlock(this, pt, R[i][j][r], direction);
					if (!b[i][j][r].isEmpty())
					{
						k[i][j][r] = b[i][j][r].FactorOfSafety(direction);
					} else k[i][j][r] = INVALID_INTERFACE;
				}
			}
 		}
		
		// Теперь у нас есть значение коэффициента запаса для всех
		// сочетаний в данной области
		// найдём минимум. Определим, где расположен минимум - если на краю области, в том числе
		// в углу, сдвигаем область в соответствующем направлении - т.е. делаем ещё один расчёт в том же масштабе, но 
		// в других границах
		// Если минимум обнаружен внутри области - уменьшаем область поиска до области очерченной, вокруг этого минимума
		double kmin = INVALID_INTERFACE;
		double xk = 0, yk = 0, rk = 0;
		int xi = 0, yj = 0, rr = 0;
		
		for (int i = 0; i < 10; i++)
		//for (int i = 9; i > -1; i--)
		{
			for (int j = 0; j < 10; j++)
			{
				for (int r = 0; r < 10; r++)
				{
					if (kmin > k[i][j][r]) 
					{
						kmin = k[i][j][r];
						xk = x[i]; yk = y[j]; rk = R[i][j][r];
						xi = i; yj = j; rr = r;
					}
				}
			}
		}
		// Нашли минимум - определяем, что делать дальше
		// При этом, если область уже достаточно мала -
		// прекращаем поиск и возвращаем результат.
		if ((xmax - xmin) / SubgradeBounds().width < 0.05) return b[xi][yj][rr];
		double xmin_new = xmin, xmax_new = xmax, ymin_new = ymin, ymax_new = ymax;
		if (xi == 0) 
		{
			xmin_new = x[xi+1] - (xmax - xmin);
			xmax_new = x[xi+1];
		}
		if (xi == 9) 
		{
			xmax_new = x[xi-1] + (xmax - xmin);
			xmin_new = x[xi-1];
		}
		if (yj == 0) 
		{
			ymax_new = y[yj+1];
			ymin_new = y[yj+1] - (ymax - ymin);
		}
		if (yj == 9) 
		{
			ymax_new = y[yj-1] + (ymax - ymin);
			ymin_new = y[yj-1];
		}
		if ((yj < 9) && (yj > 0) && (xi < 9) && (xi > 0))
		{
			ymax_new = y[yj+1];
			ymin_new = y[yj-1];
			xmax_new = x[xi+1];
			xmin_new = x[xi-1];
		}
		// Рекурсия
		return MethodTwo(xmin_new, xmax_new, ymin_new, ymax_new);
	}
	
	public Area SubgradeArea()
	{
		Area a = new Area();
		for (int i = 0; i < SoilLayers.size(); i++)
		{
			a.add(SoilLayers.get(i).SoilArea());
		}
		return a;
	}
	
	public SubgradeLoad GetLoad(Line2D.Double line)
	{
		SubgradeLoad load = null;
		for (int i = 0; i < Loads.size(); i++)
		{
			if (Loads.get(i).ItsMe(line)) return Loads.get(i);
		}
		return load;
	}
	
	public SubgradeLoad AddLoad(SubgradeLoad subgradeLoad) 
	{		
		SubgradeLoad load = GetLoad(subgradeLoad.GetLine());
		if (load == null) 
		{
			Loads.add(subgradeLoad);
			return subgradeLoad;
		} else return load;
	}
	
	public Point2D.Double FindSlopePoint(Point2D.Double point)
	{
		for (int i = 0; i < SlopePoints.size(); i++)
		{
			Point2D.Double pt = SlopePoints.get(i);
			if (SubgradeSoil.TwinPoints(pt, point)) return pt;
		}
		return null;
	}
	
	public boolean AddSlopePoint(Point2D.Double selectedPoint) 
	{
		if ((FindSlopePoint(selectedPoint) != null) || (SlopePoints.size() == 2)) return false;
		SlopePoints.add(selectedPoint);
		return true;
	}
	
	public void RemoveSlopePoint(Point2D.Double selectedPoint) 
	{
		Point2D.Double pt = FindSlopePoint(selectedPoint);
		if (pt == null) return;
		SlopePoints.remove(pt);
	}

	public void Paint(Graphics G, Point2D.Double ScreenSize)
	{
		// Определим масштаб, исходя из размеров чертежа и экрана, на котором 
		// чертёж должен быть отображён
		double Scale = GetScale(ScreenSize);
		if (Scale == 0) return;
		
		double dx = MaxX() - MinX();
		double dy = MaxY() - MinY();
		
		Graphics2D g2d = (Graphics2D) G;
				
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setStroke(new BasicStroke((float) (1/Scale)));
		
		for (int i = 0; i < SoilLayers.size(); i++) SoilLayers.get(i).Paint(g2d, dx, dy);
		
		for (int i = 0; i < Loads.size(); i++) Loads.get(i).Paint(g2d, dx, dy);
		
		if (SlopePoints.size() == 2)
		{
			Point2D.Double pt1 = SlopePoints.get(0);
			Point2D.Double pt2 = SlopePoints.get(1);
			g2d.setColor(Color.GREEN);
			g2d.draw(new Line2D.Double(pt1.x, pt1.y, pt2.x, pt2.y));
		}
		
		for (int i = 0; i < blocks.size(); i++) blocks.get(i).Paint(g2d, dx, dy);
	}

	public void onGeometryChanged() 
	{
		blocks.clear();
		// Проверяем, не затронуты ли измененями нагрузки
		for (int i = Loads.size() - 1; i > -1; i--)
		{
			Line2D.Double load_line = Loads.get(i).GetLine();
			SubgradeSoil soil = FindSoilByLine(load_line);
			if (soil == null)
			{
				// TODO Если линия изменилась - мы не найдём нагрузку
				// Поэтому надо найти линию, которая совпадает с одной из точек
				Loads.remove(i);
			}
		}
	}
	
	void Load(Document xml_doc) 
	{
		// Загружаем
		SoilLayers.clear();
		SlopePoints.clear();
		Loads.clear();
		blocks.clear();
		
		Element doc_el = xml_doc.getDocumentElement();
		if (doc_el == null) return;
		for (int i = 0; i < doc_el.getChildNodes().getLength(); i++)
		{
			Node node = doc_el.getChildNodes().item(i);
			String node_name = node.getNodeName();
			if (node_name.contains("Layer"))
			{
				// Очертания слоёв грунта
				SubgradeSoil soil = new SubgradeSoil();
				soil.setSubgrade(this);
				soil.Load(node);
				SoilLayers.add(soil);
			}
			if (node_name.contains("Load"))
			{
				SubgradeLoad load = new SubgradeLoad(node);
				Loads.add(load);
			}
			if (node_name.contains("SlopePoints"))
			{
				double fromX = SubgradeSoil.myParseDouble(node.getAttributes().getNamedItem("FromX").getNodeValue());
				double fromY = SubgradeSoil.myParseDouble(node.getAttributes().getNamedItem("FromY").getNodeValue());
				double toX = SubgradeSoil.myParseDouble(node.getAttributes().getNamedItem("ToX").getNodeValue());
				double toY = SubgradeSoil.myParseDouble(node.getAttributes().getNamedItem("ToY").getNodeValue());

				SlopePoints.clear();
				SlopePoints.add(new Point2D.Double(fromX, fromY));
				SlopePoints.add(new Point2D.Double(toX, toY));
			}
		}	
	}
	
	public void Save(Document xml_doc)
	{
		// Документ должен быть инициализированным, но пустым
		Element doc_el = xml_doc.getDocumentElement();
		if (doc_el == null) 
		{ 
			doc_el = xml_doc.createElement("subgrade");
			xml_doc.appendChild(doc_el);
		}
		for (int i = 0; i < SoilLayers.size(); i++)
		{
			Element e = xml_doc.createElement(String.format("Layer%d", i));
			doc_el.appendChild(e);
			SoilLayers.get(i).toXML(e);
		}
		for (int i = 0; i < Loads.size(); i++)
		{
			Element e = xml_doc.createElement(String.format("Load%d", i));
			doc_el.appendChild(e);
			Loads.get(i).toXML(e);
		}
		if (SlopePoints.size() == 2)
		{
			Element e = xml_doc.createElement("SlopePoints");
			doc_el.appendChild(e);
			e.setAttribute("FromX", Double.toString(SlopePoints.get(0).getX()));
			e.setAttribute("FromY", Double.toString(SlopePoints.get(0).getY()));
			e.setAttribute("ToX", Double.toString(SlopePoints.get(1).getX()));
			e.setAttribute("ToY", Double.toString(SlopePoints.get(1).getY()));
		}
	}

	public void setPatternList(DefaultListModel<SubgradeSoilHatch> patternListModel) {
		patternList = patternListModel;
	}
	
	public BufferedImage getPattern(int index) 
	{
		if (patternList == null) return null;
		if (patternList.getSize() == 0) return null;
		
		SubgradeSoilHatch hatch = patternList.getElementAt(index);
		Image img = hatch.getImage();
		BufferedImage bimg = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_3BYTE_BGR);
		bimg.createGraphics().drawImage(img, 0, 0, img.getWidth(null), img.getHeight(null), null);
		return bimg; 
	}
}
