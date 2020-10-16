package org.coolreader.cloud.litres;

import java.util.HashMap;

public class LitresPurchaseInfo {
	public boolean success = false;
	public String errorMessage = "";
	public int errorCode = 0;
	public HashMap<String, String> baskets = new HashMap<>();
}
