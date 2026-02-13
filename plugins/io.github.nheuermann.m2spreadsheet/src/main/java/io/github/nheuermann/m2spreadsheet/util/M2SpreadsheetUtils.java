package io.github.nheuermann.m2spreadsheet.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.eclipse.acceleo.query.runtime.IQueryEnvironment;
import org.eclipse.acceleo.query.runtime.IQueryBuilderEngine;
import org.eclipse.acceleo.query.runtime.IQueryBuilderEngine.AstResult;
import org.eclipse.acceleo.query.runtime.IQueryEvaluationEngine;
import org.eclipse.acceleo.query.runtime.EvaluationResult;
import org.eclipse.acceleo.query.runtime.IService;
import org.eclipse.acceleo.query.runtime.Query;
import org.eclipse.acceleo.query.runtime.ServiceUtils;
import org.eclipse.acceleo.query.runtime.impl.QueryBuilderEngine;
import org.eclipse.acceleo.query.runtime.impl.QueryEvaluationEngine;
import org.eclipse.emf.common.util.Monitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;

import io.github.nheuermann.m2spreadsheet.generator.GenerationResult;
import io.github.nheuermann.m2spreadsheet.services.MapServices;

/**
 * Main API entry point for M2Spreadsheet.
 * Provides utilities for generating Excel spreadsheets from templates with AQL expressions.
 * 
 * <p>Based on M2DocUtils architecture, adapted for spreadsheet generation.</p>
 * 
 * @author nheuermann
 */
public class M2SpreadsheetUtils {
    
    /**
     * The field start marker for template expressions.
     */
    public static final String FIELD_START = "{";
    
    /**
     * The m: prefix for M2Spreadsheet expressions.
     */
    public static final String M = "m:";
    
    /**
     * The M2Spreadsheet field start marker.
     */
    public static final String M_FIELD_START = FIELD_START + M;
    
    /**
     * The field end marker for template expressions.
     */
    public static final String FIELD_END = "}";
    
    /**
     * Excel extension file.
     */
    public static final String XLSX_EXTENSION_FILE = "xlsx";
    
    /**
     * Creates a query environment configured with M2Spreadsheet services.
     * This includes default AQL services plus Map access support.
     * 
     * @return the configured {@link IQueryEnvironment}
     */
    public static IQueryEnvironment createQueryEnvironment() {
        IQueryEnvironment queryEnv = Query.newEnvironmentWithDefaultServices(null);
        
        // Register Map services for HashMap property access
        Set<IService> services = ServiceUtils.getServices(queryEnv, MapServices.class);
        ServiceUtils.registerServices(queryEnv, services);
        
        // Future: Add more M2Spreadsheet-specific services here
        // services = ServiceUtils.getServices(queryEnv, SpreadsheetServices.class);
        // ServiceUtils.registerServices(queryEnv, services);
        
        return queryEnv;
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private M2SpreadsheetUtils() {
        // Utility class
    }
    
    /**
     * Generates a spreadsheet from the given template workbook and variables.
     * 
     * <p>Supports for_row loops with syntax: {m:for_row var | collection} ... {m:endfor_row}</p>
     * 
     * @param templateWorkbook the template workbook containing expressions
     * @param queryEnvironment the AQL query environment for expression evaluation
     * @param variables the variables map for template evaluation
     * @param resourceSetForModels the resource set for model elements
     * @param destinationURI the destination URI for the generated file
     * @param monitor the progress monitor
     * @return the generation result
     * @throws IOException if generation fails
     */
    public static GenerationResult generate(
            XSSFWorkbook templateWorkbook,
            IQueryEnvironment queryEnvironment,
            Map<String, Object> variables,
            ResourceSet resourceSetForModels,
            URI destinationURI,
            Monitor monitor) throws IOException {

        
        GenerationResult result = new GenerationResult();
        
        try {
            monitor.beginTask("Generating spreadsheet", 100);
            monitor.subTask("Processing template cells");
            
            // Create destination workbook (clone of template)
            XSSFWorkbook destinationWorkbook = new XSSFWorkbook();
            
            // Create style cache for copying styles from template to destination
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache = new java.util.HashMap<>();
            
            // Process each sheet in the template
            for (int i = 0; i < templateWorkbook.getNumberOfSheets(); i++) {
                Sheet templateSheet = templateWorkbook.getSheetAt(i);
                Sheet destSheet = destinationWorkbook.createSheet(templateSheet.getSheetName());
                
                processSheet(templateSheet, destSheet, variables, queryEnvironment, result, 
                           templateWorkbook, destinationWorkbook, styleCache);
            }
            
            monitor.worked(80);
            monitor.subTask("Saving workbook");
            
            // Save the workbook
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(new java.io.File(destinationURI.toFileString()))) {
                destinationWorkbook.write(fos);
            }
            
            destinationWorkbook.close();
            
            monitor.worked(20);
            
        } catch (Exception e) {
            result.getGenerationErrors().add(new Exception("Generation failed: " + e.getMessage(), e));
        } finally {
            monitor.done();
        }
        
        return result;
    }
    
    /**
     * Copy sheet formatting (column widths, default row height, etc.) from template to destination.
     * This preserves the visual formatting of the template in the generated output.
     */
    private static void copySheetFormatting(Sheet templateSheet, Sheet destSheet) {
        // Copy default row height
        destSheet.setDefaultRowHeight(templateSheet.getDefaultRowHeight());
        
        // Copy column widths for all columns that exist in template
        // We need to check a reasonable range since getLastColumnNum() doesn't exist
        // Iterate through first row to determine max column, or use a safe max
        int maxColumns = 0;
        for (int rowIdx = 0; rowIdx <= Math.min(templateSheet.getLastRowNum(), 100); rowIdx++) {
            Row row = templateSheet.getRow(rowIdx);
            if (row != null && row.getLastCellNum() > maxColumns) {
                maxColumns = row.getLastCellNum();
            }
        }
        
        // Copy column widths for all columns up to maxColumns
        for (int colIdx = 0; colIdx < maxColumns; colIdx++) {
            int width = templateSheet.getColumnWidth(colIdx);
            if (width != templateSheet.getDefaultColumnWidth()) {
                destSheet.setColumnWidth(colIdx, width);
            }
        }
        
        // Copy default column width
        destSheet.setDefaultColumnWidth(templateSheet.getDefaultColumnWidth());
    }
    
