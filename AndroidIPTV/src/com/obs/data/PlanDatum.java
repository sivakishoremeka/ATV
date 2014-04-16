package com.obs.data;

import java.util.ArrayList;
import java.util.List;

public class PlanDatum {

private Integer id;
 
private String planCode;
 
private String planDescription;
 
private List<Integer> startDate = new ArrayList<Integer>();
 
private Integer status;
 
private PlanstatusDatum planstatus;
 
private List<PlanServiceDatum> services = new ArrayList<PlanServiceDatum>();
 
private String contractPeriod;
 
private Integer statusname;
 
private Integer contractId;
 
private Boolean isActive;
 
private Integer planCount;

public Integer getId() {
return id;
}

public void setId(Integer id) {
this.id = id;
}

public String getPlanCode() {
return planCode;
}

public void setPlanCode(String planCode) {
this.planCode = planCode;
}

public String getPlanDescription() {
return planDescription;
}

public void setPlanDescription(String planDescription) {
this.planDescription = planDescription;
}

public List<Integer> getStartDate() {
return startDate;
}

public void setStartDate(List<Integer> startDate) {
this.startDate = startDate;
}

public Integer getStatus() {
return status;
}

public void setStatus(Integer status) {
this.status = status;
}

public PlanstatusDatum getPlanstatus() {
return planstatus;
}

public void setPlanstatus(PlanstatusDatum planstatus) {
this.planstatus = planstatus;
}

public List<PlanServiceDatum> getServices() {
return services;
}

public void setServices(List<PlanServiceDatum> services) {
this.services = services;
}

public String getContractPeriod() {
return contractPeriod;
}

public void setContractPeriod(String contractPeriod) {
this.contractPeriod = contractPeriod;
}

public Integer getStatusname() {
return statusname;
}

public void setStatusname(Integer statusname) {
this.statusname = statusname;
}

public Integer getContractId() {
return contractId;
}

public void setContractId(Integer contractId) {
this.contractId = contractId;
}

public Boolean getIsActive() {
return isActive;
}

public void setIsActive(Boolean isActive) {
this.isActive = isActive;
}

public Integer getPlanCount() {
return planCount;
}

public void setPlanCount(Integer planCount) {
this.planCount = planCount;
}

}

