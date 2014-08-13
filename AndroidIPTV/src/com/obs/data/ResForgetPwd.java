package com.obs.data;

import com.google.gson.annotations.Expose;

/**
 * ResForgetPwd bean is used to capture the results for both forget password and
 * reset password results
 * 
 * @author kishore
 * 
 */
public class ResForgetPwd {

	@Expose
	private Integer resourceId;
	@Expose
	private Changes changes;
	@Expose
	private String resourceIdentifier;

	public Integer getResourceId() {
		return resourceId;
	}

	public void setResourceId(Integer resourceId) {
		this.resourceId = resourceId;
	}

	public Changes getChanges() {
		return changes;
	}

	public void setChanges(Changes changes) {
		this.changes = changes;
	}

	public String getResourceIdentifier() {
		return resourceIdentifier;
	}

	public void setResourceIdentifier(String resourceIdentifier) {
		this.resourceIdentifier = resourceIdentifier;
	}

	private class Changes {

	}

}
