package com.dticnat.controleimpressao.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentDTO {
    @NotNull(message = "O comentário não pode ser nulo.")
    @NotEmpty(message = "O comentário não pode ser vazio.")
    private String message;
}
