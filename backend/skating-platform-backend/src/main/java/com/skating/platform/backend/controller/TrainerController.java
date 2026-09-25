package com.skating.platform.backend.controller;


import com.skating.platform.backend.dto.trainer.request.CreateTrainerRequest;
import com.skating.platform.backend.dto.trainer.response.TrainerResponse;
import com.skating.platform.backend.dto.trainer.request.UpdateTrainerRequest;
import com.skating.platform.backend.dto.trainerService.response.TrainerServiceResponse;
import com.skating.platform.backend.service.TrainerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Trainers", description = "Управление тренерами и их услугами")

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {
    private final TrainerService service;

    public TrainerController(TrainerService service){
        this.service = service;
    }



    @Operation (summary = "Получить список тренеров", description =  "Возвращает тренеров с пагинацией и сортировкой")
    @GetMapping
    public Page<TrainerResponse> getAllTrainers(Pageable pageable){
        return service.getAllTrainers(pageable);
    }

    @Operation (summary = "Создать тренера", description =  "Создает тренера")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Тренер создан"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные тренера"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Тренер с таким номером телефона уже существует"
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainerResponse createTrainer (
            @Valid @RequestBody CreateTrainerRequest request
    ){
       return service.createTrainer(request);
    }

    @Operation (summary = "Получить тренера по ID", description =  "Возвращает тренера по его идентификатору")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Тренер найден"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Тренер не найден"
            )
    })
    @GetMapping("/{trainerId}")
    public TrainerResponse getTrainerById (@PathVariable Long trainerId){
        return service.getTrainerById(trainerId);
    }

    @Operation (summary = "Обновить данные тренера по ID", description =  "Обновляет поля тренера по его идентификатору")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Тренер найден и успешно обновлен"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные для обновления тренера"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Тренер не найден"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Указанный номер телефона уже используется другим тренером"
            )
    })
    @PutMapping("/{trainerId}")
    public TrainerResponse updateTrainer(@PathVariable Long trainerId, @Valid @RequestBody UpdateTrainerRequest request){
        return service.updateTrainer(trainerId, request);
    }

    @Operation (summary = "Удалить тренера по ID", description =  "Удаляет тренера по его идентификатору")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Тренер успешно удален"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Тренер не найден"
            )
    })
    @DeleteMapping("/{trainerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrainer (@PathVariable Long trainerId){
        service.deleteTrainer(trainerId);
    }

    @Operation (summary = "Добавление тренеру услуги по ID тренера и ID сервиса", description =  "Добавляет тренеру услугу")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Тренеру успешно добавлена услуга"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Тренер не найден/услуга не найдена"
            )
    })
    @PostMapping("/{trainerId}/services/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addServiceToTrainer (@PathVariable Long trainerId, @PathVariable Long serviceId){
        service.addServiceToTrainer(trainerId, serviceId);
    }

    @Operation (summary = "Вывод всех услуг тренеров", description =  "Выводит все услуги тренеров с пагинацией")
    @GetMapping("/services")
    public Page<TrainerServiceResponse> getAllTrainersServices (Pageable pageable){
        return service.getAllTrainersServices(pageable);
    }

    @Operation (summary = "Вывод всех услуг тренера по ID", description =  "Выводит все услуги тренера")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "успешный вывод"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Тренер не найден"
            )
    })
    @GetMapping("/{trainerId}/services")
    public TrainerServiceResponse getTrainerServicesById (@PathVariable Long trainerId){
        return service.getTrainerServiceById(trainerId);
    }

    @Operation (summary = "Получить тренеров с услугами", description =  "Возвращает всех тренеров, у которых есть назначенные услуги")
    @GetMapping("/with-services")
    public Page<TrainerServiceResponse> getAllTrainersWithServices (Pageable pageable){
        return service.getAllTrainersWithServices(pageable);
    }

    @Operation (summary = "Удаление услуги у тренера по ID тренера и ID услуги", description =  "Удаляет услугу у тренера")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Услуга у тренера успешно удалена"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Тренер не найден / услуга не найдена"
            )
    })
    @DeleteMapping("/{trainerId}/services/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrainerService (@PathVariable Long trainerId, @PathVariable Long serviceId){
        service.deleteTrainerService(trainerId, serviceId);
    }

    @Operation (summary = "Поиск тренеров", description =  "Ищет тренеров по имени или фамилии с поддержкой пагинации и сортировки")
    @GetMapping("/search")
    public Page<TrainerResponse> searchTrainers(
            @RequestParam String query,
            Pageable pageable
    ){
        return service.searchTrainers(query, pageable);
    }
    
}
