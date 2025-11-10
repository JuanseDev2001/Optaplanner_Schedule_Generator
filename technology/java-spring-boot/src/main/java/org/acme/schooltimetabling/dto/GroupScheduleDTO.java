package org.acme.schooltimetabling.dto;

import lombok.Data;
import java.util.List;

@Data
public class GroupScheduleDTO {
    private String groupId;
    private String groupName;
    private int semester;
    private SubjectInfoDTO subject;
    private List<ProfessorInfoDTO> professors;
    private int classesPerWeek;
    private int totalClasses;
    private int classDurationInHours;
    private String formatTypeId;
}
