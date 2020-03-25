
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.docx4j.dml.wordprocessingDrawing.Inline;
import org.docx4j.jaxb.Context;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.wml.P;
import org.docx4j.wml.R;
import org.docx4j.wml.Tbl;
import org.docx4j.wml.TblWidth;
import org.docx4j.wml.Tr;
import org.docx4j.wml.Tc;
import org.docx4j.wml.TcPr;
import org.docx4j.wml.ContentAccessor;
import org.docx4j.wml.Drawing;
import org.docx4j.wml.ObjectFactory;


public class SubgradeReportingThread extends SubgradeAnalyseThread {
	
	protected File file, template_file;
	protected InputStream template_stream;
	private ArrayList<SubgradeImage> images;// = new ArrayList<SubgradeImage>(); 	
	private double Slide, Hold;
	
	public SubgradeReportingThread()
	{
		super();
		images = new ArrayList<SubgradeImage>(); 	
		Slide = 0;
		Hold = 0;
	}

	public void setFile(File report_file)
	{
		file = report_file;
	}
	
	public void setTemplateFile(File tfile)
	{
		template_file = tfile;
	}
	
	@Override
	public void run()
	{	
		callback.actionPerformed(new ActionEvent(this, 0, "Создаём отчёт..."));
		try {
			CreateReport();
		} catch (Exception e1) {
			callback.actionPerformed(new ActionEvent(this, 2, "Ошибка: " + e1.toString()));
			return;
		}
		callback.actionPerformed(new ActionEvent(this, 1, "Выполнено"));
	}
	
	protected Tbl findTable(List<Object> content)
	{
		for (int i = 0; i < content.size(); i++)
		{
			Object obj = content.get(i);
			if (obj instanceof Tbl) return (Tbl) obj;
		    else 
		    	if (!(obj instanceof JAXBElement))
		    	{
		    	  Tbl t = findTable(((ContentAccessor) obj).getContent());
		    	  if (t != null) return t;
		    	} 
		}
		return null;
	}
	
	protected Tc findCell(List<Object> content)
	{
		for (int i = 0; i < content.size(); i++)
		{
			Object obj = content.get(i);
			if (obj instanceof Tc) return (Tc) obj;
		    else 
		    	if (!(obj instanceof JAXBElement))
		    	{
		    	  Tc t = findCell(((ContentAccessor) obj).getContent());
		    	  if (t != null) return t;
		    	} 
		}
		return null;
	}
	
	
	protected void CreateReport() throws Docx4JException, JAXBException 
	{
		// Создание отчёта по результатам расчётов
		// Загружаем шаблон отчёта
		/*if (!template_file.exists()) 
		{
			throw (new Docx4JException("Не удалось найти файл шаблона отчёта: "+template_file.getAbsolutePath()));
		};*/
		WordprocessingMLPackage word = WordprocessingMLPackage.load(template_stream);
		if (word == null) 
		{
			throw (new Docx4JException("Не удалось инициализировать WordprocessingMLPackage"));
		}
		// Ищем места, куда нужно вставить данные
		// 1. Схема земляного полотна с изображением критической поверхности
		// 2. Таблица с данными о грунтах
		// 3. Таблица с подробностями расчёта
		// 4. Таблица с итогом расчёта
		MainDocumentPart mdp = word.getMainDocumentPart();	
		if (mdp != null)
		{
			// Получаем список всех таблиц документа - а их у нас должно быть пять. Причём третья вложена во вторую, 
			// и вторую мы напрямую модифицировать не будем
			List<Object> tbl_list = mdp.getJAXBNodesViaXPath("//w:tbl", false);
			if (tbl_list != null)
			{
				// Первая таблица - для схемы
				JAXBElement<Tbl> jtbl = (JAXBElement<Tbl>) tbl_list.get(0);
				Tbl table = jtbl.getValue();
				Tc cell = getCell(table, 0, 0);
					
				if (cell != null) InsertImage(word, cell, images.get(0).getImageBytes());
					
				// Данные о грунтах
				jtbl = (JAXBElement<Tbl>) tbl_list.get(2);
				table = jtbl.getValue();
				ReportSoilData(word, mdp, table);
					
				// Данные о нагрузках
				jtbl = (JAXBElement<Tbl>) tbl_list.get(3);
				table = jtbl.getValue();
				ReportLoadData(word, mdp, table);
					
				// Подробности расчёта
				jtbl = (JAXBElement<Tbl>) tbl_list.get(4);
				table = jtbl.getValue();
				ReportDetails(word, mdp, table);
					
				// Итоги расчёта
				jtbl = (JAXBElement<Tbl>) tbl_list.get(5);
				table = jtbl.getValue();
				ReportResult(word, mdp, table);
			}
		}	
		word.save(file);
	}

	private void ReportResult(WordprocessingMLPackage word, MainDocumentPart mdp, Tbl table) 
	{
		Tc cell = getCell(table, 0, 1);
		if (Slide != 0)
		{
			TypeText(mdp, cell, String.format("%1.2f / %2.2f = %3.2f", Hold, Slide, Hold / Slide));
		}
	}