    /**
     * Process a sheet, handling for loops and regular rows.
     */
    private static void processSheet(Sheet templateSheet, Sheet destSheet, 
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            GenerationResult result,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache) {
        
        System.out.println("DEBUG: Processing sheet: " + templateSheet.getSheetName());
        System.out.println("DEBUG: Last row num: " + templateSheet.getLastRowNum());
        
        // Copy sheet formatting (column widths, etc.) from template
        copySheetFormatting(templateSheet, destSheet);
        
        // Detect sheet-level for_column loops (typically in row 0)
        // These apply to ALL rows in the sheet
        List<SheetColumnLoop> sheetColumnLoops = detectSheetColumnLoops(templateSheet);
        
        if (!sheetColumnLoops.isEmpty()) {
            System.out.println("DEBUG: Detected " + sheetColumnLoops.size() + " sheet-level for_column loop(s)");
        }
        
        int destRowNum = 0;
        int templateRowNum = 0;
        int lastRowNum = templateSheet.getLastRowNum();
        
        while (templateRowNum <= lastRowNum) {
            Row templateRow = templateSheet.getRow(templateRowNum);
            
            if (templateRow == null) {
                // Create empty row in destination to maintain spacing
                System.out.println("DEBUG: Row " + templateRowNum + " is null, creating empty row");
                destSheet.createRow(destRowNum);
                templateRowNum++;
                destRowNum++;
                continue;
            }
            
            // Check if this row contains a for loop start
            Cell firstCell = templateRow.getCell(0);
            String firstCellContent = getCellContent(firstCell);
            
            System.out.println("DEBUG: Row " + templateRowNum + ", cell A content: " + firstCellContent);
            
            if (firstCellContent != null && isForRowLoopStart(firstCellContent)) {
                System.out.println("DEBUG: Detected for_row loop at row " + templateRowNum);
                // Parse for_row loop: {m:for_row var | collection}
                ForRowLoopInfo forLoop = parseForRowLoop(firstCellContent);
                
                if (forLoop != null) {
                    System.out.println("DEBUG: Parsed for_row loop: var=" + forLoop.varName + ", collection=" + forLoop.collectionExpr);
                    // Find the endfor_row row
                    int endforRow = findEndForRowLoop(templateSheet, templateRowNum + 1);
                    System.out.println("DEBUG: Found endfor_row at row " + endforRow);
                    
                    if (endforRow > templateRowNum) {
                        // Process the for_row loop
                        destRowNum = processForRowLoop(
                            templateSheet, destSheet, 
                            templateRowNum, endforRow,
                            destRowNum, forLoop,
                            variables, queryEnvironment, result, sheetColumnLoops,
                            templateWorkbook, destinationWorkbook, styleCache);
                        
                        // Skip to after endfor
                        templateRowNum = endforRow + 1;
                        continue;
                    } else {
                        result.getValidationMessages().add(
                            "Warning: {m:for_row} at row " + templateRowNum + " has no matching {m:endfor_row}");
                    }
                }
            }
            
            // Regular row (no for loop)
            System.out.println("DEBUG: Copying regular row " + templateRowNum + " to dest row " + destRowNum);
            copyRow(templateRow, destSheet.createRow(destRowNum), variables, queryEnvironment, sheetColumnLoops,
                   templateWorkbook, destinationWorkbook, styleCache);
            destRowNum++;
            templateRowNum++;
        }
        
        System.out.println("DEBUG: Sheet processing complete, total dest rows: " + destRowNum);
    }
    
    /**
     * Inner class to hold sheet-level column loop information.
     */
    private static class SheetColumnLoop {
        int startCol;
        int endCol;
        String varName;
        String collectionExpr;
        
        SheetColumnLoop(int startCol, int endCol, String varName, String collectionExpr) {
            this.startCol = startCol;
            this.endCol = endCol;
            this.varName = varName;
            this.collectionExpr = collectionExpr;
        }
    }
    
    /**
     * Detect sheet-level for_column loops (typically in first row).
     * These column loops apply to ALL rows in the sheet.
     */
    private static List<SheetColumnLoop> detectSheetColumnLoops(Sheet templateSheet) {
        List<SheetColumnLoop> loops = new ArrayList<>();
        
        // Check first row for for_column declarations
        Row firstRow = templateSheet.getRow(0);
        if (firstRow == null) {
            return loops;
        }
        
        short lastCellNum = firstRow.getLastCellNum();
        int colIdx = 0;
        
        while (colIdx < lastCellNum) {
            Cell cell = firstRow.getCell(colIdx);
            String content = getCellContent(cell);
            
            if (content != null && isForColumnLoopStart(content)) {
                // Parse for_column
                ForColumnLoopInfo forLoop = parseForColumnLoop(content);
                if (forLoop != null) {
                    // Find matching endfor_column
                    int endforColIdx = findEndForColumnInRow(firstRow, colIdx + 1);
                    
                    if (endforColIdx > colIdx) {
                        loops.add(new SheetColumnLoop(colIdx, endforColIdx, 
                                                     forLoop.varName, forLoop.collectionExpr));
                        System.out.println("DEBUG: Sheet-level for_column: " + forLoop.varName + 
                                         " from col " + colIdx + " to " + endforColIdx);
                        colIdx = endforColIdx + 1;
                        continue;
                    }
                }
            }
            colIdx++;
        }
        
        return loops;
    }    /**
     * Process a for_row loop, repeating the body rows for each item in the collection.
     */
    private static int processForRowLoop(Sheet templateSheet, Sheet destSheet,
            int forRowNum, int endforRowNum, int destRowNum,
            ForRowLoopInfo forLoop, Map<String, Object> variables,
            IQueryEnvironment queryEnvironment, GenerationResult result,
            List<SheetColumnLoop> sheetColumnLoops,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache) {
        
        System.out.println("DEBUG: Processing for_row loop from row " + forRowNum + " to " + endforRowNum);
        System.out.println("DEBUG: Variable: " + forLoop.varName + ", Collection: " + forLoop.collectionExpr);
        
        // Evaluate the collection expression
        Object collectionObj;
        try {
            collectionObj = evaluateAqlExpression(forLoop.collectionExpr, variables, queryEnvironment);
            System.out.println("DEBUG: Collection evaluated to: " + (collectionObj != null ? collectionObj.getClass().getName() : "null"));
        } catch (Exception e) {
            System.out.println("DEBUG: Failed to evaluate collection: " + e.getMessage());
            result.getGenerationErrors().add(new Exception(
                "Failed to evaluate for_row loop collection: " + forLoop.collectionExpr, e));
            return destRowNum;
        }
        
        // Convert to iterable
        java.util.Collection<?> collection;
        if (collectionObj instanceof java.util.Collection) {
            collection = (java.util.Collection<?>) collectionObj;
        } else if (collectionObj != null) {
            collection = java.util.Collections.singletonList(collectionObj);
        } else {
            result.getValidationMessages().add(
                "Warning: For_row loop collection is null at row " + forRowNum);
            return destRowNum;
        }
        
        System.out.println("DEBUG: Collection size: " + collection.size());
        System.out.println("DEBUG: Body rows: " + (forRowNum + 1) + " to " + (endforRowNum - 1));
        
        // Iterate over collection
        int index = 0;
        for (Object item : collection) {
            // Create new variable context with loop variable
            Map<String, Object> loopVars = new java.util.HashMap<>(variables);
            loopVars.put(forLoop.varName, item);
            loopVars.put(forLoop.varName + "_index", index);
            
            if (index < 3) {  // Debug first 3 iterations
                System.out.println("DEBUG: Iteration " + index + ", item: " + item);
            }
            
            index++;
            
            // Copy body rows (between for and endfor)
            for (int bodyRowNum = forRowNum + 1; bodyRowNum < endforRowNum; bodyRowNum++) {
                Row templateRow = templateSheet.getRow(bodyRowNum);
                if (templateRow != null) {
                    Row destRow = destSheet.createRow(destRowNum);
                    if (index <= 3) {  // Debug first 3
                        System.out.println("DEBUG:   Creating dest row " + destRowNum + " from template row " + bodyRowNum);
                    }
                    copyRow(templateRow, destRow, loopVars, queryEnvironment, sheetColumnLoops,
                           templateWorkbook, destinationWorkbook, styleCache);
                    destRowNum++;
                }
            }
        }
        
        System.out.println("DEBUG: For_row loop complete, final dest row: " + destRowNum);
        return destRowNum;
    }
    
