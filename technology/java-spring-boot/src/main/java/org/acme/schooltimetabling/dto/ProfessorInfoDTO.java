package org.acme.schooltimetabling.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProfessorInfoDTO {
    private String professorId;
    private String professorName;
    private List<RestrictionInfoDTO> restrictions;
}
