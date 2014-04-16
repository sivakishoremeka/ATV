package com.obs.data;

import java.util.ArrayList;
import java.util.List;

public class MediaDetailRes {

	private Integer noOfPages;

	private Integer pageNo;

	private List<MediaDatum> mediaDetails = new ArrayList<MediaDatum>();

	public Integer getNoOfPages() {
		return noOfPages;
	}

	public void setNoOfPages(Integer noOfPages) {
		this.noOfPages = noOfPages;
	}

	public Integer getPageNo() {
		return pageNo;
	}

	public void setPageNo(Integer pageNo) {
		this.pageNo = pageNo;
	}

	public List<MediaDatum> getMediaDetails() {
		return mediaDetails;
	}

	public void setMediaDetails(List<MediaDatum> mediaDetails) {
		this.mediaDetails = mediaDetails;
	}

}
