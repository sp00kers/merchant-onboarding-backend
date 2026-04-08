package com.merchantonboarding.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.merchantonboarding.model.OnboardingCase;
import com.merchantonboarding.model.VerificationResult;
import com.merchantonboarding.repository.CaseRepository;
import com.merchantonboarding.repository.VerificationResultRepository;

@Service
public class ExportService {

    @Autowired
    private CaseRepository caseRepository;

    @Autowired(required = false)
    private VerificationResultRepository verificationResultRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export all cases to CSV
     */
    public String exportCasesToCsv() throws IOException {
        List<OnboardingCase> cases = caseRepository.findAll();
        StringWriter writer = new StringWriter();

        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader("Case ID", "Business Name", "Business Type", "Registration Number",
                        "Merchant Category", "Director Name", "Director IC", "Director Phone",
                        "Director Email", "Status", "Priority",
                        "Assigned To", "Created Date", "Last Updated"))) {

            for (OnboardingCase c : cases) {
                printer.printRecord(
                        c.getCaseId(),
                        c.getBusinessName(),
                        c.getBusinessType(),
                        c.getRegistrationNumber(),
                        c.getMerchantCategory(),
                        c.getDirectorName(),
                        c.getDirectorIC(),
                        c.getDirectorPhone(),
                        c.getDirectorEmail(),
                        c.getStatus(),
                        c.getPriority(),
                        c.getAssignedTo(),
                        c.getCreatedDate(),
                        c.getLastUpdated()
                );
            }
        }

        return writer.toString();
    }

    /**
     * Export a single case to CSV
     */
    public String exportCaseToCsv(String caseId) throws IOException {
        OnboardingCase c = caseRepository.findById(caseId).orElse(null);
        if (c == null) {
            return "";
        }

        StringWriter writer = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                .withHeader("Field", "Value"))) {

            printer.printRecord("Case ID", c.getCaseId());
            printer.printRecord("Business Name", c.getBusinessName());
            printer.printRecord("Business Type", c.getBusinessType());
            printer.printRecord("Registration Number", c.getRegistrationNumber());
            printer.printRecord("Merchant Category", c.getMerchantCategory());
            printer.printRecord("Business Address", c.getBusinessAddress());
            printer.printRecord("Director Name", c.getDirectorName());
            printer.printRecord("Director IC", c.getDirectorIC());
            printer.printRecord("Director Phone", c.getDirectorPhone());
            printer.printRecord("Director Email", c.getDirectorEmail());
            printer.printRecord("Status", c.getStatus());
            printer.printRecord("Priority", c.getPriority());
            printer.printRecord("Assigned To", c.getAssignedTo());
            printer.printRecord("Created Date", c.getCreatedDate());
            printer.printRecord("Last Updated", c.getLastUpdated());
        }

        return writer.toString();
    }

    /**
     * Export all cases to PDF
     */
    public byte[] exportCasesToPdf() throws DocumentException {
        List<OnboardingCase> cases = caseRepository.findAll();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4.rotate());
        PdfWriter.getInstance(document, baos);
        document.open();

        addHeader(document, "Merchant Onboarding - Case Report");
        addGeneratedInfo(document);

        // Create table
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setSpacingBefore(20);

        // Header
        String[] headers = {"Case ID", "Business Name", "Type", "Status", "Priority", "Assigned To", "Created"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
            cell.setBackgroundColor(new Color(52, 152, 219));
            cell.setPadding(8);
            table.addCell(cell);
        }

        // Data rows
        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        for (OnboardingCase c : cases) {
            table.addCell(new Phrase(nvl(c.getCaseId()), dataFont));
            table.addCell(new Phrase(nvl(c.getBusinessName()), dataFont));
            table.addCell(new Phrase(nvl(c.getBusinessType()), dataFont));
            table.addCell(createStatusCell(c.getStatus()));
            table.addCell(new Phrase(nvl(c.getPriority()), dataFont));
            table.addCell(new Phrase(nvl(c.getAssignedTo()), dataFont));
            table.addCell(new Phrase(nvl(c.getCreatedDate()), dataFont));
        }

        document.add(table);

        // Summary
        addSummary(document, cases);

        document.close();
        return baos.toByteArray();
    }

    /**
     * Export a single case to PDF with full details
     */
    public byte[] exportCaseToPdf(String caseId) throws DocumentException {
        OnboardingCase c = caseRepository.findById(caseId).orElse(null);
        if (c == null) {
            return new byte[0];
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, baos);
        document.open();

        addHeader(document, "Case Details Report");
        addGeneratedInfo(document);

        // Case ID
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        document.add(new Paragraph("Case: " + c.getCaseId(), titleFont));
        document.add(Chunk.NEWLINE);

        // Business Information
        addSectionTitle(document, "Business Information");
        addDetailRow(document, "Business Name", c.getBusinessName());
        addDetailRow(document, "Business Type", c.getBusinessType());
        addDetailRow(document, "Registration Number", c.getRegistrationNumber());
        addDetailRow(document, "Merchant Category", c.getMerchantCategory());
        addDetailRow(document, "Business Address", c.getBusinessAddress());

        // Director Information
        addSectionTitle(document, "Director Information");
        addDetailRow(document, "Director Name", c.getDirectorName());
        addDetailRow(document, "IC Number", c.getDirectorIC());
        addDetailRow(document, "Phone", c.getDirectorPhone());
        addDetailRow(document, "Email", c.getDirectorEmail());

        // Case Status
        addSectionTitle(document, "Case Status");
        addDetailRow(document, "Status", c.getStatus());
        addDetailRow(document, "Priority", c.getPriority());
        addDetailRow(document, "Assigned To", c.getAssignedTo());
        addDetailRow(document, "Created Date", c.getCreatedDate());
        addDetailRow(document, "Last Updated", c.getLastUpdated());

        // Verification Results (if available)
        if (verificationResultRepository != null) {
            List<VerificationResult> verifications = verificationResultRepository.findByOnboardingCaseCaseIdOrderByRequestedAtDesc(caseId);
            if (!verifications.isEmpty()) {
                addSectionTitle(document, "Verification Results");
                for (VerificationResult v : verifications) {
                    String verificationInfo = String.format("%s: %s (Confidence: %d%%)",
                            v.getVerificationType(),
                            v.getStatus(),
                            v.getConfidenceScore() != null ? v.getConfidenceScore() : 0);
                    document.add(new Paragraph("  - " + verificationInfo,
                            FontFactory.getFont(FontFactory.HELVETICA, 10)));
                }
            }
        }

        document.close();
        return baos.toByteArray();
    }

    private void addHeader(Document document, String title) throws DocumentException {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(44, 62, 80));
        Paragraph header = new Paragraph(title, headerFont);
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);
    }

    private void addGeneratedInfo(Document document) throws DocumentException {
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
        Paragraph generated = new Paragraph("Generated: " + LocalDateTime.now().format(DATE_FORMATTER), smallFont);
        generated.setAlignment(Element.ALIGN_CENTER);
        document.add(generated);
        document.add(Chunk.NEWLINE);
    }

    private void addSectionTitle(Document document, String title) throws DocumentException {
        document.add(Chunk.NEWLINE);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(52, 152, 219));
        Paragraph section = new Paragraph(title, sectionFont);
        section.setSpacingAfter(10);
        document.add(section);
    }

    private void addDetailRow(Document document, String label, String value) throws DocumentException {
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        Paragraph row = new Paragraph();
        row.add(new Chunk(label + ": ", labelFont));
        row.add(new Chunk(nvl(value), valueFont));
        row.setSpacingAfter(5);
        document.add(row);
    }

    private PdfPCell createStatusCell(String status) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        PdfPCell cell = new PdfPCell(new Phrase(nvl(status), font));
        cell.setPadding(5);

        if ("Approved".equalsIgnoreCase(status)) {
            cell.setBackgroundColor(new Color(212, 237, 218));
        } else if ("Rejected".equalsIgnoreCase(status)) {
            cell.setBackgroundColor(new Color(248, 215, 218));
        } else if (status != null && status.contains("Review")) {
            cell.setBackgroundColor(new Color(204, 229, 255));
        }

        return cell;
    }

    private void addSummary(Document document, List<OnboardingCase> cases) throws DocumentException {
        document.add(Chunk.NEWLINE);
        addSectionTitle(document, "Summary");

        long approved = cases.stream().filter(c -> "Approved".equalsIgnoreCase(c.getStatus())).count();
        long rejected = cases.stream().filter(c -> "Rejected".equalsIgnoreCase(c.getStatus())).count();
        long pending = cases.size() - approved - rejected;

        addDetailRow(document, "Total Cases", String.valueOf(cases.size()));
        addDetailRow(document, "Approved", String.valueOf(approved));
        addDetailRow(document, "Rejected", String.valueOf(rejected));
        addDetailRow(document, "Pending", String.valueOf(pending));
    }

    private String nvl(String value) {
        return value != null ? value : "-";
    }
}
