package com.obs.data;

import com.google.gson.annotations.Expose;

public class Paytermdatum {

	
/** We change default serial ids of paytermdata with
 * Subscription data ids	
 */
@Expose
private Integer id;
@Expose
private String paytermtype;

@Expose
private String duration;

@Expose
private String planType;

public Integer getId() {
return id;
}

public void setId(Integer id) {
this.id = id;
}

public String getPaytermtype() {
return paytermtype;
}

public void setPaytermtype(String paytermtype) {
this.paytermtype = paytermtype;
}

public String getDuration() {
return duration;
}

public void setDuration(String duration) {
this.duration = duration;
}

public String getPlanType() {
return planType;
}

public void setPlanType(String planType) {
this.planType = planType;
}

}
