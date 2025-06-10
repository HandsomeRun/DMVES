package cn.edu.ncepu.Model;

public enum LogNameEnum {
    INFORMATION("information"),
    RUN_LOG("runLog"),
    ANALYSIS_LOG("analysisLog");

    private final String description;

    LogNameEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
