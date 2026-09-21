package com.skating.platform.backend.controller;


import com.skating.platform.backend.dto.request.TrainerRequest;
import com.skating.platform.backend.dto.response.TrainerResponse;
import com.skating.platform.backend.dto.request.UpdateTrainerRequest;
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
    public TrainerResponse createTrainer (
            @Valid @RequestBody TrainerRequest request
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
}
