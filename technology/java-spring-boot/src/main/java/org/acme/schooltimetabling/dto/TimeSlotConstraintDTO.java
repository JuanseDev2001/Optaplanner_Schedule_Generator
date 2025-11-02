package org.acme.schooltimetabling.dto;

import lombok.Data;

@Data
public class TimeSlotConstraintDTO {
    private String startTime;
    private String endTime;
}
