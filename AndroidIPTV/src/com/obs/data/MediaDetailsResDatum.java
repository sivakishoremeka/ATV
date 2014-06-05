package com.obs.data;

import java.util.ArrayList;
import java.util.List;
import com.google.gson.annotations.SerializedName;

public class MediaDetailsResDatum {

	private Integer mediaId;

	private String title;

	private String type;

	private String classType;

	private String overview;

	private String subject;

	private String image;

	private String duration;

	private String contentProvider;

	private String rated;

	private Double rating;

	private Integer ratingCount;

	private String status;

	private String releaseDate;
	@SerializedName("Genre")
	private List<Object> genre = new ArrayList<Object>();
	@SerializedName("Producer")
	private List<Object> producer = new ArrayList<Object>();

	private List<FilmLocation> filmLocations = new ArrayList<FilmLocation>();
	@SerializedName("Writer")
	private List<Object> writer = new ArrayList<Object>();
	@SerializedName("Director")
	private List<Object> director = new ArrayList<Object>();
	@SerializedName("Actor")
	private List<Object> actor = new ArrayList<Object>();

	private List<Object> countries = new ArrayList<Object>();

	private List<PriceDetail> priceDetails = new ArrayList<PriceDetail>();

	public Integer getMediaId() {
		return mediaId;
	}

	public void setMediaId(Integer mediaId) {
		this.mediaId = mediaId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getClassType() {
		return classType;
	}

	public void setClassType(String classType) {
		this.classType = classType;
	}

	public String getOverview() {
		return overview;
	}

	public void setOverview(String overview) {
		this.overview = overview;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getContentProvider() {
		return contentProvider;
	}

	public void setContentProvider(String contentProvider) {
		this.contentProvider = contentProvider;
	}

	public String getRated() {
		return rated;
	}

	public void setRated(String rated) {
		this.rated = rated;
	}

	public Double getRating() {
		return rating;
	}

	public void setRating(Double rating) {
		this.rating = rating;
	}

	public Integer getRatingCount() {
		return ratingCount;
	}

	public void setRatingCount(Integer ratingCount) {
		this.ratingCount = ratingCount;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getReleaseDate() {
		return releaseDate;
	}

	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}

	public List<Object> getGenre() {
		return genre;
	}

	public void setGenre(List<Object> genre) {
		this.genre = genre;
	}

	public List<Object> getProducer() {
		return producer;
	}

	public void setProducer(List<Object> producer) {
		this.producer = producer;
	}

	public List<FilmLocation> getFilmLocations() {
		return filmLocations;
	}

	public void setFilmLocations(List<FilmLocation> filmLocations) {
		this.filmLocations = filmLocations;
	}

	public List<Object> getWriter() {
		return writer;
	}

	public void setWriter(List<Object> writer) {
		this.writer = writer;
	}

	public List<Object> getDirector() {
		return director;
	}

	public void setDirector(List<Object> director) {
		this.director = director;
	}

	public List<Object> getActor() {
		return actor;
	}

	public void setActor(List<Object> actor) {
		this.actor = actor;
	}

	public List<Object> getCountries() {
		return countries;
	}

	public void setCountries(List<Object> countries) {
		this.countries = countries;
	}

	public List<PriceDetail> getPriceDetails() {
		return priceDetails;
	}

	public void setPriceDetails(List<PriceDetail> priceDetails) {
		this.priceDetails = priceDetails;
	}

}