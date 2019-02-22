import java.lang.Thread;
import java.io.*;

class gwoClientServerListener extends Thread {
	private gwoClient gs;
	
	private gwoDataInputStream dis;
	private gwoDataOutputStream dos;
	
	private boolean quit = false;
	
	public gwoClientServerListener(gwoClient gs, gwoDataInputStream dis, gwoDataOutputStream dos) {
		this.gs = gs;
		this.dis = dis;
		this.dos = dos;
	}
	
	public void run() {
		boolean byteValid = false;
		byte b = 0; 
		int size = 0;
		int blockCount = 0;
		
		while(!quit) {
			try {
				// b should only equal 66
				if(!byteValid)
					b = dis.readByte();
					
				if(b == 66) {
					byteValid = true;
					blockCount = 0;
					size = dis.readInt();
					while(dis.available() < size && blockCount <= 50) {
						blockCount++;
						// block until all data is available
						//System.out.println("block message");
						sleep(200);
						
						//if(blockCount == 20)
						//	byteValid = false;						
					}
					gs.processMessage(b, byteValid);
					byteValid = false;
				}
				else {
					gs.processMessage(b, false);
				}
			}
			catch(InterruptedIOException iioe) {}
			catch(InterruptedException ie) {
			
			}
			catch(IOException ioe) {
				if(!quit)
					gs.connectionLost();
					
				quit();
			}
		}
	}
	
	public void quit() {
		quit = true;
	}
}
