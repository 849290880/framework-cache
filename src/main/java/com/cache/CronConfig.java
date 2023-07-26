package com.cache;



public class CronConfig {

    private final String cronExpression;

    public CronConfig(String cronExpressionValue) {
        this.cronExpression = cronExpressionValue;
    }

    public String getCronExpression() {
        return cronExpression;
    }
}