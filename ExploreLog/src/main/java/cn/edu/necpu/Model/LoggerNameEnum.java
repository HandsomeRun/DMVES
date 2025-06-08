package cn.edu.necpu.Model;

public enum LoggerNameEnum {
    INFORMATION("information.logger"),
    RUN_LOG("runLog.logger"),
    ANALYSIS_LOG("analysisLog.logger");

    private final String description;

    LoggerNameEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
