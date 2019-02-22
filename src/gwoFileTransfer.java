import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

class gwoFileTransfer extends JFrame implements ActionListener {
	
	// VARIABLES
	
	private gwoClient gc;
	private JButton okButton, cancelButton;
	private JProgressBar progress;
	private JPanel contentPane;
	
	private long fileSize;
	private byte downloadState = BLANK;
	private long downloaded = 0;
	private boolean finished = false;
	private int time = 0;
	private Timer stateMachine;
	
	public static byte BLANK    = 0;
	public static byte CONNECT  = 1;
	public static byte WAIT     = 2;
	public static byte IN_PROG  = 3;
	public static byte FINISHED = 4;
	
	gwoUpdater gu;
	
	public gwoFileTransfer(gwoClient gc, int version, long fileSize) {
		super("GWO - Updater");
		this.gc = gc;
		
		setIconImage(gc.icon);
			
		okButton = new JButton("Ok");
		okButton.addActionListener(this);
		okButton.setActionCommand("$ok");
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("$cancel");
			
		progress = new JProgressBar(0, (int)fileSize);
		progress.setStringPainted(true);
		progress.setIndeterminate(false);
		progress.setString("");
		
		JPanel headerPane = new JPanel();
		headerPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		headerPane.setLayout(new BoxLayout(headerPane, BoxLayout.Y_AXIS));
		headerPane.add(new JLabel("Client version (2.0."+gc.version+") is inconsistant with Server version (2.0."+version+")."));
		headerPane.add(new JLabel("This could cause network protocol errors between Client and Server."));
		headerPane.add(new JLabel("To update to the Server version, press 'Ok'"));
		headerPane.add(new JLabel("If Unsuccessful please visit http://www.dangeross.com/gwo/"));
		headerPane.add(new JLabel("Filesize: "+fileSize+"bytes ("+(fileSize/1024)+"KB)"));
		headerPane.add(progress);
		
		JPanel footerPane = new JPanel();
		footerPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		footerPane.add(okButton);
		footerPane.add(cancelButton);

		contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		contentPane.add("Center", headerPane);
		contentPane.add("South", footerPane);
		
		addWindowListener(
			new WindowAdapter()
			{
				public void windowClosing(WindowEvent evt)
				{
					close();
				}
			}
		);

		setContentPane(contentPane);
		pack();
		setVisible(true);	
							
		gc.setVisible(false);
	
		stateMachine = new Timer(500, updateGUI);
		stateMachine.start();
	}
	
	public void close() {
		setVisible(false);
		gc.close();	
	}
	
	ActionListener updateGUI = new ActionListener() {
	
		public void actionPerformed(ActionEvent e) {
			if(downloadState == BLANK) {
				progress.setIndeterminate(false);
				progress.setString("");		
			}
			else if(downloadState == WAIT) {
				progress.setIndeterminate(false);
				progress.setString("You are in a Queue.  Please Wait.");		
			}
			else if(downloadState == CONNECT) {
				progress.setIndeterminate(true);
				progress.setString("Requesting...");
			
				okButton.setEnabled(false);
			}
			else if(downloadState == IN_PROG) {
				time+=500;
				progress.setValue((int)downloaded);
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
				progress.setValue((int)fileSize);
				progress.setString("Completed.  Please Restart");	
				okButton.setEnabled(true);
			}
			
			if(gu != null) {
				if(gu.isUpdating()) {
					gc.fileTransferCancelled(false);
				}
			}
		}
	};
	
	public void started() {
		if(gu != null) {
			gu.setVisible(false);
			gu.dispose();
		}
	}

	public void queued() {
		downloadState = WAIT;
		
		gu = new gwoUpdater(gc.icon, 0, false);
		new Thread(gu).start();
	}
	
	public void received(int size) {
		downloaded = downloaded+size;
		downloadState = IN_PROG;
	}
	
	public void finished() {
		downloadState = FINISHED;
		finished = true;
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		
		if(cmd.equals("$ok")) {
			if(!finished) {
				downloadState = CONNECT;
				gc.fileTransferStart();
			}
			else {
				gc.fileTransferCancelled(true);
			}
		}
		else if(cmd.equals("$cancel")) {
			gc.fileTransferCancelled(true);
		}
	}
}
