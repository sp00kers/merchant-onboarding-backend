package com.merchantonboarding.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.merchantonboarding.service.ExportService;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private ExportService exportService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Export all cases to CSV
     */
    @GetMapping("/cases/csv")
    @PreAuthorize("hasAnyAuthority('REPORTS', 'ALL_MODULES')")
    public ResponseEntity<byte[]> exportCasesToCsv() {
        try {
            String csv = exportService.exportCasesToCsv();
            String filename = "merchant_cases_" + LocalDate.now().format(DATE_FORMATTER) + ".csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv.getBytes());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export a single case to CSV
     */
    @GetMapping("/case/{caseId}/csv")
    @PreAuthorize("hasAnyAuthority('CASE_MANAGEMENT', 'REPORTS', 'ALL_MODULES')")
    public ResponseEntity<byte[]> exportCaseToCsv(@PathVariable String caseId) {
        try {
            String csv = exportService.exportCaseToCsv(caseId);
            String filename = "case_" + caseId + "_" + LocalDate.now().format(DATE_FORMATTER) + ".csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv.getBytes());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export all cases to PDF
     */
    @GetMapping("/cases/pdf")
    @PreAuthorize("hasAnyAuthority('REPORTS', 'ALL_MODULES')")
    public ResponseEntity<byte[]> exportCasesToPdf() {
        try {
            byte[] pdf = exportService.exportCasesToPdf();
            String filename = "merchant_cases_report_" + LocalDate.now().format(DATE_FORMATTER) + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export a single case to PDF
     */
    @GetMapping("/case/{caseId}/pdf")
    @PreAuthorize("hasAnyAuthority('CASE_MANAGEMENT', 'REPORTS', 'ALL_MODULES')")
    public ResponseEntity<byte[]> exportCaseToPdf(@PathVariable String caseId) {
        try {
            byte[] pdf = exportService.exportCaseToPdf(caseId);
            String filename = "case_" + caseId + "_report_" + LocalDate.now().format(DATE_FORMATTER) + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
