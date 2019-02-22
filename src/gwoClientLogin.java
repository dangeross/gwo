import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.StringTokenizer;
import java.util.Vector;

class gwoClientLogin extends JFrame implements ActionListener, AdjustmentListener {
	
	// VARIABLES
	
	private gwoClient gc;
	private JTabbedPane tabbedPane;
	private JButton okButton, cancelButton;
	
	// General
	private JLabel displayNameLabel, uidLabel, passLabel, serverLabel;
	private JTextField displayNameField, uidField, serverField;
	private JPasswordField passField;
	private JComboBox serverCombo, uidCombo;
	private JButton addServerButton, delServerButton;
	
	// Proxy
	private JCheckBox useProxyBox;
	private JLabel proxyServerLabel, proxyPortLabel;
	private JTextField proxyServerField, proxyPortField;
	
	// Graphics
	private JCheckBox backAABox, foreAABox, renderBox, soundBox, fontBox;
	private JLabel clockLabel, skipLabel;
	private JTextField clockField, skipField;
	private JScrollBar clockScroll, skipScroll;
	//private JSlider clockSlider, skipSlider;
	
	// Sound
	private JLabel volumeLabel;
	private JScrollBar volumeScroll;
	private JButton volumeButton;
	
	// Register
	private JButton registerButton = new JButton();
	private JButton searchButton = new JButton();
	private JButton useButton = new JButton();
	
	// Crap
	private Timer gClock;
	private byte registerState = BLANK;
	private byte servListState = BLANK;
	private byte searchState = BLANK;
	private String result = "";
	
	public static byte BLANK    = 0;
	public static byte IN_PROG  = 1;
	public static byte FINISHED = 2;
	
