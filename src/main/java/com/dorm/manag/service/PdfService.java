package com.dorm.manag.service;

import com.dorm.manag.entity.Issue;
import com.dorm.manag.entity.Payment;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PdfService {

        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        public byte[] generatePaymentReceipt(Payment payment) {
                log.info("Generating PDF receipt for payment: {}", payment.getId());

                try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PdfWriter writer = new PdfWriter(baos);
                        PdfDocument pdf = new PdfDocument(writer);

                        Document document = new Document(pdf, PageSize.A4);
                        document.setMargins(80f, 36f, 60f, 36f);

                        PdfFont headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
                        PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

                        Paragraph header = new Paragraph("DORMITORY PAYMENT RECEIPT")
                                        .setFont(headerFont)
                                        .setFontSize(20f)
                                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                                        .setMarginBottom(20f);
                        document.add(header);

                        Table table = new Table(2);
                        table.setWidth(PageSize.A4.getWidth() - 72f);

                        addTableRow(table, "Receipt ID:", "RCP-" + payment.getId());
                        addTableRow(table, "Transaction ID:", payment.getTransactionId());
                        addTableRow(table, "Payment Date:",
                                        payment.getCompletedAt() != null
                                                        ? payment.getCompletedAt().format(DATE_FORMATTER)
                                                        : payment.getCreatedAt().format(DATE_FORMATTER));
                        addTableRow(table, "Student Name:",
                                        payment.getUser().getFirstName() + " " + payment.getUser().getLastName());
                        addTableRow(table, "Student Email:", payment.getUser().getEmail());
                        addTableRow(table, "Room Number:",
                                        payment.getRoomNumber() != null ? payment.getRoomNumber() : "N/A");
                        addTableRow(table, "Description:", payment.getDescription());
                        addTableRow(table, "Payment Type:",
                                        payment.getPaymentType() != null ? payment.getPaymentType() : "General");
                        addTableRow(table, "Payment Method:", payment.getPaymentMethod().getDisplayName());
                        addTableRow(table, "Amount:", payment.getDisplayAmount());
                        addTableRow(table, "Status:", payment.getStatus().getDisplayName());

                        if (payment.getExternalPaymentId() != null) {
                                addTableRow(table, "External Payment ID:", payment.getExternalPaymentId());
                        }

                        if (payment.getPeriodStart() != null && payment.getPeriodEnd() != null) {
                                addTableRow(table, "Period:",
                                                payment.getPeriodStart().format(DATE_FORMATTER) + " - " +
                                                                payment.getPeriodEnd().format(DATE_FORMATTER));
                        }

                        document.add(table);

                        document.add(new Paragraph("\n"));

                        Paragraph footerLine1 = new Paragraph("This is an automatically generated receipt.")
                                        .setFont(normalFont)
                                        .setFontSize(10f)
                                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);

                        Paragraph footerLine2 = new Paragraph(
                                        "Generated on: " + java.time.LocalDateTime.now().format(DATE_FORMATTER))
                                        .setFont(normalFont)
                                        .setFontSize(8f)
                                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);

                        document.add(footerLine1);
                        document.add(footerLine2);

                        document.close();

                        log.info("PDF receipt generated successfully for payment: {}", payment.getId());
                        return baos.toByteArray();

                } catch (Exception e) {
                        log.error("Error generating PDF receipt for payment {}: {}", payment.getId(), e.getMessage(),
                                        e);
                        throw new RuntimeException("Failed to generate PDF receipt", e);
                }
        }

        public byte[] generateIssueReport(Issue issue) {
                log.info("Generating PDF report for issue: {}", issue.getId());

                try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PdfWriter writer = new PdfWriter(baos);
                        PdfDocument pdf = new PdfDocument(writer);

                        Document document = new Document(pdf, PageSize.A4);
                        document.setMargins(80f, 36f, 60f, 36f);

                        PdfFont headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
                        PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

                        Paragraph header = new Paragraph("DORMITORY ISSUE REPORT")
                                        .setFont(headerFont)
                                        .setFontSize(20f)
                                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                                        .setMarginBottom(20f);
                        document.add(header);

                        Table table = new Table(2);
                        table.setWidth(PageSize.A4.getWidth() - 72f);

                        addTableRow(table, "Issue ID:", "ISS-" + issue.getId());
                        addTableRow(table, "Title:", issue.getTitle());
                        addTableRow(table, "Category:", issue.getCategoryDisplay());
                        addTableRow(table, "Priority:", issue.getPriorityDisplay());
                        addTableRow(table, "Status:", issue.getStatusDisplay());
                        addTableRow(table, "Reported By:",
                                        issue.getUser().getFirstName() + " " + issue.getUser().getLastName());
                        addTableRow(table, "Room Number:",
                                        issue.getRoomNumber() != null ? issue.getRoomNumber() : "N/A");
                        addTableRow(table, "Location Details:",
                                        issue.getLocationDetails() != null ? issue.getLocationDetails() : "N/A");
                        addTableRow(table, "Reported Date:", issue.getReportedAt().format(DATE_FORMATTER));

                        if (issue.getAcknowledgedAt() != null) {
                                addTableRow(table, "Acknowledged Date:",
                                                issue.getAcknowledgedAt().format(DATE_FORMATTER));
                        }

                        if (issue.getResolvedAt() != null) {
                                addTableRow(table, "Resolved Date:", issue.getResolvedAt().format(DATE_FORMATTER));
                        }

                        if (issue.getAssignedTo() != null) {
                                addTableRow(table, "Assigned To:",
                                                issue.getAssignedTo().getFirstName() + " "
                                                                + issue.getAssignedTo().getLastName());
                        }

                        if (issue.getContractorRequired() != null && issue.getContractorRequired()) {
                                addTableRow(table, "Contractor Required:", "Yes");
                        }

                        if (issue.getCostEstimate() != null) {
                                addTableRow(table, "Cost Estimate:", issue.getCostEstimate());
                        }

                        document.add(table);

                        document.add(new Paragraph("\nDescription:")
                                        .setFont(headerFont)
                                        .setFontSize(12f)
                                        .setMarginTop(10f));
                        document.add(
                                        new Paragraph(issue.getDescription() != null ? issue.getDescription()
                                                        : "No description provided")
                                                        .setFont(normalFont)
                                                        .setFontSize(10f));

                        if (issue.getAdminNotes() != null && !issue.getAdminNotes().trim().isEmpty()) {
                                document.add(new Paragraph("\nAdmin Notes:")
                                                .setFont(headerFont)
                                                .setFontSize(12f)
                                                .setMarginTop(10f));
                                document.add(new Paragraph(issue.getAdminNotes())
                                                .setFont(normalFont)
                                                .setFontSize(10f));
                        }

                        if (issue.getResolutionNotes() != null && !issue.getResolutionNotes().trim().isEmpty()) {
                                document.add(new Paragraph("\nResolution Notes:")
                                                .setFont(headerFont)
                                                .setFontSize(12f)
                                                .setMarginTop(10f));
                                document.add(new Paragraph(issue.getResolutionNotes())
                                                .setFont(normalFont)
                                                .setFontSize(10f));
                        }

                        document.add(new Paragraph("\n"));

                        Paragraph footerLine1 = new Paragraph("This is an automatically generated report.")
                                        .setFont(normalFont)
                                        .setFontSize(10f)
                                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);

                        Paragraph footerLine2 = new Paragraph(
                                        "Generated on: " + java.time.LocalDateTime.now().format(DATE_FORMATTER))
                                        .setFont(normalFont)
                                        .setFontSize(8f)
                                        .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);

                        document.add(footerLine1);
                        document.add(footerLine2);

                        document.close();

                        log.info("PDF report generated successfully for issue: {}", issue.getId());
                        return baos.toByteArray();

                } catch (Exception e) {
                        log.error("Error generating PDF report for issue {}: {}", issue.getId(), e.getMessage(), e);
                        throw new RuntimeException("Failed to generate PDF report", e);
                }
        }

        private void addTableRow(Table table, String key, String value) {
                table.addCell(new Paragraph(key)
                                .setFontSize(10f)
                                .setBold());
                table.addCell(new Paragraph(value != null ? value : "N/A")
                                .setFontSize(10f));
        }

        public void savePdfToFile(byte[] pdfBytes, String filename) {
                try {
                        java.nio.file.Files.write(java.nio.file.Paths.get(filename), pdfBytes);
                        log.info("PDF saved to file: {}", filename);
                } catch (Exception e) {
                        log.error("Failed to save PDF to file {}: {}", filename, e.getMessage());
                        throw new RuntimeException("Failed to save PDF to file", e);
                }
        }
}