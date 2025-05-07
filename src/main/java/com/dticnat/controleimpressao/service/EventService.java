package com.dticnat.controleimpressao.service;

import com.dticnat.controleimpressao.model.Event;
import com.dticnat.controleimpressao.model.Solicitation;
import com.dticnat.controleimpressao.model.User;
import com.dticnat.controleimpressao.model.enums.EventType;
import com.dticnat.controleimpressao.repository.EventRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    public Optional<Event> getLatestEventForSolicitation(Solicitation solicitation) {
        List<Event> events = solicitation.getTimeline();

        if (events == null || events.isEmpty()) {
            return Optional.empty();
        }

        return events.stream()
                .max(Comparator.comparing(Event::getCreationDate));
    }

    public void sendNotificationForLatestEvent(Solicitation solicitation, User triggeringUser) {
        Optional<Event> latestEventOptional = getLatestEventForSolicitation(solicitation);

        latestEventOptional.ifPresent(latestEvent -> {
            boolean shouldNotify = shouldSendNotification(latestEvent.getType());

            if (shouldNotify) {
                // 1. Buscar todos os usuários interessados na solicitação
                Set<User> interestedUsers = getInterestedUsers(solicitation);

                // 2. Excluir o próprio usuário que disparou a ação
                Set<User> recipients = interestedUsers.stream()
//                        .filter(user -> !user.getRegistrationNumber().equals(triggeringUser.getRegistrationNumber()))
                        .collect(Collectors.toSet());

                // 3. Extrair os emails dos destinatários distintos
                List<String> recipientEmails = recipients.stream()
                        .map(User::getEmail) // Assuming User has an getEmail()
                        .distinct()
                        .toList();

                if (!recipientEmails.isEmpty()) {
                    String subject = "[Impressão CNAT] Notificação sobre solicitação";
                    String body = generateNotificationBodyForLatestEvent(solicitation, latestEvent);
                    String[] toAddresses = recipientEmails.toArray(new String[0]);

                    try {
                        emailService.sendEmail(toAddresses, subject, body);
                        System.out.println("Notification sent to " + recipientEmails.size() + " interested users.");
                    } catch (MessagingException e) {
                        System.err.println("Error sending notification: " + e.getMessage());
                    }
                } else {
                    System.out.println("No other interested users to notify.");
                }
            }
        });
    }

    // Used primarilly to send notifications for deletion events,
    // since deletion events are not saved in the timeline,
    // because its gone
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
            String body = generateNotificationBodyForLooseEvent(solicitation, triggeringUser, eventType);
            String[] toAddresses = recipientEmails.toArray(new String[0]);

            try {
                emailService.sendEmail(toAddresses, subject, body);
                System.out.println("Notification sent to " + recipientEmails.size() + " interested users for " + eventType + " of solicitation " + solicitation.getId() + ".");
            } catch (MessagingException e) {
                System.err.println("Error sending notification: " + e.getMessage());
            }
        } else {
            System.out.println("No other interested users to notify for " + eventType + " of solicitation " + solicitation.getId() + ".");
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
        return eventType == EventType.REQUEST_OPENING
                || eventType == EventType.REQUEST_CLOSING
                //|| eventType == EventType.REQUEST_ARCHIVING
                || eventType == EventType.REQUEST_EDITING;
    }

    private String generateNotificationBodyForLatestEvent(Solicitation solicitation, Event event) {
        return "An event has occurred for Solicitation ID: " + solicitation.getId() + "\n" +
                "Event Type: " + event.getType() + "\n" +
                "Created By: " + event.getUser().getCommonName() + " (" + event.getUser().getRegistrationNumber() + ")\n" +
                "Date/Time: " + event.getCreationDate() + "\n" +
                "Content: " + event.getContent();
    }

    private String generateNotificationBodyForLooseEvent(Solicitation solicitation, User triggeringUser, EventType eventType) {
        return "An event has occurred for Solicitation ID: " + solicitation.getId() + "\n" +
                "Event Type: " + eventType + "\n" +
                "Created By: " + triggeringUser.getCommonName() + " (" + triggeringUser.getRegistrationNumber() + ")\n" +
                "Date/Time: " + LocalDateTime.now() + "\n" +
                "Content: " + null;
    }
}