	public gwoClientLogin(gwoClient gc) {
		super("GWO Login");
		this.gc = gc;
		
		setIconImage(gc.icon);
		
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt)
				{
					close();
				}
			}
		);
			
		okButton = new JButton("Ok");
		okButton.addActionListener(this);
		okButton.setActionCommand("$ok");
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("$cancel");
		
		tabbedPane = new JTabbedPane();			
		tabbedPane.addTab("Connection", new ImageIcon(""), makeConnectionTab(), "Connection Settings");
		
		if(gc.uid != 0) {
			tabbedPane.addTab("Graphics", new ImageIcon(""), makeGraphicsTab(), "Graphics Settings");
			tabbedPane.addTab("Sound", new ImageIcon(""), makeSoundTab(), "Sound Settings");
			tabbedPane.addTab("Custom Server", new ImageIcon(""), makeServerTab(), "Add/Remove Custom Servers");
			tabbedPane.addTab("User", new ImageIcon(""), makeLoginTab(), "Login Details");
			tabbedPane.setSelectedIndex(4);
		}
		else {
			tabbedPane.addTab("Register", new ImageIcon(""), makeRegSelectionTab(), "Registration");
			okButton.setEnabled(false);
			tabbedPane.setSelectedIndex(0);
		}
		
		JPanel footerPane = new JPanel();
		footerPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		footerPane.add(new JLabel("Press 'Ok' to view Servers available"));
		footerPane.add(okButton);
		footerPane.add(cancelButton);
		
		JPanel contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(tabbedPane, BorderLayout.CENTER);
		contentPane.add(footerPane, BorderLayout.SOUTH);

		setContentPane(contentPane);
		//pack();
		setSize(400, 270);
		setResizable(false);
		
		setLocation(0, gc.height-getSize().height);
		setVisible(true);
		
		gClock = new Timer(1000, updateGUI);
		gClock.start();
		
		gc.repaint();
	}
	
	public void close() {
		setVisible(false);
		gc.close();	
	}
	
	public void adjustmentValueChanged(AdjustmentEvent e) {
		volumeLabel.setText("Volume: ["+volumeScroll.getValue()+"%] ");
		clockLabel.setText("Fps: ["+clockScroll.getValue()+"] ");
		skipLabel.setText("Skip Frames: ["+skipScroll.getValue()+"] ");
	}
	
	private Component makeGraphicsTab() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		backAABox = new JCheckBox("Background AntiAlias", gc.backgroundAntiAlias);
		foreAABox = new JCheckBox("Foreground AntiAlias", gc.backgroundAntiAlias);
		renderBox = new JCheckBox("Render For Quality", gc.renderForQuality);
		fontBox = new JCheckBox("Large Fonts", gc.largeFont);
		
		skipLabel = new JLabel("Skip Frames: ["+gc.frameSkipTo+"] ");
		skipField = new JTextField(""+gc.frameSkipTo, 4);
		//clockLabel = new JLabel("Clock Speed (1000=1sec 500=0.5sec): ");
		clockLabel = new JLabel("Fps: ["+gc.fps+"] ");
		clockField = new JTextField(""+gc.fps, 4);

		clockScroll = new JScrollBar(JScrollBar.HORIZONTAL, gc.fps, 0, 1, 200);
		clockScroll.setBlockIncrement(10);
		clockScroll.addAdjustmentListener(this);
		skipScroll = new JScrollBar(JScrollBar.HORIZONTAL, gc.frameSkipTo, 0, 0, 10);
		skipScroll.setBlockIncrement(1);
		skipScroll.addAdjustmentListener(this);
			
		JPanel labelPane = new JPanel();
		labelPane.setLayout(new GridLayout(0, 1, 1, 1));
		labelPane.add(clockLabel);
		labelPane.add(skipLabel);

		JPanel fieldPane = new JPanel();
		fieldPane.setLayout(new GridLayout(0, 1, 1, 1));
		//fieldPane.add(clockField);
		//fieldPane.add(skipField);
		fieldPane.add(clockScroll);
		fieldPane.add(skipScroll);
		
		JPanel headerPane = new JPanel();
		headerPane.setLayout(new GridLayout(4, 1));
		headerPane.add(backAABox);
		headerPane.add(foreAABox);
		headerPane.add(renderBox);
		headerPane.add(fontBox);
		
		JPanel footerPane = new JPanel();
		footerPane.setBorder(BorderFactory.createTitledBorder("Refresh"));
		footerPane.setLayout(new BoxLayout(footerPane, BoxLayout.X_AXIS));
		footerPane.add(labelPane);
		footerPane.add(fieldPane);
		
		panel.add("Center", headerPane);
		panel.add("South", footerPane);
		
		return panel;
	}
	
	private Component makeSoundTab() {
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		//panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		soundBox = new JCheckBox("Play Sounds", gc.sound);		
		volumeLabel = new JLabel("Volume: ["+gc.soundVolume+"%] ");
		volumeScroll = new JScrollBar(JScrollBar.HORIZONTAL, gc.soundVolume, 0, 0, 100);
		volumeScroll.setBlockIncrement(10);
		volumeScroll.addAdjustmentListener(this);
		volumeButton = new JButton("Test");
		volumeButton.setActionCommand("$test_volume");
		volumeButton.addActionListener(this);
		
		JPanel headerPane = new JPanel();
		headerPane.setLayout(new GridLayout(1, 1));
		headerPane.add(soundBox);
		
		JPanel footerPane = new JPanel();
		footerPane.setLayout(new BorderLayout());
		footerPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		footerPane.add("North", volumeLabel);
		footerPane.add("Center", volumeScroll);
		footerPane.add("East", volumeButton);
		
		panel.add("North", headerPane);
		panel.add("South", footerPane);
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		p.add("North", panel);
		
		return p;
	}
	
	private Component makeConnectionTab() {
		JPanel panel = new JPanel(new BorderLayout());
		//panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		useProxyBox = new JCheckBox("Use Proxy to Connect", gc.useProxy);

		proxyServerLabel = new JLabel("Proxy Server Address: ");
		proxyServerField = new JTextField(gc.proxyServer, 15);
		proxyPortLabel = new JLabel("Proxy Server Port: ");
		proxyPortField = new JTextField(""+gc.proxyPort, 15);

		JPanel headerPane = new JPanel();
		headerPane.setLayout(new BoxLayout(headerPane, BoxLayout.Y_AXIS));
		headerPane.add(useProxyBox);
		
		JPanel labelPane = new JPanel();
		labelPane.setLayout(new GridLayout(0, 1, 1, 1));
		labelPane.add(proxyServerLabel);
		labelPane.add(proxyPortLabel);

		JPanel fieldPane = new JPanel();
		fieldPane.setLayout(new GridLayout(0, 1, 1, 1));
		fieldPane.add(proxyServerField);
		fieldPane.add(proxyPortField);

		JPanel footerPane = new JPanel();
		footerPane.setBorder(BorderFactory.createTitledBorder("Proxy Settings"));
		//footerPane.setLayout(new BoxLayout(footerPane, BoxLayout.X_AXIS));
		footerPane.setLayout(new BorderLayout());
		footerPane.add("Center", labelPane);
		footerPane.add("East", fieldPane);

		JPanel masterPane = new JPanel();
		masterPane.setLayout(new BorderLayout());
		masterPane.add(headerPane, BorderLayout.NORTH);
		masterPane.add(footerPane, BorderLayout.CENTER);
		
		panel.add(masterPane, BorderLayout.NORTH);
		
		return panel;
	}
	
	private Component makeServerTab() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		serverField = new JTextField("", 15);
		addServerButton = new JButton("Add");
		addServerButton.setActionCommand("$add_server");
		addServerButton.addActionListener(this);		
		serverCombo = new JComboBox(gc.serverList);
		delServerButton = new JButton("Remove");
		delServerButton.setActionCommand("$del_server");
		delServerButton.addActionListener(this);
		
		JPanel labelPane = new JPanel();
		labelPane.setLayout(new GridLayout(0, 1, 1, 1));
		labelPane.add(new JLabel("Add Server: "));
		labelPane.add(new JLabel("Del Server: "));

		JPanel fieldPane = new JPanel();
		fieldPane.setLayout(new GridLayout(0, 1, 1, 1));
		fieldPane.add(serverField);
		fieldPane.add(serverCombo);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new GridLayout(0, 1, 1, 1));
		buttonPane.add(addServerButton);
		buttonPane.add(delServerButton);
		
		JPanel masterPane = new JPanel(new BorderLayout());
		masterPane.setBorder(BorderFactory.createTitledBorder("Add/Remove Custom Servers"));
		//masterPane.add(labelPane, BorderLayout.WEST);
		masterPane.add(fieldPane, BorderLayout.CENTER);
		masterPane.add(buttonPane, BorderLayout.EAST);
		
		panel.add(masterPane, BorderLayout.NORTH);		
		
		return panel;
	}

	private Component makeLoginTab() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		displayNameLabel = new JLabel("Display Name: ");
		displayNameField = new JTextField(gc.displayName, 15);
		uidLabel = new JLabel("UID: ");
		uidField = new JTextField(""+gc.tempUid, 15);
		uidField.setEditable(false);
		passLabel = new JLabel("Password: ");
		passField = new JPasswordField(""+gc.password, 15);
		passField.setEchoChar('*');
	
		JPanel labelPane = new JPanel();
		labelPane.setLayout(new GridLayout(0, 1, 1, 1));
		labelPane.add(displayNameLabel);
		labelPane.add(uidLabel);
		labelPane.add(passLabel);

		JPanel fieldPane = new JPanel();
		fieldPane.setLayout(new GridLayout(0, 1, 1, 1));
		fieldPane.add(displayNameField);
		fieldPane.add(uidField);
		fieldPane.add(passField);

		JPanel footerPane = new JPanel();
		footerPane.setBorder(BorderFactory.createTitledBorder("User Settings"));
		//footerPane.setLayout(new BoxLayout(footerPane, BoxLayout.X_AXIS));
		footerPane.setLayout(new BorderLayout());
		footerPane.add("Center", labelPane);
		footerPane.add("East", fieldPane);

		JPanel masterPane = new JPanel();
		masterPane.setLayout(new BorderLayout());
		masterPane.add(footerPane, BorderLayout.CENTER);
		//masterPane.add(new JLabel("Press 'Ok' to view Servers available"), BorderLayout.SOUTH);
		//masterPane.add(panel4, BorderLayout.CENTER);
		
		panel.add(masterPane, BorderLayout.NORTH);
		
		return panel;
	}
	
	private Component makeRegSelectionTab() {
		JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		JButton oldUserButton = new JButton("OLD PLAYER (Registered Before)");
		oldUserButton.setActionCommand("$oldUser");
		oldUserButton.addActionListener(this);
		
		JButton newUserButton = new JButton("NEW PLAYER");
		newUserButton.setActionCommand("$newUser");
		newUserButton.addActionListener(this);
		
		panel.add(oldUserButton);
		panel.add(newUserButton);
		
		return panel;
	}
	
	private Component makeOldUserTab() {
		JPanel panel = new JPanel(new BorderLayout());
		//panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		displayNameLabel = new JLabel("Display Name: ");
		displayNameField = new JTextField(gc.displayName, 15);
		uidLabel = new JLabel("UID (0 Found): ");
		uidCombo = new JComboBox();

		JPanel headerPane = new JPanel();
		headerPane.setLayout(new BoxLayout(headerPane, BoxLayout.Y_AXIS));
		headerPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		headerPane.add(new JLabel("Enter your last full display name or part of"));
		headerPane.add(new JLabel("it and GwoNET will attempt to find your UID."));
		
		searchButton = new JButton("Search");
		searchButton.setActionCommand("$search");
		searchButton.addActionListener(this);
		
		useButton = new JButton("Use This UID");
		useButton.setActionCommand("$use_uid");
		useButton.addActionListener(this);
		useButton.setEnabled(false);
	
		JPanel labelPane = new JPanel();
		labelPane.setLayout(new GridLayout(0, 1, 1, 1));
		labelPane.add(displayNameLabel);
		labelPane.add(uidLabel);
		labelPane.add(new JLabel(" "));

		JPanel fieldPane = new JPanel();
		fieldPane.setLayout(new GridLayout(0, 1, 1, 1));
		fieldPane.add(displayNameField);
		fieldPane.add(uidCombo);
		fieldPane.add(searchButton);

		JPanel footerPane = new JPanel();
		footerPane.setBorder(BorderFactory.createTitledBorder("User Details"));
		footerPane.setLayout(new BoxLayout(footerPane, BoxLayout.X_AXIS));
		footerPane.add(labelPane);
		footerPane.add(fieldPane);

		JPanel masterPane = new JPanel();
		//masterPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		masterPane.setLayout(new BorderLayout());
		masterPane.add(headerPane, BorderLayout.NORTH);
		masterPane.add(footerPane, BorderLayout.CENTER);
		masterPane.add(useButton, BorderLayout.SOUTH);
		
		panel.add(masterPane, BorderLayout.CENTER);
		
		return panel;
	}
	
	private Component makeNewUserTab() {
		JPanel panel = new JPanel(new BorderLayout());
		//panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		displayNameLabel = new JLabel("Display Name: ");
		displayNameField = new JTextField(gc.displayName, 15);
		uidLabel = new JLabel("UID: ");
		uidField = new JTextField(""+gc.uid, 15);
		passLabel = new JLabel("Password: ");
		passField = new JPasswordField(""+gc.password, 15);
		passField.setEchoChar('*');

		JPanel headerPane = new JPanel();
		headerPane.setLayout(new BoxLayout(headerPane, BoxLayout.Y_AXIS));
		headerPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		headerPane.add(new JLabel("A UID (User ID) is required before you can"));
		headerPane.add(new JLabel("login to a server.  Enter a name and"));
		headerPane.add(new JLabel("password to get a new UID from GwoNET."));
		
		registerButton = new JButton("Register");
		registerButton.setActionCommand("$register");
		registerButton.addActionListener(this);
	
		JPanel labelPane = new JPanel();
		labelPane.setLayout(new GridLayout(0, 1, 1, 1));
		labelPane.add(displayNameLabel);
		labelPane.add(uidLabel);
		labelPane.add(passLabel);

		JPanel fieldPane = new JPanel();
		fieldPane.setLayout(new GridLayout(0, 1, 1, 1));
		fieldPane.add(displayNameField);
		fieldPane.add(uidField);
		fieldPane.add(passField);

		JPanel footerPane = new JPanel();
		footerPane.setBorder(BorderFactory.createTitledBorder("User Details"));
		footerPane.setLayout(new BoxLayout(footerPane, BoxLayout.X_AXIS));
		footerPane.add(labelPane);
		footerPane.add(fieldPane);

		JPanel masterPane = new JPanel();
		masterPane.setLayout(new BorderLayout());
		masterPane.add(headerPane, BorderLayout.NORTH);
		masterPane.add(footerPane, BorderLayout.CENTER);
		masterPane.add(registerButton, BorderLayout.SOUTH);
		
		panel.add(masterPane, BorderLayout.NORTH);
		
		return panel;
	}
	
	ActionListener updateGUI = new ActionListener() {
	
		public void actionPerformed(ActionEvent e) {
			if(registerState == BLANK) {
				registerButton.setText("Register");
			}
			else if(registerState == IN_PROG) {
				registerButton.setText("Processing...");
			}
			else if(registerState == FINISHED) {
				registerButton.setText("Finished");
			
				if(result.equals("")) {
					registerButton.setText("Failed to Connect");
				}
				else if(result.equals("0")) {
					registerButton.setText("Invalid Name/UID/Password");
				}
				else {
					registerButton.setText("Success");
					gc.displayName = displayNameField.getText();
					gc.password = passField.getText();
					gc.tempUid = new Integer(result).intValue();
					gc.uid = new Integer(result).intValue();
				
					tabbedPane.remove(1);
					tabbedPane.addTab("Graphics", new ImageIcon(""), makeGraphicsTab(), "Graphics Settings");
					tabbedPane.addTab("Sound", new ImageIcon(""), makeSoundTab(), "Sound Settings");
					tabbedPane.addTab("Custom Server", new ImageIcon(""), makeServerTab(), "Add/Remove Custom Servers");
					tabbedPane.addTab("User", new ImageIcon(""), makeLoginTab(), "Login Details");
					tabbedPane.setSelectedIndex(4);
					//okButton.setEnabled(true);
					//pack();
					setSize(400, 270);
					setLocation(0, gc.height-getSize().height);
				}	
				
				registerState = BLANK;			
			}
			
			if(searchState == BLANK) {
				searchButton.setText("Search");
			}
			else if(searchState == IN_PROG) {
				searchButton.setText("Processing...");
			}
			else if(searchState == FINISHED) {
				searchButton.setText("Finished");
				
				if(!result.equals("")) {
					StringTokenizer st = new StringTokenizer(result, "+");
					uidLabel.setText("UID ("+st.countTokens()+" Found): ");
					uidCombo.removeAllItems();
					while(st.hasMoreTokens()) {
						uidCombo.addItem(st.nextToken());
					}
					useButton.setEnabled(true);
				}
				else {
					useButton.setEnabled(false);
				}
				
				searchState = BLANK;
			}
		}
	};
	
	public void register(String r) {
		registerState = FINISHED;
		result = r;	
	}
	
	public void serverList(String r) {		
		servListState = FINISHED;
		result = r;
	}
	
	public void search(String r) {		
		searchState = FINISHED;
		result = r;
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		//System.out.println(cmd);
		
		if(cmd.equals("$ok")) {
			gc.useProxy = useProxyBox.isSelected();
			gc.proxyServer = proxyServerField.getText();
			gc.proxyPort = new Integer(proxyPortField.getText()).intValue();
			gc.displayName = displayNameField.getText();
			gc.password = passField.getText();
			gc.uid = new Integer(uidField.getText()).intValue();
			gc.tempUid = gc.uid;
			gc.selectedServer = (String)serverCombo.getSelectedItem();
		
			gc.backgroundAntiAlias = backAABox.isSelected();
			gc.foregroundAntiAlias = foreAABox.isSelected();
			gc.renderForQuality = renderBox.isSelected();
			gc.sound = soundBox.isSelected();
			gc.soundVolume = volumeScroll.getValue();
			gc.largeFont = fontBox.isSelected();
			gc.frameSkipTo = skipScroll.getValue();
			gc.fps = clockScroll.getValue();
			
			if(gc.serverSelection())
				dispose();
		}
		else if(cmd.equals("$add_server")) {
			if(serverField.getText().length() > 0) {
				gc.serverList.addElement(serverField.getText());
				serverField.setText("");
				
				serverCombo.setSelectedIndex(serverCombo.getItemCount()-1);
			}
		}
		else if(cmd.equals("$del_server")) {
			int index = serverCombo.getSelectedIndex();
			
			if(index > -1) {
				gc.serverList.removeElementAt(index);
				
				serverCombo.setSelectedIndex(serverCombo.getItemCount()-1);
			}
		}
		else if(cmd.equals("$oldUser")) {
			tabbedPane.setComponentAt(1, makeOldUserTab());
			pack();
			setLocation(0, gc.height-getSize().height);
		}
		else if(cmd.equals("$newUser")) {
			tabbedPane.setComponentAt(1, makeNewUserTab());
			pack();
			setLocation(0, gc.height-getSize().height);
		}
		else if(cmd.equals("$test_volume")) {
			boolean curSound = gc.sound;
			
			gc.sound = true;
			gc.soundVolume = volumeScroll.getValue();
			gc.playSound("explosion2.wav");
			gc.sound = curSound;
		}		
		else if(cmd.equals("$register")) {
			gc.useProxy = useProxyBox.isSelected();
			gc.proxyServer = proxyServerField.getText();
			gc.proxyPort = new Integer(proxyPortField.getText()).intValue();
			
			try {
				new Integer(uidField.getText());
			
				if(!displayNameField.getText().equals("") && !passField.getText().equals("")) {
					registerState = IN_PROG;
					gc.gwoNetRegister(displayNameField.getText(), uidField.getText(), passField.getText());
				}
				else {
					registerState = FINISHED;
					result = "0";
				}
			}
			catch(NumberFormatException nfe) {
				registerState = FINISHED;
				result = "0";
			}
		}
		else if(cmd.equals("$use_uid")) {
			gc.displayName = displayNameField.getText();
			gc.password = "";
			gc.uid = new Integer((String)uidCombo.getSelectedItem()).intValue();
			gc.tempUid = gc.uid;
				
			tabbedPane.remove(1);
			tabbedPane.addTab("Graphics", new ImageIcon(""), makeGraphicsTab(), "Graphics Settings");
			tabbedPane.addTab("Sound", new ImageIcon(""), makeSoundTab(), "Sound Settings");
			tabbedPane.addTab("Custom Server", new ImageIcon(""), makeServerTab(), "Add/Remove Custom Servers");
			tabbedPane.addTab("User", new ImageIcon(""), makeLoginTab(), "Login Details");
			tabbedPane.setSelectedIndex(1);
			//okButton.setEnabled(true);
			//pack();
			setSize(400, 270);
			setLocation(0, gc.height-getSize().height);
		}
		else if(cmd.equals("$search")) {
			gc.useProxy = useProxyBox.isSelected();
			gc.proxyServer = proxyServerField.getText();
			gc.proxyPort = new Integer(proxyPortField.getText()).intValue();
			
			if(!displayNameField.getText().equals("")) {
				searchState = IN_PROG;
				gc.gwoNetSearch(displayNameField.getText());
			}
		}
		else if(cmd.equals("$cancel")) {
			setVisible(false);
			gc.close();
		}
	}
}
