
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SubgradeAnalyseThread extends Thread {
		protected Subgrade subgrade;
		protected ActionListener callback;
		
		public void setSubgrade(Subgrade subgradeToAnalyse, ActionListener listener)
		{
			subgrade = subgradeToAnalyse;
			callback = listener;
		}
		
		@Override
		public void run()
		{
			callback.actionPerformed(new ActionEvent(this, 0, "Идёт расчёт..."));
			try 
			{
				subgrade.Analyse();
			} catch (Exception e) {
				callback.actionPerformed(new ActionEvent(this, 2, "Ошибка: " + e.toString()));
			};
			callback.actionPerformed(new ActionEvent(this, 1, "Выполнено"));
		}
}
