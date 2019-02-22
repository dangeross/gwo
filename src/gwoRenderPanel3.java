import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.awt.event.*;
import javax.swing.*;

class gwoRenderPanel3 extends JPanel {
	private gwoClient gc;
	
	private BufferedImage bufferA, bufferB;
	private Graphics2D bufferGa, bufferGb, backBufferG;
	private BufferedImage backBuffer;
	private boolean buffersInit = false;
	private Font messageFont;
	public FontMetrics messageFontM; 
	
	private int c = 0;
	private long sT;
	private boolean detailedRenderReq = false;
	
	int[] xSet = new int[1000];
	int[] ySet = new int[1000];
	
	int x=0, y=0;
	
	public gwoRenderPanel3(gwoClient gc) {
		this.gc = gc;
		
		setMinimumSize(new Dimension(893, 666));
		setPreferredSize(new Dimension(893, 666));
		
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				handleMouseClickEvent(e.getX(), e.getY());
			}
		});	
		
		addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				handleMouseMoveEvent(e.getX(), e.getY());
			}
		});	
	}
	
	public void close() {
		backBuffer.flush();
		bufferA.flush();
		bufferB.flush();
	}
	
	private void handleMouseClickEvent(int x, int y) {
		if(gc.gameState <= gwoClient.STATE_JOIN_GAME) {
			if(y > 597) {
				if(x < gc.width/2) {
					gc.handleMouseClickEvent(gc.PAGE_DOWN);
				}
				else {
					gc.handleMouseClickEvent(gc.PAGE_UP);
				}
			}
			else {
				gc.handleMouseClickEvent(x, y);
			}
		}
		else {
			gc.handleMouseClickEvent(x, y);
		}
	}
	
	private void handleMouseMoveEvent(int x, int y) {
		if(gc.gameState == gwoClient.STATE_SERVER_LIST) {
			this.x = x;
			this.y = y;
			repaint();
		}	
	}
	
	public int nInt(double d) {
		if((d - (int)d) > 0.5)
			return (int)d+1;
		else
			return (int)d;
	}
	
	public void initBuffers() {
		//System.out.println("init");
		while(bufferGa == null) {
			bufferGa = (Graphics2D)getGraphics();
			bufferA = bufferGa.getDeviceConfiguration().createCompatibleImage(gc.width, gc.height);
			bufferGa = bufferA.createGraphics();
		}
		while(bufferGb == null) {
			bufferGb = (Graphics2D)getGraphics();
			bufferB = bufferGb.getDeviceConfiguration().createCompatibleImage(gc.width, gc.height);
			bufferGb = bufferB.createGraphics();
		}
		while(backBufferG == null) {
			backBufferG = (Graphics2D)getGraphics();
			backBuffer = backBufferG.getDeviceConfiguration().createCompatibleImage(gc.width, gc.height);
			backBufferG = backBuffer.createGraphics();
		}

		if(gc.backgroundAntiAlias)
			bufferGa.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		else
			bufferGa.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		if(gc.foregroundAntiAlias) {
			bufferGb.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			backBufferG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		else {
			bufferGb.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			backBufferG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}
		
		if(gc.renderForQuality) {
			bufferGa.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			bufferGb.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			backBufferG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		}
		else {
			bufferGa.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			bufferGb.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			backBufferG.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		}
		
		if(gc.largeFont) {
			messageFont = new Font("Dialog", Font.BOLD, 12);
			messageFontM = gc.tools.getFontMetrics(messageFont); 
		}
		else {
			messageFont = new Font("Dialog", Font.BOLD, 10);
			messageFontM = gc.tools.getFontMetrics(messageFont); 
		}
		
		backBufferG.setFont(messageFont);

		buffersInit = true;
	}
	
	public void doDetailedRender() {
		System.out.println("Render "+(++c));
		detailedRenderReq = true;	
	}
	
	public void paint(Graphics g) {
		update(g);
	}
	
	public void update(Graphics graphics) {
		Graphics2D g = (Graphics2D)graphics;
		g.setFont(messageFont);
		
		if(gc.foregroundAntiAlias)
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		else
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		
		if(gc.renderForQuality)
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		else 
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
		
		if(!buffersInit) {
			initBuffers();		
		}
		
		sT = System.currentTimeMillis();
		
		switch(gc.gameState) {
			case gwoClient.STATE_PRE_LOGIN:
				pre_login(g);
				break;
			case gwoClient.STATE_SERVER_LIST:
				server_list(g);
				break;
			case gwoClient.STATE_POST_LOGIN:
				post_login(g);
				break;
			case gwoClient.STATE_NO_PLAYER:
				join_game(g);
				break;
			case gwoClient.STATE_JOIN_GAME:
				join_game(g);
				break;
			case gwoClient.STATE_GAME_AIM:
				if(gc.isFirstGo)
					render_background();
				
				aim(g);
				break;
			case gwoClient.STATE_GAME_FIRE:
				fire(g);
				break;
			case gwoClient.STATE_GAME_HYPER:
				hyper(g);
				break;
			case gwoClient.STATE_GAME_WAIT:
				wait_for_end(g);
				break;
			case gwoClient.STATE_GAME_END:
				end(g);
				break;
		}
		
		long eT = System.currentTimeMillis();

		g.setColor(Color.white);
		g.drawString("cStep: "+(gc.step), gc.width-80, 45);
		g.drawString("rTime: "+(eT-sT), gc.width-80, 60);
		g.drawString("state: "+(gc.gameState), gc.width-80, 75);
		
		if(gc.uberUser) {
			//g.drawString(gc.debugText1, 3, 90);
			//g.drawString(gc.debugText2, 3, 105);
			//g.drawString(gc.debugText3, 3, 120);
			//g.drawString(gc.debugText4, 3, 135);
			//g.drawString(gc.debugText5, 3, 150);
		}
	}
	
	private void pre_login(Graphics2D g) {
		backBufferG.setColor(Color.black);
		backBufferG.fillRect(0, 0, gc.width, gc.height);
				
		g.drawImage(backBuffer, 0, 0, this);
	}
	
	private void server_list(Graphics2D g) {
		if(gc.back2.getWidth(this) == -1 || gc.back2.getHeight(this) == -1) {
			backBufferG.setColor(Color.black);
			backBufferG.fillRect(0, 0, gc.width, gc.height);
		}
		else {
			backBufferG.drawImage(gc.back2, 0, 0, this);
		}
			
		// Version
		backBufferG.setColor(Color.white);
		backBufferG.drawString("."+gc.version, 815, 130);
		
		// Info Msg
		backBufferG.setColor(Color.white);
		backBufferG.drawString(gc.infoMessage, gc.width / 2 - messageFontM.stringWidth(gc.infoMessage) / 2, 153);
		
		backBufferG.drawString("Title", 100, 200);
		backBufferG.drawString("Version", 400, 200);
		backBufferG.drawString("Address", 450, 200);
		backBufferG.drawString("Playing", 720, 200);
		backBufferG.drawString("Max Players", 770, 200);
		
		// Find highlight
		int highlight = y-200;
		highlight = (int)((double)highlight/15.0);
		
		if(highlight > 25)
			highlight = -1;

		int numOfElements = 0;
		if(gc.leaguePage*25 < gc.servers.size()) {
			numOfElements = 25;
		}
		else {
			numOfElements = gc.servers.size()-((gc.leaguePage-1)*25);
		}
		
		// Draw Servers
		serverElement se;
		int element;
		for(int i = 0;i < numOfElements;i++) {
			se = (serverElement)gc.servers.elementAt(((gc.leaguePage-1)*25)+i);
				
			if(highlight >= 0 && highlight == i)
				backBufferG.setColor(Color.white);
			else if(se.custom)
				backBufferG.setColor(Color.orange);
			else if(se.players == 0)
				backBufferG.setColor(Color.yellow);
			else if(se.players != se.maxPlayers)
				backBufferG.setColor(Color.green);
			else
				backBufferG.setColor(Color.red);
					
			backBufferG.drawString("" + (((gc.leaguePage-1)*25)+i+1), 60, 215+(15*i));
			backBufferG.drawString("" + se.title, 100, 215+(15*i));
			backBufferG.drawString("" + se.address, 450, 215+(15*i));
			
			if(se.custom) {
				backBufferG.drawString("2.0.---", 400, 215+(15*i));
				backBufferG.drawString("--", 720, 215+(15*i));
				backBufferG.drawString("--", 770, 215+(15*i));
			}
			else {
				backBufferG.drawString("2.0." + se.version, 400, 215+(15*i));
				backBufferG.drawString("" + se.players, 720, 215+(15*i));
				
				if(se.maxPlayers == 0)
					backBufferG.drawString("--", 770, 215+(15*i));
				else
					backBufferG.drawString("" + se.maxPlayers, 770, 215+(15*i));
			}
		}
		
		// Page
		backBufferG.setColor(Color.white);
		backBufferG.drawString("Page "+gc.leaguePage+" of "+gc.leagueNumOfPages, gc.width / 2 - messageFontM.stringWidth("Page "+gc.leaguePage+" of "+gc.leagueNumOfPages) / 2, 215+(15*26));		
			
		if(gc.leaguePage > 1)
			backBufferG.drawImage(gc.pageDown, (gc.width / 2 - messageFontM.stringWidth("Page "+gc.leaguePage+" of "+gc.leagueNumOfPages) / 2) - 12, 215+(15*26)-8, this);
		
		if(gc.leaguePage < gc.leagueNumOfPages)
			backBufferG.drawImage(gc.pageUp, (gc.width / 2 + messageFontM.stringWidth("Page "+gc.leaguePage+" of "+gc.leagueNumOfPages) / 2) + 3, 215+(15*26)-8, this);
				
		g.drawImage(backBuffer, 0, 0, this);
	}
	
	private void post_login(Graphics2D g) {
		if(gc.back1.getWidth(this) == -1 || gc.back1.getHeight(this) == -1) {
			backBufferG.setColor(Color.black);
			backBufferG.fillRect(0, 0, gc.width, gc.height);
		}
		else {
			backBufferG.drawImage(gc.back1, 0, 0, this);
		}
			
		if(gc.gwoNetServer)
			backBufferG.drawImage(gc.gwoNet, 570, 110, this);	
			
		// Version
		backBufferG.setColor(Color.white);
		backBufferG.drawString("."+gc.version, 815, 130);
		
		// Info Msg
		backBufferG.setColor(Color.white);
		backBufferG.drawString(gc.infoMessage, gc.width / 2 - messageFontM.stringWidth(gc.infoMessage) / 2, 153);
		
		// Msgs
		backBufferG.setColor(gc.msgColor1);
		backBufferG.drawString(gc.msgText1, 3, messageFontM.getHeight()*1);
		backBufferG.setColor(gc.msgColor2);
		backBufferG.drawString(gc.msgText2, 3, messageFontM.getHeight()*2);
		backBufferG.setColor(gc.msgColor3);		
		backBufferG.drawString(gc.msgText3, 3, messageFontM.getHeight()*3);	
				
		g.drawImage(backBuffer, 0, 0, this);
	}
	
	private void join_game(Graphics2D g) {
		leagueElement ue;
		
		//bufferGa.setColor(Color.black);
		//bufferGa.fillRect(0, 0, gc.width, gc.height);

		if(gc.back2.getWidth(this) == -1 || gc.back2.getHeight(this) == -1) {
			backBufferG.setColor(Color.black);
			backBufferG.fillRect(0, 0, gc.width, gc.height);
		}
		else {
			backBufferG.drawImage(gc.back2, 0, 0, this);
		}
			
		if(gc.gwoNetServer)
			backBufferG.drawImage(gc.gwoNet, 570, 110, this);	
		
		backBufferG.setColor(Color.white);
		backBufferG.drawString("."+gc.version, 815, 130);
			
		backBufferG.drawString("Rank", 100, 200);
		backBufferG.drawString("Player", 140, 200);
		backBufferG.drawString("Played", 375, 200);
		backBufferG.drawString("Won", 450, 200);
		backBufferG.drawString("Drawn", 500, 200);
		backBufferG.drawString("Lost", 550, 200);
		backBufferG.drawString("Kills", 625, 200);
		backBufferG.drawString("Hits", 675, 200);
		backBufferG.drawString("Points", 750, 200);
			
		// Draw League
		for(int i = 0;i < 25 && i < gc.league.size();i++) {
			ue = (leagueElement)gc.league.elementAt(i);
				
			if(!ue.online)
				backBufferG.setColor(Color.red);
			else if(ue.joined)
				backBufferG.setColor(Color.green);
			else
				backBufferG.setColor(Color.white);
					
			backBufferG.drawString("" + ue.position, 100, 215+(15*i));
			backBufferG.drawString("" + ue.displayName, 140, 215+(15*i));
			backBufferG.drawString("" + ue.played, 375, 215+(15*i));
			backBufferG.drawString("" + ue.won, 450, 215+(15*i));
			backBufferG.drawString("" + ue.drawn, 500, 215+(15*i));
			backBufferG.drawString("" + ue.lost, 550, 215+(15*i));
			backBufferG.drawString("" + ue.killsFor, 625, 215+(15*i));
			backBufferG.drawString("" + ue.killsAgainst, 675, 215+(15*i));
			backBufferG.drawString("" + ue.points, 750, 215+(15*i));
		}
			
		backBufferG.setColor(Color.white);
		backBufferG.drawString("Page "+gc.leaguePage+" of "+gc.leagueNumOfPages, gc.width / 2 - messageFontM.stringWidth("Page "+gc.leaguePage+" of "+gc.leagueNumOfPages) / 2, 215+(15*26));		
			
		if(gc.leaguePage > 1)
			backBufferG.drawImage(gc.pageDown, (gc.width / 2 - messageFontM.stringWidth("Page "+gc.leaguePage+" of "+gc.leagueNumOfPages) / 2) - 12, 215+(15*26)-8, this);
		
		if(gc.leaguePage < gc.leagueNumOfPages)
			backBufferG.drawImage(gc.pageUp, (gc.width / 2 + messageFontM.stringWidth("Page "+gc.leaguePage+" of "+gc.leagueNumOfPages) / 2) + 3, 215+(15*26)-8, this);
		
		// Info Msg
		backBufferG.setColor(Color.white);
		backBufferG.drawString(gc.infoMessage, gc.width / 2 - messageFontM.stringWidth(gc.infoMessage) / 2, 153);
		
		// Msgs
		backBufferG.setColor(gc.msgColor1);
		backBufferG.drawString(gc.msgText1, 3, messageFontM.getHeight()*1);
		backBufferG.setColor(gc.msgColor2);
		backBufferG.drawString(gc.msgText2, 3, messageFontM.getHeight()*2);
		backBufferG.setColor(gc.msgColor3);		
		backBufferG.drawString(gc.msgText3, 3, messageFontM.getHeight()*3);	
				
		g.drawImage(backBuffer, 0, 0, this);
	}
	
	private void render_background() {
		starElement s;
		double d1, d2;
		planetElement p;
		mineElement me;
		
		gc.isFirstGo = false;
		bufferGa.setColor(Color.black);
		bufferGa.fillRect(0, 0, gc.width, gc.height);
		
		// Draw stars
		for(int i = 0;i < gc.starfield.size();i++) {
			s = (starElement)gc.starfield.elementAt(i);
			
			bufferGa.setColor(s.color.brighter());
			bufferGa.fillOval(s.x, s.y, s.radius, s.radius);
		}
		
		// Draw planets
		for(int i = 0;i < gc.planets.size();i++) {
			p = (planetElement)gc.planets.elementAt(i);
					
			if(p.shading == 2) {					
				for(int k = 0;k < 1000;k++) {
       					d1 = 6.283185307179586 * k / 1000;
 					d2 = p.radius + 1.5 * Math.sqrt(p.radius * 1.2) + 1.5 * Math.sqrt(p.radius * 0.9 * Math.random());
					xSet[k] = nInt(d2 * Math.cos(d1) + p.x);
					ySet[k] = nInt(d2 * Math.sin(d1) + p.y);
				}
				bufferGa.setColor(p.color.darker().darker().darker().darker().darker().darker());
				bufferGa.fillPolygon(xSet, ySet, 1000);
			}
		}
		for(int i = 0;i < gc.planets.size();i++) {
			p = (planetElement)gc.planets.elementAt(i);
					
			if(p.shading == 2) {					
				for(int k = 0;k < 1000;k++) {
       					d1 = 6.283185307179586 * k / 1000;
 					d2 = p.radius + 1.5 * Math.sqrt(p.radius * 0.7) + 1.5 * Math.sqrt(p.radius * 0.8 * Math.random());
					xSet[k] = nInt(d2 * Math.cos(d1) + p.x);
					ySet[k] = nInt(d2 * Math.sin(d1) + p.y);
				}
				bufferGa.setColor(p.color.darker().darker().darker().darker().darker());
				bufferGa.fillPolygon(xSet, ySet, 1000);
			}
		}
		for(int i = 0;i < gc.planets.size();i++) {
			p = (planetElement)gc.planets.elementAt(i);
					
			if(p.shading == 2) {					
				for(int k = 0;k < 1000;k++) {
					d1 = 6.283185307179586 * k / 1000;
					d2 = p.radius + 1.5 * Math.sqrt(p.radius * 0.22) + 1.5 * Math.sqrt(p.radius * 0.6 * Math.random());
					xSet[k] = nInt(d2 * Math.cos(d1) + p.x);
					ySet[k] = nInt(d2 * Math.sin(d1) + p.y);
				}
				bufferGa.setColor(p.color.darker().darker().darker().darker());
				bufferGa.fillPolygon(xSet, ySet, 1000);
			}
		}
		for(int i = 0;i < gc.planets.size();i++) {
			p = (planetElement)gc.planets.elementAt(i);
					
			if(p.shading == 2) {					
				for(int k = 0;k < 1000;k++) {
					d1 = 6.283185307179586 * k / 1000;
					d2 = p.radius - Math.sqrt(p.radius * 0.02) + 1.5 * Math.sqrt(p.radius * 0.7 * Math.random());
					xSet[k] = nInt(d2 * Math.cos(d1) + p.x);
					ySet[k] = nInt(d2 * Math.sin(d1) + p.y);
				}
				bufferGa.setColor(p.color.darker().darker().darker());
				bufferGa.fillPolygon(xSet, ySet, 1000);
 			}
		}	
		for(int i = 0;i < gc.planets.size();i++) {
			p = (planetElement)gc.planets.elementAt(i);
					
			if(p.shading == 3) {
				bufferGa.setColor(p.color.darker().darker().darker().darker().darker());
				bufferGa.fillOval(nInt((p.x) - (p.radius * 2.5)), nInt((p.y) - (p.radius * 2.5)), nInt(p.radius * 5.0), nInt(p.radius * 5.0));
 			}
		}
		for(int i = 0;i < gc.planets.size();i++) {
			p = (planetElement)gc.planets.elementAt(i);
					
			if(p.shading == 3) {
				bufferGa.setColor(p.color.darker().darker().darker().darker());
				bufferGa.fillOval(nInt((p.x) - (p.radius * 2.2)), nInt((p.y) - (p.radius * 2.2)), nInt(p.radius * 4.4), nInt(p.radius * 4.4));
 			}
		}
		for(int i = 0;i < gc.planets.size();i++) {
			p = (planetElement)gc.planets.elementAt(i);
					
			if(p.shading == 3) {
				bufferGa.setColor(p.color.darker().darker().darker());
				bufferGa.fillOval(nInt((p.x) - (p.radius * 2.0)), nInt((p.y) - (p.radius * 2.0)), nInt(p.radius * 4.0), nInt(p.radius * 4.0));
 			}
		}
		for(int i = 0;i < gc.planets.size();i++) {
			p = (planetElement)gc.planets.elementAt(i);
					
			if(p.shading == 3) {
				bufferGa.setColor(p.color.darker().darker());
				bufferGa.fillOval(nInt((p.x) - (p.radius * 0.5 + p.radius)), nInt((p.y) - (p.radius * 0.5 + p.radius)), nInt((p.radius * 0.5 + p.radius) * 2.0), nInt((p.radius * 0.5 + p.radius) * 2.0));
 			}
		}
		for(int i = 0;i < gc.planets.size();i++) {
			p = (planetElement)gc.planets.elementAt(i);
					
			if(p.shading == 3) {
				bufferGa.setColor(p.color.darker());
				bufferGa.fillOval(nInt((p.x) - (p.radius * 0.2 + p.radius)), nInt((p.y) - (p.radius * 0.2 + p.radius)), nInt(2.0 * (p.radius * 0.2 + p.radius)), nInt(2.0 * (p.radius * 0.2 + p.radius)));
				bufferGa.setColor(p.color);
				bufferGa.fillOval(nInt((p.x) - (p.radius * 0.1 + p.radius)), nInt((p.y) - (p.radius * 0.1 + p.radius)), nInt(2.0 * (p.radius * 0.1 + p.radius)), nInt(2.0 * (p.radius * 0.1 + p.radius)));
 			}
		}
		for(int i = 0;i < gc.planets.size();i++) {
			p = (planetElement)gc.planets.elementAt(i);
					
			if(p.shading == 1) {
				bufferGa.setColor(p.color);
				bufferGa.fillOval(nInt(p.x - p.radius), nInt(p.y - p.radius), nInt(p.radius * 2.0), nInt(p.radius * 2.0));
				bufferGa.setColor(p.color.darker());
				bufferGa.fillArc(nInt(p.x - p.radius), nInt(p.y - p.radius), nInt(p.radius * 2.0)-2, nInt(p.radius * 2.0), 90, 180);
				bufferGa.setColor(p.color);
				bufferGa.fillArc(nInt(p.x - (p.radius*0.5)), nInt(p.y - p.radius), nInt(p.radius)+1, nInt(p.radius * 2.0), 90, 180);			
			}
		}
		for(int i = 0;i < gc.planets.size();i++) {
			p = (planetElement)gc.planets.elementAt(i);
					
			if(p.shading == 2) {
				bufferGa.setColor(p.color.darker());
				bufferGa.fillOval(nInt((p.x) - p.radius), nInt((p.y) - p.radius), nInt(p.radius * 2.0), nInt(p.radius * 2.0));
				bufferGa.setColor(p.color);
				bufferGa.fillOval(nInt((p.x) - p.radius + 3.5), nInt((p.y) - p.radius + 3.5), nInt(p.radius * 2.0 - 2.0 * 3.5), nInt(p.radius * 2.0 - 2.0 * 3.5));
				bufferGa.setColor(p.color.brighter());
				bufferGa.fillOval(nInt((p.x) - p.radius + 10.0), nInt((p.y) - p.radius + 10.0), nInt(p.radius * 2.0 - 2.0 * 10.0), nInt(p.radius * 2.0 - 2.0 * 10.0));
			}
		}
		for(int i = 0;i < gc.planets.size();i++) {
			p = (planetElement)gc.planets.elementAt(i);
					
			if(p.shading == 3) {
				bufferGa.setColor(p.color.darker());
				bufferGa.fillOval(nInt((p.x) - p.radius), nInt((p.y) - p.radius), nInt(p.radius * 2.0), nInt(p.radius * 2.0));
				bufferGa.setColor(p.color.darker().darker());
				bufferGa.fillOval(nInt((p.x) - p.radius + 1.0), nInt((p.y) - p.radius + 1.0), nInt(p.radius * 2.0 - 2.0), nInt(p.radius * 2.0 - 2.0));
				bufferGa.setColor(Color.black);
				bufferGa.fillOval(nInt((p.x) - p.radius + 2.0), nInt((p.y) - p.radius + 2.0), nInt(p.radius * 2.0 - 4.0), nInt(p.radius * 2.0 - 4.0));
			}
		}
				
		// Draw Mines
		/*for(int i = 0;i < gc.mines.size();i++) {
			me = (mineElement)gc.mines.elementAt(i);
					
			bufferGa.setColor(new Color(100, 100, 100));
			bufferGa.fillRect(nInt(me.x-me.radius-1.0), nInt(me.y-(me.radius/2.0)), nInt((me.radius+1.0)*2.0), nInt(me.radius));
			bufferGa.fillRect(nInt(me.x-(me.radius/2.0)), nInt(me.y-me.radius-1.0), nInt(me.radius), nInt((me.radius+1.0)*2.0));
			bufferGa.setColor(new Color(150, 150, 150));
			bufferGa.fillOval(nInt(me.x-me.radius), nInt(me.y-me.radius), nInt(me.radius*2.0), nInt(me.radius*2.0));
		}*/
		
		bufferGb.drawImage(bufferA, 0, 0, this);		
	}
	
	private void aim(Graphics2D g) {
		playerElement pl;
		mineElement me;
		
		g.drawImage(bufferB, 0, 0, this);
		
		g.setColor(new Color(0, 0, 0, 50));
		
		if(gc.shotDone || gc.isDead || gc.isWatching)
			g.fillRect(0, gc.height - 66, gc.width+10, 66);
		else
			g.fillRect(0, gc.height - 92, gc.width+10, 90);
		
		for(int i = 0;i < gc.players.size();i++) {
			pl = (playerElement)gc.players.elementAt(i);
			
			// Draw player names
			if(gc.gameScenario != 16) {
				pl = (playerElement)gc.players.elementAt(i);
					
				if(gc.uid == pl.uid && !gc.isDead && !gc.shotDone) {
					g.setColor(pl.tColor.darker().darker().darker());
				}
				else {
					g.setColor(pl.tColor.darker());
				}
					
				g.drawString(pl.displayName, nInt(pl.x), nInt(pl.y + pl.radius + 10.0));
			}
			
			if(gc.uid == pl.uid) {
				// Draw my shield
				if(pl.shield > 0) {
					g.setColor(new Color(2, 187, 204));
					g.drawOval(nInt(pl.x - pl.radius-2.0), nInt(pl.y - pl.radius-2.0), nInt(2.0 *(pl.radius+1.0))+1, nInt(2.0 *(pl.radius+1.0))+1);
				}
			
				// Draw aim stuff
				if(!gc.shotDone && !gc.isDead) {
					if(gc.gameScenario != 0 && gc.gameScenario != 18) {
						g.setColor(pl.color.darker());
						g.drawPolyline(gc.xPath, gc.yPath, gc.pathLength);
					}
					int power = gc.power.getValue(), angle = gc.angle.getValue();
					int a = 180-angle;
					if(a < 1)
						a = 360 + a;
				
					g.setColor(Color.white);
					g.drawOval(nInt(pl.x - 3.0 * pl.radius), nInt(pl.y - 3.0 * pl.radius), nInt(6.0 * pl.radius), nInt(6.0 * pl.radius));
					g.drawLine(nInt(pl.x), nInt(pl.y), nInt(pl.x + (1.05 * pl.radius + 1.9 * pl.radius * power / 1000.0) * Math.sin((double)angle / 180.0 * 3.141592653589793)), nInt(pl.y + (1.05 * pl.radius + 1.9 * pl.radius * power / 1000.0) * Math.cos((double)angle / 180.0 * 3.141592653589793)));
				
					g.drawString("Angle : " + a, 10, gc.height - 80);				
					//backBufferG.drawString("Power : " + nInt((power) / 800.0 * 100.0) + "%", ((gc.width / 5) * 4), gc.height - 80);
					String p = ""+((power) / 800.0 * 100.0);
					g.drawString("Power : " + p.substring(0, p.indexOf(".")+2) + "%", ((gc.width / 5) * 4), gc.height - 80);
				}
			}
			
			//if(gc.drawPlayers) {
				// Draw Players
				g.setColor(pl.color);
				g.fillOval(nInt(pl.x - pl.radius), nInt(pl.y - pl.radius), nInt(pl.radius * 2.0), nInt(pl.radius * 2.0));
				g.setColor(pl.color.darker());
				g.fillArc(nInt(pl.x - pl.radius), nInt(pl.y - pl.radius), nInt(pl.radius * 2.0)-1, nInt(pl.radius * 2.0), 90, 180);
				g.setColor(pl.color);
				g.fillArc(nInt(pl.x - pl.radius/2.0)+1, nInt(pl.y - pl.radius), nInt(pl.radius), nInt(pl.radius * 2.0), 90, 180);
				g.setColor(pl.color.darker());
				g.drawArc(nInt((pl.x - pl.radius) + 1.0), nInt(pl.y - 1.0), nInt(2.0 * pl.radius - 2.0), nInt(pl.radius / 3.0), 0, -180);
				g.fillOval(nInt(pl.x - (pl.radius / 10.0)), nInt(pl.y - (pl.radius * 0.8)), nInt(pl.radius * 0.8), nInt(pl.radius * 0.8));
				g.setColor(pl.color.darker().darker());
				g.fillArc(nInt(pl.x - (pl.radius / 10.0)), nInt(pl.y - (pl.radius * 0.8)), nInt(pl.radius * 0.8), nInt(pl.radius * 0.8), 270, 180);
				g.setColor(pl.color.darker());
				g.fillArc(nInt(pl.x - (pl.radius / 10.0) + (pl.radius * 0.15)), nInt(pl.y - (pl.radius * 0.8)), nInt(pl.radius * 0.4), nInt(pl.radius * 0.8), 270, 180);
			//}
		}
				
		//if(gc.drawMines) {
			// Draw Mines
			for(int i = 0;i < gc.mines.size();i++) {
				me = (mineElement)gc.mines.elementAt(i);
					
				if(me.status == 0) {
					g.setColor(new Color(100, 100, 100));
					g.fillRect(nInt(me.x-me.radius-1.0), nInt(me.y-(me.radius/2.0)), nInt((me.radius+1.0)*2.0), nInt(me.radius));
					g.fillRect(nInt(me.x-(me.radius/2.0)), nInt(me.y-me.radius-1.0), nInt(me.radius), nInt((me.radius+1.0)*2.0));
					g.setColor(new Color(150, 150, 150));
					g.fillOval(nInt(me.x-me.radius), nInt(me.y-me.radius), nInt(me.radius*2.0), nInt(me.radius*2.0));
					g.setColor(Color.red);
					g.fillOval(nInt(me.x-(me.radius/3.0)), nInt(me.y-(me.radius/3.0)), nInt(me.radius/1.5), nInt(me.radius/1.5));						
				}
			}
		//}
		
		// Msgs
		if(gc.autoMessage.length() > 0) {
			g.setColor(new Color(0, 0, 0, 50));
			g.fillRect(gc.width-messageFontM.stringWidth(gc.autoMessage)-10, 0, messageFontM.stringWidth(gc.autoMessage)+10, messageFontM.getHeight()+3);
			g.setColor(Color.white);
			g.drawString(gc.autoMessage, gc.width-messageFontM.stringWidth(gc.autoMessage)-7, messageFontM.getHeight());
		}
		
		if(gc.msgText1.length() > 0) {
			g.setColor(new Color(0, 0, 0, 50));
			g.fillRect(0, (messageFontM.getHeight()*0)+3, messageFontM.stringWidth(gc.msgText1)+10, messageFontM.getHeight());
			g.setColor(gc.msgColor1);
			g.drawString(gc.msgText1, 3, messageFontM.getHeight()*1);
		}
		
		if(gc.msgText2.length() > 0) {
			g.setColor(new Color(0, 0, 0, 50));
			g.fillRect(0, (messageFontM.getHeight()*1)+3, messageFontM.stringWidth(gc.msgText2)+10, messageFontM.getHeight());
			g.setColor(gc.msgColor2);
			g.drawString(gc.msgText2, 3, messageFontM.getHeight()*2);
		}
		
		if(gc.msgText3.length() > 0) {
			g.setColor(new Color(0, 0, 0, 50));
			g.fillRect(0, (messageFontM.getHeight()*2)+3, messageFontM.stringWidth(gc.msgText3)+10, messageFontM.getHeight());
			g.setColor(gc.msgColor3);
			g.drawString(gc.msgText3, 3, messageFontM.getHeight()*3);
		}
		
		if(gc.msgText4.length() > 0) {
			g.setColor(new Color(0, 0, 0, 50));
			g.fillRect(0, (messageFontM.getHeight()*3)+3, messageFontM.stringWidth(gc.msgText4)+10, messageFontM.getHeight());
			g.setColor(gc.msgColor4);
			g.drawString(gc.msgText4, 3, messageFontM.getHeight()*4);
		}
		
		if(gc.msgText5.length() > 0) {
			g.setColor(new Color(0, 0, 0, 50));
			g.fillRect(0, (messageFontM.getHeight()*4)+3, messageFontM.stringWidth(gc.msgText5)+10, messageFontM.getHeight());
			g.setColor(gc.msgColor5);
			g.drawString(gc.msgText5, 3, messageFontM.getHeight()*5);
		}
		
		if(gc.showSeconds) {
			g.setColor(Color.white);
			if(gc.shotDone || gc.isDead || gc.isWatching)
				g.drawString(gc.secondsLeft+" Seconds", gc.width/2 - messageFontM.stringWidth(""+gc.secondsLeft+" Seconds Left") / 2, gc.height-54);
			else
				g.drawString(gc.secondsLeft+" Seconds", gc.width/2 - messageFontM.stringWidth(""+gc.secondsLeft+" Seconds Left") / 2, gc.height-80);
		}
	}
	
	private final void detailedRender() {
		playerElement pl;
		mineElement me;

		detailedRenderReq = false;		
		backBufferG.drawImage(bufferB, 0, 0, this);
		
		for(int i = 0;i < gc.players.size();i++) {
			pl = (playerElement)gc.players.elementAt(i);
			// Draw player names
			if(gc.gameScenario != 16) {
				backBufferG.setColor(pl.tColor.darker());
					
				backBufferG.drawString(pl.displayName, nInt(pl.x), nInt(pl.y + pl.radius + 10.0));
			}
			
			if(gc.drawPlayers) {
				// Draw Players
				backBufferG.setColor(pl.color);
				backBufferG.fillOval(nInt(pl.x - pl.radius), nInt(pl.y - pl.radius), nInt(pl.radius * 2.0), nInt(pl.radius * 2.0));
				backBufferG.setColor(pl.color.darker());
				backBufferG.fillArc(nInt(pl.x - pl.radius), nInt(pl.y - pl.radius), nInt(pl.radius * 2.0)-1, nInt(pl.radius * 2.0), 90, 180);
				backBufferG.setColor(pl.color);
				backBufferG.fillArc(nInt(pl.x - pl.radius/2.0)+1, nInt(pl.y - pl.radius), nInt(pl.radius), nInt(pl.radius * 2.0), 90, 180);
				backBufferG.setColor(pl.color.darker());
				backBufferG.drawArc(nInt((pl.x - pl.radius) + 1.0), nInt(pl.y - 1.0), nInt(2.0 * pl.radius - 2.0), nInt(pl.radius / 3.0), 0, -180);
				backBufferG.fillOval(nInt(pl.x - (pl.radius / 10.0)), nInt(pl.y - (pl.radius * 0.8)), nInt(pl.radius * 0.8), nInt(pl.radius * 0.8));
				backBufferG.setColor(pl.color.darker().darker());
				backBufferG.fillArc(nInt(pl.x - (pl.radius / 10.0)), nInt(pl.y - (pl.radius * 0.8)), nInt(pl.radius * 0.8), nInt(pl.radius * 0.8), 270, 180);
				backBufferG.setColor(pl.color.darker());
				backBufferG.fillArc(nInt(pl.x - (pl.radius / 10.0) + (pl.radius * 0.15)), nInt(pl.y - (pl.radius * 0.8)), nInt(pl.radius * 0.4), nInt(pl.radius * 0.8), 270, 180);
			}
			
			// Draw shields
			if(pl.shield > 0) {
				int amt = (20*pl.shield);

				backBufferG.setColor(new Color(2, 187, 204, amt));
				backBufferG.fillOval(nInt(pl.x - pl.radius-2.0), nInt(pl.y - pl.radius-2.0), nInt(2.0 *(pl.radius+1.0))+1, nInt(2.0 *(pl.radius+1.0))+1);
				backBufferG.setColor(new Color(2, 187, 204));
				backBufferG.drawOval(nInt(pl.x - pl.radius-2.0), nInt(pl.y - pl.radius-2.0), nInt(2.0 *(pl.radius+1.0))+1, nInt(2.0 *(pl.radius+1.0))+1);
			}
		}
			
		for(int i = 0;i < gc.mines.size();i++) {
			me = (mineElement)gc.mines.elementAt(i);
			
			if(me.status == 0) {
				backBufferG.setColor(new Color(100, 100, 100));
				backBufferG.fillRect(nInt(me.x-me.radius-1.0), nInt(me.y-(me.radius/2.0)), nInt((me.radius+1.0)*2.0), nInt(me.radius));
				backBufferG.fillRect(nInt(me.x-(me.radius/2.0)), nInt(me.y-me.radius-1.0), nInt(me.radius), nInt((me.radius+1.0)*2.0));
				backBufferG.setColor(new Color(150, 150, 150));
				backBufferG.fillOval(nInt(me.x-me.radius), nInt(me.y-me.radius), nInt(me.radius*2.0), nInt(me.radius*2.0));
				backBufferG.setColor(Color.red);
				backBufferG.fillOval(nInt(me.x-(me.radius/3.0)), nInt(me.y-(me.radius/3.0)), nInt(me.radius/1.5), nInt(me.radius/1.5));						
			}
		}
	}
	
	private final void fire(Graphics2D g) {
		playerElement pl;
		mineElement me;
		
		
		if(detailedRenderReq) {
			detailedRender();
		}
		//g.drawImage(bufferB, 0, 0, this);
		g.drawImage(backBuffer, 0, 0, this);
		
		for(int i = 0;i < gc.players.size();i++) {
			pl = (playerElement)gc.players.elementAt(i);
			
			if(pl.mStatus == 1) { // Draw active missle
				if(pl.mX > -10 && pl.mX < gc.width+10 && pl.mY > -10 && pl.mY < gc.height+10) {
					g.setColor(pl.tColor.darker().darker());
					g.fillRect(nInt(pl.mX4 - (pl.mRadius*0.60)), nInt(pl.mY4 - (pl.mRadius*0.60)), nInt(pl.mRadius * 1.2), nInt(pl.mRadius * 1.2));
					backBufferG.setColor(pl.tColor.darker().darker().darker());
					backBufferG.fillOval(nInt(pl.mX1 - (pl.mRadius*0.60)), nInt(pl.mY1 - (pl.mRadius*0.60)), nInt(pl.mRadius * 1.2), nInt(pl.mRadius * 1.2));
					bufferGb.setColor(pl.tColor.darker().darker().darker());
					bufferGb.fillOval(nInt(pl.mX1 - (pl.mRadius*0.60)), nInt(pl.mY1 - (pl.mRadius*0.60)), nInt(pl.mRadius * 1.2), nInt(pl.mRadius * 1.2));
					g.setColor((Color.red).darker());
					g.fillRect(nInt(pl.mX3 - (pl.mRadius*0.70)), nInt(pl.mY3 - (pl.mRadius*0.70)), nInt(pl.mRadius * 1.4), nInt(pl.mRadius * 1.4));
					g.setColor((Color.orange).darker());
					g.fillRect(nInt(pl.mX2 - (pl.mRadius*0.80)), nInt(pl.mY2 - (pl.mRadius*0.80)), nInt(pl.mRadius * 1.6), nInt(pl.mRadius * 1.6));
					g.setColor((Color.yellow).darker());
					g.fillRect(nInt(pl.mX1 - (pl.mRadius*0.90)), nInt(pl.mY1 - (pl.mRadius*0.90)), nInt(pl.mRadius * 1.8), nInt(pl.mRadius * 1.8));
					g.setColor(Color.white);
					g.fillOval(nInt(pl.mX - pl.mRadius), nInt(pl.mY - pl.mRadius), nInt(pl.mRadius * 2.0), nInt(pl.mRadius * 2.0));
				}
			}
			else if(pl.mStatus >= 2 && pl.mStatus <= 4) { // Draw exploding missle
				int maxRadius = 50;
						
				if(pl.mStatus == 2) {
					maxRadius = nInt(4.0*pl.radius);
				}
						
				if(pl.mStatus == 2 || pl.mStatus == 3) { // Hit player or dead obj
					if(pl.mStatus == 3)
						maxRadius = 30;
						
					int gr = nInt(255.0 * pl.mExplosion/maxRadius);
					int r = nInt(75.0 * pl.mExplosion/maxRadius);
					if(gr < 0)
						gr = 0;
					else if(gr > 255) 
						gr = 255;
					g.setColor(new Color(255-r, 255-gr, 0));
				}
				else if(pl.mStatus == 4) { // planet
					maxRadius = 40;
							
					g.setColor(pl.mColor);
				}					
						
				int b = nInt(0.7 + pl.mRadius*(1.0-((double)pl.mExplosion/(double)maxRadius)));
				for(int k = 0;k < pl.mxDebP.length;k++) {
					g.fillRect(nInt(pl.mxDebP[k]), nInt(pl.myDebP[k]), b, b);
				}
			}	
		}	
				
		// Draw Mines
		for(int i = 0;i < gc.mines.size();i++) {
			me = (mineElement)gc.mines.elementAt(i);
			
			if(me.status == 1) {
				Color c = new Color(42, 97, 254);
				for(int k = 10-(gc.frameSkipTo+1);k >= 0;k--) {
					g.setColor(c);
					g.drawOval(nInt(me.x-(me.mRadius+k)), nInt(me.y-(me.mRadius+k)), nInt((me.mRadius+k)*2.0), nInt((me.mRadius+k)*2.0));
					c = c.darker();
				}
			}
		}
		
		// Msgs
		g.setColor(gc.msgColor1);
		g.drawString(gc.msgText1, 3, messageFontM.getHeight()*1);
		g.setColor(gc.msgColor2);
		g.drawString(gc.msgText2, 3, messageFontM.getHeight()*2);
		g.setColor(gc.msgColor3);
		g.drawString(gc.msgText3, 3, messageFontM.getHeight()*3);
		g.setColor(gc.msgColor4);
		g.drawString(gc.msgText4, 3, messageFontM.getHeight()*4);
		g.setColor(gc.msgColor5);
		g.drawString(gc.msgText5, 3, messageFontM.getHeight()*5);
	}
	
	private void hyper(Graphics2D g) {
		playerElement pl;
		mineElement me;
		Color c;
		
		backBufferG.drawImage(bufferB, 0, 0, this);
	
		// Draw players
		for(int i = 0;i < gc.players.size();i++) {
			pl = (playerElement)gc.players.elementAt(i);
				
			if(pl.hyper) {			
				if(pl.hStep < 20) {
					c = new Color(pl.color.getRed() / 20 * (20 - pl.hStep), pl.color.getGreen() / 20 * (20 - pl.hStep), pl.color.getBlue() / 20 * (20 - pl.hStep));
				
					backBufferG.setColor(c);
					backBufferG.fillOval(nInt(pl.x - pl.radius), nInt(pl.y - pl.radius), nInt(pl.radius * 2.0), nInt(pl.radius * 2.0));
					backBufferG.setColor(c.darker());
					backBufferG.fillArc(nInt(pl.x - pl.radius), nInt(pl.y - pl.radius), nInt(pl.radius * 2.0)-1, nInt(pl.radius * 2.0), 90, 180);
					backBufferG.setColor(c);
					backBufferG.fillArc(nInt(pl.x - pl.radius/2.0)+1, nInt(pl.y - pl.radius), nInt(pl.radius), nInt(pl.radius * 2.0), 90, 180);
					backBufferG.setColor(c.darker());
					backBufferG.drawArc(nInt((pl.x - pl.radius) + 1.0), nInt(pl.y - 1.0), nInt(2.0 * pl.radius - 2.0), nInt(pl.radius / 3.0), 0, -180);
					backBufferG.fillOval(nInt(pl.x - (pl.radius / 10.0)), nInt(pl.y - (pl.radius * 0.8)), nInt(pl.radius * 0.8), nInt(pl.radius * 0.8));
					backBufferG.setColor(c.darker().darker());
					backBufferG.fillArc(nInt(pl.x - (pl.radius / 10.0)), nInt(pl.y - (pl.radius * 0.8)), nInt(pl.radius * 0.8), nInt(pl.radius * 0.8), 270, 180);
					backBufferG.setColor(c.darker());
					backBufferG.fillArc(nInt(pl.x - (pl.radius / 10.0) + (pl.radius * 0.15)), nInt(pl.y - (pl.radius * 0.8)), nInt(pl.radius * 0.4), nInt(pl.radius * 0.8), 270, 180);
	
					if(gc.gameScenario != 16) {
						backBufferG.setColor(c.darker());
						backBufferG.drawString(pl.displayName, nInt(pl.x), nInt(pl.y + pl.radius + 10.0));
					}
				}
					
				if(pl.hStep == 20)
					c = pl.color;
				else
					c = new Color(pl.color.getRed() / 20 * pl.hStep, pl.color.getGreen() / 20 * pl.hStep, pl.color.getBlue() / 20 * pl.hStep);
				
				backBufferG.setColor(c);
				backBufferG.fillOval(nInt(pl.hX - pl.radius), nInt(pl.hY - pl.radius), nInt(pl.radius * 2.0), nInt(pl.radius * 2.0));
				backBufferG.setColor(c.darker());
				backBufferG.fillArc(nInt(pl.hX - pl.radius), nInt(pl.hY - pl.radius), nInt(pl.radius * 2.0)-1, nInt(pl.radius * 2.0), 90, 180);
				backBufferG.setColor(c);
				backBufferG.fillArc(nInt(pl.hX - pl.radius/2.0)+1, nInt(pl.hY - pl.radius), nInt(pl.radius), nInt(pl.radius * 2.0), 90, 180);
				backBufferG.setColor(c.darker());
				backBufferG.drawArc(nInt((pl.hX - pl.radius) + 1.0), nInt(pl.hY - 1.0), nInt(2.0 * pl.radius - 2.0), nInt(pl.radius / 3.0), 0, -180);
				backBufferG.fillOval(nInt(pl.hX - (pl.radius / 10.0)), nInt(pl.hY - (pl.radius * 0.8)), nInt(pl.radius * 0.8), nInt(pl.radius * 0.8));
				backBufferG.setColor(c.darker().darker());
				backBufferG.fillArc(nInt(pl.hX - (pl.radius / 10.0)), nInt(pl.hY - (pl.radius * 0.8)), nInt(pl.radius * 0.8), nInt(pl.radius * 0.8), 270, 180);
				backBufferG.setColor(c.darker());
				backBufferG.fillArc(nInt(pl.hX - (pl.radius / 10.0) + (pl.radius * 0.15)), nInt(pl.hY - (pl.radius * 0.8)), nInt(pl.radius * 0.4), nInt(pl.radius * 0.8), 270, 180);
	
				if(gc.gameScenario != 16) {
					backBufferG.setColor(c.darker());
					backBufferG.drawString(pl.displayName, nInt(pl.hX), nInt(pl.hY + pl.radius + 10.0));
				}
			}
			else {
				backBufferG.setColor(pl.color);
				backBufferG.fillOval(nInt(pl.x - pl.radius), nInt(pl.y - pl.radius), nInt(pl.radius * 2.0), nInt(pl.radius * 2.0));
				backBufferG.setColor(pl.color.darker());
				backBufferG.fillArc(nInt(pl.x - pl.radius), nInt(pl.y - pl.radius), nInt(pl.radius * 2.0)-1, nInt(pl.radius * 2.0), 90, 180);
				backBufferG.setColor(pl.color);
				backBufferG.fillArc(nInt(pl.x - pl.radius/2.0)+1, nInt(pl.y - pl.radius), nInt(pl.radius), nInt(pl.radius * 2.0), 90, 180);
				backBufferG.setColor(pl.color.darker());
				backBufferG.drawArc(nInt((pl.x - pl.radius) + 1.0), nInt(pl.y - 1.0), nInt(2.0 * pl.radius - 2.0), nInt(pl.radius / 3.0), 0, -180);
				backBufferG.fillOval(nInt(pl.x - (pl.radius / 10.0)), nInt(pl.y - (pl.radius * 0.8)), nInt(pl.radius * 0.8), nInt(pl.radius * 0.8));
				backBufferG.setColor(pl.color.darker().darker());
				backBufferG.fillArc(nInt(pl.x - (pl.radius / 10.0)), nInt(pl.y - (pl.radius * 0.8)), nInt(pl.radius * 0.8), nInt(pl.radius * 0.8), 270, 180);
				backBufferG.setColor(pl.color.darker());
				backBufferG.fillArc(nInt(pl.x - (pl.radius / 10.0) + (pl.radius * 0.15)), nInt(pl.y - (pl.radius * 0.8)), nInt(pl.radius * 0.4), nInt(pl.radius * 0.8), 270, 180);
		
				if(gc.gameScenario != 16) {
					backBufferG.setColor(pl.tColor.darker());
					backBufferG.drawString(pl.displayName, nInt(pl.x), nInt(pl.y + pl.radius + 10.0));
				}
			}
		}
				
		// Draw Mines
		for(int i = 0;i < gc.mines.size();i++) {
			me = (mineElement)gc.mines.elementAt(i);
					
			if(me.status == 0) {
				backBufferG.setColor(new Color(100, 100, 100));
				backBufferG.fillRect(nInt(me.x-me.radius-1.0), nInt(me.y-(me.radius/2.0)), nInt((me.radius+1.0)*2.0), nInt(me.radius));
				backBufferG.fillRect(nInt(me.x-(me.radius/2.0)), nInt(me.y-me.radius-1.0), nInt(me.radius), nInt((me.radius+1.0)*2.0));
				backBufferG.setColor(new Color(150, 150, 150));
				backBufferG.fillOval(nInt(me.x-me.radius), nInt(me.y-me.radius), nInt(me.radius*2.0), nInt(me.radius*2.0));
				me.flash++;
				if(me.flash == 3) {
					me.flash = 0;
					backBufferG.setColor(Color.red);
				}	
				else {
					backBufferG.setColor((Color.red).darker().darker());
				}
				backBufferG.fillOval(nInt(me.x-(me.radius/3.0)), nInt(me.y-(me.radius/3.0)), nInt(me.radius/1.5), nInt(me.radius/1.5));						
			}
		}
		
		// Msgs
		backBufferG.setColor(gc.msgColor1);
		backBufferG.drawString(gc.msgText1, 3, messageFontM.getHeight()*1);
		backBufferG.setColor(gc.msgColor2);
		backBufferG.drawString(gc.msgText2, 3, messageFontM.getHeight()*2);
		backBufferG.setColor(gc.msgColor3);
		backBufferG.drawString(gc.msgText3, 3, messageFontM.getHeight()*3);
		backBufferG.setColor(gc.msgColor4);
		backBufferG.drawString(gc.msgText4, 3, messageFontM.getHeight()*4);
		backBufferG.setColor(gc.msgColor5);
		backBufferG.drawString(gc.msgText5, 3, messageFontM.getHeight()*5);
				
		g.drawImage(backBuffer, 0, 0, this);
	}
	
	private void wait_for_end(Graphics2D g) {
		playerElement pl;
		mineElement me;
		
		bufferGb.drawImage(bufferA, 0, 0, this);
		g.drawImage(backBuffer, 0, 0, this);
		
		// Msgs				
		if(gc.msgText1.length() > 0) {
			g.setColor(new Color(0, 0, 0, 50));
			g.fillRect(0, (messageFontM.getHeight()*0)+3, messageFontM.stringWidth(gc.msgText1)+10, messageFontM.getHeight());
			g.setColor(gc.msgColor1);
			g.drawString(gc.msgText1, 3, messageFontM.getHeight()*1);
		}
		
		if(gc.msgText2.length() > 0) {
			g.setColor(new Color(0, 0, 0, 50));
			g.fillRect(0, (messageFontM.getHeight()*1)+3, messageFontM.stringWidth(gc.msgText2)+10, messageFontM.getHeight());
			g.setColor(gc.msgColor2);
			g.drawString(gc.msgText2, 3, messageFontM.getHeight()*2);
		}
		
		if(gc.msgText3.length() > 0) {
			g.setColor(new Color(0, 0, 0, 50));
			g.fillRect(0, (messageFontM.getHeight()*2)+3, messageFontM.stringWidth(gc.msgText3)+10, messageFontM.getHeight());
			g.setColor(gc.msgColor3);
			g.drawString(gc.msgText3, 3, messageFontM.getHeight()*3);
		}
		
		if(gc.msgText4.length() > 0) {
			g.setColor(new Color(0, 0, 0, 50));
			g.fillRect(0, (messageFontM.getHeight()*3)+3, messageFontM.stringWidth(gc.msgText4)+10, messageFontM.getHeight());
			g.setColor(gc.msgColor4);
			g.drawString(gc.msgText4, 3, messageFontM.getHeight()*4);
		}
		
		if(gc.msgText5.length() > 0) {
			g.setColor(new Color(0, 0, 0, 50));
			g.fillRect(0, (messageFontM.getHeight()*4)+3, messageFontM.stringWidth(gc.msgText5)+10, messageFontM.getHeight());
			g.setColor(gc.msgColor5);
			g.drawString(gc.msgText5, 3, messageFontM.getHeight()*5);
		}
		
		g.setColor(new Color(0, 0, 0, 50));
		g.fillRect(0, gc.height - 66, gc.width+10, 66);
				
		g.setColor(Color.white);
		g.drawString("Waiting for Other Players...", gc.width / 2 - messageFontM.stringWidth("Waiting for Other Players...") / 2, gc.height-54);
	}
	
	private void end(Graphics2D g) {
		playerElement pl;
		mineElement me;
	
		g.drawImage(backBuffer, 0, 0, this);
					
		g.setColor(new Color(0, 0, 0, 75));
		g.fillRect(0, 0, gc.width, gc.height);
				
		if(gc.result == gwoClient.RESULT_WIN) {
			g.drawImage(gc.youWin, (gc.width /2) - (gc.youWin.getWidth(this)/2), gc.height /3, this);
		}
		else if(gc.result == gwoClient.RESULT_DRAW) {
			g.drawImage(gc.youDraw, (gc.width /2) - (gc.youDraw.getWidth(this)/2), gc.height /3, this);
		}
		else if(gc.result == gwoClient.RESULT_LOSE) {
			g.drawImage(gc.youLose, (gc.width /2) - (gc.youLose.getWidth(this)/2), gc.height /3, this);
		}
		else if(gc.result == gwoClient.RESULT_GAME_OVER) {
			g.drawImage(gc.gameOver, (gc.width /2) - (gc.gameOver.getWidth(this)/2), gc.height /3, this);
		}
				
		g.setColor(Color.white);
		String award;
		for(int i = 0;i < gc.resultAwards.size();i++) {
			award = (String)gc.resultAwards.elementAt(i);
			g.drawString(award, gc.width / 2 - messageFontM.stringWidth(award) / 2, 350+(i*messageFontM.getHeight()));
		}
		
		// Msgs
		g.setColor(gc.msgColor1);
		g.drawString(gc.msgText1, 3, messageFontM.getHeight()*1);
		g.setColor(gc.msgColor2);
		g.drawString(gc.msgText2, 3, messageFontM.getHeight()*2);
		g.setColor(gc.msgColor3);
		g.drawString(gc.msgText3, 3, messageFontM.getHeight()*3);
		g.setColor(gc.msgColor4);
		g.drawString(gc.msgText4, 3, messageFontM.getHeight()*4);
		g.setColor(gc.msgColor5);
		g.drawString(gc.msgText5, 3, messageFontM.getHeight()*5);
	}
}
