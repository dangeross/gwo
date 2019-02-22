import java.io.*;
import java.net.*;
import javax.swing.JOptionPane;

class gwoServerIpChecker extends Thread {
	private gwoServer gs;
	private String currentIp = "0.0.0.0";
	//private boolean updateReq = false;
	//private boolean firstTime = true;
	
	public gwoServerIpChecker(gwoServer gs) {
		this.gs = gs;
	}
	
	public void recheckIp() {
		gs.updateIp(currentIp);
		currentIp = "0.0.0.0";
	}
	
	public void run() {
		while(true) {
			try {
				// Get Actual IP
				Socket s = new Socket("checkip.dyndns.org", 80);
				BufferedReader ios = new BufferedReader(new InputStreamReader(s.getInputStream()));
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());
				dos.writeBytes("GET http://checkip.dyndns.org HTTP/1.1\r\n\r\n");				

				String line="", result="";
				boolean getIPOK = false;
				
				while((line = ios.readLine()) != null && !getIPOK) {
					if(line.startsWith("<html><head><title>Current IP Check")) {
						result = line.substring(line.indexOf(':')+2, line.length());
						result = result.substring(0, result.indexOf('<'));
						getIPOK = true;
					}
				}
		
				if(getIPOK) {
					if(!result.equals(currentIp)) {
						gs.println("*** IP has changed to "+result);
						currentIp = result;
						
						if(gs.haveDyndns) {
							gs.println("*** Updating DynDNS.org...");
				
							s = new Socket("members.dyndns.org", 80);
							ios = new BufferedReader(new InputStreamReader(s.getInputStream()));
							dos = new DataOutputStream(s.getOutputStream());
		
							char[] base64encode = Base64.encode((gs.dyndnsUsername+":"+gs.dyndnsPassword).getBytes());
							result = new String(base64encode);
							dos.writeBytes("GET http://"+gs.dyndnsUsername+":"+gs.dyndnsPassword+"@members.dyndns.org/nic/update?hostname="+gs.dyndnsUrl+"&myip="+currentIp+" HTTP/1.0\r\n"+
								"Host: members.dyndns.org\r\n"+
								"Authorization: Basic "+result+"\r\n"+
								"User-Agent: gwo2.0\r\n\r\n");
							
							while((result = ios.readLine()) != null) {
								if(result.startsWith("badauth")) {
									gs.println("!!! DynDNS: Username or Password Incorrect");
									JOptionPane.showMessageDialog(gs, "Username or Password Incorrect", "DynDNS Error", JOptionPane.ERROR_MESSAGE);
								}
								else if(result.startsWith("good") || result.startsWith("nochg")) {
									gs.println("*** DynDNS: Update Complete");
									gs.updateIp(result);
								}
								else if(result.startsWith("nohost")) {
									gs.println("!!! DynDNS: Host does not Exist");
									JOptionPane.showMessageDialog(gs, "Host does not Exist", "DynDNS Error", JOptionPane.ERROR_MESSAGE);
								}
								else {
									gs.println("*** DynDNS: "+result);
								}
							}
							
							gs.println("*** Finished");
						
							ios.close();
							dos.close();
							s.close();
						}
						else { // Update GwoNET
							gs.println("*** Updating GwoNET...");
							gs.updateIp(result);
						}
					}
				}
				else {
					gs.println("!!! IP Check Failed");
				}
			}
			catch(UnknownHostException uhe) {
				gs.println("!!! "+uhe.toString());
			}
			catch(IOException ioe) {
				gs.println("!!! "+ioe.toString());
			}
			
			try {
				this.sleep(300000);
			}
			catch(InterruptedException ie) {}
			
			//if(updateReq) {
			//	updateReq = false;
			//	gs.updateIp(result);	
			//}
		}
	}
}
