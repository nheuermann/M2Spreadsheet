/*******************************************************************************
 *  Copyright (c) 2025 Nils Heuermann
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *   
 *   Contributors:
 *       Nils Heuermann - initial API and implementation
 *       (based on M2Doc's TemplateValidationMessage by Obeo)
 *  
 *******************************************************************************/
package io.github.nheuermann.m2spreadsheet.validation;

/**
 * Represents a validation message for a template cell.
 * Stores the message text, severity level, and location in the workbook.
 * 
 * @author nheuermann
 */
public class TemplateValidationMessage {

    /**
     * The validation message level (OK, INFO, WARNING, ERROR).
     */
    private final ValidationMessageLevel level;

    /**
     * The validation message text.
     */
    private final String message;

    /**
     * The sheet name where this validation message applies.
     */
    private final String sheetName;

    /**
     * The row index (0-based) where this validation message applies.
     */
    private final int rowIndex;

    /**
     * The column index (0-based) where this validation message applies.
     */
    private final int columnIndex;

    /**
     * Constructor.
     * 
     * @param level
     *            the validation message {@link ValidationMessageLevel}
     * @param message
     *            the message text
     * @param sheetName
     *            the sheet name
     * @param rowIndex
     *            the row index (0-based)
     * @param columnIndex
     *            the column index (0-based)
     */
    public TemplateValidationMessage(ValidationMessageLevel level, String message, String sheetName, int rowIndex, int columnIndex) {
        this.level = level;
        this.message = message;
        this.sheetName = sheetName;
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
    }

    /**
     * Gets the {@link ValidationMessageLevel}.
     * 
     * @return the {@link ValidationMessageLevel}
     */
    public ValidationMessageLevel getLevel() {
        return level;
    }

    /**
     * Gets the validation message text.
     * 
     * @return the validation message text
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the sheet name.
     * 
     * @return the sheet name
     */
    public String getSheetName() {
        return sheetName;
    }

    /**
     * Gets the row index (0-based).
     * 
     * @return the row index
     */
    public int getRowIndex() {
        return rowIndex;
    }

    /**
     * Gets the column index (0-based).
     * 
     * @return the column index
     */
    public int getColumnIndex() {
        return columnIndex;
    }

    /**
     * Gets a human-readable location string (e.g., "Sheet1!B5").
     * 
     * @return the location string
     */
    public String getLocation() {
        return sheetName + "!" + getColumnLetter() + (rowIndex + 1);
    }

    /**
     * Converts column index to Excel column letter (A, B, ..., Z, AA, AB, ...).
     * 
     * @return the column letter
     */
    public String getColumnLetter() {
        StringBuilder columnLetter = new StringBuilder();
        int col = columnIndex;
        while (col >= 0) {
            columnLetter.insert(0, (char) ('A' + (col % 26)));
            col = (col / 26) - 1;
        }
        return columnLetter.toString();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", level, message, getLocation());
    }
}
