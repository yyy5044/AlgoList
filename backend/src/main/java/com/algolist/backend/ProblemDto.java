package com.algolist.backend;

import java.util.List;

public class ProblemDto {
	private Long userId; // 사용자 식별
	
    private Long id;
    private String title;
    private String number;
    private String difficulty;
    private String site;
    private String link;
    private List<String> category;
    private String grade;
    private int solveCount;
    private String lastSolvedDate;

    // 기본 생성자 (MyBatis가 결과를 매핑할 때 필요)
    public ProblemDto() {}

    // 검색 결과용 생성자 (기존)
    public ProblemDto(String title, String number, String difficulty,
                      String site, String link, List<String> category) {
        this.title = title;
        this.number = number;
        this.difficulty = difficulty;
        this.site = site;
        this.link = link;
        this.category = category;
    }
    
    // 사용자 식별
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public String getNumber() { return number; }
    public String getDifficulty() { return difficulty; }
    public String getSite() { return site; }
    public String getLink() { return link; }
    public List<String> getCategory() { return category; }
    public void setCategory(List<String> category) { this.category = category; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public int getSolveCount() { return solveCount; }
    public void setSolveCount(int solveCount) { this.solveCount = solveCount; }
    public String getLastSolvedDate() { return lastSolvedDate; }
    public void setLastSolvedDate(String lastSolvedDate) { this.lastSolvedDate = lastSolvedDate; }
}