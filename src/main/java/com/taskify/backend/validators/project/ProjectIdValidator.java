package com.taskify.backend.validators.project;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;


@Data
public class ProjectIdValidator {

    @NotEmpty(message = "Project ID is required")
    private String _id;
}