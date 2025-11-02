package org.acme.schooltimetabling.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.Data;

import org.optaplanner.core.api.domain.lookup.PlanningId;

@Entity
@Data
public class Room {

    @PlanningId
    @Id @GeneratedValue
    private Long id;

    private String name;

    public Room() {
    }

    public Room(String name) {
        this.name = name;
    }

    public Room(long id, String name) {
        this(name);
        this.id = id;
    }

}
