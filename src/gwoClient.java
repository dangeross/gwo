import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.sound.sampled.*;

public class gwoClient extends JFrame implements ActionListener, AdjustmentListener {
	// VARIABLES
	
	public int version = 32;
	public int serverVersion = 0;
	public int tempUid = 0;
	public boolean gwoNetServer = false;
	
	public String systemUser, systemDir, systemSeparator;
	public StringTokenizer st;
	
	private Socket s;
	private gwoDataInputStream ios;
	private gwoDataOutputStream dos;
	private gwoClientServerListener gsl;
	private int lastValidMsgId = 0;
	
	//private Timer renderPanel;
	public Toolkit tools;
	public Timer clock, hClock, gClock, cClock;
	
	public Image back1, back2;
	public Image gwoNet;
	public Image pageUp, pageDown;
	public Image youWin, youDraw, youLose, gameOver;
	public Image icon, icon2;

	private gwoByteBuffer gbb = new gwoByteBuffer();
	public int gameScenario = 0;
	public int gameType = TYPE_SINGLE_GAME;
	public byte gameState = STATE_PRE_LOGIN; 
	
	private JPanel contentPanel, masterPanel, aimAndFirePanel, textPanel, newGamePanel, watchGamePanel;
	private gwoRenderPanel3 renderPanel;
	public JScrollBar angle, power;
	private JCheckBox beep, autoJoinBox;
	private JButton fire, hyper, shield, play, watch, login, server;
	private JTextField text;
	
	public Vector starfield = new Vector();
	public Vector planets = new Vector();
	public Vector players = new Vector();
	public Vector mines = new Vector();
	
	public Vector servers = new Vector();
	public Vector league = new Vector();
	public int leaguePage = 0;
	public int leagueNumOfPages = 0;
	
	public boolean isWatching = false;
	private boolean isPlaying = false;
	private boolean joinPressed = true;
	public boolean isDead = false;
	private boolean isFocused = true;
	public boolean isFirstGo = false;
	private boolean isController = false;
	public boolean showSeconds = false;
	private boolean shieldUsed = false;
	private boolean killedThisShot = false;
	public boolean shotDone = false;
	
	public boolean drawBackground = true;	
	public boolean drawPlayers = true;	
	public boolean drawMines = true;	
	
	private boolean repaintReq = false;
	private boolean loginReq = false;
	private boolean fileTransferReq = false;
	private boolean iconReq = false;
	
	private boolean joinPanelReq = false;
	private boolean watchPanelReq = false;
	private boolean aimPanelReq = false;
	private boolean clearPanelReq = false;
	private String title = "Galaxy Wars Online v2.0."+version;
	private boolean titleReq = false;
	
	public String infoMessage = "";
	public String autoMessage = "";
	
	public byte result = 0;
	public Vector resultAwards = new Vector();
	
	public int secondsLeft = 0;
	public int width = 900, height = 700;
	private double middleX = width / 2, middleY = height / 2, pRadius = width + (height / 3.0);
	public int shieldsAvailable = 0;
	public int pathLength = 0;
	public int step = 0;
	public int bulletLife = 2000;
	//private int missileCollideCount = 0;
	//private int missileCollidePlayer = 0;
	private int frameSkip = 0;
	
	public int[] xPath = new int[bulletLife];
	public int[] yPath = new int[bulletLife];
	
	private int mydataShots;
	private int mydataBulletLife;
	private int mydataKills;
	private int mydataHits;
	private boolean mydataHitOwn;
	private boolean mydataHitTeam;
	private int mydataShield;
	private boolean mydataMineKill;
	private boolean mydataWormKill;
	
	public String msgText1="", msgText2="", msgText3="", msgText4="", msgText5="";
	public Color msgColor1, msgColor2, msgColor3, msgColor4, msgColor5;
	
	private gwoClientLogin gcl;
	private Vector dataToPost = new Vector();
	
	// File Transfer
	public FileOutputStream ftFos;
	public gwoFileTransfer gft;
	public byte[] ftData;
	public long ftSize = 0;
	
	// Settings saved and loaded from file
	public boolean useProxy = false;
	public String proxyServer = "";
	public int proxyPort = 0;
	public String displayName = "";
	public String password = "";
	public int uid = 0;
	public Vector serverList = new Vector();
	public String selectedServer = "";
	public boolean beepSelected = true;
	public boolean autoJoin = false;
	public boolean sound = true;
	public int soundVolume = 75;
	public boolean backgroundAntiAlias = true;
	public boolean foregroundAntiAlias = true;
	public boolean renderForQuality = false;
	public int frameSkipTo = 1;
	public boolean reroute = false;
	public String remoteServer="dangeross-reroute.homeip.net";
	public int remotePort=443;
	public int fps = 120;
	public boolean uberUser = false;
	public boolean largeFont = true;
	public String msgText="<Type Messages Here>";
	public int savedAngle=1;
	public int savedPower=200;
	
	// CONSTANTS
	
	public static int SERVER = 0;
	
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

	public static final byte STATE_PRE_LOGIN      = 0;
	public static final byte STATE_SERVER_LIST    = 1;
	public static final byte STATE_POST_LOGIN     = 2;
	public static final byte STATE_NO_PLAYER      = 3;
	public static final byte STATE_JOIN_GAME      = 4;
	public static final byte STATE_GAME_AIM       = 5;
	public static final byte STATE_GAME_FIRE      = 6;
	public static final byte STATE_GAME_HYPER     = 7;
	public static final byte STATE_GAME_WAIT      = 8;
	public static final byte STATE_GAME_END       = 9;
	
	public static final byte PAGE_DOWN = 0;
	public static final byte PAGE_UP   = 1;
	
	public static final byte RESULT_WIN       = 0;
	public static final byte RESULT_DRAW      = 1;
	public static final byte RESULT_LOSE      = 2;
	public static final byte RESULT_GAME_OVER = 3;

	public static void main(String args[]) {
		new gwoClient();
	}
	
	public gwoClient() {
		super("Galaxy Wars Online");
		
		systemUser = System.getProperty("user.name");
		systemDir = System.getProperty("user.home");
		systemSeparator = System.getProperty("file.separator");
		
		readUserSettings();
		
		tempUid = uid;
		tools = getToolkit();
		
		createGUI();
		
		back1 = tools.createImage(this.getClass().getResource("gwo_01.jpg"));
		back2 = tools.createImage(this.getClass().getResource("gwo_02.jpg"));
		gwoNet = tools.createImage(this.getClass().getResource("gwoNet.jpg"));
		pageUp = tools.createImage(this.getClass().getResource("pageUpValid.gif"));
		pageDown = tools.createImage(this.getClass().getResource("pageDownValid.gif"));
		youWin = tools.createImage(this.getClass().getResource("youWin.gif"));
		youDraw = tools.createImage(this.getClass().getResource("youDraw.gif"));
		youLose = tools.createImage(this.getClass().getResource("youLose.gif"));
		gameOver = tools.createImage(this.getClass().getResource("gameOver.gif"));
		icon = tools.createImage(this.getClass().getResource("icon.gif"));
		icon2 = tools.createImage(this.getClass().getResource("icon2.gif"));
		
		addDebugMessage("");
		while(back1.getWidth(this) <= 0 && back2.getWidth(this) <= 0);
	
		setResizable(false);
		
		setIconImage(icon);
		
		setVisible(true);
		
		addWindowListener(
			new WindowAdapter()
			{
				public void windowDeactivated(WindowEvent e) {
					isFocused = false;
				}
	
				public void windowActivated(WindowEvent e) {
					isFocused = true;
					text.requestFocus();
					contentPanel.revalidate();
				}
				
				public void windowClosing(WindowEvent evt)
				{
					renderPanel.close();
					try {
						writeUserSettings();
					}
					catch(NoClassDefFoundError e) {
						addDebugMessage("Could Not Write User Settings.");
					}
					System.exit(0);
				}
			}
		);
		
		clock = new Timer(fps, calculateMisslePaths);
		hClock = new Timer(125, calculateHyperspace);
		gClock = new Timer(500, this);
		cClock = new Timer(1000, counter);
		gClock.start();
		
		renderPanel.repaint();
		
		gcl = new gwoClientLogin(this);

		//gameState = STATE_PRE_LOGIN;
	}
	
	private void createGUI() {
		msgText1 = "";
		msgText2 = "";
		msgText3 = "";
		msgText4 = "";
		msgText5 = "";
		msgColor1 = Color.white;
		msgColor2 = Color.white;
		msgColor3 = Color.white;
		msgColor4 = Color.white;
		msgColor5 = Color.white;
	
		contentPanel = new JPanel(new BorderLayout());
		masterPanel = new JPanel(new BorderLayout());
		JPanel intermediatePanel = new JPanel(new BorderLayout());
			
		aimAndFirePanel = new JPanel(new GridLayout(1, 4));
		//angle = new JScrollBar(JScrollBar.HORIZONTAL, 1, 90, 1, 450);
		angle = new JScrollBar(JScrollBar.HORIZONTAL, savedAngle, 90, 1, 450);
		angle.setBlockIncrement(10);
		angle.addAdjustmentListener(this);
		//power = new JScrollBar(JScrollBar.HORIZONTAL, 200, 199, 200, 1000);
		power = new JScrollBar(JScrollBar.HORIZONTAL, savedPower, 200, 200, 1000);
		power.setBlockIncrement(20);
		power.addAdjustmentListener(this);
		fire = new JButton("Fire!");
		fire.addActionListener(guiEvent);
		fire.setActionCommand("$fire");
		hyper = new JButton("Hyperspace");
		hyper.addActionListener(guiEvent);
		hyper.setActionCommand("$hyper");
		shield = new JButton("Shield");
		shield.addActionListener(guiEvent);
		shield.setActionCommand("$shield");
		shield.setEnabled(false);
		text = new JTextField(msgText);
		text.selectAll();
		text.addActionListener(guiEvent);
		aimAndFirePanel.add(angle);
		aimAndFirePanel.add(fire);
		aimAndFirePanel.add(hyper);
		aimAndFirePanel.add(shield);
		aimAndFirePanel.add(power);
			
		textPanel = new JPanel(new BorderLayout());
		textPanel.add("South", text);
		
		intermediatePanel.add("Center", masterPanel);
		intermediatePanel.add("South", textPanel);
			
		newGamePanel = new JPanel(new BorderLayout());
		beep = new JCheckBox("Beep?", beepSelected);
		autoJoinBox = new JCheckBox("Auto Join?", autoJoin);
		play = new JButton("Join Game");
		play.addActionListener(guiEvent);
		play.setActionCommand("$play");
		login = new JButton("Settings");
		login.addActionListener(guiEvent);
		login.setActionCommand("$login");
		server = new JButton("Server");
		server.addActionListener(guiEvent);
		server.setActionCommand("$server");
		
		JPanel ngp2 = new JPanel(new BorderLayout());
		ngp2.add("West", beep);
		ngp2.add("Center", autoJoinBox);
		
		JPanel ngp3 = new JPanel(new BorderLayout());
		ngp3.add("West", login);
		ngp3.add("Center", server);
		
		newGamePanel.add("West", ngp2);
		newGamePanel.add("Center", play);
		newGamePanel.add("East", ngp3);	
			
		watchGamePanel = new JPanel(new BorderLayout());
		watch = new JButton("Watch Current Game");
		watch.addActionListener(guiEvent);
		watch.setActionCommand("$watch");
		
		watchGamePanel.add("South", watch);
		
		renderPanel = new gwoRenderPanel3(this);
		
		// Are Scrollbars required 
		Dimension d = tools.getScreenSize();
		int nWidth = width, nHeight = height;
		boolean scrollNeeded = false;
		
		if(d.width < width) {
			nWidth = d.width;
			scrollNeeded = true;
		}
		
		if(d.height < height) {
			nHeight = d.height;
			scrollNeeded = true;
		}
		
		if(scrollNeeded) {
			JScrollPane scrollPane = new JScrollPane(renderPanel,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);	
		
			contentPanel.add("Center", scrollPane);
		}
		else {
			contentPanel.add("Center", renderPanel);
		}
		
		contentPanel.add("South", intermediatePanel);
		setContentPane(contentPanel);
		
		setSize(nWidth, nHeight);
	}
	
