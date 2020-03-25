

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JLabel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import javax.swing.SwingConstants;
import javax.swing.JToggleButton;
import javax.swing.JLayeredPane;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.CardLayout;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ChangeEvent;
import java.awt.Color;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;


import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JList;
import javax.swing.JOptionPane;

import java.awt.SystemColor;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import javax.swing.event.ListSelectionEvent;
import javax.swing.ListSelectionModel;
import javax.swing.JTextArea;
import java.awt.Font;
import java.awt.Graphics2D;

import javax.swing.JTabbedPane;

public class SubgradeMain {

	private JFrame frame;
	
	static final double NEAR_TOLERANCE = 0.3;
	
	private Subgrade subgrade = null;
	private SubgradeCanvas CrossSectionCanvas = null;
	private JTextField LoadEditBox;
	private JTextField WeightEdit;
	private JTextField CohesionEdit;
	private JTextField FricAngleEdit;
	private SlopePointsListModel slopePointsListModel;
	private DefaultListModel<SubgradeSoilHatch> patternListModel;
	private SubgradeSoilEditor soilEditor;
	private JProgressBar progress;
	private ActionListener progressActionListener;


	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					SubgradeMain window = new SubgradeMain();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public SubgradeMain() {
		initialize();
		// TODO различные варианты загрузки
		init(0);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(10, 10, 800, 600);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Расчёт устойчивости откосов");
		CreateContents();
	}

