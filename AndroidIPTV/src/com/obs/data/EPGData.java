package com.obs.data;

import java.util.ArrayList;
import java.util.List;

public class EPGData {

	private List<EpgDatum> epgData = new ArrayList<EpgDatum>();

	public List<EpgDatum> getEpgData() {
		return epgData;
	}

	public void setEpgData(List<EpgDatum> epgData) {
		this.epgData = epgData;
	}

}
