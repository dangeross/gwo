// gwo-server01.game-host.org:2000
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Calendar;
import java.lang.*;

public class gwoServer extends JFrame implements ActionListener {
	// VARIABLES
	
	private int version = 32;
	private int sid = 0;
	
	private int messageId = 0;
	
	private int width = 900, height = 700;
	public String systemUser, systemDir, systemSeparator;
	
	private ServerSocket ss;
	
	private Vector preUserList = new Vector();
	private Vector userList = new Vector();
	private Vector aiList = new Vector();
	private Vector teamList = new Vector();
	private Vector watchList = new Vector();
	private Vector kickList = new Vector();
	private Vector banList = new Vector();
	
	private Vector planets = new Vector();
	private double totalMass = 0;
	private Vector mines = new Vector();
	private Vector players = new Vector();
	private Vector stats = new Vector();
	
	private Timer clock, backupClock;
	private boolean clockValid = false;
	private boolean stateFinished = true;
	
	private long secondsRunning = 0;
	private int gameClock = 0, pingClock = 0, timeoutClock = 0;
	
	private long firedTime=0;
	
	public int gamesPlayed = 0;
	public int dataIn = 0, dataOut = 0;
	public long totalDataIn = 0, totalDataOut = 0;
	
	public Image icon;
	
	private gwoByteBuffer gbb = new gwoByteBuffer();
	private byte gameType = TYPE_SINGLE_GAME;
	private byte gameState = STATE_WAIT_FOR_PLAYERS;
	private int gamePoints = 0;
	
	private boolean atSynchPoint = false;
	
	private int usersOnline = 0;
	private int usersPlaying = 0;
	private int usersFired = 0;
	private int usersHyper = 0;
	private int usersWatching = 0;
	private int usersDead = 0;
	private int usersFinished = 0;
	private int aiPlaying = 0;
	private int aiFired = 0;
	private int aiHyper = 0;
	private int aiDead = 0;
	
	private int aiController = 0;
	
	private byte voteSingleGame = 0;
	private byte voteTeamGame = 0;
	private byte voteAiGame = 0;
	
	private int scenarioPlaying = 0;
	private String scenarioPlayingText = "None";

	private Color[] colorArray = {Color.green, Color.cyan, Color.orange, Color.magenta,
	 								Color.yellow, Color.pink, Color.red, Color.blue};
	private Color[] teamColorArray = colorArray;
	
	// File Transfer
	private FileInputStream ftFis;
	private boolean ftInProg = false;
	private File ftFile = new File("gwo2.jar");
	private long ftLastSegSent=0;
	private int ftUid=0;
	private int ftSize=0;
	private int ftNum=0;
	private int ftSegNum = 0;
	private byte[] ftData;
	private Vector ftQueue = new Vector();
	
	// GwoNET
	private gwoNetDataLink gndl;
	private gwoServerIpChecker gsic;
	public boolean gwoNetOnline = false;
	
	// Settings
	public boolean checkIp=true;
	public int port=2000;
	public boolean haveDyndns=false;
	public String dyndnsUsername="";
	public String dyndnsPassword="";
	public String dyndnsUrl="";
	public String adminPassword="";
	public boolean loadLeague=true;
	public boolean debug=false;
	public int saveNum = 0;
	public String title = "";
	public String description = "";
	public int maxPlayers = 0;
	
	private BufferedWriter bw;
	
	// GUI main	
	JPanel setupPane, runPane;
	// GUI setup
	JComboBox profileCombo;
	JCheckBox dyndnsBox, loadLeagueBox, useProxyBox;
	JLabel dNameLabel, dPassLabel, dUrlLabel, aPassLabel, portLabel;
	JTextField dNameField, dUrlField, portField, playersField, titleField;
	JScrollPane descScrollPane;
	JTextArea descArea;
	JPasswordField dPassField, aPassField;
	JButton startButton;
	
	// CLASSES
	
	class userElement {
		String displayName;
		String password;
		int uid;
		
		boolean online;
		boolean ping;
		int lastRequest;
		long finishTime;
		
		// status
		boolean playing;
		boolean watching;
		boolean dead;
		boolean diedThisShot;	
		boolean fired;
		boolean hyper;
		boolean finished;
		boolean voteDraw;
		int team;
		boolean idle;
		
		// connection
		Socket s;
		gwoDataInputStream ios;
		gwoDataOutputStream dos;
		gwoServerClientListener gsl;
		
		//Missile collide
		boolean missileCollide;
		int collideUid;
		byte collideNumber;
		
		// league
		int played;	
		int won, lost, drawn;
		int killsFor, killsAgainst;
		int points;
	}
	
	class aiElement {
		String displayName;
		int uid;
		
		double iq;
		int team;
		int shields;
		
		boolean dead;
		boolean diedThisShot;	
		boolean fired;
		boolean hyper;
		
		//Missile collide
		boolean missileCollide;
		int collideUid;
		byte collideNumber;
		
		// Targeting data
		double tX;
		double tY;
		int target;
		int shots;
		int bestAngle;
		int bestPower;
		double closestPath;
	}
	
	class teamElement {
		int usersPlaying;
		int usersDead;
		boolean diedThisShot;
	}
	
	class statsElement {
		String displayName;
		int uid;
		
		int played;	
		int won, lost, drawn;
		int killsFor, killsAgainst;
		int points;		
		
		int shots;
		int bulletLife;
		int kills;
		int hits;
		boolean hitOwn;
		boolean hitTeam;
		int shield;
		boolean mineKill;
		boolean wormKill;
	}
	
	class kickElement {
		String ip;
		int uid;
		long kickTime;
	}
	
	class banElement {
		String ip;
		int uid;
	}
	
	// Message Storage
	
	private int maxStorageSize = 40;
	private Vector emptyMessageStorage = new Vector();
	class emptyMessage {
		int msgId;
		int cmdId;
		int fromUid;
		int toUid;
	}
	
	private Vector messageStorage = new Vector();
	class message {
		int msgId;
		int cmdId;
		int fromUid;
		int toUid;
		
		int[] data;
	}
	
	// CONSTANTS
	
	public static final int SERVER = 0;
	
	private static final int TYPE_SINGLE_GAME            = 0;
	private static final int TYPE_SINGLE_SHIELDS_GAME    = 1;
	private static final int TYPE_TEAM_GAME              = 2;
	private static final int TYPE_TEAM_SHIELDS_GAME      = 3;
	private static final int TYPE_HVM_AI_GAME            = 4;
	private static final int TYPE_HVM_AI_SHIELDS_GAME    = 5;
	private static final int TYPE_SINGLE_AI_GAME         = 6;
	private static final int TYPE_SINGLE_AI_SHIELDS_GAME = 7;
	private static final int TYPE_TEAM_AI_GAME           = 8;
	private static final int TYPE_TEAM_AI_SHIELDS_GAME   = 9;
	
	private static final byte GAME_NONE   = 0;
	private static final byte GAME_SINGLE = 1;
	private static final byte GAME_TEAM   = 2;
	private static final byte GAME_AI     = 3;
	
	private static final byte STATE_WAIT_FOR_PLAYERS    = 0;
	private static final byte STATE_WAIT_FOR_NEW_GAME   = 1;
	private static final byte STATE_GENERATE            = 2;
	private static final byte STATE_NEW_SHOT            = 3;
	private static final byte STATE_WAIT_FOR_SHOT1      = 4;
	private static final byte STATE_WAIT_FOR_SHOT2      = 5;
	private static final byte STATE_WAIT_FOR_FINISHED1  = 6;
	private static final byte STATE_WAIT_FOR_FINISHED2  = 7;
	private static final byte STATE_WAIT_FOR_FINISHED3  = 8;
	private static final byte STATE_WAIT_FOR_FINISHED4  = 9;
	private static final byte STATE_HYPERSPACE          = 10;
	private static final byte STATE_CALCULATE_AWARDS    = 11;
	private static final byte STATE_END_OF_GAME         = 12;

	private static final int SEND_ALL        = 0;
	private static final int SEND_ACTIVE     = 1;
	private static final int SEND_INACTIVE   = 2;
	private static final int SEND_DEAD       = 3;
	private static final int SEND_NOT_DEAD   = 4;
	private static final int SEND_ALL_EXCEPT = 5;
	private static final int SEND_WATCHING   = 6;
	
	public static void main(String args[]) {
		new gwoServer();
	}
	
