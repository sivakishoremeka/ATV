package com.obs.data;

import com.google.gson.annotations.Expose;


public class ResourceIdentifier {

@Expose
private String resourceIdentifier;

public String getResourceIdentifier() {
return resourceIdentifier;
}

public void setResourceIdentifier(String resourceIdentifier) {
this.resourceIdentifier = resourceIdentifier;
}

}