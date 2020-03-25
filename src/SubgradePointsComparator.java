
import java.awt.geom.Point2D;
import java.util.Comparator;

public class SubgradePointsComparator implements Comparator<Point2D.Double> {
	
	public int compare(Point2D.Double pt1, Point2D.Double pt2) 
	{
	   if (pt1.x > pt2.x) return 1; 
	   else if (pt1.x < pt2.x) return -1;
	   	    else return 0;
	}
}
