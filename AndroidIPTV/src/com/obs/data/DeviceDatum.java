package com.obs.data;

public class DeviceDatum {

	private long deviceId;
	private long clientId;
	private String clientType;
	private long clientTypeId;
	private float balanceAmount;
	private boolean balanceCheck;
	private PaypalConfigData paypalConfigData;
	private String currency;

	public long getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(long deviceId) {
		this.deviceId = deviceId;
	}

	public long getClientId() {
		return clientId;
	}

	public void setClientId(long clientId) {
		this.clientId = clientId;
	}

	public String getClientType() {
		return clientType;
	}

	public void setClientType(String clientType) {
		this.clientType = clientType;
	}

	public long getClientTypeId() {
		return clientTypeId;
	}

	public void setClientTypeId(long clientTypeId) {
		this.clientTypeId = clientTypeId;
	}

	public float getBalanceAmount() {
		return balanceAmount;
	}

	public void setBalanceAmount(float balanceAmount) {
		this.balanceAmount = balanceAmount;
	}

	public boolean isBalanceCheck() {
		return balanceCheck;
	}

	public void setBalanceCheck(boolean balanceCheck) {
		this.balanceCheck = balanceCheck;
	}

	public PaypalConfigData getPaypalConfigData() {
		return paypalConfigData;
	}

	public void setPaypalConfigData(PaypalConfigData paypalConfigData) {
		this.paypalConfigData = paypalConfigData;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

}
