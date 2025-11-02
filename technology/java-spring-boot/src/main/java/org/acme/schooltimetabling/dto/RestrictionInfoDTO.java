package org.acme.schooltimetabling.dto;

import lombok.Data;

@Data
public class RestrictionInfoDTO {
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String reason;
}
