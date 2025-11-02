package org.acme.schooltimetabling.dto;

import lombok.Data;

@Data
public class SubjectInfoDTO {
    private String subjectId;
    private String subjectName;
    private int semester;
    private int position;
}
