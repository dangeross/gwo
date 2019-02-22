import java.lang.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import java.net.*;
import java.io.*;

class gwoUpdater extends JFrame implements Runnable, ActionListener {
	
	// VARIABLES
	
	private gwoServer gs;
	private JButton okButton, cancelButton;
	private JProgressBar progress;
	private JPanel contentPane;
	
	private int currentSize=0;
	private int fileSize=0;
	private byte downloadState = BLANK;
	private int downloaded = 0;
	private boolean finished = false;
	private int time = 0;
	private Timer stateMachine;
	private boolean isServer;
	
	public static byte BLANK    = 0;
	public static byte CONNECT  = 1;
	public static byte CANCEL   = 2;
	public static byte IN_PROG  = 3;
	public static byte FINISHED = 4;
	
	public gwoUpdater(Image icon, long fs, boolean isServer) {
		this.gs = gs;
		this.isServer = isServer;
		currentSize = (int)fs;
		
		setIconImage(icon);
		
		okButton = new JButton("Ok");
		okButton.addActionListener(this);
		okButton.setActionCommand("$ok");
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("$cancel");
			
		progress = new JProgressBar(0, 100);
		progress.setStringPainted(true);
		progress.setIndeterminate(false);
		progress.setString("");
		
		JPanel headerPane = new JPanel();
		headerPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		headerPane.setLayout(new BoxLayout(headerPane, BoxLayout.Y_AXIS));
		
		if(isServer) {
			setTitle("GWO Server - Updater");
			headerPane.add(new JLabel("GWO Server has detected a newer version of GWO available."));
			headerPane.add(new JLabel("To update to the Server version, press 'Ok'"));
		}
		else {
			setTitle("GWO Client - Updater");
			headerPane.add(new JLabel("You are currently held in a queue on the GWO Server."));
			headerPane.add(new JLabel("Press 'Ok' to download from the website instead or 'Cancel' to stay in the queue."));
		}
		headerPane.add(new JLabel("If Unsuccessful please visit http://www.dangeross.com/gwo/"));
		headerPane.add(progress);
		
		JPanel footerPane = new JPanel();
		footerPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		footerPane.add(okButton);
		footerPane.add(cancelButton);

		contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		contentPane.add("Center", headerPane);
		contentPane.add("South", footerPane);

		setContentPane(contentPane);
		pack();
		//setVisible(true);
		
		stateMachine = new Timer(500, updateGUI);
		stateMachine.start();
	}	
	
	ActionListener updateGUI = new ActionListener() {
	
		public void actionPerformed(ActionEvent e) {
			if(downloadState == BLANK) {
				if(fileSize > 0) {
					progress.setMaximum(fileSize);
					progress.setIndeterminate(false);
					progress.setString(fileSize+"bytes ("+(fileSize/1024)+"KB)");				
				}
				else {
					progress.setIndeterminate(false);
					progress.setString("");				
				}
			}
			else if(downloadState == CANCEL) {
				progress.setIndeterminate(false);
				progress.setString("Cancelled");		
			}
			else if(downloadState == CONNECT) {
				progress.setIndeterminate(true);
				progress.setString("Requesting...");
			
				okButton.setEnabled(false);
			}
			else if(downloadState == IN_PROG) {
				time+=500;
				progress.setValue(downloaded);
				progress.setIndeterminate(false);
				String speed = ""+(((double)downloaded/1024.0)/((double)time/1000.0));
				
				if(speed.length() > 4) {
					speed = speed.substring(0, 4);
				}
				else {
					while(speed.length() < 4)
						speed+="0";
				}
				
				if(downloaded > 0 && time > 0)
					progress.setString((int)(progress.getPercentComplete()*100) + "% @ "+speed+"KB/s");	
				else
					progress.setString((int)(progress.getPercentComplete()*100) + "%");
				
				time+=500;
			}
			else if(downloadState == FINISHED) {
				progress.setValue(fileSize);
				progress.setString("Completed.  Please Restart");	
				okButton.setEnabled(true);
			}
		}
	};
	
	public void run() {
		try {
			URLConnection urlcon = new URL("http://www.dangeross.com/gwo/files/gwo2.jar").openConnection();
			fileSize = urlcon.getContentLength();
			//System.out.println(fileSize);
			
			if(fileSize > 0 && fileSize != currentSize) {
				setVisible(true);
				//System.out.println(fileSize);
				
				while(downloadState != CONNECT && downloadState != CANCEL) {
					try {
						Thread.sleep(1000);	
					}
					catch(InterruptedException ie) {}
				}
				
				if(downloadState == CONNECT) {					
					DataInputStream dis = new DataInputStream(urlcon.getInputStream());
					FileOutputStream fos = new FileOutputStream("gwo2.jar");
					byte[] data = new byte[50000];
					int size=0;
					while((size = dis.read(data)) != -1) {
						downloadState = IN_PROG;
						
						fos.write(data, 0, size);
						downloaded += size;
					}
					
					finished = true;
					downloadState = FINISHED;
					okButton.setEnabled(true);
				}
			}
		}
		catch(IOException ioe) {
			System.out.println(ioe.toString());
		}
	}
	
	public boolean isUpdating() {
		if(downloadState > CANCEL) {
			return true;
		}
		
		return false;
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if(cmd.equals("$ok")) {
			if(!finished) {
				downloadState = CONNECT;
				okButton.setEnabled(false);
			}
			else {
				System.exit(0);
			}
		}
		else if(cmd.equals("$cancel")) {
			downloadState = CANCEL;
			setVisible(false);
		}
	}
}