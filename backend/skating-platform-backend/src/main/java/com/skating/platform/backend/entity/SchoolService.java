package com.skating.platform.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "school_services")
public class SchoolService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public SchoolService(){}

    public SchoolService(
            String name,
            String description,
            BigDecimal price,
            String type
    ){
        this.name = name;
        this.description = description;
        this.price = price;
        this.type = type;
        this.active = true;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId(){
        return id;
    }

    public String getDescription(){
        return description;
    }

    public BigDecimal getPrice(){
        return price;
    }

    public String getType(){
        return type;
    }

    public String getName(){
        return name;
    }

    public Boolean getActive(){
        return active;
    }

    public OffsetDateTime getCreatedAt(){
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt(){
        return updatedAt;
    }

    public void setName(String name){
        this.name = name;
    }

    public void setDescription(String description){
        this.description = description;
    }

    public void setPrice(BigDecimal price){
        this.price = price;
    }

    public void setType(String type){
        this.type = type;
    }

    public void setActive(Boolean active){
        this.active = active;
    }

    public void setCreatedAt(OffsetDateTime createdAt){
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt){
        this.updatedAt =updatedAt;
    }

    @ManyToMany(mappedBy = "services")
    private Set<Trainer> trainers = new HashSet<>();

    public Set<Trainer> getTrainers() {
        return trainers;
    }
}
