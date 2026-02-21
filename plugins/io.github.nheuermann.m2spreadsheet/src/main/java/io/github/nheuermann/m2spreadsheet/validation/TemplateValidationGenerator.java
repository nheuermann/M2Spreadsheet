/*******************************************************************************
 *  Copyright (c) 2025 Nils Heuermann
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *   
 *   Contributors:
 *       Nils Heuermann - initial API and implementation
 *       (based on M2Doc's TemplateValidationGenerator by Obeo)
 *  
 *******************************************************************************/
package io.github.nheuermann.m2spreadsheet.validation;

import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FontUnderline;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Generates validation-annotated workbooks by inserting validation messages
 * into cells and creating a validation summary sheet.
 * 
 * @author nheuermann
 */
public class TemplateValidationGenerator {

    /**
     * Color for ERROR level (red).
     */
    private static final byte[] COLOR_ERROR = new byte[] {(byte) 255, (byte) 192, (byte) 192}; // Light red

    /**
     * Color for WARNING level (yellow).
     */
    private static final byte[] COLOR_WARNING = new byte[] {(byte) 255, (byte) 255, (byte) 192}; // Light yellow

    /**
     * Color for INFO level (blue).
     */
    private static final byte[] COLOR_INFO = new byte[] {(byte) 192, (byte) 192, (byte) 255}; // Light blue

    /**
     * Color for validation sheet tab (red).
     */
    private static final short TAB_COLOR_RED = IndexedColors.RED.getIndex();

    /**
     * Generates validation-annotated workbook.
     * 
     * @param workbook
     *            the template {@link XSSFWorkbook} to annotate
     * @param validationMessages
     *            the list of {@link TemplateValidationMessage}
     */
    public void generateValidationWorkbook(XSSFWorkbook workbook, List<TemplateValidationMessage> validationMessages) {
        // First, create validation summary sheet at position 0 (no hyperlinks for template validation)
        createValidationSheet(workbook, validationMessages, "Validation", false);
        
        // Then, mark error cells with background colors
        markErrorCells(workbook, validationMessages);
    }

    /**
     * Adds a validation summary sheet to the workbook at position 0.
     * This method only creates the validation sheet with messages, without marking cells.
     * 
     * @param workbook
     *            the {@link XSSFWorkbook} to add validation sheet to
     * @param validationMessages
     *            the list of {@link TemplateValidationMessage}
     */
    public void addValidationSheet(XSSFWorkbook workbook, List<TemplateValidationMessage> validationMessages) {
        createValidationSheet(workbook, validationMessages, "Validation", false);
    }

    /**
     * Adds an errors summary sheet to the workbook at position 0 with hyperlinks to problem cells.
     * Used during normal generation to show errors inline with hyperlinks.
     * 
     * @param workbook
     *            the {@link XSSFWorkbook} to add errors sheet to
     * @param validationMessages
     *            the list of {@link TemplateValidationMessage}
     */
    public void addErrorsSheet(XSSFWorkbook workbook, List<TemplateValidationMessage> validationMessages) {
        createValidationSheet(workbook, validationMessages, "Errors", true);
    }

    /**
     * Creates a validation/errors summary sheet at the first position.
     * 
     * @param workbook
     *            the {@link XSSFWorkbook}
     * @param validationMessages
     *            the list of {@link TemplateValidationMessage}
     * @param sheetName
     *            the name for the sheet ("Validation" or "Errors")
     * @param  addHyperlinks
     *            whether to add hyperlinks to problem cells
     */
    private void createValidationSheet(XSSFWorkbook workbook, List<TemplateValidationMessage> validationMessages, String sheetName, boolean addHyperlinks) {
        // Create sheet at first position
        org.apache.poi.xssf.usermodel.XSSFSheet validationSheet = workbook.createSheet(sheetName);
        workbook.setSheetOrder(sheetName, 0);
        
        // Set sheet tab color to red (XSSFSheet-specific method)
        validationSheet.setTabColor(new XSSFColor(new byte[]{(byte)255, 0, 0}, null));
        
        // Create header row
        Row headerRow = validationSheet.createRow(0);
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        createHeaderCell(headerRow, 0, "Severity", headerStyle);
        createHeaderCell(headerRow, 1, "Location", headerStyle);
        createHeaderCell(headerRow, 2, "Message", headerStyle);
        
        // Create rows for each validation message
        int rowIdx = 1;
        for (TemplateValidationMessage message : validationMessages) {
            Row row = validationSheet.createRow(rowIdx++);
            
            CellStyle levelStyle = createLevelStyle(workbook, message.getLevel());
            
            Cell levelCell = row.createCell(0);
            levelCell.setCellValue(message.getLevel().toString());
            levelCell.setCellStyle(levelStyle);
            
            Cell locationCell = row.createCell(1);
            locationCell.setCellValue(message.getLocation());
            locationCell.setCellStyle(levelStyle);
            
            // Add hyperlink if requested
            if (addHyperlinks) {
                try {
                    org.apache.poi.ss.usermodel.Hyperlink link = workbook.getCreationHelper()
                        .createHyperlink(org.apache.poi.common.usermodel.HyperlinkType.DOCUMENT);
                    // Format: 'SheetName'!A1
                    String address = "'" + message.getSheetName() + "'!" + 
                        message.getColumnLetter() + (message.getRowIndex() + 1);
                    link.setAddress(address);
                    link.setLabel(message.getLocation());
                    locationCell.setHyperlink(link);
                    
                    // Make hyperlink blue and underlined
                    XSSFCellStyle hyperlinkStyle = workbook.createCellStyle();
                    // Copy background color from levelStyle
                   byte[] color = getColorForLevel(message.getLevel());
                    if (color != null) {
                        XSSFColor xssfColor = new XSSFColor(color, null);
                        hyperlinkStyle.setFillForegroundColor(xssfColor);
                        hyperlinkStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                    }
                    
                    // Create blue, underlined font for hyperlink
                    XSSFFont hyperlinkFont = workbook.createFont();
                    hyperlinkFont.setUnderline(FontUnderline.SINGLE.getByteValue());
                    hyperlinkFont.setColor(new XSSFColor(new byte[]{0, 0, (byte)255}, null)); // Blue
                    hyperlinkStyle.setFont(hyperlinkFont);
                    
                    locationCell.setCellStyle(hyperlinkStyle);
                } catch (Exception e) {
                    // If hyperlink fails, just show the location text
                    System.err.println("Warning: Failed to create hyperlink for " + message.getLocation() + ": " + e.getMessage());
                }
            }
            
            Cell messageCell = row.createCell(2);
            messageCell.setCellValue(message.getMessage());
            messageCell.setCellStyle(levelStyle);
        }
        
        // Auto-size columns
        validationSheet.autoSizeColumn(0);
        validationSheet.autoSizeColumn(1);
        validationSheet.autoSizeColumn(2);
        
        // Make sure message column has reasonable width
        if (validationSheet.getColumnWidth(2) > 15000) {
            validationSheet.setColumnWidth(2, 15000);
        }
    }

