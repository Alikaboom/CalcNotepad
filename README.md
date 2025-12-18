# CalcNotepad

**CalcNotepad** is a JavaFX-based drawing application that leverages Tesseract OCR to interpret handwritten mathematical expressions and calculate results in real-time.

---

## 🛠 Setup & Configuration

### OCR Initialization
If the OCR functionality fails to trigger, ensure the Tesseract training data path is correctly configured for your local environment:

1. Open `HelloController.java`.
2. Locate the `initializeOCR()` method (typically near line 335).
3. Update the `tessDataPath` variable to point to the **absolute path** of your `tessdata` directory.

---

## 📝 Usage Guidelines

### Recognition Accuracy
The underlying OCR model is optimized for digitized documents. To achieve the best results with handwriting, please observe the following:

* **Legibility:** Write as cleanly as possible. The engine relies on distinct shapes to identify characters correctly.
* **Sizing:** Avoid writing very small numbers. Larger, well-defined strokes are significantly easier for the engine to process after upscaling.
* **Spacing:** Ensure characters and operators are not overlapping or "clumped." Overlapping strokes may be misinterpreted by the OCR as a single, invalid character.

### 🧪 Selection Tool
> [!IMPORTANT]
> The selection tool is currently in an experimental phase.

While the core functionality exists, users may encounter inconsistent behavior. Development priority was focused on the refinement of the OCR processing pipeline and mathematical expression evaluation to ensure calculation accuracy.
