/**
 * OCRModel.java
 * This class handles the image processing and OCR logic for the application.
 * It takes a drawing from the canvas, crops it to the active area,
 * and uses Tesseract to convert the drawing into a mathematical string.
 **/
package com.example.calcnotepad;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.awt.*;
import java.awt.image.BufferedImage;

public class OCRModel {

    private static final int SCALE_FACTOR = 3;
    private static final int THRESHOLD = 180;
    private final Tesseract tesseract;

    public OCRModel(String tessDataPath) {
        tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("eng");

        tesseract.setVariable("user_defined_dpi", "300");
        tesseract.setPageSegMode(6);
        tesseract.setOcrEngineMode(1);

        // Restrict character set to numbers and math symbols
        tesseract.setVariable("tessedit_char_whitelist", "0123456789+-*/^().=!|lIoOxX:÷?");
    }

    //Main method for pre-processing the image so OCR functions better.
    public String processImage(WritableImage fxImage) throws TesseractException {
        if (fxImage == null) return "";

        BufferedImage original = SwingFXUtils.fromFXImage(fxImage, null);

        // Crop to the text area to remove excess whitespace
        BufferedImage cropped = autoCrop(original);
        if (cropped == null) return "";

        // Add padding so the text doesn't touch the edges
        BufferedImage padded = addPadding(cropped, 0.50);

        // Upscale and convert to black/white for better recognition
        BufferedImage processed = upscaleImage(padded, SCALE_FACTOR);
        processed = binarizeImage(processed, THRESHOLD);

        String rawText = tesseract.doOCR(processed);
        return extractMathExpression(rawText);
    }

    //Finds the ink on the canvas and crops tightly around it.
    private BufferedImage autoCrop(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int minX = width, minY = height;
        int maxX = -1, maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = source.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Detect anything that isn't white
                if (r < 240 || g < 240 || b < 240) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX == -1 || (maxX - minX < 5) || (maxY - minY < 5)) return null;

        // Apply a small margin so we don't cut off the edges of the strokes
        int margin = 20;
        minX = Math.max(0, minX - margin);
        minY = Math.max(0, minY - margin);
        maxX = Math.min(width, maxX + margin);
        maxY = Math.min(height, maxY + margin);
        return source.getSubimage(minX, minY, (maxX - minX) + 1, (maxY - minY) + 1);
    }


     //Centers the cropped text and adds whitespace context.
    private BufferedImage addPadding(BufferedImage original, double ratio) {
        int w = original.getWidth();
        int h = original.getHeight();
        int maxDim = Math.max(w, h);
        int pad = Math.max((int) (maxDim * ratio), 50);

        // Adjust for very wide expressions
        int verticalPadBoost = (w > h * 3) ? h : 0;
        int newW = w + (2 * pad);
        int newH = h + (2 * pad) + (2 * verticalPadBoost);

        BufferedImage padded = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = padded.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, newW, newH);

        g2d.drawImage(original, pad, pad + verticalPadBoost, null);
        g2d.dispose();

        return padded;
    }

    private BufferedImage upscaleImage(BufferedImage original, int scaleFactor) {
        int newWidth = original.getWidth() * scaleFactor;
        int newHeight = original.getHeight() * scaleFactor;
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, newWidth, newHeight);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return scaled;
    }

    private BufferedImage binarizeImage(BufferedImage original, int threshold) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage binarized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int brightness = (r + g + b) / 3;

                int newColor = (brightness < threshold) ? 0x000000 : 0xFFFFFF;
                binarized.setRGB(x, y, newColor);
            }
        }
        return binarized;
    }


    //Corrects common OCR mistakes (e.g., 'O' to '0', 'l' to '1').
    public String extractMathExpression(String ocrText) {
        if (ocrText == null) return "";
        String cleaned = ocrText.trim().replaceAll("\\s+", "");

        cleaned = cleaned.replace("O", "0").replace("o", "0")
                .replace("l", "1").replace("I", "1").replace("|", "1")
                .replace("S", "5").replace("s", "5")
                .replace("Z", "2").replace("z", "2")
                .replace("g", "9").replace("q", "9")
                .replace("B", "8")
                .replace("x", "*").replace("X", "*")
                .replace(":", "/").replace("÷", "/")
                .replace("?", "2");

        if (cleaned.endsWith("=")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        if (cleaned.contains(")") && !cleaned.contains("(")) {
            cleaned = cleaned.replace(")", "2");
        }

        return cleaned;
    }
}