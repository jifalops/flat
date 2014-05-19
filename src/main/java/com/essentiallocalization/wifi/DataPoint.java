/**
 * 
 */
package com.essentiallocalization.wifi;


import com.essentiallocalization.util.Util;

public class DataPoint {
	int 	level;
	String 	BSSID;
	String 	SSID;
	long	time;
	
	
	public DataPoint(long time, int level, String BSSID, String SSID){
		this.level = level;
		this.time = time;
		this.BSSID = BSSID;
		this.SSID = SSID;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(Util.Format.LOG.format(time)).append("\n");
		sb.append(BSSID).append(" ").append(SSID).append("\n");
		sb.append(level).append("\n\n");
		return sb.toString();
	}
	
	public String toCSVString() {
		StringBuilder sb = new StringBuilder();
		sb.append(time).append(",");
		sb.append(SSID).append(",");
		sb.append(BSSID).append(",");
		sb.append(level).append("\n");
		
		return sb.toString();	
	}
}