	private void ReportDetails(WordprocessingMLPackage word, MainDocumentPart mdp, Tbl table) 
	{
		double s, h;
		SoilBlock block = subgrade.blocks.get(0);
		for (int i = 0; i < block.getSlices().size(); i++)
		{
			SubgradeSlice slice = subgrade.blocks.get(0).getSlices().get(i);
			// № по порядку
			Tc cell = getCell(table, i+2, 0);
			TypeText(mdp, cell, Integer.toString(i+1));
			// Площадь
			cell = getCell(table, i+2, 1);
			TypeText(mdp, cell, String.format("%.2f", slice.getArea()));
			// Вес
			cell = getCell(table, i+2, 2);
			TypeText(mdp, cell, String.format("%.2f", slice.Weight()));
			// Длина основания
			cell = getCell(table, i+2, 3);
			Line2D.Double line = slice.BaseLine();
			if (line != null)
			{
				TypeText(mdp, cell, String.format("%.2f", Point2D.distance(line.x1, line.y1, line.x2, line.y2)));
			}
			// Угол основания
			cell = getCell(table, i+2, 4);
			TypeText(mdp, cell, String.format("%.2f", slice.BaseAngle() / Math.PI * 180));
			
			// Сдвигающая сила
			cell = getCell(table, i+2, 5);
			s = slice.SlideForces(block.meshDirection);
			Slide = Slide + s;
			TypeText(mdp, cell, String.format("%.2f", s));
			// Сдвигающая сила нарастающим итогом
			cell = getCell(table, i+2, 6);
			TypeText(mdp, cell, String.format("%.2f", Slide));
			// Удерживающие силы: сила трения
			cell = getCell(table, i+2, 7);
			h = slice.Friction(); Hold = Hold + h;
			TypeText(mdp, cell, String.format("%.2f", h));
			// Удерживающие силы: сила cцепления
			cell = getCell(table, i+2, 8);
			h = slice.CohesionForce(); Hold = Hold + h;
			TypeText(mdp, cell, String.format("%.2f", h));
			// Удерживающие силы: касательная сила
			cell = getCell(table, i+2, 9);
			if (s == 0) h = slice.Weight() * Math.sin(slice.BaseAngle()); else h = 0;
			TypeText(mdp, cell, String.format("%.2f", h));
			Hold = Hold + h;
			// Удерживающие силы: касательная сила
			cell = getCell(table, i+2, 10);
			TypeText(mdp, cell, String.format("%.2f", Hold));
			if (i != block.getSlices().size() - 1)
			{
				Tr row = getRow(table, i + 1);
				AppendRow(table, row);
			}
		}
	}

	private void ReportLoadData(WordprocessingMLPackage word, MainDocumentPart mdp, Tbl table) {
		for (int i = 0; i < subgrade.getLoads().size(); i++)
		{
			SubgradeLoad load = subgrade.getLoads().get(i);
			// № по порядку
			Tc cell = getCell(table, i+1, 0);
			TypeText(mdp, cell, Integer.toString(i+1));
			// Начальная точка
			cell = getCell(table, i+1, 1);
			TypeText(mdp, cell, String.format("%1.2f; %2.2f", load.GetLine().x1, load.GetLine().y1));
			// Конечная точка
			cell = getCell(table, i+1, 2);
			TypeText(mdp, cell, String.format("%1.2f; %2.2f", load.GetLine().x2, load.GetLine().y2));
			// Интенсивность
			cell = getCell(table, i+1, 3);
			TypeText(mdp, cell, String.format("%.2f", load.GetLoad()));
			
			if (i != subgrade.getLoads().size() - 1)
			{
				Tr row = getRow(table, i + 1);
				AppendRow(table, row);
			}
		}
	}

	private void ReportSoilData(WordprocessingMLPackage word, MainDocumentPart mdp, Tbl table) {
		for (int i = 0; i < subgrade.Layers().size(); i++)
		{
			SubgradeSoil soil = subgrade.Layers().get(i);
			// № по порядку
			Tc cell = getCell(table, i+1, 0);
			TypeText(mdp, cell, Integer.toString(i+1));
			// Наименование грунта
			cell = getCell(table, i+1, 1);
			TypeText(mdp, cell, soil.getSoilName());
			// Условное обозначение
			if (soil.getHatchIndex() > 0)
			{
				cell = getCell(table, i+1, 2);
				InsertImage(word, cell, images.get(soil.getHatchIndex()).getImageBytes());
			}
			// Удельный вес
			cell = getCell(table, i+1, 3);
			TypeText(mdp, cell, String.format("%.2f", soil.getG()));
			// Удельное сцепление
			cell = getCell(table, i+1, 4);
			TypeText(mdp, cell, String.format("%.2f", soil.getC()));
			// Угол внутреннего трения
			cell = getCell(table, i+1, 5);
			TypeText(mdp, cell, String.format("%.2f", soil.getF()/Math.PI * 180));
			
			if (i != subgrade.Layers().size() - 1)
			{
				Tr row = getRow(table, i + 1);
				AppendRow(table, row);
			}
		}
	}

