import java.net.*;
import java.io.*;
import java.util.Vector;

class gwoNetDataLink extends Thread {
	private gwoServer gs;
	
	private Socket s;
	private DataInputStream dis;
	private DataOutputStream dos;
	
	private Vector dataToPost;
	
	public gwoNetDataLink(gwoServer gs) {
		this.gs = gs;
		dataToPost = new Vector();
	}
	
	public void run() {
		String dataIn = "", dataOut;
		String result = "";
		
		while(true) {
			try {
				if(dataToPost.size() > 0) {
					dataIn = (String)dataToPost.elementAt(0);
					dataToPost.removeElementAt(0);
					
					s = new Socket("www.dangeross.com", 80);
					dis = new DataInputStream(s.getInputStream());
					dos = new DataOutputStream(s.getOutputStream());
							
					dos.writeBytes("POST http://www.dangeross.com/gwo/gwonet.php HTTP/1.0\r\n"+
						"Content-Type: application/x-www-form-urlencoded\r\n"+
						"Content-Length: "+dataIn.length()+"\r\n\r\n"+dataIn);
					dos.flush();
					
					while((dataOut = dis.readLine()) != null) {
						result+=dataOut+"\r\n";
					}
			
					//gs.gwoNetOnline = true;
			
					if(result.lastIndexOf("\r\n\r\n") != -1) {
						int start = result.lastIndexOf("\r\n\r\n")+4;
						int end = result.length()-2;
				
						if(start < end) {
							result = result.substring(result.lastIndexOf("\r\n\r\n")+4, result.length()-2);
							gs.println("GwoNET Result: *"+result+"*");
							
							if(!result.equals("1")) {
								dataToPost.addElement(dataIn);
							}
						}
						else {
							gs.println("GwoNET Result: ");
							dataToPost.addElement(dataIn);	
						}
					}
				}
				else {
					try {
						sleep(1000);
					}
					catch(InterruptedException ie) {}
				}
			}
			catch(IOException ioe) {
				gs.println(ioe.toString());
				// Connection problems
				
				dataToPost.insertElementAt(dataIn, 0);	
					
				try {
					sleep(60000);
				}
				catch(InterruptedException ie) {}				
			}
		}
	}
	
	public void postPassiveData(String data) {
		dataToPost.addElement(data);
	}
	
	public String postActiveData(String data) {
		String dataOut;
		String result = "";
		
		try {
			Socket s2 = new Socket("www.dangeross.com", 80);
			DataInputStream dis2 = new DataInputStream(s2.getInputStream());
			DataOutputStream dos2 = new DataOutputStream(s2.getOutputStream());
				
			dos2.writeBytes("POST http://www.dangeross.com/gwo/gwonet.php HTTP/1.0\r\n"+
				"Content-Type: application/x-www-form-urlencoded\r\n"+
				"Content-Length: "+data.length()+"\r\n\r\n"+data+"\r\n\r\n");
			dos2.flush();
					
			while((dataOut = dis2.readLine()) != null) {
				result+=dataOut+"\r\n";
			}
			
			//gs.gwoNetOnline = true;
			
			if(result.lastIndexOf("\r\n\r\n") != -1) {
				int start = result.lastIndexOf("\r\n\r\n")+4;
				int end = result.length()-2;
				
				if(start < end) {
					gs.println("GwoNET Result: *"+result.substring(result.lastIndexOf("\r\n\r\n")+4, result.length()-2)+"*");
					return result.substring(result.lastIndexOf("\r\n\r\n")+4, result.length()-2);
				}
				else {
					gs.println("GwoNET Result: ");
					return "";	
				}
			}
		}
		catch(IOException ioe) {
			gs.println(ioe.toString());
			//gs.gwoNetOnline = false;
		}
		
		return "";
	}
}