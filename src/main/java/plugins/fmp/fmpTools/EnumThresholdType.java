package plugins.fmp.fmpTools;


public enum EnumThresholdType { 
	SINGLE ("simple threshold"), COLORARRAY ("Color array"), NONE("undefined");
	
	private String label;
	EnumThresholdType (String label) { 
		this.label = label;}
	public String toString() { 
		return label;}	
	public static EnumThresholdType findByText(String abbr){
	    for(EnumThresholdType v : values()){ 
	    	if( v.toString().equals(abbr)) { 
	    		return v; }  }
	    return null;
	}
}
