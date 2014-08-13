package com.obs.data;


public class ClientnConfigDatum {


private Integer deviceId;

private Integer clientId;

private String clientType;

private Integer clientTypeId;

private Integer balanceAmount;

private Boolean balanceCheck;

private PaypalConfigData paypalConfigData;

private String currency;

public Integer getDeviceId() {
return deviceId;
}

public void setDeviceId(Integer deviceId) {
this.deviceId = deviceId;
}

public Integer getClientId() {
return clientId;
}

public void setClientId(Integer clientId) {
this.clientId = clientId;
}

public String getClientType() {
return clientType;
}

public void setClientType(String clientType) {
this.clientType = clientType;
}

public Integer getClientTypeId() {
return clientTypeId;
}

public void setClientTypeId(Integer clientTypeId) {
this.clientTypeId = clientTypeId;
}

public Integer getBalanceAmount() {
return balanceAmount;
}

public void setBalanceAmount(Integer balanceAmount) {
this.balanceAmount = balanceAmount;
}

public Boolean getBalanceCheck() {
return balanceCheck;
}

public void setBalanceCheck(Boolean balanceCheck) {
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