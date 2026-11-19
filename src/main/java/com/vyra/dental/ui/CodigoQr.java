package com.vyra.dental.ui;

import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/** Convierte un texto (el enlace de pago) en la imagen de un código QR, con ZXing. */
public final class CodigoQr {

    private CodigoQr() {
    }

    public static Image imagen(String texto, int tamano) {
        BitMatrix matriz;
        try {
            matriz = new QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamano, tamano,
                    Map.of(EncodeHintType.MARGIN, 1, EncodeHintType.CHARACTER_SET, "UTF-8"));
        } catch (WriterException e) {
            throw new IllegalArgumentException("No se pudo crear el código QR", e);
        }
        WritableImage imagen = new WritableImage(matriz.getWidth(), matriz.getHeight());
        PixelWriter pixeles = imagen.getPixelWriter();
        for (int x = 0; x < matriz.getWidth(); x++) {
            for (int y = 0; y < matriz.getHeight(); y++) {
                pixeles.setColor(x, y, matriz.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return imagen;
    }
}
