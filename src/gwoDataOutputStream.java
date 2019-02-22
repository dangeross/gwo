import java.io.*;

class gwoDataOutputStream extends DataOutputStream {

	public gwoDataOutputStream(OutputStream out) {
		super(out);
	}
	
	public void write(int[] b) throws IOException {
		for(int i = 0;i < b.length;i++) {
			write(b[i]);
		}
	}
	
	public void write(int[] b, int off, int len) throws IOException {
		for(int i = off;i < len;i++) {
			write(b[i]);
		}
	}
}