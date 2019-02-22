import java.net.*;
import java.io.File;

public class gwoLoader {
	
	public static void main(String args[]) {
		try {
			URL[] urls = { new File("gwo2.jar").toURL() };
			ClassLoader cl = new URLClassLoader(urls);
			
			if(args.length > 0) {
				if(args[0].equalsIgnoreCase("-server")) {
					cl.loadClass("gwoServer").newInstance();
				}
				else {
					cl.loadClass("gwoClient").newInstance();	
				}
			}else {
				cl.loadClass("gwoClient").newInstance();
			}
		}
		catch(MalformedURLException mue) {
			System.out.println(mue.toString());	
		}
		catch(InstantiationException ie) {
			System.out.println(ie.toString());	
		}
		catch(ClassNotFoundException cnfe) {
			System.out.println(cnfe.toString());	
		}
		catch(IllegalAccessException iae) {
			System.out.println(iae.toString());	
		}
	}	
}