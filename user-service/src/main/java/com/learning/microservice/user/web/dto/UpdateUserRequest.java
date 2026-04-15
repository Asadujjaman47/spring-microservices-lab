package com.learning.microservice.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
    @NotBlank @Size(max = 100) String firstName, @NotBlank @Size(max = 100) String lastName) {}
