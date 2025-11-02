package org.acme.schooltimetabling.dto;

import lombok.Data;

@Data
public class ScheduledLessonDTO {
    private String groupId;
    private String groupName;
    private String subjectId;
    private String subjectName;
    private String professorId;
    private String professorName;
    private int semester;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private String room;
}
