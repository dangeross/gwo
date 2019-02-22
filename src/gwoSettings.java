import java.util.StringTokenizer;

class gwoSettings {

	public gwoSettings() {
	}
	
	public String getSettingsCommand(StringTokenizer st) {
		if(st.hasMoreTokens()) {
			String t = st.nextToken();
			int i = t.lastIndexOf('\t');
			
			if(i >= 0)
				t = t.substring(i+1);
				
			return t;
		}
		else {
			return "";
		}
	}
	
	public String getSettingsValue_S(StringTokenizer st) {
		if(st.hasMoreTokens()) {
			return st.nextToken();
		}
		else {
			return "";
		}
	}
	
	public boolean getSettingsValue_B(StringTokenizer st) {
		if(st.hasMoreTokens()) {
			return new Boolean(st.nextToken()).booleanValue();
		}
		else {
			return false;
		}
	}
	
	public byte getSettingsValue_Byte(StringTokenizer st) {
		if(st.hasMoreTokens()) {
			try {
				return new Byte(st.nextToken()).byteValue();
			}
			catch(NumberFormatException nfe) {
				return 0;
			}
		}
		else {
			return 0;
		}
	}
	
	public int getSettingsValue_I(StringTokenizer st) {
		if(st.hasMoreTokens()) {
			try {
				return new Integer(st.nextToken()).intValue();
			}
			catch(NumberFormatException nfe) {
				return 0;
			}
		}
		else {
			return 0;
		}
	}
}
