
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public interface SoilEditor {
	
	//public void setSubgrade(Subgrade subgrade);
	
	public void PointSelected(Point2D.Double pt);
	public void LineSelected(Line2D.Double line);
	public void SoilSelected(SubgradeSoil soil);
	public int getMode();
	public void setMode(int mode);
	
	public SubgradeSoil getSoil();
	public Point2D.Double getPoint();
	public Line2D.Double getLine();
	
	public void MouseOver(Point2D.Double pt);
	public void MouseClicked(Point2D.Double pt, int button);
	public void KeyPressed(int key);
	
	public void onPointChanged();
	public void onSoilChanged();
	public void onLineChanged();
	
	public void Paint(Graphics2D g2d, double dx, double dy);
}
