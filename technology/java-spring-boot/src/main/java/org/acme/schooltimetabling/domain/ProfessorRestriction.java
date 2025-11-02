package org.acme.schooltimetabling.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
public class ProfessorRestriction {
    
    @Id @GeneratedValue
    private Long id;
    
    private String professorId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;

    public ProfessorRestriction() {
    }

    public ProfessorRestriction(String professorId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, String reason) {
        this.professorId = professorId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
    }

    // Method to check if a given timeslot conflicts with this restriction
    public boolean conflictsWith(Timeslot timeslot) {
        if (timeslot.getDayOfWeek() != this.dayOfWeek) {
            return false;
        }

        // Check if there is a time overlap
        return timeslot.getStartTime().isBefore(this.endTime) &&
               timeslot.getEndTime().isAfter(this.startTime);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public String getProfessorId() {
        return professorId;
    }

    public void setProfessorId(String professorId) {
        this.professorId = professorId;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
