package com.policypulse.graphql.dto;

import com.policypulse.domain.Persona;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AskQuestionInput(
        @NotNull Persona persona,
        @NotBlank String question
) {
}
