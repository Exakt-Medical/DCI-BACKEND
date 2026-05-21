package com.exakt.vvip.service;

import com.exakt.vvip.dto.VvsVehicleData;
import com.exakt.vvip.entity.DciCertificate;
import com.exakt.vvip.entity.VerificationInsurance;
import com.exakt.vvip.entity.VerificationRequest;
import com.exakt.vvip.repository.DciCertificateRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class DciCertificateService {

    private static final float PAGE_W  = PageSize.A4.getWidth();
    private static final float MARGIN  = 51f;
    private static final float USABLE  = PAGE_W - 2 * MARGIN;
    private static final float COL_LBL = 155f;
    private static final float COL_VAL = USABLE - COL_LBL;
    private static final float LOGO_SZ = 62f;

    @Value("${vvip.certificate.output-dir}")
    private String outputDir;

    @Value("${vvip.certificate.dotr-logo-path:classpath:logos/dotr_logo.png}")
    private String dotrLogoPath;

    @Value("${vvip.certificate.vvs-logo-path:classpath:logos/vvs_logo.png}")
    private String vvsLogoPath;

    private final DciCertificateRepository certRepo;

    public DciCertificateService(DciCertificateRepository certRepo) {
        this.certRepo = certRepo;
    }

    // Called by VerificationService after VERIFIED
    public String issue(VerificationRequest record, VvsVehicleData vehicleData,
                        VerificationInsurance insurance,        // ← added
                        Long issuedBy, LocalDate expiryDate) {
        String certNo  = generateCertNo();
        String authCode = certNo;

        String validationDate = java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("MMMM dd, yyyy 'AT' hh:mm a",
                        java.util.Locale.ENGLISH)).toUpperCase();

        CertificateData data = new CertificateData()
                .makeAndModel(vehicleData != null ?
                        nvl(vehicleData.getMake()) + " " + nvl(vehicleData.getSeries()) : "")
                .mvFileNo(record.getMvFileNumber())
                .plateNo(record.getPlateNumber())
                .engineNo(record.getEngineNumber())
                .chassisNo(record.getChassisNumber())
                .color(vehicleData != null ? nvl(vehicleData.getColor()) : "")
                .vehicleType(vehicleData != null ? nvl(vehicleData.getBodyType()) : "")
                .yearModel(vehicleData != null ? nvl(vehicleData.getYearModel()) : "")
                .classification("")
                .premiumType(insurance != null ? nvl(insurance.getPremiumType()) : "")
                .authCode(authCode)
                .validationDate(validationDate);

        String pdfPath = generatePdf(data);

        // Persist
        DciCertificate cert = new DciCertificate();
        cert.setCertificateNo(certNo);
        cert.setVerificationId(record.getId());
        cert.setMvFileNumber(record.getMvFileNumber());
        cert.setPlateNumber(record.getPlateNumber());
        cert.setChassisNumber(record.getChassisNumber());
        cert.setEngineNumber(record.getEngineNumber());
        cert.setOwnerName(vehicleData != null ? vehicleData.getFullOwnerName() : null);
        cert.setIssuedDate(LocalDate.now());
        cert.setExpiryDate(expiryDate);
        cert.setPdfFilePath(pdfPath);
        cert.setIssuedBy(issuedBy);
        certRepo.save(cert);

        return certNo;
    }

    public byte[] getCertificatePdfBytes(String certNo) throws IOException {
        DciCertificate cert = certRepo.findByCertificateNo(certNo)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found: " + certNo));
        Path path = Paths.get(cert.getPdfFilePath());
        if (!Files.exists(path)) throw new FileNotFoundException("PDF file not found: " + path);
        return Files.readAllBytes(path);
    }

    // ── PDF generation ────────────────────────────────────────────────────────

    private String generatePdf(CertificateData d) {
        try {
            Files.createDirectories(Paths.get(outputDir));
            String filename = "Certificate_of_Validation_" +
                    d.getPlateNo().replace(" ", "_") + "_" +
                    System.currentTimeMillis() + ".pdf";
            Path filePath = Paths.get(outputDir).resolve(filename);

            try (OutputStream os = new FileOutputStream(filePath.toFile())) {
                buildPdf(os, d);
            }
            log.info("DCI certificate generated: {}", filePath);
            return filePath.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private void buildPdf(OutputStream os, CertificateData d) throws Exception {
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
        PdfWriter writer = PdfWriter.getInstance(doc, os);
        doc.open();

        PdfContentByte cb = writer.getDirectContent();
        float y = PageSize.A4.getHeight() - MARGIN;

        y = drawHeader(cb, writer, y);
        y = drawTagline(cb, y);

        y -= 10;
        y = drawSectionLabel(cb, "Vehicle Information", y);
        y = drawTable(cb, new String[][]{
                {"Make and Model",            nvl(d.getMakeAndModel())},
                {"MV File No.",               nvl(d.getMvFileNo())},
                {"Engine No.",                nvl(d.getEngineNo())},
                {"Chassis No.",               nvl(d.getChassisNo())},
                {"Plate No.",                 nvl(d.getPlateNo())},
                {"Color",                     nvl(d.getColor())},
                {"Vehicle Type/Denomination", nvl(d.getVehicleType())},
                {"Year Model",                nvl(d.getYearModel())},
                {"Classification",            nvl(d.getClassification())},
        }, y);

        y -= 10;
        y = drawSectionLabel(cb, "Premium Type", y);
        y = drawTable(cb, new String[][]{{"Premium Type", nvl(d.getPremiumType())}}, y);

        y -= 10;
        y = drawSectionLabel(cb, "Inspection Details", y);
        y = drawTable(cb, new String[][]{
                {"DCI Authentication Code", nvl(d.getAuthCode())},
                {"Date of Validation",      nvl(d.getValidationDate())},
                {"Issuer",                  nvl(d.getIssuer())},
        }, y);

        drawQr(cb, d.getAuthCode(), y);
        doc.close();
    }

    private float drawHeader(PdfContentByte cb, PdfWriter writer, float y) throws Exception {
        float logoY = y - LOGO_SZ;
        drawLogo(cb, dotrLogoPath, MARGIN, logoY);
        drawLogo(cb, vvsLogoPath, PAGE_W - MARGIN - LOGO_SZ, logoY);

        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
        cb.setFontAndSize(bf, 16);
        cb.setColorFill(Color.BLACK);
        cb.beginText();
        cb.showTextAligned(Element.ALIGN_CENTER, "CERTIFICATE OF VALIDATION",
                MARGIN + LOGO_SZ + 4 + (USABLE - 2 * (LOGO_SZ + 4)) / 2,
                logoY + LOGO_SZ / 2, 0);
        cb.endText();
        return logoY - 10;
    }

    private float drawTagline(PdfContentByte cb, float y) throws Exception {
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
        cb.setFontAndSize(bf, 8.5f);
        cb.setColorFill(Color.BLACK);
        cb.beginText();
        cb.moveText(MARGIN, y - 13);
        cb.showText("This is to certify that the motor vehicle described below has been validated for compliance with the");
        cb.newlineShowText("relevant standards and regulations.");
        cb.endText();
        return y - 30;
    }

    private float drawSectionLabel(PdfContentByte cb, String label, float y) throws Exception {
        float h = 14f;
        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
        cb.setFontAndSize(bf, 10);
        cb.setColorFill(Color.BLACK);
        cb.beginText();
        cb.moveText(MARGIN, y - h);
        cb.showText(label);
        cb.endText();
        return y - h - 2;
    }

    private float drawTable(PdfContentByte cb, String[][] rows, float y) throws Exception {
        final float ROW_H    = 20f;
        final float PAD_LEFT = 6f;
        final float PAD_TOP  = 5f;

        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
        BaseFont bfReg  = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);

        float rowY = y;
        for (String[] row : rows) {
            float rowBottom = rowY - ROW_H;

            cb.setColorFill(Color.WHITE);
            cb.rectangle(MARGIN, rowBottom, COL_LBL, ROW_H); cb.fill();
            cb.setColorStroke(Color.BLACK); cb.setLineWidth(0.5f);
            cb.rectangle(MARGIN, rowBottom, COL_LBL, ROW_H); cb.stroke();

            cb.setColorFill(Color.WHITE);
            cb.rectangle(MARGIN + COL_LBL, rowBottom, COL_VAL, ROW_H); cb.fill();
            cb.rectangle(MARGIN + COL_LBL, rowBottom, COL_VAL, ROW_H); cb.stroke();

            cb.setFontAndSize(bfBold, 9); cb.setColorFill(Color.BLACK);
            cb.beginText();
            cb.moveText(MARGIN + PAD_LEFT, rowBottom + PAD_TOP);
            cb.showText(row[0]);
            cb.endText();

            cb.setFontAndSize(bfReg, 9);
            cb.beginText();
            cb.moveText(MARGIN + COL_LBL + PAD_LEFT, rowBottom + PAD_TOP);
            cb.showText(row[1]);
            cb.endText();

            rowY = rowBottom;
        }
        return rowY - 2;
    }

    private void drawQr(PdfContentByte cb, String content, float y) {
        try {
            byte[] qrBytes = generateQrPng(content, 150);
            if (qrBytes.length == 0) return;
            Image qr = Image.getInstance(qrBytes);
            float qrSize = 80f;
            qr.setAbsolutePosition(PAGE_W - MARGIN - qrSize, y - qrSize - 10);
            qr.scaleAbsolute(qrSize, qrSize);
            cb.addImage(qr);
        } catch (Exception e) {
            log.warn("QR drawing failed: {}", e.getMessage());
        }
    }

    private void drawLogo(PdfContentByte cb, String logoPath, float x, float y) {
        try {
            byte[] bytes = loadResource(logoPath);
            if (bytes == null) return;
            Image img = Image.getInstance(bytes);
            img.setAbsolutePosition(x, y);
            img.scaleAbsolute(LOGO_SZ, LOGO_SZ);
            cb.addImage(img);
        } catch (Exception e) {
            log.warn("Logo load failed {}: {}", logoPath, e.getMessage());
        }
    }

    private byte[] loadResource(String path) throws IOException {
        if (path.startsWith("classpath:")) {
            String res = path.substring("classpath:".length());
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(res)) {
                return is != null ? is.readAllBytes() : null;
            }
        }
        Path p = Paths.get(path);
        return Files.exists(p) ? Files.readAllBytes(p) : null;
    }

    private byte[] generateQrPng(String content, int size) {
        try {
            Class<?> qrWriter   = Class.forName("com.google.zxing.qrcode.QRCodeWriter");
            Class<?> bitMatrix  = Class.forName("com.google.zxing.common.BitMatrix");
            Class<?> matrixImg  = Class.forName("com.google.zxing.client.j2se.MatrixToImageWriter");
            Class<?> format     = Class.forName("com.google.zxing.BarcodeFormat");
            Object writer  = qrWriter.getDeclaredConstructor().newInstance();
            Object qrFormat = java.lang.reflect.Array.get(format.getMethod("values").invoke(null), 11);
            Object matrix  = qrWriter.getMethod("encode", String.class, format, int.class, int.class)
                    .invoke(writer, content, qrFormat, size, size);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            matrixImg.getMethod("writeToStream", bitMatrix, String.class, OutputStream.class)
                    .invoke(null, matrix, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("QR generation unavailable: {}", e.getMessage());
            return new byte[0];
        }
    }

    private String generateCertNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("DCI-%s-%06d", date, System.currentTimeMillis() % 1_000_000);
    }

    private String nvl(String v) { return v != null && !v.isBlank() ? v : ""; }

    // ── Data holder ───────────────────────────────────────────────────────────

    public static class CertificateData {
        private String makeAndModel, mvFileNo, engineNo, chassisNo, plateNo;
        private String color, vehicleType, yearModel, classification;
        private String premiumType, authCode, validationDate, issuer;

        public CertificateData makeAndModel(String v)  { this.makeAndModel  = v; return this; }
        public CertificateData mvFileNo(String v)      { this.mvFileNo      = v; return this; }
        public CertificateData engineNo(String v)      { this.engineNo      = v; return this; }
        public CertificateData chassisNo(String v)     { this.chassisNo     = v; return this; }
        public CertificateData plateNo(String v)       { this.plateNo       = v; return this; }
        public CertificateData color(String v)         { this.color         = v; return this; }
        public CertificateData vehicleType(String v)   { this.vehicleType   = v; return this; }
        public CertificateData yearModel(String v)     { this.yearModel     = v; return this; }
        public CertificateData classification(String v){ this.classification = v; return this; }
        public CertificateData premiumType(String v)   { this.premiumType   = v; return this; }
        public CertificateData authCode(String v)      { this.authCode      = v; return this; }
        public CertificateData validationDate(String v){ this.validationDate = v; return this; }
        public CertificateData issuer(String v)        { this.issuer        = v; return this; }

        public String getMakeAndModel()   { return makeAndModel; }
        public String getMvFileNo()       { return mvFileNo; }
        public String getEngineNo()       { return engineNo; }
        public String getChassisNo()      { return chassisNo; }
        public String getPlateNo()        { return plateNo; }
        public String getColor()          { return color; }
        public String getVehicleType()    { return vehicleType; }
        public String getYearModel()      { return yearModel; }
        public String getClassification() { return classification; }
        public String getPremiumType()    { return premiumType; }
        public String getAuthCode()       { return authCode; }
        public String getValidationDate() { return validationDate; }
        public String getIssuer()         { return issuer; }
    }
}