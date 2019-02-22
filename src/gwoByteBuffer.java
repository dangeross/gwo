public class gwoByteBuffer {
	private int[] buffer;
	private int mark = 0;
	
	private int csum = 0;
	private int csize = 0;
	
	public static void main(String args[]) {
		gwoByteBuffer gbb = new gwoByteBuffer();
		
		gbb.bufferBoolean(true);
		gbb.bufferByte(150);
		gbb.bufferInt(54887);
		gbb.bufferShort((short)548);
		gbb.bufferLong((long)548908709);
		gbb.bufferDouble(5.55);
		gbb.bufferFloat((float)5.55);
		gbb.bufferString("Hello World");
		gbb.debug();
	}
	
	public gwoByteBuffer() {
		buffer = new int[0];
		mark = 0;
		csum = 0;
		csize = 0;
	}
	
	private boolean checkSize(int n) {
		try {
			if(buffer.length < mark+n) {
				// Increase buffer size
				int[] tmp = (int[])buffer.clone();
				buffer = new int[mark+n];
				System.arraycopy(tmp, 0, buffer, 0, tmp.length);
			}
			
			return true;
		}
		catch(IndexOutOfBoundsException ioobe) {System.err.println(ioobe.toString());}
		catch(ArrayStoreException ase) {System.err.println(ase.toString());}
		catch(NullPointerException npe) {System.err.println(npe.toString());}
		
		return false;
	}
	
	public boolean checksum() {
		int tsum = 0;
		for(int i = 0;i < buffer.length;i++) {
			tsum+=buffer[i];
		}
		
		if(tsum == csum && csize == buffer.length) {
			return true;
		}
		else {
			System.out.println("CHECKSUM ERROR: actual sum:"+tsum+" correct sum:"+csum+", actual size:"+buffer.length+" correct size:"+csize);
			return false;	
		}
	}
	
	public void skip(int n) {
		if(checkSize(n)) {
			mark+=n;
		}
	}
	
	public void clear() {
		buffer = new int[0];
		mark = 0;	
		csum = 0;
		csize = 0;
	}
	
	public void bufferBoolean(boolean n) {
		try {
			if(checkSize(1)) {
				if(n)
					buffer[mark] = 1;
				else
					buffer[mark] = 0;
					
				csum+=buffer[mark];

				csize++;				
				mark++;	
			}
		}
		catch(ArrayIndexOutOfBoundsException aioobe) {
			System.out.println("--- BUFFER BOOLEAN ---");
			System.out.println("Buffer Size: "+buffer.length+" Mark: "+mark);
			debug();
			bufferBoolean(n);
		}
	}
	
	public void bufferByte(int n) {
		try {
			if(checkSize(1)) {
				buffer[mark] = n;				
				csum+=n;

				csize++;
				mark++;
			}
		}
		catch(ArrayIndexOutOfBoundsException aioobe) {
			System.out.println("--- BUFFER BYTE ---");
			System.out.println("Buffer Size: "+buffer.length+" Mark: "+mark);
			debug();
			bufferByte(n);
		}
	}
	
	public void bufferInt(int n) {
		try {
			if(checkSize(4)) {
				buffer[mark+3] = n&255;
				csum+=buffer[mark+3];
				buffer[mark+2] = (n >>= 8)&255;
				csum+=buffer[mark+2];
				buffer[mark+1] = (n >>= 8)&255;
				csum+=buffer[mark+1];
				buffer[mark] = (n >>= 8)&255;
				csum+=buffer[mark];
				
				csize+=4;
				mark+=4;
			}
		}
		catch(ArrayIndexOutOfBoundsException aioobe) {
			System.out.println("--- BUFFER INT ---");
			System.out.println("Buffer Size: "+buffer.length+" Mark: "+mark);
			debug();
			bufferInt(n);
		}
	}
	
	public void bufferShort(short n) {
		try {
			if(checkSize(2)) {
				buffer[mark+1] = n&255;
				csum+=buffer[mark+1];
				buffer[mark] = (n >>= 8)&255;
				csum+=buffer[mark];
				
				csize+=2;
				mark+=2;
			}
		}
		catch(ArrayIndexOutOfBoundsException aioobe) {
			System.out.println("--- BUFFER SHORT ---");
			System.out.println("Buffer Size: "+buffer.length+" Mark: "+mark);
			debug();
			bufferShort(n);
		}
	}
	
	public void bufferLong(long n) {
		try {
			if(checkSize(8)) {
				buffer[mark+7] = (int)n&255;
				csum+=buffer[mark+7];
				buffer[mark+6] = (int)(n >>= 8)&255;
				csum+=buffer[mark+6];
				buffer[mark+5] = (int)(n >>= 8)&255;
				csum+=buffer[mark+5];
				buffer[mark+4] = (int)(n >>= 8)&255;
				csum+=buffer[mark+4];
				buffer[mark+3] = (int)(n >>= 8)&255;
				csum+=buffer[mark+3];
				buffer[mark+2] = (int)(n >>= 8)&255;
				csum+=buffer[mark+2];
				buffer[mark+1] = (int)(n >>= 8)&255;
				csum+=buffer[mark+1];
				buffer[mark] = (int)(n >>= 8)&255;
				csum+=buffer[mark];
				
				csize+=8;
				mark+=8;
			}
		}
		catch(ArrayIndexOutOfBoundsException aioobe) {
			System.out.println("--- BUFFER LONG ---");
			System.out.println("Buffer Size: "+buffer.length+" Mark: "+mark);
			debug();
			bufferLong(n);
		}
	}
	
	public void bufferDouble(double n) {
		bufferLong(Double.doubleToLongBits(n));
	}
	
	public void bufferFloat(float n) {
		bufferInt(Float.floatToIntBits(n));
	}
	
	public void bufferString(String n) {
		int tMark = mark;
		
		try {
			// String length
			bufferInt(n.length());
			
			if(checkSize(n.length())) {
				// String length
				//buffer[mark++] = n.length();
				
				for(int i = 0;i < n.length();i++) {
					buffer[mark++] = (int)n.charAt(i);
					csum+=(int)n.charAt(i);
				}
				
				csize+=n.length();
			}
		}
		catch(ArrayIndexOutOfBoundsException aioobe) {
			System.out.println("--- BUFFER STRING ---");
			System.out.println("Buffer Size: "+buffer.length+" Mark: "+mark);
			debug();
			mark = tMark;
			bufferString(n);
		}
	}
	
	public void bufferByteArray(byte[] n, int size) {
		int tMark = mark;
		
		try {
			if(checkSize(size)) {
				for(int i = 0;i < size;i++) {
					buffer[mark++] = (int)n[i];
					csum+=(int)n[i];
				}
				
				csize+=size;			
			}
		}
		catch(ArrayIndexOutOfBoundsException aioobe) {
			System.out.println("--- BUFFER ARRAY ---");
			System.out.println("Buffer Size: "+buffer.length+" Mark: "+mark);
			debug();
			mark = tMark;
			bufferByteArray(n, size);
		}
	}
	
	public int[] data() {
		return buffer;
	}
	
	public void debug() {
		for(int i = 0;i < buffer.length;i++) {
			System.out.print("["+buffer[i]+"]");
		}
		System.out.println();
	}
}