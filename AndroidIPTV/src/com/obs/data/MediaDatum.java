package com.obs.data;

public class MediaDatum {

	private Integer mediaId;

	private String mediaTitle;

	private String mediaImage;

	private Float mediaRating;

	private Integer eventId;

	private String assetTag;

	public Integer getMediaId() {
		return mediaId;
	}

	public void setMediaId(Integer mediaId) {
		this.mediaId = mediaId;
	}

	public String getMediaTitle() {
		return mediaTitle;
	}

	public void setMediaTitle(String mediaTitle) {
		this.mediaTitle = mediaTitle;
	}

	public String getMediaImage() {
		return mediaImage;
	}

	public void setMediaImage(String mediaImage) {
		this.mediaImage = mediaImage;
	}

	public Float getMediaRating() {
		return mediaRating;
	}

	public void setMediaRating(Float mediaRating) {
		this.mediaRating = mediaRating;
	}

	public Integer getEventId() {
		return eventId;
	}

	public void setEventId(Integer eventId) {
		this.eventId = eventId;
	}

	public String getAssetTag() {
		return assetTag;
	}

	public void setAssetTag(String assetTag) {
		this.assetTag = assetTag;
	}

}