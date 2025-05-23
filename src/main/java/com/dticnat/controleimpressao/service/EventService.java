package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.model.Event;
import com.dticnat.controleimpressao.model.Solicitation;
import com.dticnat.controleimpressao.model.User;
import com.dticnat.controleimpressao.model.enums.EventType;
import com.dticnat.controleimpressao.model.enums.Role;
import com.dticnat.controleimpressao.repository.EventRepository;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventService {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Value("${BACKEND_URL}")
    private String backendUrl;

    @Autowired
    private ResourceLoader resourceLoader;

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    public Optional<Event> getLatestEventForSolicitation(Solicitation solicitation) {
        List<Event> events = solicitation.getTimeline();

        if (events == null || events.isEmpty()) {
            return Optional.empty();
        }

        return events.stream()
                .max(Comparator.comparing(Event::getCreationDate));
    }

    // Envia emails para todos os interessados associados a uma determinada solicitação, exceto a quem executou a ação
    // Executado assincronamente para não bloquear a resposta ao cliente ao enviar email
    @Async
    public void sendNotificationForLatestEvent(Solicitation solicitation, User triggeringUser) {
        Optional<Event> latestEventOptional = getLatestEventForSolicitation(solicitation);

        latestEventOptional.ifPresent(latestEvent -> {
            boolean canNotify = couldSendNotification(latestEvent.getType());

            if (canNotify) {
                // 1. Buscar todos os usuários interessados na solicitação, a depender do usuário que ativou o evento
                // Caso este usuário seja o próprio sistema, o único interessado na notificação deve ser o dono da solicitação
                Set<User> interestedUsers = getInterestedUsers(solicitation, triggeringUser);

                // 2. Extrair os emails dos destinatários distintos
                List<String> recipientEmails = interestedUsers.stream()
                        .map(User::getEmail)
                        .distinct()
                        .toList();

                if (!recipientEmails.isEmpty()) {
                    String subject = "[Impressão CNAT] Notificação sobre solicitação";
                    String body = generateHtmlContentForLatestEvent(solicitation, latestEvent);
                    String[] toAddresses = recipientEmails.toArray(new String[0]);

                    try {
                        emailService.sendEmail(toAddresses, subject, body);
                        logger.info("Notificação enviada com sucesso para a solicitação ID {} para {} usuários interessados.", solicitation.getId(), recipientEmails.size());
                    } catch (MessagingException e) {
                        logger.error("Erro ao enviar notificação para ID de solicitação {}: {}", solicitation.getId(), e.getMessage(), e);
                    }
                } else {
                    logger.info("Nenhum outro usuário interessado a ser notificado para ID de solicitação {}.", solicitation.getId());
                }
            }
        });
    }

    // Used primarilly to send notifications for deletion events,
    // since deletion events are not saved in the timeline,
    // because its gone
    // Executado assincronamente para não bloquear a resposta ao cliente ao enviar email
    @Async
    public void sendNotificationForLooseEvent(Solicitation solicitation, User triggeringUser, EventType eventType) {
            Set<User> interestedUsers = getInterestedUsers(solicitation, triggeringUser);

            List<String> recipientEmails = interestedUsers.stream()
                    .map(User::getEmail)
                    .distinct()
                    .toList();

            if (!recipientEmails.isEmpty()) {
                String subject = "[Impressão CNAT] Notificação sobre solicitação";
                String body = generateHtmlContentForLooseEvent(solicitation, triggeringUser, eventType);
                String[] toAddresses = recipientEmails.toArray(new String[0]);

                try {
                    emailService.sendEmail(toAddresses, subject, body);
                    logger.info("Notificação enviada com sucesso para a ID de solicitação {} (Tipo de evento: {}) para {} usuários interessados.", solicitation.getId(), eventType, recipientEmails.size());
                } catch (MessagingException e) {
                    logger.error("Erro ao enviar notificação para ID de solicitação {} (Tipo de evento: {}): {}", solicitation.getId(), eventType, e.getMessage(), e);
                }
            } else {
                logger.info("Nenhum outro usuário interessado a ser notificado para o ID de solicitação {} (Tipo de evento: {}).", solicitation.getId(), eventType);
            }
    }

    public Set<User> getInterestedUsers(Solicitation solicitation, User triggeringUser) {
        List<Event> events = solicitation.getTimeline(); // Get the list of events for the solicitation

        if (events == null || events.isEmpty()) {
            return Set.of(); // Return an empty set if there are no events
        }

        // Check if the user on the last event has a role of "system"
        if (triggeringUser.getRole() == Role.SYSTEM) {
            // If so, the only interested user is the solicitation's owner
            User owner = solicitation.getUser();
            if (owner != null) {
                return Set.of(owner);
            } else {
                return Set.of();
            }
        }

        // Otherwise, extract the unique users from the list of events
        return events.stream()
                .map(Event::getUser) // Get the User from each Event
                .filter(java.util.Objects::nonNull) // Ensure users are not null before collecting
//                .filter(user -> !user.getRegistrationNumber().equals(triggeringUser.getRegistrationNumber())) // Excluir o próprio usuário que disparou a ação dos interessados
                .collect(Collectors.toSet()); // Collect the unique users into a Set
    }

    private boolean couldSendNotification(EventType eventType) {
        // Atualmente, qualquer operação exceto TOGGLE pode notificar via email
        return eventType == EventType.COMMENT
                || eventType == EventType.REQUEST_OPENING
                || eventType == EventType.REQUEST_CLOSING
                || eventType == EventType.REQUEST_DELETING
                || eventType == EventType.REQUEST_ARCHIVING
                //|| eventType == EventType.REQUEST_TOGGLE
                || eventType == EventType.REQUEST_EDITING;
    }

    // Gerar conteúdo do email
    private String generateHtmlContent(Solicitation solicitation, User user, EventType eventType, String eventContent) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String solicitationNumber = String.format("Nº%06d", solicitation.getId());
        boolean systemNotification = user.getRole() == Role.SYSTEM;

        final Context ctx = new Context();
        ctx.setVariable("eventMessage", switch (eventType) {
            case COMMENT -> "Um novo comentário foi adicionado à solicitação " + solicitationNumber + ".";
            case REQUEST_TOGGLE -> "A status da solicitação " + solicitationNumber + " foi alterado.";
            case REQUEST_OPENING -> "A solicitação " + solicitationNumber + " foi aberta.";
            case REQUEST_CLOSING -> "A solicitação " + solicitationNumber + " foi fechada.";
            case REQUEST_EDITING -> "A solicitação " + solicitationNumber + " foi editada.";
            case REQUEST_DELETING -> "A solicitação " + solicitationNumber + " foi excluída.";
            case REQUEST_ARCHIVING -> "A solicitação " + solicitationNumber + " foi arquivada.";
            default -> "Uma atualização ocorreu na solicitação " + solicitationNumber + ".";
        });

        ctx.setVariable("sender", systemNotification ? "automaticamente" : "por " + user.getCommonName() + " " + user.getRegistrationNumber());
        ctx.setVariable("eventDate", LocalDateTime.now().format(formatter)); // Default to now
        ctx.setVariable("showContent", eventType == EventType.COMMENT);
        ctx.setVariable("showRedirect", eventType != EventType.REQUEST_DELETING);
        ctx.setVariable("eventContent", eventContent != null ? eventContent : "Nenhuma informação de conteúdo específica para este evento.");
        ctx.setVariable("solicitationLink", frontendUrl + "/solicitacoes/ver/" + solicitation.getId().toString());
        ctx.setVariable("currentYear", Year.now().getValue());

        try {
            Resource dtiResource = resourceLoader.getResource("classpath:static/images/logodti.png");
            byte[] dtiBytes = dtiResource.getInputStream().readAllBytes();
            String base64Dti = Base64.getEncoder().encodeToString(dtiBytes);
            ctx.setVariable("logodti", "data:image/png;base64," + base64Dti);

            Resource ifrnResource = resourceLoader.getResource("classpath:static/images/logoifrn.png");
            byte[] ifrnBytes = ifrnResource.getInputStream().readAllBytes();
            String base64Ifrn = Base64.getEncoder().encodeToString(ifrnBytes);
            ctx.setVariable("logoifrn", "data:image/png;base64," + base64Ifrn);

        } catch (IOException e) {
            System.err.println("Erro ao carregar imagens: " + e.getMessage());
            // Handle the error appropriately with fallback URL
            ctx.setVariable("logodti", backendUrl + "/api/images/logodti.png"); // Fallback
            ctx.setVariable("logoifrn", backendUrl + "/api/images/logoifrn.png"); // Fallback
        }

        return templateEngine.process("email_notification.html", ctx);
    }

    private String generateHtmlContentForLatestEvent(Solicitation solicitation, Event event) {
        return generateHtmlContent(
                solicitation,
                event.getUser(),
                event.getType(),
                event.getContent()
        );
    }

    private String generateHtmlContentForLooseEvent(Solicitation solicitation, User triggeringUser, EventType eventType) {
        return generateHtmlContent(
                solicitation,
                triggeringUser,
                eventType,
                null // No specific content for loose events in the original method
        );
    }
}
