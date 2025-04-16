package com.dticnat.controleimpressao.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SuapLoginResponseDTO {
    private String username;
    private String refresh;
    private String access;
}
