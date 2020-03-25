
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class SubgradeHatchListRenderer extends JLabel implements ListCellRenderer<SubgradeSoilHatch> {

	private static final long serialVersionUID = 1L;

	public SubgradeHatchListRenderer() {
        setOpaque(true);
    }
 
    @Override
    public Component getListCellRendererComponent(JList<? extends SubgradeSoilHatch> list, SubgradeSoilHatch soilHatch, int index,
            boolean isSelected, boolean cellHasFocus) {
 
        setIcon(soilHatch.getImageIcon());
        setText(soilHatch.getSoilTypeName());
 
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
 
        return this;
    }
	
}
