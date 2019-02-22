public class gwo {
	
	public static void main(String args[]) {
		if(args.length == 1) {
			if(args[0].equalsIgnoreCase("-server")) {
				new gwoServer();
			}
			else if(args[0].equalsIgnoreCase("-both")) {
				new gwoClient();
				new gwoServer();
			}
			else {
				System.out.println("Usage: java -jar gwo2.jar [-server]");
			}
		}
		else {
			new gwoClient();
		}
	}
}
