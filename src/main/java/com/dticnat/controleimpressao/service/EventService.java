package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.model.Event;
import com.dticnat.controleimpressao.model.Solicitation;
import com.dticnat.controleimpressao.model.User;
import com.dticnat.controleimpressao.model.enums.EventType;
import com.dticnat.controleimpressao.repository.EventRepository;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
            boolean shouldNotify = shouldSendNotification(latestEvent.getType());

            if (shouldNotify) {
                // 1. Buscar todos os usuários interessados na solicitação
                Set<User> interestedUsers = getInterestedUsers(solicitation);

                // 2. Excluir o próprio usuário que disparou a ação
                Set<User> recipients = interestedUsers.stream()
//                        .filter(user -> triggeringUser == null || !user.getRegistrationNumber().equals(triggeringUser.getRegistrationNumber()))
                        .collect(Collectors.toSet());

                // 3. Extrair os emails dos destinatários distintos
                List<String> recipientEmails = recipients.stream()
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

        Set<User> interestedUsers = getInterestedUsers(solicitation);
        Set<User> recipients = interestedUsers.stream()
//                        .filter(user -> !user.getId().equals(triggeringUser.getId()))
                .collect(Collectors.toSet());

        List<String> recipientEmails = recipients.stream()
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

    private Set<User> getInterestedUsers(Solicitation solicitation) {
        List<Event> events = solicitation.getTimeline(); // Get the list of events for the solicitation

        if (events == null || events.isEmpty()) {
            return Set.of(); // Return an empty set if there are no events
        }

        // Extract the unique users from the list of events
        return events.stream()
                .map(Event::getUser) // Get the User from each Event
                .collect(Collectors.toSet()); // Collect the unique users into a Set
    }

    private boolean shouldSendNotification(EventType eventType) {
        // Atualmente, qualquer operação exceto ARQUIVAMENTO pode notificar via email
        return eventType == EventType.COMMENT
                || eventType == EventType.REQUEST_OPENING
                || eventType == EventType.REQUEST_CLOSING
                || eventType == EventType.REQUEST_DELETING
                //|| eventType == EventType.REQUEST_ARCHIVING
                || eventType == EventType.REQUEST_EDITING;
    }


    private String generateHtmlContentForLatestEvent(Solicitation solicitation, Event event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String solicitationNumber = String.format("Nº%06d", solicitation.getId());

        // Prepare Thymeleaf context
        final Context ctx = new Context();
        ctx.setVariable("eventMessage",  (event.getType() == EventType.COMMENT) ? "Um novo comentário foi adicionado à solicitação " + solicitationNumber + "." :
                (event.getType() == EventType.REQUEST_OPENING) ? "A solicitação " + solicitationNumber + " foi aberta." :
                        (event.getType() == EventType.REQUEST_CLOSING) ? "A solicitação " + solicitationNumber + " foi fechada." :
                                (event.getType() == EventType.REQUEST_EDITING) ? "A solicitação " + solicitationNumber + " foi editada." :
                                        (event.getType() == EventType.REQUEST_DELETING) ? "A solicitação " + solicitationNumber + " foi excluída." :
                                                "Uma atualização ocorreu na solicitação " + solicitationNumber + ".");
        ctx.setVariable("userName", event.getUser().getCommonName());
        ctx.setVariable("userRegistration", event.getUser().getRegistrationNumber());
        ctx.setVariable("eventDate", event.getCreationDate().format(formatter));
        ctx.setVariable("showContent", event.getType() == EventType.COMMENT);
        ctx.setVariable("eventContent", event.getContent() != null ? event.getContent() : "Nenhum conteúdo adicional.");
        ctx.setVariable("solicitationLink", frontendUrl + "/minhas-solicitacoes/ver/" + solicitation.getId().toString());
        ctx.setVariable("logodti", backendUrl + "/api/images/logodti.png");
        ctx.setVariable("logoifrn", backendUrl + "/api/images/logoifrn.png");

        return templateEngine.process("email_notification.html", ctx);
    }

    private String generateHtmlContentForLooseEvent(Solicitation solicitation, User triggeringUser, EventType eventType) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String solicitationNumber = String.format("Nº%06d", solicitation.getId());

        final Context ctx = new Context();
        ctx.setVariable("eventMessage",  (eventType == EventType.REQUEST_OPENING) ? "A solicitação " + solicitationNumber + " foi aberta." :
                (eventType == EventType.REQUEST_CLOSING) ? "A solicitação " + solicitationNumber + " foi encerrada." :
                        (eventType == EventType.REQUEST_EDITING) ? "A solicitação " + solicitationNumber + " foi editada." :
                                (eventType == EventType.REQUEST_DELETING) ? "A solicitação " + solicitationNumber + " foi excluída." :
                                        "Uma atualização ocorreu na solicitação " + solicitationNumber + ".");
        ctx.setVariable("userName", triggeringUser.getCommonName());
        ctx.setVariable("userRegistration", triggeringUser.getRegistrationNumber());
        ctx.setVariable("eventDate", LocalDateTime.now().format(formatter));
        ctx.setVariable("showContent", false);
        ctx.setVariable("eventContent",  "Nenhuma informação de conteúdo específica para este evento.");
        ctx.setVariable("solicitationLink", frontendUrl + "/minhas-solicitacoes/ver/" + solicitation.getId().toString());
        ctx.setVariable("logodti", backendUrl + "/api/images/logodti.png");
        ctx.setVariable("logoifrn", backendUrl + "/api/images/logoifrn.png");

        return templateEngine.process("email_notification.html", ctx);
    }
}
