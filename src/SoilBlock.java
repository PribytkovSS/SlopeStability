
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

// Блок грунта, устойчивость которого оценивается коэффициентом
public class SoilBlock {

	private Area blockArea = null;
	protected Point2D.Double blockCenter;
	protected double blockRadius;
	private Subgrade blockSubgrade;
	private Rectangle2D.Double blockBoundingRect;
	private ArrayList<SubgradeSlice> Slices;
	private double preferredSliceWidth = 1; 
	double safetyFactorCW = 9999;
	double safetyFactorCCW = 9999;
	protected int meshDirection;
	
	// Создаётся блок, очерченный круглоцилиндрической поверхностью
	// с центром в center и радиусом radius
	// данные о грунтах и геометрии земляного полотна содержит subgrade
	public SoilBlock(Subgrade subgrade, Point2D.Double center, double radius, int direction)
	{
		// 1. Создаём блок грунта, ограниченный контуром земляного полотна и 
		// окружностью
		// 2. Разбиваем этот блок на фрагменты
		blockCenter = center;
		blockRadius = radius;
		blockSubgrade = subgrade;
		meshDirection = direction;
		
		Area Cylinder = new Area(new Ellipse2D.Double(center.x - radius, center.y - radius, 2*radius, 2*radius));
		blockArea = Cylinder;
		blockArea.intersect(subgrade.SubgradeArea());
		// Если получилось несколько отдельных областей, выбираем только ту, 
		// что пересекается с контуром, ограниченным поверхностью откоса и 
		// прямой между двумя указанными пользователем точками интересующего его откоса
		ArrayList<Area> block_areas = SubgradeSlice.SplitArea(blockArea);
		if (block_areas.size() > 1)
		{
			int i = FindSlopeArea(block_areas);
			if (i > -1) blockArea = block_areas.get(i);
		}
		blockBoundingRect = (Rectangle2D.Double)blockArea.getBounds2D();
		Slices = new ArrayList<SubgradeSlice>();
		// Проверяем, достаточно ли большим является блок - если нет, 
		// то множим его на ноль и не делим на фрагменты
		// Также множим на ноль блок, если центр лежит внутри него
		if ((blockBoundingRect.width / subgrade.SubgradeBounds().width >= 0.02) && (!blockArea.contains(center)))
		{
			CreateSlices();
		} else blockArea.reset();
	}
	
	public boolean isEmpty()
	{
		return blockArea.isEmpty();
	}
	
	public ArrayList<SubgradeSlice> getSlices()
	{
		return Slices;
	};
	
	public int FindSlopeArea(ArrayList<Area> areas)
	{
		// Берём линию между двумя укзанными пользователем "точками склона"
		// поворачиваем её вокруг нижней точки до тех пор, пока она не пересечётся с 
		// с первой попавшейся областью из areas. Её и оставляем, а остальные удаляем
		Point2D.Double pt1 = blockSubgrade.SlopePoints.get(0);
		Point2D.Double pt2 = blockSubgrade.SlopePoints.get(1);
		Line2D.Double slope_line;
		// Начальной точкой будет та, которая ниже
		if (pt1.y < pt2.y) slope_line = new Line2D.Double(pt1, pt2); else slope_line = new Line2D.Double(pt2, pt1);
		double angle = Math.atan((slope_line.y2 - slope_line.y1)/(slope_line.x2 - slope_line.x1));
		double angle1 = angle - Math.signum(angle) * 0.02;
		double R = Point2D.distance(pt1.x, pt1.y, pt2.x, pt2.y);
		// Если угол отрицательный - вращаем по часовой (т.е. отнимаем от угла)
		// если положительный - против (т.е прибавляем к углу)
		while (Math.abs(angle1) < Math.PI/2)
		{
			Path2D.Double triangle = new Path2D.Double();
			double dx = R * (1/Math.tan(angle1) - 1/Math.tan(angle));
			double dy = R * (Math.tan(angle1) - Math.tan(angle));
			triangle.moveTo(slope_line.x1, slope_line.y1);
			triangle.lineTo(slope_line.x2, slope_line.y2);
			triangle.lineTo(slope_line.x2 + dx, slope_line.y2 + dy);
			triangle.closePath();
			for (int i = 0; i < areas.size(); i++)
			{
				Area t_area = new Area(triangle);
				t_area.intersect(areas.get(i));
				if (!t_area.isEmpty()) 
					return i;
			}
			angle1 = angle1 + Math.signum(angle) * 0.02;
		};
		return -1;
	}

	public Area BlockArea()
	{
		return blockArea;
	}
	
