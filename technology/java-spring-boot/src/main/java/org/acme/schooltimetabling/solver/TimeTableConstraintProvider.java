package org.acme.schooltimetabling.solver;

import java.time.Duration;
import java.time.temporal.WeekFields;
import java.util.List;

import org.optaplanner.core.api.score.stream.ConstraintCollectors;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;

import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.domain.ProfessorRestriction;

public class TimeTableConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints
                roomConflict(constraintFactory),
                teacherConflict(constraintFactory),
                studentGroupConflict(constraintFactory),
                professorAvailability(constraintFactory),
                lessonDurationMatch(constraintFactory),
                teacherTimeSlotOverlap(constraintFactory),
                roomTimeSlotOverlap(constraintFactory),
                studentGroupTimeSlotOverlap(constraintFactory),
                groupDailyLimit(constraintFactory),
                groupWeeklyLimit(constraintFactory),
                weeklyPatternConsistency(constraintFactory),
                positionBasedPrecedence(constraintFactory),
                // Soft constraints
                teacherRoomStability(constraintFactory),
                teacherTimeEfficiency(constraintFactory),
                studentGroupSubjectVariety(constraintFactory)
        };
    }

    Constraint roomConflict(ConstraintFactory constraintFactory) {
        // A room can accommodate at most one lesson at the same time.
        return constraintFactory
                // Select each pair of 2 different lessons ...
                .forEachUniquePair(Lesson.class,
                        // ... in the same timeslot ...
                        Joiners.equal(Lesson::getTimeslot),
                        // ... in the same room ...
                        Joiners.equal(Lesson::getRoom))
                // ... and penalize each pair with a hard weight.
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Room conflict");
    }

    Constraint teacherConflict(ConstraintFactory constraintFactory) {
        // A teacher can teach at most one lesson at the same time.
        // Verificar si algún profesor está en ambas lecciones al mismo tiempo
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeslot))
                .filter((lesson1, lesson2) -> {
                    if (lesson1.getProfessorIds() == null || lesson2.getProfessorIds() == null) {
                        return false;
                    }
                    // Verificar si hay algún profesor en común entre las dos lecciones
                    return lesson1.getProfessorIds().stream()
                            .anyMatch(profId -> lesson2.getProfessorIds().contains(profId));
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher conflict");
    }

    Constraint studentGroupConflict(ConstraintFactory constraintFactory) {
        // A student can attend at most one lesson at the same time.
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getTimeslot),
                        Joiners.equal(Lesson::getStudentGroup))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Student group conflict");
    }

    Constraint professorAvailability(ConstraintFactory constraintFactory) {
        // A professor cannot teach during their restricted times.
        // Verificar las restricciones para TODOS los profesores de cada lección
        return constraintFactory
                .forEach(Lesson.class)
                .join(ProfessorRestriction.class)
                .filter((lesson, restriction) -> {
                    if (lesson.getTimeslot() == null || lesson.getProfessorIds() == null) {
                        return false;
                    }
                    // Verificar si alguno de los profesores de la lección tiene esta restricción
                    // y si el timeslot conflictúa con ella
                    return lesson.getProfessorIds().contains(restriction.getProfessorId())
                            && restriction.conflictsWith(lesson.getTimeslot());
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Professor availability");
    }

    Constraint teacherRoomStability(ConstraintFactory constraintFactory) {
        // A teacher prefers to teach in a single room.
        // Verificar estabilidad de sala para profesores compartidos
        return constraintFactory
                .forEachUniquePair(Lesson.class)
                .filter((lesson1, lesson2) -> {
                    if (lesson1.getRoom() == null || lesson2.getRoom() == null
                            || lesson1.getProfessorIds() == null || lesson2.getProfessorIds() == null) {
                        return false;
                    }
                    // Verificar si hay algún profesor en común y están en salas diferentes
                    boolean hasCommonProfessor = lesson1.getProfessorIds().stream()
                            .anyMatch(profId -> lesson2.getProfessorIds().contains(profId));
                    
                    return hasCommonProfessor && !lesson1.getRoom().equals(lesson2.getRoom());
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Teacher room stability");
    }

    Constraint teacherTimeEfficiency(ConstraintFactory constraintFactory) {
        // A teacher prefers to teach sequential lessons and dislikes gaps between lessons.
        // Recompensar cuando profesores compartidos tienen lecciones cercanas
        return constraintFactory
                .forEach(Lesson.class)
                .join(Lesson.class,
                        Joiners.equal((lesson) -> lesson.getTimeslot() == null ? null : lesson.getTimeslot().getDayOfWeek()))
                .filter((lesson1, lesson2) -> {
                    if (lesson1.getTimeslot() == null || lesson2.getTimeslot() == null
                            || lesson1.getProfessorIds() == null || lesson2.getProfessorIds() == null) {
                        return false;
                    }
                    
                    // Verificar si hay algún profesor en común
                    boolean hasCommonProfessor = lesson1.getProfessorIds().stream()
                            .anyMatch(profId -> lesson2.getProfessorIds().contains(profId));
                    
                    if (!hasCommonProfessor) {
                        return false;
                    }
                    
                    Duration between = Duration.between(lesson1.getTimeslot().getEndTime(),
                            lesson2.getTimeslot().getStartTime());
                    return !between.isNegative() && between.compareTo(Duration.ofMinutes(30)) <= 0;
                })
                .reward(HardSoftScore.ONE_SOFT)
                .asConstraint("Teacher time efficiency");
    }

    Constraint studentGroupSubjectVariety(ConstraintFactory constraintFactory) {
        // A student group dislikes sequential lessons on the same subject.
        return constraintFactory
                .forEach(Lesson.class)
                .join(Lesson.class,
                        Joiners.equal(Lesson::getSubject),
                        Joiners.equal(Lesson::getStudentGroup),
                        Joiners.equal((lesson) -> lesson.getTimeslot().getDayOfWeek()))
                .filter((lesson1, lesson2) -> {
                    Duration between = Duration.between(lesson1.getTimeslot().getEndTime(),
                            lesson2.getTimeslot().getStartTime());
                    return !between.isNegative() && between.compareTo(Duration.ofMinutes(30)) <= 0;
                })
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Student group subject variety");
    }

    Constraint lessonDurationMatch(ConstraintFactory constraintFactory) {
        // A lesson must be assigned to a timeslot with the correct duration
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> {
                    if (lesson.getTimeslot() == null) {
                        return false;
                    }
                    Duration timeslotDuration = Duration.between(
                            lesson.getTimeslot().getStartDateTime(),
                            lesson.getTimeslot().getEndDateTime());
                    long timeslotHours = timeslotDuration.toHours();
                    return timeslotHours != lesson.getRequiredDurationInHours();
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Lesson duration match");
    }

    Constraint teacherTimeSlotOverlap(ConstraintFactory constraintFactory) {
        // A teacher cannot have overlapping classes (not just same timeslot, but any overlap)
        // Verificar solapamiento de tiempo para profesores compartidos
        return constraintFactory
                .forEachUniquePair(Lesson.class)
                .filter((lesson1, lesson2) -> {
                    if (lesson1.getTimeslot() == null || lesson2.getTimeslot() == null 
                            || lesson1.getProfessorIds() == null || lesson2.getProfessorIds() == null) {
                        return false;
                    }
                    // Verificar si hay algún profesor en común
                    boolean hasCommonProfessor = lesson1.getProfessorIds().stream()
                            .anyMatch(profId -> lesson2.getProfessorIds().contains(profId));
                    
                    if (!hasCommonProfessor) {
                        return false;
                    }
                    
                    // Verificar si los timeslots se solapan en tiempo
                    return lesson1.getTimeslot().getStartDateTime().isBefore(lesson2.getTimeslot().getEndDateTime())
                            && lesson2.getTimeslot().getStartDateTime().isBefore(lesson1.getTimeslot().getEndDateTime());
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher timeslot overlap");
    }

    Constraint roomTimeSlotOverlap(ConstraintFactory constraintFactory) {
        // A room cannot have overlapping classes
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getRoom))
                .filter((lesson1, lesson2) -> {
                    if (lesson1.getTimeslot() == null || lesson2.getTimeslot() == null) {
                        return false;
                    }
                    // Verificar si los timeslots se solapan en tiempo
                    return lesson1.getTimeslot().getStartDateTime().isBefore(lesson2.getTimeslot().getEndDateTime())
                            && lesson2.getTimeslot().getStartDateTime().isBefore(lesson1.getTimeslot().getEndDateTime());
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Room timeslot overlap");
    }

    Constraint studentGroupTimeSlotOverlap(ConstraintFactory constraintFactory) {
        // A student group cannot have overlapping classes
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getStudentGroup))
                .filter((lesson1, lesson2) -> {
                    if (lesson1.getTimeslot() == null || lesson2.getTimeslot() == null) {
                        return false;
                    }
                    // Verificar si los timeslots se solapan en tiempo
                    return lesson1.getTimeslot().getStartDateTime().isBefore(lesson2.getTimeslot().getEndDateTime())
                            && lesson2.getTimeslot().getStartDateTime().isBefore(lesson1.getTimeslot().getEndDateTime());
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Student group timeslot overlap");
    }

    Constraint groupDailyLimit(ConstraintFactory constraintFactory) {
        // A group can have at most one lesson per calendar day
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroupId),
                        Joiners.equal(lesson -> lesson.getTimeslot() == null ? null : lesson.getTimeslot().getStartDateTime().toLocalDate()))
                .filter((lesson1, lesson2) -> {
                    if (lesson1.getTimeslot() == null || lesson2.getTimeslot() == null) {
                        return false;
                    }
                    return lesson1.getTimeslot().getStartDateTime().toLocalDate()
                            .equals(lesson2.getTimeslot().getStartDateTime().toLocalDate());
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Group one lesson per day");
    }

    Constraint groupWeeklyLimit(ConstraintFactory constraintFactory) {
        // A group should not have more lessons in a week than classesPerWeek
        return constraintFactory
                .forEach(Lesson.class)
                .filter(lesson -> lesson.getTimeslot() != null)
                .groupBy(
                        Lesson::getGroupId,
                        lesson -> lesson.getTimeslot().getStartDateTime().get(WeekFields.ISO.weekOfWeekBasedYear()),
                        ConstraintCollectors.toList())
                .filter((groupId, week, lessons) -> !lessons.isEmpty() && lessons.size() > lessons.get(0).getClassesPerWeek())
                .penalize(HardSoftScore.ONE_HARD, (groupId, week, lessons) -> lessons.size() - lessons.get(0).getClassesPerWeek())
                .asConstraint("Group weekly class limit");
    }

    Constraint weeklyPatternConsistency(ConstraintFactory constraintFactory) {
        // Lessons with the same groupId and weeklyPatternIndex must be scheduled 
        // at the same day of week and time across all weeks
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.equal(Lesson::getGroupId),
                        Joiners.equal(Lesson::getWeeklyPatternIndex))
                .filter((lesson1, lesson2) -> {
                    if (lesson1.getTimeslot() == null || lesson2.getTimeslot() == null) {
                        return false;
                    }
                    // Check if they are on different days of week OR different times
                    boolean sameDayOfWeek = lesson1.getTimeslot().getDayOfWeek()
                            .equals(lesson2.getTimeslot().getDayOfWeek());
                    boolean sameTime = lesson1.getTimeslot().getStartDateTime().toLocalTime()
                            .equals(lesson2.getTimeslot().getStartDateTime().toLocalTime());
                    
                    // Penalize if NOT both same day and same time
                    return !(sameDayOfWeek && sameTime);
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Weekly pattern consistency");
    }

    Constraint positionBasedPrecedence(ConstraintFactory constraintFactory) {
        // Groups with higher position values cannot start until ALL lessons 
        // from lower position groups are completed
        return constraintFactory
                .forEachUniquePair(Lesson.class,
                        Joiners.lessThan(Lesson::getSubjectPosition))
                .filter((lowerPositionLesson, higherPositionLesson) -> {
                    if (lowerPositionLesson.getTimeslot() == null || higherPositionLesson.getTimeslot() == null) {
                        return false;
                    }
                    // Penalize if higher position lesson starts before lower position lesson ends
                    return higherPositionLesson.getTimeslot().getStartDateTime()
                            .isBefore(lowerPositionLesson.getTimeslot().getEndDateTime());
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Position based precedence");
    }

}
