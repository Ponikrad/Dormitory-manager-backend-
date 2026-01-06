package com.dorm.manag.service;

import com.dorm.manag.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    public void sendPushNotification(User user, String title, String message, NotificationType type) {
        log.info("Sending push notification to {}: {} - {}", user.getUsername(), title, message);

        try {

            Map<String, Object> notification = new HashMap<>();
            notification.put("to", user.getId());
            notification.put("title", title);
            notification.put("body", message);
            notification.put("type", type.name());
            notification.put("timestamp", System.currentTimeMillis());

            simulatePushNotification(notification);

        } catch (Exception e) {
            log.error("Failed to send push notification to {}: {}", user.getUsername(), e.getMessage());
        }
    }

    public void sendEmailNotification(User user, String subject, String content) {
        log.info("Sending email notification to {}: {}", user.getEmail(), subject);

        try {

            Map<String, Object> email = new HashMap<>();
            email.put("to", user.getEmail());
            email.put("subject", subject);
            email.put("content", content);
            email.put("timestamp", System.currentTimeMillis());

            simulateEmailNotification(email);

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    public void notifyPaymentReminder(User user, String amount, String dueDate) {
        String title = "Payment Reminder";
        String message = String.format("Payment of %s is due on %s", amount, dueDate);

        sendPushNotification(user, title, message, NotificationType.PAYMENT);
        sendEmailNotification(user, title,
                String.format(
                        "Dear %s,\n\nThis is a reminder that your payment of %s is due on %s.\n\nBest regards,\nDormitory Management",
                        user.getFirstName(), amount, dueDate));
    }

    public void notifyReservationConfirmed(User user, String resourceName, String dateTime) {
        String title = "Reservation Confirmed";
        String message = String.format("Your reservation for %s on %s is confirmed", resourceName, dateTime);

        sendPushNotification(user, title, message, NotificationType.RESERVATION);
    }

    public void notifyKeyPickupReady(User user, String keyType, String location) {
        String title = "Key Ready for Pickup";
        String message = String.format("Your %s is ready for pickup at %s", keyType, location);

        sendPushNotification(user, title, message, NotificationType.KEY_MANAGEMENT);
    }

    public void notifyIssueStatusUpdate(User user, String issueTitle, String newStatus) {
        String title = "Issue Update";
        String message = String.format("Issue '%s' status updated to: %s", issueTitle, newStatus);

        sendPushNotification(user, title, message, NotificationType.ISSUE_UPDATE);
    }

    public void notifyNewAnnouncement(User user, String announcementTitle) {
        String title = "New Announcement";
        String message = String.format("New announcement: %s", announcementTitle);

        sendPushNotification(user, title, message, NotificationType.ANNOUNCEMENT);
    }

    public void notifyApplicationStatusChange(User user, String applicationNumber, String newStatus) {
        String title = "Application Update";
        String message = String.format("Application %s status: %s", applicationNumber, newStatus);

        sendPushNotification(user, title, message, NotificationType.APPLICATION);
        sendEmailNotification(user, title,
                String.format(
                        "Dear %s,\n\nYour dormitory application %s status has been updated to: %s.\n\nPlease check your application portal for more details.\n\nBest regards,\nDormitory Admissions",
                        user.getFirstName(), applicationNumber, newStatus));
    }

    public void notifyMaintenanceScheduled(User user, String description, String scheduledDate) {
        String title = "Maintenance Scheduled";
        String message = String.format("Maintenance: %s scheduled for %s", description, scheduledDate);

        sendPushNotification(user, title, message, NotificationType.MAINTENANCE);
    }

    public void notifyDocumentExpiring(User user, String documentTitle, String expiryDate) {
        String title = "Document Expiring";
        String message = String.format("Document '%s' expires on %s", documentTitle, expiryDate);

        sendPushNotification(user, title, message, NotificationType.DOCUMENT);
    }

    private void simulatePushNotification(Map<String, Object> notification) {
        log.info("🔔 PUSH NOTIFICATION SENT: {}", notification);

    }

    private void simulateEmailNotification(Map<String, Object> email) {
        log.info("📧 EMAIL SENT: {}", email);

    }

    public enum NotificationType {
        PAYMENT,
        RESERVATION,
        KEY_MANAGEMENT,
        ISSUE_UPDATE,
        ANNOUNCEMENT,
        APPLICATION,
        MAINTENANCE,
        DOCUMENT,
        GENERAL
    }
}