	public static ArrayList<Point2D.Double> GetNextSegment(PathIterator pathi, Area A) 
	{
		if (pathi == null) return null;
		if (pathi.isDone()) return null;
		
		ArrayList<Point2D.Double> result = new ArrayList<Point2D.Double>();
		
		double coords[] = new double[6];
		
		Point2D.Double pt1;
		switch (pathi.currentSegment(coords))
		{
			case PathIterator.SEG_QUADTO: pt1 = new Point2D.Double(coords[2], coords[3]); break;
			case PathIterator.SEG_CUBICTO: pt1 = new Point2D.Double(coords[4], coords[5]); break;
			default: pt1 = new Point2D.Double(coords[0], coords[1]); 
		}
		Point2D.Double pt2 = null;
		pathi.next();
		if (!pathi.isDone())
		{
			switch (pathi.currentSegment(coords))
			{
				case PathIterator.SEG_CLOSE: {
					PathIterator p = A.getPathIterator(null);
					p.currentSegment(coords);
					pt2 = new Point2D.Double(coords[0], coords[1]);
					result.add(pt1);
					result.add(pt2);
				  } break;
				case PathIterator.SEG_LINETO: {
					result.add(pt1); 
					result.add(new Point2D.Double(coords[0], coords[1])); 
			  	  } break;
				case PathIterator.SEG_QUADTO: {
					result.add(pt1); 
					result.add(new Point2D.Double(coords[0], coords[1]));
					result.add(new Point2D.Double(coords[2], coords[3]));
				  } break;
				case PathIterator.SEG_CUBICTO: {
					result.add(pt1); 
					result.add(new Point2D.Double(coords[0], coords[1]));
					result.add(new Point2D.Double(coords[2], coords[3]));
					result.add(new Point2D.Double(coords[4], coords[5]));
				  } break;
			}
		};
		return result;
	}
	
	public static ArrayList<Point2D.Double> FindCurvedSegment(PathIterator pathi, Area A)
	{
		ArrayList<Point2D.Double> result = GetNextSegment(pathi, A);
		if (result == null) return null;
		
		while ((result.size() < 3) && (result.size() > 0))
		{
			result = GetNextSegment(pathi, A);
		}
		if (result.size() >= 3) return result; else return null;
	}
	
	private ArrayList<Point2D.Double> getPointsInside(Area slice)
	{
		ArrayList<Point2D.Double> points = new ArrayList<Point2D.Double>();
		for (int i = 0; i < blockSubgrade.Layers().size(); i++)
		{
			SubgradeSoil soil = blockSubgrade.Layers().get(i);
			ArrayList<Point2D.Double> layer_points = soil.LayerPoints();
			// Удаляем из points те, что не лежат внутри заданной области
			for (int j = layer_points.size()-1; j >= 0; j--)
			{
				if (!slice.contains(layer_points.get(j))) layer_points.remove(j); 
			}
			points.addAll(layer_points);
		}
		return points;
	}
	
	public void SortPointsByX(ArrayList<Point2D.Double> points)
	{
		if (points.size() == 0) return;
		
		SubgradePointsComparator com = new SubgradePointsComparator();
		if (meshDirection == -1) 
		{
		   	   points.sort(com.reversed());
		} else points.sort(com);
		// Теперь удалим дубликаты
		int i = 0;
		while (i < points.size()-1)
		{
			if (points.get(i).x == points.get(i+1).x) points.remove(i+1); else i++;
		}
	}
	
	private void RefineMesh(ArrayList<Point2D.Double> points)
	{
		if (points.size() == 0) return;

		for (int i = 0; i < points.size()-1; i++)
		{
			Point2D.Double pt1 = points.get(i);
			Point2D.Double pt2 = points.get(i+1);
			if (Math.abs(pt2.x - pt1.x) > 1.5 * preferredSliceWidth)
			{
				// Делим список на две части
				// в первой - все элементы до i, включая его
				ArrayList<Point2D.Double> pts1 = new ArrayList<Point2D.Double>();
				for (int j = 0; j <= i; j++) pts1.add(points.get(j));
				// Во второй - все после i
				ArrayList<Point2D.Double> pts2 = new ArrayList<Point2D.Double>();
				for (int j = i+1; j < points.size(); j++) pts2.add(points.get(j));
				
				// Между ними вставляем новый элемент
				pts1.add(new Point2D.Double(pt1.x + preferredSliceWidth * meshDirection, pt1.y));
				points.clear();
				// Склеиваем
				points.addAll(pts1);
				points.addAll(pts2);
				// И снова отправляем на разбивку
				RefineMesh(points);
			}
		}
	}
	
