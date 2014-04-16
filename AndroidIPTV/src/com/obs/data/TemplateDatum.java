package com.obs.data;

import java.util.ArrayList;
import java.util.List;

public class TemplateDatum {

	private List<Long> activationDate = new ArrayList<Long>();
	private long officeId;
	private long balanceAmount;
	private List<OfficeOptionDatum> officeOptions = new ArrayList<OfficeOptionDatum>();
	private List<ClientCategoryDatum> clientCategoryDatas = new ArrayList<ClientCategoryDatum>();
	private AddressTemplateDatum addressTemplateData;
	private ConfigurationPropertyDatum configurationProperty;

	public List<Long> getActivationDate() {
		return activationDate;
	}

	public void setActivationDate(List<Long> activationDate) {
		this.activationDate = activationDate;
	}

	public long getOfficeId() {
		return officeId;
	}

	public void setOfficeId(long officeId) {
		this.officeId = officeId;
	}

	public long getBalanceAmount() {
		return balanceAmount;
	}

	public void setBalanceAmount(long balanceAmount) {
		this.balanceAmount = balanceAmount;
	}

	public List<OfficeOptionDatum> getOfficeOptions() {
		return officeOptions;
	}

	public void setOfficeOptions(List<OfficeOptionDatum> officeOptions) {
		this.officeOptions = officeOptions;
	}

	public List<ClientCategoryDatum> getClientCategoryDatas() {
		return clientCategoryDatas;
	}

	public void setClientCategoryDatas(
			List<ClientCategoryDatum> clientCategoryDatas) {
		this.clientCategoryDatas = clientCategoryDatas;
	}

	public AddressTemplateDatum getAddressTemplateData() {
		return addressTemplateData;
	}

	public void setAddressTemplateData(AddressTemplateDatum addressTemplateData) {
		this.addressTemplateData = addressTemplateData;
	}

	public ConfigurationPropertyDatum getConfigurationProperty() {
		return configurationProperty;
	}

	public void setConfigurationProperty(
			ConfigurationPropertyDatum configurationProperty) {
		this.configurationProperty = configurationProperty;
	}

}
