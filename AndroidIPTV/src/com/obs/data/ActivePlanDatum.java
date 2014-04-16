package com.obs.data;

public class ActivePlanDatum {

	String id;
	String planCode;
	String planDescription;
	String price;
	String billingFrequency;
	String contractPeriod;
	String flag;

	public String getId() {
		return id;
	}

	public void setId(String id) {
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

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getBillingFrequency() {
		return billingFrequency;
	}

	public void setBillingFrequency(String billingFrequency) {
		this.billingFrequency = billingFrequency;
	}

	public String getContractPeriod() {
		return contractPeriod;
	}

	public void setContractPeriod(String contractPeriod) {
		this.contractPeriod = contractPeriod;
	}

	public String getFlag() {
		return flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

}
