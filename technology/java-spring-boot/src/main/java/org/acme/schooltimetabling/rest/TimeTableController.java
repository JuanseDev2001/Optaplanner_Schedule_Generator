package org.acme.schooltimetabling.rest;

import org.acme.schooltimetabling.domain.TimeTable;
import org.acme.schooltimetabling.dto.AutoScheduleRequestDTO;
import org.acme.schooltimetabling.dto.AutoScheduleResponseDTO;
import org.acme.schooltimetabling.persistence.TimeTableRepository;
import org.acme.schooltimetabling.service.TimeTableService;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.solver.SolutionManager;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/timeTable")
@CrossOrigin(origins = "*")
public class TimeTableController {

    @Autowired
    private TimeTableRepository timeTableRepository;
    @Autowired
    private SolverManager<TimeTable, Long> solverManager;
    @Autowired
    private SolutionManager<TimeTable, HardSoftScore> solutionManager;
    @Autowired
    private TimeTableService timeTableService;

    @GetMapping()
    public TimeTable getTimeTable() {
        SolverStatus solverStatus = getSolverStatus();
        TimeTable solution = timeTableRepository.findById(TimeTableRepository.SINGLETON_TIME_TABLE_ID);
        solutionManager.update(solution); // Sets the score
        solution.setSolverStatus(solverStatus);
        return solution;
    }

    @PostMapping("/solve")
    public void solve() {
        solverManager.solveAndListen(TimeTableRepository.SINGLETON_TIME_TABLE_ID,
                timeTableRepository::findById,
                timeTableRepository::save);
    }

    public SolverStatus getSolverStatus() {
        return solverManager.getSolverStatus(TimeTableRepository.SINGLETON_TIME_TABLE_ID);
    }

    @PostMapping("/stopSolving")
    public void stopSolving() {
        solverManager.terminateEarly(TimeTableRepository.SINGLETON_TIME_TABLE_ID);
    }

    // New endpoint for generating schedule from request DTO
    @PostMapping("/api/schedule")
    public ResponseEntity<AutoScheduleResponseDTO> generateSchedule(@RequestBody AutoScheduleRequestDTO request) {
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
            AutoScheduleResponseDTO response = timeTableService.convertTimeTableToResponse(
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
            AutoScheduleResponseDTO errorResponse = new AutoScheduleResponseDTO();
            errorResponse.setPlanningId(request.getPlanningId());
            errorResponse.setStatus("ERROR");
            errorResponse.setScoreExplanation("Error al generar el horario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}
