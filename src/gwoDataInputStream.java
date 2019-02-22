import java.io.*;

class gwoDataInputStream extends DataInputStream {

	public gwoDataInputStream(InputStream out) {
		super(out);
	}
	
	public String readString() throws IOException {
		int len = readInt();
		byte[] data = new byte[len];
		readFully(data);
		
		return new String(data);
	}
	
	public boolean readBool() throws IOException {
		int b = readUnsignedByte();
		
		if(b == 1)
			return true;
		else
			return false;
	}
}