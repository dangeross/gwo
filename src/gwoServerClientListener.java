import java.lang.Thread;
import java.io.*;

class gwoServerClientListener extends Thread {
	private gwoServer gs;
	private int uid;
	
	private gwoDataInputStream dis;
	private gwoDataOutputStream dos;
	
	private boolean quit = false;
	private boolean loggedIn = false;
	
	public gwoServerClientListener(gwoServer gs, int uid, gwoDataInputStream dis, gwoDataOutputStream dos) {
		this.gs = gs;
		this.uid = uid;
		this.dis = dis;
		this.dos = dos;
	}
	
	public void changeUid(int uid) {
		loggedIn = true;
		this.uid = uid;
	}
	
	public void run() {
		byte b = 0; 
		int size = 0;
		
		while(!quit) {
			try {
				
				b = dis.readByte();
				if(b == 66) {
					size = dis.readInt();
					
					if(loggedIn)
						gs.dataIn+=(size+1);
					
					while(dis.available() < size) {
						// block until all data is available
						gs.println("block message");
						sleep(100);
					}
				}
				gs.processMessage(dis, b);
			}
			catch(InterruptedIOException iioe) {}
			catch(InterruptedException ie) {}
			catch(IOException ioe) {
				if(!quit)
					gs.connectionLost(uid);
					
				quit();
			}
		}
	}
	
	public void quit() {
		quit = true;
	}
}
