package com.skating.platform.backend.controller;


import com.skating.platform.backend.dto.trainer.request.CreateTrainerRequest;
import com.skating.platform.backend.dto.trainer.response.TrainerResponse;
import com.skating.platform.backend.dto.trainer.request.UpdateTrainerRequest;
import com.skating.platform.backend.dto.trainer_service.responce.TrainerServiceResponse;
import com.skating.platform.backend.service.TrainerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trainers")
public class TrainerController {
    private final TrainerService service;

    public TrainerController(TrainerService service){
        this.service = service;
    }




    @GetMapping
    public List<TrainerResponse> getAllTrainers(){
        return service.getAllTrainers();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TrainerResponse createTrainer (
            @Valid @RequestBody CreateTrainerRequest request
    ){
       return service.createTrainer(request);
    }

    @GetMapping("/{trainerId}")
    public TrainerResponse getTrainerById (@PathVariable Long trainerId){
        return service.getTrainerById(trainerId);
    }

    @PutMapping("/{trainerId}")
    public TrainerResponse updateTrainer(@PathVariable Long trainerId, @Valid @RequestBody UpdateTrainerRequest request){
        return service.updateTrainer(trainerId, request);
    }

    @DeleteMapping("/{trainerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrainer (@PathVariable Long trainerId){
        service.deleteTrainer(trainerId);
    }

    @PostMapping("/{trainerId}/services/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addServiceToTrainer (@PathVariable Long trainerId, @PathVariable Long serviceId){
        service.addServiceToTrainer(trainerId, serviceId);
    }

    @GetMapping("/services")
    public List<TrainerServiceResponse> getAllTrainers_Services (){
        return service.getAllTrainers_Services();
    }

    @GetMapping("/{trainerId}/services")
    public TrainerServiceResponse getTrainerServicesById (@PathVariable Long trainerId){
        return service.getTrainerServiceById(trainerId);
    }
    @GetMapping("/with-services")
    public List<TrainerServiceResponse> getAllTrainers_With_Services (){
        return service.getAllTrainers_With_Services();
    }

    @DeleteMapping("/{trainerId}/services/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrainerService (@PathVariable Long trainerId, @PathVariable Long serviceId){
        service.deleteTrainerService(trainerId, serviceId);
    }
    
}
