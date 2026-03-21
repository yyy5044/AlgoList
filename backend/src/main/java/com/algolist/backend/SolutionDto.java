package com.algolist.backend;

public class SolutionDto {
    private Long id;
    private String algorithm;
    private String type;
    private String language;
    private String code;
    private Long problemId;

    // 기본 생성자 (MyBatis 매핑용)
    public SolutionDto() {}

    // 생성자
    public SolutionDto(String algorithm, String type, String language, String code, Long problemId) {
        this.algorithm = algorithm;
        this.type = type;
        this.language = language;
        this.code = code;
        this.problemId = problemId;
    }

    // 지금은 이렇게 하고 나중에 롬복 써보자
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getProblemId() { return problemId; }
    public void setProblemId(Long problemId) { this.problemId = problemId; }
    
}