/*******************************************************************************
 *  Copyright (c) 2025 Nils Heuermann
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *   
 *   Contributors:
 *       Nils Heuermann - initial API and implementation
 *       (based on M2Doc's M2DocValidator by Obeo)
 *  
 *******************************************************************************/
package io.github.nheuermann.m2spreadsheet.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.acceleo.query.runtime.IQueryEnvironment;
import org.eclipse.acceleo.query.runtime.IQueryBuilderEngine;
import org.eclipse.acceleo.query.runtime.IQueryBuilderEngine.AstResult;
import org.eclipse.acceleo.query.runtime.impl.QueryBuilderEngine;
import org.eclipse.emf.common.util.Diagnostic;

/**
 * Validates M2Spreadsheet templates by checking AQL expressions.
 * Collects validation messages for all issues found in the template.
 * 
 * @author nheuermann
 */
public class M2SpreadsheetValidator {

    /**
     * Pattern to match template expressions: {m:...}
     */
    private static final Pattern TEMPLATE_EXPR_PATTERN = Pattern.compile("\\{m:([^}]+)\\}");

    /**
     * The query environment for AQL validation.
     */
    private final IQueryEnvironment queryEnvironment;

    /**
     * List of collected validation messages.
     */
    private final List<TemplateValidationMessage> validationMessages = new ArrayList<>();

    /**
     * The highest validation level found so far.
     */
    private ValidationMessageLevel highestLevel = ValidationMessageLevel.OK;

    /**
     * Constructor.
     * 
     * @param queryEnvironment
     *            the {@link IQueryEnvironment} for AQL validation
     */
    public M2SpreadsheetValidator(IQueryEnvironment queryEnvironment) {
        this.queryEnvironment = queryEnvironment;
    }

    /**
     * Validates the given template workbook.
     * Scans all sheets and cells for template expressions and validates them.
     * 
     * @param templateWorkbook
     *            the template {@link XSSFWorkbook}
     * @param variables
     *            the variables map for validation context (currently unused but reserved for future type inference)
     * @return the highest {@link ValidationMessageLevel} found
     */
    public ValidationMessageLevel validate(XSSFWorkbook templateWorkbook, Map<String, Object> variables) {
        validationMessages.clear();
        highestLevel = ValidationMessageLevel.OK;

        // Scan all sheets
        for (int sheetIdx = 0; sheetIdx < templateWorkbook.getNumberOfSheets(); sheetIdx++) {
            Sheet sheet = templateWorkbook.getSheetAt(sheetIdx);
            validateSheet(sheet);
        }

        return highestLevel;
    }

    /**
     * Validates all cells in the given sheet.
     * 
     * @param sheet
     *            the {@link Sheet} to validate
     */
    private void validateSheet(Sheet sheet) {
        String sheetName = sheet.getSheetName();
        
        for (Row row : sheet) {
            if (row == null) continue;
            
            for (Cell cell : row) {
                if (cell == null) continue;
                
                validateCell(cell, sheetName, row.getRowNum(), cell.getColumnIndex());
            }
        }
    }