	private void CreateContents() {
		frame.setLayout(new BorderLayout(0, 0));
		
		JPanel CrossSectionPanel = new JPanel();
		frame.add(CrossSectionPanel, BorderLayout.CENTER);
		CrossSectionPanel.setLayout(new BorderLayout(0, 0));
				
		CrossSectionCanvas = new SubgradeCanvas();
		CrossSectionCanvas.setBackground(Color.WHITE);
		CrossSectionCanvas.setForeground(Color.BLACK);
		CrossSectionPanel.add(CrossSectionCanvas, BorderLayout.CENTER);
				
		JLayeredPane layeredPane = new JLayeredPane();
		CrossSectionPanel.add(layeredPane, BorderLayout.EAST);
		layeredPane.setLayout(new CardLayout(0, 0));
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		layeredPane.add(tabbedPane, "name_10209733508782");
		
		JPanel PanelPointData = new JPanel();
		//layeredPane.add(PanelPointData, "PanelPointData");
		tabbedPane.addTab("Геометрия", PanelPointData);
		PanelPointData.setLayout(null);
		
		JLabel lblNewLabel = new JLabel("X:");
		lblNewLabel.setBounds(15, 165, 20, 20);
		PanelPointData.add(lblNewLabel);
		
		JTextField PointXEdit = new JTextField();
		PointXEdit.setBounds(35, 165, 80, 20);
		lblNewLabel.setLabelFor(PointXEdit);
		PanelPointData.add(PointXEdit);
		
		JLabel label_1 = new JLabel("Y:");
		label_1.setBounds(15, 190, 20, 20);
		PanelPointData.add(label_1);
		
		JTextField PointYEdit = new JTextField();
		PointYEdit.setBounds(35, 190, 80, 20);
		label_1.setLabelFor(PointYEdit);
		PanelPointData.add(PointYEdit);
		
		JButton ButtonChangeCoords = new JButton("\u0418\u0437\u043C\u0435\u043D\u0438\u0442\u044C");
		ButtonChangeCoords.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (soilEditor.getPoint() != null)
				{
					double x = SubgradeSoil.myParseDouble(PointXEdit.getText());
					double y = SubgradeSoil.myParseDouble(PointYEdit.getText());
					if ((x != SubgradeSoil.PARSE_ERROR) && (y != SubgradeSoil.PARSE_ERROR))
					{
						subgrade.MovePoint(soilEditor.getPoint(), new Point2D.Double(x, y));
						soilEditor.getPoint().setLocation(x, y);
						CrossSectionCanvas.repaint();
					}
				}
			}
		});
		ButtonChangeCoords.setBounds(20, 215, 100, 25);
		PanelPointData.add(ButtonChangeCoords);
		
		JButton ButtonChangeCoordsCancel = new JButton("\u041E\u0442\u043C\u0435\u043D\u0430");
		ButtonChangeCoordsCancel.setBounds(20, 245, 100, 25);
		PanelPointData.add(ButtonChangeCoordsCancel);
		
		JLabel lblNewLabel_1 = new JLabel("\u041A\u043E\u043E\u0440\u0434\u0438\u043D\u0430\u0442\u044B \u0442\u043E\u0447\u043A\u0438:");
		lblNewLabel_1.setBounds(10, 145, 120, 14);
		PanelPointData.add(lblNewLabel_1);
		
		
		JToggleButton ButtonNewLine = new JToggleButton("\u041B\u0438\u043D\u0438\u044F");
		ButtonNewLine.setBounds(15, 11, 115, 25);
		PanelPointData.add(ButtonNewLine);
		ButtonNewLine.setToolTipText("\u0414\u043E\u0431\u0430\u0432\u0438\u0442\u044C \u043D\u043E\u0432\u0443\u044E \u043B\u0438\u043D\u0438\u044E \u043D\u0430 \u0441\u0445\u0435\u043C\u0443");
		
		JToggleButton ButtonAddPoint = new JToggleButton("\u0422\u043E\u0447\u043A\u0430");
		ButtonAddPoint.setBounds(15, 42, 115, 25);
		PanelPointData.add(ButtonAddPoint);
		ButtonAddPoint.setToolTipText("\u0414\u043E\u0431\u0430\u0432\u0438\u0442\u044C \u043D\u043E\u0432\u0443\u044E \u0442\u043E\u0447\u043A\u0443 \u043D\u0430 \u0432\u044B\u0431\u0440\u0430\u043D\u043D\u0443\u044E \u043B\u0438\u043D\u0438\u044E");
		ButtonAddPoint.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (ButtonAddPoint.isSelected()) 
				{
					soilEditor.setMode(SubgradeSoilEditor.EM_ADD_POINT);
				} else soilEditor.setMode(SubgradeSoilEditor.EM_NORMAL);
			}
		});
		
		JToggleButton ButtonDelete = new JToggleButton("\u0423\u0434\u0430\u043B\u0438\u0442\u044C");
		ButtonDelete.setBounds(15, 74, 115, 25);
		PanelPointData.add(ButtonDelete);
		ButtonDelete.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (ButtonDelete.isSelected()) 
				{
					soilEditor.setMode(SubgradeSoilEditor.EM_DELETE_OBJECT);
				} else soilEditor.setMode(SubgradeSoilEditor.EM_NORMAL);
			}
		});
		ButtonDelete.setToolTipText("\u0423\u0434\u0430\u043B\u0438\u0442\u044C \u0432\u044B\u0431\u0440\u0430\u043D\u043D\u0443\u044E \u0442\u043E\u0447\u043A\u0443 \u0438\u043B\u0438 \u043B\u0438\u043D\u0438\u044E");
		
		// Редактирование координат
		JToggleButton ButtonCoordinates = new JToggleButton("(x; y)");
		ButtonCoordinates.setBounds(15, 106, 115, 25);
		PanelPointData.add(ButtonCoordinates);
		ButtonCoordinates.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (ButtonCoordinates.isSelected()) 
				{
					soilEditor.setMode(SubgradeSoilEditor.EM_COORDINATES);
				} else soilEditor.setMode(SubgradeSoilEditor.EM_NORMAL);
			}
		});
		ButtonCoordinates.setToolTipText("\u0417\u0430\u0434\u0430\u0442\u044C \u043A\u043E\u043E\u0440\u0434\u0438\u043D\u0430\u0442\u044B \u0432\u044B\u0431\u0440\u0430\u043D\u043D\u043E\u0439 \u0442\u043E\u0447\u043A\u0438");
		
		JButton button = new JButton("Построить");
		button.setToolTipText("Создать отдельные слои грунта из замкнутых контуров");
		button.setBounds(15, 281, 115, 25);
		PanelPointData.add(button);
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
					subgrade.RefineLayers(null);
			}
		});
		
		JButton button_exp = new JButton("Экспорт");
		button_exp.setToolTipText("Сохранить описание земляного полотна в файл");
		button_exp.setBounds(15, 313, 115, 25);
		PanelPointData.add(button_exp);
		button_exp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
					JFileChooser dlg = new JFileChooser();
					dlg.setDialogTitle("Укажите имя файла и путь к нему");
					dlg.setFileFilter(new FileNameExtensionFilter("Файлы XML", "xml"));
					int r = dlg.showSaveDialog(CrossSectionCanvas);
					File file = dlg.getSelectedFile();
					if (file.exists()) 
					{
						int ui = JOptionPane.showConfirmDialog(CrossSectionCanvas, "Файл "+file.getName() + " существует. Перезаписать?",
								"Предупреждение", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
						if (ui != JOptionPane.YES_OPTION) return;
					}
					if (r == JFileChooser.APPROVE_OPTION) 
					{
						if (!file.exists() && (getFileExtension(file.getName()) == null)) 
							file = new File(file.getName() + ".xml");
						SaveSubgrade(file);
					}
			}
		});
		
		JButton button_imp = new JButton("Импорт");
		button_imp.setToolTipText("Загрузить описание земляного полотна из файла");
		button_imp.setBounds(15, 345, 115, 25);
		PanelPointData.add(button_imp);
		
		ButtonNewLine.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if (ButtonNewLine.isSelected()) 
				{
					soilEditor.setMode(SubgradeSoilEditor.EM_ADD_LINE);
				} else soilEditor.setMode(SubgradeSoilEditor.EM_NORMAL);
			}
		});
		
		ButtonGroup bg = new ButtonGroup();
		bg.add(ButtonNewLine);
		bg.add(ButtonAddPoint);
		bg.add(ButtonCoordinates);
		bg.add(ButtonDelete);
		
		JPanel PanelLoadData = new JPanel();
		//layeredPane.add(PanelLoadData, "PanelLoadData");
		tabbedPane.addTab("Нагрузка", PanelLoadData);
		PanelLoadData.setLayout(null);
		
		JLabel LabelLoad = new JLabel("\u041D\u0430\u0433\u0440\u0443\u0437\u043A\u0430, \u043A\u041F\u0430:");
		LabelLoad.setBounds(13, 93, 110, 14);
		PanelLoadData.add(LabelLoad);
		
		LoadEditBox = new JTextField();
		LabelLoad.setLabelFor(LoadEditBox);
		LoadEditBox.setHorizontalAlignment(SwingConstants.RIGHT);
		LoadEditBox.setText("0");
		LoadEditBox.setBounds(13, 113, 110, 20);
		PanelLoadData.add(LoadEditBox);
		LoadEditBox.setColumns(10);
				
		JButton ButtonSetLoad = new JButton("\u0423\u0441\u0442\u0430\u043D\u043E\u0432\u0438\u0442\u044C");
		ButtonSetLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// Изменяем нагрузку на линию
				if (soilEditor.getLine() != null)
				{
					double load = SubgradeSoil.myParseDouble(LoadEditBox.getText());
					if (load != SubgradeSoil.PARSE_ERROR)
					{
						SubgradeLoad l = subgrade.GetLoad(soilEditor.getLine());
						if (l == null)
							subgrade.AddLoad(new SubgradeLoad(soilEditor.getLine(), load)); 
						else l.SetLoad(load);
					};
					CrossSectionCanvas.repaint();
				}
			}
		});
		ButtonSetLoad.setBounds(10, 142, 113, 23);
		PanelLoadData.add(ButtonSetLoad);
		
		LoadEditBox.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					ButtonSetLoad.doClick(10);				
				}
			}
		});
		
		JButton ButtonDeleteLoad = new JButton("\u0421\u043D\u044F\u0442\u044C");
		ButtonDeleteLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (soilEditor.getLine() != null)
				{
					SubgradeLoad load = subgrade.GetLoad(soilEditor.getLine());
					if (load != null) load.SetLoad(0);
					LoadEditBox.setText("0");
					CrossSectionCanvas.repaint();
				}
			}
		});
		ButtonDeleteLoad.setBounds(10, 171, 113, 23);
		PanelLoadData.add(ButtonDeleteLoad);
		
		JTextArea textArea_1 = new JTextArea();
		textArea_1.setWrapStyleWord(true);
		textArea_1.setText("\u0423\u043A\u0430\u0436\u0438\u0442\u0435 \u043B\u0438\u043D\u0438\u044E \u043D\u0430 \u0441\u0445\u0435\u043C\u0435 \u0438 \u0437\u0430\u0434\u0430\u0439\u0442\u0435 \u0438\u043D\u0442\u0435\u043D\u0441\u0438\u0432\u043D\u043E\u0441\u0442\u044C \u0432\u0435\u0440\u0442\u0438\u043A\u0430\u043B\u044C\u043D\u043E\u0439 \u0440\u0430\u0441\u043F\u0440\u0435\u0434\u0435\u043B\u0451\u043D\u043D\u043E\u0439 \u043D\u0430\u0433\u0440\u0443\u0437\u043A\u0438.");
		textArea_1.setLineWrap(true);
		textArea_1.setFont(new Font("Tahoma", textArea_1.getFont().getStyle() & ~Font.BOLD & ~Font.ITALIC, 11));
		textArea_1.setEditable(false);
		textArea_1.setBackground(SystemColor.menu);
		textArea_1.setBounds(10, 11, 134, 73);
		PanelLoadData.add(textArea_1);
		
		JPanel PanelSoilData = new JPanel();
		//layeredPane.add(PanelSoilData, "PanelSoilData");
		tabbedPane.addTab("Грунт", PanelSoilData);
		PanelSoilData.setLayout(null);
		
		JLabel WeightLabel = new JLabel("\u0423\u0434\u0435\u043B\u044C\u043D\u044B\u0439 \u0432\u0435\u0441:");
		WeightLabel.setBounds(10, 107, 123, 14);
		WeightLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		PanelSoilData.add(WeightLabel);
		
		WeightEdit = new JTextField();
		WeightEdit.setHorizontalAlignment(SwingConstants.RIGHT);
		WeightEdit.setBounds(10, 125, 72, 20);
		PanelSoilData.add(WeightEdit);
		WeightEdit.setColumns(10);
		
		JLabel label_3 = new JLabel("\u0423\u0434\u0435\u043B\u044C\u043D\u043E\u0435 \u0441\u0446\u0435\u043F\u043B\u0435\u043D\u0438\u0435:");
		label_3.setAlignmentX(0.5f);
		label_3.setBounds(10, 151, 139, 14);
		PanelSoilData.add(label_3);
		
		CohesionEdit = new JTextField();
		CohesionEdit.setHorizontalAlignment(SwingConstants.RIGHT);
		CohesionEdit.setColumns(10);
		CohesionEdit.setBounds(10, 171, 72, 20);
		PanelSoilData.add(CohesionEdit);
		
		JLabel label_4 = new JLabel("\u0423\u0433\u043E\u043B \u0432\u043D\u0443\u0442\u0440\u0435\u043D\u043D\u0435\u0433\u043E \u0442\u0440\u0435\u043D\u0438\u044F:");
		label_4.setAlignmentX(0.5f);
		label_4.setBounds(10, 201, 149, 14);
		PanelSoilData.add(label_4);
		
		JLabel lblNewLabel_2 = new JLabel("\u043A\u041D/\u043A\u0443\u0431. \u043C");
		lblNewLabel_2.setBounds(92, 128, 57, 14);
		PanelSoilData.add(lblNewLabel_2);
		
		JLabel label_5 = new JLabel("\u043A\u041F\u0430");
		label_5.setBounds(92, 176, 57, 14);
		PanelSoilData.add(label_5);
		
		FricAngleEdit = new JTextField();
		FricAngleEdit.setHorizontalAlignment(SwingConstants.RIGHT);
		FricAngleEdit.setColumns(10);
		FricAngleEdit.setBounds(10, 220, 72, 20);
		PanelSoilData.add(FricAngleEdit);
		
		JLabel label_6 = new JLabel("\u0433\u0440\u0430\u0434.");
		label_6.setBounds(92, 223, 57, 14);
		PanelSoilData.add(label_6);
		
		JLabel labelSoilName = new JLabel("Наименование:");
		labelSoilName.setBounds(10, 254, 150, 14);
		PanelSoilData.add(labelSoilName);
		
		JTextArea SoilNameEdit = new JTextArea();
		SoilNameEdit.setBounds(10, 274, 150, 48);
		SoilNameEdit.setLineWrap(true);
		PanelSoilData.add(SoilNameEdit);
		
		JLabel labelPattern = new JLabel("Штриховка:");
		labelPattern.setBounds(10, 332, 150, 14);
		PanelSoilData.add(labelPattern);
		
		patternListModel = new DefaultListModel<SubgradeSoilHatch>();
		JList<SubgradeSoilHatch> PatternList = new JList<SubgradeSoilHatch>(patternListModel);
		PatternList.setCellRenderer(new SubgradeHatchListRenderer());
		//PatternList.setBounds(10, 354, 150, 100);
		//PanelSoilData.add(PatternList);
		JScrollPane scrollPane = new JScrollPane(PatternList);
		scrollPane.setBounds(10, 354, 150, 100);
		scrollPane.setMinimumSize(new Dimension(150, 100));
		PanelSoilData.add(scrollPane);
		patternListModel.addElement(new SubgradeSoilHatch("без штриховки", null));
		patternListModel.addElement(new SubgradeSoilHatch("грунт 01", this.getClass().getResource("01-hatch.jpg")));
	    patternListModel.addElement(new SubgradeSoilHatch("грунт 02", this.getClass().getResource("02-hatch.jpg")));
		patternListModel.addElement(new SubgradeSoilHatch("грунт 03", this.getClass().getResource("03-hatch.jpg")));
		patternListModel.addElement(new SubgradeSoilHatch("грунт 04", this.getClass().getResource("04-hatch.jpg")));
		patternListModel.addElement(new SubgradeSoilHatch("грунт 05", this.getClass().getResource("05-hatch.jpg")));
	    patternListModel.addElement(new SubgradeSoilHatch("грунт 06", this.getClass().getResource("06-hatch.jpg")));
		patternListModel.addElement(new SubgradeSoilHatch("грунт 07", this.getClass().getResource("07-hatch.jpg")));
		patternListModel.addElement(new SubgradeSoilHatch("грунт 08", this.getClass().getResource("08-hatch.jpg")));
		patternListModel.addElement(new SubgradeSoilHatch("грунт 09", this.getClass().getResource("09-hatch.jpg")));
	    patternListModel.addElement(new SubgradeSoilHatch("грунт 10", this.getClass().getResource("10-hatch.jpg")));
		patternListModel.addElement(new SubgradeSoilHatch("грунт 11", this.getClass().getResource("11-hatch.jpg")));
		patternListModel.addElement(new SubgradeSoilHatch("грунт 12", this.getClass().getResource("12-hatch.jpg")));
		patternListModel.addElement(new SubgradeSoilHatch("грунт 13", this.getClass().getResource("13-hatch.jpg")));
		patternListModel.addElement(new SubgradeSoilHatch("грунт 14", this.getClass().getResource("14-hatch.jpg")));
	
		JButton ButtonSetProps = new JButton("\u0418\u0437\u043C\u0435\u043D\u0438\u0442\u044C");
		ButtonSetProps.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (soilEditor.getSoil() != null)
				{
					double weight = SubgradeSoil.myParseDouble(WeightEdit.getText());
					double cohesion = SubgradeSoil.myParseDouble(CohesionEdit.getText()); 
					double fric = SubgradeSoil.myParseDouble(FricAngleEdit.getText());
					
					if (fric != SubgradeSoil.PARSE_ERROR) fric = fric / 180 * Math.PI;
					if (weight != SubgradeSoil.PARSE_ERROR) soilEditor.getSoil().setG(weight);
					if (cohesion != SubgradeSoil.PARSE_ERROR) soilEditor.getSoil().setC(cohesion);
					if (fric != SubgradeSoil.PARSE_ERROR) soilEditor.getSoil().setF(fric);
					soilEditor.getSoil().setSoilName(SoilNameEdit.getText());
					soilEditor.getSoil().setHatchIndex(PatternList.getSelectedIndex());
					CrossSectionCanvas.repaint();
				}
			}
		});
		ButtonSetProps.setBounds(10, 462, 89, 23);
		PanelSoilData.add(ButtonSetProps);
		
		JTextArea textArea_2 = new JTextArea();
		textArea_2.setWrapStyleWord(true);
		textArea_2.setText("\u0429\u0451\u043B\u043A\u043D\u0438\u0442\u0435 \u0432\u043D\u0443\u0442\u0440\u0438 \u043A\u043E\u043D\u0442\u0443\u0440\u0430, \u043E\u0433\u0440\u0430\u043D\u0438\u0447\u0438\u0432\u0430\u044E\u0449\u0435\u0433\u043E \u0441\u043B\u043E\u0439 \u0433\u0440\u0443\u043D\u0442\u0430, \u0438 \u043E\u0442\u0440\u0435\u0434\u0430\u043A\u0442\u0438\u0440\u0443\u0439\u0442\u0435 \u0441\u0432\u043E\u0439\u0441\u0442\u0432\u0430.");
		textArea_2.setLineWrap(true);
		textArea_2.setFont(new Font("Tahoma", textArea_2.getFont().getStyle() & ~Font.BOLD & ~Font.ITALIC, 11));
		textArea_2.setEditable(false);
		textArea_2.setBackground(SystemColor.menu);
		textArea_2.setBounds(10, 11, 134, 85);
		PanelSoilData.add(textArea_2);
		
		JPanel PanelSlopeData = new JPanel();
		//layeredPane.add(PanelSlopeData, "PanelSlopeData");
		tabbedPane.addTab("Расчёт", PanelSlopeData);
		PanelSlopeData.setLayout(null);
		
		slopePointsListModel = new SlopePointsListModel();
		
		JList<String> SlopePointList = new JList<String>(slopePointsListModel);
		SlopePointList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		SlopePointList.setBorder(new LineBorder(new Color(0, 0, 0)));
		SlopePointList.setBounds(10, 121, 134, 79);
		PanelSlopeData.add(SlopePointList);
		
			
		JButton ButtonDeletePoint = new JButton("\u0423\u0434\u0430\u043B\u0438\u0442\u044C \u0442\u043E\u0447\u043A\u0443");
		ButtonDeletePoint.setToolTipText("\u0423\u0434\u0430\u043B\u0438\u0442\u044C \u0438\u0437 \u0441\u043F\u0438\u0441\u043A\u0430 \u0432\u044B\u0431\u0440\u0430\u043D\u043D\u0443\u044E \u0442\u043E\u0447\u043A\u0443 \u043E\u0442\u043A\u043E\u0441\u0430");
		ButtonDeletePoint.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!SlopePointList.isSelectionEmpty())
					slopePointsListModel.removePointElementAt(SlopePointList.getSelectedIndex());
			}
		});
		
		ButtonDeletePoint.setBounds(10, 211, 134, 23);
		PanelSlopeData.add(ButtonDeletePoint);
		
		JTextArea textArea = new JTextArea();
		textArea.setBackground(SystemColor.control);
		textArea.setFont(new Font("Tahoma", textArea.getFont().getStyle() & ~Font.BOLD & ~Font.ITALIC, 11));
		textArea.setEditable(false);
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		textArea.setText("\u0423\u043A\u0430\u0436\u0438\u0442\u0435 \u0434\u0432\u0435 \u0442\u043E\u0447\u043A\u0438 - \u043D\u0430 \u0432\u0435\u0440\u0448\u0438\u043D\u0435 \u0438 \u0443 \u043F\u043E\u0434\u043D\u043E\u0436\u0438\u044F \u0441\u043A\u043B\u043E\u043D\u0430, \u0443\u0441\u0442\u043E\u0439\u0447\u0438\u0432\u043E\u0441\u0442\u044C \u043A\u043E\u0442\u043E\u0440\u043E\u0433\u043E \u0432\u044B \u0445\u043E\u0442\u0438\u0442\u0435 \u043E\u0446\u0435\u043D\u0438\u0442\u044C:");
		textArea.setBounds(10, 43, 134, 74);
		PanelSlopeData.add(textArea);
		
		JToggleButton SlopeButton = new JToggleButton("\u041E\u0442\u043A\u043E\u0441");
		SlopeButton.setBounds(10, 11, 134, 21);
		PanelSlopeData.add(SlopeButton);
		
		JButton CalcButton = new JButton("\u0420\u0430\u0441\u0447\u0451\u0442");
		CalcButton.setBounds(10, 275, 134, 21);
		PanelSlopeData.add(CalcButton);
		
		button_imp.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
					JFileChooser dlg = new JFileChooser();
					dlg.setDialogTitle("Укажите имя файла и путь к нему");
					dlg.setFileFilter(new FileNameExtensionFilter("Файлы XML", "xml"));
					int r = dlg.showOpenDialog(CrossSectionCanvas);
					File file = dlg.getSelectedFile();
					if (file.exists()) 
					{
						LoadSubgrade(file);
						CalcButton.setEnabled(slopePointsListModel.getSize() == 2);
					}
			}
		});
		
		JButton ReportButton = new JButton("Отчёт");
		ReportButton.setBounds(10, 306, 134, 21);
		PanelSlopeData.add(ReportButton);
		ReportButton.setEnabled(false);
		ReportButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser dlg = new JFileChooser();
				dlg.setDialogTitle("Укажите имя файла и путь к нему");
				dlg.setFileFilter(new FileNameExtensionFilter("Файлы Microsoft Word DOCX", "docx"));
				int r = dlg.showSaveDialog(CrossSectionCanvas);
				File file = dlg.getSelectedFile();
				if (file.exists()) 
				{
					int ui = JOptionPane.showConfirmDialog(CrossSectionCanvas, "Файл "+file.getName() + " существует. Перезаписать?",
							"Предупреждение", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
					if (ui != JOptionPane.YES_OPTION) return;
				} 
				if (r == JFileChooser.APPROVE_OPTION) 
				{				
					if (!file.exists() && (getFileExtension(file.getName()) == null)) file = new File(file.getAbsolutePath() + ".docx");
					try 
					{
						SubgradeReportingThread th = new SubgradeReportingThread();	
						th.setSubgrade(subgrade, progressActionListener);
						th.setFile(file);
					
						InputStream template = getClass().getResourceAsStream("SubgradeReportTemplate.docx");
						th.setTemplateStream(template);
						th.setImage(CrossSectionCanvas.getScreenShot());
						th.setHatchImages(patternListModel);
						th.start();
					} catch (Exception e1) {
						JOptionPane.showMessageDialog(CrossSectionCanvas, "Проблема с созданием отчёта: "+e1.toString());
					}
				}
			}
			
		});
		
		progress = new JProgressBar();
		progress.setBounds(10, 337, 134, 20);
		PanelSlopeData.add(progress);
		progress.setIndeterminate(true);
		progress.setVisible(false);
		
		progressActionListener = new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent e) {
				switch (e.getID())
				{
					case 0: { 
						progress.setVisible(true);
						progress.setString(e.getActionCommand());
						progress.setStringPainted(true);
						break;
					}
					case 1: { 
						progress.setVisible(false);
						ReportButton.setEnabled(true);
						CrossSectionCanvas.repaint();
						//repaint();
						break;
					}
					case 2: { 
						JOptionPane.showMessageDialog(CrossSectionCanvas, e.getActionCommand());
						//repaint();
						break;
					}
					case 3:
					{
						progress.setVisible(false);
						ReportButton.setEnabled(true);
						for (Point2D.Double pt: subgrade.SlopePoints) slopePointsListModel.addElement(pt);
						CrossSectionCanvas.repaint();
						//repaint();
					}
				}		
			}
		};
		
		CalcButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {		
				SubgradeAnalyseThread th = new SubgradeAnalyseThread();	
				th.setSubgrade(subgrade, progressActionListener);
				th.start();
			}
		});
		
		tabbedPane.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				// Все нажатые JToggleButton "отжимаем"
				bg.clearSelection();
				if (SlopeButton.isSelected()) SlopeButton.doClick();
			}
		});
		
		slopePointsListModel.addListDataListener(new ListDataListener() {
			
			@Override
			public void intervalRemoved(ListDataEvent e) {
				CalcButton.setEnabled(slopePointsListModel.getSize() == 2);
			}
			
			@Override
			public void intervalAdded(ListDataEvent e) {
				CalcButton.setEnabled(slopePointsListModel.getSize() == 2);
			}
			
			@Override
			public void contentsChanged(ListDataEvent e) {
				CalcButton.setEnabled(slopePointsListModel.getSize() == 2);
			}
		});
		
		SlopeButton.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				// Режим задания линий откоса
				if (SlopeButton.isSelected()) soilEditor.setMode(SubgradeSoilEditor.EM_SLOPE);
				else soilEditor.setMode(SubgradeSoilEditor.EM_SLOPE);
			}
		});
		
		SlopePointList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent e) {
				ButtonDeletePoint.setEnabled(!SlopePointList.isSelectionEmpty());
			}
		});
		
		JPanel panel = new JPanel();
		layeredPane.add(panel, "name_28733959370219");
		
		JLabel label_2 = new JLabel("\u0423\u0434\u0435\u043B\u044C\u043D\u044B\u0439 \u0432\u0435\u0441 \u043A\u043E\u043C\u0430\u0440\u0438\u043A\u0430-\u0433\u0435\u044F");
		label_2.setAlignmentX(0.5f);
		panel.add(label_2);
		
		soilEditor = new SubgradeSoilEditor() {
			
			@Override
			public void setMode(int mode) {
				editMode = mode;
			}
			
			@Override
			public void onSoilChanged() {
				WeightEdit.setEnabled(selectedSoil != null);
				CohesionEdit.setEnabled(selectedSoil != null);
				FricAngleEdit.setEnabled(selectedSoil != null);
				SoilNameEdit.setEnabled(selectedSoil != null);
				PatternList.setEnabled(selectedSoil != null);
				ButtonSetProps.setEnabled(selectedSoil != null);
				
				if (selectedSoil != null)
				{
					WeightEdit.setText(String.format("%.2f", selectedSoil.getG()));
					CohesionEdit.setText(String.format("%.2f", selectedSoil.getC()));
					FricAngleEdit.setText(String.format("%.1f", selectedSoil.getF() * 180 / Math.PI));
					SoilNameEdit.setText(selectedSoil.getSoilName());
					PatternList.setSelectedIndex(selectedSoil.getHatchIndex());
				} else
				{
					SoilNameEdit.setText("");
					PatternList.setSelectedIndex(-1);
				}
				CrossSectionCanvas.repaint();
			}
			
			@Override
			public void onPointChanged() {
				PointXEdit.setEnabled(selectedPoint != null);
				PointYEdit.setEnabled(selectedPoint != null);
				ButtonChangeCoords.setEnabled(selectedPoint != null);
				ButtonChangeCoordsCancel.setEnabled(selectedPoint != null);
				
				if (selectedPoint != null)
				{
					PointXEdit.setText(String.format("%.2f", selectedPoint.x));
					PointYEdit.setText(String.format("%.2f", selectedPoint.y));
				};
				startPoint = selectedPoint;
				CrossSectionCanvas.repaint();
			}
			
			@Override
			public void onLineChanged() {
				LoadEditBox.setEnabled(selectedLine != null);
				ButtonDeleteLoad.setEnabled(selectedLine != null);
				ButtonSetLoad.setEnabled(selectedLine != null);
				
				if (selectedLine != null)
				{
					SubgradeLoad line_load = subgrade.GetLoad(selectedLine);
					if (line_load != null)
						 LoadEditBox.setText(String.format("%.2f", line_load.GetLoad()));
					else LoadEditBox.setText("0");
				} else LoadEditBox.setText("0");
				CrossSectionCanvas.repaint();
			}
			
			@Override
			public void SoilSelected(SubgradeSoil soil) {
				if (selectedSoil != soil)
				{
					selectedSoil = soil;
					onSoilChanged();
				}
			}
			
			@Override
			public void PointSelected(Point2D.Double pt) {
				if (selectedPoint != pt)
				{
					if (selectedPoint != null)
					{
						if (selectedPoint.equals(pt))
						{
							selectedPoint = null;
							onPointChanged();
							return;
						}
					} 
					selectedPoint = pt;
					onPointChanged();
				}
			}
			
			@Override
			public void LineSelected(java.awt.geom.Line2D.Double line) {
				if (selectedLine != line)
				{
					if ((selectedLine != null) && (line != null))
					{
						if (selectedLine.getP1().equals(line.getP1()) && selectedLine.getP2().equals(line.getP2()))
						{
							selectedLine = null;
							onPointChanged();
							return;
						} 
					}
					selectedLine = line;
					onPointChanged();
				}
			}
			
			@Override
			public SubgradeSoil getSoil() {
				return selectedSoil;
			}
			
			@Override
			public Double getPoint() {
				return selectedPoint;
			}
			
			@Override
			public int getMode() {
				return editMode;
			}
			
			@Override
			public java.awt.geom.Line2D.Double getLine() {
				return selectedLine;
			}

			@Override
			public void MouseOver(Double pt) {
				// Когда указатель мыши приближается к объекту на рисунке - 
				// точке или линии - "выбираем" этот объект, чтобы потом к нему привязаться,
				// если пользователь щёлкнет мышью
				highlightedLine = subgrade.LineNearby(pt.x, pt.y, NEAR_TOLERANCE);
				highlightedPoint = subgrade.PointNearby(pt.x, pt.y, NEAR_TOLERANCE);
				highlightedSoil = subgrade.SoilByPoint(pt);
				endPoint = pt;
				
				/*if (editMode == EM_ADD_LINE)*/ CrossSectionCanvas.repaint();
			}
			
			@Override
			public void MouseClicked(Double pt, int button) {
				
				PointSelected(subgrade.PointNearby(pt.x, pt.y, NEAR_TOLERANCE));
				LineSelected(subgrade.LineNearby(pt.x, pt.y, NEAR_TOLERANCE));
				SoilSelected(subgrade.SoilByPoint(pt));
				
				// Когда пользователь перемещает курсор в режиме рисования новых линий 
				if ((newLayer != null) && (editMode == EM_ADD_LINE))
				{	
					startPoint = newLayer.LastPoint(); 
					endPoint = new Point2D.Double(pt.x, pt.y);
					subgrade.onGeometryChanged();
					slopePointsListModel.clear();
					CrossSectionCanvas.repaint();
				};
				
				// Если мы находимся в режиме удаления объекта
				if ((editMode == EM_DELETE_OBJECT) && (button == MouseEvent.BUTTON1))
				{	
					if (selectedPoint != null) 
					{	
						subgrade.DeletePoint(selectedPoint);
						subgrade.onGeometryChanged();
						slopePointsListModel.clear();
					}
					else if (selectedLine != null) 
						 {
						 	subgrade.DeleteLine(selectedLine);
						 	subgrade.onGeometryChanged();
						 	slopePointsListModel.clear();
						 }
					
					PointSelected(subgrade.PointNearby(pt.x, pt.y, NEAR_TOLERANCE));
					LineSelected(subgrade.LineNearby(pt.x, pt.y, NEAR_TOLERANCE));
					SoilSelected(subgrade.SoilByPoint(pt));
					
					CrossSectionCanvas.repaint();
					return;
				}
				
				// Если находимся в режиме задания склона к расчёту
				if ((editMode == EM_SLOPE) && (button == MouseEvent.BUTTON1))
				{
					if (selectedPoint != null) 
						slopePointsListModel.addElement(selectedPoint);
				}
				
				// Если нажимают правую кнопку - расцениваем это как отмену (завершение) 
				if (button == MouseEvent.BUTTON3) KeyPressed(KeyEvent.VK_ESCAPE);
				
				// Добавление точки на линию
				if (editMode == EM_ADD_POINT)
				{
					if (button == MouseEvent.BUTTON1)
					{
						if ((highlightedLine != null) && (highlightedPoint == null))
						{
							highlightedPoint = subgrade.SplitLineAt(highlightedLine, pt);	
							subgrade.onGeometryChanged();
							slopePointsListModel.clear();
							CrossSectionCanvas.repaint();
						}
					}
				}
						
				// Если мы находимся в режиме добавления линий
				if (editMode == EM_ADD_LINE) 
				{					
					if (button == MouseEvent.BUTTON1)
					{
						// Добавляем новую точку к создаваемому слою
						// Если пользователь щёлкнул в линию, то содаём на ней точку
						if ((highlightedLine != null) && (highlightedPoint == null))
						{
							highlightedPoint = subgrade.SplitLineAt(highlightedLine, pt);	
							pt = highlightedPoint;
						}
						
						// Если точка первая, т.е. начинаем чертить новый слой - то создаём этот новый слой
						if (newLayer == null) 
						{ 
							newLayer = subgrade.AddNewSoilLayer(pt);
							startPoint = pt;
							CrossSectionCanvas.repaint();
							return;
						}
					
						// Если попали в пустое место - создаём там точку
						if (highlightedPoint == null)
						{
							newLayer.AddPoint(pt.x, pt.y);
							startPoint = pt;	
						} else
							// Если попали в точку - добавляем её к слою
							{
								newLayer.AddPoint(highlightedPoint.x, highlightedPoint.y);
								startPoint = highlightedPoint;
							};
					}
					subgrade.onGeometryChanged();
					slopePointsListModel.clear();
					CrossSectionCanvas.repaint();
				}
				
			}
			
			@Override
			public void KeyPressed(int key) {
				if (key == KeyEvent.VK_ESCAPE) 
				{
					editMode = EM_NORMAL;
					bg.clearSelection();
					newLayer = null;
					startPoint = null;
					endPoint = null;
					SoilSelected(null);
					LineSelected(null);
					PointSelected(null);
					CrossSectionCanvas.repaint();
				}
			}

			@Override
			public void Paint(Graphics2D g2d, double dx, double dy) {
				if ((newLayer != null) && (editMode == EM_ADD_LINE) && 
					(startPoint != null) && (endPoint != null))
				{
					g2d.setColor(Color.red);
					g2d.draw(new Line2D.Double(startPoint.x, startPoint.y, endPoint.x, endPoint.y));
				}
			
				if (highlightedPoint != null)
				{
					double cr = Math.max(dx, dy) * 0.005;
					g2d.setColor(Color.orange);
					g2d.fill(new Ellipse2D.Double(highlightedPoint.x-cr, highlightedPoint.y-cr, 2*cr, 2*cr));
				}
				
				if (selectedPoint != null)
				{
					double cr = Math.max(dx, dy) * 0.007;
					g2d.setColor(Color.ORANGE);
					g2d.fill(new Ellipse2D.Double(selectedPoint.x-cr, selectedPoint.y-cr, 2*cr, 2*cr));
				}
			
				if (highlightedLine != null)
				{
					g2d.setColor(Color.orange);
					g2d.draw(highlightedLine);
				}
				
				if (selectedLine != null)
				{
					g2d.setColor(Color.YELLOW);
					BasicStroke s = (BasicStroke)g2d.getStroke();
					g2d.setStroke(new BasicStroke(s.getLineWidth() * 3));
					g2d.draw(selectedLine);
					g2d.setStroke(s);
				}
				
			}
		};
		
		CrossSectionCanvas.setSoilEditor(soilEditor);		
	}
	
	protected void LoadSubgrade(File file) 
	{
		SubgradeImportThread th = new SubgradeImportThread();	
		th.setSubgrade(subgrade, progressActionListener);
		th.setFile(file);
		th.start();
	}

	protected void SaveSubgrade(File selectedFile) 
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder doc_builder;
		try {
			doc_builder = factory.newDocumentBuilder();
			Document doc = doc_builder.newDocument();
			Transformer tr = TransformerFactory.newInstance().newTransformer();
			try {
				subgrade.Save(doc);
				FileOutputStream fs = new FileOutputStream(selectedFile);
				tr.transform(new DOMSource(doc), new StreamResult(fs)); 
				fs.flush();
				fs.close();
			} catch (FileNotFoundException e1) {
				JOptionPane.showMessageDialog(CrossSectionCanvas, "Не найден файл: "+e1.toString());
				e1.printStackTrace();
			} catch (TransformerException e) {
				JOptionPane.showMessageDialog(CrossSectionCanvas, "Ошибка XML: "+e.toString());
				e.printStackTrace();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(CrossSectionCanvas, "Ошибка ввода/вывода: "+e.toString());
				e.printStackTrace();
			};	
		} catch (ParserConfigurationException e) {
			JOptionPane.showMessageDialog(CrossSectionCanvas, "Ошибка разбора XML: "+e.toString());
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			JOptionPane.showMessageDialog(CrossSectionCanvas, "Ошибка конфигурации XML: "+e.toString());
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			JOptionPane.showMessageDialog(CrossSectionCanvas, "Ошибка XML: "+e.toString());
			e.printStackTrace();
		}
	}

	public void init(int InitType)
	{
	  /* Инициализация приложения
		 Возможны три варианта
		 1. Инициализация "впервые" - по данным профиля из ЕК АСУИ ЗП
		 2. Инициализация "повторная" - инициализация по полным данным для расчёта
		 3. Инициализация "с нуля" - пустое, все исходные данные вводятся пользователем
	  */
		switch (InitType)
		{
			case 2: InitFullData(); break;
			default: InitBlank(); break;
		}
	}
	
	private void InitFullData()
	{
		// TODO Загрузка из полного описания (как xml-файл экспорта)
	}
	
	private void InitBlank()
	{
		// Загрузка "с нуля"
		subgrade = new Subgrade();
		subgrade.setPatternList(patternListModel);
		CrossSectionCanvas.setSubgrade(subgrade);
		slopePointsListModel.setSubgrade(subgrade);
	}	
	
	private static String getFileExtension(String name) 
	{	    
	    int lastIndexOf = name.lastIndexOf(".");
	    if (lastIndexOf == -1) {
	        return null; // empty extension
	    }
	    return name.substring(lastIndexOf);
	};
}