	public static ArrayList<Point2D.Double> PathPoints(PathIterator path)
	{
		ArrayList<Point2D.Double> result = new ArrayList<Point2D.Double>();
		
		if (path != null) 
		{	
			double c[] = new double[6];
			while (!path.isDone())
			{
				switch (path.currentSegment(c))
				{
					case PathIterator.SEG_MOVETO: 
						case PathIterator.SEG_LINETO: result.add(new Point2D.Double(c[0], c[1])); break; 
			
					case PathIterator.SEG_CUBICTO: result.add(new Point2D.Double(c[4], c[5])); break; 
			
					case PathIterator.SEG_QUADTO: result.add(new Point2D.Double(c[2], c[3])); break; 
				};
				path.next();
			}
		}
		return result;
	}
	
	// Ищем все точки пересечения кривой третьего порядка, заданной точками cubic_curve
	// и сегментами слоёв грунта
	public ArrayList<Point2D.Double> IntersectionPoints(Area slice)
	{
		ArrayList<Point2D.Double> points = new ArrayList<Point2D.Double>();
		for (int i = 0; i < blockSubgrade.Layers().size(); i++)
		{
			SubgradeSoil soil = blockSubgrade.Layers().get(i);
			Area a = soil.SoilArea();
			a.intersect(slice);
			if (!a.isEmpty())
			{
				ArrayList<Point2D.Double> curve_pts = FindCurvedSegment(a.getPathIterator(null), a);
				if (curve_pts == null) continue;
				
				if (curve_pts.size() > 0)
				{
					points.add(curve_pts.get(0));
					points.add(curve_pts.get(curve_pts.size()-1));
				}
			}
		}
		return points;
	}
	
	// Разбивка блока грунта на отдельные фрагменты
	private void CreateSlices()
	{
		PathIterator my_path = blockArea.getPathIterator(null);
		Slices.clear();
		
		Point2D.Double pt1, pt2;
		ArrayList<Point2D.Double> points = new ArrayList<Point2D.Double>(); 
		ArrayList<Area> slices = new ArrayList<Area>();
		
		for (int i = 0; i < blockSubgrade.Layers().size(); i++)
		{
			SubgradeSoil soil = blockSubgrade.Layers().get(i);
			Area soil_area = (Area)soil.SoilArea().clone();
			soil_area.intersect(blockArea);
			// В каждом из блоков ищем криволинейный сегмент
			// - это поверхность скольжения.
			// Координаты её начала и конца в пределах фрагмента
			// нас интересуют в первую очередь
			ArrayList<Point2D.Double> curved_seg = FindCurvedSegment(my_path, blockArea);
			// Возможно, криволинейный сегмент не один. 	
			while (curved_seg != null)
			{
				// Формируем прямоугольник вокруг этого фрагмента
				pt1 = curved_seg.get(0);
				pt2 = curved_seg.get(curved_seg.size()-1);
				
				Point2D.Double lower_left = new Point2D.Double(Math.min(pt1.x, pt2.x), blockBoundingRect.getY()); 
				double h = blockBoundingRect.height;
				double w = Math.abs(pt1.x - pt2.x);
				
				Rectangle2D.Double r = new Rectangle2D.Double(lower_left.x, lower_left.y, w, h);
				// Находим точки геометрии земполотна и слоёв грунта, которые лежат внутри этого прямоугольника
				Area slice = new Area(r);
				slice.intersect(blockArea);
				points.addAll(getPointsInside(slice));
				// Добавляем также все точки контура slice
				points.addAll(PathPoints(slice.getPathIterator(null)));
				// А также точки пересечения криволинейного сегмента с
				// контурами слоёв грунтов
				points.addAll(IntersectionPoints(slice));
				slices.add(slice);
				my_path.next();
				curved_seg = FindCurvedSegment(my_path, blockArea);
			}
		}
		
		// Теперь сортируем точки по возрастанию координаты x - 
		// это будут координаты вертикальных границ будущих фрагментов
		SortPointsByX(points);
		// Таким образом, теперь у нас есть 
		// область slice. Эту область нам нужно теперь "нарезать" на ещё более мелкие
		// части по координатам, что хранятся в points
		if (points.size() > 1)
		{
			Area slice = new Area();
			for (Area sl: slices) slice.add(sl);
			
			preferredSliceWidth = Math.max(slice.getBounds2D().getWidth() / 10, preferredSliceWidth);
			RefineMesh(points);
			
			for (int j = 0; j < points.size()-1; j++)
			{
				pt1 = points.get(j);
				pt2 = points.get(j+1);
				if (!SubgradeSoil.TwinPoints(new Point2D.Double(pt1.x, pt1.y), new Point2D.Double(pt2.x, pt1.y)))
				{
					SubgradeSlice new_slice = new SubgradeSlice(blockSubgrade, slice, Math.min(pt1.x, pt2.x), Math.max(pt1.x, pt2.x));
					if (new_slice.getArea() >= 0.01)
							Slices.add(new_slice);
				}
			}
		}
	}
	