	private void AppendRow(Tbl table, Tr row) 
	{
		org.docx4j.wml.ObjectFactory factory = Context.getWmlObjectFactory();
		Tr new_tr = factory.createTr(); 
		table.getContent().add(new_tr); 
		// Теперь пройдёмся по колонкам
		for (Object col_obj: row.getContent())
		{
			if (col_obj instanceof JAXBElement)
			{
				JAXBElement<Tc> column = (JAXBElement<Tc>) col_obj;
				Tc origin_col = column.getValue();
				Tc new_col = factory.createTc(); 
		        JAXBElement<Tc> new_col_wrap = factory.createTrTc(new_col); 
		        new_tr.getContent().add(new_col_wrap); 
		        
		        org.docx4j.wml.TcPr tcpr = CellPropsCopy(factory, origin_col.getTcPr());
		        new_col.setTcPr(tcpr);
		        
		        P new_p = factory.createP();
		        new_col.getContent().add(new_p);	
		        org.docx4j.wml.PPr new_ppr = factory.createPPr(); 
		        new_p.setPPr(new_ppr); 
                  
		        org.docx4j.wml.ParaRPr new_pararpr = factory.createParaRPr(); 
		        new_ppr.setRPr(new_pararpr); 
                
		        org.docx4j.wml.HpsMeasure new_hpsmeasure = factory.createHpsMeasure(); 
		        new_pararpr.setSz(new_hpsmeasure); 
		        new_hpsmeasure.setVal(BigInteger.valueOf(20)); 
			}
		}
	}

	private TcPr CellPropsCopy(ObjectFactory factory, TcPr pr_origin) 
	{
		org.docx4j.wml.TcPr new_pr = factory.createTcPr();
		TblWidth tw = factory.createTblWidth(); 
		
		new_pr.setTcW(tw); 
		tw.setType(pr_origin.getTcW().getType()); 
        tw.setW(pr_origin.getTcW().getW()); 
        
		return new_pr;
	}

	private void TypeText(MainDocumentPart mdp, Tc cell, String string) 
	{
		cell.getContent().clear();
		org.docx4j.wml.ObjectFactory factory = Context.getWmlObjectFactory();
		org.docx4j.wml.P para = factory.createP();
		if (string != null) 
		{
			org.docx4j.wml.Text t = factory.createText();
			t.setValue(string);
			org.docx4j.wml.R run = factory.createR();
			run.getContent().add(t); 
			para.getContent().add(run); 
		};
		cell.getContent().add(para); 
	}

	private Tc getCell(Tbl table, int row_index, int column_index) {
		// Первая строка таблицы
		Tr row = (Tr) table.getContent().get(row_index);
		// Первая ячейка таблицы
		JAXBElement<Tc> jcell = (JAXBElement<Tc>) row.getContent().get(column_index);
		return jcell.getValue();
	}
	
	private Tr getRow(Tbl table, int row_index) {
		// Первая строка таблицы
		Tr row = (Tr) table.getContent().get(row_index);
	    return row;
	}

	private void InsertImage(WordprocessingMLPackage word, Tc cell, byte[] imageBytes) 
	{
		cell.getContent().clear();
		org.docx4j.wml.ObjectFactory factory = Context.getWmlObjectFactory();
		P  p = factory.createP();
		R  run = factory.createR();	
		p.getContent().add(run);  
			
		BinaryPartAbstractImage imagePart;
		try 
		{
			imagePart = BinaryPartAbstractImage.createImagePart(word, imageBytes);
			Inline inline = imagePart.createImageInline("", "рисунок", 0, 1, false);
			Drawing drawing = factory.createDrawing();

			run.getContent().add(drawing);		
			drawing.getAnchorOrInline().add(inline);
				
			cell.getContent().add(p);	
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

	public void setImage(BufferedImage screenShot) {
		images.add(new SubgradeImage(screenShot));
	}
	
	public void setHatchImages(DefaultListModel<SubgradeSoilHatch> patternListModel) {
		for (int i = 1; i < patternListModel.size(); i++)
		{ 
			SubgradeSoilHatch hatch = patternListModel.getElementAt(i);
			BufferedImage scaled_hatch = new BufferedImage(70, 25, BufferedImage.TYPE_INT_RGB);
			Image ihatch = hatch.getImage().getScaledInstance(70, 25, Image.SCALE_SMOOTH);
			Graphics2D g2d = scaled_hatch.createGraphics();
			while (!g2d.drawImage(ihatch, 0, 0, null));
			images.add(new SubgradeImage(scaled_hatch));
		}
	}

	public void setTemplateStream(InputStream template) {
		template_stream = template;
	}
}