    /**
     * Copy a row from template to destination, evaluating expressions.
     * Handles for_column loops within the row and applies sheet-level column loops.
     */
    private static void copyRow(Row templateRow, Row destRow,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            List<SheetColumnLoop> sheetColumnLoops,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache) {
        
        // Check if this row contains for_column loops OR if sheet-level column loops exist
        if (hasForColumnLoop(templateRow) || (sheetColumnLoops != null && !sheetColumnLoops.isEmpty())) {
            copyRowWithColumnLoops(templateRow, destRow, variables, queryEnvironment, sheetColumnLoops,
                                  templateWorkbook, destinationWorkbook, styleCache);
        } else {
            // Regular row copy
            copyRowSimple(templateRow, destRow, variables, queryEnvironment,
                         templateWorkbook, destinationWorkbook, styleCache);
        }
    }
    
    /**
     * Check if a row contains for_column loop markers.
     */
    private static boolean hasForColumnLoop(Row row) {
        short lastCellNum = row.getLastCellNum();
        for (int colIdx = 0; colIdx < lastCellNum; colIdx++) {
            Cell cell = row.getCell(colIdx);
            String content = getCellContent(cell);
            if (content != null && isForColumnLoopStart(content)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Copy a row that contains for_column loops OR is subject to sheet-level column loops,
     * expanding columns for each iteration.
     */
    private static void copyRowWithColumnLoops(Row templateRow, Row destRow,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            List<SheetColumnLoop> sheetColumnLoops,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache) {
        
        System.out.println("DEBUG: Processing row with for_column loops");
        
        // If this row has inline for_column markers, use them
        if (hasForColumnLoop(templateRow)) {
            // Use inline for_column processing (original behavior)
            copyRowWithInlineColumnLoops(templateRow, destRow, variables, queryEnvironment,
                                        templateWorkbook, destinationWorkbook, styleCache);
            return;
        }
        
        // Otherwise, apply sheet-level column loops
        if (sheetColumnLoops != null && !sheetColumnLoops.isEmpty()) {
            copyRowWithSheetColumnLoops(templateRow, destRow, variables, queryEnvironment, sheetColumnLoops,
                                       templateWorkbook, destinationWorkbook, styleCache);
        } else {
            // Fallback to simple copy
            copyRowSimple(templateRow, destRow, variables, queryEnvironment,
                         templateWorkbook, destinationWorkbook, styleCache);
        }
    }
    
    /**
     * Copy a row using inline for_column markers.
     */
    private static void copyRowWithInlineColumnLoops(Row templateRow, Row destRow,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache) {
        
        System.out.println("DEBUG: Processing row with for_column loops");
        
        int destColIdx = 0;
        int templateColIdx = 0;
        short lastCellNum = templateRow.getLastCellNum();
        
        while (templateColIdx < lastCellNum) {
            Cell templateCell = templateRow.getCell(templateColIdx);
            String content = getCellContent(templateCell);
            
            // Check if this is a for_column start
            if (content != null && isForColumnLoopStart(content)) {
                System.out.println("DEBUG: Found for_column at column " + templateColIdx);
                
                // Parse for_column loop
                ForColumnLoopInfo forLoop = parseForColumnLoop(content);
                if (forLoop != null) {
                    // Find matching endfor_column
                    int endforColIdx = findEndForColumnInRow(templateRow, templateColIdx + 1);
                    
                    if (endforColIdx > templateColIdx) {
                        System.out.println("DEBUG: Found endfor_column at column " + endforColIdx);
                        System.out.println("DEBUG: Processing for_column: var=" + forLoop.varName + 
                                         ", collection=" + forLoop.collectionExpr);
                        
                        // Process the for_column loop
                        destColIdx = processColumnLoop(templateRow, destRow, templateColIdx, endforColIdx,
                                                     destColIdx, forLoop, variables, queryEnvironment,
                                                     templateWorkbook, destinationWorkbook, styleCache);
                        
                        // Skip to after endfor_column
                        templateColIdx = endforColIdx + 1;
                        continue;
                    } else {
                        System.err.println("WARNING: for_column at column " + templateColIdx + 
                                         " has no matching endfor_column");
                    }
                }
            }
            
            // Regular cell copy
            Cell destCell = destRow.createCell(destColIdx);
            copyCellContent(templateCell, destCell, variables, queryEnvironment,
                           templateWorkbook, destinationWorkbook, styleCache);
            destColIdx++;
            templateColIdx++;
        }
        
        // Copy row height
        destRow.setHeight(templateRow.getHeight());
    }
    
    /**
     * Copy a row using sheet-level column loops.
     * This applies column expansion based on for_column declarations in other rows (typically row 0).
     */
    private static void copyRowWithSheetColumnLoops(Row templateRow, Row destRow,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            List<SheetColumnLoop> sheetColumnLoops,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache) {
        
        int destColIdx = 0;
        int templateColIdx = 0;
        short lastCellNum = templateRow.getLastCellNum();
        
        // Process each column, expanding based on sheet-level loops
        while (templateColIdx < lastCellNum) {
            boolean processedByColumnLoop = false;
            
            // Check if this column is within any sheet-level column loop range
            for (SheetColumnLoop loop : sheetColumnLoops) {
                if (templateColIdx >= loop.startCol && templateColIdx <= loop.endCol) {
                    // Process this column range with the loop
                    destColIdx = applySheetColumnLoop(templateRow, destRow, loop,
                                                      destColIdx, variables, queryEnvironment,
                                                      templateWorkbook, destinationWorkbook, styleCache);
                    templateColIdx = loop.endCol + 1;
                    processedByColumnLoop = true;
                    break;
                }
            }
            
            if (!processedByColumnLoop) {
                // Regular cell copy
                Cell templateCell = templateRow.getCell(templateColIdx);
                Cell destCell = destRow.createCell(destColIdx);
                copyCellContent(templateCell, destCell, variables, queryEnvironment,
                               templateWorkbook, destinationWorkbook, styleCache);
                destColIdx++;
                templateColIdx++;
            }
        }
        
        // Copy row height
        destRow.setHeight(templateRow.getHeight());
    }
    
    /**
     * Apply a sheet-level column loop to a row.
     */
    private static int applySheetColumnLoop(Row templateRow, Row destRow,
            SheetColumnLoop loop, int destColIdx,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache) {
        
        // Evaluate the collection expression
        Object collectionObj;
        try {
            collectionObj = evaluateAqlExpression(loop.collectionExpr, variables, queryEnvironment);
        } catch (Exception e) {
            System.err.println("ERROR: Failed to evaluate sheet-level for_column collection: " + loop.collectionExpr);
            return destColIdx;
        }
        
        // Convert to collection
        java.util.Collection<?> collection;
        if (collectionObj instanceof java.util.Collection) {
            collection = (java.util.Collection<?>) collectionObj;
        } else if (collectionObj != null) {
            collection = java.util.Collections.singletonList(collectionObj);
        } else {
            return destColIdx;
        }
        
        // Iterate over collection
        int index = 0;
        for (Object item : collection) {
            // Create variable context with loop variable
            Map<String, Object> loopVars = new java.util.HashMap<>(variables);
            loopVars.put(loop.varName, item);
            loopVars.put(loop.varName + "_index", index++);
            
            // Copy body columns (between start and end markers)
            for (int bodyColIdx = loop.startCol + 1; bodyColIdx < loop.endCol; bodyColIdx++) {
                Cell templateCell = templateRow.getCell(bodyColIdx);
                Cell destCell = destRow.createCell(destColIdx);
                copyCellContent(templateCell, destCell, loopVars, queryEnvironment,
                               templateWorkbook, destinationWorkbook, styleCache);
                
                // Copy column width from template column to destination column
                int templateWidth = templateRow.getSheet().getColumnWidth(bodyColIdx);
                destRow.getSheet().setColumnWidth(destColIdx, templateWidth);
                
                destColIdx++;
            }
        }
        
        return destColIdx;
    }
    
    /**
     * Find endfor_column marker in a row, starting from a given column.
     */
    private static int findEndForColumnInRow(Row row, int startCol) {
        short lastCellNum = row.getLastCellNum();
        for (int colIdx = startCol; colIdx < lastCellNum; colIdx++) {
            Cell cell = row.getCell(colIdx);
            String content = getCellContent(cell);
            if (content != null && isForColumnLoopEnd(content)) {
                return colIdx;
            }
        }
        return -1;
    }
    
    /**
     * Process a for_column loop, expanding columns for each item in the collection.
     */
    private static int processColumnLoop(Row templateRow, Row destRow,
            int forColIdx, int endforColIdx, int destColIdx,
            ForColumnLoopInfo forLoop, Map<String, Object> variables,
            IQueryEnvironment queryEnvironment,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache) {
        
        // Evaluate the collection expression
        Object collectionObj;
        try {
            collectionObj = evaluateAqlExpression(forLoop.collectionExpr, variables, queryEnvironment);
            System.out.println("DEBUG: Collection evaluated to: " + 
                             (collectionObj != null ? collectionObj.getClass().getName() : "null"));
        } catch (Exception e) {
            System.err.println("ERROR: Failed to evaluate for_column collection: " + e.getMessage());
            return destColIdx;
        }
        
        // Convert to collection
        java.util.Collection<?> collection;
        if (collectionObj instanceof java.util.Collection) {
            collection = (java.util.Collection<?>) collectionObj;
        } else if (collectionObj != null) {
            collection = java.util.Collections.singletonList(collectionObj);
        } else {
            System.err.println("WARNING: For_column collection is null");
            return destColIdx;
        }
        
        System.out.println("DEBUG: Collection size: " + collection.size());
        System.out.println("DEBUG: Body columns: " + (forColIdx + 1) + " to " + (endforColIdx - 1));
        
        // Iterate over collection
        int index = 0;
        for (Object item : collection) {
            // Create new variable context with loop variable
            Map<String, Object> loopVars = new java.util.HashMap<>(variables);
            loopVars.put(forLoop.varName, item);
            loopVars.put(forLoop.varName + "_index", index);
            
            if (index < 3) {  // Debug first 3 iterations
                System.out.println("DEBUG: Column iteration " + index + ", item: " + item);
            }
            
            index++;
            
            // Copy body columns (between for_column and endfor_column)
            for (int bodyColIdx = forColIdx + 1; bodyColIdx < endforColIdx; bodyColIdx++) {
                Cell templateCell = templateRow.getCell(bodyColIdx);
                Cell destCell = destRow.createCell(destColIdx);
                
                copyCellContent(templateCell, destCell, loopVars, queryEnvironment,
                               templateWorkbook, destinationWorkbook, styleCache);
                
                // Copy column width from template column to destination column
                int templateWidth = templateRow.getSheet().getColumnWidth(bodyColIdx);
                destRow.getSheet().setColumnWidth(destColIdx, templateWidth);
                
                destColIdx++;
            }
        }
        
        System.out.println("DEBUG: For_column loop complete, final dest column: " + destColIdx);
        return destColIdx;
    }
    
    /**
     * Simple row copy without for_column processing.
     */
    private static void copyRowSimple(Row templateRow, Row destRow,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache) {
        
        // Get the last cell index to ensure we copy all columns
        short lastCellNum = templateRow.getLastCellNum();
        
        for (int cellIdx = 0; cellIdx < lastCellNum; cellIdx++) {
            Cell templateCell = templateRow.getCell(cellIdx);
            Cell destCell = destRow.createCell(cellIdx);
            
            copyCellContent(templateCell, destCell, variables, queryEnvironment,
                           templateWorkbook, destinationWorkbook, styleCache);
        }
        
        // Copy row height
        destRow.setHeight(templateRow.getHeight());
    }
    
    /**
     * Copy cell content with expression evaluation.
     */
    private static void copyCellContent(Cell templateCell, Cell destCell,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache) {
        
        if (templateCell == null) {
            return;
        }
        
        // Get cell content
        String cellContent = getCellContent(templateCell);
        
        if (cellContent != null && containsExpression(cellContent)) {
            // Evaluate and replace expressions
            String evaluated = evaluateExpressions(cellContent, variables, queryEnvironment);
            destCell.setCellValue(evaluated);
        } else if (cellContent != null) {
            // Copy as-is
            destCell.setCellValue(cellContent);
        }
        
        // Copy cell style
        copyCellStyle(templateCell, destCell, templateWorkbook, destinationWorkbook, styleCache);
    }
    
    /**
     * Copy cell style from template to destination, using cache to avoid duplicate styles.
     * Properly handles cross-workbook style copying including fonts.
     */
    private static void copyCellStyle(Cell templateCell, Cell destCell,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache) {
        
        if (templateCell == null || destCell == null) {
            return;
        }
        
        org.apache.poi.ss.usermodel.CellStyle templateStyle = templateCell.getCellStyle();
        if (templateStyle == null) {
            return;
        }
        
        short templateStyleIndex = templateStyle.getIndex();
        
        // Check if we've already cloned this style
        org.apache.poi.ss.usermodel.CellStyle destStyle = styleCache.get(templateStyleIndex);
        
        if (destStyle == null) {
            // Create new style in destination workbook
            destStyle = destinationWorkbook.createCellStyle();
            
            // Copy font first (cloneStyleFrom doesn't properly copy fonts across workbooks)
            org.apache.poi.ss.usermodel.Font templateFont = templateWorkbook.getFontAt(templateStyle.getFontIndex());
            org.apache.poi.ss.usermodel.Font destFont = destinationWorkbook.findFont(
                templateFont.getBold(),
                templateFont.getColor(),
                templateFont.getFontHeight(),
                templateFont.getFontName(),
                templateFont.getItalic(),
                templateFont.getStrikeout(),
                templateFont.getTypeOffset(),
                templateFont.getUnderline()
            );
            
            if (destFont == null) {
                // Font doesn't exist in destination, create it
                destFont = destinationWorkbook.createFont();
                destFont.setBold(templateFont.getBold());
                destFont.setColor(templateFont.getColor());
                destFont.setFontHeight(templateFont.getFontHeight());
                destFont.setFontName(templateFont.getFontName());
                destFont.setItalic(templateFont.getItalic());
                destFont.setStrikeout(templateFont.getStrikeout());
                destFont.setTypeOffset(templateFont.getTypeOffset());
                destFont.setUnderline(templateFont.getUnderline());
                if (templateFont instanceof org.apache.poi.xssf.usermodel.XSSFFont) {
                    org.apache.poi.xssf.usermodel.XSSFFont xssfTemplateFont = (org.apache.poi.xssf.usermodel.XSSFFont) templateFont;
                    org.apache.poi.xssf.usermodel.XSSFFont xssfDestFont = (org.apache.poi.xssf.usermodel.XSSFFont) destFont;
                    xssfDestFont.setCharSet(xssfTemplateFont.getCharSet());
                }
            }
            
            // Now clone the style (this copies alignment, borders, colors, etc.)
            destStyle.cloneStyleFrom(templateStyle);
            
            // Set the font (cloneStyleFrom doesn't do this properly across workbooks)
            destStyle.setFont(destFont);
            
            styleCache.put(templateStyleIndex, destStyle);
        }
        
        destCell.setCellStyle(destStyle);
    }
    
    /**
     * Check if content is a for_row loop start command.
     */
    private static boolean isForRowLoopStart(String content) {
        if (content == null) return false;
        String trimmed = content.trim();
        return trimmed.startsWith("{m:for_row ") && trimmed.endsWith("}");
    }
    
    /**
     * Check if content is a for_row loop end command.
     */
    private static boolean isForRowLoopEnd(String content) {
        if (content == null) return false;
        String trimmed = content.trim();
        return trimmed.equals("{m:endfor_row}");
    }
    
    /**
     * Parse for_row loop command: {m:for_row var | collection}
     */
    private static ForRowLoopInfo parseForRowLoop(String content) {
        // Remove {m:for_row and }
        String inner = content.trim();
        if (inner.startsWith("{m:for_row ")) {
            inner = inner.substring(11); // Remove "{m:for_row "
        }
        if (inner.endsWith("}")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        
        // Split by |
        String[] parts = inner.split("\\|");
        if (parts.length == 2) {
            String varName = parts[0].trim();
            String collectionExpr = parts[1].trim();
            return new ForRowLoopInfo(varName, collectionExpr);
        }
        
        return null;
    }
    
    /**
     * Find the row containing {m:endfor_row}
     */
    private static int findEndForRowLoop(Sheet sheet, int startRow) {
        int lastRow = sheet.getLastRowNum();
        for (int rowNum = startRow; rowNum <= lastRow; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null) {
                Cell firstCell = row.getCell(0);
                String content = getCellContent(firstCell);
                if (isForRowLoopEnd(content)) {
                    return rowNum;
                }
            }
        }
        return -1;
    }
    
    /**
     * Data class for for_row loop information.
     */
    private static class ForRowLoopInfo {
        final String varName;
        final String collectionExpr;
        
        ForRowLoopInfo(String varName, String collectionExpr) {
            this.varName = varName;
            this.collectionExpr = collectionExpr;
        }
    }
    
    // =================================================================
    // Column Repetition Support (m:for_column / m:endfor_column)
    // Implemented: Horizontal column expansion within rows
    // =================================================================
    
    /**
     * Check if content is a for_column loop start command.
     * Supports horizontal column repetition with variable binding.
     * for_column loops can be nested with for_row and support index variables.
     */
    private static boolean isForColumnLoopStart(String content) {
        if (content == null) return false;
        String trimmed = content.trim();
        return trimmed.startsWith("{m:for_column ") && trimmed.endsWith("}");
    }
    
    /**
     * Check if content is a for_column loop end command.
     */
    private static boolean isForColumnLoopEnd(String content) {
        if (content == null) return false;
        String trimmed = content.trim();
        return trimmed.equals("{m:endfor_column}");
    }
    
    /**
     * Parse for_column loop command: {m:for_column var | collection}
     * Similar to parseForRowLoop but for horizontal column iteration.
     */
    private static ForColumnLoopInfo parseForColumnLoop(String content) {
        // Remove {m:for_column and }
        String inner = content.trim();
        if (inner.startsWith("{m:for_column ")) {
            inner = inner.substring(14); // Remove "{m:for_column "
        }
        if (inner.endsWith("}")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        
        // Split by |
        String[] parts = inner.split("\\|");
        if (parts.length == 2) {
            String varName = parts[0].trim();
            String collectionExpr = parts[1].trim();
            return new ForColumnLoopInfo(varName, collectionExpr);
        }
        
        return null;
    }
    
    /**
     * Find the column containing {m:endfor_column} in the first row.
     * Searches horizontally for the endfor_column marker.
     * @deprecated - Use findEndForColumnInRow instead for row-specific search
     */
    private static int findEndForColumnLoop(Sheet sheet, int startCol) {
        Row firstRow = sheet.getRow(0);
        if (firstRow == null) return -1;
        
        int lastCol = firstRow.getLastCellNum();
        for (int colNum = startCol; colNum < lastCol; colNum++) {
            Cell cell = firstRow.getCell(colNum);
            if (cell != null) {
                String content = getCellContent(cell);
                if (isForColumnLoopEnd(content)) {
                    return colNum;
                }
            }
        }
        return -1;
    }
    
    /**
     * Data class for for_column loop information.
     */
    private static class ForColumnLoopInfo {
        final String varName;
        final String collectionExpr;
        
        ForColumnLoopInfo(String varName, String collectionExpr) {
            this.varName = varName;
            this.collectionExpr = collectionExpr;
        }
    }
    
    /**
     * Gets the string content of a cell.
     */
    private static String getCellContent(Cell cell) {
        if (cell == null) {
            return null;
        }
        
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
     * Checks if the content contains M2Spreadsheet expressions.
     */
    private static boolean containsExpression(String content) {
        return content != null && content.contains(M_FIELD_START) && content.contains(FIELD_END);
    }
    
    // =================================================================
    // Within-Cell For Loop Support (m:for / m:endfor)
    // =================================================================
    
    /**
     * Check if text contains within-cell for loop start at given position.
     */
    private static boolean isForLoopStart(String text, int pos) {
        if (text == null || pos < 0 || pos >= text.length()) return false;
        return text.startsWith("{m:for ", pos);
    }
    
    /**
     * Check if text contains within-cell for loop end at given position.
     */
    private static boolean isForLoopEnd(String text, int pos) {
        if (text == null || pos < 0 || pos >= text.length()) return false;
        return text.startsWith("{m:endfor", pos);
    }
    
    // =================================================================
    // Within-Cell If/Endif Support (m:if / m:endif)
    // =================================================================
    
    /**
     * Check if text contains within-cell if statement start at given position.
     */
    private static boolean isIfStart(String text, int pos) {
        if (text == null || pos < 0 || pos >= text.length()) return false;
        return text.startsWith("{m:if ", pos);
    }
    
    /**
     * Check if text contains within-cell if statement end at given position.
     */
    private static boolean isIfEnd(String text, int pos) {
        if (text == null || pos < 0 || pos >= text.length()) return false;
        return text.startsWith("{m:endif", pos);
    }
    
    /**
     * Inner class to hold parsed if statement information.
     */
    private static class IfInfo {
        String condition;
        int ifEndPos;  // Position after the closing }
        
        IfInfo(String condition, int ifEndPos) {
            this.condition = condition;
            this.ifEndPos = ifEndPos;
        }
    }
    
    /**
     * Parse if statement syntax: {m:if condition}
     * Returns IfInfo with condition expression and position after closing brace.
     */
    private static IfInfo parseIf(String text, int startPos) {
        if (!text.startsWith("{m:if ", startPos)) {
            return null;
        }
        
        // Find the closing }
        int closingBrace = text.indexOf('}', startPos + "{m:if ".length());
        if (closingBrace == -1) {
            System.err.println("ERROR: Malformed {m:if} - no closing brace");
            return null;
        }
        
        // Extract condition (without "m:if " prefix and closing })
        String condition = text.substring(startPos + "{m:if ".length(), closingBrace).trim();
        
        return new IfInfo(condition, closingBrace + 1);
    }
    
    /**
     * Find matching {m:endif} for {m:if} at given position, handling nesting.
     */
    private static int findMatchingEndIf(String text, int ifStartPos) {
        int depth = 0;
        int pos = ifStartPos + "{m:if ".length();
        
        while (pos < text.length()) {
            if (isIfStart(text, pos)) {
                depth++;
                pos += "{m:if ".length();
            } else if (isIfEnd(text, pos)) {
                if (depth == 0) {
                    return pos;  // Found matching endif
                }
                depth--;
                pos += "{m:endif}".length();
            } else {
                pos++;
            }
        }
        
        System.err.println("ERROR: No matching {m:endif} for {m:if} at position " + ifStartPos);
        return -1;
    }
    
    /**
     * Process within-cell if statements in the text.
     * This handles {m:if condition} ... {m:endif} patterns within a single cell.
     * Supports nesting and sequential if statements.
     */
    private static String processIfStatements(String text, Map<String, Object> variables,
            IQueryEnvironment queryEnvironment) {
        
        StringBuilder result = new StringBuilder();
        int pos = 0;
        
        while (pos < text.length()) {
            // Look for next if statement
            int ifStartPos = text.indexOf("{m:if ", pos);
            
            if (ifStartPos == -1) {
                // No more if statements, append remaining text
                result.append(text.substring(pos));
                break;
            }
            
            // Append text before the if statement
            if (ifStartPos > pos) {
                result.append(text.substring(pos, ifStartPos));
            }
            
            // Parse if statement
            IfInfo ifInfo = parseIf(text, ifStartPos);
            if (ifInfo == null) {
                // Parse error, skip this malformed if statement
                result.append(text.substring(ifStartPos, ifStartPos + "{m:if ".length()));
                pos = ifStartPos + "{m:if ".length();
                continue;
            }
            
            // Find matching endif
            int endIfPos = findMatchingEndIf(text, ifStartPos);
            if (endIfPos == -1) {
                // No matching endif, skip this if statement
                result.append(text.substring(ifStartPos, ifInfo.ifEndPos));
                pos = ifInfo.ifEndPos;
                continue;
            }
            
            // Extract body content (between {m:if ...} and {m:endif})
            String bodyContent = text.substring(ifInfo.ifEndPos, endIfPos);
            
            // Evaluate condition expression
            boolean conditionResult = false;
            try {
                Object value = evaluateAqlExpression(ifInfo.condition, variables, queryEnvironment);
                // Convert to boolean
                if (value instanceof Boolean) {
                    conditionResult = (Boolean) value;
                } else if (value instanceof Number) {
                    conditionResult = ((Number) value).doubleValue() != 0;
                } else if (value instanceof String) {
                    conditionResult = !((String) value).isEmpty();
                } else if (value instanceof java.util.Collection) {
                    conditionResult = !((java.util.Collection<?>) value).isEmpty();
                } else {
                    conditionResult = (value != null);
                }
            } catch (Exception e) {
                System.err.println("ERROR: Failed to evaluate if condition: " + ifInfo.condition);
                System.err.println("       " + e.getMessage());
                conditionResult = false;
            }
            
            // Include body content only if condition is true
            if (conditionResult) {
                // Recursively process the body (may contain nested if statements)
                result.append(processIfStatements(bodyContent, variables, queryEnvironment));
            }
            
            // Move past {m:endif}
            pos = endIfPos + "{m:endif}".length();
        }
        
        return result.toString();
    }
    
    /**
     * Inner class to hold parsed for loop information.
     */
    private static class ForLoopInfo {
        String varName;
        String collectionExpr;
        int forEndPos;  // Position after the closing }
        
        ForLoopInfo(String varName, String collectionExpr, int forEndPos) {
            this.varName = varName;
            this.collectionExpr = collectionExpr;
            this.endPos = forEndPos;
        }
        
        int endPos;
    }
    
    /**
     * Parse for loop syntax: {m:for var | collection}
     * Returns ForLoopInfo with variable name, collection expression, and position after closing brace.
     */
    private static ForLoopInfo parseForLoop(String text, int startPos) {
        // Find the closing brace
        int closeBrace = text.indexOf('}', startPos);
        if (closeBrace == -1) {
            System.err.println("ERROR: No closing brace for {m:for at position " + startPos);
            return null;
        }
        
        // Extract content: "{m:for var | collection}"
        String forContent = text.substring(startPos + "{m:for ".length(), closeBrace).trim();
        
        // Split by pipe: "var | collection"
        int pipeIdx = forContent.indexOf('|');
        if (pipeIdx == -1) {
            System.err.println("ERROR: No pipe separator in for loop: " + forContent);
            return null;
        }
        
        String varName = forContent.substring(0, pipeIdx).trim();
        String collectionExpr = forContent.substring(pipeIdx + 1).trim();
        
        if (varName.isEmpty() || collectionExpr.isEmpty()) {
            System.err.println("ERROR: Empty variable or collection in for loop");
            return null;
        }
        
        System.out.println("DEBUG: Parsed for loop: var=" + varName + ", collection=" + collectionExpr);
        
        return new ForLoopInfo(varName, collectionExpr, closeBrace + 1);
    }
    
    /**
     * Find matching {m:endfor} for a {m:for at startPos, handling nested loops.
     * Returns position of the opening brace of matching endfor, or -1 if not found.
     */
    private static int findMatchingEndFor(String text, int forStartPos) {
        int depth = 1;  // We're starting inside one for loop
        int pos = forStartPos + "{m:for".length();
        
        while (pos < text.length() && depth > 0) {
            if (isForLoopStart(text, pos)) {
                depth++;
                pos += "{m:for ".length();
                System.out.println("DEBUG: Found nested for at pos " + pos + ", depth=" + depth);
            } else if (isForLoopEnd(text, pos)) {
                depth--;
                System.out.println("DEBUG: Found endfor at pos " + pos + ", depth=" + depth);
                if (depth == 0) {
                    return pos;  // Found matching endfor
                }
                pos += "{m:endfor}".length();
            } else {
                pos++;
            }
        }
        
        System.err.println("ERROR: No matching {m:endfor} for {m:for at position " + forStartPos);
        return -1;
    }
    
    /**
     * Process within-cell for loops in the text.
     * This handles {m:for var | collection} ... {m:endfor} patterns within a single cell.
     * Supports nesting and sequential loops.
     */
    private static String processForLoops(String text, Map<String, Object> variables, 
            IQueryEnvironment queryEnvironment) {
        
        StringBuilder result = new StringBuilder();
        int pos = 0;
        
        while (pos < text.length()) {
            // Look for next for loop
            int forStartPos = text.indexOf("{m:for ", pos);
            
            if (forStartPos == -1) {
                // No more for loops, process remaining text
                String remaining = text.substring(pos);
                result.append(processNonLoopExpressions(remaining, variables, queryEnvironment));
                break;
            }
            
            // Append text before the for loop
            if (forStartPos > pos) {
                String beforeLoop = text.substring(pos, forStartPos);
                result.append(processNonLoopExpressions(beforeLoop, variables, queryEnvironment));
            }
            
            // Parse for loop
            ForLoopInfo forInfo = parseForLoop(text, forStartPos);
            if (forInfo == null) {
                // Parse error, skip this malformed for loop
                result.append(text.substring(forStartPos, forStartPos + "{m:for ".length()));
                pos = forStartPos + "{m:for ".length();
                continue;
            }
            
            // Find matching endfor
            int endForPos = findMatchingEndFor(text, forStartPos);
            if (endForPos == -1) {
                // No matching endfor, skip this for loop
                result.append(text.substring(forStartPos, forInfo.endPos));
                pos = forInfo.endPos;
                continue;
            }
            
            // Extract body content (between {m:for ...} and {m:endfor})
            String bodyContent = text.substring(forInfo.endPos, endForPos);
            
            System.out.println("DEBUG: For loop body: " + bodyContent);
            
            // Evaluate collection expression
            Object collectionObj = null;
            try {
                collectionObj = evaluateAqlExpression(forInfo.collectionExpr, variables, queryEnvironment);
            } catch (Exception e) {
                System.err.println("ERROR: Failed to evaluate collection expression: " + forInfo.collectionExpr);
                System.err.println("       " + e.getMessage());
                pos = endForPos + "{m:endfor}".length();
                continue;
            }
            
            // Check if result is iterable
            if (collectionObj instanceof Iterable) {
                Iterable<?> collection = (Iterable<?>) collectionObj;
                int index = 0;
                
                // Iterate over collection
                for (Object item : collection) {
                    // Create new variable context for this iteration
                    Map<String, Object> iterationVars = new java.util.HashMap<>(variables);
                    iterationVars.put(forInfo.varName, item);
                    iterationVars.put(forInfo.varName + "_index", index++);
                    
                    System.out.println("DEBUG:   Iteration " + index + ", item=" + item);
                    
                    // Recursively process body (may contain nested loops)
                    String iterationResult = processForLoops(bodyContent, iterationVars, queryEnvironment);
                    result.append(iterationResult);
                }
                
                System.out.println("DEBUG: For loop complete, " + index + " iterations");
            } else {
                System.err.println("ERROR: Collection expression did not evaluate to Iterable: " + collectionObj);
            }
            
            // Move past the endfor
            // Find the } after endfor
            int endForClosePos = text.indexOf('}', endForPos);
            if (endForClosePos != -1) {
                pos = endForClosePos + 1;
            } else {
                pos = endForPos + "{m:endfor}".length();
            }
        }
        
        return result.toString();
    }
    
    /**
     * Process expressions that are not for loops or if statements (regular {m:expr} patterns).
     * This is separated to avoid infinite recursion.
     */
    private static String processNonLoopExpressions(String text, Map<String, Object> variables,
            IQueryEnvironment queryEnvironment) {
        String result = text;
        
        // Find all {m:expression} patterns (but not {m:for, {m:endfor, {m:if, {m:endif})
        int startIdx = 0;
        while ((startIdx = result.indexOf(M_FIELD_START, startIdx)) != -1) {
            // Skip if this is a for, endfor, if, or endif
            if (result.startsWith("{m:for ", startIdx) || 
                result.startsWith("{m:endfor", startIdx) ||
                result.startsWith("{m:if ", startIdx) ||
                result.startsWith("{m:endif", startIdx)) {
                startIdx += M_FIELD_START.length();
                continue;
            }
            
            int endIdx = result.indexOf(FIELD_END, startIdx);
            if (endIdx == -1) {
                break;
            }
            
            // Extract expression (without {m: and })
            String expression = result.substring(startIdx + M_FIELD_START.length(), endIdx).trim();
            
            // Evaluate using AQL
            Object value;
            String errorMessage = null;
            if (queryEnvironment != null) {
                try {
                    value = evaluateAqlExpression(expression, variables, queryEnvironment);
                    
                    // Check if result is null and might be an error (property doesn't exist)
                    if (value == null && expression.contains(".")) {
                        // This looks like a property access that returned null
                        // Validate if the property actually exists
                        String[] parts = expression.split("\\.");
                        if (parts.length > 0) {
                            Object base = variables.get(parts[0]);
                            if (base != null && base instanceof Map) {
                                Map<?, ?> baseMap = (Map<?, ?>) base;
                                // Check if the last property exists in the map
                                if (parts.length > 1 && !baseMap.containsKey(parts[parts.length - 1])) {
                                    errorMessage = "ERROR: Property '" + parts[parts.length - 1] + "' not found";
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    // Fallback to simple evaluator on AQL error
                    try {
                        value = evaluateSimpleExpression(expression, variables);
                        if (value == null) {
                            // Simple evaluator returned null, treat as error
                            errorMessage = "ERROR: " + e.getMessage();
                        }
                    } catch (Exception e2) {
                        value = null;
                        errorMessage = "ERROR: " + e.getMessage();
                    }
                }
            } else {
                try {
                    value = evaluateSimpleExpression(expression, variables);
                    if (value == null) {
                        // Simple evaluator returned null, treat as error
                        errorMessage = "ERROR: Expression evaluated to null";
                    }
                } catch (Exception e) {
                    value = null;
                    errorMessage = "ERROR: " + e.getMessage();
                }
            }
            
            String replacement;
            if (errorMessage != null) {
                // Keep the original expression and append error
                replacement = result.substring(startIdx, endIdx + 1) + " [" + errorMessage + "]";
            } else {
                replacement = value != null ? value.toString() : "";
            }
            
            // Replace the expression with its value
            result = result.substring(0, startIdx) + replacement + result.substring(endIdx + 1);
            startIdx += replacement.length();
        }
        
        return result;
    }
    
    /**
     * Evaluates AQL expressions in the content string.
     * Uses Acceleo Query Language (AQL) for expression evaluation.
     * Now supports within-cell for loops and if statements.
     */
    private static String evaluateExpressions(String text, Map<String, Object> variables, 
            IQueryEnvironment queryEnvironment) {
        // First, process any if statements
        String afterIfs = processIfStatements(text, variables, queryEnvironment);
        
        // Then process any for loops (which may be nested)
        String afterLoops = processForLoops(afterIfs, variables, queryEnvironment);
        
        // The processForLoops already handles regular expressions via processNonLoopExpressions
        // So we can just return the result
        return afterLoops;
    }
    
    /**
     * DEPRECATED: Old evaluateExpressions implementation without for loop support.
     * Keeping for reference during migration.
     */
    @SuppressWarnings("unused")
    private static String evaluateExpressionsOld(String text, Map<String, Object> variables, 
            IQueryEnvironment queryEnvironment) {
        String result = text;
        
        // Find all {m:expression} patterns
        int startIdx = 0;
        while ((startIdx = result.indexOf(M_FIELD_START, startIdx)) != -1) {
            int endIdx = result.indexOf(FIELD_END, startIdx);
            if (endIdx == -1) {
                break;
            }
            
            // Extract expression (without {m: and })
            String expression = result.substring(startIdx + M_FIELD_START.length(), endIdx).trim();
            
            // Evaluate using AQL (fallback to simple evaluator if AQL fails or is null)
            Object value;
            if (queryEnvironment != null) {
                try {
                    value = evaluateAqlExpression(expression, variables, queryEnvironment);
                } catch (Exception e) {
                    // Fallback to simple evaluator on AQL error
                    value = evaluateSimpleExpression(expression, variables);
                }
            } else {
                value = evaluateSimpleExpression(expression, variables);
            }
            
            String replacement = value != null ? value.toString() : "";
            
            // Replace the expression with its value
            result = result.substring(0, startIdx) + replacement + result.substring(endIdx + 1);
            startIdx += replacement.length();
        }
        
        return result;
    }
    
    /**
     * Evaluates an AQL expression using the query environment.
     */
    private static Object evaluateAqlExpression(String expression, Map<String, Object> variables,
            IQueryEnvironment queryEnvironment) {
        // Create query builder engine
        IQueryBuilderEngine queryBuilder = new QueryBuilderEngine(queryEnvironment);
        
        // Parse the expression
        AstResult astResult = queryBuilder.build(expression);
        
        if (!astResult.getDiagnostic().getChildren().isEmpty()) {
            // If there are parsing errors, throw exception to fallback to simple evaluator
            throw new RuntimeException("AQL parse error: " + astResult.getDiagnostic());
        }
        
        // Create evaluation engine and evaluate
        IQueryEvaluationEngine evaluationEngine = new QueryEvaluationEngine(queryEnvironment);
        EvaluationResult evalResult = evaluationEngine.eval(astResult, variables);
        
        // Check for evaluation errors (not warnings) - look for ERROR severity
        if (evalResult.getDiagnostic() != null && hasErrors(evalResult.getDiagnostic())) {
            throw new RuntimeException("AQL evaluation error: " + evalResult.getDiagnostic());
        }
        
        // If there are warnings but no errors, check if we got null and fallback
        Object result = evalResult.getResult();
        if (result == null && evalResult.getDiagnostic() != null && !evalResult.getDiagnostic().getChildren().isEmpty()) {
            throw new RuntimeException("AQL returned null: " + evalResult.getDiagnostic());
        }
        
        return result;
    }
    
    /**
     * Check if a diagnostic contains errors (not just warnings).
     */
    private static boolean hasErrors(org.eclipse.emf.common.util.Diagnostic diagnostic) {
        if (diagnostic.getSeverity() >= org.eclipse.emf.common.util.Diagnostic.ERROR) {
            return true;
        }
        for (org.eclipse.emf.common.util.Diagnostic child : diagnostic.getChildren()) {
            if (hasErrors(child)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Simple expression evaluator - handles basic variable access like "customer.name".
     * TODO: Replace with proper AQL evaluation
     */
    private static Object evaluateSimpleExpression(String expression, Map<String, Object> variables) {
        // Handle simple property access: customer.name
        String[] parts = expression.split("\\.");
        
        if (parts.length == 0) {
            return null;
        }
        
        Object current = variables.get(parts[0]);
        
        // Navigate through properties
        for (int i = 1; i < parts.length && current != null; i++) {
            // For now, if current is a Map, try to get the next property
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(parts[i]);
            } else {
                // TODO: Use reflection or AQL to access properties
                // For now, just return null if we can't navigate further
                return null;
            }
        }
        
        return current;
    }
}
