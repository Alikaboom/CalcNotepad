/**
 * HelloController.java
 * Main controller class for the CalcNotepad application.
 * Manages the drawing canvas, user interface interactions,
 * and orchestrates the OCR and math evaluation process.
 **/
package com.example.calcnotepad;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.License;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Stack;

public class HelloController {
    @FXML private Canvas canvas;
    @FXML private ToggleButton pencilBtn;
    @FXML private ToggleButton eraserBtn;
    @FXML private ToggleButton selectBtn;
    @FXML private Slider brushSizeSlider;
    @FXML private Label brushSizeLabel;
    @FXML private Circle currentColorDisplay;
    @FXML private VBox historyContainer;
    @FXML private Label statusLabel;
    @FXML private Label coordinatesLabel;
    @FXML private Label zoomLabel;
    @FXML private Button undoButton;
    @FXML private Button redoButton;

    @FXML private Circle color1, color2, color3, color4, color5;
    @FXML private Circle color6, color7, color8, color9, color10;
    @FXML private Circle color11, color12, color13, color14, color15;
    @FXML private Circle color16, color17, color18, color19, color20;

    private GraphicsContext gc;
    private Color currentColor = Color.BLACK;
    private double lastX, lastY;
    private boolean isDrawing = false;

    private enum Tool { PENCIL, ERASER, SELECT }
    private Tool currentTool = Tool.PENCIL;

    private final Stack<WritableImage> undoStack = new Stack<>();
    private final Stack<WritableImage> redoStack = new Stack<>();
    private static final int MAX_UNDO = 20;

    private double selectionStartX, selectionStartY;
    private double selectionEndX, selectionEndY;
    private boolean isSelecting = false;

    private double minX = Double.MAX_VALUE;
    private double minY = Double.MAX_VALUE;
    private double maxX = Double.MIN_VALUE;
    private double maxY = Double.MIN_VALUE;

    private int calculationCount = 0;
    private OCRModel ocrModel;

    @FXML
    public void initialize() {
        License.iConfirmNonCommercialUse("Alikaboom1719");
        gc = canvas.getGraphicsContext2D();
        initializeCanvas();
        setupTools();
        setupBrushSizeSlider();
        updateUndoRedoButtons();
        initializeOCR();
        saveState();
    }

