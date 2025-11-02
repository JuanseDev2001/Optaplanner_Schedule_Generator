package org.acme.schooltimetabling.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.acme.schooltimetabling.dto.ScheduleGeneratorRequestDTO;
import org.acme.schooltimetabling.dto.ScheduleGeneratorResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "optaplanner.solver.termination.spent-limit=2m",
                "optaplanner.solver.termination.best-score-limit=0hard/*soft"
        })
public class ScheduleGeneratorIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGenerateScheduleWithRealData() throws IOException {
        // Charge JSON file
        File testRequestFile = new File("../../test-request.json");
        
        // Try alternative paths if not found
        if (!testRequestFile.exists()) {
            testRequestFile = new File("../../../test-request.json");
        }
        if (!testRequestFile.exists()) {
            testRequestFile = new File("test-request.json");
        }
        
        assertThat(testRequestFile.exists())
                .withFailMessage("No se encontró el archivo test-request.json")
                .isTrue();

        // Deserialize JSON to DTO
        ScheduleGeneratorRequestDTO request = objectMapper.readValue(testRequestFile, ScheduleGeneratorRequestDTO.class);

        // Validate that it loaded correctly
        System.out.println("=== REQUEST CARGADO ===");
        System.out.println("Planning ID: " + request.getPlanningId());
        System.out.println("Fecha inicio: " + request.getStartDate());
        System.out.println("Fecha fin: " + request.getEndDate());
        System.out.println("Número de grupos: " + request.getGroups().size());
        System.out.println("Horario semana: " + request.getWorkWeekConfig().getStartTime() + " - " + request.getWorkWeekConfig().getEndTime());
        System.out.println("Horario fin de semana: " + request.getWeekendConfig().getStartTime() + " - " + request.getWeekendConfig().getEndTime());
        
        request.getGroups().forEach(group -> {
            System.out.println("  Grupo: " + group.getGroupName());
            System.out.println("    Materia: " + group.getSubject().getSubjectName());
            System.out.println("    Profesor: " + group.getProfessor().getProfessorName());
            System.out.println("    Clases por semana: " + group.getClassesPerWeek());
            System.out.println("    Total de clases: " + group.getTotalClasses());
            System.out.println("    Restricciones del profesor: " + group.getProfessor().getRestrictions().size());
        });

        assertThat(request).isNotNull();
        assertThat(request.getPlanningId()).isEqualTo("plan1");
        assertThat(request.getGroups()).hasSize(5);

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create HTTP request
        HttpEntity<ScheduleGeneratorRequestDTO> httpRequest = new HttpEntity<>(request, headers);

        // Execute request
        System.out.println("\n=== EJECUTANDO GENERACIÓN DE HORARIO ===");
        long startTime = System.currentTimeMillis();

        ResponseEntity<ScheduleGeneratorResponseDTO> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/schedule",
                httpRequest,
                ScheduleGeneratorResponseDTO.class
        );
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Validate response
        System.out.println("\n=== RESPUESTA RECIBIDA ===");
        System.out.println("Status HTTP: " + response.getStatusCode());
        System.out.println("Tiempo de ejecución: " + duration + " ms (" + (duration / 1000.0) + " segundos)");

        // If there's an error, show the response body
        if (response.getStatusCode() != HttpStatus.OK) {
            System.out.println("\nERROR EN LA RESPUESTA:");
            System.out.println("Cuerpo de la respuesta: " + response.getBody());
            if (response.getBody() != null) {
                System.out.println("Status: " + response.getBody().getStatus());
                System.out.println("Score Explanation: " + response.getBody().getScoreExplanation());
            }
        }

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        ScheduleGeneratorResponseDTO responseBody = response.getBody();
        
        System.out.println("Planning ID: " + responseBody.getPlanningId());
        System.out.println("Status: " + responseBody.getStatus());
        System.out.println("Score Explanation: " + responseBody.getScoreExplanation());
        System.out.println("Número de clases programadas: " + 
                (responseBody.getScheduledClasses() != null ? responseBody.getScheduledClasses().size() : 0));

        // Validate response content
        assertThat(responseBody.getPlanningId()).isEqualTo("plan1");
        assertThat(responseBody.getStatus()).isIn("SOLVED", "FEASIBLE", "FAILED", "NOT_SOLVED");
        assertThat(responseBody.getScoreExplanation()).isNotNull();

        // Print scheduled classes details
        if (responseBody.getScheduledClasses() != null && !responseBody.getScheduledClasses().isEmpty()) {
            System.out.println("\n=== CLASES PROGRAMADAS ===");
            responseBody.getScheduledClasses().forEach(scheduledClass -> {
                System.out.println("Grupo ID: " + scheduledClass.getGroupId());
                System.out.println("  Formato: " + scheduledClass.getFormatTypeId());
                System.out.println("  Profesores: " + scheduledClass.getProfessorIds());
                System.out.println("  Horario: " + scheduledClass.getStartTime() + " - " + scheduledClass.getEndTime());
                System.out.println();
            });

            // Validate that classes were scheduled
            assertThat(responseBody.getScheduledClasses()).isNotEmpty();

            // Calculate expected total classes
            int expectedTotalClasses = request.getGroups().stream()
                    .mapToInt(group -> group.getTotalClasses())
                    .sum();
            
            System.out.println("Total de clases esperadas: " + expectedTotalClasses);
            System.out.println("Total de clases programadas: " + responseBody.getScheduledClasses().size());
            
            // Validate total number of scheduled classes
            assertThat(responseBody.getScheduledClasses().size()).isEqualTo(expectedTotalClasses);
        } else {
            System.out.println("\nADVERTENCIA: No se programaron lecciones");
        }

        // Print complete JSON response
        System.out.println("\n=== COMPLETE RESPONSE JSON ===");
        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseBody));
    }
}