    /**
     * Validates a single cell for template expressions.
     * 
     * @param cell
     *            the {@link Cell} to validate
     * @param sheetName
     *            the sheet name
     * @param rowIndex
     *            the row index (0-based)
     * @param colIndex
     *            the column index (0-based)
     */
    private void validateCell(Cell cell, String sheetName, int rowIndex, int colIndex) {
        String cellValue = getCellValueAsString(cell);
        if (cellValue == null || cellValue.isEmpty()) {
            return;
        }

        Matcher matcher = TEMPLATE_EXPR_PATTERN.matcher(cellValue);
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            validateExpression(expression, sheetName, rowIndex, colIndex);
        }
    }

    /**
     * Gets cell value as string, handling different cell types.
     * 
     * @param cell
     *            the {@link Cell}
     * @return the cell value as string, or null
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
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
                return null;
        }
    }

    /**
     * Validates a single AQL expression.
     * 
     * @param expression
     *            the AQL expression text
     * @param sheetName
     *            the sheet name
     * @param rowIndex
     *            the row index (0-based)
     * @param colIndex
     *            the column index (0-based)
     */
    private void validateExpression(String expression, String sheetName, int rowIndex, int colIndex) {
        try {
            // Build the AQL expression (add "aql:" prefix if not present)
            String aqlExpression = expression;
            if (!aqlExpression.startsWith("aql:")) {
                aqlExpression = "aql:" + aqlExpression;
            }

            // Use QueryBuilderEngine to build and validate expression
            IQueryBuilderEngine queryBuilder = new QueryBuilderEngine(queryEnvironment);
            AstResult astResult = queryBuilder.build(aqlExpression);
            
            // Check for validation issues
            if (astResult.getDiagnostic().getSeverity() != Diagnostic.OK) {
                processDiagnostic(astResult.getDiagnostic(), sheetName, rowIndex, colIndex);
            }
            
        } catch (Exception e) {
            // If validation itself fails, report as error
            addValidationMessage(
                ValidationMessageLevel.ERROR,
                "Expression validation failed: " + e.getMessage(),
                sheetName,
                rowIndex,
                colIndex
            );
        }
    }

    /**
     * Processes an AQL validation diagnostic and creates validation messages.
     * 
     * @param diagnostic
     *            the {@link Diagnostic}
     * @param sheetName
     *            the sheet name
     * @param rowIndex
     *            the row index (0-based)
     * @param colIndex
     *            the column index (0-based)
     */
    private void processDiagnostic(Diagnostic diagnostic, String sheetName, int rowIndex, int colIndex) {
        ValidationMessageLevel level = mapSeverityToLevel(diagnostic.getSeverity());
        
        // Add message for this diagnostic (if message is not null/empty)
        String message = diagnostic.getMessage();
        if (message != null && !message.isEmpty()) {
            addValidationMessage(level, message, sheetName, rowIndex, colIndex);
        }
        
        // Process child diagnostics recursively
        for (Diagnostic child : diagnostic.getChildren()) {
            processDiagnostic(child, sheetName, rowIndex, colIndex);
        }
    }

    /**
     * Maps EMF Diagnostic severity to ValidationMessageLevel.
     * 
     * @param severity
     *            the EMF diagnostic severity
     * @return the corresponding {@link ValidationMessageLevel}
     */
    private ValidationMessageLevel mapSeverityToLevel(int severity) {
        switch (severity) {
            case Diagnostic.ERROR:
                return ValidationMessageLevel.ERROR;
            case Diagnostic.WARNING:
                return ValidationMessageLevel.WARNING;
            case Diagnostic.INFO:
                return ValidationMessageLevel.INFO;
            default:
                return ValidationMessageLevel.OK;
        }
    }

    /**
     * Adds a validation message and updates the highest level.
     * 
     * @param level
     *            the {@link ValidationMessageLevel}
     * @param message
     *            the message text
     * @param sheetName
     *            the sheet name
     * @param rowIndex
     *            the row index (0-based)
     * @param colIndex
     *            the column index (0-based)
     */
    private void addValidationMessage(ValidationMessageLevel level, String message, String sheetName, int rowIndex, int colIndex) {
        TemplateValidationMessage validationMessage = new TemplateValidationMessage(
            level, message, sheetName, rowIndex, colIndex
        );
        validationMessages.add(validationMessage);
        highestLevel = ValidationMessageLevel.updateLevel(highestLevel, level);
    }

    /**
     * Gets the list of validation messages collected during validation.
     * 
     * @return the list of {@link TemplateValidationMessage}
     */
    public List<TemplateValidationMessage> getValidationMessages() {
        return new ArrayList<>(validationMessages);
    }

    /**
     * Gets the highest validation level found during validation.
     * 
     * @return the highest {@link ValidationMessageLevel}
     */
    public ValidationMessageLevel getHighestLevel() {
        return highestLevel;
    }
}
