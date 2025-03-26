package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.model.Request;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// @Controller ao invés de @RestController
// Isso porque aqui retornamos uma página estática (HTML) ao invés de dados (JSON), como nos outros controllers
@Controller
@RequestMapping("/relatorio")
@Tag(name = "Relatórios", description = "Operações relacionadas a geração de relatórios")
public class ReportController {

    /**
     * Gera um relatório em HTML com base na lista de solicitações fornecida.
     * Este endpoint recebe uma lista de objetos Request no corpo da requisição,
     * calcula informações relevantes para o relatório (data de geração, total de páginas,
     * número total de solicitações) e as adiciona ao modelo para serem renderizadas
     * em um template Thymeleaf chamado "report.html".
     *
     * @param requests A lista de solicitações a serem incluídas no relatório.
     * @param model    O modelo Spring para passar dados para a view (Thymeleaf).
     * @return O nome do template Thymeleaf a ser renderizado ("report").
     */
    @PostMapping
    public String generateReport(@RequestBody @Valid List<Request> requests, Model model) {
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