	public double FactorOfSafety(int direction)
	{
		// Вычисляем отношение момента удерживающих сил
		// к моменту сдвигающих
		double H = 0, S = 0, sf = 9999;
		
		for (int i = 0; i < Slices.size(); i++)
		{
			H = H + Slices.get(i).HoldForces(direction);
			S = S + Slices.get(i).SlideForces(direction);
		}
		if (S != 0)	sf = H / S; else sf = 9999;
		
		switch (direction)
		{
			case -1: safetyFactorCCW = sf;
			case 1: safetyFactorCW = sf;
		}
		return sf; 
	}
	
	public void Paint(Graphics2D g2d, double dx, double dy)
	{
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f));
		g2d.setPaint(Color.BLUE);
		g2d.fill(BlockArea()); 
		
		// Отрисуем вертикальные границы Slices
		for (int i = 0; i < Slices.size(); i++) 
				Slices.get(i).Paint(g2d, dx, dy);
		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
		
		g2d.setPaint(Color.BLACK);
		String s = String.format("k = %.02f", Math.min(safetyFactorCW,  safetyFactorCCW));
		String s1 = String.format("R = %2.02f", blockRadius);
		String s2 = String.format("x, y = (%1.02f, %2.02f)", blockCenter.x, blockCenter.y);
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		
		AffineTransform at = g2d.getTransform();
		AffineTransform new_y_scale = AffineTransform.getScaleInstance(1, 1); //AffineTransform.getScaleInstance(at.getScaleX(), -at.getScaleY());
		AffineTransform new_y_trans = AffineTransform.getTranslateInstance(0, 0); //AffineTransform.getTranslateInstance(at.getTranslateX(), 0);
		g2d.setTransform(new_y_trans);
		g2d.transform(new_y_scale);
		
		// Центр блока 
		int trans_y = (int)Math.round(at.getTranslateY());
		int x_scale = (int)Math.round(at.getScaleX());
		int x = (int)Math.round(blockArea.getBounds2D().getCenterX() * at.getScaleX());
		int y = (int)Math.round(trans_y + blockArea.getBounds2D().getMaxY() * at.getScaleY());
		g2d.setFont(new Font("Times", Font.BOLD, 1 * x_scale));
		
		if (meshDirection == 1) x = x - 10 * x_scale; 
		
		g2d.drawString(s, x, y);
		g2d.drawString(s1, x, y + 1 * x_scale);
		g2d.drawString(s2, x, y + 2 * x_scale);
		
		// Отрисуем нормера сегментов
		g2d.setFont(new Font("Times", Font.BOLD, 1 * Math.max(x_scale, 0)));
		g2d.setColor(Color.RED);
		for (int i = 0; i < Slices.size(); i++) 
		{
			Point2D.Double slice_center = Slices.get(i).getSliceCenter();
			slice_center.x = slice_center.x * at.getScaleX();
			slice_center.y = trans_y + (slice_center.y) * at.getScaleY();
			
			//DrawOutlinedText(g2d, Color.ORANGE, Color.BLACK, Integer.toString(i+1), 
			//				(int)Math.round(slice_center.x), 
			//				(int)Math.round(slice_center.y));
			g2d.drawString(Integer.toString(i+1), (int)Math.round(slice_center.x), (int)Math.round(slice_center.y));
		}
		
		g2d.setTransform(at);
	}

	private void DrawOutlinedText(Graphics2D g2d, Color fillColor, Color outlineColor, String s, int x, int y) 
	{
		Color originalColor = g2d.getColor();
        Stroke originalStroke = g2d.getStroke();
        RenderingHints originalHints = g2d.getRenderingHints();

        // create a glyph vector from your text
        GlyphVector glyphVector = g2d.getFont().createGlyphVector(g2d.getFontRenderContext(), s);
        // get the shape object
        Shape textShape = glyphVector.getOutline();
        textShape = AffineTransform.getTranslateInstance(x, y).createTransformedShape(textShape);

        // activate anti aliasing for text rendering (if you want it to look nice)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        
        g2d.setColor(outlineColor);
        g2d.setStroke(new BasicStroke(1f));
        g2d.draw(textShape); // draw outline
        
        g2d.setColor(fillColor);
        g2d.fill(textShape); // fill the shape
        
        // reset to original settings after painting
        g2d.setColor(originalColor);
        g2d.setStroke(originalStroke);
        g2d.setRenderingHints(originalHints);
	}
}
