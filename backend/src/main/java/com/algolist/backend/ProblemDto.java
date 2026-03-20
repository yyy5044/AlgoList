package com.algolist.backend;

import java.util.List;

public class ProblemDto {
	private String title;
	private String number;
	private String difficulty;
	private String site;
	private String link;
	private List<String> category;
	
	public ProblemDto(String title, String number, String difficulty, String site, String link, List<String> category) {
		super();
		this.title = title;
		this.number = number;
		this.difficulty = difficulty;
		this.site = site;
		this.link = link;
		this.category = category;
	}
	
    public String getTitle() { return title; }
    public String getNumber() { return number; }
    public String getDifficulty() { return difficulty; }
    public String getSite() { return site; }
    public String getLink() { return link; }
    public List<String> getCategory() { return category; }
}
