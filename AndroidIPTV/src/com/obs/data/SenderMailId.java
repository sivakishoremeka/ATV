package com.obs.data;

import com.google.gson.annotations.SerializedName;

public class SenderMailId {
	
	@SerializedName("uniqueReference")
	public String mailId;

	public SenderMailId(String mailId){
		this.mailId = mailId;
	}
}
