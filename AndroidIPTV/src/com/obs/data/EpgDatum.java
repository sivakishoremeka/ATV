package com.obs.data;

public class EpgDatum {

	private String channelName;

	private String channelIcon;

	private String programDate;

	private String startTime;

	private String stopTime;

	private String programTitle;

	private String programDescription;

	private String type;

	private String genre;

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getChannelIcon() {
		return channelIcon;
	}

	public void setChannelIcon(String channelIcon) {
		this.channelIcon = channelIcon;
	}

	public String getProgramDate() {
		return programDate;
	}

	public void setProgramDate(String programDate) {
		this.programDate = programDate;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getStopTime() {
		return stopTime;
	}

	public void setStopTime(String stopTime) {
		this.stopTime = stopTime;
	}

	public String getProgramTitle() {
		return programTitle;
	}

	public void setProgramTitle(String programTitle) {
		this.programTitle = programTitle;
	}

	public String getProgramDescription() {
		return programDescription;
	}

	public void setProgramDescription(String programDescription) {
		this.programDescription = programDescription;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

}
