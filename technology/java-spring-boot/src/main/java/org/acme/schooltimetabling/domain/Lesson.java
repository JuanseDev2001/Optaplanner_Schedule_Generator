package org.acme.schooltimetabling.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.ElementCollection;
import lombok.Data;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.util.List;
import java.util.ArrayList;

@PlanningEntity
@Entity
@Data
public class Lesson {

    @PlanningId
    @Id @GeneratedValue
    private Long id;

    private String subject;
    private String teacher;
    private String studentGroup;

    private String groupId;
    
    @ElementCollection
    private List<String> professorIds = new ArrayList<>();
    
    private String formatTypeId;
    private int subjectPosition;  // priority of the subject in the group's curriculum
    private int requiredDurationInHours;  // required duration of the class in hours
    private int classesPerWeek; // expected number of classes per week for the group
    private int weeklyPatternIndex; // 0, 1, 2... indicates which of the N weekly classes this is (0 = first class of the week, 1 = second, etc.)

    @PlanningVariable
    @ManyToOne
    private Timeslot timeslot;

    @PlanningVariable
    @ManyToOne
    private Room room;

    // No-arg constructor required for Hibernate and OptaPlanner
    public Lesson() {
    }

    public Lesson(String subject, String teacher, String studentGroup) {
        this.subject = subject;
        this.teacher = teacher;
        this.studentGroup = studentGroup;
    }

    public Lesson(long id, String subject, String teacher, String studentGroup, Timeslot timeslot, Room room) {
        this(subject, teacher, studentGroup);
        this.id = id;
        this.timeslot = timeslot;
        this.room = room;
    }

    @Override
    public String toString() {
        return subject + "(" + id + ")";
    }

}
