
import java.awt.geom.Point2D;

import javax.swing.DefaultListModel;

public class SlopePointsListModel extends DefaultListModel<String> {
	private static final long serialVersionUID = 1L;
	
	private Subgrade subgrade;
	
	public void setSubgrade(Subgrade sub)
	{
		subgrade = sub;
		for (int i = 0; i < subgrade.SlopePoints.size(); i++) addElement(GetPointStringRepresentation(i));
		if (subgrade.SlopePoints.size() == 0) fireContentsChanged(this, -1, -1);
	}
	
	@Override
	public int getSize() {
		if (subgrade != null)
		{
			return subgrade.SlopePoints.size();
		} else return 0;
	};
	

	@Override
	public String getElementAt(int index) {
		if (subgrade == null) return "";
		if (index >= subgrade.SlopePoints.size()) return "";
		return GetPointStringRepresentation(index);
	};
	
	private String GetPointStringRepresentation(int index) {
		// TODO Auto-generated method stub
		return String.format("X: %1.02f, Y: %2.02f", subgrade.SlopePoints.get(index).x, subgrade.SlopePoints.get(index).y);
	}

	public Point2D.Double getPointElement(int index) {
		if (subgrade == null) return null;
		if (index >= subgrade.SlopePoints.size()) return null;
		return subgrade.SlopePoints.get(index);			
	};
	
	public void addElement(Point2D.Double element) {
		if (subgrade == null) return;
		if (subgrade.AddSlopePoint(element))
		{
			addElement(GetPointStringRepresentation(subgrade.SlopePoints.size()-1));
		}
	};
	
	public void removePointElementAt(int index) {
		if (subgrade == null) return;
		subgrade.RemoveSlopePoint(getPointElement(index)); 
		removeElementAt(index);
	}

}
