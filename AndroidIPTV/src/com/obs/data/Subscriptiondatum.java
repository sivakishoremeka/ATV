package com.obs.data;

import com.google.gson.annotations.Expose;

public class Subscriptiondatum {

@Expose
private Integer id;
@Expose
private String Contractdata;
@Expose
private String subscriptionType;

public Integer getId() {
return id;
}

public void setId(Integer id) {
this.id = id;
}

public String getContractdata() {
return Contractdata;
}

public void setContractdata(String Contractdata) {
this.Contractdata = Contractdata;
}

public String getSubscriptionType() {
return subscriptionType;
}

public void setSubscriptionType(String subscriptionType) {
this.subscriptionType = subscriptionType;
}

}