	public gwoServer() {
		systemUser = System.getProperty("user.name");
		systemDir = System.getProperty("user.home");
		systemSeparator = System.getProperty("file.separator");
		
		setTitle("GWO Server 2.0."+version);
		
		File f = new File(systemDir+systemSeparator);
		String[] fileList = f.list();
		
		for(int i = 0;i < fileList.length;i++) {
			if(fileList[i].startsWith("gwoServerSettings")) {
				readSettings(fileList[i]);
				i = fileList.length;
			}	
		}
		
		icon = getToolkit().createImage(this.getClass().getResource("icon.gif"));
		
		createGUI();
	
		pack();
		//setSize(360, 200);	

		setIconImage(icon);

		setVisible(true);
		
		profileCombo.setActionCommand("$profileChange");
		profileCombo.addActionListener(this);
		
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt)
				{	
					if(gwoNetOnline) {
						gndl.postActiveData("cmdId=403&sid="+sid);
					}
					
					setVisible(false);
					
					try {
						if(ss != null)
							ss.close();
					}
					catch(IOException ioe) {
						println(ioe.toString() + " (8)");
					}
					
					writeSettings();
		
					if(debug) {
						try {
							if(bw != null)
								bw.close();
						}
						catch(IOException ioe) {
							println(ioe.toString() + " (1)");
						}
					}
					
					dispose();
					System.exit(0);
				}
			});
		
		//clock = new Timer(1000, stateMachine);
		//clock.setCoalesce(true);
		//backupClock = new Timer(2000, clockTest);
		
		new Thread(new gwoUpdater(icon, ftFile.length(), true)).start();
			
		/*try {
			//clock = new Timer(1000, stateMachine);
			//clock.start();

			ss = new ServerSocket(port);
			ss.setSoTimeout(500);
			
			if(ss.isBound()) {
				listenAtSocket();
			}
		}
		catch(IOException ioe) {
			//println(ioe.toString() + " (2)");
		}*/
	}
	
	public void print(String txt) {
		try {
			if(debug)
				bw.write(txt);	
		}
		catch(IOException ioe) {
			println(ioe.toString() + " (3)");
		}
	}
	
	public void println(String txt) {
		//System.out.println(txt);
		
		try {
			if(debug)
				bw.write(txt+"\n");	
		}
		catch(IOException ioe) {
			println(ioe.toString() + " (4)");
		}
	}
	
	public void println(boolean txt) {
		try {
			if(debug)
				bw.write(txt+"\n");	
		}
		catch(IOException ioe) {
			println(ioe.toString() + " (5)");
		}
	}
	
	public void println(int txt) {
		try {
			if(debug)
				bw.write(txt+"\n");	
		}
		catch(IOException ioe) {
			println(ioe.toString() + " (6)");
		}
	}
	
	private void createGUI() {
		// SETUP
		setupPane = new JPanel(new BorderLayout());
		setupPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		// Profile
		File f = new File(systemDir+systemSeparator);
		String[] fileList = f.list();
		profileCombo = new JComboBox();
		
		for(int i = 0;i < fileList.length;i++) {
			//System.out.println(fileList[i]);
			
			if(fileList[i].startsWith("gwoServerSettings")) {
				profileCombo.addItem(fileList[i]);
			}	
		}
		
		JPanel profilePane = new JPanel(new BorderLayout());
		profilePane.add("West", new JLabel("Profile: "));
		profilePane.add("Center", profileCombo);
		
		// Admin
		JPanel headerPane = new JPanel();
		headerPane.setLayout(new BoxLayout(headerPane, BoxLayout.X_AXIS));
		aPassLabel = new JLabel("Remote Administrator Password:");
		aPassField = new JPasswordField(adminPassword, 8);
		aPassField.setEchoChar('*');
		
		JPanel aLabelPane = new JPanel(new GridLayout(0, 1));
		aLabelPane.add(aPassLabel);
		JPanel aFieldPane = new JPanel(new GridLayout(0, 1));
		aFieldPane.add(aPassField);		
		
		headerPane.add(aLabelPane);
		headerPane.add(aFieldPane);
		
		
		// DynDNS
		JPanel center1Pane = new JPanel(new BorderLayout());
		center1Pane.setBorder(BorderFactory.createTitledBorder("DynDNS"));
		dyndnsBox = new JCheckBox("Use DynDNS.org to register your server IP", haveDyndns);
		dPassLabel = new JLabel("DynDNS Password:");
		dPassField = new JPasswordField(dyndnsPassword, 15);
		dPassField.setEchoChar('*');
		dNameLabel = new JLabel("DynDNS Username:");
		dNameField = new JTextField(dyndnsUsername, 15);
		dUrlLabel = new JLabel("DynDNS URL:");
		dUrlField = new JTextField(dyndnsUrl, 15);
		
		JPanel bLabelPane = new JPanel(new GridLayout(0, 1));
		bLabelPane.add(dUrlLabel);
		bLabelPane.add(dNameLabel);
		bLabelPane.add(dPassLabel);
		JPanel bFieldPane = new JPanel(new GridLayout(0, 1));
		bFieldPane.add(dUrlField);		
		bFieldPane.add(dNameField);		
		bFieldPane.add(dPassField);		
		
		JPanel centerInnerPane = new JPanel();
		centerInnerPane.setLayout(new BoxLayout(centerInnerPane, BoxLayout.X_AXIS));
		centerInnerPane.add(bLabelPane);
		centerInnerPane.add(bFieldPane);
		
		center1Pane.add("North", dyndnsBox);
		center1Pane.add("Center", centerInnerPane);
		
		// Start Button
		JPanel footerPane = new JPanel(new BorderLayout());
		loadLeagueBox = new JCheckBox("Load existing League Table on Startup", loadLeague);
		
		JPanel footPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		startButton = new JButton("Start");
		startButton.setActionCommand("$start");
		startButton.addActionListener(this);	
		//footPane.add(new JLabel("NOTE: Make sure port 2000 is open on your Firewall"));
		footPane.add(startButton);
		
		JPanel portPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		portLabel = new JLabel("Server Port: (NOTE - Make sure open in firewall)");
		portField = new JTextField(""+port, 5);
		portPane.add(portLabel);
		portPane.add(portField);
		
		// Additional Info
		JPanel center2Pane = new JPanel(new BorderLayout());
		center2Pane.setBorder(BorderFactory.createTitledBorder("Additional GwoNET Infomation"));
		titleField = new JTextField(title, 15);
		descArea = new JTextArea(description, 3, 20);
		descArea.setLineWrap(true);
		descArea.setBorder(BorderFactory.createLineBorder(Color.gray));
		descScrollPane = new JScrollPane(descArea,
                                       JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                       JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		playersField = new JTextField(""+maxPlayers, 15);
		
		JPanel cLabelPane = new JPanel(new GridLayout(0, 1));
		cLabelPane.add(new JLabel("Max Players (0 for unlimited): "));
		cLabelPane.add(new JLabel("Server Title: "));
		JPanel cFieldPane = new JPanel(new GridLayout(0, 1));
		cFieldPane.add(playersField);		
		cFieldPane.add(titleField);		
		
		JPanel centerInner2Pane = new JPanel();
		centerInner2Pane.setLayout(new BoxLayout(centerInner2Pane, BoxLayout.X_AXIS));
		centerInner2Pane.add(cLabelPane);
		centerInner2Pane.add(cFieldPane);
		
		JPanel descPane = new JPanel(new BorderLayout());
		descPane.add("West", new JLabel("Server Description:"));
		descPane.add("Center", descScrollPane);
		
		center2Pane.add("Center", centerInner2Pane);
		center2Pane.add("South", descPane);
		
		footerPane.add("North", center2Pane);
		footerPane.add("Center", portPane);
		footerPane.add("South", loadLeagueBox);
		
		JPanel setupBottomPane = new JPanel(new BorderLayout());
		setupBottomPane.setBorder(BorderFactory.createTitledBorder("Profile Settings"));
		
		setupBottomPane.add("North", headerPane);
		setupBottomPane.add("Center", center1Pane);
		setupBottomPane.add("South", footerPane);
		
		setupPane.add("North", profilePane);
		setupPane.add("Center", setupBottomPane);
		setupPane.add("South", footPane);
		
		// RUN
		runPane = new JPanel();
		runPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		runPane.setLayout(new BorderLayout());
		
		// END
		setContentPane(setupPane);
	}
	
	private void updateGUI() {			
		runPane.removeAll();
		runPane.add("North", new JLabel("*** USER/AI INFORMATION:"));
		
		JPanel h = new JPanel(new GridLayout(3, 4));
		h.add(new JLabel("usOnline: "+usersOnline));
		h.add(new JLabel("usFire: "+usersFired));
		h.add(new JLabel("usFinish: "+usersFinished));
		h.add(new JLabel("usPlay: "+usersPlaying));
		h.add(new JLabel("puOnline: "+preUserList.size()));
		h.add(new JLabel("usHyper: "+usersHyper));
		h.add(new JLabel("usWatch: "+usersWatching));
		h.add(new JLabel("usDead: "+usersDead));
		h.add(new JLabel("aiPlay: "+aiPlaying));
		h.add(new JLabel("aiFire: "+aiFired));
		h.add(new JLabel("aiHyper: "+aiHyper));
		h.add(new JLabel("aiDead: "+aiDead));
		runPane.add("Center", h);
		
		JPanel r = new JPanel();
		r.setLayout(new BoxLayout(r, BoxLayout.Y_AXIS));
		
		r.add(new JLabel("*** GAMES PLAYED: "+gamesPlayed));
		
		if(gameState == STATE_WAIT_FOR_PLAYERS)
			r.add(new JLabel("*** GAME STATE: Waiting for Players"));
		else if(gameState == STATE_WAIT_FOR_NEW_GAME)
			r.add(new JLabel("*** GAME STATE: Waiting for New Game"));
		else if(gameState == STATE_GENERATE)
			r.add(new JLabel("*** GAME STATE: Generating"));
		else if(gameState == STATE_NEW_SHOT)
			r.add(new JLabel("*** GAME STATE: New Shot"));
		else if(gameState == STATE_WAIT_FOR_SHOT1)
			r.add(new JLabel("*** GAME STATE: Waiting for Shots"));
		else if(gameState == STATE_WAIT_FOR_SHOT2)
			r.add(new JLabel("*** GAME STATE: Waiting for Shots (With AI)"));
		else if(gameState == STATE_WAIT_FOR_FINISHED1)
			r.add(new JLabel("*** GAME STATE: Waiting for Finish (Single)"));
		else if(gameState == STATE_WAIT_FOR_FINISHED2)
			r.add(new JLabel("*** GAME STATE: Waiting for Finish (Team/HvM)"));
		else if(gameState == STATE_WAIT_FOR_FINISHED3)
			r.add(new JLabel("*** GAME STATE: Waiting for Finish (Single AI)"));
		else if(gameState == STATE_WAIT_FOR_FINISHED4)
			r.add(new JLabel("*** GAME STATE: Waiting for Finish (Team AI)"));
		else if(gameState == STATE_HYPERSPACE)
			r.add(new JLabel("*** GAME STATE: Waiting for Hyperspace"));
		else if(gameState == STATE_CALCULATE_AWARDS)
			r.add(new JLabel("*** GAME STATE: Calculating Awards"));
		else if(gameState == STATE_END_OF_GAME)
			r.add(new JLabel("*** GAME STATE: End of Game"));
			
		r.add(new JLabel("*** SCENARIO: "+scenarioPlayingText));	
		r.add(new JLabel("*** NETWORK LOAD:"));
		
		if(secondsRunning > 0) {
			r.add(new JLabel("In:  "+dataIn+" Bytes (Total: "+totalDataIn+" Bytes) [Average: "+(totalDataIn/secondsRunning)+" Bytes]"));
			r.add(new JLabel("Out: "+dataOut+" Bytes (Total: "+totalDataOut+" Bytes) [Average: "+(totalDataOut/secondsRunning)+" Bytes]"));
		}
		else {
			r.add(new JLabel("In:  "+dataIn+" Bytes (Total: "+totalDataIn+" Bytes)"));
			r.add(new JLabel("Out: "+dataOut+" Bytes (Total: "+totalDataOut+" Bytes)"));
		}
		
		r.add(new JLabel("*** SERVER AVAILABILITY:"));
		
		if(gwoNetOnline) {
			if(ftInProg)
				r.add(new JLabel("Global Server (GwoNET Enabled) - Transfer "+ftNum+" (Queue: "+ftQueue.size()+" Seg: "+ftSegNum+")"));
			else
				r.add(new JLabel("Global Server (GwoNET Enabled) - Transfer "+ftNum+" (Idle)"));
		}
		else {
			if(ftInProg)
				r.add(new JLabel("Local Network Server Only - Transfer "+ftNum+" (Queue: "+ftQueue.size()+" Seg: "+ftSegNum+")"));
			else
				r.add(new JLabel("Local Network Server Only - Transfer "+ftNum+" (Idle)"));
		}
			
		runPane.add("South", r);
			
		//pack();
		validate();
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if(cmd.equals("$start")) {
			// Get settings
			haveDyndns = dyndnsBox.isSelected();
			dyndnsUsername = dNameField.getText();
			dyndnsPassword = dPassField.getText();
			dyndnsUrl = dUrlField.getText();
			adminPassword = aPassField.getText();
			loadLeague = loadLeagueBox.isSelected();
			title = titleField.getText();
			description = descArea.getText();
			
			if(title.length() > 40)
				title = title.substring(0, 40);
			
			try {
				port = new Integer(portField.getText()).intValue();
			}
			catch(NumberFormatException nfe) {
				port = 2000;	
			}
			
			try {
				maxPlayers = new Integer(playersField.getText()).intValue();
			}
			catch(NumberFormatException nfe) {
				maxPlayers = 0;	
			}
		
			if(debug) {
				try {
					saveNum++;
					bw = new BufferedWriter(new FileWriter("gwoServer@port"+port+"_"+saveNum+".log"));
				}
				catch(IOException ioe) {
					debug = false;		
				}
			}
			
			// Change GUI
			setContentPane(runPane);	
		
			setSize(400, 250);	
		
			// Start State Machine		
			if(checkIp) {
				gsic = new gwoServerIpChecker(this);
				gsic.start();	
			}		
			
			gndl = new gwoNetDataLink(this);
			gndl.start();
			
			if(loadLeague)
				readPlayerLeague();
			
			//clock.start();
			//backupClock.start();
			
			new cyclicTimer().start();
			
			try {
				//clock = new Timer(1000, stateMachine);
				//clock.start();
	
				ss = new ServerSocket(port);
				ss.setSoTimeout(500);
				
				if(ss.isBound()) {
					new listenThread().start();
				}
			}
			catch(IOException ioe) {
				//println(ioe.toString() + " (2)");
			}
		}
		else if(cmd.equals("$profileChange")) {
			readSettings((String)profileCombo.getSelectedItem());
		
			aPassField.setText(adminPassword);
			dyndnsBox.setSelected(haveDyndns);
			dPassField.setText(dyndnsPassword);
			dNameField.setText(dyndnsUsername);
			dUrlField.setText(dyndnsUrl);
			loadLeagueBox.setSelected(loadLeague);
			portField.setText(""+port);
			titleField.setText(title);
			descArea.setText(description);
			playersField.setText(""+maxPlayers);
		}
	}
	
	public void updateIp(String ip) {
		userElement ue;
		boolean oldGwoNetState = gwoNetOnline;
		
		try {	
			if(haveDyndns && sid == 0) {
				setTitle("GWO Server 2.0."+version+" ["+dyndnsUrl+":"+port+"]");
				sid = new Integer(gndl.postActiveData("cmdId=401&sid="+sid+"&version="+version+"&name="+dyndnsUrl+"&port="+port+"&title="+title+"&desc="+description+"&max="+maxPlayers)).intValue();
			}
			else if(!haveDyndns) {
				setTitle("GWO Server 2.0."+version+" ["+ip+":"+port+"]");
				sid = new Integer(gndl.postActiveData("cmdId=401&sid="+sid+"&version="+version+"&name="+ip+"&port="+port+"&title="+title+"&desc="+description+"&max="+maxPlayers)).intValue();
			}
				
			if(sid == 0)
				gwoNetOnline = false;
			else 
				gwoNetOnline = true;
				
			if(gwoNetOnline && !oldGwoNetState) {
				gbb.clear();
				gbb.bufferBoolean(gwoNetOnline);
				sendMessageToGroup(SERVER, SEND_ALL, 137, gbb.data());
								
				for(int i = 0;i < userList.size();i++) {
					ue = (userElement)userList.elementAt(i);
			
					if(ue.online) {
						println("ip: "+ue.uid);
						gndl.postActiveData("cmdId=404&sid="+sid+"&uid="+ue.uid+"&name="+ue.displayName+"&pass="+ue.password);
					}
				}				
			}
		}
		catch(NumberFormatException nfe) {
			println(nfe.toString() + " (7)");
			gwoNetOnline = false;	
		}
	}
	
	private void newPlayerOnline(Socket s) {
		try {
			userElement ue = new userElement();

			ue.s = s;
			ue.uid = generateUid(0);
			ue.online = true;
			ue.ios = new gwoDataInputStream(s.getInputStream());
			ue.dos = new gwoDataOutputStream(s.getOutputStream());
			ue.gsl = new gwoServerClientListener(this, ue.uid, ue.ios, ue.dos);
			ue.gsl.start();
					
			preUserList.addElement(ue);
					
			gbb.clear();
			gbb.bufferInt(version);
			sendMessageToPlayer(SERVER, ue.uid, 100, gbb.data());	
		}				
		catch(IOException ioe) {
		}
	}
	
	private void checkPort() {
		if(ss.getLocalPort() != port) {
			try {
				ss.close();
				ss = new ServerSocket(port);
			}
			catch(IOException ioe2) {
			}
		}			
	}
	
	public class listenThread extends Thread {
	//private void listenAtSocket() {
		boolean connectOK = true;
	
		public void run() {
			while(connectOK) {
				try {
					newPlayerOnline(ss.accept());
				}
				catch(InterruptedIOException iioe) {
					checkPort();
				}
				catch(IOException ioe) {
					connectOK = false;
					println(ioe.toString() + " (9)");
				}
			}
		}
	}
	
	// Message Storage
	
	private void storeEmptyMessage(int msgId, int cmdId, int fromUid, int toUid) {
		emptyMessage em = new emptyMessage();
		em.msgId = msgId;
		em.cmdId = cmdId;
		em.fromUid = fromUid;
		em.toUid = toUid;
		
		emptyMessageStorage.addElement(em);
		
		while(emptyMessageStorage.size() > maxStorageSize) {
			emptyMessageStorage.removeElementAt(0);
		}
	}
	
	private void storeMessage(int msgId, int cmdId, int fromUid, int toUid, int[] data) {
		message m = new message();
		m.msgId = msgId;
		m.cmdId = cmdId;
		m.fromUid = fromUid;
		m.toUid = toUid;
		m.data = data;
		
		messageStorage.addElement(m);
		
		while(messageStorage.size() > maxStorageSize) {
			messageStorage.removeElementAt(0);
		}
	}
	
	private void recallMessage(int msgId, int toUid) {
		println("*** Recalling Stored Message > "+msgId);
		
		// Empty Message
		emptyMessage em = new emptyMessage();
		boolean emValid = false;
		
		for(int i = 0;i < emptyMessageStorage.size() && !emValid;i++) {
			em = (emptyMessage)emptyMessageStorage.elementAt(i);
			
			if(em.toUid == toUid && em.msgId > msgId) {
				println("*** Empty Message "+em.msgId+" found ("+em.cmdId+")");
				emValid = true;
			}
		}
		
		// Message
		message m = new message();
		boolean mValid = false;
		
		for(int i = 0;i < messageStorage.size() && !mValid;i++) {
			m = (message)messageStorage.elementAt(i);
			
			if(m.toUid == toUid && m.msgId > msgId) {
				println("*** Message "+m.msgId+" found ("+m.cmdId+")");
				mValid = true;
			}
		}
		
		// Compare
		if(emValid && mValid) {
			if(em.msgId < m.msgId) {
				println("*** Resending Message "+em.msgId);
				sendEmptyMessageToPlayer(em.fromUid, em.toUid, em.cmdId);
			}
			else {
				println("*** Resending Message "+m.msgId);
				sendMessageToPlayer(m.fromUid, m.toUid, m.cmdId, m.data);
			}
		}
		else if(emValid) {
			println("*** Resending Message "+em.msgId);
			sendEmptyMessageToPlayer(em.fromUid, em.toUid, em.cmdId);
		}
		else if(mValid) {
			println("*** Resending Message "+m.msgId);
			sendMessageToPlayer(m.fromUid, m.toUid, m.cmdId, m.data);
		}
	}
	
	// Message Sending
	
	private void sendMessageToGroup(int fromUid, int mode, int cmdId, int[] data) {
		// mode
		// 0 : no if
		// 1 : only playing players or watching players
		// 2 : not playing players
		// 3 : dead players
		// 4 : not dead players
		userElement ue;
		boolean send = false;
		
		//println("<-- "+cmdId+" *** From: "+fromUid+" *** To Group: "+mode+" *** Size: "+data.length);
	
		try {
			for(int i = 0;i < userList.size();i++) {
				ue = (userElement)userList.elementAt(i);
				send = false;
			
				if(ue.online) {
					switch(mode) {
						case SEND_ALL:
							send = true;
							break;
						case SEND_ALL_EXCEPT:
							if(fromUid != ue.uid)
								send = true;
							break;
						case SEND_ACTIVE:
							if(ue.playing || ue.watching)
								send = true;
							break;
						case SEND_INACTIVE:
							if(!ue.playing)
								send = true;
							break;
						case SEND_WATCHING:
							if(ue.watching)
								send = true;
							break;
						case SEND_DEAD:
							if(ue.playing && ue.dead)
								send = true;
							break;
						case SEND_NOT_DEAD:
							if(ue.playing && !ue.dead)
								send = true;
							break;
					}
				
					if(send) {
						messageId++;
						println("<-- ["+messageId+"] "+cmdId+" *** From: "+fromUid+" *** Group, To: "+ue.uid+" *** Size: "+data.length);
						dataOut+=(1+4+4+4+4+data.length+1);
						
						ue.dos.writeByte(66); // Validation
						ue.dos.flush();
						ue.dos.writeInt(4+4+4+4+data.length+1);
						ue.dos.flush();
						ue.dos.writeInt(messageId);
						ue.dos.flush();
						ue.dos.writeInt(cmdId);
						ue.dos.flush();
						ue.dos.writeInt(0);
						ue.dos.flush();
						ue.dos.writeInt(fromUid);
						ue.dos.flush();
						ue.dos.write(data);
						ue.dos.flush();
						ue.dos.writeByte(99); // Validation
						ue.dos.flush();
						
						storeMessage(messageId, cmdId, fromUid, ue.uid, data);
					}
				}
			}
		}
		catch(IOException ioe) {
			println(ioe.toString() + " (10)");
		}
	}
	
	private void sendEmptyMessageToGroup(int fromUid, int mode, int cmdId) {
		userElement ue;
		boolean send = false;
		
		//println("<*- "+cmdId+" *** From: "+fromUid+" *** To Group: "+mode);
	
		try {
			for(int i = 0;i < userList.size();i++) {
				ue = (userElement)userList.elementAt(i);
				send = false;
			
				if(ue.online) {
					switch(mode) {
						case SEND_ALL:
							send = true;
							break;
						case SEND_ALL_EXCEPT:
							if(fromUid != ue.uid)
								send = true;
							break;
						case SEND_ACTIVE:
							if(ue.playing || ue.watching)
								send = true;
							break;
						case SEND_INACTIVE:
							if(!ue.playing)
								send = true;
							break;
						case SEND_WATCHING:
							if(ue.watching)
								send = true;
							break;
						case SEND_DEAD:
							if(ue.playing && ue.dead)
								send = true;
							break;
						case SEND_NOT_DEAD:
							if(ue.playing && !ue.dead)
								send = true;
							break;
					}
				
					if(send) {
						messageId++;
						println("<*- ["+messageId+"] "+cmdId+" *** From: "+fromUid+" *** Group, To: "+ue.uid);
						dataOut+=(1+4+4+4+4+1);
						
						ue.dos.writeByte(66); // Validation
						ue.dos.flush();
						ue.dos.writeInt(4+4+4+4+1);
						ue.dos.flush();
						ue.dos.writeInt(messageId);
						ue.dos.flush();
						ue.dos.writeInt(cmdId);
						ue.dos.flush();
						ue.dos.writeInt(0);
						ue.dos.flush();
						ue.dos.writeInt(fromUid);
						ue.dos.flush();
						ue.dos.writeByte(99); // Validation
						ue.dos.flush();
							
						storeEmptyMessage(messageId, cmdId, fromUid, ue.uid);
					}
				}
			}
		}
		catch(IOException ioe) {
			println(ioe.toString() + " (11)");
		}
	}
	
	private void sendMessageToPlayer(int fromUid, int toUid, int cmdId, int[] data) {
		userElement ue;
		boolean sent = false;
		
		//println("<-- "+cmdId+" *** From: "+fromUid+" *** To: "+toUid+" *** Size: "+data.length);
			
		for(int i = 0;i < preUserList.size();i++) {
			ue = (userElement)preUserList.elementAt(i);
			
			if(ue.uid == toUid) {
				try {
					messageId++;
					println("<-P ["+messageId+"] "+cmdId+" *** From: "+fromUid+" *** To: "+toUid+" *** Size: "+data.length);
					//dataOut+=(1+4+4+4+4+data.length+1);
					
					ue.dos.writeByte(66); // Validation
					ue.dos.flush();
					ue.dos.writeInt(4+4+4+4+data.length+1);
					ue.dos.flush();
					ue.dos.writeInt(messageId);
					ue.dos.flush();
					ue.dos.writeInt(cmdId);
					ue.dos.flush();
					ue.dos.writeInt(toUid);
					ue.dos.flush();
					ue.dos.writeInt(fromUid);
					ue.dos.flush();
					ue.dos.write(data);
					ue.dos.flush();
					ue.dos.writeByte(99); // Validation
					ue.dos.flush();
					sent = true;
						
					storeMessage(messageId, cmdId, fromUid, toUid, data);
				}
				catch(IOException ioe) {
					println(ioe.toString() + " (12)");
				}
			}
		}
		
		if(!sent) {
			for(int i = 0;i < userList.size();i++) {
				ue = (userElement)userList.elementAt(i);
			
				if(ue.uid == toUid && ue.online) {
					try {
						messageId++;
						println("<-- ["+messageId+"] "+cmdId+" *** From: "+fromUid+" *** To: "+toUid+" *** Size: "+data.length);
						dataOut+=(1+4+4+4+4+data.length+1);
						
						ue.dos.writeByte(66); // Validation
						ue.dos.flush();
						ue.dos.writeInt(4+4+4+4+data.length+1);
						ue.dos.flush();
						ue.dos.writeInt(messageId);
						ue.dos.flush();
						ue.dos.writeInt(cmdId);
						ue.dos.flush();
						ue.dos.writeInt(toUid);
						ue.dos.flush();
						ue.dos.writeInt(fromUid);
						ue.dos.flush();
						ue.dos.write(data);
						ue.dos.flush();
						ue.dos.writeByte(99); // Validation
						ue.dos.flush();
						
						storeMessage(messageId, cmdId, fromUid, toUid, data);
					}
					catch(IOException ioe) {
						println(ioe.toString() + " (13)");
					}
				}
			}
		}
	}
	
	private void sendEmptyMessageToPlayer(int fromUid, int toUid, int cmdId) {
		userElement ue;
		boolean sent = false;
		
		//println("<*- "+cmdId+" *** From: "+fromUid+" *** To: "+toUid);
			
		for(int i = 0;i < preUserList.size();i++) {
			ue = (userElement)preUserList.elementAt(i);
			
			if(ue.uid == toUid) {
				try {
					messageId++;
					println("<*P ["+messageId+"] "+cmdId+" *** From: "+fromUid+" *** To: "+toUid);
					//dataOut+=(1+4+4+4+4+1);
					
					ue.dos.writeByte(66); // Validation
					ue.dos.flush();
					ue.dos.writeInt(4+4+4+4+1);
					ue.dos.flush();
					ue.dos.writeInt(messageId);
					ue.dos.flush();
					ue.dos.writeInt(cmdId);
					ue.dos.flush();
					ue.dos.writeInt(toUid);
					ue.dos.flush();
					ue.dos.writeInt(fromUid);
					ue.dos.flush();
					ue.dos.writeByte(99); // Validation
					ue.dos.flush();
					sent = true;
						
					storeEmptyMessage(messageId, cmdId, fromUid, toUid);
				}
				catch(IOException ioe) {
					println(ioe.toString() + " (14)");
				}
			}
		}
		
		if(!sent) {
			for(int i = 0;i < userList.size();i++) {
				ue = (userElement)userList.elementAt(i);
			
				if(ue.uid == toUid && ue.online) {
					try {
						messageId++;
						println("<*- ["+messageId+"] "+cmdId+" *** From: "+fromUid+" *** To: "+toUid);
						dataOut+=(1+4+4+4+4+1);
						
						ue.dos.writeByte(66); // Validation
						ue.dos.flush();
						ue.dos.writeInt(4+4+4+4+1);
						ue.dos.flush();
						ue.dos.writeInt(messageId);
						ue.dos.flush();
						ue.dos.writeInt(cmdId);
						ue.dos.flush();
						ue.dos.writeInt(toUid);
						ue.dos.flush();
						ue.dos.writeInt(fromUid);
						ue.dos.flush();
						ue.dos.writeByte(99); // Validation
						ue.dos.flush();
						
						storeEmptyMessage(messageId, cmdId, fromUid, toUid);
					}
					catch(IOException ioe) {
						println(ioe.toString() + " (15)");
					}
				}
			}
		}
	}
	
	public void connectionLost(int uid) {
		exeStateMachine = false;
		
		userElement ue;
		playerElement p;
		String name = "";

		for(int i = watchList.size()-1;i >= 0;i--) {
			int wUid = ((Integer)watchList.elementAt(i)).intValue();
			
			if(uid == wUid) {
				watchList.removeElementAt(i);
			}
		}
		
		for(int i = ftQueue.size()-1;i >= 0;i--) {
			int fUid = ((Integer)ftQueue.elementAt(i)).intValue();
			
			if(uid == fUid) {
				ftQueue.removeElementAt(i);
			}
		}
		
		for(int i = preUserList.size()-1;i >= 0;i--) {
			ue = (userElement)preUserList.elementAt(i);
			
			if(uid == ue.uid) {			
				preUserList.removeElementAt(i);
			}
		}
		
		if(uid == ftUid) {
			ftInProg = false;
			
			if(ftQueue.size() > 0) {
				try {
					ftUid = ((Integer)ftQueue.elementAt(0)).intValue();
					ftQueue.removeElementAt(0);
					ftSegNum = 0;
					ftNum++;
					ftLastSegSent = System.currentTimeMillis();
					ftFis = new FileInputStream("gwo2.jar");
					ftData = new byte[4096];
					
					ftInProg = true;
					sendEmptyMessageToPlayer(SERVER, ftUid, 161);	
				}
				catch(FileNotFoundException fnfe) {
					println(fnfe.toString() + " (16)");
				}	
			}
		}
				
		// Select Controlling Player
		if(aiController == uid) {
			for(int i = 0;i < userList.size();i++) {
				aiController = 0;
				ue = (userElement)userList.elementAt(i);
					
				if(ue.playing) {
					sendEmptyMessageToPlayer(SERVER, ue.uid, 129);
					aiController = ue.uid;
					i = userList.size();
				}
			}			
			if(aiController == 0) {
				for(int i = 0;i < userList.size();i++) {
					ue = (userElement)userList.elementAt(i);
						
					if(ue.watching) {
						sendEmptyMessageToPlayer(SERVER, ue.uid, 129);
						aiController = ue.uid;
						i = userList.size();
					}
				}
			}	
		}	
		
		usersOnline = 0;
		usersPlaying = 0;
		usersWatching = 0;
		usersDead = 0;
		usersFinished = 0;
		usersFired = 0;
		usersHyper = 0;
		
		for(int i = 0;i < userList.size();i++) {
			ue = (userElement)userList.elementAt(i);
			
			if(ue.uid == uid) {			
				name = ue.displayName;
				println("Player "+name+" ("+uid+") quits.");
				
				if(ue.online && gwoNetOnline) {
					gndl.postPassiveData("cmdId=406&sid="+sid+"&uid="+uid);	
				}
				
				if(gameType == TYPE_TEAM_GAME || gameType == TYPE_TEAM_SHIELDS_GAME || gameType == TYPE_HVM_AI_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME || gameType == TYPE_TEAM_AI_GAME || gameType == TYPE_TEAM_AI_SHIELDS_GAME) {
					if(ue.playing && !ue.dead) {
						((teamElement)teamList.elementAt(ue.team)).usersDead++;
					}
				}
				
				ue.online = false;
				ue.playing = false;
				ue.watching = false;
				ue.dead = false;
				ue.diedThisShot = false;
				ue.fired = false;
				ue.hyper = false;
				ue.finished = false;
				ue.voteDraw = false;					
				
				try {
					ue.gsl.quit();
					if(!ue.s.isInputShutdown())
						ue.ios.close();
					if(!ue.s.isOutputShutdown())
						ue.dos.close();
					if(!ue.s.isClosed())
						ue.s.close();
				}
				catch(IOException ioe) {
					println(ioe.toString() + " (17)");	
				}
				
				userList.setElementAt(ue, i);
			}
			
			if(ue.online)
				usersOnline++;
					
			if(ue.playing)
				usersPlaying++;
					
			if(ue.watching)
				usersWatching++;
					
			if(ue.dead)
				usersDead++;
					
			if(ue.finished)
				usersFinished++;
					
			if(ue.fired)
				usersFired++;
					
			if(ue.hyper)
				usersHyper++;
		
			println("*** usersOnline "+usersOnline);
			println("*** usersPlaying "+usersPlaying);
			println("*** usersWatching "+usersWatching);
			println("*** usersFinished "+usersFinished);
			println("*** usersDead "+usersDead);
			println("*** usersFired "+usersFired);
			println("*** usersHyper "+usersHyper);				
			println("***");				
		}
				
		if(usersOnline <= 0) {
			println("reset");
			usersOnline = 0;
			usersPlaying = 0;
			usersWatching = 0;
			usersDead = 0;
			usersFinished = 0;
			gameClock = 0;
			atSynchPoint = true;
					
			players = new Vector();
			planets = new Vector();
			messageStorage = new Vector();
			emptyMessageStorage = new Vector();
					
			println("*** GC Started");
			System.gc();
			println("*** GC Finished");
					
			gameState = STATE_WAIT_FOR_PLAYERS;
		}
		
		for(int i = 0;i < players.size();i++) {
			p = (playerElement)players.elementAt(i);
				
			if(p.uid == uid) {
				p.hyper = false;
				p.fired = false;
				p.status = 2;
				players.setElementAt(p, i);
			}
		}
		
		gbb.clear();
		gbb.bufferString(name);
		gbb.bufferBoolean(false);
		sendMessageToGroup(uid, SEND_ALL, 192, gbb.data());
		
		exeStateMachine = true;
	}
	
	private int generateUid(int offset) {
		userElement ue;
		aiElement ae;
		boolean uidOk = true;
		int uid=0;
		do {
			uid = offset + (int)(1000000.0 * Math.random());
			
			for(int i = 0;i < preUserList.size();i++) {
				ue = (userElement)preUserList.elementAt(i);
			
				if(ue.uid == uid) {
					uidOk = false;
				}
			}
			
			for(int i = 0;i < userList.size();i++) {
				ue = (userElement)userList.elementAt(i);
			
				if(ue.uid == uid) {
					uidOk = false;
				}
			}
			
			for(int i = 0;i < aiList.size();i++) {
				ae = (aiElement)aiList.elementAt(i);
			
				if(ae.uid == uid) {
					uidOk = false;
				}
			}
		} while(!uidOk);
		
		return uid;
	}
	
	private void updatePlayerStats(int uid, int played, int won, int drawn, int lost, int kills, int hits, int points) {
		statsElement s;
		
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(s.uid == uid) {
				s.played+=played;	
				s.won+=won;	
				s.drawn+=drawn;	
				s.lost+=lost;	
				s.killsFor+=kills;	
				s.killsAgainst+=hits;	
				s.points+=points;	
				
				stats.setElementAt(s, i);
			}	
		}
	}
	
	/*ActionListener clockTest = new ActionListener() {
		public void actionPerformed(ActionEvent e) {			
			if(!clockValid) {
				println("TIMER INVALID " + stateFinished);
				clock.stop();
				clock = null;
				clock = new Timer(1000, stateMachine);
				clock.start();
			}
			
			clockValid = false;
		}
	};*/
	
	private boolean exeStateMachine = true;
	
	private class cyclicTimer extends Thread {
		public void run() {
			while(true) {
				if(exeStateMachine)
					stateMachine();
				
				try {
					Thread.sleep(1000);	
				}
				catch(InterruptedException ie) {
					
				}
			}	
		}
	}
	
	//ActionListener stateMachine = new ActionListener() {
	//public void actionPerformed(ActionEvent e) {		
	private void stateMachine() {
		clockValid = true;
		stateFinished = false;
		
		gameClock++;
		pingClock++;
		userElement ue;
		aiElement ae;
		
		// Set Storage Size
		maxStorageSize = 25*usersOnline;
		
		if(usersOnline > 0 && pingClock >= 30) { // Ping
			Calendar cal = Calendar.getInstance();
			println("TIME "+cal.get(Calendar.DAY_OF_MONTH)+"/"+(1+cal.get(Calendar.MONTH))+" "+cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE)+"."+cal.get(Calendar.SECOND));
			
			for(int i = 0;i < userList.size();i++) {
				ue = (userElement)userList.elementAt(i);
					
				if(ue.online) {
					if(ue.ping) {
						// TODO: Handle non-communicating client
						println("Player "+ue.uid+" not communicating.");
						connectionLost(ue.uid);
					}
				
					ue.ping = true;
					
					userList.setElementAt(ue, i);
				}
			}
				
			sendEmptyMessageToGroup(SERVER, SEND_ALL, 102);
			pingClock = 0;
		}
		
		if(ftInProg && ftLastSegSent < System.currentTimeMillis()-20000) {
			ftLastSegSent = System.currentTimeMillis();
			gbb.clear();
			gbb.bufferInt(ftSize);
			gbb.bufferByteArray(ftData, ftSize);
			sendMessageToPlayer(SERVER, ftUid, 162, gbb.data());
		}
		
		if(gameState == STATE_WAIT_FOR_PLAYERS) {
			if(usersOnline >= 2) {
				usersFired = 0;
				usersHyper = 0;
				usersWatching = 0;
				usersDead = 0;
				usersFinished = 0;
				aiPlaying = 0;
				aiFired = 0;
				aiHyper = 0;
				aiDead = 0;
				gameClock = 0;
				atSynchPoint = false;
				
				sendEmptyMessageToGroup(SERVER, SEND_ALL, 105);
				gameState = STATE_WAIT_FOR_NEW_GAME;
			}
			else if(usersPlaying == 1) {
				aiPlaying = 0;
				aiFired = 0;
				aiHyper = 0;
				aiDead = 0;
				gameClock = 0;
				gameState = STATE_GENERATE;
					
				sendEmptyMessageToGroup(SERVER, SEND_ACTIVE, 108);
				sendEmptyMessageToGroup(SERVER, SEND_INACTIVE, 104);				
			}
		}
		else if(gameState == STATE_WAIT_FOR_NEW_GAME) {
			if(usersOnline < 2) {
				for(int i = 0;i < userList.size();i++) {
					ue = (userElement)userList.elementAt(i);
					
					if(ue.playing) {
						ue.playing = false;
						sendEmptyMessageToPlayer(SERVER, ue.uid, 120);
						usersPlaying--;
								
						if(ue.finished) {
							ue.finished = false;
							usersFinished--;
						}
					}
					
					userList.setElementAt(ue, i);
				}
				
				sendEmptyMessageToGroup(SERVER, SEND_ALL, 103);
				gameState = STATE_WAIT_FOR_PLAYERS;
			}
			else if(usersPlaying >= 2) {
				if(gameClock <= 10 && usersPlaying != usersOnline) {
					if(gameClock == 1) {
						gbb.clear();
						gbb.bufferByte((byte)(9));
						sendMessageToGroup(SERVER, SEND_ALL, 106, gbb.data());
					}
				}
				else {	
					gameClock = 0;
					gameState = STATE_GENERATE;
					
					sendEmptyMessageToGroup(SERVER, SEND_ACTIVE, 108);
					sendEmptyMessageToGroup(SERVER, SEND_INACTIVE, 104);
				}
			}
			else {
				gameClock = 0;
			}
		}
		else if(gameState == STATE_GENERATE) {
			if(usersFinished >= usersPlaying || gameClock > 15) {
				gameState = STATE_NEW_SHOT;
				
				generateGame();
				generateGalaxy();
				generateStarfield();
					
				if(!generatePlayers())
					gameState = STATE_GENERATE;
			}
		}
		else if(gameState == STATE_NEW_SHOT) {
			atSynchPoint = true;
			usersFired = 0;
			usersHyper = 0;
			aiFired = 0;
			aiHyper = 0;
			usersFinished = 0;
			gameClock = 0;
				
			for(int i = 0;i < userList.size();i++) {
				ue = (userElement)userList.elementAt(i);
					
				ue.finished = false;
				userList.setElementAt(ue, i);
			}
			
			for(int i = 0;i < watchList.size();i++) {
				int uid = ((Integer)watchList.elementAt(i)).intValue();
				
				for(int y = 0;y < userList.size();y++) {
					ue = (userElement)userList.elementAt(y);
				
					if(ue.uid == uid) {
						sendEmptyMessageToPlayer(SERVER, uid, 108);
						
						// Game Scenario
						gbb.clear();
						gbb.bufferByte((byte)scenarioPlaying);
						gbb.bufferByte((byte)gameType);
						gbb.bufferString(scenarioPlayingText);
						sendMessageToPlayer(SERVER, uid, 117, gbb.data());
		
						// Galaxy
						planetElement p;
						do {
							gbb.clear();
							gbb.bufferByte((byte)planets.size());
							for(int x = 0;x < planets.size();x++) {
								p = (planetElement)planets.elementAt(x);
				
								gbb.bufferInt((int)p.x);
								gbb.bufferInt((int)p.y);
								gbb.bufferDouble(p.radius);
								gbb.bufferByte((byte)p.color.getRed());
								gbb.bufferByte((byte)p.color.getGreen());
								gbb.bufferByte((byte)p.color.getBlue());
								gbb.bufferDouble(p.M);
								gbb.bufferDouble(p.density);
								gbb.bufferByte((byte)p.shading);
								gbb.bufferByte((byte)p.impact);
							}
						}while(!gbb.checksum());
						sendMessageToPlayer(SERVER, uid, 110, gbb.data());
		
						// Starfield
						gbb.clear();
						gbb.bufferByte((byte)(100 + (150.0 * Math.random())));
						gbb.bufferDouble(Math.random());
						sendMessageToPlayer(SERVER, uid, 109, gbb.data());
							
						// Mines
						if(mines.size() > 0) {
							mineElement me = (mineElement)mines.elementAt(0);
							do{
								gbb.clear();
								gbb.bufferByte((byte)mines.size());
								gbb.bufferDouble(me.radius);
								for(int x = 0;x < mines.size();x++) {
									me = (mineElement)mines.elementAt(x);	
					
									gbb.bufferInt((int)me.x);
									gbb.bufferInt((int)me.y);
									gbb.bufferByte((byte)me.status);
								}
							}while(!gbb.checksum());
							sendMessageToPlayer(SERVER, uid, 139, gbb.data());
						}
				
						// Players
						playerElement pl;
						do {
							gbb.clear();
							gbb.bufferInt(players.size());
							for(int x = 0;x < players.size();x++) {
								pl = (playerElement)players.elementAt(x);
					
								gbb.bufferString(pl.displayName);
								gbb.bufferInt(pl.uid);
								gbb.bufferInt((int)pl.x);
								gbb.bufferInt((int)pl.y);
								gbb.bufferByte(pl.team);
								gbb.bufferByte(pl.status);
								gbb.bufferByte((byte)pl.color.getRed());
								gbb.bufferByte((byte)pl.color.getGreen());
								gbb.bufferByte((byte)pl.color.getBlue());
								gbb.bufferDouble(pl.radius);				
							}
						}while(!gbb.checksum());
						sendMessageToPlayer(SERVER, uid, 111, gbb.data());
	
						ue.watching = true;
						usersWatching++;
						userList.setElementAt(ue, y);
						
						sendEmptyMessageToPlayer(SERVER, uid, 132);
						
						// Notify others
						gbb.clear();
						gbb.bufferString(ue.displayName);
						sendMessageToGroup(uid, SEND_ACTIVE, 131, gbb.data());
					}
				}
			}
			
			watchList = new Vector();
			
			sendEmptyMessageToGroup(SERVER, SEND_ACTIVE, 112);
			
			if(gameType <= TYPE_TEAM_SHIELDS_GAME)
				gameState = STATE_WAIT_FOR_SHOT1;
			else
				gameState = STATE_WAIT_FOR_SHOT2;
				
			timeoutClock = 0;
			gameClock = 0;
		}
		else if(gameState == STATE_WAIT_FOR_SHOT1) {
			if(usersFired + usersHyper > 0) {
				if(gameClock <= 30 && usersFired + usersHyper < usersPlaying - usersDead) {
					if(gameClock == 1 || gameClock == 25) {
						gbb.clear();
						gbb.bufferByte((byte)(30-gameClock));
						sendMessageToGroup(SERVER, SEND_ACTIVE, 114, gbb.data());
					}
				}
				else {										
					gameClock = 0;
					
					if(usersHyper == usersPlaying-usersDead) {
						hyperSpace();
					}
					else {
						firedTime = System.currentTimeMillis();
						sendEmptyMessageToGroup(SERVER, SEND_ACTIVE, 115);
						
						if(gameType < TYPE_TEAM_GAME)
							gameState = STATE_WAIT_FOR_FINISHED1;
						else
							gameState = STATE_WAIT_FOR_FINISHED2;
					}
				}				
			}
			else {
				gameClock = 0;
				timeoutClock++;
				
				// Check for timeout
				
				if(timeoutClock == 30) {
					gbb.clear();
					gbb.bufferByte((byte)(60));
					sendMessageToGroup(SERVER, SEND_ACTIVE, 114, gbb.data());
				}
				else if(timeoutClock == 90) {
					// Reset Data
					usersOnline = 0;
	 				usersPlaying = 0;
					usersFired = 0;
					usersHyper = 0;
					usersWatching = 0;
					usersDead = 0;
					usersFinished = 0;
				
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
					
						if(ue.online)
							usersOnline++;
						
						ue.playing = false;
						ue.watching = false;
						ue.dead = false;
						ue.diedThisShot = false;
						ue.fired = false;
						ue.hyper = false;
						ue.finished = false;
						ue.voteDraw = false;
						ue.idle = false;
					
						userList.setElementAt(ue, i);
					}
					
					gameState = STATE_END_OF_GAME;
					gameClock = 10;	
				}
			}
		}
		else if(gameState == STATE_WAIT_FOR_SHOT2) {
			if(gameClock <= 30 && usersFired + usersHyper < usersPlaying - usersDead) {
				if(gameClock == 1 || gameClock == 25) {
					gbb.clear();
					gbb.bufferByte((byte)(30-gameClock));
					sendMessageToGroup(SERVER, SEND_ACTIVE, 114, gbb.data());
				}
			}
			else {					
				if(usersPlaying == usersDead) {
					gbb.clear();
					gbb.bufferByte((byte)(29));
					sendMessageToGroup(SERVER, SEND_ACTIVE, 114, gbb.data());					
				}
				
				aiThink();
				
				aiController = 0;
				// Select Controlling Player
				for(int i = 0;i < userList.size();i++) {
					ue = (userElement)userList.elementAt(i);
					
					if(ue.playing) {
						sendEmptyMessageToPlayer(SERVER, ue.uid, 129);
						aiController = ue.uid;
						i = userList.size();
					}
				}			
				if(aiController == 0) {
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
						
						if(ue.watching) {
							sendEmptyMessageToPlayer(SERVER, ue.uid, 129);
							aiController = ue.uid;
							i = userList.size();
						}
					}
				}			
				
				gameClock = 0;
				
				if(usersHyper+aiHyper == usersPlaying-usersDead+aiPlaying-aiDead) {
					hyperSpace();
				}
				else {
					firedTime = System.currentTimeMillis();
					sendEmptyMessageToGroup(SERVER, SEND_ACTIVE, 115);
						
					if(gameType == TYPE_HVM_AI_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME)
						gameState = STATE_WAIT_FOR_FINISHED2;
					else if(gameType == TYPE_SINGLE_AI_GAME || gameType == TYPE_SINGLE_AI_SHIELDS_GAME)
						gameState = STATE_WAIT_FOR_FINISHED3;
					else
						gameState = STATE_WAIT_FOR_FINISHED4;
				}					
			}			
		}
		else if(gameState == STATE_WAIT_FOR_FINISHED1) {
			if(usersFinished == usersPlaying + usersWatching) {	
				println("FINISHED");
				latencyControl();
				
				for(int i = 0;i < userList.size();i++) {
					ue = (userElement)userList.elementAt(i);
					
					if(ue.playing && !ue.dead) {
						if(!ue.fired && !ue.hyper) {
							// idle
							if(ue.idle) {
								//kick
								ue.dead = true;
								ue.diedThisShot = true;
								usersDead++;
		
								playerElement p;
								for(int x = 0;x < players.size();x++) {
									p = (playerElement)players.elementAt(x);
				
									if(p.uid == ue.uid) {
										p.hyper = false;
										p.fired = false;
										p.status = 2;
										players.setElementAt(p, x);
									}	
								}
							
								gbb.clear();
								gbb.bufferString(ue.displayName);
								sendMessageToGroup(ue.uid, SEND_ACTIVE, 128, gbb.data());								
							}
							else {
								ue.idle = true;
							}
						}					
						else {
							ue.idle = false;	
						}					
					}
				
					ue.finished = false;
					userList.setElementAt(ue, i);
				}
			
				usersFinished = 0;
			
				if(usersDead == usersPlaying) { // Draw
					int drawers = 0;
					// Set points
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
						
						if(ue.playing) {
							if(ue.diedThisShot) { //Winning drawers
								drawers++;
							}
						}
					}
					// Set draw/lost
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
						
						if(ue.playing) {
							if(ue.diedThisShot) { //Winning drawers
								//ue.played++;
								//ue.drawn++;
								//ue.points+=usersPlaying/drawers;
								updatePlayerStats(ue.uid, 1, 0, 1, 0, 0, 0, gamePoints/drawers);
								
								sendEmptyMessageToPlayer(SERVER, ue.uid, 122);
							}
							else {
								//ue.played++;
								//ue.lost++;
								updatePlayerStats(ue.uid, 1, 0, 0, 1, 0, 0, 0);
								
								sendEmptyMessageToPlayer(SERVER, ue.uid, 123);
							}
							
							//userList.setElementAt(ue, i);
						}
					}
					
					sendEmptyMessageToGroup(SERVER, SEND_WATCHING, 124);		
				
					gameClock = 0;
					//usersFinished = 0;
					gameState = STATE_CALCULATE_AWARDS;	
				}
				else if(usersDead == usersPlaying - 1) { // Win
					// Set win/points
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
						
						if(ue.playing) {
							if(!ue.dead) { //Winning player
								//ue.played++;
								//ue.won++;
								//ue.points+=usersPlaying;
								updatePlayerStats(ue.uid, 1, 1, 0, 0, 0, 0, gamePoints);
								
								sendEmptyMessageToPlayer(SERVER, ue.uid, 121);
							}
							else {
								//ue.played++;
								//ue.lost++;
								updatePlayerStats(ue.uid, 1, 0, 0, 1, 0, 0, 0);
								
								sendEmptyMessageToPlayer(SERVER, ue.uid, 123);
							}
							
							//userList.setElementAt(ue, i);
						}
					}
					
					sendEmptyMessageToGroup(SERVER, SEND_WATCHING, 124);		
					
					gameClock = 0;
					//usersFinished = 0;
					gameState = STATE_CALCULATE_AWARDS;	
				}
				else { // Keep playing
					// Reset Values
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
						
						ue.fired = false;
						ue.diedThisShot = false;						
						ue.missileCollide = false;
					
						userList.setElementAt(ue, i);
					}
					
					gameClock = 0;
					hyperSpace();
				}
			}
		}
		else if(gameState == STATE_WAIT_FOR_FINISHED2) {
			if(usersFinished == usersPlaying + usersWatching) {
				latencyControl();
				
				for(int i = 0;i < userList.size();i++) {
					ue = (userElement)userList.elementAt(i);
					
					if(ue.playing && !ue.dead) {
						if(!ue.fired && !ue.hyper) {
							// idle
							if(ue.idle) {
								//kick
								ue.dead = true;
								ue.diedThisShot = true;
								usersDead++;
		
								playerElement p;
								for(int x = 0;x < players.size();x++) {
									p = (playerElement)players.elementAt(x);
				
									if(p.uid == ue.uid) {
										p.hyper = false;
										p.fired = false;
										p.status = 2;
										players.setElementAt(p, x);
									}	
								}
							
								teamElement te = (teamElement)teamList.elementAt(ue.team);
								te.usersDead++;
								te.diedThisShot = true;
								teamList.setElementAt(te, ue.team);
							
								gbb.clear();
								gbb.bufferString(ue.displayName);
								sendMessageToGroup(ue.uid, SEND_ACTIVE, 128, gbb.data());								
							}
							else {
								ue.idle = true;
							}
						}
						else {
							ue.idle = false;	
						}					
					}
					
					ue.finished = false;
					userList.setElementAt(ue, i);
				}
			
				usersFinished = 0;
			
				teamElement te;
				
				int teamsDead=0;
				for(int i = 0;i < teamList.size();i++) {
					te = (teamElement)teamList.elementAt(i);
						
					println(i);
					println(te.usersPlaying);
					println(te.usersDead);
					println(te.diedThisShot);
					
					if(te.usersPlaying == te.usersDead)
						teamsDead++;
				}
				
				println("Teamsdead "+teamsDead);
					
				if(teamsDead == teamList.size()) { // Draw
					for(int i = 0;i < teamList.size();i++) {
						te = (teamElement)teamList.elementAt(i);
						
						if(te.diedThisShot) { // Drawing team
							for(int x = 0;x < userList.size();x++) {
								ue = (userElement)userList.elementAt(x);
								
								if(ue.playing && ue.team == i) { // Drawing Player
									//ue.played++;
									//ue.drawn++;
									//ue.points+=usersPlaying/(te.usersPlaying*teamsDead);
									updatePlayerStats(ue.uid, 1, 0, 1, 0, 0, 0, gamePoints/(te.usersPlaying*teamsDead));
									
									sendEmptyMessageToPlayer(SERVER, ue.uid, 122);
								}
								
								//userList.setElementAt(ue, x);
							}
						}
						else { // Losing team
							for(int x = 0;x < userList.size();x++) {
								ue = (userElement)userList.elementAt(x);
								
								if(ue.playing && ue.team == i) { // Losing Player
									//ue.played++;
									//ue.lost++;
									updatePlayerStats(ue.uid, 1, 0, 0, 1, 0, 0, 0);
									
									sendEmptyMessageToPlayer(SERVER, ue.uid, 123);
								}
								
								//userList.setElementAt(ue, x);
							}
						}
					}	
					
					sendEmptyMessageToGroup(SERVER, SEND_WATCHING, 124);		
					
					gameClock = 0;
					//usersFinished = 0;
					gameState = STATE_CALCULATE_AWARDS;	
				}
				else if(teamsDead == teamList.size() - 1) { // Win
					for(int i = 0;i < teamList.size();i++) {
						te = (teamElement)teamList.elementAt(i);
						
						if(te.usersPlaying > te.usersDead) { // Winning team
							for(int x = 0;x < userList.size();x++) {
								ue = (userElement)userList.elementAt(x);
								
								if(ue.playing && ue.team == i) { // Winning Player
									//ue.played++;
									//ue.won++;
									//ue.points+=usersPlaying/te.usersPlaying;
									updatePlayerStats(ue.uid, 1, 1, 0, 0, 0, 0, gamePoints/te.usersPlaying);
									
									sendEmptyMessageToPlayer(SERVER, ue.uid, 121);
								}
								
								//userList.setElementAt(ue, x);
							}
						}
						else { // Losing team
							for(int x = 0;x < userList.size();x++) {
								ue = (userElement)userList.elementAt(x);
								
								if(ue.playing && ue.team == i) { // Losing Player
									//ue.played++;
									//ue.lost++;
									updatePlayerStats(ue.uid, 1, 0, 0, 1, 0, 0, 0);
									
									sendEmptyMessageToPlayer(SERVER, ue.uid, 123);
								}
								//userList.setElementAt(ue, x);
							}
						}	
					}	
					
					sendEmptyMessageToGroup(SERVER, SEND_WATCHING, 124);		
					
					gameClock = 0;
					//usersFinished = 0;
					gameState = STATE_CALCULATE_AWARDS;	
				}
				else { // Keep playing
					// Reset Values
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
						
						ue.fired = false;
						ue.diedThisShot = false;						
						ue.missileCollide = false;
					
						userList.setElementAt(ue, i);
					}
					
					for(int i = 0;i < teamList.size();i++) {
						te = (teamElement)teamList.elementAt(i);
						te.diedThisShot = false;
						teamList.setElementAt(te, i);
					}
					
					gameClock = 0;
					hyperSpace();
				}
			}
		}
		else if(gameState == STATE_WAIT_FOR_FINISHED3) {
			if(usersFinished == usersPlaying + usersWatching) {					
				latencyControl();
				
				for(int i = 0;i < userList.size();i++) {
					ue = (userElement)userList.elementAt(i);
					
					if(ue.playing && !ue.dead) {
						if(!ue.fired && !ue.hyper) {
							// idle
							if(ue.idle) {
								//kick
								ue.dead = true;
								ue.diedThisShot = true;
								usersDead++;
		
								playerElement p;
								for(int x = 0;x < players.size();x++) {
									p = (playerElement)players.elementAt(x);
				
									if(p.uid == ue.uid) {
										p.hyper = false;
										p.fired = false;
										p.status = 2;
										players.setElementAt(p, x);
									}	
								}
							
								gbb.clear();
								gbb.bufferString(ue.displayName);
								sendMessageToGroup(ue.uid, SEND_ACTIVE, 128, gbb.data());								
							}
							else {
								ue.idle = true;
							}
						}					
						else {
							ue.idle = false;	
						}					
					}
				
					ue.finished = false;
					userList.setElementAt(ue, i);
				}

				if(usersDead+aiDead == usersPlaying+aiPlaying) { // Draw
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
						
						if(ue.playing) {
							if(ue.diedThisShot) { //Winning drawers
								sendEmptyMessageToPlayer(SERVER, ue.uid, 122);
							}
							else {
								sendEmptyMessageToPlayer(SERVER, ue.uid, 123);
							}
						}
					}
					
					sendEmptyMessageToGroup(SERVER, SEND_WATCHING, 124);		
				
					gameClock = 0;
					gameState = STATE_END_OF_GAME;	
				}
				else if(usersDead+aiDead == usersPlaying+aiPlaying - 1) { // Win
					// Set win/points
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
						
						if(ue.playing) {
							if(!ue.dead) { //Winning player
								sendEmptyMessageToPlayer(SERVER, ue.uid, 121);
							}
							else { //Losing player
								sendEmptyMessageToPlayer(SERVER, ue.uid, 123);
							}
						}
					}
					
					sendEmptyMessageToGroup(SERVER, SEND_WATCHING, 124);		
					
					gameClock = 0;
					gameState = STATE_END_OF_GAME;	
				}
				else { // Keep playing
					// Reset Values
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
						
						ue.fired = false;
						ue.diedThisShot = false;						
						ue.missileCollide = false;
					
						userList.setElementAt(ue, i);
					}
					
					for(int i = 0;i < aiList.size();i++) {
						ae = (aiElement)aiList.elementAt(i);
						
						ae.fired = false;
						ae.diedThisShot = false;							
					
						aiList.setElementAt(ae, i);
					}
					
					gameClock = 0;
					usersFinished = 0;
					hyperSpace();
				}
			}
		}
		else if(gameState == STATE_WAIT_FOR_FINISHED4) {
			if(usersFinished == usersPlaying + usersWatching) {	
				latencyControl();
								
				for(int i = 0;i < userList.size();i++) {
					ue = (userElement)userList.elementAt(i);
					
					if(ue.playing && !ue.dead) {
						if(!ue.fired && !ue.hyper) {
							// idle
							if(ue.idle) {
								//kick
								ue.dead = true;
								ue.diedThisShot = true;
								usersDead++;
		
								playerElement p;
								for(int x = 0;x < players.size();x++) {
									p = (playerElement)players.elementAt(x);
				
									if(p.uid == ue.uid) {
										p.hyper = false;
										p.fired = false;
										p.status = 2;
										players.setElementAt(p, x);
									}	
								}
							
								gbb.clear();
								gbb.bufferString(ue.displayName);
								sendMessageToGroup(ue.uid, SEND_ACTIVE, 128, gbb.data());								
							}
							else {
								ue.idle = true;
							}
						}					
						else {
							ue.idle = false;	
						}					
					}
				
					ue.finished = false;
					userList.setElementAt(ue, i);
				}
			
				usersFinished = 0;
			
				teamElement te;
				
				int teamsDead=0;
				for(int i = 0;i < teamList.size();i++) {
					te = (teamElement)teamList.elementAt(i);
						
					println(i);
					println(te.usersPlaying);
					println(te.usersDead);
					println(te.diedThisShot);
					
					if(te.usersPlaying == te.usersDead)
						teamsDead++;
				}
				
				println("Teamsdead "+teamsDead);
					
				if(teamsDead == teamList.size()) { // Draw
					for(int i = 0;i < teamList.size();i++) {
						te = (teamElement)teamList.elementAt(i);
						
						if(te.diedThisShot) { // Drawing team
							for(int x = 0;x < userList.size();x++) {
								ue = (userElement)userList.elementAt(x);
								
								if(ue.playing && ue.team == i) { // Drawing Player
									sendEmptyMessageToPlayer(SERVER, ue.uid, 122);
								}
							}
						}
						else { // Losing team
							for(int x = 0;x < userList.size();x++) {
								ue = (userElement)userList.elementAt(x);
								
								if(ue.playing && ue.team == i) { // Losing Player
									sendEmptyMessageToPlayer(SERVER, ue.uid, 123);
								}
							}
						}
					}	
					
					sendEmptyMessageToGroup(SERVER, SEND_WATCHING, 124);		
					
					gameClock = 0;
					//usersFinished = 0;
					gameState = STATE_END_OF_GAME;	
				}
				else if(teamsDead == teamList.size() - 1) { // Win
					for(int i = 0;i < teamList.size();i++) {
						te = (teamElement)teamList.elementAt(i);
						
						if(te.usersPlaying > te.usersDead) { // Winning team
							for(int x = 0;x < userList.size();x++) {
								ue = (userElement)userList.elementAt(x);
								
								if(ue.playing && ue.team == i) { // Winning Player
									sendEmptyMessageToPlayer(SERVER, ue.uid, 121);
								}
							}
						}
						else { // Losing team
							for(int x = 0;x < userList.size();x++) {
								ue = (userElement)userList.elementAt(x);
								
								if(ue.playing && ue.team == i) { // Losing Player
									sendEmptyMessageToPlayer(SERVER, ue.uid, 123);
								}
							}
						}	
					}	
					
					sendEmptyMessageToGroup(SERVER, SEND_WATCHING, 124);		
					
					gameClock = 0;
					gameState = STATE_END_OF_GAME;	
				}
				else { // Keep playing					
					// Reset Values
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);

						ue.fired = false;
						ue.diedThisShot = false;						
						ue.missileCollide = false;
					
						userList.setElementAt(ue, i);
					}
					
					for(int i = 0;i < aiList.size();i++) {
						ae = (aiElement)aiList.elementAt(i);
						
						ae.fired = false;
						ae.diedThisShot = false;							
					
						aiList.setElementAt(ae, i);
					}
					
					for(int i = 0;i < teamList.size();i++) {
						te = (teamElement)teamList.elementAt(i);
						te.diedThisShot = false;
						teamList.setElementAt(te, i);
					}
					
					gameClock = 0;
					hyperSpace();
				}
			}
		}
		else if(gameState == STATE_HYPERSPACE) {
			if(usersFinished == usersPlaying + usersWatching || gameClock > 20) {
				for(int i = 0;i < userList.size();i++) {
					ue = (userElement)userList.elementAt(i);
					
					ue.hyper = false;
					ue.finished = false;
					userList.setElementAt(ue, i);
				}
				
				for(int i = 0;i < aiList.size();i++) {
					ae = (aiElement)aiList.elementAt(i);
					
					ae.hyper = false;
					aiList.setElementAt(ae, i);
				}
			
				usersFinished = 0;
				gameClock = 0;			
				gameState = STATE_NEW_SHOT;
			}
		}
		else if(gameState == STATE_CALCULATE_AWARDS) {
			if(usersFinished == usersPlaying || gameClock > 10) {
				// TODO: Calculate Awards
				calculateAwards();
				
				if(gameType <= TYPE_HVM_AI_SHIELDS_GAME) {
					// Set League Tables
					statsElement s;
				
					for(int i = 0;i < stats.size();i++) {
						s = (statsElement)stats.elementAt(i);
					
						for(int x = 0;x < userList.size();x++) {
							ue = (userElement)userList.elementAt(x);
						
							if(s.uid == ue.uid) {
								ue.played+=s.played;	
								ue.won+=s.won;	
								ue.drawn+=s.drawn;	
								ue.lost+=s.lost;	
								ue.killsFor+=s.killsFor;	
								ue.killsAgainst+=s.killsAgainst;	
								ue.points+=s.points;	
							
								userList.setElementAt(ue, x);
							}	
						}
					}
				
					// Pack GwoNET data
					if(gwoNetOnline) {
						String data = "cmdId=405&sid="+sid+"&num="+stats.size();
					
						for(int i = 0;i < stats.size();i++) {
							s = (statsElement)stats.elementAt(i);
						
							data+="&uid"+i+"="+s.uid;
							data+="&won"+i+"="+s.won;
							data+="&drawn"+i+"="+s.drawn;
							data+="&lost"+i+"="+s.lost;
							data+="&shots"+i+"="+s.shots;
							data+="&kills"+i+"="+s.killsFor;
							data+="&hits"+i+"="+s.killsAgainst;
							data+="&points"+i+"="+s.points;
						}
					
						println(data);
						gndl.postPassiveData(data);
					}
				}
				
				gameClock = 0;
				gameState = STATE_END_OF_GAME;
			}
		}
		else if(gameState == STATE_END_OF_GAME) {
			if(gameClock == 1) {
				sortUserList();
			
				writePlayerLeague();
				gamesPlayed++;
				
				// Reset Data
				usersOnline = 0;
	 			usersPlaying = 0;
				usersFired = 0;
				usersHyper = 0;
				usersWatching = 0;
				usersDead = 0;
				usersFinished = 0;
	 			aiPlaying = 0;
				aiFired = 0;
				aiHyper = 0;
				aiDead = 0;
				
				aiController = 0;
				
				for(int i = 0;i < userList.size();i++) {
					ue = (userElement)userList.elementAt(i);
					
					if(ue.online)
						usersOnline++;
						
					ue.playing = false;
					ue.watching = false;
					ue.dead = false;
					ue.diedThisShot = false;
					ue.fired = false;
					ue.hyper = false;
					ue.finished = false;
					ue.voteDraw = false;
					ue.idle = false;
					
					ue.missileCollide = false;
					ue.collideUid = 0;
					ue.collideNumber = 0;
					ue.finishTime = 0;
					
					userList.setElementAt(ue, i);
				}
			
				scenarioPlayingText = "None";			
				watchList = new Vector();
				aiList = new Vector();

				for(int x = 0;x < userList.size();x++) {
					ue = (userElement)userList.elementAt(x);
				
					if(ue.online) {
						bufferLeague(x);
						sendMessageToPlayer(SERVER, ue.uid, 190, gbb.data());
					}
				}
			}
			else if(gameClock >= 10) {		
				if(usersOnline < 2)
					sendEmptyMessageToGroup(SERVER, SEND_ALL, 103);
			
				gameState = STATE_WAIT_FOR_PLAYERS;
			}
		}
		
		if(usersOnline > 0) {
			secondsRunning++;
			//System.out.println(secondsRunning);
		}
		
		totalDataIn+=(long)dataIn;
		totalDataOut+=(long)dataOut;
		
		updateGUI();
			
		dataIn = 0;
		dataOut = 0;
		stateFinished = true;
	}//};
	
	private void latencyControl() {
		userElement ue;
		//int fastUid = 0, fastTime = 2000000;
		//boolean adjusted = false;
		
		int totalTime=0;
		double averagedTime=0.0;
					
		for(int i = 0;i < userList.size();i++) {
			ue = (userElement)userList.elementAt(i);
						
			if(ue.online && (ue.playing || ue.watching)) {
				System.out.println("Time "+ue.uid+" "+ue.finishTime);
				totalTime+=(int)ue.finishTime;
			}
		}

		averagedTime = (double)totalTime/(double)(usersPlaying+usersWatching);
		System.out.println("Average "+averagedTime);
		
		// also adjust fps
		gbb.clear();
		gbb.bufferBoolean(true);
		
		for(int i = 0;i < userList.size();i++) {
			ue = (userElement)userList.elementAt(i);
						
			if(ue.playing || ue.watching) {
				if(ue.finishTime < (averagedTime-4000.0)) {
					System.out.println("Adjusting fast uid:"+ue.uid);
					sendMessageToPlayer(SERVER, ue.uid, 141, gbb.data());					
				}
				
				if(ue.finishTime > (averagedTime+4000.0)) {
					System.out.println("Adjusting slow uid:"+ue.uid);
					sendMessageToPlayer(SERVER, ue.uid, 142, gbb.data());					
				}
			}
		}
				
		/*for(int i = 0;i < userList.size();i++) {
			ue = (userElement)userList.elementAt(i);
						
			if(ue.playing || ue.watching) {
				if(ue.finishTime < fastTime) {
					//System.out.println("Fast "+ue.uid+" "+ue.finishTime);
					fastUid = ue.uid;
					fastTime = ue.finishTime;
				}							
			}
		}
					
		// also adjust fps
		gbb.clear();
		gbb.bufferBoolean(true);
					
		// Reset Values
		for(int i = 0;i < userList.size();i++) {
			ue = (userElement)userList.elementAt(i);
						
			if(ue.playing || ue.watching) {
				if((double)fastTime*1.66 < (double)ue.finishTime) {
					// Adjust
					//System.out.println("Adjusting slow uid:"+ue.uid+" fast:"+fastTime+" slow:"+ue.finishTime);
					sendMessageToPlayer(SERVER, ue.uid, 142, gbb.data());
					adjusted = true;
				}

			}
						
			ue.finishTime = 0;
					
			userList.setElementAt(ue, i);
		}
					
		if(adjusted) {
			// Adjust
			//System.out.println("Adjusting fast uid:"+fastUid);
			sendMessageToPlayer(SERVER, fastUid, 141, gbb.data());
		}	*/
	}
	
	private void calculateAwards() {
		statsElement s;
		teamElement t;
		userElement ue;
		
		int awardCheckInt = 0;
		String awardCheckName = "";
		int awardCheckUid = 0;
		
		Vector awardNames = new Vector();
		Vector awardUids = new Vector();
		String awardString = "";
		
		// Won
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(s.won == 1) {
				if(awardString.equals(""))
					awardString = "Winner(s) (+"+s.points+"): "+s.displayName;
				else
					awardString += ", "+s.displayName;
			}	
		}
		if(!awardString.equals("")) {
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}
		
		// Draw
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(s.drawn == 1) {
				if(awardString.equals(""))
					awardString = "Drawers (+"+s.points+"): "+s.displayName;
				else
					awardString += ", "+s.displayName;
			}	
		}
		if(!awardString.equals("")) {
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}
		
		// Quitters
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(s.played == 0 && s.won == 0 && s.drawn == 0 && s.lost == 0) {
				if(awardString.equals(""))
					awardString = "Quitter(s): "+s.displayName;
				else
					awardString += ", "+s.displayName;

				updatePlayerStats(s.uid, 1, 0, 0, 1, 0, 0, 0);
			}	
		}
		if(!awardString.equals("")) {
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}		
		
		// Team
		if(gameType == TYPE_TEAM_GAME || gameType == TYPE_TEAM_SHIELDS_GAME || gameType == TYPE_HVM_AI_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME || gameType == TYPE_TEAM_AI_GAME || gameType == TYPE_TEAM_AI_SHIELDS_GAME) {
			// No Fatality
			awardString = "";
			for(int i = 0;i < teamList.size();i++) {
				t = (teamElement)teamList.elementAt(i);
				
				if(t.usersDead == 0) {
					for(int x = 0;x < userList.size();x++) {
						ue = (userElement)userList.elementAt(x);
						
						if(ue.team == i && ue.playing) {
							if(awardString.equals(""))
								awardString = "No Fatality Bonus (+1): "+ue.displayName;
							else
								awardString += ", "+ue.displayName;	
							
							updatePlayerStats(ue.uid, 0, 0, 0, 0, 0, 0, 1);
						}
					}	
				}	
			}
			if(!awardString.equals("")) {
				gbb.clear();
				gbb.bufferString(awardString);
				sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
			}
			
			// Friendly Fire
			awardString = "";
			for(int i = 0;i < stats.size();i++) {
				s = (statsElement)stats.elementAt(i);
			
				if(s.hitTeam) {
					if(awardString.equals(""))
						awardString = "Friendly Fire Incident(s) (-2): "+s.displayName;
					else
						awardString += ", "+s.displayName;
						
					updatePlayerStats(s.uid, 0, 0, 0, 0, 0, 0, -2);
				}	
			}
			if(!awardString.equals("")) {
				gbb.clear();
				gbb.bufferString(awardString);
				sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
			}
		}			
			
		// Massacre Award
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(s.won == 1 && usersPlaying >= 5 && s.kills == usersPlaying-1) {
				if(awardString.equals(""))
					awardString = "Massacre Award(s) (+3): "+s.displayName+" ("+s.kills+" Kills)";
				else
					awardString += ", "+s.displayName+" ("+s.kills+" Kills)";
						
				updatePlayerStats(s.uid, 0, 0, 0, 0, 0, 0, 3);			
			}	
		}
		if(!awardString.equals("")) {
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}
			
		// Rambo Award
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(((double)s.kills/(double)usersPlaying)*100.0 > 66.0) {
				if(awardString.equals(""))
					awardString = "Rambo Award(s) (+2): "+s.displayName+" ("+s.kills+" Kills)";
				else
					awardString += ", "+s.displayName+" ("+s.kills+" Kills)";
						
				updatePlayerStats(s.uid, 0, 0, 0, 0, 0, 0, 2);			
			}	
		}
		if(!awardString.equals("")) {
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}

		// Sniper Award
		awardNames = new Vector();
		awardUids = new Vector();
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(s.shots >= 2 && s.kills == s.shots) {
				awardNames.addElement(s.displayName);
				awardUids.addElement(new Integer(s.uid));
			}
		}	
		if(awardNames.size() > 0) {
			for(int i = 0;i < awardNames.size();i++) {
				if(awardString.equals(""))
					awardString = "Sniper Award(s) (+1): "+(String)awardNames.elementAt(i);
				else
					awardString += ", "+(String)awardNames.elementAt(i);
				
				updatePlayerStats(((Integer)awardUids.elementAt(i)).intValue(), 0, 0, 0, 0, 0, 0, 1);
			}
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}
			
		// Best Shot Award
		awardNames = new Vector();
		awardUids = new Vector();
		awardCheckInt = 0;
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			//System.out.println(s.displayName+" "+s.bulletLife);
			
			if(s.bulletLife > 0 && s.bulletLife > awardCheckInt) {
				awardNames = new Vector();
				awardUids = new Vector();
				awardNames.addElement(s.displayName);
				awardUids.addElement(new Integer(s.uid));
				
				awardCheckInt = s.bulletLife;
			}
			else if(s.bulletLife > 0 && s.bulletLife == awardCheckInt) {
				awardNames.addElement(s.displayName);
				awardUids.addElement(new Integer(s.uid));
			}
		}	
		if(awardNames.size() > 0) {
			for(int i = 0;i < awardNames.size();i++) {
				if(awardString.equals(""))
					awardString = "Best Shot Award(s) (+1): "+(String)awardNames.elementAt(i);
				else
					awardString += ", "+(String)awardNames.elementAt(i);
				
				updatePlayerStats(((Integer)awardUids.elementAt(i)).intValue(), 0, 0, 0, 0, 0, 0, 1);
			}
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}

		// Shield Award
		awardNames = new Vector();
		awardUids = new Vector();
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(s.shield > 0) {
				awardNames.addElement(s.displayName);
				awardUids.addElement(new Integer(s.uid));
			}
		}	
		if(awardNames.size() > 0) {
			for(int i = 0;i < awardNames.size();i++) {
				if(awardString.equals(""))
					awardString = "Unused Shield Award(s) (+1): "+(String)awardNames.elementAt(i);
				else
					awardString += ", "+(String)awardNames.elementAt(i);
				
				updatePlayerStats(((Integer)awardUids.elementAt(i)).intValue(), 0, 0, 0, 0, 0, 0, 1);
			}
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}

		// Mine Kill Award
		awardNames = new Vector();
		awardUids = new Vector();
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(s.mineKill && scenarioPlaying != 19) {
				awardNames.addElement(s.displayName);
				awardUids.addElement(new Integer(s.uid));
			}
		}	
		if(awardNames.size() > 0) {
			for(int i = 0;i < awardNames.size();i++) {
				if(awardString.equals(""))
					awardString = "Mine Kill Award(s) (+1): "+(String)awardNames.elementAt(i);
				else
					awardString += ", "+(String)awardNames.elementAt(i);
				
				updatePlayerStats(((Integer)awardUids.elementAt(i)).intValue(), 0, 0, 0, 0, 0, 0, 1);
			}
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}

		// Worm Kill Award
		awardNames = new Vector();
		awardUids = new Vector();
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(s.wormKill) {
				awardNames.addElement(s.displayName);
				awardUids.addElement(new Integer(s.uid));
			}
		}	
		if(awardNames.size() > 0) {
			for(int i = 0;i < awardNames.size();i++) {
				if(awardString.equals(""))
					awardString = "Wormhole Kill Award(s) (+1): "+(String)awardNames.elementAt(i);
				else
					awardString += ", "+(String)awardNames.elementAt(i);
				
				updatePlayerStats(((Integer)awardUids.elementAt(i)).intValue(), 0, 0, 0, 0, 0, 0, 1);
			}
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}
		
		// Most Useless
		awardNames = new Vector();
		awardUids = new Vector();
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(s.killsFor == 0) {
				awardNames.addElement(s.displayName);
				awardUids.addElement(new Integer(s.uid));
			}
		}	
		if(awardNames.size() > 0) {
			for(int i = 0;i < awardNames.size();i++) {
				if(awardString.equals(""))
					awardString = "Most Useless Player(s): "+(String)awardNames.elementAt(i);
				else
					awardString += ", "+(String)awardNames.elementAt(i);
			}
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}
		
		// Most Picked On
		awardNames = new Vector();
		awardUids = new Vector();
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(s.hits > 1) {
				awardNames.addElement(s.displayName);
				awardUids.addElement(new Integer(s.uid));
			}
		}	
		if(awardNames.size() > 0) {
			for(int i = 0;i < awardNames.size();i++) {
				if(awardString.equals(""))
					awardString = "Most Picked On Player(s): "+(String)awardNames.elementAt(i);
				else
					awardString += ", "+(String)awardNames.elementAt(i);
			}
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}

		// Darwin Award
		awardNames = new Vector();
		awardUids = new Vector();
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(s.hitOwn) {
				awardNames.addElement(s.displayName);
				awardUids.addElement(new Integer(s.uid));
			}
		}	
		if(awardNames.size() > 0) {
			for(int i = 0;i < awardNames.size();i++) {
				if(awardString.equals(""))
					awardString = "Darwin Player Award(s) (-1): "+(String)awardNames.elementAt(i);
				else
					awardString += ", "+(String)awardNames.elementAt(i);
				
				updatePlayerStats(((Integer)awardUids.elementAt(i)).intValue(), 0, 0, 0, 0, 0, 0, -1);
			}
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}

		// 'How did you win?!?' Award
		awardNames = new Vector();
		awardUids = new Vector();
		awardString = "";
		for(int i = 0;i < stats.size();i++) {
			s = (statsElement)stats.elementAt(i);
			
			if(s.won == 1 && s.killsFor == 0 && usersPlaying > 2) {
				awardNames.addElement(s.displayName);
				awardUids.addElement(new Integer(s.uid));
			}
		}	
		if(awardNames.size() > 0) {
			for(int i = 0;i < awardNames.size();i++) {
				if(awardString.equals(""))
					awardString = "'How did you win?!?!' Award(s) (-1): "+(String)awardNames.elementAt(i);
				else
					awardString += ", "+(String)awardNames.elementAt(i);
				
				updatePlayerStats(((Integer)awardUids.elementAt(i)).intValue(), 0, 0, 0, 0, 0, 0, -1);
			}
			gbb.clear();
			gbb.bufferString(awardString);
			sendMessageToGroup(SERVER, SEND_ACTIVE, 125, gbb.data());
		}
	}
	
	private void bufferLeague(int pos) {		
		int page = 0;
		int numOfPages = 1+(userList.size()/25);
		int numOfElements = 0;
		userElement ue;
			
		if(userList.size() % 25 == 0) {
			numOfPages--;
		}
			
		page = 1+(pos/25);
					
		if(page*25 < userList.size()) {
			numOfElements = 25;
		}
		else {
			numOfElements = userList.size()-((page-1)*25);
		}
					
		do {
			gbb.clear();
			gbb.bufferInt(page);
			gbb.bufferInt(numOfPages);
			gbb.bufferInt(numOfElements);
			for(int y = 0;y < numOfElements;y++) {
				ue = (userElement)userList.elementAt(((page-1)*25)+y);
				gbb.bufferInt(((page-1)*25)+(y+1));
				gbb.bufferString(ue.displayName);
				gbb.bufferInt(ue.uid);
				gbb.bufferBoolean(ue.online);
				gbb.bufferBoolean(ue.playing);
				gbb.bufferInt(ue.played);
				gbb.bufferInt(ue.won);
				gbb.bufferInt(ue.drawn);
				gbb.bufferInt(ue.lost);
				gbb.bufferInt(ue.killsFor);
				gbb.bufferInt(ue.killsAgainst);
				gbb.bufferInt(ue.points);
			}
		}while(!gbb.checksum());
	}
	
	private String checkUid(int uid, String ip) {
		kickElement k;
		banElement b;
		
		for(int i = 0;i < kickList.size();i++) {
			k = (kickElement)kickList.elementAt(i);
			
			if(uid == k.uid || ip.equals(k.ip)) {
				if(k.kickTime+600000 < System.currentTimeMillis()) {
					kickList.removeElementAt(i);
					
					return "OK";
				}
				else {
					return "CHECK FAILED";
				}
			}
		}
		
		for(int i = 0;i < banList.size();i++) {
			b = (banElement)banList.elementAt(i);
			
			if(uid == b.uid || ip.equals(b.ip)) {
				return "CHECK FAILED";
			}
		}
		
		return "OK";
	}
	
	public synchronized void processMessage(gwoDataInputStream ios, byte b) {
		userElement ue;
		aiElement ae;
		
		try {
			// Find next valid message
			if(b != 66) {
				println("*** Data Invalid (Scrap Data): "+b);
				println("");
				do {
					if(ios.available() > 0) {
						b = ios.readByte();
						print("["+b+"]");
					}
					else {
						return;	
					}
				} while(b != 66);
				println("");
				ios.readInt(); // size
			}
			
			int cmdId = ios.readInt();
			int toUid = ios.readInt();
			int fromUid = ios.readInt();
		
			println("--> "+cmdId+" *** From: "+fromUid+" *** To: "+toUid);
		
			if(b == 66) {
				if(cmdId == 200) { // Initial Login + uid
				}
				else if(cmdId == 201) { // Initial Login
					String name = ios.readString();
					int uid = ios.readInt();
					String pass = ios.readString();
					
					// -- Check name -------------------------------------------------------------------
					
					// Trim whitespace
					name = name.trim();
					
					// Clip size
					if(name.length() > 25) {
						name = name.substring(0, 25);	
					}
					
					// Blank name test
					String nameTest = "";
					while(nameTest.length() < name.length()) {
						nameTest = nameTest+" ";	
					}
					
					if(name.equals(nameTest) || name.length() == 0) {
						name = ""+uid;
					}
					// ---------------------------------------------------------------------------------
					
					
					// GwoNET
					String result = "OK";
					
					for(int i = 0;i < preUserList.size();i++) {
						ue = (userElement)preUserList.elementAt(i);
				
						if(ue.uid == fromUid) {
							result = checkUid(uid, ue.s.getInetAddress().getHostAddress());
						}
					}
					
					if(result.equals("OK") && gwoNetOnline) {
						println("login: "+uid);
						result = gndl.postActiveData("cmdId=404&sid="+sid+"&uid="+uid+"&name="+name+"&pass="+pass);
					}
						
					if(result.equals("SERVER OFFLINE")) {
						gwoNetOnline = false;
						sid = 0;
						result = "OK";
						
						gsic.recheckIp();
						
						if(gwoNetOnline) {
							result = gndl.postActiveData("cmdId=404&sid="+sid+"&uid="+uid+"&name="+name+"&pass="+pass);
						}
						
						gbb.clear();
						gbb.bufferBoolean(gwoNetOnline);
						sendMessageToGroup(SERVER, SEND_ALL, 137, gbb.data());
					}
					
					if(maxPlayers > 0 && usersOnline == maxPlayers) {
						result = "MAXIMUM PLAYERS";	
					}
				
					if(result.equals("OK")) {						
						for(int i = 0;i < preUserList.size();i++) {
							ue = (userElement)preUserList.elementAt(i);
				
							if(ue.uid == fromUid) {
								boolean isNew = true;
								boolean isValid = true;
								userElement ue2;
						
								for(int x = 0;x < userList.size();x++) {
									ue2 = (userElement)userList.elementAt(x);
								
									if(uid == ue2.uid) {
										isNew = false;
										
										if(!ue2.online) {
											ue2.displayName = name;
											ue2.password = pass;
											ue2.online = true;
											ue2.ping = false;
											ue2.s = ue.s;
											ue2.ios = ue.ios;
											ue2.dos = ue.dos;
											ue2.gsl = ue.gsl;
											ue2.gsl.changeUid(uid);
											
											preUserList.removeElementAt(i);
									
											userList.setElementAt(ue2, x);
											
											usersOnline++;
									
											// Send status online to others
											gbb.clear();
											gbb.bufferString(ue2.displayName);
											gbb.bufferBoolean(ue2.online);
											sendMessageToGroup(ue2.uid, SEND_ALL_EXCEPT, 192, gbb.data());
										
											bufferLeague(x);
										}
										else {
											sendEmptyMessageToPlayer(SERVER, fromUid, 137);
											preUserList.removeElementAt(i);
											isValid = false;
										}
									}
								}
							
								if(isValid) {
									if(isNew) {
										usersOnline++;							
										ue.displayName = name;
										ue.password = pass;
										ue.uid = uid;
										ue.gsl.changeUid(uid);
								
										userList.addElement(ue);
											
										preUserList.removeElementAt(i);
								
										// Send league element to others
										do {
											gbb.clear();
											gbb.bufferInt(userList.size());
											gbb.bufferString(name);
											gbb.bufferInt(uid);
											gbb.bufferBoolean(ue.online);
											gbb.bufferBoolean(ue.playing);
											gbb.bufferInt(ue.played);
											gbb.bufferInt(ue.won);
											gbb.bufferInt(ue.drawn);
											gbb.bufferInt(ue.lost);
											gbb.bufferInt(ue.killsFor);
											gbb.bufferInt(ue.killsAgainst);
											gbb.bufferInt(ue.points);
										}while(!gbb.checksum());
										sendMessageToGroup(ue.uid, SEND_ALL_EXCEPT, 191, gbb.data());
								
										bufferLeague(userList.size()-1);
									}
							
									sendMessageToPlayer(SERVER, uid, 190, gbb.data());
								
									gbb.clear();
									gbb.bufferInt(uid);	
									gbb.bufferString(name);
									gbb.bufferBoolean(gwoNetOnline);		
									sendMessageToPlayer(SERVER, uid, 101, gbb.data());
			
									if(usersOnline < 2 && gameState == STATE_WAIT_FOR_PLAYERS)
										sendEmptyMessageToPlayer(SERVER, uid, 103); 
									else if(gameState == STATE_WAIT_FOR_NEW_GAME)
										sendEmptyMessageToPlayer(SERVER, uid, 105); 
									else if(gameState > STATE_WAIT_FOR_NEW_GAME)
										sendEmptyMessageToPlayer(SERVER, uid, 104);
								}
							}
						}	
					}
					else {
						for(int i = 0;i < preUserList.size();i++) {
							ue = (userElement)preUserList.elementAt(i);
				
							if(ue.uid == fromUid) {
								if(result.equals("CHECK FAILED"))
									sendEmptyMessageToPlayer(SERVER, fromUid, 133);
								else if(result.equals("ONLINE ALREADY"))
									sendEmptyMessageToPlayer(SERVER, fromUid, 134);
								else if(result.equals("PASS INCORRECT"))
									sendEmptyMessageToPlayer(SERVER, fromUid, 135);
								else if(result.equals("UID NOT EXIST"))
									sendEmptyMessageToPlayer(SERVER, fromUid, 136);
								else if(result.equals("MAXIMUM PLAYERS"))
									sendEmptyMessageToPlayer(SERVER, fromUid, 143);
								else
									sendEmptyMessageToPlayer(SERVER, fromUid, 137);
								
								preUserList.removeElementAt(i);
							}
						}		
					}
				}
				else if(cmdId == 202) { // Join Game
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == fromUid && (gameState == STATE_WAIT_FOR_PLAYERS || gameState == STATE_WAIT_FOR_NEW_GAME)) {
							ue.playing = !ue.playing;
							userList.setElementAt(ue, i);
									
							// Send status joined to others
							gbb.clear();
							gbb.bufferBoolean(ue.playing);
							sendMessageToGroup(ue.uid, SEND_ALL, 193, gbb.data());
							
							if(ue.playing) {
								sendEmptyMessageToPlayer(SERVER, fromUid, 107);
								usersPlaying++;
							}
							else {
								sendEmptyMessageToPlayer(SERVER, fromUid, 120);
								usersPlaying--;
								
								if(ue.finished) {
									ue.finished = false;
									usersFinished--;
								}
							}
						}
						
						userList.setElementAt(ue, i);
					}
				}
				else if(cmdId == 203) { // Player fired
					atSynchPoint = false;
					
					int angle = ios.readInt();
					int power = ios.readInt();
					
					gbb.clear();
					gbb.bufferInt(angle);
					gbb.bufferInt(power);
					sendMessageToGroup(fromUid, SEND_ACTIVE, 113, gbb.data());
					
					playerElement p;
					for(int i = 0;i < players.size();i++) {
						p = (playerElement)players.elementAt(i);
				
						if(p.uid == fromUid) {
							p.fired = true;
							p.hyper = false;
							p.angle = angle;
							p.power = power;
							players.setElementAt(p, i);
						}
					}
					
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == fromUid) {
							if(!ue.fired) {
								usersFired++;
							}
							ue.fired = true;
							userList.setElementAt(ue, i);
						}
					}
				}
				else if(cmdId == 204) { // Player hyper
					atSynchPoint = false;
				
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == fromUid) {
							if(!ue.hyper) {
								usersHyper++;
							}
							ue.hyper = true;
							userList.setElementAt(ue, i);
						}
					}
					
					playerElement p;
					for(int i = 0;i < players.size();i++) {
						p = (playerElement)players.elementAt(i);
				
						if(p.uid == fromUid) {
							p.fired = false;
							p.hyper = true;
							players.setElementAt(p, i);
						}
					}
				}
				else if(cmdId == 205) { // I am dead
					int killerUid = ios.readInt();
					
					//System.out.println("die");
					
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == fromUid) {
							if(!ue.dead) {
								//System.out.println("now dead");
								ue.dead = true;
								ue.diedThisShot = true;
								ue.hyper = false;
								usersDead++;
							
								updatePlayerStats(ue.uid, 0, 0, 0, 0, 0, 1, 0);
								updatePlayerStats(killerUid, 0, 0, 0, 0, 1, 0, 0);
							
								if(gameType == TYPE_TEAM_GAME || gameType == TYPE_TEAM_SHIELDS_GAME || gameType == TYPE_HVM_AI_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME || gameType == TYPE_TEAM_AI_GAME || gameType == TYPE_TEAM_AI_SHIELDS_GAME) {
									teamElement te = (teamElement)teamList.elementAt(ue.team);
									te.usersDead++;
									te.diedThisShot = true;
									teamList.setElementAt(te, ue.team);
									
									println("!!! TEAM DEATH !!!");
									println("team "+ue.team);
									println("play "+te.usersPlaying);
									println("dead "+te.usersDead);
								}
							}						
						}
						
						userList.setElementAt(ue, i);
					}
					
					for(int i = 0;i < aiList.size();i++) {
						ae = (aiElement)aiList.elementAt(i);
				
						if(ae.uid == killerUid) {
							ae.target = -1;
							
							updatePlayerStats(killerUid, 0, 0, 0, 0, 1, 0, 0);
								
							if(gameType == TYPE_TEAM_SHIELDS_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME || gameType == TYPE_TEAM_AI_SHIELDS_GAME) {
								if(ae.fired) {
									ae.shields++;
									ae.fired = false;
								}
							}							
						}
						
						aiList.setElementAt(ae, i);
					}
					
					playerElement p;
					for(int i = 0;i < players.size();i++) {
						p = (playerElement)players.elementAt(i);
				
						if(p.uid == fromUid) {
							p.status = 2;
							p.hyper = false;
							players.setElementAt(p, i);
						}
					}
				}
				else if(cmdId == 206) { // Player finished
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == fromUid) {
							ue.finished = true;
							ue.finishTime = System.currentTimeMillis()-firedTime;
							userList.setElementAt(ue, i);
							
							usersFinished++;
						}
					}
				}
				else if(cmdId == 207) { // Player message
					int type = ios.readUnsignedByte();
					String txt = ios.readString();
					
					userElement ue2;
					
					gbb.clear();
					gbb.bufferString(txt);
					
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == fromUid) {
							if(type == 0) { // All
								if(ue.playing || ue.watching) 
									sendMessageToGroup(fromUid, SEND_ACTIVE, 119, gbb.data());
								else
									sendMessageToGroup(fromUid, SEND_INACTIVE, 119, gbb.data());
							}
							else if(type == 1) { // Team
								for(int x = 0;x < userList.size();x++) {
									ue2 = (userElement)userList.elementAt(x);
									
									if(ue2.team == ue.team) {
										sendMessageToPlayer(fromUid, ue2.uid, 119, gbb.data());
									}
								}
							}
							else if(type == 2) { // Private
								sendMessageToPlayer(fromUid, fromUid, 119, gbb.data());
								sendMessageToPlayer(fromUid, toUid, 119, gbb.data());
								
								for(int x = 0;x < aiList.size();x++) {
									ae = (aiElement)aiList.elementAt(x);
									
									if(ae.uid == toUid) {
										double seed = Math.random();
										gbb.clear();
										
										if(seed < 0.10) {
											gbb.bufferString(ae.displayName+" [private] => What?");
											sendMessageToPlayer(toUid, fromUid, 119, gbb.data());
										}
										else if(seed < 0.20) {
											gbb.bufferString(ae.displayName+" [private] => Shut up");
											sendMessageToPlayer(toUid, fromUid, 119, gbb.data());
										}
										else if(seed < 0.30) {
											gbb.bufferString(ae.displayName+" [private] => Ya like cheeeese?");
											sendMessageToPlayer(toUid, fromUid, 119, gbb.data());
										}
										else if(seed < 0.40) {
											gbb.bufferString(ae.displayName+" [private] => My ass may be dumb, but I ain't a dumbass");
											sendMessageToPlayer(toUid, fromUid, 119, gbb.data());
										}
										else if(seed < 0.50) {
											gbb.bufferString(ae.displayName+" [private] => Your dead, biatch");
											sendMessageToPlayer(toUid, fromUid, 119, gbb.data());
										}
										else if(seed < 0.60) {
											gbb.bufferString(ae.displayName+" [private] => What did u say?");
											sendMessageToPlayer(toUid, fromUid, 119, gbb.data());
										}
										else if(seed < 0.70) {
											gbb.bufferString(ae.displayName+" [private] => Whoop-ti-doo");
											sendMessageToPlayer(toUid, fromUid, 119, gbb.data());
										}
										else if(seed < 0.80) {
											gbb.bufferString(ae.displayName+" [private] => You talkin' to me?");
											sendMessageToPlayer(toUid, fromUid, 119, gbb.data());
										}
										else if(seed < 0.90) {
											gbb.bufferString(ae.displayName+" [private] => Eh?");
											sendMessageToPlayer(toUid, fromUid, 119, gbb.data());
										}
									}
								}								
							}
						}
					}
				}
				else if(cmdId == 208) { // Player Stats return	
					int shots = ios.readInt();
					int bulletLife = ios.readInt();
					int kills = ios.readInt();
					int hits = ios.readInt();
					boolean hitOwn = ios.readBool();
					boolean hitTeam = ios.readBool();
					int shield = ios.readInt();
					boolean mineKill = ios.readBool();
					boolean wormKill = ios.readBool();
					
					//System.out.println(shots+" "+bulletLife+" "+kills+" "+hits+" "+hitOwn+" "+hitTeam+" "+shield);
					
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == fromUid) {
							ue.finished = true;
							userList.setElementAt(ue, i);
							
							usersFinished++;
						}
					}
					
					statsElement s;
					for(int i = 0;i < stats.size();i++) {
						s = (statsElement)stats.elementAt(i);
						
						if(s.uid == fromUid) {
							s.shots = shots;
							s.bulletLife = bulletLife;
							s.kills = kills;
							s.hits = hits;
							s.hitOwn = hitOwn;
							s.hitTeam = hitTeam;
							s.shield = shield;
							s.mineKill = mineKill;
							s.wormKill = wormKill;
							
							stats.setElementAt(s, i);
						}
					}
				}
				else if(cmdId == 209) { // Missile Collide
					int pUid = ios.readInt();
					int qUid = ios.readInt();
					
					// user
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == pUid || ue.uid == qUid) {
							if(!ue.missileCollide) {
								ue.missileCollide = true;
								
								if(ue.uid == pUid) {
									if(ue.collideUid == qUid) {
										ue.collideNumber++;	
									}
									else {
										ue.collideUid = qUid;
										ue.collideNumber = 1;
									}
								}
								else {
									if(ue.collideUid == pUid) {
										ue.collideNumber++;	
									}
									else {
										ue.collideUid = pUid;
										ue.collideNumber = 1;
									}
								}
									
								if(ue.collideNumber == 3) {
									ue.hyper = true;
									ue.collideNumber = 0;
								}							
							}
						}
						
						userList.setElementAt(ue, i);
					}
					
					// ai
					for(int i = 0;i < aiList.size();i++) {
						ae = (aiElement)aiList.elementAt(i);
				
						if(ae.uid == pUid || ae.uid == qUid) {
							if(!ae.missileCollide) {
								ae.missileCollide = true;
								
								if(ae.uid == pUid) {
									if(ae.collideUid == qUid) {
										ae.collideNumber++;	
									}
									else {
										ae.collideUid = qUid;
										ae.collideNumber = 1;
									}
								}
								else {
									if(ae.collideUid == pUid) {
										ae.collideNumber++;	
									}
									else {
										ae.collideUid = pUid;
										ae.collideNumber = 1;
									}
								}
									
								if(ae.collideNumber == 3) {
									ae.hyper = true;
									ae.collideNumber = 0;
								}							
							}
						}
						
						aiList.setElementAt(ae, i);
					}				
				}
				else if(cmdId == 210) { // He is dead (ai only)
					int killerUid = ios.readInt();
					int killedUid = ios.readInt();
					
					//System.out.println("die");
					
					for(int i = 0;i < aiList.size();i++) {
						ae = (aiElement)aiList.elementAt(i);
				
						if(ae.uid == killedUid) {
							if(!ae.dead) {
								//System.out.println("now dead");
								ae.dead = true;
								ae.diedThisShot = true;
								ae.hyper = false;
								aiDead++;
							
								updatePlayerStats(ae.uid, 0, 0, 0, 0, 0, 1, 0);
							
								if(gameType == TYPE_TEAM_GAME || gameType == TYPE_TEAM_SHIELDS_GAME || gameType == TYPE_HVM_AI_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME || gameType == TYPE_TEAM_AI_GAME || gameType == TYPE_TEAM_AI_SHIELDS_GAME) {
									teamElement te = (teamElement)teamList.elementAt(ae.team);
									te.usersDead++;
									te.diedThisShot = true;
									teamList.setElementAt(te, ae.team);
									
									println("!!! TEAM DEATH !!!");
									println("team "+ae.team);
									println("play "+te.usersPlaying);
									println("dead "+te.usersDead);
								}
							}						
						}
						else if(ae.uid == killerUid) {
							ae.target = -1;
							
							updatePlayerStats(killerUid, 0, 0, 0, 0, 1, 0, 0);
								
							if(gameType == TYPE_TEAM_SHIELDS_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME || gameType == TYPE_TEAM_AI_SHIELDS_GAME) {
								if(ae.fired) {
									ae.shields++;
									ae.fired = false;
								}
							}							
						}
						
						aiList.setElementAt(ae, i);
					}
					
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == killerUid) {
							updatePlayerStats(killerUid, 0, 0, 0, 0, 1, 0, 0);
						}
						
						userList.setElementAt(ue, i);
					}
					
					playerElement p;
					for(int i = 0;i < players.size();i++) {
						p = (playerElement)players.elementAt(i);
				
						if(p.uid == killedUid) {
							p.status = 2;
							p.hyper = false;
							players.setElementAt(p, i);
						}
					}
				}
				else if(cmdId == 211) { // Add Watcher
					if(gameState > STATE_WAIT_FOR_NEW_GAME && gameState < STATE_CALCULATE_AWARDS) {
						if(atSynchPoint) {
							for(int i = 0;i < userList.size();i++) {
								ue = (userElement)userList.elementAt(i);
				
								if(ue.uid == fromUid) {				
									sendEmptyMessageToPlayer(SERVER, fromUid, 108);
								
									// Game Scenario
									gbb.clear();
									gbb.bufferByte((byte)scenarioPlaying);
									gbb.bufferByte((byte)gameType);
									gbb.bufferString(scenarioPlayingText);
									sendMessageToPlayer(SERVER, fromUid, 117, gbb.data());
		
									// Galaxy
									planetElement p;
									do {
										gbb.clear();
										gbb.bufferByte((byte)planets.size());
										for(int x = 0;x < planets.size();x++) {
											p = (planetElement)planets.elementAt(x);
				
											gbb.bufferInt((int)p.x);
											gbb.bufferInt((int)p.y);
											gbb.bufferDouble(p.radius);
											gbb.bufferByte((byte)p.color.getRed());
											gbb.bufferByte((byte)p.color.getGreen());
											gbb.bufferByte((byte)p.color.getBlue());
											gbb.bufferDouble(p.M);
											gbb.bufferDouble(p.density);
											gbb.bufferByte((byte)p.shading);
											gbb.bufferByte((byte)p.impact);
										}
									}while(!gbb.checksum());
									sendMessageToPlayer(SERVER, fromUid, 110, gbb.data());
		
									// Starfield
									gbb.clear();
									gbb.bufferByte((byte)(100 + (150.0 * Math.random())));
									gbb.bufferDouble(Math.random());
									sendMessageToPlayer(SERVER, fromUid, 109, gbb.data());
							
									// Mines
									if(mines.size() > 0) {
										mineElement me = (mineElement)mines.elementAt(0);
										do {
											gbb.clear();
											gbb.bufferByte((byte)mines.size());
											gbb.bufferDouble(me.radius);
											for(int x = 0;x < mines.size();x++) {
												me = (mineElement)mines.elementAt(x);	
					
												gbb.bufferInt((int)me.x);
												gbb.bufferInt((int)me.y);
												gbb.bufferByte((byte)me.status);
											}
										}while(!gbb.checksum());
										sendMessageToPlayer(SERVER, fromUid, 139, gbb.data());
									}
				
									// Players
									playerElement pl;
									do {
										gbb.clear();
										gbb.bufferInt(players.size());
										for(int x = 0;x < players.size();x++) {
											pl = (playerElement)players.elementAt(x);
					
											gbb.bufferString(pl.displayName);
											gbb.bufferInt(pl.uid);
											gbb.bufferInt((int)pl.x);
											gbb.bufferInt((int)pl.y);
											gbb.bufferByte(pl.team);
											gbb.bufferByte(pl.status);
											gbb.bufferByte((byte)pl.color.getRed());
											gbb.bufferByte((byte)pl.color.getGreen());
											gbb.bufferByte((byte)pl.color.getBlue());
											gbb.bufferDouble(pl.radius);				
										}
									}while(!gbb.checksum());
									sendMessageToPlayer(SERVER, fromUid, 111, gbb.data());
								
									ue.watching = true;
									usersWatching++;
									userList.setElementAt(ue, i);
								
									sendEmptyMessageToPlayer(SERVER, fromUid, 132);
						
									// Notify others
									gbb.clear();
									gbb.bufferString(ue.displayName);
									sendMessageToGroup(fromUid, SEND_ACTIVE, 131, gbb.data());
								}
							}
						}
						else {
							sendEmptyMessageToPlayer(SERVER, fromUid, 130);
							watchList.addElement(new Integer(fromUid));
						}
					}
				}
				else if(cmdId == 212) { // Watcher finished
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == fromUid) {
							ue.finished = true;
							ue.finishTime = System.currentTimeMillis()-firedTime;
							userList.setElementAt(ue, i);
							
							usersFinished++;
						}
					}
				}
				else if(cmdId == 213) { // Mine Exploded
					int num = ios.readUnsignedByte();				
					mineElement me = (mineElement)mines.elementAt(num);
					me.status = 2;
					mines.setElementAt(me, num);
					
				}
				else if(cmdId == 218) { // Force hyper
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == fromUid) {
							ue.hyper = true;
							userList.setElementAt(ue, i);
						}
					}
					
					playerElement p;
					for(int i = 0;i < players.size();i++) {
						p = (playerElement)players.elementAt(i);
				
						if(p.uid == fromUid) {
							p.hyper = true;
							players.setElementAt(p, i);
						}
					}
				}
				else if(cmdId == 224) { // Player shield
					atSynchPoint = false;
				
					playerElement p;
					for(int i = 0;i < players.size();i++) {
						p = (playerElement)players.elementAt(i);
				
						if(p.uid == fromUid) {
							p.shield++;
							players.setElementAt(p, i);
						}
					}
					
					sendEmptyMessageToGroup(fromUid, SEND_ACTIVE, 138);
				}
				else if(cmdId == 250) { // Vote: Draw
					String name="";
					int votecount = 0;
					boolean alreadyVoted = false, cannotVote = false;
					
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.playing && !ue.dead && gameType <= TYPE_HVM_AI_GAME) {
							if(ue.uid == fromUid) {
								alreadyVoted = ue.voteDraw;
								
								name = ue.displayName;
								ue.voteDraw = true;
								userList.setElementAt(ue, i);
							}
							
							if(ue.voteDraw) {
								votecount++;	
							}								
						}
						else if(ue.uid == fromUid) {
							cannotVote = true;
						}
						
					}
					
					if(votecount == usersPlaying-usersDead) {
						for(int i = 0;i < userList.size();i++) {
							ue = (userElement)userList.elementAt(i);
					
							if(ue.playing) {
								if(!ue.dead) {
									ue.dead = true;
									ue.diedThisShot = true;
									ue.hyper = false;
									usersDead++;
								
									updatePlayerStats(ue.uid, 0, 0, 0, 0, 0, 1, 0);
								
									if(gameType == TYPE_TEAM_GAME || gameType == TYPE_TEAM_SHIELDS_GAME) {
										teamElement te = (teamElement)teamList.elementAt(ue.team);
										te.usersDead++;
										te.diedThisShot = true;
										teamList.setElementAt(te, ue.team);
										
										println("!!! TEAM DEATH !!!");
										println("team "+ue.team);
										println("play "+te.usersPlaying);
										println("dead "+te.usersDead);
									}
								}										
							}
							
							userList.setElementAt(ue, i);
						}
						
						usersFinished = usersPlaying+usersWatching;					
					
						if(gameType < TYPE_TEAM_GAME)
							gameState = STATE_WAIT_FOR_FINISHED1;
						else
							gameState = STATE_WAIT_FOR_FINISHED2;
					}
					else if(cannotVote) {
						gbb.clear();
						gbb.bufferString("You cannot vote for a draw in this game");
						sendMessageToPlayer(SERVER, fromUid, 127, gbb.data());						
					}
					else if(!alreadyVoted) {
						gbb.clear();
						gbb.bufferString(name+" has voted for a draw.  "+(usersPlaying-usersDead-votecount)+" more votes required.");
						sendMessageToGroup(SERVER, SEND_ACTIVE, 127, gbb.data());						
					}
					else {
						gbb.clear();
						gbb.bufferString("You have already voted.");
						sendMessageToPlayer(SERVER, fromUid, 127, gbb.data());												
					}				
				}
				else if(cmdId == 251) { // Vote: Game
					String type = ios.readString();
					
					gbb.clear();
					
					if(type.equalsIgnoreCase("single")) {
						voteSingleGame++;
					
						gbb.bufferString("Vote Cast for Single game.");
						sendMessageToPlayer(SERVER, fromUid, 127, gbb.data());												
					}
					else if(type.equalsIgnoreCase("team")) {
						voteTeamGame++;
					
						gbb.bufferString("Vote Cast for Team game.");
						sendMessageToPlayer(SERVER, fromUid, 127, gbb.data());												
					}
					else if(type.equalsIgnoreCase("ai")) {
						voteAiGame++;
					
						gbb.bufferString("Vote Cast for AI game.");
						sendMessageToPlayer(SERVER, fromUid, 127, gbb.data());												
					}
					else {
						gbb.bufferString("You cannot Vote for "+type+".");
						sendMessageToPlayer(SERVER, fromUid, 127, gbb.data());														
					}			
				}
				else if(cmdId == 260) { // File Transfer: Details
					gbb.clear();
					gbb.bufferLong(ftFile.length());
					sendMessageToPlayer(SERVER, fromUid, 160, gbb.data());		
				}
				else if(cmdId == 261) { // File Transfer: Accept
					if(!ftInProg) {
						ftUid = fromUid;
						ftSegNum = 0;
						ftNum++;
						ftLastSegSent = System.currentTimeMillis();
						ftFis = new FileInputStream("gwo2.jar");
						ftData = new byte[4096];
					
						ftInProg = true;
						sendEmptyMessageToPlayer(SERVER, fromUid, 161);		
					}
					else {
						ftQueue.addElement(new Integer(fromUid));
						sendEmptyMessageToPlayer(SERVER, fromUid, 164);			
					}
				}
				else if(cmdId == 262) { // File Transfer: Data Block
					ftSize = ftFis.read(ftData);
					
					if(ftSize != -1) {
						ftSegNum++;
						ftLastSegSent = System.currentTimeMillis();
						
						do {
							gbb.clear();
							gbb.bufferInt(ftSize);
							gbb.bufferByteArray(ftData, ftSize);
						}while(!gbb.checksum());
						sendMessageToPlayer(SERVER, fromUid, 162, gbb.data());
					}
					else { 
						sendEmptyMessageToPlayer(SERVER, fromUid, 163);		
						ftInProg = false;	
											
						if(ftQueue.size() == 0) {
							ftUid = 0;
							ftFis.close();
							ftData = null;				
						}
						else {
							ftUid = ((Integer)ftQueue.elementAt(0)).intValue();
							ftSegNum = 0;
							ftNum++;
							ftLastSegSent = System.currentTimeMillis();
							ftQueue.removeElementAt(0);
							ftFis = new FileInputStream("gwo2.jar");
							ftData = new byte[4096];
					
							ftInProg = true;
							sendEmptyMessageToPlayer(SERVER, ftUid, 161);							
						}		
					}
				}
				else if(cmdId == 263) { // File Transfer: Cancel
					for(int i = ftQueue.size()-1;i >= 0;i--) {
						int fUid = ((Integer)ftQueue.elementAt(i)).intValue();
			
						if(fromUid == fUid) {
							ftQueue.removeElementAt(i);
						}
					}
				}
				else if(cmdId == 264) { // File Transfer: Resend Data Block
					ftLastSegSent = System.currentTimeMillis();
					do {
						gbb.clear();
						gbb.bufferInt(ftSize);
						gbb.bufferByteArray(ftData, ftSize);
					}while(!gbb.checksum());
					sendMessageToPlayer(SERVER, fromUid, 162, gbb.data());
				}				
				else if(cmdId == 270) { // League Page Request
					int page = ios.readInt();
					int numOfPages = 1+(userList.size()/25);
					int numOfElements = 0;
				
					if(userList.size() % 25 == 0) {
						numOfPages--;
					}
				
					if(page*25 < userList.size()) {
						numOfElements = 25;
					}
					else {
						numOfElements = userList.size()-((page-1)*25);
					}
					
					do {
						gbb.clear();
						gbb.bufferInt(page);
						gbb.bufferInt(numOfPages);
						gbb.bufferInt(numOfElements);
						for(int y = 0;y < numOfElements;y++) {
							ue = (userElement)userList.elementAt(((page-1)*25)+y);
							gbb.bufferInt(((page-1)*25)+(y+1));
							gbb.bufferString(ue.displayName);
							gbb.bufferInt(ue.uid);
							gbb.bufferBoolean(ue.online);
							gbb.bufferBoolean(ue.playing);
							gbb.bufferInt(ue.played);
							gbb.bufferInt(ue.won);
							gbb.bufferInt(ue.drawn);
							gbb.bufferInt(ue.lost);
							gbb.bufferInt(ue.killsFor);
							gbb.bufferInt(ue.killsAgainst);
							gbb.bufferInt(ue.points);
						}
					}while(!gbb.checksum());
					sendMessageToPlayer(SERVER, fromUid, 190, gbb.data());
				}
				else if(cmdId == 280) { // Recall Message				
					int request = ios.readInt();
					
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == fromUid) {
							// Handle same request being resend.
							// If this happens then data must be invalid
							// so best to just ignore it.
							if(ue.lastRequest != request) {
								ue.lastRequest = request;
								userList.setElementAt(ue, i);
								
								recallMessage(request, fromUid);	
							}
						}
					}			
					
					for(int i = 0;i < preUserList.size();i++) {
						ue = (userElement)preUserList.elementAt(i);
				
						if(ue.uid == fromUid) {
							println("Recall from preUser");
							do {
								gbb.clear();
								gbb.bufferInt(ftSize);
								gbb.bufferByteArray(ftData, ftSize);
							}while(!gbb.checksum());
							sendMessageToPlayer(SERVER, fromUid, 162, gbb.data());
						}
					}
				}
				else if(cmdId == 281) { // UberUser: Beep Message
					if(toUid == SERVER) {
						sendEmptyMessageToGroup(fromUid, SEND_ALL, 180);
					}
					else {
						sendEmptyMessageToPlayer(fromUid, toUid, 180);
					}			
				}
				else if(cmdId == 282) { // UberUser: Reset Game
					gbb.clear();
					gbb.bufferString("Resetting Game.  Please Wait...");
					sendMessageToGroup(SERVER, SEND_ACTIVE, 127, gbb.data());	

					gameClock = 0;
					gameState = STATE_END_OF_GAME;
				}
				else if(cmdId == 283) { // UberUser: Login
					String pass = ios.readString();
					
					if(pass.equals(adminPassword)) {
						sendEmptyMessageToPlayer(SERVER, fromUid, 181);	
					}
				}
				else if(cmdId == 284) { // UberUser: Kick
					kickElement k = new kickElement();
					
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == toUid) {
							k.uid = toUid;
							k.ip = ue.s.getInetAddress().getHostAddress();
							k.kickTime = System.currentTimeMillis();
							kickList.addElement(k);
							
							sendEmptyMessageToPlayer(SERVER, toUid, 182);	
						}
					}
					
				}
				else if(cmdId == 285) { // UberUser: Ban
					banElement be = new banElement();
					
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == toUid) {
							be.uid = toUid;
							be.ip = ue.s.getInetAddress().getHostAddress();
							banList.addElement(be);
							
							sendEmptyMessageToPlayer(SERVER, toUid, 183);	
						}
					}
					
				}
				else if(cmdId == 286) { // UberUser: Reset Server
					//clock.stop();
					//clock = null;
					//clock = new Timer(1000, stateMachine);
					//clock.start();
					
					gbb.clear();
					gbb.bufferString("Resetting Server.  Please Wait...");
					sendMessageToGroup(SERVER, SEND_ACTIVE, 127, gbb.data());	

					gameClock = 0;
					gameState = STATE_END_OF_GAME;
				}
				else if(cmdId == 287) { // UberUser: Stop Timer
					//clock.stop();
				}
				else if(cmdId == 288) { // UberUser: Get UID
					String name = ios.readString();
					
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.displayName.indexOf(name) != -1) {
							gbb.clear();
							gbb.bufferInt(ue.uid);
							gbb.bufferString(ue.displayName);
							sendMessageToPlayer(SERVER, fromUid, 184, gbb.data());	
						}
					}
					
				}
				else if(cmdId == 289) { // Quit AI Game					
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == fromUid) {
							if(ue.playing && gameType >= TYPE_SINGLE_AI_GAME) {
								gameClock = 0;
								gameState = STATE_END_OF_GAME;
								
								gbb.clear();
								gbb.bufferString("Quitting Game.  Please Wait...");
								sendMessageToPlayer(SERVER, fromUid, 127, gbb.data());	
							}
						}
					}					
				}
				else if(cmdId == 290) { // Ping return
					for(int i = 0;i < userList.size();i++) {
						ue = (userElement)userList.elementAt(i);
				
						if(ue.uid == fromUid) {
							ue.ping = false;
							userList.setElementAt(ue, i);
						}
					}
				}
		
				b = ios.readByte();
		
				if(b != 99) {
					println("DATA FOOTER INVALID: "+b);
				}
			}
			else {
				println("DATA HEADER INVALID: "+b);
			}
		}
		catch(IOException ioe) {
			println(ioe.toString() + " (18)");
		}
		//println("--> End Message");
	}
	
	private void sortUserList() {
		boolean swap = false;
		userElement uex, uey;
		
		for(int x = 0;x < userList.size();x++) {			
			
			for(int y = x+1;y < userList.size();y++) {
				uex = (userElement)userList.elementAt(x);
				uey = (userElement)userList.elementAt(y);
				
				swap = false;
				
				if(uey.points > uex.points)
					swap = true;
				else if(uey.points == uex.points) {
					if(uey.killsFor-uey.killsAgainst > uex.killsFor-uex.killsAgainst)
						swap = true;
					else if(uey.killsFor-uey.killsAgainst == uex.killsFor-uex.killsAgainst) {
						if((double)uey.points/(double)uey.played > (double)uex.points/(double)uex.played)
							swap = true;
					}
				}
				
				if(swap) {
					userList.setElementAt(uey, x);
					userList.setElementAt(uex, y);
				}
			}
		}
	}
	
	private void hyperSpace() {
		boolean anyHyperspacers = false;
		userElement ue;
		aiElement ae;
		playerElement p;
		int count = 0;
		
		// humans
		for(int i = 0;i < userList.size();i++) {
			ue = (userElement)userList.elementAt(i);
			
			if(ue.playing && !ue.dead && (ue.hyper || scenarioPlaying == 18)) {
				anyHyperspacers = true;
				ue.hyper = false;
				
				for(int k = 0;k < players.size();k++) {
					p = (playerElement)players.elementAt(k);
					players.removeElementAt(k);
					
					if(ue.uid == p.uid) {
						count = 0;
						
						p.hX = p.x;
						p.hY = p.y;
						
						do {
							count++;
							
							if(count < 500) {
								p.x = (0.8 * Math.random() * width + 0.05 * width);
								p.y = (0.8 * Math.random() * height + 0.05 * height);
							}
							else {
								p.x = p.hX;
								p.y = p.hY;
							}
						} while(checkPlayerCollisions(p, 0));
				
						gbb.clear();
						gbb.bufferInt((int)p.x);
						gbb.bufferInt((int)p.y);
						sendMessageToGroup(p.uid, SEND_ACTIVE, 116, gbb.data());
					}
					
					players.insertElementAt(p, k);
				}
			}
		}
		
		// ai
		for(int i = 0;i < aiList.size();i++) {
			ae = (aiElement)aiList.elementAt(i);
			
			if(!ae.dead && (ae.hyper || scenarioPlaying == 18)) {
				anyHyperspacers = true;
				ae.hyper = false;
				ae.target = -1;
				ae.shots = 0;
				
				for(int k = 0;k < players.size();k++) {
					p = (playerElement)players.elementAt(k);
					players.removeElementAt(k);
					
					if(ae.uid == p.uid) {
						count = 0;
						
						p.hX = p.x;
						p.hY = p.y;
						
						do {
							count++;
							
							if(count < 500) {
								p.x = (0.8 * Math.random() * width + 0.05 * width);
								p.y = (0.8 * Math.random() * height + 0.05 * height);
							}
							else {
								p.x = p.hX;
								p.y = p.hY;
							}
						} while(checkPlayerCollisions(p, 0));
				
						gbb.clear();
						gbb.bufferInt((int)p.x);
						gbb.bufferInt((int)p.y);
						sendMessageToGroup(p.uid, SEND_ACTIVE, 116, gbb.data());
					}
					
					players.insertElementAt(p, k);
				}
			}
		}
		
		if(anyHyperspacers) {
			gameState = STATE_HYPERSPACE;
		}
		else {
			gameState = STATE_NEW_SHOT;
		}
	}
	
	private void generateStarfield() {
		gbb.clear();
		gbb.bufferByte((byte)(100 + (150.0 * Math.random())));
		gbb.bufferDouble(Math.random());
		sendMessageToGroup(SERVER, SEND_ACTIVE, 109, gbb.data());
	}
	
	private void generateGalaxy() {
		int numOfPlanets = 0, numOfWorms = 0;
		double seed;
		planets = new Vector();
		planetElement p;
		scenarioPlayingText = "";
		totalMass = 0;
		
		if(scenarioPlaying == 19) {
			scenarioPlaying = 0;
		}
		
		scenarioPlaying += 1 + (int)(5.0 * Math.random());	
		
		if(usersPlaying <= 2 && scenarioPlaying == 16) {
			scenarioPlaying = 6;
		}
		
		if(scenarioPlaying > 19) {
			scenarioPlaying = 19;
		}
		
		switch(scenarioPlaying) {
		case 1: // Planetary
			scenarioPlayingText = "Planetary";
			numOfPlanets = 3 + (int)(12.0 * Math.random());
			
			generateOtherPlanets(numOfPlanets, 1.3, 0.4, 0.4, 0.1, 30.0, 30.0, 10.0, 1, 1, 140.0, 20.0, 110.0, 20.0, 70.0, 20.0);
			break;	
		case 2: // Asteroids
			scenarioPlayingText = "Asteroids";
			numOfPlanets = 10 + (int)(40.0 * Math.random());
			
			generateOtherPlanets(numOfPlanets, 1.5, 1.0, 0.0, 0.0, 7.0, 2.0, 1.0, 1, 1, 110.0, 20.0, 70.0, 20.0, 0.0, 20.0);
			break;
		case 3: // Star system
			scenarioPlayingText = "Star System";
			numOfPlanets = (int)(10.0 * Math.random());
			
			generateOtherPlanets(1, 2.0, 0.1, 0.1, 0.4, 80.0, 80.0, 80.0, 2, 1, 205.0, 30.0, 205.0, 30.0, 15.0, 190.0);
			
			generateOtherPlanets(numOfPlanets, 1.5, 1.0, 0.0, 0.0, 20.0, 5.0, 3.0, 1, 1, 140.0, 20.0, 110.0, 20.0, 70.0, 20.0);
			break;
		case 4: // Binary system
			scenarioPlayingText = "Binary System";
			numOfPlanets = (int)(17.0 * Math.random());
			
			generateOtherPlanets(2, 2.0, 0.3, 0.3, 0.2, 80.0, 80.0, 80.0, 2, 1, 205.0, 30.0, 205.0, 30.0, 15.0, 190.0);
			
			generateOtherPlanets(numOfPlanets, 1.8, 1.0, 0.0, 0.0, 20.0, 5.0, 4.0, 1, 1, 110.0, 20.0, 70.0, 20.0, 0.0, 20.0);
			break;
		case 5: // Jovian
			scenarioPlayingText = "Jovian";
			numOfPlanets = (int)(17.0 * Math.random());
			
			generateOtherPlanets(1, 2.0, 0.1, 0.1, 0.4, 80.0, 80.0, 40.0, 1, 1, 145.0, 100.0, 0.0, 125.0, 0.0, 55.0);
			
			generateOtherPlanets(numOfPlanets, 1.4, 1.0, 0.0, 0.0, 6.0, 6.0, 3.0, 1, 1, 110.0, 20.0, 90.0, 20.0, 0.0, 60.0);
			break;
		case 6: // Supergiant
			scenarioPlayingText = "Supergiant";
			numOfPlanets = (int)(14.0 * Math.random());
			
			generateOtherPlanets(1, 2.0, 3.0, 0.0, -1.0, 0.0, 0.0, (double)height - 50.0, 2, 1, 245.0, 10.0, 10.0, 245.0, 0.0, 45.0);
			
			generateOtherPlanets(numOfPlanets, 1.8, 1.0, 0.0, 0.0, 30.0, 5.0, 9.0, 1, 1, 110.0, 20.0, 70.0, 20.0, 0.0, 20.0);
			break;
		case 7: // Superbinary
			scenarioPlayingText = "Superbinary";
			numOfPlanets = (int)(12.0 * Math.random());
			
			generateOtherPlanets(2, 2.0, 3.0, 0.0, -1.0, 0.2, 0.2, (double)height - 50.0, 2, 1, 245.0, 10.0, 0.0, 245.0, 0.0, 45.0);
			
			generateOtherPlanets(numOfPlanets, 1.8, 1.0, 0.0, 0.0, 10.0, 10.0, 9.0, 1, 1, 110.0, 20.0, 70.0, 20.0, 0.0, 20.0);
			break;
		case 8: // Uneven Binary
			scenarioPlayingText = "Uneven Binary";
			numOfPlanets = (int)(13.0 * Math.random());
			
			generateOtherPlanets(1, 2.0, 3.0, 0.0, -1.0, 0.2, 0.2, (double)height - 50.0, 2, 1, 245.0, 10.0, 0.0, 245.0, 0.0, 45.0);
			generateOtherPlanets(1, 3.0, 04, 0.4, 0.1, 80.0, 80.0, 50.0, 2, 1, 205.0, 30.0, 205.0, 30.0, 15.0, 190.0);
			
			generateOtherPlanets(numOfPlanets, 1.9, 1.0, 0.0, 0.0, 10.0, 10.0, 9.0, 1, 1, 110.0, 20.0, 70.0, 20.0, 0.0, 20.0);
			break;
		case 9: // Red Giant
			scenarioPlayingText = "Red Giant";
			numOfPlanets = (int)(8.0 * Math.random());
			
			generateOtherPlanets(1, 3.0, 0.1, 0.1, 0.4, 80.0, 80.0, 140.0, 2, 1, 245.0, 10.0, 0.0, 115.0, 0.0, 21.0);
			
			generateOtherPlanets(numOfPlanets, 1.65, 1.0, 0.0, 0.0, 20.0, 5.0, 4.0, 1, 1, 110.0, 20.0, 70.0, 20.0, 0.0, 20.0);
			break;
		case 10: // Star Cluster
			scenarioPlayingText = "Star Cluster";
			numOfPlanets = 3 + (int)(6.0 * Math.random());
			
			generateOtherPlanets(numOfPlanets, 2.0, 1.2, 0.0, -0.1, 70.0, 70.0, 30.0, 2, 1, 215.0, 30.0, 205.0, 30.0, 15.0, 190.0);
			break;
		case 11: // Mixture
			scenarioPlayingText = "Mixture";
			numOfPlanets = (int)(3.0 * Math.random());
			
			for(int i = 0;i < numOfPlanets;i++) {
				generateOtherPlanets(1, 0.02, 0.9, 0.0, 0.1, 0.0, 0.0, 3.0, 0, 2, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
				((planetElement)planets.elementAt(i)).M = (80.0 * Math.random() + 80.0 * Math.random() + 140.0) * 500;
			}
			
			numOfPlanets = (int)(8.0 * Math.random());
			
			generateOtherPlanets(numOfPlanets, 2.0, 1.2, 0.0, -0.1, 70.0, 70.0, 30.0, 2, 1, 215.0, 30.0, 205.0, 30.0, 15.0, 190.0);
			
			numOfPlanets = (int)(8.0 * Math.random());
			generateOtherPlanets(numOfPlanets, 1.1, 1.0, 0.0, 0.0, 20.0, 5.0, 4.0, 1, 1, 110.0, 20.0, 70.0, 20.0, 0.0, 20.0);
			break;
		case 12: // White Dwarf
			scenarioPlayingText = "White Dwarf";
			numOfPlanets = (int)(10.0 * Math.random());
			
			generateOtherPlanets(1, 2.0, 0.1, 0.1, 0.4, 80.0, 80.0, 100.5, 2, 1, 255.0, 0.0, 255.0, 0.0, 255.0, 0.0);
			generateOtherPlanets(numOfPlanets, 1.5, 1.0, 0.0, 0.0, 5.0, 5.0, 3.0, 1, 1, 150.0, 20.0, 90.0, 20.0, 60.0, 20.0);
			break;
		case 13: // Worm Hole
			scenarioPlayingText = "Worm Hole";
			numOfPlanets = (int)(10.0 * Math.random());
			seed = Math.random();
			numOfWorms = 1 + (int)(3.0 * Math.random());
			
			for(int i = 0;i < numOfWorms;i++) {
				if(i == 0)
					generateOtherPlanets(1, 2.0, 0.2, 0.2, 0.3, 15.0, 15.0, 15.0, 3, -(numOfWorms - 1), 55.0, 0.0, 255.0, 0.0, 55.0, 0.0);
				else
					generateOtherPlanets(1, 2.0, 0.2, 0.2, 0.3, 15.0, 15.0, 15.0, 3, -(i - 1), 55.0, 0.0, 255.0, 0.0, 55.0, 0.0);
			}
			
			generateOtherPlanets(numOfPlanets, 1.3, 1.0, 0.0, 0.0, 20.0, 20.0, 3.0, 1, 1, 140.0, 20.0, 110.0, 20.0, 70.0, 20.0);
			break;
		case 14: // Dwarfs
			scenarioPlayingText = "Dwarfs";
			numOfPlanets = 4 + (int)(6.0 * Math.random());
			
			generateOtherPlanets(numOfPlanets, 20.0, 0.9, 0.0, 0.1, 3.0, 3.0, 4.0, 2, 1, 255.0, 0.0, 255.0, 0.0, 255.0, 0.0);
			
			for(int i = 0;i < planets.size();i++) {
				((planetElement)planets.elementAt(i)).M = (60.0 * Math.random() + 60.0 * Math.random() + 100.0) * 100;
			}
			break;
		case 15: // Black Holes
			scenarioPlayingText = "Black Holes";
			numOfPlanets = 2 + (int)(3.0 * Math.random());
			
			for(int i = 0;i < numOfPlanets;i++) {
				generateOtherPlanets(1, 0.02, 0.9, 0.0, 0.1, 0.0, 0.0, 10.0, 0, 2, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
				((planetElement)planets.elementAt(i)).M = (80.0 * Math.random() + 80.0 * Math.random() + 140.0) * 500;
			}
			break;
		case 16: // Mystery Round
			scenarioPlayingText = "Mystery Round";
			numOfPlanets = (int)(10.0 * Math.random());
			seed = Math.random();
			numOfWorms = 1 + (int)(3.0 * Math.random());
			
			for(int i = 0;i < numOfWorms;i++) {
				if(i == 0)
					generateOtherPlanets(1, 2.0, 0.2, 0.2, 0.3, 15.0, 15.0, 15.0, 3, -(numOfWorms - 1), 120.0, 0.0, 0.0, 0.0, 255.0, 0.0);
				else
					generateOtherPlanets(1, 2.0, 0.2, 0.2, 0.3, 15.0, 15.0, 15.0, 3, -(i - 1), 120.0, 0.0, 0.0, 0.0, 255.0, 0.0);
			}
			
			generateOtherPlanets(numOfPlanets, 1.3, 1.0, 0.0, 0.0, 20.0, 20.0, 3.0, 1, 1, 140.0, 20.0, 110.0, 20.0, 70.0, 20.0);
			break;
		case 17: // Worm Holes
			scenarioPlayingText = "Worm Holes";
			numOfPlanets = 2 + (int)(16.0 * Math.random());
			
			for(int i = 0;i < numOfPlanets;i++) {
				if(i == 0)
					generateOtherPlanets(1, 2.0, 0.7, 0.2, 0.03, 8.0, 8.0, 8.0, 3, -(numOfPlanets - 1), 55.0, 0.0, 255.0, 0.0, 55.0, 0.0);
				else
					generateOtherPlanets(1, 2.0, 0.7, 0.2, 0.03, 8.0, 8.0, 8.0, 3, -(i - 1), 55.0, 0.0, 255.0, 0.0, 55.0, 0.0);
			}
			
			generateOtherPlanets(numOfPlanets, 0.03, 1.0, 0.0, 0.0, 20.0, 20.0, 3.0, 1, 1, 140.0, 20.0, 110.0, 20.0, 70.0, 20.0);
			break;
		case 18: // Hyperspace
			scenarioPlayingText = "Hyperspace";
			numOfPlanets = 3 + (int)(14.0 * Math.random());
			double d;
			int c;
			
			for(int i = 0;i < numOfPlanets;i++) {
				if(Math.random() < 0.33) {
					d = 500.0 * Math.random();
					c = (int)(255.0 - 40.0 * d);
					
					if(c < 0)
						c = 0;
					else if(c > 255)
						c = 255;
						
					generateOtherPlanets(1, d, 0.9, 0.0, 0.1, 0.0, 0.0, 5.0, 2, 2, c, 0.0, 255.0, 0.0, 0.0, 0.0);
				}
				if(Math.random() > 0.66) {
					if(Math.random() > 0.5)
						d = 250.0 * Math.random();
					else
						d = -250.0 * Math.random();
						
					c = (int)(255.0 - 40.0 * d);
					
					if(c < 0)
						c = 0;
					else if(c > 255)
						c = 255;
						
					generateOtherPlanets(1, d, 0.9, 0.0, 0.1, 0.0, 0.0, 5.0, 2, 2, 255.0, 0.0, 0.0, 0.0, 0.0, 0.0);
				}
				else {
					d = -500.0 * Math.random();
					c = (int)(255.0 - 40.0 * d);
					
					if(c < 0)
						c = 0;
					else if(c > 255)
						c = 255;
						
					generateOtherPlanets(1, d, 0.9, 0.0, 0.1, 0.0, 0.0, 5.0, 2, 2, 255.0, 0.0, c, 0.0, 0.0, 0.0);
				}
			}
			break;
		case 19: // Mine Field
			scenarioPlayingText = "Mine Field";
			break;
		}
			
		if(gameType == TYPE_TEAM_GAME || gameType == TYPE_TEAM_SHIELDS_GAME) {
			scenarioPlayingText = scenarioPlayingText + " (Team Game)";
		}
		
		if(gameType == TYPE_TEAM_AI_GAME || gameType == TYPE_TEAM_AI_SHIELDS_GAME) {
			scenarioPlayingText = scenarioPlayingText + " (Team AI Game)";
		}

		if(gameType == TYPE_SINGLE_AI_GAME || gameType == TYPE_SINGLE_AI_SHIELDS_GAME) {
			scenarioPlayingText = scenarioPlayingText + " (Single AI Game)";
		}

		if(gameType == TYPE_HVM_AI_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME) {
			scenarioPlayingText = scenarioPlayingText + " (Humans Vs Machines Game)";
		}
						
		if(gameType == TYPE_SINGLE_SHIELDS_GAME || gameType == TYPE_TEAM_SHIELDS_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME || gameType == TYPE_SINGLE_SHIELDS_GAME || gameType == TYPE_TEAM_AI_SHIELDS_GAME) {
			scenarioPlayingText = scenarioPlayingText + " [With Shields]";
		}
			
		gbb.clear();
		gbb.bufferByte((byte)scenarioPlaying);
		gbb.bufferByte((byte)gameType);
		gbb.bufferString(scenarioPlayingText);
		sendMessageToGroup(SERVER, SEND_ACTIVE, 117, gbb.data());
		
		do {
			gbb.clear();
			gbb.bufferByte((byte)planets.size());
			for(int i = 0;i < planets.size();i++) {
				p = (planetElement)planets.elementAt(i);
				
				gbb.bufferInt((int)p.x);
				gbb.bufferInt((int)p.y);
				gbb.bufferDouble(p.radius);
				gbb.bufferByte((byte)p.color.getRed());
				gbb.bufferByte((byte)p.color.getGreen());
				gbb.bufferByte((byte)p.color.getBlue());
				gbb.bufferDouble(p.M);
				gbb.bufferDouble(p.density);
				gbb.bufferByte((byte)p.shading);
				gbb.bufferByte((byte)p.impact);
			}
		}while(!gbb.checksum());
		sendMessageToGroup(SERVER, SEND_ACTIVE, 110, gbb.data());
	}
	
	private boolean checkPlanetCollisions(planetElement p) {
		planetElement q;
		
		for(int k = 0;k < planets.size();k++) {
			q = (planetElement)planets.elementAt(k);
					
			if((p.x - q.x) * (p.x - q.x) + (p.y - q.y) * (p.y - q.y) < (10.0 + p.radius + q.radius) * (10.0 + p.radius + q.radius)) {
				return true;
			}
		}
		
		return false;
	}
	
	private void generateMines(double stationSize) {
		mineElement me;
		int count = 0;
		mines = new Vector();
		boolean tooManyCollisions = false;
		int numofmines;
		
		if(scenarioPlaying == 19) {
			numofmines = (int)(30.0 + (100.0 * Math.random()));
		}
		else {
			numofmines = (int)(4.0 + (((double)usersPlaying) * Math.random()));
		}
		
		if(Math.random() < 0.5 || scenarioPlaying == 19) {
			for(int i = 0;i < numofmines;i++) {
				me = new mineElement();
			
				me.status = 0;
				me.radius = stationSize/4.0;
                
                count = 0;
                	
				do {
					count++;
					me.x = width * Math.random();
					me.y = height * Math.random();
			
					if(count > 250) {
						tooManyCollisions = true;
					}
				} while(checkMineCollisions(me) && !tooManyCollisions);
			
				if(!tooManyCollisions) {
					mines.addElement(me);
				}
			}
			
			do {
				gbb.clear();
				gbb.bufferByte((byte)mines.size());
				gbb.bufferDouble(stationSize/4.0);
				for(int i = 0;i < mines.size();i++) {
					me = (mineElement)mines.elementAt(i);	
					
					gbb.bufferInt((int)me.x);
					gbb.bufferInt((int)me.y);
				}
			}while(!gbb.checksum());
			sendMessageToGroup(SERVER, SEND_ACTIVE, 140, gbb.data());
		}
	}
	
	private void generateOtherPlanets(int num, double ds, double ps1, double ps2, double ps3, double rs1, double rs2, double rs3, int ss, int is, double r1, double r2, double g1, double g2, double b1, double b2) {
		planetElement p;
		int count = 0;
		boolean tooManyCollisions = false;
		
		for(int i = 0;i < num;i++) {
			p = new planetElement();
			
			p.density = ds;
			p.color = new Color((int)(r1 + (r2 * Math.random())), (int)(g1 + (g2 * Math.random())), (int)(b1 + (b2 * Math.random())));
			p.radius = rs1 * Math.random() + rs2 * Math.random() + rs3;
			p.M = 2.3*(p.radius * p.radius * p.density);
			p.shading = ss;
			p.impact = is;
			
			count = 0;
                	
			do {
				count++;
				p.x = (ps1 * Math.random() + ps2 * Math.random()) * width + ps3 * width;
				p.y = (ps1 * Math.random() + ps2 * Math.random()) * height + ps3 * height;
			
				if(count > 250) {
					tooManyCollisions = true;
				}
			} while(checkPlanetCollisions(p) && !tooManyCollisions);
			
			if(!tooManyCollisions) {
				planets.addElement(p);
				totalMass += p.M;	
			}
		}
	}
	
	private int addToRandomTeam() {
		int team;
		int maxSize = (usersPlaying+aiPlaying)/teamList.size();
		
		do {
			team = (int)((double)teamList.size() * Math.random());
		} while(((teamElement)teamList.elementAt(team)).usersPlaying >= maxSize);
		
		((teamElement)teamList.elementAt(team)).usersPlaying++;
		return team;
	}
			
	private void createTeamColors(int size) {
		int r,g,b;
		
		if(size < colorArray.length) {
			teamColorArray = colorArray;
		}
		else {
			teamColorArray = new Color[size];
			
			for(int i = 0;i < teamColorArray.length;i++) {
				if(i < colorArray.length) {
					teamColorArray[i] = colorArray[i];
				}
				else {
						do {
							r = (int)(255.0 * Math.random());
							g = (int)(255.0 * Math.random());
							b = (int)(255.0 * Math.random());
						} while(r < 100 && g < 100 && b < 100);
					
						teamColorArray[i] = new Color(r, g, b);
				}
			}
		}
	}
	
	public int nInt(double d) {
		if((d - (int)d) > 0.5)
			return (int)d+1;
		else
			return (int)d;
	}

	private void generateGame() {	
		double gameSeed = Math.random();
							
		gamePoints = usersPlaying;
			
		if(usersPlaying == 1) {
			aiPlaying = 1+(int)(19.0 * Math.random());
			
			aiList = new Vector();
			aiElement ae;
			
			for(int i = 0;i < aiPlaying;i++) {
				ae = new aiElement();
				ae.uid = generateUid(200000);
				ae.iq = Math.random();
				String q = ""+ae.iq;
				if(q.length() > 4)
					q = q.substring(0, 4);
				ae.displayName = "AiBot "+(i+1);
				ae.target = -1;
				
				aiList.addElement(ae);
			}
			
			aiSort();
		}
		else {
			aiPlaying = 0;	
			aiList = new Vector();
		}
		
		// find most voted for game
		byte votedGame = GAME_NONE;
		if(voteSingleGame > 0 || voteTeamGame > 0 || voteAiGame > 0) {
			if(voteSingleGame > voteTeamGame)
				votedGame = GAME_SINGLE;
			else
				votedGame = GAME_TEAM;
				
			if((votedGame == GAME_SINGLE && voteAiGame > voteSingleGame) || (votedGame == GAME_TEAM && voteAiGame > voteTeamGame))
				votedGame = GAME_AI;
				
			voteSingleGame = 0;
			voteTeamGame = 0;
			voteAiGame = 0;
		}
		
		if(usersPlaying + aiPlaying >= 4 && gameSeed <= 0.33 && votedGame == GAME_NONE || votedGame == GAME_TEAM) {
			println("Team");
			
			if(Math.random() > 0.50) {
				if(usersPlaying > 1)
					gameType = TYPE_TEAM_GAME;
				else
					gameType = TYPE_TEAM_AI_GAME;
			}
			else {
				if(usersPlaying > 1)
					gameType = TYPE_TEAM_SHIELDS_GAME;
				else
					gameType = TYPE_TEAM_AI_SHIELDS_GAME;
			}
			
			int teamSeed = (usersPlaying+aiPlaying)/2;
			Vector teams = new Vector();
						
			for(int i = teamSeed;i >= 2;i--) { //2
				println((usersPlaying+aiPlaying)+" "+i+" "+((usersPlaying+aiPlaying) % i));
				if(((usersPlaying+aiPlaying) % i) == 0) {
					println(i);
					teams.addElement(new Integer(i));
				}
			}
			
			if(teams.size() == 0) {
				if(usersPlaying > 1)
					gameType = TYPE_SINGLE_GAME;
				else
					gameType = TYPE_SINGLE_AI_GAME;
			}
			else {
				teamSeed = (int)((double)(teams.size()) * Math.random()); // Select random element
				println("Selected "+teamSeed);
				teamSeed = ((Integer)teams.elementAt(teamSeed)).intValue(); // Number of teams
				println("Teams "+teamSeed);
				
				createTeamColors(teamSeed);
				
				teamList = new Vector();
				for(int i = 0;i < teamSeed;i++) {
					teamList.addElement(new teamElement());
				}
			}
		}
		else if(gameSeed <= 0.70 && votedGame == GAME_NONE || usersPlaying == 1 || votedGame == GAME_SINGLE) {
			if(Math.random() > 0.50 || usersPlaying+aiPlaying == 2) {
				if(usersPlaying > 1)
					gameType = TYPE_SINGLE_GAME;
				else
					gameType = TYPE_SINGLE_AI_GAME;
			}
			else {
				if(usersPlaying > 1)
					gameType = TYPE_SINGLE_SHIELDS_GAME;
				else
					gameType = TYPE_SINGLE_AI_SHIELDS_GAME;
			}
		}
		else {
			// TODO: HVM AI game	
			gamePoints = gamePoints*2;
			
			aiPlaying = usersPlaying;
			
			aiList = new Vector();
			aiElement ae;
			
			for(int i = 0;i < aiPlaying;i++) {
				ae = new aiElement();
				ae.uid = generateUid(200000);
				ae.iq = Math.random();
				String q = ""+ae.iq;
				if(q.length() > 4)
					q = q.substring(0, 4);
				ae.displayName = "AiBot "+(i+1);
				ae.target = -1;
				
				aiList.addElement(ae);
			}
			
			aiSort();
			teamList = new Vector();
			teamElement te;
			te = new teamElement();
			te.usersPlaying = usersPlaying;
			teamList.addElement(te);
			te = new teamElement();
			te.usersPlaying = aiPlaying;
			teamList.addElement(te);
			
			if(Math.random() > 0.50)
				gameType = TYPE_HVM_AI_GAME;
			else
				gameType = TYPE_HVM_AI_SHIELDS_GAME;
		}
	}

	private boolean generatePlayers() {
		double stationSize = 4.0 + (15.0 * Math.random());
		players = new Vector();
		stats = new Vector();
		playerElement p;
		statsElement s;
		userElement ue;
		aiElement ae;
		int count = 0;
		int r, g, b;
		int randomColor = (int)(8.0 * Math.random());
		int collisionCount = 0;
		boolean tooManyCollisions = false;
		
		generateMines(stationSize);
		
		// human
		for(int i = 0;i < userList.size() && !tooManyCollisions;i++) {
			ue = (userElement)userList.elementAt(i);
			
			if(ue.playing) {
				// Stats
				s = new statsElement();
				s.displayName = ue.displayName;
				s.uid = ue.uid;
				stats.addElement(s);
				
				// Player
				p = new playerElement();
				p.displayName = ue.displayName;
				p.uid = ue.uid;
				
				//p.radius = radiusArray[stationSize];
				//p.mRadius = mRadiusArray[stationSize];
				p.radius = stationSize;
				p.mRadius = stationSize/4.0;
				
				if(gameType == TYPE_SINGLE_GAME || gameType == TYPE_SINGLE_SHIELDS_GAME || gameType == TYPE_SINGLE_AI_GAME || gameType == TYPE_SINGLE_AI_SHIELDS_GAME) {
					p.team = -1;
				}
				else if(gameType == TYPE_HVM_AI_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME) {
					p.team = 0;
					//((teamElement)teamList.elementAt(0)).usersPlaying++;
				}
				else {
					p.team = addToRandomTeam();
				}
				
				ue.team = p.team;
				
				if(scenarioPlaying == 16) {
					p.color = colorArray[randomColor];
				}
				else if(gameType == TYPE_TEAM_GAME || gameType == TYPE_TEAM_SHIELDS_GAME || gameType == TYPE_HVM_AI_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME || gameType == TYPE_TEAM_AI_GAME || gameType == TYPE_TEAM_AI_SHIELDS_GAME) {
					p.color = teamColorArray[p.team];
				}
				else {
					if(count < colorArray.length)
						p.color = colorArray[count];
					else {
						do {
							r = (int)(255.0 * Math.random());
							g = (int)(255.0 * Math.random());
							b = (int)(255.0 * Math.random());
						} while(r < 100 && g < 100 && b < 100);
					
						p.color = new Color(r, g, b);
					}
				}
				
				count++;
				int spacer = width/(usersPlaying+aiPlaying);
				
				do {
					collisionCount = 0;
					tooManyCollisions = false;	
					do {
						collisionCount++;
						p.x = (0.8 * Math.random() + 0.075 * Math.random()) * width + 0.075 * width;
						p.y = (0.8 * Math.random() + 0.075 * Math.random()) * (height - 50) + 0.075 * (height - 50);		
					
						if(collisionCount > 500)
							tooManyCollisions = true;			
					} while(checkPlayerCollisions(p, spacer) && !tooManyCollisions);
					
					spacer-=10;
				}while(tooManyCollisions && spacer > 0);
	
				if(!tooManyCollisions) {
					userList.setElementAt(ue, i);
					players.addElement(p);
				}
				else {
					return false;
				}
			}
		}
		
		// ai
		for(int i = 0;i < aiList.size() && !tooManyCollisions;i++) {
			ae = (aiElement)aiList.elementAt(i);
			
				// Stats
				//s = new statsElement();
				//s.displayName = ae.displayName;
				//s.uid = ae.uid;
				//stats.addElement(s);
				
				// Player
				p = new playerElement();
				p.displayName = ae.displayName;
				p.uid = ae.uid;
				
				p.radius = stationSize;
				p.mRadius = stationSize/4.0;
				
				if(gameType == TYPE_SINGLE_GAME || gameType == TYPE_SINGLE_SHIELDS_GAME || gameType == TYPE_SINGLE_AI_GAME || gameType == TYPE_SINGLE_AI_SHIELDS_GAME) {
					p.team = -1;
				}
				else if(gameType == TYPE_HVM_AI_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME) {
					p.team = 1;
					//((teamElement)teamList.elementAt(1)).usersPlaying++;
				}
				else {
					p.team = addToRandomTeam();
				}
				
				ae.team = p.team;
				
				if(scenarioPlaying == 16) {
					p.color = colorArray[randomColor];
				}
				else if(gameType == TYPE_TEAM_GAME || gameType == TYPE_TEAM_SHIELDS_GAME || gameType == TYPE_HVM_AI_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME || gameType == TYPE_TEAM_AI_GAME || gameType == TYPE_TEAM_AI_SHIELDS_GAME) {
					p.color = teamColorArray[p.team];
				}
				else {
					if(count < colorArray.length)
						p.color = colorArray[count];
					else {
						do {
							r = (int)(255.0 * Math.random());
							g = (int)(255.0 * Math.random());
							b = (int)(255.0 * Math.random());
						} while(r == g || g == b || r == b);
					
						p.color = new Color(r, g, b);
					}
				}
				
				count++;
				int spacer = width/(usersPlaying+aiPlaying);
				
				do {
					collisionCount = 0;
					tooManyCollisions = false;	
					do {
						collisionCount++;
						p.x = (0.8 * Math.random() + 0.075 * Math.random()) * width + 0.075 * width;
						p.y = (0.8 * Math.random() + 0.075 * Math.random()) * (height - 50) + 0.075 * (height - 50);		
					
						if(collisionCount > 500)
							tooManyCollisions = true;			
					} while(checkPlayerCollisions(p, spacer) && !tooManyCollisions);
					
					spacer-=10;
				}while(tooManyCollisions && spacer > 0);
	
				if(!tooManyCollisions) {
					aiList.setElementAt(ae, i);
					players.addElement(p);
				}
				else {
					return false;
				}
		}
		
		do {
			gbb.clear();
			gbb.bufferInt(players.size());
			for(int i = 0;i < players.size();i++) {
				p = (playerElement)players.elementAt(i);	
					
				gbb.bufferString(p.displayName);
				gbb.bufferInt(p.uid);
				gbb.bufferInt((int)p.x);
				gbb.bufferInt((int)p.y);
				gbb.bufferByte(p.team);
				gbb.bufferByte(p.status);
				gbb.bufferByte((byte)p.color.getRed());
				gbb.bufferByte((byte)p.color.getGreen());
				gbb.bufferByte((byte)p.color.getBlue());
				gbb.bufferDouble(p.radius);				
			}
		}while(!gbb.checksum());
		sendMessageToGroup(SERVER, SEND_ACTIVE, 111, gbb.data());
		
		return true;
	}
	
	private boolean checkMineCollisions(mineElement p) {
		mineElement r;
		planetElement q;
		
		for(int k = 0;k < mines.size();k++) {
			r = (mineElement)mines.elementAt(k);
					
			if((p.x - r.x) * (p.x - r.x) + (p.y - r.y) * (p.y - r.y) < (10.0 + p.radius + r.radius) * (10.0 + p.radius + r.radius)) {
				return true;
			}
		}
		
		for(int k = 0;k < planets.size();k++) {
			q = (planetElement)planets.elementAt(k);
					
			if((p.x - q.x) * (p.x - q.x) + (p.y - q.y) * (p.y - q.y) < (10.0 + p.radius + q.radius) * (10.0 + p.radius + q.radius)) {
				return true;
			}
		}
		
		return false;
	}
	
	private boolean checkPlayerCollisions(playerElement p, int spacer) {
		mineElement m;
		playerElement r;
		planetElement q;
		
		for(int k = 0;k < mines.size();k++) {
			m = (mineElement)mines.elementAt(k);
					
			if((p.x - m.x) * (p.x - m.x) + (p.y - m.y) * (p.y - m.y) < (10.0 + p.radius + m.radius) * (10.0 + p.radius + m.radius)) {
				return true;
			}
		}
		
		for(int k = 0;k < players.size();k++) {
			r = (playerElement)players.elementAt(k);
					
			if((p.x - r.x) * (p.x - r.x) + (p.y - r.y) * (p.y - r.y) < ((double)spacer + p.radius + r.radius) * ((double)spacer + p.radius + r.radius)) {
				return true;
			}
		}
		
		for(int k = 0;k < planets.size();k++) {
			q = (planetElement)planets.elementAt(k);
					
			if((p.x - q.x) * (p.x - q.x) + (p.y - q.y) * (p.y - q.y) < (10.0 + p.radius + q.radius) * (10.0 + p.radius + q.radius)) {
				return true;
			}
		}
		
		return false;
	}
	
	private void aiSort() throws ArrayIndexOutOfBoundsException {
		// Sort ai players by iq 0 -> 1
		boolean swap = false;
		aiElement aex, aey;
		
		for(int x = 0;x < aiList.size();x++) {			
			
			for(int y = x+1;y < aiList.size();y++) {
				aex = (aiElement)aiList.elementAt(x);
				aey = (aiElement)aiList.elementAt(y);
				
				swap = false;
				
				if(aey.iq < aex.iq)
					swap = true;
				
				if(swap) {
					aiList.setElementAt(aey, x);
					aiList.setElementAt(aex, y);
				}
			}
		}
	}
	
	private boolean aiTargetAlive(int i) throws ArrayIndexOutOfBoundsException {
		playerElement p;
		
		if(i == -1 || i >= players.size()) {
			return false;	
		}
		
		p = (playerElement)players.elementAt(i);
		
		if(p.status == 2)
			return false;
		else
			return true;
	}
	
	private boolean aiTargetMoved(double x, double y, int i) throws ArrayIndexOutOfBoundsException {
		playerElement p;
		
		if(i == -1 || i >= players.size()) {
			return true;	
		}
		
		p = (playerElement)players.elementAt(i);
		
		if(p.x == x && p.y == y)
			return false;
		else
			return true;
	}
	
	private void aiFindNewTarget(int x) throws ArrayIndexOutOfBoundsException {
		aiElement ae = (aiElement)aiList.elementAt(x);
		aiElement ae2;
		playerElement me = (playerElement)players.elementAt(0), p;
		
		double dist, distX, distY, closestPlayerDist = 2000000.0;
		int closestPlayer = -1;
		boolean allTargeted = true;		
		
		// find me
		for(int i = 0;i < players.size();i++) {
			p = (playerElement)players.elementAt(i);
									
			if(p.uid == ae.uid) {
				me = p;	
			}
		}		
		
		if(gameType == TYPE_SINGLE_AI_GAME || gameType == TYPE_SINGLE_AI_SHIELDS_GAME) {
			if(ae.iq < 0.20) {
				do {
					ae.target = (int)((double)(players.size()-1) * Math.random());
				} while(ae.uid == ((playerElement)players.elementAt(ae.target)).uid);		
			}
			else {
				for(int i = 0;i < players.size();i++) {
					p = (playerElement)players.elementAt(i);
									
					if(p.uid != me.uid && aiTargetAlive(i)) {
						distX = p.x - me.x;
						distY = p.y - me.y;
						dist = distX*distX+distY*distY;
										
						if(dist < closestPlayerDist) {
							closestPlayerDist = dist;
							closestPlayer = i;
						}
					}
				}
				
				ae.target = closestPlayer;			
			}
		}
		else {
			if(ae.iq < 0.20) {
				do {
					ae.target = (int)((double)(players.size()-1) * Math.random());
				} while(ae.uid == ((playerElement)players.elementAt(ae.target)).uid);		
			}
			else if(ae.iq < 0.60){
				for(int i = 0;i < players.size();i++) {
					p = (playerElement)players.elementAt(i);
									
					if(p.uid != me.uid && p.team != me.team && aiTargetAlive(i)) {
						distX = p.x - me.x;
						distY = p.y - me.y;
						dist = distX*distX+distY*distY;
										
						if(dist < closestPlayerDist) {
							closestPlayerDist = dist;
							closestPlayer = i;
						}
					}
				}
				
				ae.target = closestPlayer;			
			}
			else {
				boolean isTargeted = false;	
				double closestPlayerDist2 = 2000000.0;
				int closestPlayer2 = -1;
				
				for(int i = 0;i < players.size();i++) {
					p = (playerElement)players.elementAt(i);
									
					if(p.uid != me.uid && p.team != me.team && aiTargetAlive(i)) {
						distX = p.x - me.x;
						distY = p.y - me.y;
						dist = distX*distX+distY*distY;
						
						isTargeted = false;
						for(int k = 0;k < aiList.size();k++) {
							ae2 = (aiElement)aiList.elementAt(k);
									
							if(ae2.uid != ae.uid && ae.team == ae2.team && !ae2.dead) {
								if(ae2.target == i) {
									isTargeted = true;
								}
							}
						}
										
						if(isTargeted && dist < closestPlayerDist2) {
							closestPlayerDist2 = dist;
							closestPlayer2 = i;
						}
						else if(!isTargeted && dist < closestPlayerDist) {
							closestPlayerDist = dist;
							closestPlayer = i;
						}
					}
				}
				
				if(closestPlayer == -1)
					ae.target = closestPlayer2;
				else
					ae.target = closestPlayer;
			}			
		}
		
		ae.shots = 0;
		ae.bestAngle = 0;
		ae.bestPower = 0;
		ae.closestPath = 2000000.0;
		
		if(ae.target < 0 || ae.target >= players.size()) {
			ae.target = 0;
		}
		
		aiList.setElementAt(ae, x);
	}
	
	private void aiThink() {
		aiElement ae;
		
		for(int i = 0;i < aiList.size();i++) {	
			ae = (aiElement)aiList.elementAt(i);
			
			if(!ae.dead) {
				try {
					println("Bot: "+ae.displayName);
					if(gameType == TYPE_HVM_AI_SHIELDS_GAME || gameType == TYPE_SINGLE_AI_SHIELDS_GAME || gameType == TYPE_TEAM_AI_SHIELDS_GAME) {
						println("Check Shield");
						aiCheckShield(i);
					}
					
					if(!aiTargetAlive(ae.target) || aiTargetMoved(ae.tX, ae.tY, ae.target)) {
						println("New Target");
						aiFindNewTarget(i);	
					}
									
					if(!aiHyper(i)) {
						println("Calc Shot");
						aiCalculateShot(i);	
					}
					
					ae.missileCollide = false;
				}
				catch(ArrayIndexOutOfBoundsException aioobe) {
					System.out.println("Error Thinking: "+aioobe.toString());
					println("Error Thinking");
				}
				
				aiList.setElementAt(ae, i);		
			}
		}			
	}
	
	private boolean aiHyper(int i) throws ArrayIndexOutOfBoundsException {
		aiElement ae = (aiElement)aiList.elementAt(i);
		
		if(scenarioPlaying != 18 && !ae.missileCollide) {
			if(ae.iq < 0.15) {
				if(Math.random() < 0.25) {
					aiHyper++;
					ae.hyper = true;
					aiList.setElementAt(ae, i);
					return true;
				}
			}
			else if(ae.iq < 0.30) {
				if(Math.random() < 0.13) {
					aiHyper++;
					ae.hyper = true;
					aiList.setElementAt(ae, i);
					return true;
				}				
			}
			else {
				if(ae.shots > 3 && ae.closestPath > 10000.0 || Math.random() > 0.90 || ae.hyper) {
					aiHyper++;
					ae.hyper = true;
					aiList.setElementAt(ae, i);
					return true;					
				}
			}
		}
		
		return false;	
	}
	
	private void aiCheckShield(int i) throws ArrayIndexOutOfBoundsException {
		playerElement p;
		aiElement ae = (aiElement)aiList.elementAt(i);
		double cp;
		
		println("...have "+ae.shields);
		
		if(ae.iq < 0.33) {
			if(ae.shields > 0) {
				println("...shield");
				ae.shields--;
				sendEmptyMessageToGroup(ae.uid, SEND_ACTIVE, 138);
			}
		}
		else if(ae.iq < 0.66) {
			if(ae.shields > 0 && Math.random() > 0.50) {
				println("...shield");
				ae.shields--;
				sendEmptyMessageToGroup(ae.uid, SEND_ACTIVE, 138);
			}
		}
		else {
			if(ae.shields > 0) {
				for(int x = 0;x < players.size();x++) {
					p = (playerElement)players.elementAt(x);
					
					cp = aiSimulateShot(p.angle, p.power, p.uid, 5, 2000, ae.uid, 200000);
						
					if(cp < p.radius * p.radius) {
						println("...shield");
						ae.shields--;
						sendEmptyMessageToGroup(ae.uid, SEND_ACTIVE, 138);
							
						if(ae.iq > 0.85 && scenarioPlaying != 18) {
							aiHyper++;
							ae.hyper = true;
						}
					}
				}
			}
		}
		
		aiList.setElementAt(ae, i);
	}
	
	private double aiSimulateShot(int angle, int power, int youUid, int velocityFactor, int loop, int myUid, double currentDist) throws ArrayIndexOutOfBoundsException {
        planetElement pl;
		playerElement p;
		playerElement me = null, you = null;
		
		for(int i = 0;i < players.size();i++) {
			p = (playerElement)players.elementAt(i);
			
			if(p.uid == myUid)
				me = p;
				
			if(p.uid == youUid)
				you = p;
		}
		
		if(me == null || you == null) {
			return currentDist;
		}
		
		double xVelocity = ((double)power / 27.0 + 0.2) * 0.8 * Math.sin((double)angle / 180.0 * 3.141592653589793);
		double yVelocity = ((double)power / 27.0 + 0.2) * 0.8 * Math.cos((double)angle / 180.0 * 3.141592653589793);
		double X = me.x + (me.radius + 1.0) * Math.sin((double)angle / 180.0 * 3.141592653589793);
		double Y = me.y + (me.radius + 1.0) * Math.cos((double)angle / 180.0 * 3.141592653589793);
		double factor = velocityFactor * 0.25;
		double thisDist;
		boolean run = true;
		int cycles = 0;
		
		while(run) {
			for(int k = 0;k < planets.size();k++) {
				pl = (planetElement) planets.elementAt(k);
					
				double d6 = 1.0;
				double d1 = pl.x - X;
				double d2 = pl.y - Y;
				double d3 = Math.atan(d2 / d1);
				if (d1 < 0.0)
					d6 = -1.0;
				double d7 = d1 * d1 + d2 * d2;
                               
				if (d7 >= pl.radius * pl.radius) {
					double d8 = d6 * 0.2 * pl.M / d7;
					xVelocity += Math.cos(d3) * d8 * 0.25; //0.25
					yVelocity += Math.sin(d3) * d8 * 0.25; //0.25
				}
				else if (pl.impact == 1) {
					run = false;
				}
				else if (pl.impact == 2) {
					run = false;
				}
				else if (pl.impact <= 0) {
					double d5;
					if (d6 < 0.0)
						d5 = d3 + 3.141592653589793;
					else
						d5 = d3;
					int j = -pl.impact;
					X = ((planetElement)planets.elementAt(j)).x + Math.cos(d5) * (((planetElement)planets.elementAt(j)).radius + 0.5);
					Y = ((planetElement)planets.elementAt(j)).y + Math.sin(d5) * (((planetElement)planets.elementAt(j)).radius + 0.5);
				}
			}
			
			X = X + xVelocity * 0.25; //0.25
			Y = Y + yVelocity * 0.25; //0.25
			
			for(int k = 0;k < players.size();k++) {
				p = (playerElement)players.elementAt(k);
					
				if((X - p.x) * (X - p.x) + (Y - p.y) * (Y - p.y) < (p.radius) * (p.radius)) {
					run = false;
				}
			}
		
			thisDist = (X - you.x) * (X - you.x) + (Y - you.y) * (Y - you.y);           		
			if (thisDist < currentDist)
				currentDist = thisDist;
			
			cycles++;
			
			if(cycles >= loop) {
				run = false;	
			}
		}
            		
		return currentDist;
	}
	
	private void aiCalculateShot(int x) throws ArrayIndexOutOfBoundsException {
		int i3, j3;
		double d7=1.0, d5;
		double pathClosest=20000000;
		double targetX=0.0, targetY=0.0;
		int k1, k2;
		
		aiElement ae = (aiElement)aiList.elementAt(x);
		playerElement you = (playerElement)players.elementAt(ae.target);
		playerElement me = null, p;
		
		println("Target: "+you.displayName);
		
		for(int i = 0;i < players.size();i++) {
			p = (playerElement)players.elementAt(i);
			
			if(p.uid == ae.uid)
				me = p;
		}
		
		int b1, s, b3;
		
		//b1 = (int)(20.0+(ae.iq*3.0*((double)ae.shots+1.0)));
		//s = (int)(400.0+(ae.iq*300.0*((double)ae.shots+1.0)));
		b3 = 5;
		
		if(ae.shots == 0) {
			b1 = (int)(10.0*ae.iq);
			s = (int)(1500.0*ae.iq);
		}
		else if(ae.shots == 1) {
			b1 = (int)(20.0*ae.iq);
			s = (int)(2000.0*ae.iq);
		}
		else if(ae.shots < 3) {
			b1 = (int)(30.0*ae.iq);
			s = (int)(2500.0*ae.iq);
		}
		else if(ae.shots < 6) {
			b1 = (int)(40.0*ae.iq);
			s = (int)(3000.0*ae.iq);
		}
		else {
			b1 = 50;
			s = 3500;
		}
            					
		if(ae.shots == 0) {
            ae.tX = you.x;
            ae.tY = you.y;
            
            d7 = 1.0;
            targetX = you.x - me.x;
			targetY = you.y - me.y;
            if (targetX < 0.0)
            	d7 = -1.0;
			me.angle = -((int)(180.0 * Math.atan(targetY / targetX) / 3.141592653589793)) + (int)(d7 * 90.0);
			me.power = 700;
			                    			
			for (; me.angle > 360; me.angle -= 360);
			for (; me.angle < 0; me.angle += 360);
				
			me.angle = me.angle + (int)(20.0 * Math.random() - 20.0 * Math.random());
	                    			
			for (; me.angle > 360; me.angle -= 360);
			for (; me.angle < 0; me.angle += 360);
				
			ae.bestAngle = me.angle;
			ae.bestPower = me.power;
			i3 = me.angle;
			j3 = me.power;
		}
		else {
			i3 = ae.bestAngle;
			j3 = ae.bestPower;
			pathClosest = ae.closestPath;
		}
            				
		double d13 = aiSimulateShot(i3, j3, you.uid, b3, s, me.uid, 2000000);
		k1 = i3;
		k2 = j3;
          				
		for (int i5 = 1; i5 < b1; i5++) {
			int i4;
                    					
			i3 = k1 + (int)(15.0 * Math.random() - 15.0 * Math.random());
                   
			for (; i3 > 360; i3 -= 360);
			for (; i3 < 0; i3 += 360);
                    					
			j3 = k2 + (int)(50.0 * Math.random() - 50.0 * Math.random());
                    
			if (j3 < 200)
				j3 = 200;
			if (j3 > 800)
				j3 = 800;
                    					
			d13 = aiSimulateShot(i3, j3, you.uid, b3, s, me.uid, pathClosest);
                    
			if (d13 < pathClosest) {
				k2 = j3;
				k1 = i3;
				pathClosest = d13;
			}
			else {
				j3 = k2;
				i3 = k1;
			}

			//if(ae.iq > 3) {
				if(pathClosest < me.radius * me.radius) {
					if(Math.random() < 0.6) {
						i3 += (int)(Math.random() - Math.random());
                      				
						for (; i3 > 360; i3 -= 360);
						for (; i3 < 0; i3 += 360);
                      					
						d13 = aiSimulateShot(i3, j3, you.uid, b3, s, me.uid, pathClosest);
						k2 = j3;
						k1 = i3;
						pathClosest = d13;
                        			
						// end loop
						i5 = b1;
					}
				}
				else {
					if(ae.iq == 5) {
						i3 = k1 + (int)(3.0 * Math.random());
                   
						for (; i3 > 360; i3 -= 360);
						for (; i3 < 0; i3 += 360);
                      					
						d13 = aiSimulateShot(i3, j3, you.uid, b3, s, me.uid, pathClosest);
                    
						if (d13 < pathClosest) {
							k2 = j3;
							k1 = i3;
							pathClosest = d13;
						}
						else {
							j3 = k2;
							i3 = k1;
						}
                       				
						i3 = k1 - (int)(3.0 * Math.random());
	                   
						for (; i3 > 360; i3 -= 360);
						for (; i3 < 0; i3 += 360);
                      					
						d13 = aiSimulateShot(i3, j3, you.uid, b3, s, me.uid, pathClosest);
                    
						if (d13 < pathClosest) {
							k2 = j3;
 							k1 = i3;
							pathClosest = d13;
						}
						else {
							j3 = k2;
							i3 = k1;
						}
 					}
				}
                  			
				j3 = k2 + (int)(90.0 * Math.random() - 90.0 * Math.random());
                    
				if (j3 < 200)
					j3 = 200;
				if (j3 > 800)
					j3 = 800;
                    					
				d13 = aiSimulateShot(i3, j3, you.uid, b3, s, me.uid, pathClosest);
                    
				if (d13 < pathClosest) {
					k2 = j3;
					k1 = i3;
 					pathClosest = d13;
				}
				else {
					j3 = k2;
					i3 = k1;
				}
 				
				d5 = -((int)(180.0 * Math.atan(targetY / targetX) / 3.141592653589793)) + (int)(d7 * 90.0); //+180
				
				if (totalMass < 20.0)
					d7 = (int)((Math.random() * Math.random() + Math.random() * Math.random() + Math.random() * Math.random()) * 10.0);
				else if (totalMass < 100.0)
					d7 = (int)((Math.random() * Math.random() + Math.random() * Math.random() + Math.random()) * 60.0);
				else if (totalMass < 300.0)
					d7 = (int)((Math.random() * Math.random() + Math.random() + Math.random()) * 60.0);
				else
					d7 = (int)(Math.random() * 220.0);
            					
				for (i3 = (int)Math.round(d5 + (double)d7 * Math.random() - (double)d7 * Math.random()); i3 > 360; i3 -= 360) /* null body */ ;
				for (; i3 < 0; i3 += 360);
            					
				if (totalMass < 100.0)
					j3 = 200 + (int)(Math.random() * 600.0);
				else if (totalMass < 300.0)
 					j3 = 200 + (int)((Math.random() + Math.random()) * 400.0);
				else
					j3 = 200 + (int)((3.0 * Math.random() * Math.random() + Math.random()) * 200.0);
                     			
				if (j3 < 200)
					j3 = 200;
				if (j3 > 800)
					j3 = 800;
                    					
				d13 = aiSimulateShot(i3, j3, you.uid, b3, s, me.uid, pathClosest);
                    
				if (d13 < pathClosest) {
					k2 = j3;
					k1 = i3;
					pathClosest = d13;
				}
				else {
					j3 = k2;
					i3 = k1;
				}
 			//}
			
			ae.fired = true;
			me.angle = k1;
			me.power = k2;
			ae.bestAngle = k1;
			ae.bestPower = k2;
   			ae.closestPath = pathClosest;
 			ae.shots++;
 			
 			aiList.setElementAt(ae, x);
		}
		
		aiFired++;
		println(""+pathClosest);
			
		// send
		gbb.clear();
		gbb.bufferInt(me.angle);
		gbb.bufferInt(me.power);
		sendMessageToGroup(ae.uid, SEND_ACTIVE, 113, gbb.data());
	}
	
	public String convertToFriendly(String in) {
		String t = "";
		int s = 0;
		int pos;
		
		while((pos = in.indexOf(" ", s)) != -1) {
			t = t + in.substring(s, pos) + "%20";
			s = pos+1;
		}
		
		t = t + in.substring(s);
		in = t;
		t = "";
		s = 0;
		
		while((pos = in.indexOf("\n", s)) != -1) {
			t = t + in.substring(s, pos) + "%99";
			s = pos+1;
		}
		
		return (t + in.substring(s));
	}
	
	public String convertFromFriendly(String in) {
		String t = "";
		int s = 0;
		int pos;
		
		while((pos = in.indexOf("%20", s)) != -1) {
			t = t + in.substring(s, pos) + " ";
			s = pos+3;
		}
		
		t = t + in.substring(s);
		in = t;
		t = "";
		s = 0;
		
		while((pos = in.indexOf("%99", s)) != -1) {
			t = t + in.substring(s, pos) + "\n";
			s = pos+3;
		}
		
		return (t + in.substring(s));
	}
		
	private void readSettings(String file) {
		StringTokenizer st;
		
		try {
			BufferedReader in = new BufferedReader(new FileReader(systemDir+systemSeparator+file));
			String data;
			gwoSettings gs = new gwoSettings();
			
			while((data = in.readLine()) != null) {
				st = new StringTokenizer(data, " ");
				data = gs.getSettingsCommand(st);
				
				if(data.equals("check_ip:")) {
					checkIp = gs.getSettingsValue_B(st);
				}
				else if(data.equals("port:")) {
					port = gs.getSettingsValue_I(st);
				}
				else if(data.equals("have_dyndns:")) {
					haveDyndns = gs.getSettingsValue_B(st);
				}
				else if(data.equals("dyndns_username:")) {
					dyndnsUsername = gs.getSettingsValue_S(st);
				}
				else if(data.equals("dyndns_password:")) {
					dyndnsPassword = gs.getSettingsValue_S(st);
				}
				else if(data.equals("dyndns_url:")) {
					dyndnsUrl = gs.getSettingsValue_S(st);
				}
				else if(data.equals("admin_password:")) {
					adminPassword = gs.getSettingsValue_S(st);
				}
				else if(data.equals("debug:")) {
					debug = gs.getSettingsValue_B(st);
				}
				else if(data.equals("save:")) {
					saveNum = gs.getSettingsValue_I(st);
				}
				else if(data.equals("load_league:")) {
					loadLeague = gs.getSettingsValue_B(st);
				}
				else if(data.equals("ban:")) {
					String ban = gs.getSettingsValue_S(st);
					banElement b = new banElement();
					b.uid = new Integer(ban.substring(0, ban.indexOf("@"))).intValue();
					b.ip = ban.substring(ban.indexOf("@")+1, ban.length());
					System.out.println(b.uid+" "+b.ip);
					banList.addElement(b);
				}
				else if(data.equals("title:")) {
					title = convertFromFriendly(gs.getSettingsValue_S(st));
				}
				else if(data.equals("description:")) {
					description = convertFromFriendly(gs.getSettingsValue_S(st));
				}
				else if(data.equals("max_players:")) {
					maxPlayers = gs.getSettingsValue_I(st);
				}
			}
			
			in.close();
		}
		catch(FileNotFoundException fnfe) {
			println(fnfe.toString() + " (19)");
		}
		catch(IOException ioe) {
			println(ioe.toString() + " (20)");
		}
	}
	
	public void writeSettings() {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(systemDir+systemSeparator+"gwoServerSettings@port"+port+".ini"));
			out.write("check_ip: "+checkIp+"\n");
			out.write("port: "+port+"\n");
			out.write("have_dyndns: "+haveDyndns+"\n");
			out.write("dyndns_username: "+dyndnsUsername+"\n");
			out.write("dyndns_password: "+dyndnsPassword+"\n");
			out.write("dyndns_url: "+dyndnsUrl+"\n");
			out.write("admin_password: "+adminPassword+"\n");
			out.write("load_league: "+loadLeague+"\n");
			out.write("debug: "+debug+"\n");
			out.write("save: "+saveNum+"\n");
			out.write("title: "+convertToFriendly(title)+"\n");
			out.write("description: "+convertToFriendly(description)+"\n");
			out.write("max_players: "+maxPlayers+"\n");
			
			banElement b;
			for(int i = 0;i < banList.size();i++) {
				b = (banElement)banList.elementAt(i);
				out.write("ban: "+b.uid+"@"+b.ip+"\n");
			}
			
			out.close();
		}
		catch(IOException ioe) {
			println(ioe.toString() + " (21)");
		}
	}
	
	private void readPlayerLeague() {
		try {
			BufferedReader in = new BufferedReader(new FileReader(systemDir+systemSeparator+"gwoLeague@port"+port+".ini"));
			String data;
			userElement ue;
			StringTokenizer st;
			
			userList = new Vector();
		
			while((data = in.readLine()) != null) {
				st = new StringTokenizer(data, " ");
				
				if(st.countTokens() == 9) {
					ue = new userElement();
					ue.displayName = convertFromFriendly(st.nextToken());
					ue.uid = new Integer(st.nextToken()).intValue();
					ue.played = new Integer(st.nextToken()).intValue();
					ue.won = new Integer(st.nextToken()).intValue();
					ue.drawn = new Integer(st.nextToken()).intValue();
					ue.lost = new Integer(st.nextToken()).intValue();
					ue.killsFor = new Integer(st.nextToken()).intValue();
					ue.killsAgainst = new Integer(st.nextToken()).intValue();
					ue.points = new Integer(st.nextToken()).intValue();
					userList.addElement(ue);
				}
			}
		}
		catch(FileNotFoundException fnfe) {
			println(fnfe.toString() + " (22)");
		}
		catch(IOException ioe) {
			println(ioe.toString() + " (23)");
		}		
	}
	
	private void writePlayerLeague() {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(systemDir+systemSeparator+"gwoLeague@port"+port+".ini"));
			userElement ue;
			
			for(int i = 0;i < userList.size();i++) {
				ue = (userElement)userList.elementAt(i);
				
				if(ue.played > 0) {
					out.write(convertToFriendly(ue.displayName)+ " "+ue.uid+" "+ue.played+" "+ue.won+" "+ue.drawn+
									" "+ue.lost+" "+ue.killsFor+" "+ue.killsAgainst+" "+ue.points+"\n");
				}
			}
			out.close();
		}
		catch(FileNotFoundException fnfe) {
			println(fnfe.toString() + " (24)");
		}
		catch(IOException ioe) {
			println(ioe.toString() + " (25)");
		}
	}
}
