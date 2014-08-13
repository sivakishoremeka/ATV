package com.obs.data;

import com.google.gson.annotations.SerializedName;

public class ResetPwdDatum {

	@SerializedName("uniqueReference")
	public String mailId;
	@SerializedName("password")
	public String password;

	public ResetPwdDatum(String mailId,String pwd){
		this.mailId = mailId;
		this.password = pwd;
	}
}
