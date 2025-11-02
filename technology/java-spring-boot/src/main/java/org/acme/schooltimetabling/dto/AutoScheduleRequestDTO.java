package org.acme.schooltimetabling.dto;

import lombok.Data;
import java.util.List;
import java.time.LocalDate;

@Data
public class AutoScheduleRequestDTO {
    private String planningId;
    private LocalDate startDate;
    private LocalDate endDate;
    private TimeSlotConstraintDTO workWeekConfig;
    private TimeSlotConstraintDTO weekendConfig;
    private String formatTypePresId;
    private String formatTypeVirtId;
    private List<GroupScheduleDTO> groups;
}
