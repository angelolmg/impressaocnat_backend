package com.dticnat.controleimpressao.controller;

import com.dticnat.controleimpressao.exception.UnauthorizedException;
import com.dticnat.controleimpressao.model.Solicitation;
import com.dticnat.controleimpressao.model.User;
import com.dticnat.controleimpressao.service.EventService;
import com.dticnat.controleimpressao.service.SolicitationService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/eventos")
@Tag(name = "Eventos", description = "Operações relacionadas a eventos de usuários e notificações")
public class EventController {

    @Autowired
    private SolicitationService solicitationService;

    @Autowired
    private EventService eventService;

    @PostMapping("/notificar/{solicitationId}")
    public ResponseEntity<?> notifyLastEvent(HttpServletRequest httpRequest,
                                                       @Parameter(description = "ID da solicitação da qual o último evento será notificado.") @PathVariable Long solicitationId) {
        // Recuperar dados do usuário
        User user = (User) httpRequest.getAttribute("userPrincipal");

        try {
            // Verificar se solicitação sendo alterada pertence ao usuário tentando editá-la
            // Se o usuario for admin, ele pode editar mesmo solicitações que não são dele
            Solicitation solicitation = solicitationService.canInteract(solicitationId, user, false);
            eventService.sendNotificationForLatestEvent(solicitation, user);
            return ResponseEntity.ok(true);

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Solicitação (ID " + String.format("%06d", solicitationId) + ") não encontrada.");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body("Usuário não autorizado a acessar esta solicitação.");
        }
    }
}
