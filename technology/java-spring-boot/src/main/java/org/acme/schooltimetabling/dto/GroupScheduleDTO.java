package org.acme.schooltimetabling.dto;

import lombok.Data;

@Data
public class GroupScheduleDTO {
    private String groupId;
    private String groupName;
    private int semester;
    private SubjectInfoDTO subject;
    private ProfessorInfoDTO professor;
    private int classesPerWeek;
    private int totalClasses;
    private int classDurationInHours;
    private String formatTypeId;
}
