
import java.awt.event.ActionEvent;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;


public class SubgradeImportThread extends SubgradeReportingThread {

	public void run()
	{
		callback.actionPerformed(new ActionEvent(this, 0, "Идёт расчёт..."));
		try 
		{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder doc_builder;
			doc_builder = factory.newDocumentBuilder();
			Document doc = doc_builder.parse(file);
			subgrade.Load(doc);
		} catch (Exception e) {
			callback.actionPerformed(new ActionEvent(this, 2, "Ошибка: " + e.toString()));
			return;
		}	
		callback.actionPerformed(new ActionEvent(this, 3, "Выполнено"));
	}
}
