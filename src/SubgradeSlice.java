
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class SubgradeSlice {
	
	private ArrayList<SubgradeSoil> Layers;
	private Area SliceArea;
	private ArrayList<Line2D.Double> all_lines; 
	private double Load = 0;
	private Subgrade sliceSubgrade;
	
	public SubgradeSlice(Subgrade subgrade, Area slice, double left_x, double right_x)
	{
		Layers = new ArrayList<SubgradeSoil>(); 
		all_lines = new ArrayList<Line2D.Double>();
		sliceSubgrade = subgrade;
		// ���� ����������� �������� slice, ���� ������� � subgrade, � ����� ��������������
		Rectangle2D.Double rect = (Rectangle2D.Double)slice.getBounds2D();
		// ��������� ������������, ���������� ������ ��������������� �������������� 
		// ��������� ��������� ����������
		rect.setRect(left_x, rect.y, right_x - left_x, rect.height);
		// ������ �������� �� ��������� ��������� �������, ������� ����� � ������ ��������� ���������� 
		// ������ ��� ���������������, ����� ��������� ������������ ������, � ������ - ������������ ����������
		Area slice_area = (Area)slice.clone();
		slice_area.intersect(new Area(rect));
		SliceArea = (Area) slice_area.clone();
		// ������ ���� ���������� - �� �������:
		for (int i = 0; i < subgrade.Layers().size(); i++)
		{
			SubgradeSoil soil = subgrade.Layers().get(i);
			Area soil_area = soil.SoilArea();
			Area soil_fragment = (Area)slice_area.clone();
			soil_fragment.intersect(soil_area);
			// ����� ����������, ��� ������� ����������� 
			// ���� ������ � ���������� ������� �� ���������� ��������� ��������� ��������
			// �� ���� ���������� �� �����������
			ArrayList<Area> fragments = SplitArea(soil_fragment);  
			for (int j = 0; j < fragments.size(); j++)
			{
				Area fragment = fragments.get(j);
				if (fragment.isEmpty()) continue;
				
				// ������� �� ��������� ��������� -
				// ������ ����� ��������� � ���� ������ "��� ����",
				// � ������������� �������, ������������ ����������� ������ -
				// ���������� � �������������, ����� ���� ������ ������������ ����.
				PathIterator path = fragment.getPathIterator(null);
				ArrayList<Point2D.Double> pts = SoilBlock.GetNextSegment(path, fragment); 
				ArrayList<Line2D.Double> layer_lines = new ArrayList<Line2D.Double>();
				while (pts.size() > 0)
				{
					Point2D.Double pt1 = new Point2D.Double(pts.get(0).x, pts.get(0).y);
					Point2D.Double pt2 = new Point2D.Double(pts.get(pts.size()-1).x, pts.get(pts.size()-1).y);
					if (!SubgradeSoil.TwinPoints(pt1, pt2))
						layer_lines.add(new Line2D.Double(pt1, pt2));
					pts = SoilBlock.GetNextSegment(path, fragment);
				}
				// ��������� �������� ���� �����, �������� ����� ��� ��������
				for (int k = 0; k < layer_lines.size(); k++) Subgrade.AddLine(all_lines, layer_lines.get(k));
				
				// ������ �� ������� ���� ������
				if (layer_lines.size() > 2)
				{
					SubgradeSoil Layer = new SubgradeSoil(Subgrade.MakeLayer(layer_lines));
					// �������� �������� ������
					Layer.setC(soil.getC());
					Layer.setF(soil.getF());
					Layer.setG(soil.getG());
					Layers.add(Layer);
				}
			}
		}
		CalculateLoad();
	}
	
	private void CalculateLoad()
	{
		Load = 0;
		for (int i = 0; i < all_lines.size(); i++)
		{
			SubgradeLoad sl = sliceSubgrade.GetLoad(all_lines.get(i));
			if (sl != null) Load = Load + sl.GetLoad();
		}
	}
	
	public static boolean ContainsPoint(ArrayList<Point2D.Double> list, Point2D.Double pt)
	{
		for (int i = 0; i < list.size(); i++)
		{
			Point2D.Double l = list.get(i);
			if (SubgradeSoil.TwinPoints(l, pt)) return true;
		}
		return false;
	}
	
	// ������ ���� ������, �� �������� �������� ���������� ������
	public SubgradeSoil FindLowerLayer()
	{
		SubgradeSoil soil = null;
		ArrayList<Point2D.Double> curve = SoilBlock.FindCurvedSegment(SliceArea.getPathIterator(null), SliceArea);
		for (int i = 0; i < Layers.size(); i++)
		{
			soil = Layers.get(i);
			ArrayList<Point2D.Double> layer_pts = soil.LayerPoints();
			// ���� ��� ����� �������������� �������� ����������� ����, �� ��� ������ ����
			if (ContainsPoint(layer_pts, curve.get(0)) && 
				ContainsPoint(layer_pts, curve.get(curve.size()-1))) return soil;
		}
		return soil;
	}
	
	// �������� ���� ��������� �� ����������� � ����������� ������
	public double TangentForce()
	{
		return Weight() * Math.sin(BaseAngle());
	}
	
	// ���� ������� ����������� ������
	public double BaseAngle()
	{
		Line2D.Double base = BaseLine();
		if (base != null)
		{
			return Math.atan((base.y2 - base.y1)/(base.x2 - base.x1)); 
		} else 
		{
			return 0;
		}
	}
	
	// �������� ���� ��������� �� ����������� � ����������� ������
	public double NormalForce()
	{
		return Weight() * Math.cos(BaseAngle());
	}
	
	// ���� �������������, ������������� ���������� �������
	public double Friction()
	{
		SubgradeSoil soil = FindLowerLayer();
		if (soil != null)
		{
			return NormalForce() * Math.tan(soil.getF());
		} else
		{
			return 0;
		}
	}
	
	// ����� ���������, ������� �� ����������� ������
	public Line2D.Double BaseLine()
	{
		// ���� �������, ������� ��������� � ����������� ������
		PathIterator p = SliceArea.getPathIterator(null);
		ArrayList<Point2D.Double> pts = SoilBlock.FindCurvedSegment(p, SliceArea);
		if (pts != null) 
		{ 
			return new Line2D.Double(pts.get(0), pts.get(pts.size()-1)); 
		}
		else 
		{ 
			return null;
		}
	}
	
	// ���� ������������� ������, �������������� �������� ����������
	public double CohesionForce()
	{
		// ���� �������, ������� ��������� � ����������� ������
		Line2D.Double base = BaseLine();
		SubgradeSoil soil = FindLowerLayer();
		if ((base != null) && (soil != null))
		{
			return Point2D.distance(base.x1, base.y1, base.x2, base.y2) * soil.getC();
		} return 0;
	}
	
	// ��� ���������
	public double Weight()
	{
		// ����� ���������� ��� ���������
		// ��������� ������� ������� ���� ������
		// ����� ���� �������� � �� �������� ���
		// ���������� ���������� �������� ��� ��������� ����������
		double W = 0;
		for (int i = 0; i < Layers.size(); i++)
		{
			// ������ ���� ������ � ������ ��������� ����� ����� ���� ��������, ����
			// ������������. � ����� ������ ���� ������ = ������ ���������
			double A = Layers.get(i).Area();
			W = W + A * Layers.get(i).getG();
		}
		return W + Load * getWidth();
	}
	
	public double getWidth()
	{
		return SliceArea.getBounds2D().getWidth();
	}
	
	public double FrictionAngle()
	{
		SubgradeSoil soil = FindLowerLayer();
		if (soil != null) return soil.getF(); else return 0;
	}
	
	public double tau()
	{
		return Math.cos(FrictionAngle())/Math.cos(Math.abs(BaseAngle())-FrictionAngle());
	}
	
	// ���������� ����, ����������� �� ��������
	public double SlideForces(int direction)
	{
		// ���������� ���� ����. ��� �������� ���� ����� �� ����������� �
		// ����������� ������
		// � direction - ����������� ���������������� ������
		// ���� -1 - �� ������ ������� �������, ���� +1 - �� �� �������.
		double T = TangentForce();
		if (Math.signum(T * direction) < 0) T = 0; 
		return Math.abs(T);
	}
	
	// ������������ ����
	public double HoldForces(int direction)
	{
		// �������� ������������ �� ������ �������� ����������, ���������� ������� 
		// � ����������� �����, ���� ��� �������� �� ����������� ���������� ���
		// ��������� � �����������, ��������������� ������.
		// � direction - ����������� ���������������� ������
		// ���� -1 - �� ������ ������� �������, ���� +1 - �� �� �������.
		double T = TangentForce();
		if (Math.signum(T * direction) > 0) T = 0; 
		double F = CohesionForce() + Friction() + Math.abs(T);
		return F;// / tau();
	}
	
	public static ArrayList<Area> SplitArea(Area A)
	{
		ArrayList<Area> result = new ArrayList<Area>();
		if (!A.isSingular()) 
		{
			PathIterator p = A.getPathIterator(null);
			Path2D.Double new_path = new Path2D.Double();
			
			double c[] = new double[6];
			while (!p.isDone())
			{
				Point2D.Double pt = (Point2D.Double)new_path.getCurrentPoint(); 
				switch (p.currentSegment(c))
				{
					case PathIterator.SEG_MOVETO: { 
						if (pt != null) 
						{	
							result.add(new Area(new_path));
							new_path = new Path2D.Double();
						};
						new_path.moveTo(c[0], c[1]);
						break; 
					}
					case PathIterator.SEG_LINETO: new_path.lineTo(c[0], c[1]); break;
					case PathIterator.SEG_QUADTO: new_path.quadTo(c[0], c[1], c[2], c[3]); break;
					case PathIterator.SEG_CUBICTO: new_path.curveTo(c[0], c[1], c[2], c[3], c[4], c[5]); break;
					case PathIterator.SEG_CLOSE: 
						{
							new_path.closePath();
							//result.add(new Area(new_path));
							break;
						}
				}
				p.next();
			}
		} else result.add(A);
		return result;
	}
	
	public void Paint(Graphics2D g2d, double dx, double dy)
	{
		for (int i = 0; i < Layers.size(); i++)
			Layers.get(i).Paint(g2d, false, dx, dy);
	}
	
	public Point2D.Double getSliceCenter()
	{
		return new Point2D.Double(SliceArea.getBounds2D().getCenterX(), SliceArea.getBounds2D().getCenterY());
	}

	public double getArea() {
		double W = 0;
		for (SubgradeSoil soil: Layers) W = W + soil.Area();
		return W;
	}
}
