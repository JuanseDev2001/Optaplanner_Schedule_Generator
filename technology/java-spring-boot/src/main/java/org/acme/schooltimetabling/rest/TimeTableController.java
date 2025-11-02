package org.acme.schooltimetabling.rest;

import org.acme.schooltimetabling.domain.TimeTable;
import org.acme.schooltimetabling.dto.ScheduleGeneratorRequestDTO;
import org.acme.schooltimetabling.dto.ScheduleGeneratorResponseDTO;
import org.acme.schooltimetabling.service.TimeTableService;
import org.optaplanner.core.api.solver.SolverManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = "*")
public class TimeTableController {

    @Autowired
    private SolverManager<TimeTable, Long> solverManager;
    
    @Autowired
    private TimeTableService timeTableService;

    /**
     * Endpoint principal para generar horarios automáticamente
     * @param request DTO con la información de materias, profesores, grupos, restricciones, etc.
     * @return DTO con el horario generado y la información de asignaciones
     */
    @PostMapping("")
    public ResponseEntity<ScheduleGeneratorResponseDTO> generateSchedule(@RequestBody ScheduleGeneratorRequestDTO request) {
        Long problemId = null;
        try {
            // Convert request DTO to TimeTable problem
            TimeTable problem = timeTableService.convertRequestToTimeTable(request);

            // Create a unique ID for this problem
            problemId = System.currentTimeMillis();

            // Solve the problem synchronously and wait for the result
            TimeTable solution = solverManager.solve(problemId, problem).getFinalBestSolution();

            // Terminate the solver explicitly to free resources
            solverManager.terminateEarly(problemId);

            // Convert solution to response DTO
            ScheduleGeneratorResponseDTO response = timeTableService.convertTimeTableToResponse(
                request.getPlanningId(), solution);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            // Ensure to terminate the solver even in case of error
            if (problemId != null) {
                try {
                    solverManager.terminateEarly(problemId);
                } catch (Exception ex) {
                    // Ignore errors when terminating
                }
            }
            ScheduleGeneratorResponseDTO errorResponse = new ScheduleGeneratorResponseDTO();
            errorResponse.setPlanningId(request.getPlanningId());
            errorResponse.setStatus("ERROR");
            errorResponse.setScoreExplanation("Error al generar el horario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}
