package com.obs.data;

public class DeviceDatum {

	private long deviceId;
	private long clientId;
	private String clientType;
	private long clientTypeId;

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
}
