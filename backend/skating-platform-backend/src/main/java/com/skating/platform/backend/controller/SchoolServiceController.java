package com.skating.platform.backend.controller;

import com.skating.platform.backend.dto.service.request.CreateSchoolServiceRequest;
import com.skating.platform.backend.dto.service.request.UpdateSchoolServiceRequest;
import com.skating.platform.backend.dto.service.response.SchoolServiceResponse;
import com.skating.platform.backend.service.SchoolServiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Tag(name = "Services", description = "Управление услугами школы")

@RestController
@RequestMapping("/api/services")
public class SchoolServiceController {
    private final SchoolServiceService schoolServiceService;

    public SchoolServiceController(SchoolServiceService schoolServiceService){
        this.schoolServiceService = schoolServiceService;
    }

    @Operation(summary = "Получить все услуги", description =  "Выводит все услуги")
    @GetMapping
    public Page<SchoolServiceResponse> getAllServices(Pageable pageable){
        return schoolServiceService.getAllServices(pageable);
    }

    @Operation(summary = "Создать услугу", description =  "Создает услугу")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Услуга создана"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные услуги"
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolServiceResponse createService(
           @Valid @RequestBody CreateSchoolServiceRequest request
    ){
        return schoolServiceService.createService(request);
    }

    @Operation(summary = "Получить услугу по ID", description =  "Возвращает услугу по его идентификатору")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Услуга найдена"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Услуга не найдена"
            )
    })
    @GetMapping("/{serviceId}")
    public SchoolServiceResponse getServiceById(@PathVariable Long serviceId){
        return schoolServiceService.getServiceById(serviceId);
    }

    @Operation(summary = "Обновить данные услуги по ID", description =  "Обновляет данные услуги")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "услуга найдена и успешно обновлена"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные для обновления услуги"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "услуга не найдена"
            )
    })
    @PutMapping("/{serviceId}")
    public SchoolServiceResponse updateService(@PathVariable Long serviceId, @Valid @RequestBody UpdateSchoolServiceRequest request){
        return schoolServiceService.updateService(serviceId, request);
    }

    @Operation (summary = "Удалить услугу по ID", description =  "Удаляет услугу по её идентификатору")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Услуга успешно удалена"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "услуга не найдена"
            )
    })
    @DeleteMapping("/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteService(@PathVariable Long serviceId){
        schoolServiceService.deleteService(serviceId);
    }

    @Operation (summary = "Поиск услуг", description =  "Ищет услуги по названию или типу с поддержкой пагинации и сортировки")
    @GetMapping("/search")
    public Page<SchoolServiceResponse> searchServices(
            @RequestParam String query,
            Pageable pageable
    ){
        return schoolServiceService.searchServices(query, pageable);
    }

    @Operation (summary = "Фильтрация услуг", description =  "Фильтрует услуги по названию, типу, цене и активности с поддержкой пагинации и сортировки")
    @GetMapping("/filter")
    public Page<SchoolServiceResponse> filterServices(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean active,
            Pageable pageable
    ){
        return schoolServiceService.filterServices(
                name,
                type,
                minPrice,
                maxPrice,
                active,
                pageable
        );
    }

}
