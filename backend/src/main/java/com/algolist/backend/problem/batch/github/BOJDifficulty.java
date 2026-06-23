package com.algolist.backend.problem.batch.github;

public enum BOJDifficulty {
	BRONZE("Bronze"),
    BRONZE_I("Bronze I"),
    BRONZE_II("Bronze II"),
    BRONZE_III("Bronze III"),
    BRONZE_IV("Bronze IV"),
    BRONZE_V("Bronze V"),

    SILVER("Silver"),
    SILVER_I("Silver I"),
    SILVER_II("Silver II"),
    SILVER_III("Silver III"),
    SILVER_IV("Silver IV"),
    SILVER_V("Silver V"),

    GOLD("Gold"),
    GOLD_I("Gold I"),
    GOLD_II("Gold II"),
    GOLD_III("Gold III"),
    GOLD_IV("Gold IV"),
    GOLD_V("Gold V"),

    PLATINUM("Platinum"),
    PLATINUM_I("Platinum I"),
    PLATINUM_II("Platinum II"),
    PLATINUM_III("Platinum III"),
    PLATINUM_IV("Platinum IV"),
    PLATINUM_V("Platinum V"),

    DIAMOND("Diamond"),
    DIAMOND_I("Diamond I"),
    DIAMOND_II("Diamond II"),
    DIAMOND_III("Diamond III"),
    DIAMOND_IV("Diamond IV"),
    DIAMOND_V("Diamond V"),

    RUBY("Ruby"),
    RUBY_I("Ruby I"),
    RUBY_II("Ruby II"),
    RUBY_III("Ruby III"),
    RUBY_IV("Ruby IV"),
    RUBY_V("Ruby V");
	
    private final String searchName;

    BOJDifficulty(String searchName) {
        this.searchName = searchName;
    }

    public String getSearchName() {
        return searchName;
    }
}
