
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

public class SubgradeGrid {
	Path2D.Double grid = new Path2D.Double();
	
	public void paint(Graphics2D g, double max_x, double max_y, double scale, Point2D.Double ScreenSize)
	{
		if (scale == 0) return;
		
		double grid_step;
		double X = ScreenSize.x / scale;
		double Y = ScreenSize.y / scale;
		
		BasicStroke st = (BasicStroke)g.getStroke();
		
		// Шаг сетки - один метр
		grid_step = 1;
		// Сетка
		float hsbvals[] = new float[3];
		Color.RGBtoHSB(245, 245, 245, hsbvals);
		g.setColor(Color.getHSBColor(hsbvals[0], hsbvals[1], hsbvals[2]));
		g.setStroke(new BasicStroke(1 / (float) scale));
		double x = grid_step;
		while (x <= X)
		{
			g.draw(new Line2D.Double(x, 0, x, Y - (40 / scale)));
			x = x + grid_step;
		}
		double y = grid_step;
		while (y <= Y - (40 / scale))
		{
			g.draw(new Line2D.Double(0, y, X, y));
			y = y + grid_step;
		}
		// Подписи значений на осях
		g.setStroke(st);
	}
}
