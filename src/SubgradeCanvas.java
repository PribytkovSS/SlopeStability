
import java.awt.Font;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class SubgradeCanvas extends JPanel {
	private static final long serialVersionUID = 1L;
	private static int X_SHIFT = 5, Y_SHIFT = 1;
	
	private SubgradeSoilEditor soilEditor = null;
	
	private Subgrade subgrade = null;
	private SubgradeGrid grid = new SubgradeGrid();
	
	public SubgradeCanvas()
	{
		addKeyListener(new KeyListener() {
	
			@Override
			public void keyPressed(KeyEvent e) {
					if (soilEditor != null) soilEditor.KeyPressed(e.getKeyCode());
			}

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void keyReleased(KeyEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
			
		addMouseMotionListener(new MouseMotionListener() {
			
			@Override
			public void mouseMoved(MouseEvent e) {
				Point2D.Double clickPoint = MousePoint(e);
				if (soilEditor != null) soilEditor.MouseOver(clickPoint);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				// TODO Auto-generated method stub
			
			}
		});
		
		addMouseListener(new MouseListener() {
				
			@Override
			public void mouseClicked(MouseEvent e) {
				// Определяем масштаб, чтобы пересчитать координаты из экранных в координаты модели
				Point2D.Double clickPoint = MousePoint(e);	
				
				// Первым делом - забираем фокус
				if (!isFocusOwner()) grabFocus();
				if (soilEditor != null) soilEditor.MouseClicked(clickPoint, e.getButton());
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	private Point2D.Double MousePoint(MouseEvent e)
	{
		Point2D.Double ScreenSize;
		ScreenSize = new Point2D.Double(getWidth(), getHeight());
		double Scale = subgrade.GetScale(ScreenSize);
		
		return new Point2D.Double((e.getX() - X_SHIFT) / Scale, (ScreenSize.y - e.getY() + Y_SHIFT) / Scale);
	}

	public void setSoilEditor(SubgradeSoilEditor editor)
	{
		soilEditor = editor;
	}
	
	public SubgradeSoilEditor getSoilEditor()
	{
		return soilEditor;
	}
	
	
	public Subgrade getSubgrade() {
		return subgrade;
	}
	
	public void setSubgrade(Subgrade subgrade) {
		this.subgrade = subgrade;
	}
	
	public BufferedImage getScreenShot()
	{
		// TODO: Увеличить разрешение картинки, чтобы она хорошо смотрелась в отчёте
		BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = (Graphics2D) img.getGraphics();
		paintComponent(g2d);
		return img;
	}
	
	public void paintComponent(Graphics g) {
		Image buf = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = (Graphics2D) buf.getGraphics();
		
		Point2D.Double selectedPoint = soilEditor.getPoint();
		SubgradeSoil selectedSoil = soilEditor.getSoil();
		
		g2d.setColor(Color.white);
		g2d.fillRect(0, 0, getWidth(), getHeight());
		
		int fontSize = 12;
		g2d.setFont(new Font("TimesRoman", Font.PLAIN, fontSize));
		g2d.setColor(Color.black);
		g2d.drawString(String.format("Размеры: %1$d, %2$d", getWidth(),  getHeight()), 5, 20);
		
		if (selectedPoint != null) {
			g2d.drawString(String.format("Ближняя точка: %1$f, %2$f", selectedPoint.x,  selectedPoint.y), 150, 20);
		} else g2d.drawString("Ничего не выбрано", 150, 20);
		
		if (subgrade != null) 
		{
			Point2D.Double ScreenSize = new Point2D.Double(getWidth(), getHeight());
			double Scale = subgrade.GetScale(ScreenSize);
			AffineTransform at_scale = AffineTransform.getScaleInstance(Scale, -Scale);
			AffineTransform at_trans = AffineTransform.getTranslateInstance(X_SHIFT, ScreenSize.y - Y_SHIFT);
			g2d.setTransform(at_trans);
			g2d.transform(at_scale);
			
			grid.paint(g2d, subgrade.MaxX(), subgrade.MaxY(), Scale, ScreenSize);
			
			subgrade.Paint(g2d, ScreenSize);	
			
			if (selectedSoil != null)
			{		
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.30f));
				g2d.setColor(Color.lightGray);
				Area a = selectedSoil.SoilArea();
				g2d.fill(a);
				g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
			}
		
			if (soilEditor != null) soilEditor.Paint(g2d, subgrade.MaxX(), subgrade.MaxY());
		}
		g.drawImage(buf, 0, 0, null);
	}

}
