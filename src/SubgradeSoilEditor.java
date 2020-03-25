
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

public abstract class SubgradeSoilEditor implements SoilEditor {
	protected final static int EM_NORMAL = 0, EM_ADD_LINE = 1, EM_DELETE_OBJECT = 2, 
	                 EM_COORDINATES = 3, EM_LOAD = 4, EM_SOIL_PROPS = 5, EM_SLOPE = 6;

	protected static final int EM_ADD_POINT = 7;
	
	public int editMode = EM_NORMAL;
	
	public SubgradeSoil selectedSoil;
	public Point2D.Double selectedPoint;
	public Line2D.Double selectedLine;
	
	public SubgradeSoil highlightedSoil;
	public Point2D.Double highlightedPoint;
	public Line2D.Double highlightedLine;
	
	public SubgradeSoil newLayer;
	
	public Point2D.Double startPoint, endPoint;
}