	public void handleMouseClickEvent(byte type) {
		if(type == PAGE_DOWN && leaguePage > 1) {
			if(gameState == STATE_SERVER_LIST) {
				leaguePage--;
				repaintReq = true;
			}
			else {		
				gbb.clear();
				gbb.bufferInt(leaguePage-1);
				sendMessage(uid, SERVER, 270, gbb.data());
			}
		}
		else if(type == PAGE_UP && leaguePage < leagueNumOfPages) {
			if(gameState == STATE_SERVER_LIST) {
				leaguePage++;
				repaintReq = true;
			}
			else {		
				gbb.clear();
				gbb.bufferInt(leaguePage+1);
				sendMessage(uid, SERVER, 270, gbb.data());		
			}
		}
	}
	
	public void handleMouseClickEvent(int x, int y) {
		if(gameState == STATE_SERVER_LIST) {
			int highlight = y-200;
			highlight = (int)((double)highlight/15.0);
		
			if(highlight >= 0 && highlight <= 25) {
				int numOfElements = 0;
				serverElement se;
				
				if(leaguePage*25 < servers.size()) {
					numOfElements = 25;
				}
				else {
					numOfElements = servers.size()-((leaguePage-1)*25);
				}
		
				for(int i = 0;i < numOfElements;i++) {
					if(i == highlight) {
						se = (serverElement)servers.elementAt(((leaguePage-1)*25)+i);
					
						selectedServer = se.address;
						if(!connectToServer())
							gcl = new gwoClientLogin(this);
					}
				}
			}
		}
		else {
			if(gameScenario != 16) {
				playerElement p;
				String txt = text.getText();
				// Check player click
				
				for(int i = 0;i < players.size();i++) {
					p = (playerElement)players.elementAt(i);
					
					if(x >= p.x-p.radius && x <= p.x+p.radius) {
						if(y >= p.y-p.radius && y <= p.y+p.radius) {
							if(txt.startsWith("/priv")) {
								txt = txt.substring(txt.indexOf(" ", 6)+1, txt.length());	
							}
							
							text.setText("/priv "+p.uid+" "+txt);	
						}	
					}
				}
			}
		}
	}
	
	private void addUserMessage(String txt, Color col) {
		msgText5 = msgText4;
		msgText4 = msgText3;
		msgText3 = msgText2;
		msgText2 = msgText1;
		msgText1 = txt;
		
		msgColor5 = msgColor4;
		msgColor4 = msgColor3;
		msgColor3 = msgColor2;
		msgColor2 = msgColor1;
		msgColor1 = col;
	}
	
	public void addDebugMessage(String txt) {
		System.out.println(txt);
	}
	
	public void fileTransferStart() {
		sendEmptyMessage(uid, SERVER, 261);
	}
	
	public void fileTransferCancelled(boolean quit) {
		if(quit) {
			try {
				writeUserSettings();
			}
			catch(NoClassDefFoundError e) {
				addDebugMessage("Could Not Write User Settings.");
			}
			System.exit(0);
		}
		else {
			sendEmptyMessage(uid, SERVER, 263);
		}
	}
	
	public void close() {
		try {
			writeUserSettings();
		}
		catch(NoClassDefFoundError e) {
			addDebugMessage("Could Not Write User Settings.");
		}
		System.exit(0);		
	}
	
	public void adjustmentValueChanged(AdjustmentEvent e) {
		renderPanel.repaint();
	}
	
