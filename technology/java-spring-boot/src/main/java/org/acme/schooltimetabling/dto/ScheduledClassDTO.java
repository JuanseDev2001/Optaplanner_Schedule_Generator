package org.acme.schooltimetabling.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledClassDTO {
    private String startTime;
    private String endTime;
    private String groupId;
    private String formatTypeId;
    private List<String> professorIds;
}