    /**
     * Creates a header cell with style.
     * 
     * @param row
     *            the {@link Row}
     * @param colIdx
     *            the column index
     * @param value
     *            the cell value
     * @param style
     *            the {@link CellStyle}
     */
    private void createHeaderCell(Row row, int colIdx, String value, CellStyle style) {
        Cell cell = row.createCell(colIdx);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * Creates header cell style (bold, gray background).
     * 
     * @param workbook
     *            the {@link XSSFWorkbook}
     * @return the {@link CellStyle}
     */
    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        return style;
    }

    /**
     * Creates cell style for a validation level.
     * 
     * @param workbook
     *            the {@link XSSFWorkbook}
     * @param level
     *            the {@link ValidationMessageLevel}
     * @return the {@link CellStyle}
     */
    private CellStyle createLevelStyle(XSSFWorkbook workbook, ValidationMessageLevel level) {
        XSSFCellStyle style = workbook.createCellStyle();
        
        byte[] color = getColorForLevel(level);
        if (color != null) {
            XSSFColor xssfColor = new XSSFColor(color, null);
            style.setFillForegroundColor(xssfColor);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        
        return style;
    }

    /**
     * Marks error cells with colored backgrounds.
     * 
     * @param workbook
     *            the {@link XSSFWorkbook}
     * @param validationMessages
     *            the list of {@link TemplateValidationMessage}
     */
    private void markErrorCells(XSSFWorkbook workbook, List<TemplateValidationMessage> validationMessages) {
        for (TemplateValidationMessage message : validationMessages) {
            markCell(workbook, message);
        }
    }

    /**
     * Marks a single cell with validation message.
     * 
     * @param workbook
     *            the {@link XSSFWorkbook}
     * @param message
     *            the {@link TemplateValidationMessage}
     */
    private void markCell(XSSFWorkbook workbook, TemplateValidationMessage message) {
        // Find the sheet
        Sheet sheet = workbook.getSheet(message.getSheetName());
        if (sheet == null) {
            return; // Sheet might have been renamed/deleted
        }
        
        // Find the row
        Row row = sheet.getRow(message.getRowIndex());
        if (row == null) {
            return; // Row might not exist
        }
        
        // Find the cell
        Cell cell = row.getCell(message.getColumnIndex());
        if (cell == null) {
            return; // Cell might not exist
        }
        
        // Get or create cell style with background color
        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
        
        // Copy existing style if present
        if (cell.getCellStyle() != null) {
            style.cloneStyleFrom(cell.getCellStyle());
        }
        
        // Set background color based on level
        byte[] color = getColorForLevel(message.getLevel());
        if (color != null) {
            XSSFColor xssfColor = new XSSFColor(color, null);
            style.setFillForegroundColor(xssfColor);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        
        // Append validation message to cell content
        String originalValue = getCellValueAsString(cell);
        String newValue = originalValue + "\n[" + message.getLevel() + "] " + message.getMessage();
        cell.setCellValue(newValue);
        
        // Apply style
        cell.setCellStyle(style);
    }

    /**
     * Gets cell value as string.
     * 
     * @param cell
     *            the {@link Cell}
     * @return the cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * Gets the color for a validation level.
     * 
     * @param level
     *            the {@link ValidationMessageLevel}
     * @return the RGB color bytes, or null for OK
     */
    private byte[] getColorForLevel(ValidationMessageLevel level) {
        switch (level) {
            case ERROR:
                return COLOR_ERROR;
            case WARNING:
                return COLOR_WARNING;
            case INFO:
                return COLOR_INFO;
            default:
                return null;
        }
    }
}
