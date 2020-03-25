
import java.awt.Image;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;

public class SubgradeSoilHatch {

	private String soilTypeName;
	private ImageIcon soilHatch; 
	private ImageIcon smallHatch;
	
	public SubgradeSoilHatch(String TypeName, URL SoilHatchResource)
	{
		soilTypeName = TypeName;
		soilHatch = null;
		smallHatch = null;
		if (SoilHatchResource != null)
		{
			soilHatch = new ImageIcon(SoilHatchResource);
			smallHatch = new ImageIcon(soilHatch.getImage().getScaledInstance(43, 18, Image.SCALE_SMOOTH));
		}
	}
	
	public String toString()
	{
		return soilTypeName;
	}
	
	public String getSoilTypeName()
	{
		return soilTypeName;
	}
	
	public Image getImage()
	{
		return soilHatch.getImage();
	}
	
	public ImageIcon getImageIcon()
	{
		return smallHatch;
	}
}
