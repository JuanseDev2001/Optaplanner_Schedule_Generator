# Configuración del Servicio OptaPlanner para Planeación de Maestrías

## Estado del Proyecto
✅ **Servicio REST configurado y corriendo**

## Información del Servidor
- **Puerto**: 8082
- **URL Base**: `http://localhost:8082`

## Endpoint para tu Monolito

### POST - Generar Horario
**URL**: `http://localhost:8082/timeTable/api/schedule`

**Método**: POST

**Headers**:
```
Content-Type: application/json
```

**Body (ejemplo)**:
```json
{
  "planningId": "plan1",
  "workWeekConfig": {
    "startTime": "19:00",
    "endTime": "22:00"
  },
  "weekendConfig": {
    "startTime": "07:00",
    "endTime": "12:00"
  },
  "groups": [
    {
      "groupId": "group1",
      "groupName": "Grupo 1",
      "semester": 1,
      "subject": {
        "subjectId": "subj1",
        "subjectName": "Materia 1",
        "semester": 1,
        "position": 1
      },
      "professor": {
        "professorId": "prof1",
        "professorName": "Profesor 1",
        "restrictions": [
          {
            "dayOfWeek": "1",
            "startTime": "16:00",
            "endTime": "18:00",
            "reason": "Recurrente"
          }
        ]
      },
      "classesPerWeek": 3,
      "totalClasses": 11
    }
  ]
}
```

**Respuesta Exitosa (200 OK)**:
```json
{
  "planningId": "plan1",
  "status": "SOLVED",
  "scoreExplanation": "0hard/0soft",
  "scheduledLessons": [
    {
      "groupId": null,
      "groupName": "Grupo 1",
      "subjectId": null,
      "subjectName": "Materia 1",
      "professorId": null,
      "professorName": "Profesor 1",
      "semester": 0,
      "dayOfWeek": "MONDAY",
      "startTime": "19:00",
      "endTime": "21:00",
      "room": "Sala 1"
    }
  ]
}
```

**Respuesta de Error (500)**:
```json
{
  "planningId": "plan1",
  "status": "ERROR",
  "scoreExplanation": "Error al generar el horario: [mensaje de error]",
  "scheduledLessons": null
}
```

## Cómo Probar desde tu Monolito

### Usando RestTemplate (Spring):
```java
RestTemplate restTemplate = new RestTemplate();
String url = "http://localhost:8082/timeTable/api/schedule";
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);

HttpEntity<AutoScheduleRequestDTO> request = new HttpEntity<>(requestDTO, headers);
ResponseEntity<AutoScheduleResponseDTO> response = restTemplate.postForEntity(url, request, AutoScheduleResponseDTO.class);
```

### Usando WebClient (Spring WebFlux):
```java
WebClient webClient = WebClient.create("http://localhost:8082");
AutoScheduleResponseDTO response = webClient.post()
    .uri("/timeTable/api/schedule")
    .contentType(MediaType.APPLICATION_JSON)
    .bodyValue(requestDTO)
    .retrieve()
    .bodyToMono(AutoScheduleResponseDTO.class)
    .block();
```

### Usando cURL (para pruebas):
```bash
curl -X POST http://localhost:8082/timeTable/api/schedule \
  -H "Content-Type: application/json" \
  -d '{
    "planningId": "plan1",
    "workWeekConfig": {
      "startTime": "19:00",
      "endTime": "22:00"
    },
    "weekendConfig": {
      "startTime": "07:00",
      "endTime": "12:00"
    },
    "groups": [...]
  }'
```

## Configuración Actual

### Configuración de Timeslots
- **Entre semana (Lunes-Viernes)**: Usa `workWeekConfig`
- **Fin de semana (Sábado-Domingo)**: Usa `weekendConfig`
- **Duración de slots**: 2 horas por clase (configurable en TimeTableService.java)

### CORS
- CORS habilitado para todos los orígenes (*)
- Métodos permitidos: GET, POST, PUT, DELETE, OPTIONS

### Solver Configuration
- **Tiempo de ejecución**: 30 segundos (configurado en application.properties)
- **Modo**: Sincrónico (espera hasta que termine la optimización)

## Archivos Creados/Modificados

### DTOs Creados:
1. `AutoScheduleRequestDTO.java` - DTO principal de entrada
2. `TimeSlotConstraintDTO.java` - Configuración de horarios
3. `GroupScheduleDTO.java` - Información de grupos
4. `SubjectInfoDTO.java` - Información de materias
5. `ProfessorInfoDTO.java` - Información de profesores
6. `RestrictionInfoDTO.java` - Restricciones de profesores
7. `AutoScheduleResponseDTO.java` - DTO de respuesta
8. `ScheduledLessonDTO.java` - Clases programadas

### Servicios Creados:
1. `TimeTableService.java` - Conversión entre DTOs y entidades OptaPlanner

### Configuración:
1. `CorsConfig.java` - Configuración CORS
2. `application.properties` - Puerto 8082

### Controladores Modificados:
1. `TimeTableController.java` - Nuevo endpoint `/api/schedule`

## Próximos Pasos (Opcionales)

1. **Validación**: Agregar validaciones @Valid en los DTOs
2. **Restricciones**: Implementar las restricciones de profesores en el solver
3. **Persistencia**: Guardar los resultados en base de datos
4. **Asincrónico**: Convertir el endpoint a modo asíncrono para planes grandes
5. **Logging**: Mejorar el logging para debugging

## Notas Importantes

- El servicio genera automáticamente salas genéricas (Sala 1, Sala 2, etc.)
- Los timeslots se generan en bloques de 2 horas
- El solver corre por 30 segundos por defecto
- Las restricciones de profesores están en el JSON pero aún no se procesan en el solver
