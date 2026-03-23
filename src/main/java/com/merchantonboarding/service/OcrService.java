package com.merchantonboarding.service;

import java.io.File;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * OCR Service for extracting text from document images.
 * This is a mock implementation that simulates OCR processing.
 * For production, integrate with Tesseract OCR via Tess4J or a cloud OCR service.
 */
@Service
public class OcrService {

    private final Random random = new Random();

    // Patterns for extracting information
    private static final Pattern BUSINESS_NAME_PATTERN = Pattern.compile(
            "(?i)(?:company|business|enterprise|sdn\\s*bhd|bhd)\\s*name\\s*[:\\s]*([A-Za-z0-9\\s&\\-\\.]+(?:Sdn\\s*Bhd|Bhd)?)",
            Pattern.MULTILINE
    );
    private static final Pattern REGISTRATION_PATTERN = Pattern.compile(
            "(?i)(?:registration|company)\\s*(?:no|number|#)\\s*[:\\s]*([A-Z0-9\\-]+)",
            Pattern.MULTILINE
    );
    private static final Pattern IC_PATTERN = Pattern.compile(
            "(?i)(?:ic|identification|nric|mykad)\\s*(?:no|number)?\\s*[:\\s]*(\\d{6}[\\-\\s]?\\d{2}[\\-\\s]?\\d{4})",
            Pattern.MULTILINE
    );
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "(?i)(?:director|owner|name)\\s*[:\\s]*([A-Za-z\\s]+)",
            Pattern.MULTILINE
    );
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "(?i)(?:address|location)\\s*[:\\s]*([A-Za-z0-9\\s,\\.\\-]+(?:Malaysia)?)",
            Pattern.MULTILINE
    );

    /**
     * Extract text from an image file using mock OCR.
     * In production, this would use Tesseract or a cloud OCR service.
     */
    @Async
    public CompletableFuture<OcrResult> extractText(File imageFile, String documentType) {
        try {
            // Simulate processing delay (2-5 seconds)
            Thread.sleep(2000 + random.nextInt(3000));

            // Generate mock OCR result based on document type
            OcrResult result = generateMockOcrResult(documentType, imageFile.getName());
            return CompletableFuture.completedFuture(result);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            OcrResult errorResult = new OcrResult();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage("OCR processing was interrupted");
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    /**
     * Generate a mock OCR result based on document type.
     * This simulates what Tesseract would return for different document types.
     */
    private OcrResult generateMockOcrResult(String documentType, String fileName) {
        OcrResult result = new OcrResult();
        result.setSuccess(true);
        result.setConfidenceScore(75 + random.nextInt(20)); // 75-94 confidence

        String mockText;
        if (documentType != null) {
            switch (documentType.toLowerCase()) {
                case "business_registration":
                case "ssm":
                    mockText = generateBusinessRegistrationText();
                    break;
                case "director_ic":
                case "mykad":
                case "identity":
                    mockText = generateIdentityDocumentText();
                    break;
                case "financial":
                case "bank_statement":
                    mockText = generateFinancialDocumentText();
                    break;
                case "address_proof":
                case "utility_bill":
                    mockText = generateAddressDocumentText();
                    break;
                default:
                    mockText = generateGenericDocumentText(fileName);
            }
        } else {
            mockText = generateGenericDocumentText(fileName);
        }

        result.setRawText(mockText);
        result.setExtractedBusinessName(extractField(mockText, BUSINESS_NAME_PATTERN));
        result.setExtractedRegistrationNumber(extractField(mockText, REGISTRATION_PATTERN));
        result.setExtractedDirectorName(extractField(mockText, NAME_PATTERN));
        result.setExtractedDirectorIC(extractField(mockText, IC_PATTERN));
        result.setExtractedAddress(extractField(mockText, ADDRESS_PATTERN));

        return result;
    }

    private String generateBusinessRegistrationText() {
        String[] companyNames = {
            "Tech Solutions Sdn Bhd", "Global Trade Enterprise Sdn Bhd",
            "Digital Innovations Bhd", "Merchant Services Sdn Bhd"
        };
        String companyName = companyNames[random.nextInt(companyNames.length)];
        String regNo = String.format("%d-%s-%06d",
            1990 + random.nextInt(34),
            random.nextBoolean() ? "A" : "D",
            random.nextInt(999999));

        return String.format("""
            SURUHANJAYA SYARIKAT MALAYSIA
            COMPANIES COMMISSION OF MALAYSIA

            CERTIFICATE OF INCORPORATION

            Company Name: %s
            Registration No: %s
            Date of Incorporation: %d-%02d-%02d

            This is to certify that the above-named company
            has been incorporated under the Companies Act 2016.

            Business Address: 123 Jalan Teknologi,
            Cyberjaya Technology Park,
            63000 Cyberjaya, Selangor, Malaysia

            Directors:
            1. Ahmad bin Hassan - Director Name
            2. Siti binti Abdullah
            """,
            companyName, regNo,
            2010 + random.nextInt(14),
            1 + random.nextInt(12),
            1 + random.nextInt(28));
    }

    private String generateIdentityDocumentText() {
        String[] names = {"Ahmad bin Hassan", "Siti binti Abdullah", "Lee Wei Ming", "Raj Kumar"};
        String name = names[random.nextInt(names.length)];
        String ic = String.format("%02d%02d%02d-%02d-%04d",
            70 + random.nextInt(30),
            1 + random.nextInt(12),
            1 + random.nextInt(28),
            1 + random.nextInt(14),
            random.nextInt(10000));

        return String.format("""
            MALAYSIA IDENTITY CARD
            MyKad

            Name: %s
            IC Number: %s

            Address: 45 Taman Harmoni,
            Jalan Bahagia, 43000 Kajang,
            Selangor, Malaysia

            Date of Birth: %02d-%02d-19%d
            """,
            name, ic,
            1 + random.nextInt(28),
            1 + random.nextInt(12),
            70 + random.nextInt(30));
    }

    private String generateFinancialDocumentText() {
        return String.format("""
            MAYBANK ISLAMIC BERHAD
            BANK STATEMENT

            Account Holder: Tech Solutions Sdn Bhd
            Account Number: 1144-2233-5566
            Statement Period: Jan 2024 - Dec 2024

            Opening Balance: RM 250,000.00
            Total Credits: RM 1,500,000.00
            Total Debits: RM 1,200,000.00
            Closing Balance: RM 550,000.00

            Average Monthly Balance: RM %,d.00

            Address: 123 Jalan Teknologi,
            Cyberjaya, Selangor, Malaysia
            """,
            100000 + random.nextInt(500000));
    }

    private String generateAddressDocumentText() {
        return """
            TENAGA NASIONAL BERHAD
            ELECTRICITY BILL

            Account Name: Tech Solutions Sdn Bhd
            Account Number: 12345678901234

            Service Address: 123 Jalan Teknologi,
            Cyberjaya Technology Park,
            63000 Cyberjaya, Selangor, Malaysia

            Billing Period: 01/01/2024 - 31/01/2024
            Amount Due: RM 1,250.00

            This serves as proof of address.
            """;
    }

    private String generateGenericDocumentText(String fileName) {
        return String.format("""
            Document: %s

            Company Name: Generic Business Sdn Bhd
            Registration No: 12345-K

            Director Name: John Doe
            IC Number: 800101-14-5678

            Address: 100 Jalan Utama,
            50000 Kuala Lumpur, Malaysia

            This document contains business information.
            """, fileName);
    }

    private String extractField(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Result class for OCR extraction
     */
    public static class OcrResult {
        private boolean success;
        private String rawText;
        private int confidenceScore;
        private String extractedBusinessName;
        private String extractedRegistrationNumber;
        private String extractedDirectorName;
        private String extractedDirectorIC;
        private String extractedAddress;
        private String errorMessage;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getRawText() { return rawText; }
        public void setRawText(String rawText) { this.rawText = rawText; }
        public int getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(int confidenceScore) { this.confidenceScore = confidenceScore; }
        public String getExtractedBusinessName() { return extractedBusinessName; }
        public void setExtractedBusinessName(String extractedBusinessName) { this.extractedBusinessName = extractedBusinessName; }
        public String getExtractedRegistrationNumber() { return extractedRegistrationNumber; }
        public void setExtractedRegistrationNumber(String extractedRegistrationNumber) { this.extractedRegistrationNumber = extractedRegistrationNumber; }
        public String getExtractedDirectorName() { return extractedDirectorName; }
        public void setExtractedDirectorName(String extractedDirectorName) { this.extractedDirectorName = extractedDirectorName; }
        public String getExtractedDirectorIC() { return extractedDirectorIC; }
        public void setExtractedDirectorIC(String extractedDirectorIC) { this.extractedDirectorIC = extractedDirectorIC; }
        public String getExtractedAddress() { return extractedAddress; }
        public void setExtractedAddress(String extractedAddress) { this.extractedAddress = extractedAddress; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
