package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.model.Request;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
public class ReportController {

    @PostMapping("/api/relatorio")
    public String generateReport(@RequestBody List<Request> requests, Model model) {
        // Calcular a data de geração do relatório
        String reportGenerationDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        // Calcular o número total de páginas de todos os relatórios
        int totalPageCount = requests.stream()
                .mapToInt(Request::getTotalPageCount)
                .sum();

        // Adicionar informações ao modelo
        model.addAttribute("institutionName", "Instituto Federal do Rio Grande do Norte - Natal Central (IFRN - CNAT)");
        model.addAttribute("reportGenerationDate", reportGenerationDate);
        model.addAttribute("totalPageCount", totalPageCount);
        model.addAttribute("totalRequests", requests.size());  // Nova variável
        model.addAttribute("requests", requests);

        return "report"; // Renderiza o template Thymeleaf com as informações
    }
}
