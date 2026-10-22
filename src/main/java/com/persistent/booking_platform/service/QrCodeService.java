package com.persistent.booking_platform.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.persistent.booking_platform.exception.ApiException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Creates and verifies the QR code printed on every ticket. The payload carries the booking id and an
 * HMAC-SHA256 signature, so a forged or edited code is rejected at the theatre entrance without a database lookup.
 */
@Service
public class QrCodeService {
    static final String PREFIX = "XYZ-TKT";
    private static final int SIZE = 300;
    private static final int SIGNATURE_HEX_LENGTH = 32;

    private final byte[] secret;

    public QrCodeService(@Value("${app.qr.secret}") String secret) {
        if (secret == null || secret.length() < 16) {
            throw new IllegalArgumentException("app.qr.secret must be at least 16 characters long");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String payload(Long bookingId, Long showId, List<String> seatNumbers) {
        String body = PREFIX + "|" + bookingId + "|" + showId + "|" + String.join(",", seatNumbers);
        return body + "|" + sign(body);
    }

    /** @return the booking id when the payload is well formed and its signature is genuine */
    public Optional<Long> verify(String payload) {
        if (payload == null) {
            return Optional.empty();
        }
        String[] parts = payload.split("\\|");
        if (parts.length != 5 || !PREFIX.equals(parts[0])) {
            return Optional.empty();
        }
        String body = String.join("|", parts[0], parts[1], parts[2], parts[3]);
        byte[] expected = sign(body).getBytes(StandardCharsets.UTF_8);
        byte[] actual = parts[4].getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(parts[1]));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public byte[] png(String payload) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, SIZE, SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "QR_GENERATION_FAILED",
                    "The QR code could not be generated. Please try again.");
        }
    }

    private String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            String hex = HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
            return hex.substring(0, SIGNATURE_HEX_LENGTH);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA256 is not available", ex);
        }
    }
}
