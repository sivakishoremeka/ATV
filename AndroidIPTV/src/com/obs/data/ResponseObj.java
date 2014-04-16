package com.obs.data;

public class ResponseObj {
	private int statusCode;
	private String sResponse;
	private String sErrorMessage;

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getsResponse() {
		return sResponse;
	}

	public void setsResponse(String sResponse) {
		this.sResponse = sResponse;
	}

	public String getsErrorMessage() {
		return sErrorMessage;
	}

	public void setsErrorMessage(String sErrorMessage) {
		this.sErrorMessage = sErrorMessage;
	}

	public void setSuccessResponse(int statusCode, String sResponse) {
		this.statusCode = statusCode;
		this.sResponse = sResponse;
	}

	public void setFailResponse(int statusCode, String sErrorMessage) {
		this.statusCode = statusCode;
		this.sErrorMessage = sErrorMessage;
	}
}
