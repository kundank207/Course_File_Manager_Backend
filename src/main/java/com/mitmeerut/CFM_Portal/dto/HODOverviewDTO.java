package com.mitmeerut.CFM_Portal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HODOverviewDTO {
    private long totalTeachers;
    private long totalCourses;
    private long pendingApprovals;
    private List<Map<String, Object>> weeklyEngagementData;
    private List<Map<String, Object>> courseDistribution;
    private double activityPercent;
}
