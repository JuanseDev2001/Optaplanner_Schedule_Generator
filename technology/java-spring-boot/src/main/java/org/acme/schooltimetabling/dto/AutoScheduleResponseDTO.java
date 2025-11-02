package org.acme.schooltimetabling.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoScheduleResponseDTO {
    private String planningId;
    private String status;
    private String scoreExplanation;
    private List<ScheduledClassDTO> scheduledClasses;
}