	ActionListener guiEvent = new ActionListener() {
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if(cmd.equalsIgnoreCase("$fire")) {
			shotDone = true;
			mydataShots++;
					
			masterPanel.removeAll();
			contentPanel.revalidate();
			
			gbb.clear();
			gbb.bufferInt(angle.getValue());
			gbb.bufferInt(power.getValue());
			sendMessage(uid, SERVER, 203, gbb.data());
			
			renderPanel.repaint();
		}
		else if(cmd.equalsIgnoreCase("$hyper")) { 
			pathLength = 0;
			shotDone = true;
					
			masterPanel.removeAll();
			contentPanel.revalidate();
			
			sendEmptyMessage(uid, SERVER, 204);
			renderPanel.repaint();
		}
		else if(cmd.equalsIgnoreCase("$shield")) { 
			shieldUsed = true;
			shieldsAvailable--;
			
			sendEmptyMessage(uid, SERVER, 224);
			renderPanel.repaint();
		}
		else if(cmd.equalsIgnoreCase("$play")) { 
			joinPressed = true;
			sendEmptyMessage(uid, SERVER, 202);
		}
		else if(cmd.equalsIgnoreCase("$watch")) { 
			//watch.setEnabled(false);
			masterPanel.removeAll();
			contentPanel.revalidate();

			sendEmptyMessage(uid, SERVER, 211);
		}
		else if(cmd.equalsIgnoreCase("$login")) { 			
			if(gameState > STATE_SERVER_LIST) {
				infoMessage = "Logged Out";
			
				if(gsl != null)
					gsl.quit();
					
				try {
					s.close();
				}
				catch(IOException ioe) {}
			}
			else {
				infoMessage = "";
			}

			gameState = STATE_POST_LOGIN;	
			loginReq = true;
		}
		else if(cmd.equalsIgnoreCase("$server")) { 
			if(gameState > STATE_SERVER_LIST) {
				if(gsl != null)
					gsl.quit();
					
				try {
					s.close();
				}
				catch(IOException ioe) {}
			}

			serverSelection();
			
			selectedServer = "";
		}
		else {
			String txt = text.getText();
			text.setText("");
			
			if(txt.startsWith("/")) {
				if(txt.startsWith("/admin")) {
					if(!uberUser) {
						String pass = txt.substring(7, txt.length());
						gbb.clear();
						gbb.bufferString(pass);
						sendMessage(uid, SERVER, 283, gbb.data());
					}
					else {
						uberUser = false;	
					}
				}			
				else if(txt.startsWith("/reroute")) {
					reroute = !reroute;
				}
				else if(txt.startsWith("/dback")) {
					drawBackground = !drawBackground;
				}
				else if(txt.startsWith("/dplay")) {
					drawPlayers = !drawPlayers;
				}
				else if(txt.startsWith("/dmine")) {
					drawMines = !drawMines;
				}
				else if(txt.startsWith("/team")) {
					txt = txt.substring(6, txt.length());
				
					if(gameScenario == 16 && isPlaying) {
						addUserMessage("*** Team msgs not allowed during Mystery games", Color.white);
					}
					else {
						txt = displayName + " [team] => " + txt;
					
						gbb.clear();
						gbb.bufferByte(1);
						gbb.bufferString(txt);
						sendMessage(uid, SERVER, 207, gbb.data());
					}		
				}
				else if(txt.startsWith("/priv")) {
					try {
						int toUid = new Integer(txt.substring(6, txt.indexOf(" ", 6))).intValue();
					
						txt = txt.substring(txt.indexOf(" ", 6), txt.length());
				
						if(gameScenario == 16 && isPlaying) {
							addUserMessage("*** Private msgs not allowed during Mystery games", Color.white);
						}
						else {
							txt = displayName + " [private] => " + txt;
			
							gbb.clear();
							gbb.bufferByte(2);
							gbb.bufferString(txt);
							sendMessage(uid, toUid, 207, gbb.data());
						}
					}
					catch(NumberFormatException nfe) {
					}
				}
				else if(txt.startsWith("/help")) {
					addUserMessage("*** '/admin [password]' to gain Administrator rights", Color.white); 
					addUserMessage("*** '/team' for team msgs.  Click on player for a private msg", Color.white); 					
					addUserMessage("*** '/quit' to finish an AI game.  '/votedraw' to draw games.  '/votegame [single | team | ai]' to pick next game", Color.white); 					
					addUserMessage("*** '/help' to see this list of commands again", Color.white); 					
				}
				else if(txt.startsWith("/frame_info")) {
					addUserMessage("*** or 'Fps' value either up or down to make the playing experience better for all players.", Color.white); 					
					addUserMessage("*** to other players machines on the server, the server can automatically adjust your 'Skip Frames'", Color.white); 					
					addUserMessage("*** How do we solve this problem?  When the server deems your machine too fast or too slow compaired", Color.white); 
					addUserMessage("*** Bored With Waiting?  Some people are.  This is because your machine might be faster or slower than others.", Color.white); 
				}
				else if(txt.startsWith("/quit")) {
					sendEmptyMessage(uid, SERVER, 289);
				}
				else if(txt.startsWith("/votedraw")) {
					sendEmptyMessage(uid, SERVER, 250);
				}
				else if(txt.startsWith("/votegame")) {
					txt = txt.substring(10, txt.length());
					
					gbb.clear();
					gbb.bufferString(txt);
					sendMessage(uid, SERVER, 251, gbb.data());
				}
			}
			else if(txt.startsWith("#")) {
				if(uberUser) {
					if(txt.startsWith("#recall")) {
						addDebugMessage("*** Requesting Server Stored Message > "+lastValidMsgId);
						gbb.clear();
						gbb.bufferInt(lastValidMsgId);
						sendMessage(uid, SERVER, 280, gbb.data());
					}
					else if(txt.startsWith("#beep")) {
						try {
							int toUid = new Integer(txt.substring(6, txt.length())).intValue();
							sendEmptyMessage(uid, toUid, 281);
						}
						catch(NumberFormatException nfe) {
							sendEmptyMessage(uid, SERVER, 281);
						}
					}
					else if(txt.startsWith("#reset_game")) {
						sendEmptyMessage(uid, SERVER, 282);
					}
					else if(txt.startsWith("#reset_server")) {
						sendEmptyMessage(uid, SERVER, 286);
					}
					else if(txt.startsWith("#kick")) {
						try {
							int toUid = new Integer(txt.substring(6, txt.length())).intValue();
							sendEmptyMessage(uid, toUid, 284);
						}
						catch(NumberFormatException nfe) {}
					}
					else if(txt.startsWith("#ban")) {
						try {
							int toUid = new Integer(txt.substring(5, txt.length())).intValue();
							sendEmptyMessage(uid, toUid, 285);
						}
						catch(NumberFormatException nfe) {}
					}
					else if(txt.startsWith("#nuke")) {
					}
					else if(txt.startsWith("#say")) {
					}
					else if(txt.startsWith("#ip")) {
					}
					else if(txt.startsWith("#stop_timer")) {
						sendEmptyMessage(uid, SERVER, 287);
					}
					else if(txt.startsWith("#get_uid")) {
						if(txt.length() > 9) {
							txt = txt.substring(9, txt.length());
							gbb.clear();
							gbb.bufferString(txt);
							sendMessage(uid, SERVER, 288, gbb.data());
						}
					}
				}
				else {
					addUserMessage("*** You are Not Administrator.", Color.white);	
				}
			}
			else {
				if(txt.length() > 0) {
					if(gameScenario == 16 && isPlaying) {
						txt = "??????" + " => " + txt;
					}
					else {
						txt = displayName + " => " + txt;
					}
				
					gbb.clear();
					gbb.bufferByte(0);
					gbb.bufferString(txt);
					sendMessage(uid, SERVER, 207, gbb.data());
				}
				else {
					addUserMessage("", Color.white);	
				}
			}
		}
		
		text.requestFocus();
	}
	};
	
	private String getHeader() {
		String header = "";
		int len = 0;
		byte[] buf = new byte[8];
		
		try {
			while(header.indexOf("\r\n\r\n") == -1) {
				len = ios.read(buf);
				if(len != -1)
					header+=new String(buf, 0, len);
				else
					return "FAILED\r\n\r\n";
			}
		}
		catch(IOException ioe) {	
			addDebugMessage("!!! "+ioe.toString());
			return "FAILED\r\n\r\n";
		}
		
		return header;
	}
	
	public boolean serverSelection() {
		serverElement se, se2;
		
		joinPanelReq = true;
		renderPanel.initBuffers();
		clock.setDelay((int)(1000.0/(double)fps));
		
		String result = postToGwoNet("cmdId=302&version="+version);
		
		if(result.length() == 0 || !result.startsWith("<t>")) {
			gameState = STATE_POST_LOGIN;
			infoMessage = "Could not connect to GwoNET (Proxy Required?)";
			repaintReq = true;
			return false;
		}
		else {
			servers = new Vector();				
			Vector s1 = new Vector(), s2 = new Vector(), s3 = new Vector();
			
			// Tokenize gwonet list
			StringTokenizer st = new StringTokenizer(result, "\n");
			String server;
			boolean added;
			while(st.hasMoreTokens()) {
				added = false;
				server = st.nextToken();
					
				se = new serverElement();
				se.custom = false;
				se.title = server.substring(server.indexOf("<t>")+3, server.indexOf("<a>"));
				se.address = server.substring(server.indexOf("<a>")+3, server.indexOf("<v>"));
				se.version = new Integer(server.substring(server.indexOf("<v>")+3, server.indexOf("<p>"))).intValue();
				se.players = new Integer(server.substring(server.indexOf("<p>")+3, server.indexOf("<m>"))).intValue();
				se.maxPlayers = new Integer(server.substring(server.indexOf("<m>")+3, server.indexOf("<e>"))).intValue();
				
				if(se.address.equals(selectedServer) && se.players > 0) {
					se.players--;	
				}
				
				// Sort
				if(se.players != se.maxPlayers && se.players > 0) {
					s1.addElement(se);
				}
				
				if(se.players == 0) {
					s2.addElement(se);
				}
				
				if(se.players == se.maxPlayers && se.maxPlayers > 0) {
					s3.addElement(se);
				}
			}
			
			// Add Custom Servers
			for(int i = 0;i < serverList.size();i++) {
				se = new serverElement();
				se.custom = true;
				se.title = "Your Custom Server "+(i+1);
				se.address = (String)serverList.elementAt(i);
				servers.addElement(se);
			}
			
			for(int i = 0;i < s1.size();i++) {
				servers.addElement(s1.elementAt(i));
			}
			for(int i = 0;i < s2.size();i++) {
				servers.addElement(s2.elementAt(i));
			}
			for(int i = 0;i < s3.size();i++) {
				servers.addElement(s3.elementAt(i));
			}
						
			// Compute Pages
			leaguePage = 1;
			leagueNumOfPages = 1+(servers.size()/25);
				
			if(servers.size() % 25 == 0) {
				leagueNumOfPages--;
			}			
		
			gameState = STATE_SERVER_LIST;
			infoMessage = "Click on a Server to Connect to it.";
			repaintReq = true;
		
			return true;
		}
	}
	
	public boolean connectToServer() {
		if(selectedServer.indexOf(':') == -1) {
			selectedServer = selectedServer+":2000";
		}
		
		gameState = STATE_POST_LOGIN;
		infoMessage = "Connecting to "+selectedServer+".  Please Wait...";
		repaintReq = true;
		
		try {
			if(useProxy) {
				if(reroute) {
					//addDebugMessage("*** Initializing Reroute(c) Technology");
					// connect to http proxy
					s = new Socket(proxyServer, proxyPort);
					ios = new gwoDataInputStream(s.getInputStream());
					dos = new gwoDataOutputStream(s.getOutputStream());
					
					//addDebugMessage("*** Connecting to Reroute Remote Proxy");
					dos.writeBytes("CONNECT "+remoteServer+":"+remotePort+" HTTP/1.0\r\n\r\n");
					String header = getHeader();
					//addDebugMessage("*** Status : "+header.substring(0, header.length()-4));
		
					if(header.startsWith("HTTP/1.0 200") || header.startsWith("HTTP/1.1 200")) {
						//addDebugMessage("*** Start Stream");
						dos.writeBytes("CONNECT\r\n\r\n");
						header = getHeader();
						//addDebugMessage("*** Stream Status : "+header.substring(0, header.length()-4));
					
						if(header.startsWith("HTTP/1.0 200 OK")) {
							String connOk;
							if(selectedServer.startsWith("gwo-server01.game-host.org")) {
								dos.writeBytes("CONNECT dangeross-reroute.homeip.net:" + new Integer(selectedServer.substring(selectedServer.indexOf(":")+1, selectedServer.length())).intValue() + " HTTP/1.1\r\n" +
									"Proxy-Connection: Keep-Alive\r\n\r\n");
							}
							else if(selectedServer.startsWith("gwo-server00.game-host.org")) {
								dos.writeBytes("CONNECT dangeross-reroute.homeip.net:" + new Integer(selectedServer.substring(selectedServer.indexOf(":")+1, selectedServer.length())).intValue() + " HTTP/1.1\r\n" +
									"Proxy-Connection: Keep-Alive\r\n\r\n");
							}
							else {
								dos.writeBytes("CONNECT " + selectedServer + " HTTP/1.1\r\n" +
									"Proxy-Connection: Keep-Alive\r\n\r\n");
							}
										
							if((connOk = ios.readLine()) != null) {
								while(!(connOk = ios.readLine()).equals("")) {
									addDebugMessage(connOk);
								}
							
								s.setSoTimeout(500);
								gsl = new gwoClientServerListener(this, ios, dos);
								gsl.start();
				
								return true;
							}
							else {
								s.close();
								infoMessage = "Could not login to "+selectedServer+" (1: Could Not Connect to Server)";
								repaintReq = true;
								
								return false;
							}
						}
						else {
							new gwoDebugWindow("Could not initialize connection stream: "+header);
							s.close();
							infoMessage = "Could not login to "+selectedServer+" (2: Reroute Connection Fail)";
							repaintReq = true;
							
							return false;
						}
					}
					else {
						new gwoDebugWindow("Could not connect to reroute service: "+header);
						s.close();
						infoMessage = "Could not login to "+selectedServer+" (3: Could not Connect to Reroute.  Proxy Correct?)";
						repaintReq = true;
						return false;
					}
				}
				else {
					s = new Socket(proxyServer, proxyPort);
			
					ios = new gwoDataInputStream(s.getInputStream());
					dos = new gwoDataOutputStream(s.getOutputStream());
			
					dos.writeBytes("CONNECT " + selectedServer + " HTTP/1.1\r\n" +
						"Proxy-Connection: Keep-Alive\r\n\r\n");			
					String connOk = ios.readLine();
				
					if(connOk == null) {
						connOk = "FAILED";
					}
			
					if(connOk.startsWith("HTTP/1.0 200") || connOk.startsWith("HTTP/1.1 200")) {					
						addDebugMessage(connOk);
						while((connOk = ios.readLine()) != null) {
							addDebugMessage(connOk);
						}

						s.setSoTimeout(500);
						gsl = new gwoClientServerListener(this, ios, dos);
						gsl.start();
				
						return true;
					}
					else {
						String debug = connOk+"\n";
						while(!(connOk = ios.readLine()).equals("")) {
							debug = debug+connOk+"\n";
						}
						
						new gwoDebugWindow(debug);
						s.close();
						
						infoMessage = "Could not login to "+selectedServer+" (4: Could not Connect to Server)";
						repaintReq = true;
						return false;
					}
				}
			}
			else {
				if(reroute) {
					addDebugMessage("*** Initializing Reroute(c) Technology");
					// connect to http proxy
					s = new Socket(remoteServer, remotePort);
					ios = new gwoDataInputStream(s.getInputStream());
					dos = new gwoDataOutputStream(s.getOutputStream());
					
					addDebugMessage("*** Connecting to Reroute Remote Proxy");
					String header;
						
					addDebugMessage("*** Start Stream");
					dos.writeBytes("CONNECT\r\n\r\n");
					header = getHeader();
					addDebugMessage("*** Stream Status : "+header.substring(0, header.length()-4));
					
					if(header.startsWith("HTTP/1.0 200 OK")) {
						String connOk;
						if(selectedServer.startsWith("gwo-server01.game-host.org")) {
							dos.writeBytes("CONNECT dangeross-reroute.homeip.net:" + new Integer(selectedServer.substring(selectedServer.indexOf(":")+1, selectedServer.length())).intValue() + " HTTP/1.1\r\n" +
								"Proxy-Connection: Keep-Alive\r\n\r\n");
						}
						else if(selectedServer.startsWith("gwo-server00.game-host.org")) {
							dos.writeBytes("CONNECT dangeross-reroute.homeip.net:" + new Integer(selectedServer.substring(selectedServer.indexOf(":")+1, selectedServer.length())).intValue() + " HTTP/1.1\r\n" +
								"Proxy-Connection: Keep-Alive\r\n\r\n");
						}
						else {
							dos.writeBytes("CONNECT " + selectedServer + " HTTP/1.1\r\n" +
								"Proxy-Connection: Keep-Alive\r\n\r\n");
						}
										
						if((connOk = ios.readLine()) != null) {
							while(!(connOk = ios.readLine()).equals("")) {
								addDebugMessage(connOk);
							}
							
							s.setSoTimeout(500);
							gsl = new gwoClientServerListener(this, ios, dos);
							gsl.start();
				
							return true;
						}
						else {
							s.close();
							infoMessage = "Could not login to "+selectedServer+" (5: Could Not Connect to Server)";
							repaintReq = true;
								
							return false;
						}
					}
					else {
						new gwoDebugWindow(header);
						s.close();
						
						infoMessage = "Could not login to "+selectedServer+" (6: Reroute Connection Fail.  Proxy Required?)";
						repaintReq = true;
							
						return false;
					}
				}
				else {
					s = new Socket(selectedServer.substring(0, selectedServer.indexOf(":")), new Integer(selectedServer.substring(selectedServer.indexOf(":")+1, selectedServer.length())).intValue());
					s.setSoTimeout(10000);
			
					ios = new gwoDataInputStream(s.getInputStream());
					dos = new gwoDataOutputStream(s.getOutputStream());
					
					s.setSoTimeout(500);
					gsl = new gwoClientServerListener(this, ios, dos);
					gsl.start();
				
					return true;
				}
			}
		}
		catch(InterruptedIOException iioe) {
			if(!activateReroute()) {
				new gwoDebugWindow(iioe.toString());
				infoMessage = "Could not login to "+selectedServer+" (7: Initial Connection Failure.  Proxy Required?)";
				repaintReq = true;
				return false;
			}
			else {
				return true;	
			}
		}
		catch(IOException ioe) {
			if(!activateReroute()) {
				new gwoDebugWindow(ioe.toString());
				infoMessage = "Could not login to "+selectedServer+" (8: Initial Connection Failure.  Proxy Required?)";
				repaintReq = true;
				return false;
			}
			else {
				return true;	
			}
		}	
	}
	
	private boolean activateReroute() {
		if(!reroute && (selectedServer.startsWith("gwo-server01.game-host.org") || selectedServer.startsWith("gwo-server00.game-host.org"))) {
			reroute = true;
			boolean result = connectToServer();
			reroute = false;
			
			return result;
		}
		else {
			return false;	
		}
		
	}
	
	public void connectionLost() {
		infoMessage = "Connection Lost.";
		repaintReq = true;
		uberUser = false;
		
		clock.stop();
		hClock.stop();
		cClock.stop();
		
		if(!connectToServer()) {
			serverSelection();
		}
	}
	
	private void sendMessage(int fromUid, int toUid, int cmdId, int[] data) {
		//addDebugMessage("<-- "+cmdId+" *** From: "+fromUid+" *** To: "+toUid+" *** Size: "+data.length);
		
		try {
			dos.writeByte(66); // Validation
			dos.flush();
			dos.writeInt(4+4+4+data.length+1);
			dos.flush();
			dos.writeInt(cmdId);
			dos.flush();
			dos.writeInt(toUid);
			dos.flush();
			dos.writeInt(fromUid);
			dos.flush();
			dos.write(data);
			dos.flush();
			dos.writeByte(99); // Validation
			dos.flush();
		}
		catch(IOException ioe) {}
	}
	
	private void sendEmptyMessage(int fromUid, int toUid, int cmdId) {
		//addDebugMessage("<*- "+cmdId+" *** From: "+fromUid+" *** To: "+toUid);
		
		try {
			dos.writeByte(66); // Validation
			dos.flush();
			dos.writeInt(4+4+4+1);
			dos.flush();
			dos.writeInt(cmdId);
			dos.flush();
			dos.writeInt(toUid);
			dos.flush();
			dos.writeInt(fromUid);
			dos.flush();
			dos.writeByte(99); // Validation
			dos.flush();
		}
		catch(IOException ioe) {}
	}
	
	public synchronized void processMessage(byte b, boolean valid) {
		
		try {
			// Find next valid message
			if(!valid) {				
				addDebugMessage("*** Data Invalid (Scrap Data): "+b);			
				addDebugMessage("*** Requesting Server Stored Message > "+lastValidMsgId);
				gbb.clear();
				gbb.bufferInt(lastValidMsgId);
				sendMessage(uid, SERVER, 280, gbb.data());
				
				//addDebugMessage("");
				String data = "";
				do {
					b = ios.readByte();
					data+="["+b+"]";
				}
				while(b != 66);				
				addDebugMessage(data);
				
				// packet size
				int size = ios.readInt();
			}		
		
			int msgId = ios.readInt();
			int cmdId = ios.readInt();
			int toUid = ios.readInt();
			int fromUid = ios.readInt();
		
			//addDebugMessage("--> ["+msgId+"] "+cmdId+" *** From: "+fromUid+" *** To: "+toUid);
		
			//if(b == 66) {
				if(cmdId == 100) { // Initial Login
					serverVersion = ios.readInt();
					
					if(serverVersion == version) {
						if(displayName.length() > 40)
							displayName = displayName.substring(0, 40);
					
						gbb.clear();			
						gbb.bufferString(displayName);
						gbb.bufferInt(tempUid);
						gbb.bufferString(password);
						//sendMessage(toUid, SERVER, 200, gbb.data());
						sendMessage(toUid, SERVER, 201, gbb.data());
					}
					else {
						uid = toUid;
						sendEmptyMessage(uid, SERVER, 260);
					}
					
					//gameState = STATE_POST_LOGIN;
				}
				else if(cmdId == 101) { // Initial Login Confirm
					uid = ios.readInt();
					displayName = ios.readString();
					gwoNetServer = ios.readBool();
					
					addUserMessage("*** '/admin [password]' to gain Administrator rights", Color.white); 
					addUserMessage("*** '/team' for team msgs.  Click on player for a private msg", Color.white); 
					addUserMessage("*** '/quit' to finish an AI game.  '/votedraw' to draw games.  '/votegame [single | team | ai]' to pick next game", Color.white); 					
					addUserMessage("*** '/help' to see this list of commands again", Color.white); 					
					addUserMessage("*** Welcome to Galaxy Wars Online", Color.white);
					
					isPlaying = false;
					isWatching = false;
				}
				else if(cmdId == 102) { // Ping
					sendEmptyMessage(uid, SERVER, 290);
				}
				else if(cmdId == 103) { // Waiting for players
					title = "GWO - http://www.dangeross.com/gwo/";
					titleReq = true;
					
					//planets = new Vector();
					//players = new Vector();
					isWatching = false;
					isPlaying = false;
					
					iconReq = true;
					
					joinPressed = true;
					
					clock.stop();
					hClock.stop();

					joinPanelReq = true;
					
					infoMessage = "Not enough players online.  Do you want to start a practice game against AI players?";
					gameState = STATE_POST_LOGIN;
				}
				else if(cmdId == 104) { // Game being played
					title = "GWO - http://www.dangeross.com/gwo/";
					titleReq = true;
					
					//planets = new Vector();
					//players = new Vector();
					shotDone = true;
					isWatching = false;
					isPlaying = false;
					
					iconReq = true;
					
					joinPressed = true;
					
					clock.stop();
					hClock.stop();

					watchPanelReq = true;
					
					infoMessage = "There is a game currently being played.  Please wait.";
					gameState = STATE_NO_PLAYER;
				}
				else if(cmdId == 105) { // New Game starting
					title = "GWO - http://www.dangeross.com/gwo/";
					titleReq = true;
					
					shotDone = true;
					isWatching = false;
					showSeconds = false;
					
					iconReq = true;
					
					joinPressed = false;
					
					clock.stop();
					hClock.stop();

					joinPanelReq = true;
					
					infoMessage = "Do you want to join a new game?";
					gameState = STATE_JOIN_GAME;
				}
				else if(cmdId == 106) { // New Game countdown
					secondsLeft = ios.readUnsignedByte();
					if(!isFocused && beep.isSelected() && secondsLeft == 9 && !joinPressed)
						tools.beep();
						
					infoMessage = "New Game in "+secondsLeft+" seconds.";
					
					cClock.start();
				}
				else if(cmdId == 107) { // Confirm Join
					infoMessage = "Please wait...";
					isPlaying = true;
					isDead = false;
					shieldsAvailable = 0;
					shieldUsed = false;
					killedThisShot = false;
					mydataShots = 0;
					mydataBulletLife = 0;
					mydataKills = 0;
					mydataHits = 0;
					mydataHitOwn = false;
					mydataHitTeam = false;
					mydataShield = 0;
					mydataMineKill = false;
					mydataWormKill = false;
					pathLength = 0;
					//missileCollideCount = 0;
					//missileCollidePlayer = 0;
					
					sendEmptyMessage(uid, SERVER, 206);
				}
				else if(cmdId == 108) { // Generating
					infoMessage = "Generating...";
					showSeconds = false;
					isFirstGo = true;
					//planets = new Vector();
					//players = new Vector();
					mines = new Vector();
					
					cClock.stop();
					clearPanelReq = true;
				}
				else if(cmdId == 109) { // Starfield
					starfield = new Vector();
					int maxNumOfStars = ios.readUnsignedByte();
					double starSize = 1.0 + (2.0 * ios.readDouble());
					starElement s;
		
					for(int i = 0;i < maxNumOfStars;i++) {
						s = new starElement();
						s.x = (int)((double)900 * Math.random());
						s.y = (int)((double)700 * Math.random());
						s.radius = (int)((starSize * 3.0) * Math.random() * Math.random() * Math.random() + (starSize * 0.6) * Math.random() + (starSize * 0.6) * Math.random() + 2.0);
                				s.color = new Color((int)((starSize * 4.8) * Math.random() + (starSize * 32.5) * Math.random()), (int)((starSize * 1.0) * Math.random() + (starSize * 4.0) * Math.random()), (int)((starSize * 3.3) * Math.random() + (starSize * 23.3) * Math.random()));
						starfield.addElement(s);
					}
				}
				else if(cmdId == 110) { // Planets
					planets = new Vector();
					planetElement p;
					int num = ios.readUnsignedByte();
					
					for(int i = 0;i < num;i++) {
						p = new planetElement();
						p.x = (double)ios.readInt();
						p.y = (double)ios.readInt();
						p.radius = ios.readDouble();
						p.color = new Color(ios.readUnsignedByte(), ios.readUnsignedByte(), ios.readUnsignedByte());
						p.M = ios.readDouble();
						p.density = ios.readDouble();
						p.shading = ios.readUnsignedByte();
						p.impact = (int)ios.readByte();
					
						planets.addElement(p);
					}
				}
				else if(cmdId == 111) { // Players
					players = new Vector();
					playerElement p;
					int num = ios.readInt();
					
					for(int i = 0;i < num;i++) {
						p = new playerElement();
						p.displayName = ios.readString();
						p.uid = ios.readInt();
					
						if(uberUser)
							p.displayName = p.displayName+" ("+p.uid+")";
					
						p.x = (double)ios.readInt();
						p.y = (double)ios.readInt();
						p.team = ios.readByte();
						p.status = ios.readByte();
						if(p.status == 2) {
							p.color = new Color(100, 100, 100);
							p.tColor = new Color(ios.readUnsignedByte(), ios.readUnsignedByte(), ios.readUnsignedByte());
						}
						else {
							p.color = new Color(ios.readUnsignedByte(), ios.readUnsignedByte(), ios.readUnsignedByte());
							p.tColor = p.color;
							p.mColor = p.color;
						}
						p.radius = ios.readDouble();
						p.mRadius = p.radius/4.0;
						p.mxDebP = new double[1];
						p.myDebP = new double[1];
						p.mxDebV = new double[1];
						p.myDebV = new double[1];
					
						players.addElement(p);
					}
				}
				else if(cmdId == 112) { // Can take shot
					shotDone = false;
					showSeconds = false;
		
					for(int i = 0;i < players.size();i++) {
						((playerElement)players.elementAt(i)).fired = false;
						((playerElement)players.elementAt(i)).hyper = false;
					}
	
					if(gameType == TYPE_SINGLE_SHIELDS_GAME || gameType == TYPE_TEAM_SHIELDS_GAME || gameType == TYPE_HVM_AI_SHIELDS_GAME || gameType == TYPE_SINGLE_AI_SHIELDS_GAME || gameType == TYPE_TEAM_AI_SHIELDS_GAME) {
						if(killedThisShot && !isDead && !isWatching) {
							shieldsAvailable++;
						}
			
						killedThisShot = false;
					}
					
					if(!isDead && isPlaying) {
						aimPanelReq = true;
					}
					
					gameState = STATE_GAME_AIM;
				}
				else if(cmdId == 113) { // Player shot
					playerElement p;
					int angle = ios.readInt();
					int power = ios.readInt();
					
					for(int i = 0;i < players.size();i++) {
						p = (playerElement)players.elementAt(i);
						
						if(p.uid == fromUid) {
							p.angle = angle;
							p.power = power;
							p.fired = true;
							
							players.setElementAt(p, i);
						}
					}
				}
				else if(cmdId == 114) { // Shot countdown
					secondsLeft = ios.readUnsignedByte();
					showSeconds = true;
					
					cClock.start();
				}
				else if(cmdId == 115) { // Fire!
					playerElement p;
					showSeconds = false;
					cClock.stop();
					shotDone = true;
					step = 0;
					pathLength = 0;
					
					clearPanelReq = true;
					autoMessage = "";
					boolean hasFired = false;
					
					for(int i = 0;i < players.size();i++) {
						p = (playerElement)players.elementAt(i);
						
						if(p.fired) { //75.0 // 35.0
							hasFired = true;
							
							p.mWorm = false;
							p.mxVelocity = ((double)p.power / 27.0 + 0.2) * 0.8 * Math.sin((double)p.angle / 180.0 * 3.141592653589793);
							p.myVelocity = ((double)p.power / 27.0 + 0.2) * 0.8 * Math.cos((double)p.angle / 180.0 * 3.141592653589793);
							p.mX = p.x + (p.radius + 1.0) * Math.sin((double)p.angle / 180.0 * 3.141592653589793);
							p.mY = p.y + (p.radius + 1.0) * Math.cos((double)p.angle / 180.0 * 3.141592653589793);
							p.mX1 = (int)p.mX;
							p.mY1 = (int)p.mY;
							p.mX2 = (int)p.mX;
							p.mY2 = (int)p.mY;
							p.mX3 = (int)p.mX;
							p.mY3 = (int)p.mY;
							p.mX4 = (int)p.mX;
							p.mY4 = (int)p.mY;
							p.mStatus = 1; //alive
							
							players.setElementAt(p, i);
						}
					}
					
					renderPanel.doDetailedRender();
					gameState = STATE_GAME_FIRE;	
					clock.start();
					
					if(hasFired)
						playSound("missile2.wav");
				}
				else if(cmdId == 116) { // Player Hyper		
					gameState = STATE_GAME_HYPER;

					showSeconds = false;
					playerElement p;
					int x = ios.readInt();
					int y = ios.readInt();
					
					if(uid == fromUid) {
						pathLength = 0;			
					}
						
					for(int i = 0;i < players.size();i++) {
						p = (playerElement)players.elementAt(i);
						
						if(p.uid == fromUid) {
							//p.hX = p.x;
							//p.hY = p.y;
							p.hX = (double)x;
							p.hY = (double)y;
							p.hStep = 1;
							p.hyper = true;
							
							players.setElementAt(p, i);
						}
					}
					hClock.start();
					
					playSound("sci_fi_takeoff.wav");
				}
				else if(cmdId == 117) { // Game type
					if(!isFocused && beep.isSelected())
						tools.beep();

					infoMessage = "Generating...";
					gameScenario = ios.readUnsignedByte();
					gameType = ios.readUnsignedByte();
					infoMessage = ios.readString();
			
					title = "GWO - "+infoMessage;
					titleReq = true;
				}		
				else if(cmdId == 118) { // Player Quit
					playerElement p;
						
					for(int i = 0;i < players.size();i++) {
						p = (playerElement)players.elementAt(i);
						
						if(p.uid == fromUid) {
							p.color = new Color(100, 100, 100);
							p.status = 2; //dead
						}
					}
					
					renderPanel.doDetailedRender();
				}
				else if(cmdId == 119) { // Player message
					playerElement p;
					String txt = ios.readString();
					Color col = Color.white;
						
					if(gameState > STATE_JOIN_GAME) {
						for(int i = 0;i < players.size();i++) {
							p = (playerElement)players.elementAt(i);
						
							if(p.uid == fromUid) {
								col = p.tColor;
							}
						}
					}
					
					Vector messageLines = new Vector();
					String word = "", line = "";
			
					StringTokenizer st = new StringTokenizer(txt, " ");
			
					while(st.hasMoreTokens()) {
						word = st.nextToken();
				
						if(renderPanel.messageFontM.stringWidth(line + " " + word) < width - 30) {
							if(line.equals("")) {
								if(messageLines.size() == 0)
									line = word;
								else
									line = "      " + word;
							}		
							else {
								line = line + " " + word;
							}
						}
						else {
							messageLines.addElement(line);
							line = "      " + word;
						}
					}
			
					if(!line.equals("")) {
						messageLines.addElement(line);
					}
					
					for(int i = messageLines.size()-1;i >= 0;i--) {
						addUserMessage((String)messageLines.elementAt(i), col);
					}				
				}
				else if(cmdId == 120) { // Confirm Un-Join
					isPlaying = false;
				}
				else if(cmdId == 121) {	// Result: Win	
					result = RESULT_WIN;
					gameState = STATE_GAME_END;
					isPlaying = false;
					resultAwards = new Vector();
					
					gbb.clear();
					gbb.bufferInt(mydataShots);
					gbb.bufferInt(mydataBulletLife);
					gbb.bufferInt(mydataKills);
					gbb.bufferInt(mydataHits);
					gbb.bufferBoolean(mydataHitOwn);
					gbb.bufferBoolean(mydataHitTeam);
					gbb.bufferInt(mydataShield);
					gbb.bufferBoolean(mydataMineKill);
					gbb.bufferBoolean(mydataWormKill);
					sendMessage(uid, SERVER, 208, gbb.data());
				}
				else if(cmdId == 122) {	// Result: Draw
					result = RESULT_DRAW;
					gameState = STATE_GAME_END;
					isPlaying = false;
					resultAwards = new Vector();
					
					gbb.clear();
					gbb.bufferInt(mydataShots);
					gbb.bufferInt(mydataBulletLife);
					gbb.bufferInt(mydataKills);
					gbb.bufferInt(mydataHits);
					gbb.bufferBoolean(mydataHitOwn);
					gbb.bufferBoolean(mydataHitTeam);
					gbb.bufferInt(mydataShield);
					gbb.bufferBoolean(mydataMineKill);
					gbb.bufferBoolean(mydataWormKill);
					sendMessage(uid, SERVER, 208, gbb.data());
				}
				else if(cmdId == 123) {	// Result: Lose
					result = RESULT_LOSE;
					gameState = STATE_GAME_END;
					isPlaying = false;
					resultAwards = new Vector();
					
					gbb.clear();
					gbb.bufferInt(mydataShots);
					gbb.bufferInt(mydataBulletLife);
					gbb.bufferInt(mydataKills);
					gbb.bufferInt(mydataHits);
					gbb.bufferBoolean(mydataHitOwn);
					gbb.bufferBoolean(mydataHitTeam);
					gbb.bufferInt(mydataShield);
					gbb.bufferBoolean(mydataMineKill);
					gbb.bufferBoolean(mydataWormKill);
					sendMessage(uid, SERVER, 208, gbb.data());
				}
				else if(cmdId == 124) {	// Result: Game Over	
					result = RESULT_GAME_OVER;
					gameState = STATE_GAME_END;
					resultAwards = new Vector();
				}
				else if(cmdId == 125) {	// Result: Awards	
					String award = ios.readString();
					
					Vector messageLines = new Vector();
					String word = "", line = "";
			
					StringTokenizer st = new StringTokenizer(award, " ");
			
					while(st.hasMoreTokens()) {
						word = st.nextToken();
				
						if(renderPanel.messageFontM.stringWidth(line + " " + word) < width - 30) {
							if(line.equals("")) {
								if(messageLines.size() == 0)
									line = word;
								else
									line = "      " + word;
							}		
							else {
								line = line + " " + word;
							}
						}
						else {
							messageLines.addElement(line);
							line = "      " + word;
						}
					}
			
					if(!line.equals("")) {
						messageLines.addElement(line);
					}
					
					for(int i = messageLines.size()-1;i >= 0;i--) {
						resultAwards.addElement((String)messageLines.elementAt(i));
					}				
				}
				else if(cmdId == 127) { // Reset/Quit game in progress
					String msg = ios.readString();
					addUserMessage("*** "+msg, Color.white);
				}			
				else if(cmdId == 128) { // Kicked from Game for idling
					String name = ios.readString();
					
					if(uid == fromUid) {
						isPlaying = false;
						isWatching = true;
						isDead = true;
						autoJoinBox.setSelected(false);
						addUserMessage("*** You have been Kicked from the Game for Idling.", Color.white);
					}					
					else {
						addUserMessage("*** "+name+" has been Kicked from the Game for Idling.", Color.white);
					}					
					
					playerElement p;
						
					for(int i = 0;i < players.size();i++) {
						p = (playerElement)players.elementAt(i);
						
						if(p.uid == fromUid) {
							p.color = new Color(100, 100, 100);
							p.status = 2; //dead
						}
					}
					
					renderPanel.doDetailedRender();
				}
				else if(cmdId == 129) { // Player in control
					isController = true;
				}
				else if(cmdId == 130) { // Watch: Wait for sync
					infoMessage = "Waiting for Synchronization Point.";
				}
				else if(cmdId == 131) { // Watch: New Player watching
					String name = ios.readString();
					addUserMessage("*** "+name+" is now Watching the Game.", Color.white);
				}
				else if(cmdId == 132) { // Watch: Synchronized
					isWatching = true;
					gameState = STATE_GAME_AIM;
				}
				else if(cmdId == 133) { // Login Failed: Kicked/Banned
					infoMessage = "Ban/Kick Timeout Active on this Server.";
					
					try {
						gsl.quit();
						s.close();
					}
					catch(IOException ioe) {}
					
					loginReq = true;					
				}
				else if(cmdId == 134) { // Login Failed: Online Already
					infoMessage = "You are Already Online.";
					
					try {
						gsl.quit();
						s.close();
					}
					catch(IOException ioe) {}
					
					loginReq = true;					
				}
				else if(cmdId == 135) { // Login Failed: Password Incorrect
					infoMessage = "Password is Incorrect for this UID.";
					
					try {
						gsl.quit();
						s.close();
					}
					catch(IOException ioe) {}
					
					loginReq = true;					
				}
				else if(cmdId == 136) { // Login Failed: UID Incorrect
					infoMessage = "Invalid UID.  Please Register again.";
					uid = 0;
					
					try {
						gsl.quit();
						s.close();
					}
					catch(IOException ioe) {}
					
					loginReq = true;					
				}
				else if(cmdId == 137) { // GwoNET status
					gwoNetServer = ios.readBool();
				}
				else if(cmdId == 138) { // Player shield
					playerElement p;
						
					for(int i = 0;i < players.size();i++) {
						p = (playerElement)players.elementAt(i);
						
						if(p.uid == fromUid) {
							p.shield++;
						}
					}
				}
				else if(cmdId == 139) { // Mines
					mines = new Vector();
					mineElement me;
					int numofmines = ios.readUnsignedByte();
					double radius = ios.readDouble();
					
					for(int i = 0;i < numofmines;i++) {
						me = new mineElement();
						me.x = (double)ios.readInt();
						me.y = (double)ios.readInt();
						me.radius = radius;
						me.status = ios.readUnsignedByte();	
						
						mines.addElement(me);
					}
				}
				else if(cmdId == 140) { // Mines Init
					mines = new Vector();
					mineElement me;
					int numofmines = ios.readUnsignedByte();
					double radius = ios.readDouble();
					
					for(int i = 0;i < numofmines;i++) {
						me = new mineElement();
						me.x = (double)ios.readInt();
						me.y = (double)ios.readInt();
						me.radius = radius;
						me.status = 0;	
						
						mines.addElement(me);
					}
				}
				else if(cmdId == 141) { // Frame Skip adjust down
					boolean ajustFps = ios.readBool();
					
					if(frameSkipTo > 0) {
						frameSkipTo--;
						autoMessage = "FrameSkip--, See '/frame_info'";
					}
					else if(ajustFps && fps >= 21){
						fps-=20;
						clock.setDelay((int)(1000.0/(double)fps));
						autoMessage = "Fps--, See '/frame_info'";
					}
				}
				else if(cmdId == 142) { // Frame Skip adjust up
					boolean ajustFps = ios.readBool();
					
					if(ajustFps && fps <= 180){
						fps+=20;
						clock.setDelay((int)(1000.0/(double)fps));
						autoMessage = "Fps++, See '/frame_info'";
					}
					else if(frameSkipTo < 10) {
						frameSkipTo++;
						autoMessage = "FrameSkip++, See '/frame_info'";
					}
				}
				else if(cmdId == 143) { // Login Failed: Maximum players reached
					infoMessage = "Maximum amount of Players for this Server reached.";
					
					try {
						gsl.quit();
						s.close();
					}
					catch(IOException ioe) {}
					
					loginReq = true;					
				}
				else if(cmdId == 160) { // File Transfer: Initial
					ftSize = ios.readLong();
					
					fileTransferReq = true;
				}
				else if(cmdId == 161) { // File Transfer: Start
					try {
						ftFos = new FileOutputStream("gwo2.jar");
					}
					catch(FileNotFoundException fnfe) {
						addDebugMessage(fnfe.toString());
					}
					
					gft.started();
					sendEmptyMessage(uid, SERVER, 262);
				}
				else if(cmdId == 162) { // File Transfer: Data
					int size = ios.readInt();
					ftData = new byte[size];
					
					if(ios.available() >= size) {
						ios.readFully(ftData);
						ftFos.write(ftData);
						gft.received(size);
						
						sendEmptyMessage(uid, SERVER, 262);
					}
					else {
						sendEmptyMessage(uid, SERVER, 264);	
					}
				}
				else if(cmdId == 163) { // File Transfer: End
					infoMessage = "Please Restart Galaxy Wars Online.  Update was Successful.";
					ftFos.close();
					ftData = null;
									
					gft.finished();
				}
				else if(cmdId == 164) { // File Transfer: Busy Queuing
					gft.queued();
				}
				else if(cmdId == 170) { // Login from other location
					gsl.quit();
					gameState = STATE_POST_LOGIN;
					infoMessage = "UID Login from Another Location";
					
					loginReq = true;
				}
				else if(cmdId == 180) { // UberUser: beep
					tools.beep();
				}
				else if(cmdId == 181) { // UberUser: logged in
					uberUser = true;
					addUserMessage("*** '#reset_game' '#reset_server'", Color.white); 
					addUserMessage("*** '#beep [uid (0 for all)]' '#get_uid [partial name]'", Color.white); 
					addUserMessage("*** '#kick [uid]' kicks user for 10 mins.  '#ban [uid]' bans users from server", Color.white); 
					addUserMessage("*** You now have Administrator rights for this Server", Color.white);
				}
				else if(cmdId == 182) { // UberUser: Kicked
					infoMessage = "You have been Kicked from the Server.";
					
					try {
						gsl.quit();
						s.close();
					}
					catch(IOException ioe) {}
					
					gameState = STATE_POST_LOGIN;					
					loginReq = true;					
				}
				else if(cmdId == 183) { // UberUser: Ban
					infoMessage = "You have been Banned from the Server.";
					
					try {
						gsl.quit();
						s.close();
					}
					catch(IOException ioe) {}
					
					gameState = STATE_POST_LOGIN;					
					loginReq = true;					
				}
				else if(cmdId == 184) { // UberUser: get uid
					int hisUid = ios.readInt();
					String hisName = ios.readString();
					
					addUserMessage("*** UID for "+hisName+" is "+hisUid, Color.white);					
				}
				else if(cmdId == 190) { // League: Page
					leaguePage = ios.readInt();
					leagueNumOfPages = ios.readInt();
					int noe = ios.readInt();
					leagueElement ue;
					
					league = new Vector();
					for(int i = 0;i < noe;i++) {
						ue = new leagueElement();
						
						ue.position = ios.readInt();
						ue.displayName = ios.readString();
						ue.uid = ios.readInt();
						
						if(uberUser)
							ue.displayName+=" ("+ue.uid+")";
						
						ue.online = ios.readBool();
						ue.joined = ios.readBool();
						ue.played = ios.readInt();
						ue.won = ios.readInt();
						ue.drawn = ios.readInt();
						ue.lost = ios.readInt();
						ue.killsFor = ios.readInt();
						ue.killsAgainst = ios.readInt();
						ue.points = ios.readInt();
						
						league.addElement(ue);
					}
				}
				else if(cmdId == 191) { // League: element
					leagueElement ue = new leagueElement();
							
					ue.position = ios.readInt();
					ue.displayName = ios.readString();
					ue.uid = ios.readInt();
						
					if(uberUser)
						ue.displayName+=" ("+ue.uid+")";

					ue.online = ios.readBool();
					ue.joined = ios.readBool();
					ue.played = ios.readInt();
					ue.won = ios.readInt();
					ue.drawn = ios.readInt();
					ue.lost = ios.readInt();
					ue.killsFor = ios.readInt();
					ue.killsAgainst = ios.readInt();
					ue.points = ios.readInt();
						
					if(leaguePage == leagueNumOfPages) {
						if(league.size() < 25) {
							league.addElement(ue);
						}
						else {
							leagueNumOfPages++;
						}
					}
				}
				else if(cmdId == 192) { // League: element online
					String name = ios.readString();
					boolean isOnline = ios.readBool();
					
					leagueElement ue;
						
					for(int i = 0;i < league.size();i++) {
						ue = (leagueElement)league.elementAt(i);
						
						if(ue.uid == fromUid) {
							ue.displayName = name;
							
							if(uberUser)
								ue.displayName+=" ("+ue.uid+")";
							
							ue.online = isOnline;
							league.setElementAt(ue, i);
						}
					}
					
					if(!isOnline) {
						playerElement p;
						
						for(int i = 0;i < players.size();i++) {
							p = (playerElement)players.elementAt(i);
						
							if(p.uid == fromUid) {
								p.color = new Color(100, 100, 100);
								p.status = 2; //dead
							}
						}
					}
				}
				else if(cmdId == 193) { // League: element joined
					boolean isJoined = ios.readBool();
					int joinCount = 0;
					leagueElement ue;
						
					for(int i = 0;i < league.size();i++) {
						ue = (leagueElement)league.elementAt(i);
						
						if(ue.uid == fromUid) {
							ue.joined = isJoined;
							
							if(ue.joined)
								setTitle("*** GWO ***");
							
							league.setElementAt(ue, i);
						}
						
						if(ue.joined)
							joinCount++;
					}
					
					if(joinCount < 2) {
						infoMessage = "Do you want to join a new game?";
						cClock.stop();
					}
				}
				else if(cmdId == 194) { // League: reset all joined
					leagueElement ue;
					
					isPlaying = false;
						
					title = "GWO";
					titleReq = true;
						
					for(int i = 0;i < league.size();i++) {
						ue = (leagueElement)league.elementAt(i);
						ue.joined = false;	
						league.setElementAt(ue, i);
					}
				}

				//addDebugMessage("--> Check Footer");
				b = ios.readByte();
		
				if(b == 99) {
					lastValidMsgId = msgId;
				}
				/*if(b != 99) {
					addDebugMessage("*** Data Footer Invalid: "+b);
					dataInvalid = true;
				
					addDebugMessage("*** Requesting Server Stored Message > "+lastValidMsgId);
					gbb.clear();
					gbb.bufferInt(lastValidMsgId);
					sendMessage(uid, SERVER, 280, gbb.data());
					
					lastValidMsgId = msgId;
				}
				else {
					lastValidMsgId = msgId;
				}*/
			//}
			//else {
			//	addDebugMessage("DATA HEADER INVALID: "+b);
			//	dataInvalid = true;
			//}
			//addDebugMessage("Pre");
			//renderPanel.repaint();
			//addDebugMessage("Post");
			repaintReq = true;
		}
		catch(IOException ioe) {}
	}
	
	public void actionPerformed(ActionEvent e) {			
		if(repaintReq) {
			renderPanel.repaint();
			repaintReq = false;	
		}
		
		if(gameScenario == 18)
			hyper.setEnabled(false);
		else
			hyper.setEnabled(true);
				
		shield.setLabel("Shield ("+shieldsAvailable+")");
			
		if(shieldsAvailable > 0)
			shield.setEnabled(true);
		else
			shield.setEnabled(false);
				
		if(gameState != STATE_SERVER_LIST) {
			play.setEnabled(true);
			
			if(isPlaying)
				play.setText("Un-Join Game");
			else
				play.setText("Join Game");
				
			server.setText("Server");
		}
		else {
			server.setText("Refresh");
			play.setEnabled(false);
		}
				
		// GwoNET post
		if(dataToPost.size() > 0) {
			String out = (String)dataToPost.elementAt(0);
			dataToPost.removeElementAt(0);
				
			String in = postToGwoNet(out);
				
			if(out.startsWith("cmdId=301")) {
				gcl.register(in);
			}
			else if(out.startsWith("cmdId=303")) {
				gcl.search(in);
			}
		}
		
		if(fileTransferReq) {
			gft = new gwoFileTransfer(this, serverVersion, ftSize);
			fileTransferReq = false;
		}
			
		if(iconReq) {
			if(gameState < STATE_JOIN_GAME)
				setIconImage(icon2);
			else
				setIconImage(icon);
					
			iconReq = false;
		}
			
		if(joinPanelReq) {
			masterPanel.removeAll();
			masterPanel.add("Center", newGamePanel);
			contentPanel.revalidate();
			joinPanelReq = false;
		}
			
		if(watchPanelReq) {
			masterPanel.removeAll();
			masterPanel.add("Center", watchGamePanel);
			contentPanel.revalidate();
			watchPanelReq = false;
		}
			
		if(aimPanelReq) {
			masterPanel.removeAll();
			masterPanel.add("Center", aimAndFirePanel);
			contentPanel.revalidate();
			aimPanelReq = false;
		}
			
		if(clearPanelReq) {
			masterPanel.removeAll();
			contentPanel.revalidate();
			clearPanelReq = false;
		}
			
		if(titleReq) {
			setTitle(title);
			titleReq = false;
		}	
			
		if(autoJoinBox.isSelected() && !joinPressed) {
			joinPressed = true;
			sendEmptyMessage(uid, SERVER, 202);				
		}
			
		if(loginReq) {
			gcl = new gwoClientLogin(this);
			loginReq = false;
		}
	}
	
	ActionListener counter = new ActionListener() {
	
		public void actionPerformed(ActionEvent e) {
			secondsLeft--;
			
			infoMessage = "New Game in "+secondsLeft+" seconds.";
			renderPanel.repaint();
			
			if(secondsLeft == 0) {
				cClock.stop();
				showSeconds = false;	
			}
		}
	};
	
	ActionListener calculateHyperspace = new ActionListener() {
	
		public void actionPerformed(ActionEvent e) {
				playerElement p;
				int usersHyping = 0;
				
				for(int i = 0;i < players.size();i++) {
					p = (playerElement)players.elementAt(i);
				
					if(p.hyper) {
						p.hStep++;

						if(p.hStep > 20) {
							p.x = p.hX;
							p.y = p.hY;
							p.hyper = false;
						}
						
						usersHyping++;
						
						players.setElementAt(p, i);
					}
				}
				
				if(usersHyping == 0) {
					gameState = STATE_GAME_WAIT;
		
					if(isWatching) {
						sendEmptyMessage(uid, SERVER, 212);
					}
					else {
						sendEmptyMessage(uid, SERVER, 206);
					}		
					
					hClock.stop();
				}	
					
				renderPanel.repaint();
		}
	};
	
	public void playSound(String file) {
		if(sound) {
			//System.out.println(file);
			
			try {
				Clip clip;
				AudioInputStream ain = AudioSystem.getAudioInputStream(this.getClass().getResource(file));
				try {
					DataLine.Info info = new DataLine.Info(Clip.class,ain.getFormat( ));
					clip = (Clip) AudioSystem.getLine(info);
 					clip.open(ain);
 
 					// Set Volume
   					FloatControl control = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
     				//float vol = (float)soundVolume/(float)100; //(((control.getMaximum()/(float)100)*(float)soundVolume));
					double gain = (1.0/100.0)*(double)soundVolume;    // number between 0 and 1 (loudest)
    				float dB = (float)(Math.log(gain)/Math.log(10.0)*20.0);
	   				control.setValue(dB);
				}
				finally { // We're done with the input stream.
					ain.close( );
				}
				clip.start();
			}
			catch(IOException ioe) {System.out.println(ioe.toString());}
			catch(UnsupportedAudioFileException uafe) {System.out.println(uafe.toString());}
			catch(LineUnavailableException lue) {System.out.println(lue.toString());}
		}
	}
	
	ActionListener calculateMisslePaths = new ActionListener() {
	
		public void actionPerformed(ActionEvent e) {
		playerElement p;
		planetElement pl;
		mineElement me1, me2;
		
		for(int i = 0;i < players.size();i++) {
			p = (playerElement)players.elementAt(i);
			
			if(p.fired && p.mStatus != 0) {
				for(int k = 0;k < planets.size();k++) {
					pl = (planetElement) planets.elementAt(k);
					
					double d6 = 1.0;
					double d1 = pl.x - p.mX;
					double d2 = pl.y - p.mY;
					double d3 = Math.atan(d2 / d1);
					if (d1 < 0.0)
						d6 = -1.0;
					double d7 = d1 * d1 + d2 * d2;
                               
					if (d7 >= pl.radius * pl.radius) {
						double d8 = d6 * 0.2 * pl.M / d7;
						p.mxVelocity += Math.cos(d3) * d8 * 0.25;
						p.myVelocity += Math.sin(d3) * d8 * 0.25;
						
						
					}
					else if (pl.impact == 1) {
						if(p.mStatus == 1)
							playSound("explosion2.wav");
						
						p.mStatus = 4;
						p.mColor = pl.color.darker();
					}
					else if (pl.impact == 2) {
						p.mStatus = 0;
					}
					else if (pl.impact == 3) {
						p.mX = Math.random() * width;
						p.mY = Math.random() * height;
					}
					else if (pl.impact == 4) {
						p.mWorm = true;
						
						double d4;
						if (d6 < 0.0)
							d4 = d3 + 3.141592653589793;
						else
							d4 = d3;
						int j = (int)((double)planets.size() * Math.random());
   						p.mX = ((planetElement)planets.elementAt(j)).x + Math.cos(d4) * (((planetElement)planets.elementAt(j)).radius + 0.5);
						p.mY = ((planetElement)planets.elementAt(j)).y + Math.sin(d4) * (((planetElement)planets.elementAt(j)).radius + 0.5);
					}
					else if (pl.impact <= 0) {
						p.mWorm = true;
						
						double d5;
						if (d6 < 0.0)
							d5 = d3 + 3.141592653589793;
						else
							d5 = d3;
						int j = -pl.impact;
						p.mX = ((planetElement)planets.elementAt(j)).x + Math.cos(d5) * (((planetElement)planets.elementAt(j)).radius + 0.5);
						p.mY = ((planetElement)planets.elementAt(j)).y + Math.sin(d5) * (((planetElement)planets.elementAt(j)).radius + 0.5);
					}
				}
				
				if(p.mStatus == 1) {
					for(int k = 0;k < mines.size();k++) {
						me1 = (mineElement)mines.elementAt(k);
					
						if(me1.status == 0) {
							if((p.mX - me1.x) * (p.mX - me1.x) + (p.mY - me1.y) * (p.mY - me1.y) < (p.mRadius+me1.radius) * (p.mRadius+me1.radius)) {
								me1.status = 1;
								me1.hitUid = p.uid;
								me1.hitTeam = p.team;
								
								if(p.mWorm) {
									me1.hitWorm = true;	
								}
								else {
									me1.hitWorm = false;	
								}
							
								renderPanel.doDetailedRender();
								playSound("explosion3.wav");
							
								p.mStatus = 3;
							
								if(p.uid == uid || isController) {
									gbb.clear();
									gbb.bufferByte((byte)k);
									sendMessage(uid, SERVER, 213, gbb.data());
								}
						
								mines.setElementAt(me1, k);
							}
						}
						else if(me1.status == 1) {
							if((p.mX - me1.x) * (p.mX - me1.x) + (p.mY - me1.y) * (p.mY - me1.y) <= (p.mRadius+me1.mRadius) * (p.mRadius+me1.mRadius)) {
								p.mStatus = 3;
							
								playSound("explosion2.wav");
							}						
						}
					}
				}
				
				if(p.mStatus == 1) {
					p.mX4 = p.mX3;
					p.mY4 = p.mY3;
					p.mX3 = p.mX2;
					p.mY3 = p.mY2;
					p.mX2 = p.mX1;
					p.mY2 = p.mY1;
					p.mX1 = (int)p.mX;
					p.mY1 = (int)p.mY;
					
					p.mX = p.mX + p.mxVelocity * 0.25;
					p.mY = p.mY + p.myVelocity * 0.25;
					
					if(uid == p.uid) {
						xPath[pathLength] = (int)p.mX;
						yPath[pathLength] = (int)p.mY;
						pathLength++;
					}
					
					if(step >= bulletLife - 20) {
						playSound("explosion2.wav");
						
						p.mStatus = 3;	
					}
					
					if((p.mX - middleX) * (p.mX - middleX) + (p.mY - middleY) * (p.mY - middleY) > pRadius * pRadius) {
						System.out.println("out of bounds");
						p.mStatus = 0;
					}
				}
			}
			
			players.setElementAt(p, i);
		}
		
		boolean missilesLeft = false;
		
		for(int i = 0;i < mines.size();i++) {
			me1 = (mineElement)mines.elementAt(i);
			
			if(me1.mRadius >= (26.0*me1.radius)) {
				me1.status = 2;
			}
			
			if(me1.status == 1) {
				// advance explosion
				me1.mRadius+=me1.radius*2.0;
				missilesLeft = true;
			
				// check other mines
				for(int k = 0;k < mines.size();k++) {
					me2 = (mineElement)mines.elementAt(k);
					
					if(me2.status == 0) {
						if((me1.x - me2.x) * (me1.x - me2.x) + (me1.y - me2.y) * (me1.y - me2.y) < (me1.mRadius+me2.radius) * (me1.mRadius+me2.radius)) {
							me2.status = 1;
							me2.hitUid = me1.hitUid;
							me2.hitTeam = me1.hitTeam;
							
							if(me1.hitUid == uid || isController) {
								gbb.clear();
								gbb.bufferByte((byte)k);
								sendMessage(uid, SERVER, 213, gbb.data());
							}
							
							renderPanel.doDetailedRender();
							playSound("explosion3.wav");
						}											
					}
					
					mines.setElementAt(me2, k);
				}
				
				// check players
				for(int k = 0;k < players.size();k++) {
					playerElement q = (playerElement)players.elementAt(k);
					
					if(q.status == 1) {
						if((me1.x - q.x) * (me1.x - q.x) + (me1.y - q.y) * (me1.y - q.y) < (me1.mRadius) * (me1.mRadius)) {//(me1.mRadius) * (me1.mRadius)) {			
							if(q.shield > 0) {
								q.shield--;
							}
							else {
								if(uid == me1.hitUid) {
									if(q.uid == me1.hitUid) {
										mydataHitOwn = true;
									}
									else if(me1.hitTeam > -1 && me1.hitTeam == q.team) {
										mydataHitTeam = true;
									}
									else {
										mydataKills++;
										killedThisShot = true;
										
										mydataMineKill = true;
										
										if(me1.hitWorm) {
											//System.out.println("worm");
											mydataWormKill = true;
										}
									}
								}
							
								if(uid == q.uid) {
									if(!isDead) {
										gbb.clear();
										gbb.bufferInt(me1.hitUid);
										sendMessage(uid, SERVER, 205, gbb.data());
										//System.out.println("die "+me1.hitUid);
										isDead = true;
									}
									mydataHits++;
								}
								else if(isController) {
									if(q.status != 2) {
										gbb.clear();
										gbb.bufferInt(me1.hitUid);
										gbb.bufferInt(q.uid);
										sendMessage(uid, SERVER, 210, gbb.data());
									}	
								}
							
								if(q.status == 1) {
									q.color = new Color(100, 100, 100);
									q.status = 2;
							
									renderPanel.doDetailedRender();
									playSound("explosion.wav");
								}								
							}
						}
					}
					
					players.setElementAt(q, k);
				}
			}
			
			mines.setElementAt(me1, i);
		}
		
		for(int i = 0;i < players.size();i++) {
			p = (playerElement)players.elementAt(i);
			
			if(p.mStatus != 0)
				missilesLeft = true;
			
			if(p.fired && p.mStatus == 1) {
				for(int k = 0;k < players.size();k++) {
					playerElement q = (playerElement)players.elementAt(k);
					
					if(p.mStatus == 1) {
						if(q.shield > 0 && ((p.mX - q.x) * (p.mX - q.x) + (p.mY - q.y) * (p.mY - q.y) < (q.radius) * (q.radius))) {
							// shields
							
							p.mStatus = 0;
							q.shield--;
							
							renderPanel.doDetailedRender();
							playSound("explosion3.wav");
						}
						else if((p.mX - q.x) * (p.mX - q.x) + (p.mY - q.y) * (p.mY - q.y) < q.radius * q.radius) {
							// Stations
							if(p.uid == uid && q.status == 1) {
								if(i != k) {									
									if(p.team != -1) {
										if(p.team != q.team) {
											mydataKills++;
											killedThisShot = true;
											
											if(p.mWorm) {
												//System.out.println("worm");
												mydataWormKill = true;	
											}
										
											if(step > mydataBulletLife) {
												mydataBulletLife = step;
											}
										}
										else {
											mydataHitTeam = true;
										}
									}
									else {
										mydataKills++;
										killedThisShot = true;
										
										if(p.mWorm) {
											//System.out.println("worm");
											mydataWormKill = true;	
										}
										
										if(step > mydataBulletLife) {
											mydataBulletLife = step;
										}
									}
								}
								else {
									mydataHitOwn = true;
								}
							}
							
							if(uid == q.uid) {
								if(!isDead) {
									gbb.clear();
									gbb.bufferInt(p.uid);
									sendMessage(uid, SERVER, 205, gbb.data());
									isDead = true;
								}
								mydataHits++;
							}
							else if(isController) {
								if(q.status != 2) {
									gbb.clear();
									gbb.bufferInt(p.uid);
									gbb.bufferInt(q.uid);
									sendMessage(uid, SERVER, 210, gbb.data());
								}	
							}
							
							if(q.status == 1) {
								p.mStatus = 2;
								q.color = new Color(100, 100, 100);
								q.status = 2;
								
								playSound("explosion.wav");
							}
							else {
								p.mStatus = 3;
								
								playSound("explosion2.wav");
							}
							
							renderPanel.doDetailedRender();
						}
						else if(q.mStatus == 1 && i != k && ((p.mX - q.mX) * (p.mX - q.mX) + (p.mY - q.mY) * (p.mY - q.mY) < q.mRadius * q.mRadius)) {
							// Missiles
							p.mStatus = 3;
							q.mStatus = 3;
							
							if(uid == p.uid || uid == q.uid || isController) {
								gbb.clear();
								gbb.bufferInt(p.uid);
								gbb.bufferInt(q.uid);
								sendMessage(uid, SERVER, 209, gbb.data());							
							}
							
							renderPanel.doDetailedRender();
							playSound("explosion2.wav");
						}
					}
					
					players.setElementAt(q, k);
				}
			}
			
			if(p.mStatus >= 2 && p.mStatus <= 4) { // Calc exploding missle
				if(p.mX > -10 && p.mX < width+10 && p.mY > -10 && p.mY < height+10) {
					int maxRadius = 50;
							
					if(p.mStatus == 2) {
						maxRadius = (int)(4.0*p.radius);
					}
							
					if(p.mExplosion == 0) {
						// Generate debris
						int size = 60+(int)(60.0*Math.random())/(frameSkipTo+1);
								
						p.mxDebP = new double[size];
						p.myDebP = new double[size];
						p.mxDebV = new double[size];
						p.myDebV = new double[size];
								
						for(int l = 0;l < size;l++) {
							p.mxDebP[l] = p.mX;
							p.myDebP[l] = p.mY;
							p.mxDebV[l] = (0-(p.mxVelocity))-(20.0*Math.random())+(40.0*Math.random());
							p.myDebV[l] = (0-(p.myVelocity))-(20.0*Math.random())+(40.0*Math.random());
						}
								
						//addDebugMessage("Init Explosion");
					}
							
					for(int m = 0;m < p.mxDebP.length;m++) {
						p.mxDebP[m] = p.mxDebP[m] + (p.mxDebV[m]*(0.001*p.mExplosion));
						p.myDebP[m] = p.myDebP[m] + (p.myDebV[m]*(0.001*p.mExplosion));
					}
	
					p.mExplosion++;
							
					if(p.mStatus == 2 || p.mStatus == 3) { // Hit player or dead obj
						if(p.mStatus == 3)
							maxRadius = 30;
					}
					else if(p.mStatus == 4) { // planet
						maxRadius = 40;
	
						p.mColor = p.mColor.darker();
					}					
							
					if(p.mExplosion > maxRadius) {
						p.mStatus = 0;
						p.mExplosion = 0;
						p.mxDebP = new double[1];
						p.myDebP = new double[1];
						p.mxDebV = new double[1];
						p.myDebV = new double[1];
					}
				}
				else {
					p.mStatus = 0;
					p.mExplosion = 0;					
				}
			}
			
			players.setElementAt(p, i);
		}
		
		step++;
		
		frameSkip++;
		if(frameSkip >= frameSkipTo) {
			frameSkip = 0;
			renderPanel.repaint();
		}
		
		if(step >= bulletLife || !missilesLeft) {
			if(!isWatching)
				sendEmptyMessage(uid, SERVER, 206);
			else
				sendEmptyMessage(uid, SERVER, 212);
			
			for(int i = 0;i < players.size();i++) {
				((playerElement)players.elementAt(i)).shield = 0;
			}
			
			mydataShield = shieldsAvailable;
			
			isController = false;
			
			gameState = STATE_GAME_WAIT;
			clock.stop();
			renderPanel.repaint();
		}
		}
	};
	
	// STRING CONVERSION 
	
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
	
	// SETTINGS
	
	private void readUserSettings() {
		try {
			BufferedReader in = new BufferedReader(new FileReader(systemDir+systemSeparator+"gwoClientSettings.ini"));
			String data;
			gwoSettings gs = new gwoSettings();
		
			while((data = in.readLine()) != null) {
				st = new StringTokenizer(data, " ");
				data = gs.getSettingsCommand(st);
								
				if(data.equals("use_proxy:")) {
					useProxy = gs.getSettingsValue_B(st);
				}
				else if(data.equals("display_name:")) {
					displayName = convertFromFriendly(gs.getSettingsValue_S(st));
				}
				else if(data.equals("password:")) {
					password = convertFromFriendly(gs.getSettingsValue_S(st));
				}
				else if(data.equals("uid:")) {
					uid = gs.getSettingsValue_I(st);
				}
				else if(data.equals("proxy_server:")) {
					proxyServer = gs.getSettingsValue_S(st);
				}
				else if(data.equals("proxy_port:")) {
					proxyPort = gs.getSettingsValue_I(st);
				}
				else if(data.equals("custom_server_list:")) {
					String server = gs.getSettingsValue_S(st);
					if(server.indexOf(':') == -1)
						serverList.addElement(server+":2000");
					else
						serverList.addElement(server);					
				}
				else if(data.equals("beep_selected:")) {
					beepSelected = gs.getSettingsValue_B(st);
				}
				else if(data.equals("auto_join:")) {
					autoJoin = gs.getSettingsValue_B(st);
				}
				else if(data.equals("play_sound:")) {
					sound = gs.getSettingsValue_B(st);
				}
				else if(data.equals("sound_volume:")) {
					soundVolume = gs.getSettingsValue_I(st);
				}
				else if(data.equals("large_font:")) {
					largeFont = gs.getSettingsValue_B(st);
				}
				else if(data.equals("background_antialias:")) {
					backgroundAntiAlias = gs.getSettingsValue_B(st);
				}
				else if(data.equals("foreground_antialias:")) {
					foregroundAntiAlias = gs.getSettingsValue_B(st);
				}
				else if(data.equals("render_for_quality:")) {
					renderForQuality = gs.getSettingsValue_B(st);
				}
				else if(data.equals("reroute:")) {
					reroute = gs.getSettingsValue_B(st);
				}
				else if(data.equals("frame_skip:")) {
					frameSkipTo = gs.getSettingsValue_I(st);
				}
				else if(data.equals("remote_server:")) {
					remoteServer = gs.getSettingsValue_S(st);
				}
				else if(data.equals("message_text:")) {
					msgText = gs.getSettingsValue_S(st);
				}
				else if(data.equals("angle:")) {
					savedAngle = gs.getSettingsValue_I(st);
				}
				else if(data.equals("power:")) {
					savedPower = gs.getSettingsValue_I(st);
				}
				else if(data.equals("remote_port:")) {
					remotePort = gs.getSettingsValue_I(st);
				}
				else if(data.equals("fps:")) {
					fps = gs.getSettingsValue_I(st);
					
					if(fps < 1)
						fps = 1;
						
					if(fps > 200)
						fps = 200;
				}
				else if(data.equals("clock_speed:")) {
					// deprecated
					fps = (int)(1000.0/(double)gs.getSettingsValue_I(st));
					
					if(fps < 1)
						fps = 1;
						
					if(fps > 200)
						fps = 200;
				}
			}

		}
		catch(FileNotFoundException fnfe) {}
		catch(IOException ioe) {}
	}
	
	private void writeUserSettings() {	
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(systemDir+systemSeparator+"gwoClientSettings.ini"));
			
			out.write("use_proxy: " + useProxy + "\n");
			out.write("display_name: " + convertToFriendly(displayName) + "\n");
			out.write("password: " + convertToFriendly(password) + "\n");
			out.write("uid: " + tempUid + "\n");
			out.write("proxy_server: " + proxyServer + "\n");
			out.write("proxy_port: " + proxyPort + "\n");
			out.write("beep_selected: " + beep.isSelected() + "\n");
			out.write("auto_join: " + autoJoinBox.isSelected() + "\n");
			out.write("play_sound: " + sound + "\n");
			out.write("sound_volume: " + soundVolume + "\n");
			out.write("large_font: " + largeFont + "\n");
			out.write("background_antialias: " + backgroundAntiAlias + "\n");
			out.write("foreground_antialias: " + foregroundAntiAlias + "\n");
			out.write("render_for_quality: " + renderForQuality + "\n");
			out.write("frame_skip: " + frameSkipTo + "\n");
			out.write("fps: " + fps + "\n");
			out.write("message_text: " +  convertToFriendly(text.getText()) + "\n");
			out.write("angle: " + angle.getValue() + "\n");
			out.write("power: " + power.getValue() + "\n");
			
			if(reroute) {
				out.write("reroute: " + reroute + "\n");
				out.write("remote_server: " + remoteServer + "\n");
				out.write("remote_port: " + remotePort + "\n");
			}
			
			for(int x = 0;x < serverList.size();x++) {
				out.write("custom_server_list: " + (String)serverList.elementAt(x) + "\n");
			}
			out.close();
		}
		catch(FileNotFoundException fnfe) {}
		catch(IOException ioe) {}
	}
	
	// GwoNET DATA LINK
	
	private void postPassiveData(String data) {
		dataToPost.addElement(data);
	}
	
	private String postToGwoNet(String data) {
		Socket s2;
		DataInputStream dis2;
		DataOutputStream dos2;
		String dataOut;
		String result = "";
		
		try {
			if(useProxy) {
				s2 = new Socket(proxyServer, proxyPort);
			}
			else {
				s2 = new Socket("www.dangeross.com", 80);
			}
				
			dis2 = new DataInputStream(s2.getInputStream());
			dos2 = new DataOutputStream(s2.getOutputStream());
			dos2.writeBytes("POST http://www.dangeross.com/gwo/gwonet.php HTTP/1.0\r\n"+
				"Content-Type: application/x-www-form-urlencoded\r\n"+
				"Content-Length: "+data.length()+"\r\n\r\n"+data);
					
			while((dataOut = dis2.readLine()) != null) {
				result+=dataOut+"\r\n";
			}
			
			//addDebugMessage(result);
			
			if(result.indexOf("\r\n\r\n") != -1) {
				int start = result.indexOf("\r\n\r\n")+4;
				int end = result.length()-2;
				
				if(start < end) {
					//addDebugMessage("GwoNET Result: "+result.substring(result.indexOf("\r\n\r\n")+4, result.length()-2));
					return result.substring(result.indexOf("\r\n\r\n")+4, result.length()-2);
				}
				else {
					//addDebugMessage("GwoNET Result: ");
					return "";	
				}
			}
		}
		catch(IOException ioe) {
			new gwoDebugWindow(ioe.toString());
		}
		
		return "";
	}
	
	public void gwoNetServerList() {
		postPassiveData("cmdId=302&version="+version);
	}
	
	public void gwoNetRegister(String name, String uid, String pass) {
		postPassiveData("cmdId=301&uid="+uid+"&name="+name+"&pass="+pass);
	}
	
	public void gwoNetSearch(String name) {
		postPassiveData("cmdId=303&name="+name);
	}
}