    // Clears canvas with white fill and initializes default line properties.
    private void initializeCanvas() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(currentColor);
        gc.setLineWidth(brushSizeSlider.getValue());
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        statusLabel.setText("Ready to draw");
    }

    // Initializes the tool toggle group to ensure more than one tool isn't selected simultaneously.
    private void setupTools() {
        ToggleGroup toolGroup = new ToggleGroup();
        pencilBtn.setToggleGroup(toolGroup);
        eraserBtn.setToggleGroup(toolGroup);
        selectBtn.setToggleGroup(toolGroup);
        pencilBtn.setSelected(true);
    }

    private void setupBrushSizeSlider() {
        brushSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int size = newVal.intValue();
            brushSizeLabel.setText(size + "px");
            gc.setLineWidth(size);
        });
    }

    private void updateUndoRedoButtons() {
        if (undoButton != null) undoButton.setDisable(undoStack.isEmpty());
        if (redoButton != null) redoButton.setDisable(redoStack.isEmpty());
    }

    // Creates a state snapshot for undo history and handles stack limit.
    private void saveState() {
        WritableImage snapshot = canvas.snapshot(null, null);
        undoStack.push(snapshot);
        redoStack.clear();
        if (undoStack.size() > MAX_UNDO) undoStack.removeFirst();
        updateUndoRedoButtons();
    }

    @FXML private void handlePencilTool() { currentTool = Tool.PENCIL; gc.setStroke(currentColor); }
    @FXML private void handleEraserTool() { currentTool = Tool.ERASER; gc.setStroke(Color.WHITE); }
    @FXML private void handleSelectTool() { currentTool = Tool.SELECT; }

    // Updates active color from palette selection and applies to graphics context.
    @FXML
    private void handleColorPick(MouseEvent event) {
        Circle clickedCircle = (Circle) event.getSource();
        currentColor = (Color) clickedCircle.getFill();
        currentColorDisplay.setFill(currentColor);
        if (currentTool != Tool.ERASER) gc.setStroke(currentColor);
    }

    // The trigger for the drawing or selection logic based on the active tool.
    @FXML
    private void handleMousePressed(MouseEvent event) {
        lastX = event.getX();
        lastY = event.getY();
        coordinatesLabel.setText(String.format("X: %.0f, Y: %.0f", lastX, lastY));
        if (currentTool == Tool.SELECT) {
            selectionStartX = lastX;
            selectionStartY = lastY;
            isSelecting = true;
        } else {
            isDrawing = true;
            updateBoundingBox(lastX, lastY);
            saveState();
        }
    }

    // Manages the drawing or the selection drawing logic
    @FXML
    private void handleMouseDragged(MouseEvent event) {
        double currentX = event.getX();
        double currentY = event.getY();
        coordinatesLabel.setText(String.format("X: %.0f, Y: %.0f", currentX, currentY));
        if (currentTool == Tool.SELECT && isSelecting) {
            selectionEndX = currentX;
            selectionEndY = currentY;
            drawSelectionPreview();
        } else if (isDrawing && currentTool != Tool.SELECT) {
            gc.strokeLine(lastX, lastY, currentX, currentY);
            updateBoundingBox(currentX, currentY);
            lastX = currentX;
            lastY = currentY;
        }
    }

    private void drawSelectionPreview() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(Color.BLACK);
        gc.setLineDashes(5);
        double x = Math.min(selectionStartX, selectionEndX);
        double y = Math.min(selectionStartY, selectionEndY);
        double w = Math.abs(selectionEndX - selectionStartX);
        double h = Math.abs(selectionEndY - selectionStartY);
        gc.strokeRect(x, y, w, h);
        gc.setLineDashes(0);
    }

    private void updateBoundingBox(double x, double y) {
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
    }

    // Terminates active input tool lifecycle.
    @FXML
    private void handleMouseReleased(MouseEvent event) {
        if (currentTool == Tool.SELECT && isSelecting) isSelecting = false;
        isDrawing = false;
    }

    // Clears the canvas and clears bounding box data after user confirmation.
    @FXML
    private void handleClear() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Canvas");
        alert.setHeaderText("Clear the entire canvas?");
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                resetBoundingBox();
                saveState();
                gc.setFill(Color.WHITE);
                gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
                statusLabel.setText("Canvas cleared");
            }
        });
    }

    private void resetBoundingBox() {
        minX = Double.MAX_VALUE;
        minY = Double.MAX_VALUE;
        maxX = Double.MIN_VALUE;
        maxY = Double.MIN_VALUE;
    }

    @FXML private void handleNew() { handleClear(); }

    // Save the canvas snapshot to PNG format with FileChooser.
    @FXML
    private void handleSave() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                WritableImage image = canvas.snapshot(null, null);
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                statusLabel.setText("Saved successfully");
            } catch (IOException e) {
                showError("Save Error", e.getMessage());
            }
        }
    }

    // Open the image file onto canvas.
    @FXML
    private void handleOpen() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                javafx.scene.image.Image image = new javafx.scene.image.Image(file.toURI().toString());
                saveState();
                gc.drawImage(image, 0, 0);
            } catch (Exception e) {
                showError("Load Error!", e.getMessage());
            }
        }
    }

    @FXML private void handleExit()
    {
        System.exit(0);
    }

    // Pops previous state from undo stack and pushes current to redo.
    @FXML
    private void handleUndo() {
        if (!undoStack.isEmpty()) {
            WritableImage currentState = canvas.snapshot(null, null);
            redoStack.push(currentState);
            gc.drawImage(undoStack.pop(), 0, 0);
            updateUndoRedoButtons();
        }
    }

    // Pops state from redo stack and applies it to canvas.
    @FXML
    private void handleRedo() {
        if (!redoStack.isEmpty()) {
            saveState();
            gc.drawImage(redoStack.pop(), 0, 0);
            updateUndoRedoButtons();
        }
    }

    @FXML private void handleZoomIn() { adjustZoom(0.1); }
    @FXML private void handleZoomOut() { adjustZoom(-0.1); }

    // Updates canvas scale within the limits (0.5x - 3.0x).
    private void adjustZoom(double delta) {
        double newScale = Math.max(0.5, Math.min(3.0, canvas.getScaleX() + delta));
        canvas.setScaleX(newScale);
        canvas.setScaleY(newScale);
        zoomLabel.setText(String.format("%.0f%%", newScale * 100));
    }

    @FXML private void handleResetZoom() { canvas.setScaleX(1.0); canvas.setScaleY(1.0); zoomLabel.setText("100%"); }

    @FXML
    private void handleClearHistory() {
        historyContainer.getChildren().clear();
        calculationCount = 0;
    }

    // Appends a calculation record to the history box.
    private void addCalculationToHistory(String expression, String result) {
        calculationCount++;
        VBox item = new VBox(5);
        item.getStyleClass().add("history-item");
        item.setPadding(new Insets(8));

        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        item.getChildren().addAll(
                new Label("#" + calculationCount + " - " + time),
                new Label("Expression: " + expression),
                new Label("Result: " + result)
        );
        historyContainer.getChildren().addFirst(item);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Instantiate the OCRModel with the training data path.
    private void initializeOCR() {
        try {
            String tessDataPath = "D:/Projects/Java/CalcNotepad/src/main/resources/tessdata";
            ocrModel = new OCRModel(tessDataPath);
        } catch (Exception e) {
            statusLabel.setText("OCR initialization failed");
        }
    }

    // Controller action to start image snapshotting and OCR pipeline.
    @FXML
    private void handleCalculateOCR() {
        statusLabel.setText("Processing OCR...");
        performOCR();
    }

    // Executes the full OCR logic.
    private void performOCR() {
        try {
            WritableImage snapshot = canvas.snapshot(null, null);
            String ocrText = ocrModel.processImage(snapshot);
            String expression = ocrModel.extractMathExpression(ocrText);

            if (!expression.isEmpty()) {
                String cleanExpr = expression.contains("=") ? expression.split("=")[0].trim() : expression;
                if (!cleanExpr.isEmpty()) {
                    try {
                        double result = evaluateExpression(cleanExpr);
                        drawResultOnCanvas(result);
                        addCalculationToHistory(cleanExpr, String.valueOf(result));
                        statusLabel.setText("Calculation complete");
                    } catch (Exception e) {
                        statusLabel.setText("Math Error: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            statusLabel.setText("OCR Failed");
        }
    }

    //Renders the result as text on the canvas near active ink bounds.
    private void drawResultOnCanvas(double result) {
        gc.setFill(Color.RED);
        gc.setFont(new javafx.scene.text.Font("Arial", 48));
        String resultText = " = " + String.format("%.2f", result);
        double drawX = (maxX > Double.MIN_VALUE) ? maxX + 20 : canvas.getWidth() / 2 + 100;
        double drawY = (minY < Double.MAX_VALUE && maxY > Double.MIN_VALUE) ? (minY + maxY) / 2 : canvas.getHeight() / 2;
        if (drawX + 150 > canvas.getWidth()) {
            drawX = canvas.getWidth() - 160;
            drawY = maxY + 50;
        }
        gc.fillText(resultText, drawX, drawY);
        gc.setFill(currentColor);
    }

    //parse the string to double and calculates expression string with mXparser library.
    private double evaluateExpression(String expressionString) throws Exception {
        Expression e = new Expression(expressionString);
        if (!e.checkSyntax()) {
            throw new Exception("Syntax Error");
        }
        double result = e.calculate();
        if (Double.isNaN(result)) throw new Exception("Invalid Result");
        return result;
    }
}