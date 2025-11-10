package org.acme.schooltimetabling.service;

import org.acme.schooltimetabling.domain.Lesson;
import org.acme.schooltimetabling.domain.Room;
import org.acme.schooltimetabling.domain.TimeTable;
import org.acme.schooltimetabling.domain.Timeslot;
import org.acme.schooltimetabling.domain.ProfessorRestriction;
import org.acme.schooltimetabling.dto.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TimeTableService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TimeTable convertRequestToTimeTable(ScheduleGeneratorRequestDTO request) {
        // Obtener las duraciones únicas requeridas
        Set<Integer> requiredDurations = request.getGroups().stream()
            .map(GroupScheduleDTO::getClassDurationInHours)
            .collect(Collectors.toSet());
        
        // Generar timeslots basados en la configuración de horarios con fechas reales
        List<Timeslot> timeslots = generateTimeslotsWithRealDates(
            request.getStartDate(), 
            request.getEndDate(),
            request.getWorkWeekConfig(), 
            request.getWeekendConfig(),
            requiredDurations
        );
        
        // Generar salas genéricas
        List<Room> rooms = generateRooms();
        
        // Generar restricciones de profesores
        List<ProfessorRestriction> professorRestrictions = generateProfessorRestrictions(request.getGroups());
        
        // Generar lecciones ordenadas por prioridad (position)
        List<Lesson> lessons = generateLessonsWithPriority(request.getGroups());
        
        return new TimeTable(timeslots, rooms, professorRestrictions, lessons);
    }

    private List<Timeslot> generateTimeslotsWithRealDates(
            LocalDate startDate, 
            LocalDate endDate,
            TimeSlotConstraintDTO workWeekConfig, 
            TimeSlotConstraintDTO weekendConfig,
            Set<Integer> requiredDurations) {
        
        List<Timeslot> timeslots = new ArrayList<>();
        long timeslotIdCounter = 1; // Contador para IDs únicos
        
        // Parsear las franjas horarias
        LocalTime workStartTime = LocalTime.parse(workWeekConfig.getStartTime(), TIME_FORMATTER);
        LocalTime workEndTime = LocalTime.parse(workWeekConfig.getEndTime(), TIME_FORMATTER);
        
        LocalTime weekendStartTime = null;
        LocalTime weekendEndTime = null;
        if (weekendConfig != null) {
            weekendStartTime = LocalTime.parse(weekendConfig.getStartTime(), TIME_FORMATTER);
            weekendEndTime = LocalTime.parse(weekendConfig.getEndTime(), TIME_FORMATTER);
        }
        
        // Iterar sobre cada día en el rango de fechas (desde startDate hasta endDate)
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            
            // Determinar qué configuración usar según el día
            LocalTime dayStartTime;
            LocalTime dayEndTime;
            
            if (dayOfWeek == DayOfWeek.SUNDAY) {
                // Domingo: NO se programan clases, saltar al siguiente día
                currentDate = currentDate.plusDays(1);
                continue;
            } else if (dayOfWeek == DayOfWeek.SATURDAY) {
                // Sábado: usar weekendConfig
                if (weekendConfig == null) {
                    // Si no hay configuración de fin de semana, saltar al siguiente día
                    currentDate = currentDate.plusDays(1);
                    continue;
                }
                dayStartTime = weekendStartTime;
                dayEndTime = weekendEndTime;
            } else {
                // Lunes a Viernes: usar workWeekConfig
                dayStartTime = workStartTime;
                dayEndTime = workEndTime;
            }
            
            // Generar slots dentro de la franja horaria de este día
            // Solo generamos slots de las duraciones que realmente se necesitan
            timeslotIdCounter = generateSlotsForDay(timeslots, currentDate, dayStartTime, dayEndTime, timeslotIdCounter, requiredDurations);
            
            currentDate = currentDate.plusDays(1);
        }
        
        return timeslots;
    }

    private long generateSlotsForDay(List<Timeslot> timeslots, LocalDate date, LocalTime dayStartTime, LocalTime dayEndTime, long idCounter, Set<Integer> requiredDurations) {
        // Generar slots solo de las duraciones que realmente se necesitan
        
        LocalTime currentTime = dayStartTime;
        
        while (currentTime.isBefore(dayEndTime)) {
            // Generar slots para cada duración requerida
            for (Integer durationHours : requiredDurations) {
                LocalTime endTime = currentTime.plusHours(durationHours);
                if (!endTime.isAfter(dayEndTime)) {
                    idCounter = addTimeslot(timeslots, date, currentTime, endTime, idCounter);
                }
            }
            
            // Avanzar 1 hora para el siguiente conjunto de slots
            currentTime = currentTime.plusHours(1);
        }
        
        return idCounter;
    }

    private long addTimeslot(List<Timeslot> timeslots, LocalDate date, LocalTime startTime, LocalTime endTime, long id) {
        // Convertir LocalDate + LocalTime a LocalDateTime
        LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
        LocalDateTime endDateTime = LocalDateTime.of(date, endTime);
        
        // Crear timeslot con ID
        Timeslot timeslot = new Timeslot(startDateTime, endDateTime);
        // Usar reflexión para asignar el ID, ya que no hay setter público
        try {
            java.lang.reflect.Field idField = Timeslot.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(timeslot, id);
        } catch (Exception e) {
            throw new RuntimeException("Error al asignar ID al timeslot", e);
        }
        
        timeslots.add(timeslot);
        return id + 1;
    }

    private List<Room> generateRooms() {
        List<Room> rooms = new ArrayList<>();
        // Generar salas genéricas (suficientes para todos los grupos)
        for (int i = 1; i <= 10; i++) {
            rooms.add(new Room((long) i, "Sala " + i));
        }
        return rooms;
    }

    private List<ProfessorRestriction> generateProfessorRestrictions(List<GroupScheduleDTO> groups) {
        List<ProfessorRestriction> restrictions = new ArrayList<>();
        
        for (GroupScheduleDTO group : groups) {
            // Iterar sobre todos los profesores del grupo
            if (group.getProfessors() != null && !group.getProfessors().isEmpty()) {
                for (ProfessorInfoDTO professor : group.getProfessors()) {
                    if (professor.getRestrictions() != null) {
                        for (RestrictionInfoDTO restrictionDTO : professor.getRestrictions()) {
                            // Convertir dayOfWeek de String a DayOfWeek
                            // Asumiendo que "1" = MONDAY, "2" = TUESDAY, etc.
                            int dayNum = Integer.parseInt(restrictionDTO.getDayOfWeek());
                            DayOfWeek dayOfWeek = DayOfWeek.of(dayNum);
                            
                            LocalTime startTime = LocalTime.parse(restrictionDTO.getStartTime(), TIME_FORMATTER);
                            LocalTime endTime = LocalTime.parse(restrictionDTO.getEndTime(), TIME_FORMATTER);
                            
                            ProfessorRestriction restriction = new ProfessorRestriction(
                                professor.getProfessorId(),
                                dayOfWeek,
                                startTime,
                                endTime,
                                restrictionDTO.getReason()
                            );
                            
                            restrictions.add(restriction);
                        }
                    }
                }
            }
        }
        
        return restrictions;
    }

    private List<Lesson> generateLessonsWithPriority(List<GroupScheduleDTO> groups) {
        List<Lesson> lessons = new ArrayList<>();
        long lessonIdCounter = 1; // Contador para IDs únicos
        
        // Ordenar grupos por position del subject (prioridad)
        List<GroupScheduleDTO> sortedGroups = groups.stream()
            .sorted(Comparator.comparingInt(g -> g.getSubject().getPosition()))
            .collect(Collectors.toList());
        
        for (GroupScheduleDTO group : sortedGroups) {
            // Crear nombres de profesores concatenados para el campo teacher
            String teacherNames = "";
            List<String> professorIdsList = new ArrayList<>();
            
            if (group.getProfessors() != null && !group.getProfessors().isEmpty()) {
                teacherNames = group.getProfessors().stream()
                    .map(ProfessorInfoDTO::getProfessorName)
                    .collect(Collectors.joining(", "));
                
                professorIdsList = group.getProfessors().stream()
                    .map(ProfessorInfoDTO::getProfessorId)
                    .collect(Collectors.toList());
            }
            
            // Crear lecciones según totalClasses
            for (int i = 0; i < group.getTotalClasses(); i++) {
                Lesson lesson = new Lesson(
                    lessonIdCounter++,  // Asignar ID único
                    group.getSubject().getSubjectName(),
                    teacherNames,
                    group.getGroupName(),
                    null,  // timeslot se asignará por OptaPlanner
                    null   // room se asignará por OptaPlanner
                );
                
                // Agregar información adicional necesaria para la respuesta
                lesson.setGroupId(group.getGroupId());
                lesson.setProfessorIds(new ArrayList<>(professorIdsList)); // Asignar lista de IDs de profesores
                lesson.setFormatTypeId(group.getFormatTypeId());
                lesson.setSubjectPosition(group.getSubject().getPosition());
                lesson.setRequiredDurationInHours(group.getClassDurationInHours());
                lesson.setClassesPerWeek(group.getClassesPerWeek());
                
                // Asignar índice del patrón semanal: qué clase de la semana es (0, 1, 2...)
                // Por ejemplo: si classesPerWeek=2, las lecciones 0,2,4,6... tienen index 0
                // y las lecciones 1,3,5,7... tienen index 1
                lesson.setWeeklyPatternIndex(i % group.getClassesPerWeek());
                
                lessons.add(lesson);
            }
        }
        
        return lessons;
    }

    public ScheduleGeneratorResponseDTO convertTimeTableToResponse(String planningId, TimeTable timeTable) {
        List<ScheduledClassDTO> scheduledClasses = new ArrayList<>();
        
        for (Lesson lesson : timeTable.getLessonList()) {
            if (lesson.getTimeslot() != null && lesson.getRoom() != null) {
                Timeslot timeslot = lesson.getTimeslot();
                
                // Crear el DTO en el formato esperado
                ScheduledClassDTO classDTO = new ScheduledClassDTO();
                
                // Convertir LocalDateTime a String en formato ISO 8601
                classDTO.setStartTime(timeslot.getStartDateTime().format(ISO_FORMATTER));
                classDTO.setEndTime(timeslot.getEndDateTime().format(ISO_FORMATTER));
                classDTO.setGroupId(lesson.getGroupId());
                classDTO.setFormatTypeId(lesson.getFormatTypeId());
                
                // Usar la lista de IDs de profesores de la lección
                classDTO.setProfessorIds(lesson.getProfessorIds());
                
                scheduledClasses.add(classDTO);
            }
        }
        
        // Determinar el status basado en el hard score
        String status;
        String scoreExplanation;
        
        if (timeTable.getScore() == null) {
            status = "NOT_SOLVED";
            scoreExplanation = "No score available";
        } else {
            scoreExplanation = timeTable.getScore().toString();
            // Si el hard score es negativo, hay violaciones de restricciones duras
            if (timeTable.getScore().getHardScore() < 0) {
                status = "FAILED";
            } else {
                status = "SOLVED";
            }
        }

        return new ScheduleGeneratorResponseDTO(planningId, status, scoreExplanation, scheduledClasses);
    }
}