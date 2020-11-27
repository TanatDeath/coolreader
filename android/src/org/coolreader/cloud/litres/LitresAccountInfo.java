package org.coolreader.cloud.litres;

import java.util.HashMap;

public class LitresAccountInfo {
	public boolean success = false;
	public String errorMessage = "";
	public int errorCode = 0;
	public boolean needRefresh = true;
	public HashMap<String, String> fields = new HashMap<>();
	public HashMap<String, String> moneyDetails = new HashMap<>();
	public HashMap<String, String> socnet = new HashMap<>();
}
