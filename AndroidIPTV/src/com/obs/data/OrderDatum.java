package com.obs.data;

public class OrderDatum {
	public String orderId;
	public String planCode;
	public int pdid;

	public String price;
	public String activeDate;
	public String invoiceTilldate;
	public String status;

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getPlanCode() {
		return planCode;
	}

	public void setPlanCode(String planCode) {
		this.planCode = planCode;
	}

	public String getPrice() {
		return price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getActiveDate() {
		return activeDate;
	}

	public void setActiveDate(String activeDate) {
		this.activeDate = activeDate;
	}

	public String getInvoiceTilldate() {
		return invoiceTilldate;
	}

	public void setInvoiceTilldate(String invoiceTilldate) {
		this.invoiceTilldate = invoiceTilldate;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getPdid() {
		return pdid;
	}

	public void setPdid(int pdid) {
		this.pdid = pdid;
	}
}
