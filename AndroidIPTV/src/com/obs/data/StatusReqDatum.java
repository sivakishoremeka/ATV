package com.obs.data;

import com.google.gson.annotations.Expose;


public class StatusReqDatum {

@Expose
private String clientId;
@Expose
private String status;

public String getClientId() {
return clientId;
}

public void setClientId(String clientId) {
this.clientId = clientId;
}

public String getStatus() {
return status;
}

public void setStatus(String status) {
this.status = status;
}

}