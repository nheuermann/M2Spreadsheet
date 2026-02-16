package io.github.nheuermann.m2spreadsheet.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
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
import org.eclipse.emf.common.util.Diagnostic;
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
            
            // Create font cache for copying rich text fonts from template to destination
            Map<Short, XSSFFont> fontCache = new java.util.HashMap<>();
            
            // Process each sheet in the template
            for (int i = 0; i < templateWorkbook.getNumberOfSheets(); i++) {
                Sheet templateSheet = templateWorkbook.getSheetAt(i);
                Sheet destSheet = destinationWorkbook.createSheet(templateSheet.getSheetName());
                
                processSheet(templateSheet, destSheet, variables, queryEnvironment, result, 
                           templateWorkbook, destinationWorkbook, styleCache, fontCache);
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
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache,
            Map<Short, XSSFFont> fontCache) {
        
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
                            templateWorkbook, destinationWorkbook, styleCache, fontCache);
                        
                        // Skip to after endfor
                        templateRowNum = endforRow + 1;
                        continue;
                    } else {
                        // Write error to the cell
                        Row destErrorRow = destSheet.createRow(destRowNum);
                        Cell errorCell = destErrorRow.createCell(0);
                        errorCell.setCellValue(firstCellContent + "\n[ERROR] Missing {m:endfor_row} in a following row");
                        result.getValidationMessages().add(
                            "Warning: {m:for_row} at row " + templateRowNum + " has no matching {m:endfor_row}");
                        templateRowNum++;
                        destRowNum++;
                    }
                }
            }
            
            // Check for orphaned endfor_row
            if (firstCellContent != null && firstCellContent.trim().equals("{m:endfor_row}")) {
                Row destErrorRow = destSheet.createRow(destRowNum);
                Cell errorCell = destErrorRow.createCell(0);
                errorCell.setCellValue("{m:endfor_row}\n[ERROR] Missing {m:for_row} in a previous row");
                result.getValidationMessages().add(
                    "Warning: Orphaned {m:endfor_row} at row " + templateRowNum);
                templateRowNum++;
                destRowNum++;
                continue;
            }
            
            // Regular row (no for loop)
            System.out.println("DEBUG: Copying regular row " + templateRowNum + " to dest row " + destRowNum);
            copyRow(templateRow, destSheet.createRow(destRowNum), variables, queryEnvironment, sheetColumnLoops,
                   templateWorkbook, destinationWorkbook, styleCache, fontCache);
            destRowNum++;
            templateRowNum++;
        }
        
        System.out.println("DEBUG: Sheet processing complete, total dest rows: " + destRowNum);
    }
    
    /**
     * Inner class to hold sheet-level column loop information.
     * Supports nested loops via childLoops list.
     */
    private static class SheetColumnLoop {
        int startCol;
        int endCol;
        String varName;
        String collectionExpr;
        java.util.List<SheetColumnLoop> childLoops;  // Nested loops within this loop's body
        
        SheetColumnLoop(int startCol, int endCol, String varName, String collectionExpr) {
            this.startCol = startCol;
            this.endCol = endCol;
            this.varName = varName;
            this.collectionExpr = collectionExpr;
            this.childLoops = new java.util.ArrayList<>();
        }
    }
    
    /**
     * Detect sheet-level for_column loops (typically in first row).
     * These column loops apply to ALL rows in the sheet.
     * Recursively detects nested loops.
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
                        SheetColumnLoop loop = new SheetColumnLoop(colIdx, endforColIdx, 
                                                     forLoop.varName, forLoop.collectionExpr);
                        
                        // Recursively detect nested loops in the body columns
                        loop.childLoops = detectNestedColumnLoops(firstRow, colIdx + 1, endforColIdx);
                        
                        loops.add(loop);
                        System.out.println("DEBUG: Sheet-level for_column: " + forLoop.varName + 
                                         " from col " + colIdx + " to " + endforColIdx +
                                         " with " + loop.childLoops.size() + " nested loop(s)");
                        colIdx = endforColIdx + 1;
                        continue;
                    }
                }
            }
            colIdx++;
        }
        
        return loops;
    }
    
    /**
     * Detect nested column loops within a column range.
     */
    private static List<SheetColumnLoop> detectNestedColumnLoops(Row row, int startCol, int endCol) {
        List<SheetColumnLoop> nestedLoops = new ArrayList<>();
        
        int colIdx = startCol;
        while (colIdx < endCol) {
            Cell cell = row.getCell(colIdx);
            String content = getCellContent(cell);
            
            if (content != null && isForColumnLoopStart(content)) {
                ForColumnLoopInfo forLoop = parseForColumnLoop(content);
                if (forLoop != null) {
                    int endforColIdx = findEndForColumnInRow(row, colIdx + 1);
                    
                    if (endforColIdx > colIdx && endforColIdx < endCol) {
                        SheetColumnLoop nestedLoop = new SheetColumnLoop(colIdx, endforColIdx,
                                                         forLoop.varName, forLoop.collectionExpr);
                        
                        // Recursively detect nested loops within this loop
                        nestedLoop.childLoops = detectNestedColumnLoops(row, colIdx + 1, endforColIdx);
                        
                        nestedLoops.add(nestedLoop);
                        System.out.println("DEBUG:   Nested loop detected: " + forLoop.varName + 
                                         " from col " + colIdx + " to " + endforColIdx +
                                         " with " + nestedLoop.childLoops.size() + " child loop(s)");
                        colIdx = endforColIdx + 1;
                        continue;
                    }
                }
            }
            colIdx++;
        }
        
        return nestedLoops;
    }

    /**
     * Process a for_row loop, repeating the body rows for each item in the collection.
     */
    private static int processForRowLoop(Sheet templateSheet, Sheet destSheet,
            int forRowNum, int endforRowNum, int destRowNum,
            ForRowLoopInfo forLoop, Map<String, Object> variables,
            IQueryEnvironment queryEnvironment, GenerationResult result,
            List<SheetColumnLoop> sheetColumnLoops,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache,
            Map<Short, XSSFFont> fontCache) {
        
        System.out.println("DEBUG: Processing for_row loop from row " + forRowNum + " to " + endforRowNum);
        System.out.println("DEBUG: Variable: " + forLoop.varName + ", Collection: " + forLoop.collectionExpr);
        
        // Evaluate the collection expression
        AqlEvaluationResult aqlResult = evaluateAqlExpression(forLoop.collectionExpr, variables, queryEnvironment);
        Object collectionObj = aqlResult.getResult();
        
        if (aqlResult.hasError()) {
            String diagnosticMsg = formatDiagnosticMessages(aqlResult.getDiagnostic());
            System.err.println("ERROR: Failed to evaluate for_row collection: " + forLoop.collectionExpr);
            System.err.println(diagnosticMsg);
            result.getGenerationErrors().add(new Exception(
                "For_row loop collection error: " + forLoop.collectionExpr + "\n" + diagnosticMsg));
            return destRowNum;
        }
        
        System.out.println("DEBUG: Collection evaluated to: " + (collectionObj != null ? collectionObj.getClass().getName() : "null"));
        
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
        
        // Scan template body for merge_row directives
        Map<Integer, MergeInfo> mergeColumns = new java.util.HashMap<>();
        for (int scanRowNum = forRowNum + 1; scanRowNum < endforRowNum; scanRowNum++) {
            Row scanRow = templateSheet.getRow(scanRowNum);
            if (scanRow == null) continue;
            
            // Check if this is a nested for_row start or endfor_row - skip the directive row itself
            Cell firstCell = scanRow.getCell(0);
            String firstCellContent = getCellContent(firstCell);
            if (firstCellContent != null && 
                (isForRowLoopStart(firstCellContent) || isForRowLoopEnd(firstCellContent))) {
                // Skip the loop control row, but continue scanning the body
                continue;
            }
            
            // Scan cells for merge directives (even inside nested loop bodies)
            for (int colIdx = 0; colIdx <= scanRow.getLastCellNum(); colIdx++) {
                Cell cell = scanRow.getCell(colIdx);
                String cellContent = getCellContent(cell);
                if (cellContent == null) continue;
                
                String mergeRowVar = parseMergeRowDirective(cellContent);
                if (mergeRowVar != null) {
                    // Check for multiple merge_row in same cell - not allowed
                    int mergeRowCount = 0;
                    int idx = 0;
                    while ((idx = cellContent.indexOf("{m:merge_row ", idx)) != -1) {
                        mergeRowCount++;
                        idx += 13; // length of "{m:merge_row "
                    }
                    if (mergeRowCount > 1) {
                        result.getGenerationErrors().add(new Exception(
                            "ERROR at row " + scanRowNum + ", column " + colIdx + 
                            ": Multiple {m:merge_row} directives in same cell not allowed"));
                        continue;
                    }
                    
                    // Record merge directive for this column
                    if (mergeColumns.containsKey(colIdx)) {
                        MergeInfo existing = mergeColumns.get(colIdx);
                        if (!existing.varName.equals(mergeRowVar)) {
                            result.getGenerationErrors().add(new Exception(
                                "ERROR: Column " + colIdx + " has conflicting merge_row directives: " + 
                                existing.varName + " vs " + mergeRowVar));
                        }
                    } else {
                        mergeColumns.put(colIdx, new MergeInfo(mergeRowVar, colIdx, "row"));
                        System.out.println("DEBUG: Found merge_row directive: column " + colIdx + ", var=" + mergeRowVar);
                    }
                }
            }
        }
        
        // Track merge regions to apply after loop
        java.util.List<MergeRegion> mergeRegions = new java.util.ArrayList<>();
        
        // Track first row of each iteration (for merge_row)
        int iterationStartRow = destRowNum;
        
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
            
            // Track where this iteration starts
            int thisIterationStartRow = destRowNum;
            
            index++;
            
            // Process body rows (between for and endfor), checking for nested for_row loops
            int bodyRowNum = forRowNum + 1;
            while (bodyRowNum < endforRowNum) {
                Row templateRow = templateSheet.getRow(bodyRowNum);
                if (templateRow == null) {
                    destSheet.createRow(destRowNum);
                    bodyRowNum++;
                    destRowNum++;
                    continue;
                }
                
                // Check if this body row contains a nested for_row loop
                Cell firstCell = templateRow.getCell(0);
                String firstCellContent = getCellContent(firstCell);
                
                if (firstCellContent != null && isForRowLoopStart(firstCellContent)) {
                    // Nested for_row loop detected
                    if (index <= 3) {
                        System.out.println("DEBUG:   Nested for_row detected at body row " + bodyRowNum);
                    }
                    
                    ForRowLoopInfo nestedForLoop = parseForRowLoop(firstCellContent);
                    if (nestedForLoop != null) {
                        // Find the endfor_row for this nested loop
                        int nestedEndforRow = findEndForRowLoop(templateSheet, bodyRowNum + 1);
                        
                        if (nestedEndforRow > bodyRowNum && nestedEndforRow < endforRowNum) {
                            // Process the nested for_row loop recursively
                            destRowNum = processForRowLoop(
                                templateSheet, destSheet,
                                bodyRowNum, nestedEndforRow,
                                destRowNum, nestedForLoop,
                                loopVars, queryEnvironment, result, sheetColumnLoops,
                                templateWorkbook, destinationWorkbook, styleCache, fontCache);
                            
                            // Skip to after the nested loop's endfor
                            bodyRowNum = nestedEndforRow + 1;
                            continue;
                        } else {
                            // Error: nested loop's endfor not found or invalid
                            Row destRow = destSheet.createRow(destRowNum);
                            Cell errorCell = destRow.createCell(0);
                            errorCell.setCellValue(firstCellContent + "\n[ERROR] Missing {m:endfor_row} in a following row (nested loop)");
                            destRowNum++;
                            bodyRowNum++;
                            continue;
                        }
                    }
                }
                
                // Regular row (no nested for_row)
                Row destRow = destSheet.createRow(destRowNum);
                if (index <= 3) {  // Debug first 3
                    System.out.println("DEBUG:   Creating dest row " + destRowNum + " from template row " + bodyRowNum);
                }
                copyRow(templateRow, destRow, loopVars, queryEnvironment, sheetColumnLoops,
                       templateWorkbook, destinationWorkbook, styleCache, fontCache);
                destRowNum++;
                bodyRowNum++;
            }
            
            // After processing all body rows for this iteration, check if we need to record merge regions
            int thisIterationEndRow = destRowNum - 1;  // Last row of this iteration
            
            // For each column with merge_row directive matching current loop variable
            for (Map.Entry<Integer, MergeInfo> entry : mergeColumns.entrySet()) {
                int colIdx = entry.getKey();
                MergeInfo mergeInfo = entry.getValue();
                
                // Only process if this merge is for the current loop variable
                if (mergeInfo.varName.equals(forLoop.varName)) {
                    // If this iteration spans multiple rows, create merge region
                    if (thisIterationEndRow > thisIterationStartRow) {
                        // Capture content from first row of this iteration
                        Row firstRow = destSheet.getRow(thisIterationStartRow);
                        Cell firstCell = firstRow != null ? firstRow.getCell(colIdx) : null;
                        String content = getCellContent(firstCell);
                        
                        mergeRegions.add(new MergeRegion(
                            thisIterationStartRow, thisIterationEndRow,
                            colIdx, colIdx,
                            content
                        ));
                        
                        if (index <= 3) {
                            System.out.println("DEBUG: Merge region recorded: column " + colIdx + 
                                             ", rows " + thisIterationStartRow + "-" + thisIterationEndRow);
                        }
                    }
                }
            }
        }
        
        // Apply merge regions after all iterations complete
        if (!mergeRegions.isEmpty()) {
            System.out.println("DEBUG: Applying " + mergeRegions.size() + " merge regions");
            for (MergeRegion merge : mergeRegions) {
                try {
                    // Add merged region to sheet
                    destSheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        merge.firstRow, merge.lastRow, merge.firstCol, merge.lastCol));
                    
                    // Write content in first cell and clear others
                    Row firstRow = destSheet.getRow(merge.firstRow);
                    if (firstRow != null) {
                        Cell firstCell = firstRow.getCell(merge.firstCol);
                        if (firstCell == null) {
                            firstCell = firstRow.createCell(merge.firstCol);
                        }
                        // Content should already be there from copyRow, but ensure merge directive is removed
                        String cleanContent = removeMergeDirectives(merge.content);
                        if (cleanContent != null && !cleanContent.trim().isEmpty()) {
                            firstCell.setCellValue(cleanContent);
                        }
                        
                        // Clear content from other cells in the merged region
                        for (int row = merge.firstRow + 1; row <= merge.lastRow; row++) {
                            Row r = destSheet.getRow(row);
                            if (r != null) {
                                Cell c = r.getCell(merge.firstCol);
                                if (c != null) {
                                    c.setBlank();
                                }
                            }
                        }
                    }
                    
                    System.out.println("DEBUG: Applied merge: rows " + merge.firstRow + "-" + merge.lastRow + 
                                     ", column " + merge.firstCol);
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to apply merge region: " + e.getMessage());
                    result.getGenerationErrors().add(e);
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
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache,
            Map<Short, XSSFFont> fontCache) {
        
        // Check if this row contains for_column loops OR if sheet-level column loops exist
        if (hasForColumnLoop(templateRow) || (sheetColumnLoops != null && !sheetColumnLoops.isEmpty())) {
            copyRowWithColumnLoops(templateRow, destRow, variables, queryEnvironment, sheetColumnLoops,
                                  templateWorkbook, destinationWorkbook, styleCache, fontCache);
        } else {
            // Regular row copy
            copyRowSimple(templateRow, destRow, variables, queryEnvironment,
                         templateWorkbook, destinationWorkbook, styleCache, fontCache);
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
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache,
            Map<Short, XSSFFont> fontCache) {
        
        // If this row has inline for_column markers, use them (they may have merge_column directives)
        if (hasForColumnLoop(templateRow)) {
            // Use inline for_column processing (original behavior)
            copyRowWithInlineColumnLoops(templateRow, destRow, variables, queryEnvironment,
                                        templateWorkbook, destinationWorkbook, styleCache, fontCache);
            return;
        }
        
        // Otherwise, apply sheet-level column loops
        if (sheetColumnLoops != null && !sheetColumnLoops.isEmpty()) {
            copyRowWithSheetColumnLoops(templateRow, destRow, variables, queryEnvironment, sheetColumnLoops,
                                       templateWorkbook, destinationWorkbook, styleCache, fontCache);
        } else {
            // Fallback to simple copy
            copyRowSimple(templateRow, destRow, variables, queryEnvironment,
                         templateWorkbook, destinationWorkbook, styleCache, fontCache);
        }
    }
    
    /**
     * Copy a row using inline for_column markers.
     */
    private static void copyRowWithInlineColumnLoops(Row templateRow, Row destRow,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache,
            Map<Short, XSSFFont> fontCache) {
        
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
                                                     templateWorkbook, destinationWorkbook, styleCache, fontCache);
                        
                        // Skip to after endfor_column
                        templateColIdx = endforColIdx + 1;
                        continue;
                    } else {
                        // Write error to the cell
                        Cell destCell = destRow.createCell(destColIdx);
                        destCell.setCellValue(content + "\n[ERROR] Missing {m:endfor_column} in this row");
                        System.err.println("WARNING: for_column at column " + templateColIdx + 
                                         " has no matching endfor_column");
                        destColIdx++;
                        templateColIdx++;
                        continue;
                    }
                }
            }
            
            // Check for orphaned endfor_column
            if (content != null && content.trim().equals("{m:endfor_column}")) {
                Cell destCell = destRow.createCell(destColIdx);
                destCell.setCellValue("{m:endfor_column}\n[ERROR] Missing {m:for_column} in this row");
                destColIdx++;
                templateColIdx++;
                continue;
            }
            
            // Regular cell copy
            Cell destCell = destRow.createCell(destColIdx);
            copyCellContent(templateCell, destCell, variables, queryEnvironment,
                           templateWorkbook, destinationWorkbook, styleCache, fontCache);
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
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache,
            Map<Short, XSSFFont> fontCache) {
        
        System.out.println("DEBUG: copyRowWithSheetColumnLoops for row " + templateRow.getRowNum() + 
                          ", " + sheetColumnLoops.size() + " sheet loops");
        
        int destColIdx = 0;
        int templateColIdx = 0;
        short lastCellNum = templateRow.getLastCellNum();
        
        // Process cells in order: before loops, expand loops, after loops
        for (SheetColumnLoop loop : sheetColumnLoops) {
            // Copy cells BEFORE this loop
            while (templateColIdx < loop.startCol) {
                Cell templateCell = templateRow.getCell(templateColIdx);
                Cell destCell = destRow.createCell(destColIdx);
                copyCellContent(templateCell, destCell, variables, queryEnvironment,
                               templateWorkbook, destinationWorkbook, styleCache, fontCache);
                
                // Copy column width
                int templateWidth = templateRow.getSheet().getColumnWidth(templateColIdx);
                destRow.getSheet().setColumnWidth(destColIdx, templateWidth);
                
                destColIdx++;
                templateColIdx++;
            }
            
            // Expand this loop
            destColIdx = expandRowWithLoop(templateRow, destRow, loop, destColIdx,
                                          variables, queryEnvironment,
                                          templateWorkbook, destinationWorkbook, styleCache, fontCache);
            
            // Skip past the loop in template
            templateColIdx = loop.endCol + 1;
        }
        
        // Copy cells AFTER all loops
        while (templateColIdx < lastCellNum) {
            Cell templateCell = templateRow.getCell(templateColIdx);
            Cell destCell = destRow.createCell(destColIdx);
            copyCellContent(templateCell, destCell, variables, queryEnvironment,
                           templateWorkbook, destinationWorkbook, styleCache, fontCache);
            
            // Copy column width
            int templateWidth = templateRow.getSheet().getColumnWidth(templateColIdx);
            destRow.getSheet().setColumnWidth(destColIdx, templateWidth);
            
            destColIdx++;
            templateColIdx++;
        }
        
        // Copy row height
        destRow.setHeight(templateRow.getHeight());
    }
    
    /**
     * Expand a template row for all iterations of a loop and its nested loops.
     * This creates output columns for each combination of loop iterations, 
     * applying template cells with the appropriate variable context.
     * 
     * This works analogously to processForRowLoop: iterate over body columns,
     * and for each column, either process a nested loop (using childLoops)
     * or copy a regular cell.
     */
    private static int expandRowWithLoop(Row templateRow, Row destRow,
            SheetColumnLoop loop, int destColIdx,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache,
            Map<Short, XSSFFont> fontCache) {
        
        System.out.println("DEBUG: expandRowWithLoop for var=" + loop.varName + 
                          ", cols " + loop.startCol + "-" + loop.endCol + 
                          ", " + loop.childLoops.size() + " children, row=" + templateRow.getRowNum());
        
        // Evaluate the collection expression
        AqlEvaluationResult aqlResult = evaluateAqlExpression(loop.collectionExpr, variables, queryEnvironment);
        Object collectionObj = aqlResult.getResult();
        
        if (aqlResult.hasError()) {
            String diagnosticMsg = formatDiagnosticMessages(aqlResult.getDiagnostic());
            System.err.println("ERROR: Failed to evaluate sheet-level for_column collection: " + loop.collectionExpr);
            System.err.println(diagnosticMsg);
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
        
        System.out.println("DEBUG: Collection size: " + collection.size() + ", starting destColIdx=" + destColIdx);
        
        // Scan template body for merge_column directives
        // Scan the CURRENT row being processed, not row 0, since nested loops have directives in their own rows
        int rowNum = templateRow.getRowNum();
        Map<Integer, MergeInfo> mergeCells = new java.util.HashMap<>();
        
        if (templateRow != null) {
            for (int scanColIdx = loop.startCol + 1; scanColIdx < loop.endCol; scanColIdx++) {
                Cell scanCell = templateRow.getCell(scanColIdx);
                String cellContent = getCellContent(scanCell);
                if (cellContent == null) continue;
                
                // Skip nested for_column markers
                if (isForColumnLoopStart(cellContent) || isForColumnLoopEnd(cellContent)) {
                    continue;
                }
                
                String mergeColVar = parseMergeColumnDirective(cellContent);
                if (mergeColVar != null) {
                    // Check for multiple merge_column in same cell - not allowed
                    int mergeColCount = 0;
                    int idx = 0;
                    while ((idx = cellContent.indexOf("{m:merge_column ", idx)) != -1) {
                        mergeColCount++;
                        idx += 16; // length of "{m:merge_column "
                    }
                    if (mergeColCount > 1) {
                        System.err.println("ERROR at row " + rowNum + ", column " + scanColIdx + 
                            ": Multiple {m:merge_column} directives in same cell not allowed");
                        continue;
                    }
                    
                    // Record merge directive for this column
                    if (mergeCells.containsKey(scanColIdx)) {
                        MergeInfo existing = mergeCells.get(scanColIdx);
                        if (!existing.varName.equals(mergeColVar)) {
                            System.err.println("ERROR: Column " + scanColIdx + " has conflicting merge_column directives: " + 
                                existing.varName + " vs " + mergeColVar);
                        }
                    } else {
                        mergeCells.put(scanColIdx, new MergeInfo(mergeColVar, scanColIdx, "column"));
                        System.out.println("DEBUG: Found merge_column directive: column " + scanColIdx + ", var=" + mergeColVar);
                    }
                }
            }
        }
        
        // Track merge regions to apply after loop
        java.util.List<MergeRegion> mergeRegions = new java.util.ArrayList<>();
        
        // Iterate over collection
        int index = 0;
        for (Object item : collection) {
            // Create variable context with loop variable
            Map<String, Object> loopVars = new java.util.HashMap<>(variables);
            loopVars.put(loop.varName, item);
            loopVars.put(loop.varName + "_index", index);
            
            if (index < 2) {
                System.out.println("DEBUG: Iteration " + index + " of " + loop.varName);
            }
            
            // Track where this iteration starts
            int thisIterationStartCol = destColIdx;
            
            index++;
            
            // Process body columns (like processForRowLoop processes body rows)
            // For sheet-level column loops, the startCol contains the loop marker in row 0,
            // and endCol contains the end marker. The body is startCol+1 to endCol-1.
            // This is analogous to for_row where markers are in separate rows from body.
            int templateColIdx = loop.startCol + 1;
            
            while (templateColIdx < loop.endCol) {
                // Check if templateColIdx is the start of a child loop
                SheetColumnLoop childLoop = null;
                for (SheetColumnLoop child : loop.childLoops) {
                    if (child.startCol == templateColIdx) {
                        childLoop = child;
                        break;
                    }
                }
                
                if (childLoop != null) {
                    // This column starts a nested loop - process it recursively
                    if (index <= 2) {
                        System.out.println("DEBUG:   Processing child loop at col " + templateColIdx + 
                                         " (var=" + childLoop.varName + ")");
                    }
                    destColIdx = expandRowWithLoop(templateRow, destRow, childLoop, destColIdx,
                                                  loopVars, queryEnvironment,
                                                  templateWorkbook, destinationWorkbook, styleCache, fontCache);
                    // Skip past the nested loop
                    templateColIdx = childLoop.endCol + 1;
                    continue;
                }
                
                // Regular cell - copy it (but skip loop markers from row 0)
                Cell templateCell = templateRow.getCell(templateColIdx);
                String content = getCellContent(templateCell);
                
                // Skip loop control markers (they only appear in row 0, but we process all rows)
                if (content != null && (isForColumnLoopStart(content) || content.trim().equals("{m:endfor_column}"))) {
                    templateColIdx++;
                    continue;
                }
                
                // Copy the cell
                if (templateRow.getRowNum() <= 2 && index <= 2) {
                    System.out.println("DEBUG:     Copying cell from templateCol " + templateColIdx +  
                                      " to destCol " + destColIdx + " (row " + templateRow.getRowNum() + ")");
                }
                Cell destCell = destRow.createCell(destColIdx);
                copyCellContent(templateCell, destCell, loopVars, queryEnvironment,
                               templateWorkbook, destinationWorkbook, styleCache, fontCache);
                
                // Copy column width
                int templateWidth = templateRow.getSheet().getColumnWidth(templateColIdx);
                destRow.getSheet().setColumnWidth(destColIdx, templateWidth);
                
                destColIdx++;
                templateColIdx++;
            }
            
            // After processing all body columns for this iteration, check if we need to record merge regions
            int thisIterationEndCol = destColIdx - 1;  // Last column of this iteration
            
            // For each column with merge_column directive matching current loop variable
            for (Map.Entry<Integer, MergeInfo> entry : mergeCells.entrySet()) {
                int colIdx = entry.getKey();
                MergeInfo mergeInfo = entry.getValue();
                
                if (mergeInfo.varName.equals(loop.varName)) {
                    // If this iteration spans multiple columns, create merge region
                    if (thisIterationEndCol > thisIterationStartCol) {
                        // Merge spans entire iteration width, starting at first column
                        // (mirrors merge_row which spans entire iteration height in one column)
                        
                        // Capture content from first cell of iteration
                        Cell firstCell = destRow.getCell(thisIterationStartCol);
                        String content = getCellContent(firstCell);
                        
                        // Create merge region spanning entire iteration
                        mergeRegions.add(new MergeRegion(
                            rowNum, rowNum,  // Single row
                            thisIterationStartCol, thisIterationEndCol,  // Entire iteration span
                            content
                        ));
                        
                        if (index <= 3) {
                            System.out.println("DEBUG: Merge region recorded: row " + rowNum + 
                                             ", columns " + thisIterationStartCol + "-" + thisIterationEndCol);
                        }
                    }
                }
            }
        }
        
        // Apply merge regions after all iterations complete
        if (!mergeRegions.isEmpty()) {
            System.out.println("DEBUG: Applying " + mergeRegions.size() + " merge regions");
            Sheet sheet = destRow.getSheet();
            for (MergeRegion merge : mergeRegions) {
                try {
                    // Add merged region to sheet
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        merge.firstRow, merge.lastRow, merge.firstCol, merge.lastCol));
                    
                    // Write content in first cell and clear others
                    Row row = sheet.getRow(merge.firstRow);
                    if (row != null) {
                        Cell firstCell = row.getCell(merge.firstCol);
                        if (firstCell == null) {
                            firstCell = row.createCell(merge.firstCol);
                        }
                        // Content should already be there from copyCellContent, but ensure merge directive is removed
                        String cleanContent = removeMergeDirectives(merge.content);
                        if (cleanContent != null && !cleanContent.trim().isEmpty()) {
                            firstCell.setCellValue(cleanContent);
                        }
                        
                        // Clear content from other cells in the merged region
                        for (int col = merge.firstCol + 1; col <= merge.lastCol; col++) {
                            Cell c = row.getCell(col);
                            if (c != null) {
                                c.setBlank();
                            }
                        }
                    }
                    
                    System.out.println("DEBUG: Applied merge: row " + merge.firstRow + 
                                     ", columns " + merge.firstCol + "-" + merge.lastCol);
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to apply merge region: " + e.getMessage());
                }
            }
        }
        
        return destColIdx;
    }
    
    /**
     * OLD IMPLEMENTATION - Remove or replace
     */
    private static void copyRowWithSheetColumnLoops_OLD(Row templateRow, Row destRow,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            List<SheetColumnLoop> sheetColumnLoops,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache,
            Map<Short, XSSFFont> fontCache) {
        
        System.out.println("DEBUG: copyRowWithSheetColumnLoops for row " + templateRow.getRowNum() + 
                          ", " + sheetColumnLoops.size() + " sheet loops");
        
        int destColIdx = 0;
        int templateColIdx = 0;
        short lastCellNum = templateRow.getLastCellNum();
        
        System.out.println("DEBUG:   lastCellNum=" + lastCellNum);
    }
    
    /**
     * Apply a sheet-level column loop to a row.
     */
    private static int applySheetColumnLoop(Row templateRow, Row destRow,
            SheetColumnLoop loop, int destColIdx,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache,
            Map<Short, XSSFFont> fontCache) {
        
        // Evaluate the collection expression
        AqlEvaluationResult aqlResult = evaluateAqlExpression(loop.collectionExpr, variables, queryEnvironment);
        Object collectionObj = aqlResult.getResult();
        
        if (aqlResult.hasError()) {
            String diagnosticMsg = formatDiagnosticMessages(aqlResult.getDiagnostic());
            System.err.println("ERROR: Failed to evaluate sheet-level for_column collection: " + loop.collectionExpr);
            System.err.println(diagnosticMsg);
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
            
            // Process body columns (between start and end markers)
            // Check if we have pre-detected child loops to apply
            if (!loop.childLoops.isEmpty()) {
                // Apply child loops recursively
                for (SheetColumnLoop childLoop : loop.childLoops) {
                    destColIdx = applySheetColumnLoop(
                        templateRow, destRow, childLoop, destColIdx,
                        loopVars, queryEnvironment,
                        templateWorkbook, destinationWorkbook, styleCache, fontCache
                    );
                }
            } else {
                // No child loops - process body columns directly
                int bodyColIdx = loop.startCol + 1;
                while (bodyColIdx < loop.endCol) {
                    Cell templateCell = templateRow.getCell(bodyColIdx);
                    String cellContent = getCellContent(templateCell);
                    
                    // Skip loop markers
                    if (cellContent != null && 
                        (isForColumnLoopStart(cellContent) || isForColumnLoopEnd(cellContent))) {
                        bodyColIdx++;
                        continue;
                    }
                    
                    // Regular column (copy cell)
                    Cell destCell = destRow.createCell(destColIdx);
                    copyCellContent(templateCell, destCell, loopVars, queryEnvironment,
                                   templateWorkbook, destinationWorkbook, styleCache, fontCache);
                    
                    // Copy column width
                    int templateWidth = templateRow.getSheet().getColumnWidth(bodyColIdx);
                    destRow.getSheet().setColumnWidth(destColIdx, templateWidth);
                    
                    destColIdx++;
                    bodyColIdx++;
                }
            }
        }
        
        return destColIdx;
    }
    
    /**
     * Find endfor_column marker in a row, starting from a given column.
     */
    /**
     * Find the column containing the matching {m:endfor_column} for a for_column loop.
     * Handles nested for_column loops by counting depth.
     */
    private static int findEndForColumnInRow(Row row, int startCol) {
        short lastCellNum = row.getLastCellNum();
        int depth = 1;  // Start at depth 1 (we're inside a for_column)
        
        for (int colIdx = startCol; colIdx < lastCellNum; colIdx++) {
            Cell cell = row.getCell(colIdx);
            String content = getCellContent(cell);
            
            // Check for nested for_column (increases depth)
            if (content != null && isForColumnLoopStart(content)) {
                depth++;
            }
            // Check for endfor_column (decreases depth)
            else if (content != null && isForColumnLoopEnd(content)) {
                depth--;
                if (depth == 0) {
                    return colIdx;
                }
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
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache,
            Map<Short, XSSFFont> fontCache) {
        
        return processColumnLoopInternal(templateRow, destRow, forColIdx, endforColIdx, destColIdx,
            forLoop, variables, queryEnvironment, templateWorkbook, destinationWorkbook, 
            styleCache, fontCache, null);
    }
    
    /**
     * Internal column loop processing with optional merge tracking.
     */
    private static int processColumnLoopInternal(Row templateRow, Row destRow,
            int forColIdx, int endforColIdx, int destColIdx,
            ForColumnLoopInfo forLoop, Map<String, Object> variables,
            IQueryEnvironment queryEnvironment,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache,
            Map<Short, XSSFFont> fontCache,
            java.util.List<MergeRegion> parentMergeRegions) {
        
        // Evaluate the collection expression
        AqlEvaluationResult aqlResult = evaluateAqlExpression(forLoop.collectionExpr, variables, queryEnvironment);
        Object collectionObj = aqlResult.getResult();
        
        if (aqlResult.hasError()) {
            String diagnosticMsg = formatDiagnosticMessages(aqlResult.getDiagnostic());
            System.err.println("ERROR: Failed to evaluate for_column collection: " + forLoop.collectionExpr);
            System.err.println(diagnosticMsg);
            return destColIdx;
        }
        
        System.out.println("DEBUG: Collection evaluated to: " + 
                         (collectionObj != null ? collectionObj.getClass().getName() : "null"));
        
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
        
        // Scan template body for merge_column directives
        int rowNum = templateRow.getRowNum();
        java.util.List<MergeInfo> mergeCells = new java.util.ArrayList<>();
        for (int scanColIdx = forColIdx + 1; scanColIdx < endforColIdx; scanColIdx++) {
            Cell scanCell = templateRow.getCell(scanColIdx);
            String cellContent = getCellContent(scanCell);
            if (cellContent == null) continue;
            
            // Skip nested for_column markers
            if (isForColumnLoopStart(cellContent) || isForColumnLoopEnd(cellContent)) {
                continue;
            }
            
            String mergeColVar = parseMergeColumnDirective(cellContent);
            String mergeRowVar = parseMergeRowDirective(cellContent);
            
            if (mergeColVar != null || mergeRowVar != null) {
                // Check for multiple directives of same type - not allowed
                if (mergeColVar != null) {
                    int count = 0;
                    int idx = 0;
                    while ((idx = cellContent.indexOf("{m:merge_column ", idx)) != -1) {
                        count++;
                        idx += 16;
                    }
                    if (count > 1) {
                        System.err.println("ERROR: Multiple {m:merge_column} directives in same cell at column " + scanColIdx);
                        continue;
                    }
                }
                if (mergeRowVar != null) {
                    int count = 0;
                    int idx = 0;
                    while ((idx = cellContent.indexOf("{m:merge_row ", idx)) != -1) {
                        count++;
                        idx += 13;
                    }
                    if (count > 1) {
                        System.err.println("ERROR: Multiple {m:merge_row} directives in same cell at column " + scanColIdx);
                        continue;
                    }
                }
                
                // Record merge directive(s)
                if (mergeColVar != null && mergeColVar.equals(forLoop.varName)) {
                    mergeCells.add(new MergeInfo(mergeColVar, scanColIdx, "column"));
                    System.out.println("DEBUG: Found merge_column directive: column " + scanColIdx + ", var=" + mergeColVar);
                }
                // Note: merge_row directives in column loops are handled by parent row loop
            }
        }
        
        // Track merge regions to apply after loop
        java.util.List<MergeRegion> mergeRegions = new java.util.ArrayList<>();
        
        // Track first column of each iteration (for merge_column)
        int iterationStartCol = destColIdx;
        
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
            
            // Track where this iteration starts
            int thisIterationStartCol = destColIdx;
            
            index++;
            
            // Process body columns (between for_column and endfor_column), checking for nested for_column loops
            int bodyColIdx = forColIdx + 1;
            while (bodyColIdx < endforColIdx) {
                Cell templateCell = templateRow.getCell(bodyColIdx);
                String cellContent = getCellContent(templateCell);
                
                // Check if this body column contains a nested for_column loop
                if (cellContent != null && isForColumnLoopStart(cellContent)) {
                    // Nested for_column loop detected
                    if (index <= 3) {
                        System.out.println("DEBUG:   Nested for_column detected at body column " + bodyColIdx);
                    }
                    
                    ForColumnLoopInfo nestedForLoop = parseForColumnLoop(cellContent);
                    if (nestedForLoop != null) {
                        // Find the endfor_column for this nested loop
                        int nestedEndforCol = findEndForColumnInRow(templateRow, bodyColIdx + 1);
                        
                        if (nestedEndforCol > bodyColIdx && nestedEndforCol < endforColIdx) {
                            // Process the nested for_column loop recursively
                            destColIdx = processColumnLoopInternal(
                                templateRow, destRow,
                                bodyColIdx, nestedEndforCol,
                                destColIdx, nestedForLoop,
                                loopVars, queryEnvironment,
                                templateWorkbook, destinationWorkbook, styleCache, fontCache,
                                mergeRegions);
                            
                            // Skip to after the nested loop's endfor
                            bodyColIdx = nestedEndforCol + 1;
                            continue;
                        } else {
                            // Error: nested loop's endfor not found or invalid
                            Cell destCell = destRow.createCell(destColIdx);
                            destCell.setCellValue(cellContent + "\n[ERROR] Missing {m:endfor_column} in this row (nested loop)");
                            destColIdx++;
                            bodyColIdx++;
                            continue;
                        }
                    }
                }
                
                // Regular column (no nested for_column)
                Cell destCell = destRow.createCell(destColIdx);
                copyCellContent(templateCell, destCell, loopVars, queryEnvironment,
                               templateWorkbook, destinationWorkbook, styleCache, fontCache);
                
                // Copy column width from template column to destination column
                int templateWidth = templateRow.getSheet().getColumnWidth(bodyColIdx);
                destRow.getSheet().setColumnWidth(destColIdx, templateWidth);
                
                destColIdx++;
                bodyColIdx++;
            }
            
            // After processing all body columns for this iteration, check if we need to record merge regions
            int thisIterationEndCol = destColIdx - 1;  // Last column of this iteration
            
            // For each cell with merge_column directive matching current loop variable
            for (MergeInfo mergeInfo : mergeCells) {
                if (mergeInfo.varName.equals(forLoop.varName)) {
                    // If this iteration spans multiple columns, create merge region
                    if (thisIterationEndCol > thisIterationStartCol) {
                        // Merge spans entire iteration width, starting at first column
                        // (mirrors merge_row which spans entire iteration height in one column)
                        
                        // Capture content from first cell of iteration
                        Cell firstCell = destRow.getCell(thisIterationStartCol);
                        String content = getCellContent(firstCell);
                        
                        // Check if cell also has merge_row directive
                        String cellContent = getCellContent(templateRow.getCell(mergeInfo.columnIndex));
                        String mergeRowVar = parseMergeRowDirective(cellContent);
                        
                        // Create merge region spanning entire iteration
                        mergeRegions.add(new MergeRegion(
                            rowNum, rowNum,  // Single row for now
                            thisIterationStartCol, thisIterationEndCol,  // Entire iteration span
                            content
                        ));
                        
                        if (index <= 3) {
                            System.out.println("DEBUG: Merge region recorded: row " + rowNum + 
                                             ", columns " + thisIterationStartCol + "-" + thisIterationEndCol);
                        }
                    }
                }
            }
        }
        
        // Apply merge regions after all iterations complete
        if (!mergeRegions.isEmpty() && parentMergeRegions == null) {
            System.out.println("DEBUG: Applying " + mergeRegions.size() + " merge_column regions");
            Sheet sheet = destRow.getSheet();
            for (MergeRegion merge : mergeRegions) {
                try {
                    // Add merged region to sheet
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        merge.firstRow, merge.lastRow, merge.firstCol, merge.lastCol));
                    
                    // Write content in first cell and clear others
                    Row row = sheet.getRow(merge.firstRow);
                    if (row != null) {
                        Cell firstCell = row.getCell(merge.firstCol);
                        if (firstCell == null) {
                            firstCell = row.createCell(merge.firstCol);
                        }
                        // Content should already be there, but ensure merge directive is removed
                        String cleanContent = removeMergeDirectives(merge.content);
                        if (cleanContent != null && !cleanContent.trim().isEmpty()) {
                            firstCell.setCellValue(cleanContent);
                        }
                        
                        // Clear content from other cells in the merged region
                        for (int col = merge.firstCol + 1; col <= merge.lastCol; col++) {
                            Cell c = row.getCell(col);
                            if (c != null) {
                                c.setBlank();
                            }
                        }
                    }
                    
                    System.out.println("DEBUG: Applied merge: row " + merge.firstRow + 
                                     ", columns " + merge.firstCol + "-" + merge.lastCol);
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to apply merge_column region: " + e.getMessage());
                }
            }
        } else if (!mergeRegions.isEmpty() && parentMergeRegions != null) {
            // Pass merge regions up to parent (for nested loops)
            parentMergeRegions.addAll(mergeRegions);
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
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache,
            Map<Short, XSSFFont> fontCache) {
        
        // Get the last cell index to ensure we copy all columns
        short lastCellNum = templateRow.getLastCellNum();
        
        for (int cellIdx = 0; cellIdx < lastCellNum; cellIdx++) {
            Cell templateCell = templateRow.getCell(cellIdx);
            Cell destCell = destRow.createCell(cellIdx);
            
            copyCellContent(templateCell, destCell, variables, queryEnvironment,
                           templateWorkbook, destinationWorkbook, styleCache, fontCache);
        }
        
        // Copy row height
        destRow.setHeight(templateRow.getHeight());
    }
    
    /**
     * Copy cell content with expression evaluation, preserving rich text formatting.
     */
    private static void copyCellContent(Cell templateCell, Cell destCell,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, org.apache.poi.ss.usermodel.CellStyle> styleCache,
            Map<Short, XSSFFont> fontCache) {
        
        if (templateCell == null) {
            return;
        }
        
        // Get cell content as rich text (preserves formatting)
        RichTextContent richContent = getRichTextContent(templateCell);
        
        if (richContent != null && containsExpression(richContent.text)) {
            // Evaluate and replace expressions, preserving formatting
            RichTextContent evaluated = evaluateRichTextExpressions(richContent, variables, queryEnvironment);
            
            // Apply rich text to destination cell (if XSSF cell)
            if (destCell instanceof XSSFCell) {
                applyCellRichText(evaluated, (XSSFCell) destCell, templateWorkbook, destinationWorkbook, fontCache);
            } else {
                // Fallback for non-XSSF cells
                destCell.setCellValue(evaluated != null ? evaluated.text : "");
            }
        } else if (richContent != null) {
            // Copy as-is with formatting
            if (destCell instanceof XSSFCell) {
                applyCellRichText(richContent, (XSSFCell) destCell, templateWorkbook, destinationWorkbook, fontCache);
            } else {
                destCell.setCellValue(richContent.text);
            }
        }
        
        // Copy cell style (base formatting)
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
     * Apply rich text content to a cell, preserving formatting runs.
     * Uses a font cache to avoid creating duplicate fonts in the destination workbook.
     * 
     * @param richText The rich text content to apply
     * @param destCell The destination cell (must be XSSFCell)
     * @param templateWorkbook The template workbook (for font lookups)
     * @param destinationWorkbook The destination workbook
     * @param fontCache Cache mapping template fonts to destination fonts (by font index)
     */
    private static void applyCellRichText(RichTextContent richText, XSSFCell destCell,
            XSSFWorkbook templateWorkbook, XSSFWorkbook destinationWorkbook,
            Map<Short, XSSFFont> fontCache) {
        
        if (richText == null || richText.text == null || richText.text.isEmpty()) {
            destCell.setCellValue("");
            return;
        }
        
        // If there are no formatting runs or only one run covering the whole text,
        // we can use simple cell value (the cell style will handle formatting)
        if (richText.formattingRuns == null || richText.formattingRuns.isEmpty()) {
            destCell.setCellValue(richText.text);
            return;
        }
        
        // Check if all runs use the same font - if so, we can use simple cell value
        if (richText.formattingRuns.size() == 1) {
            FormattingRun singleRun = richText.formattingRuns.get(0);
            if (singleRun.startIndex == 0 && singleRun.endIndex == richText.text.length()) {
                destCell.setCellValue(richText.text);
                return;
            }
        }
        
        // Need to create rich text string with formatting runs
        XSSFRichTextString xssfRichText = new XSSFRichTextString(richText.text);
        
        for (FormattingRun run : richText.formattingRuns) {
            if (run.font == null) {
                continue; // Skip runs without font
            }
            
            // Look up or create the font in destination workbook
            XSSFFont destFont = getOrCreateFont(run.font, templateWorkbook, destinationWorkbook, fontCache);
            
            // Apply the font to this character range
            // POI uses start index and length for applyFont
            if (run.startIndex < run.endIndex && run.startIndex < richText.text.length()) {
                int length = Math.min(run.endIndex, richText.text.length()) - run.startIndex;
                xssfRichText.applyFont(run.startIndex, run.startIndex + length, destFont);
            }
        }
        
        destCell.setCellValue(xssfRichText);
    }
    
    /**
     * Get or create a font in the destination workbook that matches the template font.
     * Uses the font cache to avoid creating duplicate fonts.
     * 
     * @param templateFont The font from the template
     * @param templateWorkbook The template workbook
     * @param destinationWorkbook The destination workbook
     * @param fontCache Cache mapping template font indexes to destination fonts (NOTE: only works for cell styles, not rich text!)
     * @return The matching font in the destination workbook
     */
    private static XSSFFont getOrCreateFont(XSSFFont templateFont, XSSFWorkbook templateWorkbook,
            XSSFWorkbook destinationWorkbook, Map<Short, XSSFFont> fontCache) {
        
        if (templateFont == null) {
            return null;
        }
        
        // Create a signature key based on font properties, not just index
        // (Rich text fonts can have the same index but different properties!)
        String fontSignature = String.format("%s-%s-%s-%s-%s-%s-%s-%s",
            templateFont.getBold(),
            templateFont.getItalic(),
            templateFont.getUnderline(),
            templateFont.getStrikeout(),
            templateFont.getColor(),
            templateFont.getFontHeight(),
            templateFont.getFontName(),
            templateFont.getTypeOffset()
        );
        
        // Try to find existing matching font in destination
        XSSFFont destFont = destinationWorkbook.findFont(
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
            destFont.setCharSet(templateFont.getCharSet());
        }
        
        // Note: Not caching rich text fonts because they can have same index but different properties
        // The findFont() method above already provides efficient lookuptrue
        return destFont;
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
    /**
     * Find the row containing the matching {m:endfor_row} for a for_row loop.
     * Handles nested for_row loops by counting depth.
     */
    private static int findEndForRowLoop(Sheet sheet, int startRow) {
        int lastRow = sheet.getLastRowNum();
        int depth = 1;  // Start at depth 1 (we're inside a for_row)
        
        for (int rowNum = startRow; rowNum <= lastRow; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null) {
                Cell firstCell = row.getCell(0);
                String content = getCellContent(firstCell);
                
                // Check for nested for_row (increases depth)
                if (content != null && isForRowLoopStart(content)) {
                    depth++;
                }
                // Check for endfor_row (decreases depth)
                else if (isForRowLoopEnd(content)) {
                    depth--;
                    if (depth == 0) {
                        return rowNum;
                    }
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
    // Cell Merging Support (m:merge_row / m:merge_column)
    // Allows merging cells across loop iterations
    // 
    // USAGE:
    // 
    // 1. merge_row: Merges cells vertically across for_row loop iterations
    //    Syntax: {m:merge_row variablename}
    //    Example:
    //      {m:for_row component | components}
    //        {m:for_row detail | component.details}
    //          {m:merge_row component}{m:component.name}    {m:detail.name}
    //        {m:endfor_row}
    //      {m:endfor_row}
    //    Result: Component name is merged across all detail rows of that component
    //
    // 2. merge_column: Merges cells horizontally across for_column loop iterations
    //    Syntax: {m:merge_column variablename}
    //    Example:
    //      {m:for_column phase | phases}
    //        {m:merge_column phase}{m:phase.name}
    //        {m:for_column task | phase.tasks}
    //          {m:task.name}
    //        {m:endfor_column}
    //      {m:endfor_column}
    //    Result: Phase name is merged across all task columns of that phase
    //
    // 3. Combination: Both directives can be used together
    //    Syntax: {m:merge_row var1}{m:merge_column var2}
    //    Example:
    //      {m:for_row component | components}
    //        {m:for_column status | statuses}
    //          {m:merge_row component}{m:component.name}
    //        {m:endfor_column}
    //      {m:endfor_row}
    //    Result: Component name is merged both vertically and horizontally
    //
    // VALIDATION:
    // - Multiple merge_row directives in same cell: ERROR
    // - Multiple merge_column directives in same cell: ERROR
    // - Both merge_row and merge_column in same cell: OK (2D merge)
    //
    // IMPLEMENTATION:
    // - Merge directives are automatically removed from output
    // - Merges are applied after loop completion
    // - Content is written to first cell of merged region
    // - Other cells in merged region are cleared
    // =================================================================
    
    /**
     * Information about a merge directive in a template cell.
     */
    private static class MergeInfo {
        final String varName;       // Variable name to match (e.g., "component" in {m:merge_row component})
        final int columnIndex;      // Column where merge should occur
        final String direction;     // "row" or "column"
        
        MergeInfo(String varName, int columnIndex, String direction) {
            this.varName = varName;
            this.columnIndex = columnIndex;
            this.direction = direction;
        }
    }
    
    /**
     * Tracks a merge region to be applied after loop completion.
     */
    private static class MergeRegion {
        final int firstRow;
        final int lastRow;
        final int firstCol;
        final int lastCol;
        final String content;  // Content to write in merged cell
        
        MergeRegion(int firstRow, int lastRow, int firstCol, int lastCol, String content) {
            this.firstRow = firstRow;
            this.lastRow = lastRow;
            this.firstCol = firstCol;
            this.lastCol = lastCol;
            this.content = content;
        }
    }
    
    /**
     * Check if content contains a merge_row directive.
     */
    private static boolean hasMergeRowDirective(String content) {
        if (content == null) return false;
        return content.contains("{m:merge_row ");
    }
    
    /**
     * Check if content contains a merge_column directive.
     */
    private static boolean hasMergeColumnDirective(String content) {
        if (content == null) return false;
        return content.contains("{m:merge_column ");
    }
    
    /**
     * Parse merge_row directive from cell content.
     * Format: {m:merge_row variablename}
     * Returns null if not found or malformed.
     */
    private static String parseMergeRowDirective(String content) {
        if (content == null) return null;
        
        int startIdx = content.indexOf("{m:merge_row ");
        if (startIdx == -1) return null;
        
        int endIdx = content.indexOf("}", startIdx);
        if (endIdx == -1) return null;
        
        String directive = content.substring(startIdx + "{m:merge_row ".length(), endIdx).trim();
        return directive.isEmpty() ? null : directive;
    }
    
    /**
     * Parse merge_column directive from cell content.
     * Format: {m:merge_column variablename}
     * Returns null if not found or malformed.
     */
    private static String parseMergeColumnDirective(String content) {
        if (content == null) return null;
        
        int startIdx = content.indexOf("{m:merge_column ");
        if (startIdx == -1) return null;
        
        int endIdx = content.indexOf("}", startIdx);
        if (endIdx == -1) return null;
        
        String directive = content.substring(startIdx + "{m:merge_column ".length(), endIdx).trim();
        return directive.isEmpty() ? null : directive;
    }
    
    /**
     * Remove merge directives from cell content.
     * This removes {m:merge_row ...} and {m:merge_column ...} from the text.
     */
    private static String removeMergeDirectives(String content) {
        if (content == null) return null;
        
        String result = content;
        
        // Remove merge_row directives
        while (result.contains("{m:merge_row ")) {
            int startIdx = result.indexOf("{m:merge_row ");
            int endIdx = result.indexOf("}", startIdx);
            if (endIdx != -1) {
                result = result.substring(0, startIdx) + result.substring(endIdx + 1);
            } else {
                break;
            }
        }
        
        // Remove merge_column directives
        while (result.contains("{m:merge_column ")) {
            int startIdx = result.indexOf("{m:merge_column ");
            int endIdx = result.indexOf("}", startIdx);
            if (endIdx != -1) {
                result = result.substring(0, startIdx) + result.substring(endIdx + 1);
            } else {
                break;
            }
        }
        
        return result;
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
     * Gets the cell content as rich text with formatting information preserved.
     * For STRING cells in XSSF workbooks, this extracts the rich text formatting runs.
     * For other cell types, returns plain (unformatted) text.
     * 
     * @param cell The cell to read
     * @return RichTextContent with text and formatting runs, or null if cell is null
     */
    private static RichTextContent getRichTextContent(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        // Handle STRING cells with potential rich text formatting
        if (cell.getCellType() == CellType.STRING && cell instanceof XSSFCell) {
            XSSFCell xssfCell = (XSSFCell) cell;
            XSSFRichTextString richText = xssfCell.getRichStringCellValue();
            String text = richText.getString();
            
            if (text == null || text.isEmpty()) {
                return RichTextContent.plain("");
            }
            
            List<FormattingRun> runs = new ArrayList<>();
            int numFormattingRuns = richText.numFormattingRuns();
            
            // If no formatting runs, the entire text has the same formatting (cell's base font)
            if (numFormattingRuns == 0) {
                // Use cell's font as the formatting for the entire text
                XSSFFont cellFont = xssfCell.getCellStyle().getFont();
                runs.add(new FormattingRun(0, text.length(), cellFont));
            } else {
                // Process each formatting run
                for (int i = 0; i < numFormattingRuns; i++) {
                    int startIdx = richText.getIndexOfFormattingRun(i);
                    XSSFFont font = richText.getFontOfFormattingRun(i);
                    
                    // Determine the end index (start of next run, or end of text)
                    int endIdx;
                    if (i < numFormattingRuns - 1) {
                        endIdx = richText.getIndexOfFormattingRun(i + 1);
                    } else {
                        endIdx = text.length();
                    }
                    
                    // Add this formatting run
                    runs.add(new FormattingRun(startIdx, endIdx, font));
                }
            }
            
            return new RichTextContent(text, runs);
        }
        
        // For non-STRING cells or non-XSSF cells, convert to plain text
        String plainText = getCellContent(cell);
        return plainText != null ? RichTextContent.plain(plainText) : null;
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
     * Check if text contains within-cell elseif statement at given position.
     */
    private static boolean isElseIf(String text, int pos) {
        if (text == null || pos < 0 || pos >= text.length()) return false;
        return text.startsWith("{m:elseif ", pos);
    }
    
    /**
     * Check if text contains within-cell else statement at given position.
     */
    private static boolean isElse(String text, int pos) {
        if (text == null || pos < 0 || pos >= text.length()) return false;
        return text.startsWith("{m:else}", pos);
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
     * Represents a formatting run - a segment of text with specific font formatting.
     */
    private static class FormattingRun {
        final int startIndex;    // Character index where this formatting starts
        final int endIndex;      // Character index where this formatting ends (exclusive)
        final XSSFFont font;     // The font applied to this segment
        
        FormattingRun(int startIndex, int endIndex, XSSFFont font) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.font = font;
        }
        
        /**
         * Creates a copy with adjusted indices.
         */
        FormattingRun withAdjustedIndices(int startOffset, int endOffset) {
            return new FormattingRun(startIndex + startOffset, endIndex + endOffset, font);
        }
        
        /**
         * Creates a copy with new indices but same font.
         */
        FormattingRun withIndices(int newStart, int newEnd) {
            return new FormattingRun(newStart, newEnd, font);
        }
    }
    
    /**
     * Holds rich text content with formatting information.
     */
    private static class RichTextContent {
        final String text;
        final List<FormattingRun> formattingRuns;
        
        RichTextContent(String text, List<FormattingRun> formattingRuns) {
            this.text = text;
            this.formattingRuns = formattingRuns != null ? formattingRuns : new ArrayList<>();
        }
        
        /**
         * Creates plain text content (no formatting).
         */
        static RichTextContent plain(String text) {
            return new RichTextContent(text, null);
        }
        
        /**
         * Gets the font at a specific character position (uses formatting of 'm' in {m:...}).
         */
        XSSFFont getFontAt(int position) {
            for (FormattingRun run : formattingRuns) {
                if (position >= run.startIndex && position < run.endIndex) {
                    return run.font;
                }
            }
            return null; // No specific formatting
        }
        
        /**
         * Extracts a substring with its formatting.
         */
        RichTextContent substring(int start, int end) {
            String subText = text.substring(start, end);
            List<FormattingRun> subRuns = new ArrayList<>();
            
            for (FormattingRun run : formattingRuns) {
                // Check if this run overlaps with the substring range
                int runStart = Math.max(run.startIndex, start);
                int runEnd = Math.min(run.endIndex, end);
                
                if (runStart < runEnd) {
                    // Adjust to substring-relative indices
                    subRuns.add(new FormattingRun(runStart - start, runEnd - start, run.font));
                }
            }
            
            return new RichTextContent(subText, subRuns);
        }
        
        /**
         * Returns a substring from the given start to the end of the text.
         */
        RichTextContent substring(int start) {
            return substring(start, text.length());
        }
        
        /**
         * Concatenates two rich text contents.
         */
        RichTextContent append(RichTextContent other) {
            StringBuilder textBuilder = new StringBuilder(this.text);
            textBuilder.append(other.text);
            
            List<FormattingRun> combinedRuns = new ArrayList<>(this.formattingRuns);
            int offset = this.text.length();
            
            for (FormattingRun run : other.formattingRuns) {
                combinedRuns.add(run.withAdjustedIndices(offset, offset));
            }
            
            return new RichTextContent(textBuilder.toString(), combinedRuns);
        }
    }
    
    /**
     * Parse if or elseif statement syntax: {m:if condition} or {m:elseif condition}
     * Returns IfInfo with condition expression and position after closing brace.
     */
    private static IfInfo parseIf(String text, int startPos) {
        boolean isElseIf = text.startsWith("{m:elseif ", startPos);
        boolean isIf = text.startsWith("{m:if ", startPos);
        
        if (!isIf && !isElseIf) {
            return null;
        }
        
        String prefix = isElseIf ? "{m:elseif " : "{m:if ";
        
        // Find the closing }
        int closingBrace = text.indexOf('}', startPos + prefix.length());
        if (closingBrace == -1) {
            System.err.println("ERROR: Malformed " + prefix + "- no closing brace");
            return null;
        }
        
        // Extract condition (without prefix and closing })
        String condition = text.substring(startPos + prefix.length(), closingBrace).trim();
        
        return new IfInfo(condition, closingBrace + 1);
    }
    
    /**
     * Find matching {m:endif} for {m:if} at given position, handling nesting.
     * Also finds {m:elseif} and {m:else} at the same nesting level.
     * Returns a FindIfEndResult with the position and type found.
     */
    private static class FindIfEndResult {
        enum Type { ELSEIF, ELSE, ENDIF }
        final int position;
        final Type type;
        
        FindIfEndResult(int position, Type type) {
            this.position = position;
            this.type = type;
        }
    }
    
    private static FindIfEndResult findNextIfControl(String text, int ifStartPos) {
        int depth = 0;
        int pos = ifStartPos + "{m:if ".length();
        
        while (pos < text.length()) {
            if (isIfStart(text, pos)) {
                depth++;
                pos += "{m:if ".length();
            } else if (isElseIf(text, pos)) {
                if (depth == 0) {
                    return new FindIfEndResult(pos, FindIfEndResult.Type.ELSEIF);
                }
                pos += "{m:elseif ".length();
            } else if (isElse(text, pos)) {
                if (depth == 0) {
                    return new FindIfEndResult(pos, FindIfEndResult.Type.ELSE);
                }
                pos += "{m:else}".length();
            } else if (isIfEnd(text, pos)) {
                if (depth == 0) {
                    return new FindIfEndResult(pos, FindIfEndResult.Type.ENDIF);
                }
                depth--;
                pos += "{m:endif}".length();
            } else {
                pos++;
            }
        }
        
        System.err.println("ERROR: No matching {m:endif} for {m:if} at position " + ifStartPos);
        return null;
    }
    
    /**
     * Find matching {m:endif} for {m:if} at given position, handling nesting.
     * @deprecated Use findNextIfControl instead for better elseif/else support
     */
    @Deprecated
    private static int findMatchingEndIf(String text, int ifStartPos) {
        FindIfEndResult result = findNextIfControl(text, ifStartPos);
        return result != null && result.type == FindIfEndResult.Type.ENDIF ? result.position : -1;
    }
    
    /**
     * Process within-cell if statements in the text.
     * This handles {m:if condition} ... [{m:elseif condition}]* [{m:else}]? {m:endif} patterns within a single cell.
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
            
            // Process the complete if/elseif/else/endif structure
            int endPos = processCompleteIfStructure(text, ifStartPos, ifInfo, variables, queryEnvironment, result);
            if (endPos == -1) {
                // Error processing if structure, skip to end of if header
                pos = ifInfo.ifEndPos;
            } else {
                pos = endPos;
            }
        }
        
        return result.toString();
    }
    
    /**
     * Process within-cell if statements preserving rich text formatting.
     * This is the rich text version of processIfStatements that tracks formatting through transformations.
     */
    private static RichTextContent processIfStatementsRichText(RichTextContent richText, 
            Map<String, Object> variables, IQueryEnvironment queryEnvironment) {
        
        if (richText == null || richText.text == null) {
            return richText;
        }
        
        String text = richText.text;
        List<RichTextContent> parts = new ArrayList<>();
        int pos = 0;
        
        while (pos < text.length()) {
            // Look for next if statement
            int ifStartPos = text.indexOf("{m:if ", pos);
            
            if (ifStartPos == -1) {
                // No more if statements, append remaining text with formatting
                if (pos < text.length()) {
                    parts.add(richText.substring(pos, text.length()));
                }
                break;
            }
            
            // Append text before the if statement with formatting
            if (ifStartPos > pos) {
                parts.add(richText.substring(pos, ifStartPos));
            }
            
            // Parse if statement
            IfInfo ifInfo = parseIf(text, ifStartPos);
            if (ifInfo == null) {
                // Parse error, include the malformed part
                parts.add(richText.substring(ifStartPos, Math.min(ifStartPos + "{m:if ".length(), text.length())));
                pos = ifStartPos + "{m:if ".length();
                continue;
            }
            
            // Get the font at the 'm' position (for applying to replacement text)
            // Position of 'm' in "{m:if " is ifStartPos + 1
            XSSFFont expressionFont = richText.getFontAt(ifStartPos + 1);
            
            // Process the complete if/elseif/else/endif structure
            RichTextContent ifResult = processCompleteIfStructureRichText(richText, ifStartPos, ifInfo, 
                    variables, queryEnvironment, expressionFont);
            
            if (ifResult != null) {
                parts.add(ifResult);
                // Find endif to determine where to continue
                FindIfEndResult endifResult = findNextIfControlFinal(text, ifStartPos);
                if (endifResult != null) {
                    pos = endifResult.position + "{m:endif}".length();
                } else {
                    pos = ifInfo.ifEndPos; // Error case
                }
            } else {
                // Error processing, skip
                pos = ifInfo.ifEndPos;
            }
        }
        
        // Concatenate all parts
        if (parts.isEmpty()) {
            return RichTextContent.plain("");
        }
        
        RichTextContent result = parts.get(0);
        for (int i = 1; i < parts.size(); i++) {
            result = result.append(parts.get(i));
        }
        
        return result;
    }
    
    /**
     * Finds the final endif for an if structure (skipping all elseif/else).
     */
    private static FindIfEndResult findNextIfControlFinal(String text, int ifStartPos) {
        FindIfEndResult result = findNextIfControl(text, ifStartPos);
        while (result != null && result.type != FindIfEndResult.Type.ENDIF) {
            result = findNextIfControl(text, result.position);
        }
        return result;
    }
    
    /**
     * Process a complete if/elseif/else/endif structure with rich text formatting.
     * Returns the evaluated content with formatting preserved.
     */
    private static RichTextContent processCompleteIfStructureRichText(RichTextContent richText, 
            int ifStartPos, IfInfo ifInfo, Map<String, Object> variables, 
            IQueryEnvironment queryEnvironment, XSSFFont expressionFont) {
        
        String text = richText.text;
        boolean conditionMatched = false;
        int currentPos = ifInfo.ifEndPos;
        
        // Evaluate the main if condition
        boolean ifCondition = evaluateCondition(ifInfo.condition, variables, queryEnvironment);
        
        // Find the next control structure (elseif, else, or endif)
        FindIfEndResult controlResult = findNextIfControl(text, ifStartPos);
        if (controlResult == null) {
            return null;
        }
        
        // Extract the "then" body with formatting
        RichTextContent thenBody = richText.substring(ifInfo.ifEndPos, controlResult.position);
        RichTextContent result = null;
        
        if (ifCondition) {
            // If condition was true, process the then body
            result = processIfStatementsRichText(thenBody, variables, queryEnvironment);
            conditionMatched = true;
        }
        
        // Now process any elseif/else clauses
        currentPos = controlResult.position;
        FindIfEndResult.Type currentType = controlResult.type;
        
        while (currentType != FindIfEndResult.Type.ENDIF && !conditionMatched) {
            if (currentType == FindIfEndResult.Type.ELSEIF) {
                // Parse the elseif condition
                IfInfo elseIfInfo = parseIf(text, currentPos);
                if (elseIfInfo == null) {
                    return null;
                }
                
                // Find next control structure after this elseif
                FindIfEndResult nextControl = findNextIfControl(text, currentPos);
                if (nextControl == null) {
                    return null;
                }
                
                // Extract elseif body with formatting
                RichTextContent elseIfBody = richText.substring(elseIfInfo.ifEndPos, nextControl.position);
                
                // Evaluate condition
                boolean elseIfCondition = evaluateCondition(elseIfInfo.condition, variables, queryEnvironment);
                if (elseIfCondition) {
                    result = processIfStatementsRichText(elseIfBody, variables, queryEnvironment);
                    conditionMatched = true;
                }
                
                currentPos = nextControl.position;
                currentType = nextControl.type;
                
            } else if (currentType == FindIfEndResult.Type.ELSE) {
                // Skip past {m:else}
                int elseEndPos = currentPos + "{m:else}".length();
                
                // Find the endif
                FindIfEndResult endifResult = findNextIfControl(text, currentPos);
                if (endifResult == null || endifResult.type != FindIfEndResult.Type.ENDIF) {
                    return null;
                }
                
                // Extract else body with formatting
                RichTextContent elseBody = richText.substring(elseEndPos, endifResult.position);
                
                // Process else body
                result = processIfStatementsRichText(elseBody, variables, queryEnvironment);
                conditionMatched = true;
                
                break; // Endif will follow
            }
        }
        
        // If no condition matched, return empty
        if (result == null) {
            result = RichTextContent.plain("");
        }
        
        // Apply expression font if result is plain text (preserves 'm' character formatting rule)
        if (expressionFont != null && result.formattingRuns.isEmpty() && !result.text.isEmpty()) {
            List<FormattingRun> runs = new ArrayList<>();
            runs.add(new FormattingRun(0, result.text.length(), expressionFont));
            result = new RichTextContent(result.text, runs);
        }
        
        return result;
    }
    
    /**
     * Process a complete if/elseif/else/endif structure starting at ifStartPos.
     * Returns the position after the endif, or -1 on error.
     */
    private static int processCompleteIfStructure(String text, int ifStartPos, IfInfo ifInfo,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment, StringBuilder result) {
        
        boolean conditionMatched = false;
        int currentPos = ifInfo.ifEndPos;
        
        // Evaluate the main if condition
        boolean ifCondition = evaluateCondition(ifInfo.condition, variables, queryEnvironment);
        
        // Find the next control structure (elseif, else, or endif)
        FindIfEndResult controlResult = findNextIfControl(text, ifStartPos);
        if (controlResult == null) {
            return -1;
        }
        
        // Extract the "then" body (between {m:if ...} and next control)
        String thenBody = text.substring(ifInfo.ifEndPos, controlResult.position);
        
        if (ifCondition) {
            // If condition was true, process the then body
            result.append(processIfStatements(thenBody, variables, queryEnvironment));
            conditionMatched = true;
        }
        
        // Now process any elseif/else clauses
        currentPos = controlResult.position;
        FindIfEndResult.Type currentType = controlResult.type;
        
        while (currentType != FindIfEndResult.Type.ENDIF) {
            if (currentType == FindIfEndResult.Type.ELSEIF) {
                // Parse the elseif condition
                IfInfo elseIfInfo = parseIf(text, currentPos);
                if (elseIfInfo == null) {
                    System.err.println("ERROR: Failed to parse elseif at position " + currentPos);
                    return -1;
                }
                
                // Find next control structure after this elseif
                FindIfEndResult nextControl = findNextIfControl(text, currentPos);
                if (nextControl == null) {
                    return -1;
                }
                
                // Extract elseif body
                String elseIfBody = text.substring(elseIfInfo.ifEndPos, nextControl.position);
                
                // Only evaluate and process if no previous condition matched
                if (!conditionMatched) {
                    boolean elseIfCondition = evaluateCondition(elseIfInfo.condition, variables, queryEnvironment);
                    if (elseIfCondition) {
                        result.append(processIfStatements(elseIfBody, variables, queryEnvironment));
                        conditionMatched = true;
                    }
                }
                
                currentPos = nextControl.position;
                currentType = nextControl.type;
                
            } else if (currentType == FindIfEndResult.Type.ELSE) {
                // Skip past {m:else}
                int elseEndPos = currentPos + "{m:else}".length();
                
                // Find the endif
                FindIfEndResult endifResult = findNextIfControl(text, currentPos);
                if (endifResult == null || endifResult.type != FindIfEndResult.Type.ENDIF) {
                    System.err.println("ERROR: Expected endif after else at position " + currentPos);
                    return -1;
                }
                
                // Extract else body
                String elseBody = text.substring(elseEndPos, endifResult.position);
                
                // Only process if no previous condition matched
                if (!conditionMatched) {
                    result.append(processIfStatements(elseBody, variables, queryEnvironment));
                }
                
                currentPos = endifResult.position;
                currentType = FindIfEndResult.Type.ENDIF;
            }
        }
        
        // Skip past the {m:endif}
        return currentPos + "{m:endif}".length();
    }
    
    /**
     * Evaluate a condition expression and return boolean result.
     */
    private static boolean evaluateCondition(String condition, Map<String, Object> variables,
            IQueryEnvironment queryEnvironment) {
        
        AqlEvaluationResult aqlResult = evaluateAqlExpression(condition, variables, queryEnvironment);
        
        if (aqlResult.hasError()) {
            String diagnosticMsg = formatDiagnosticMessages(aqlResult.getDiagnostic());
            System.err.println("ERROR: Failed to evaluate if condition: " + condition);
            System.err.println("       " + diagnosticMsg);
            return false;
        }
        
        Object value = aqlResult.getResult();
        // Convert to boolean
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue() != 0;
        } else if (value instanceof String) {
            return !((String) value).isEmpty();
        } else if (value instanceof java.util.Collection) {
            return !((java.util.Collection<?>) value).isEmpty();
        } else {
            return (value != null);
        }
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
     * 
     * TODO: Add validation-only mode that checks template structure and variable references
     *       without generating output. This would enable pre-generation validation reporting.
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
                // Parse error - write error to output
                String errorMsg = "[ERROR] Malformed {m:for} statement. Syntax: {m:for var | collection}\n" +
                                  "        Note: For row-level loops use {m:for_row}, for column-level use {m:for_column}";
                result.append(text.substring(forStartPos, forStartPos + "{m:for ".length()));
                result.append("\n").append(errorMsg);
                pos = forStartPos + "{m:for ".length();
                continue;
            }
            
            // Find matching endfor
            int endForPos = findMatchingEndFor(text, forStartPos);
            if (endForPos == -1) {
                // No matching endfor - write error to output
                String errorMsg = "[ERROR] Missing {m:endfor} in the same cell";
                result.append(text.substring(forStartPos, forInfo.endPos));
                result.append("\n").append(errorMsg);
                pos = forInfo.endPos;
                continue;
            }
            
            // Extract body content (between {m:for ...} and {m:endfor})
            String bodyContent = text.substring(forInfo.endPos, endForPos);
            
            System.out.println("DEBUG: For loop body: " + bodyContent);
            
            // Evaluate collection expression
            AqlEvaluationResult aqlResult = evaluateAqlExpression(forInfo.collectionExpr, variables, queryEnvironment);
            if (aqlResult.hasError()) {
                String diagnosticMsg = formatDiagnosticMessages(aqlResult.getDiagnostic());
                // Write error to console AND output
                System.err.println("ERROR: Failed to evaluate collection expression: " + forInfo.collectionExpr);
                System.err.println("       " + diagnosticMsg);
                String errorMsg = "[ERROR] Failed to evaluate for loop collection: " + forInfo.collectionExpr + "\n" + diagnosticMsg +
                                  "\n        Note: Variables defined in for loops are only available within that loop's body." +
                                  "\n              For row iteration use {m:for_row}, for column iteration use {m:for_column}";
                result.append(errorMsg);
                pos = endForPos + "{m:endfor}".length();
                continue;
            }
            Object collectionObj = aqlResult.getResult();
            
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
                // Write error to console AND output
                System.err.println("ERROR: Collection expression did not evaluate to Iterable: " + collectionObj);
                String errorMsg = "[ERROR] For loop collection is not iterable: " + forInfo.collectionExpr + "\n" +
                                  "        Evaluated to: " + (collectionObj != null ? collectionObj.getClass().getSimpleName() : "null") + "\n" +
                                  "        Expected a collection or list.";
                result.append(errorMsg);
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
     * Process within-cell for loops in rich text content, preserving formatting.
     * This handles {m:for var | collection} ... {m:endfor} patterns within a single cell.
     * Supports nesting and sequential loops.
     * 
     * Formatting preservation rule: The formatting of the 'm' character in {m:for ...}
     * is applied to each iteration's result.
     * 
     * @param richText The rich text content with for loops
     * @param variables Variable context for evaluation
     * @param queryEnvironment AQL query environment
     * @return Rich text content with loops expanded and formatting preserved
     */
    private static RichTextContent processForLoopsRichText(RichTextContent richText, 
            Map<String, Object> variables, IQueryEnvironment queryEnvironment) {
        
        String text = richText.text;
        RichTextContent result = RichTextContent.plain("");
        int pos = 0;
        
        while (pos < text.length()) {
            // Look for next for loop
            int forStartPos = text.indexOf("{m:for ", pos);
            
            if (forStartPos == -1) {
                // No more for loops, process remaining text
                RichTextContent remaining = richText.substring(pos);
                RichTextContent processedRemaining = processNonLoopExpressionsRichText(
                    remaining, variables, queryEnvironment);
                result = result.append(processedRemaining);
                break;
            }
            
            // Append text before the for loop
            if (forStartPos > pos) {
                RichTextContent beforeLoop = richText.substring(pos, forStartPos);
                RichTextContent processedBefore = processNonLoopExpressionsRichText(
                    beforeLoop, variables, queryEnvironment);
                result = result.append(processedBefore);
            }
            
            // Get formatting of the 'm' character for this for loop
            // The 'm' is at position forStartPos + 1 (after the '{')
            XSSFFont expressionFont = richText.getFontAt(forStartPos + 1);
            
            // Parse for loop
            ForLoopInfo forInfo = parseForLoop(text, forStartPos);
            if (forInfo == null) {
                // Parse error - write error to output
                RichTextContent errorPart = richText.substring(
                    forStartPos, forStartPos + "{m:for ".length());
                String errorMsg = "\n[ERROR] Malformed {m:for} statement. Syntax: {m:for var | collection}\n" +
                                  "        Note: For row-level loops use {m:for_row}, for column-level use {m:for_column}";
                result = result.append(errorPart).append(new RichTextContent(errorMsg, new ArrayList<>()));
                pos = forStartPos + "{m:for ".length();
                continue;
            }
            
            // Find matching endfor
            int endForPos = findMatchingEndFor(text, forStartPos);
            if (endForPos == -1) {
                // No matching endfor - write error to output
                RichTextContent errorPart = richText.substring(forStartPos, forInfo.endPos);
                String errorMsg = "\n[ERROR] Missing {m:endfor} in the same cell";
                result = result.append(errorPart).append(new RichTextContent(errorMsg, new ArrayList<>()));
                pos = forInfo.endPos;
                continue;
            }
            
            // Extract body content (between {m:for ...} and {m:endfor})
            RichTextContent bodyContent = richText.substring(forInfo.endPos, endForPos);
            
            System.out.println("DEBUG: For loop body (rich text): " + bodyContent.text);
            
            // Evaluate collection expression
            AqlEvaluationResult aqlResult = evaluateAqlExpression(
                forInfo.collectionExpr, variables, queryEnvironment);
            if (aqlResult.hasError()) {
                String diagnosticMsg = formatDiagnosticMessages(aqlResult.getDiagnostic());
                // Write error to console AND output
                System.err.println("ERROR: Failed to evaluate collection expression: " + forInfo.collectionExpr);
                System.err.println("       " + diagnosticMsg);
                String errorMsg = "[ERROR] Failed to evaluate for loop collection: " + forInfo.collectionExpr + "\n" + diagnosticMsg +
                                  "\n        Note: Variables defined in for loops are only available within that loop's body." +
                                  "\n              For row iteration use {m:for_row}, for column iteration use {m:for_column}";
                result = result.append(new RichTextContent(errorMsg, new ArrayList<>()));
                pos = endForPos + "{m:endfor}".length();
                continue;
            }
            Object collectionObj = aqlResult.getResult();
            
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
                    
                    // Recursively process body (may contain nested loops or expressions)
                    RichTextContent iterationResult = processForLoopsRichText(
                        bodyContent, iterationVars, queryEnvironment);
                    
                    // If the result lost formatting (fell back to plain text), 
                    // and we have an expressionFont, apply it
                    if (iterationResult.formattingRuns.isEmpty() && expressionFont != null 
                            && !iterationResult.text.isEmpty()) {
                        FormattingRun run = new FormattingRun(0, iterationResult.text.length(), expressionFont);
                        iterationResult = new RichTextContent(iterationResult.text, 
                            java.util.Arrays.asList(run));
                    }
                    
                    result = result.append(iterationResult);
                }
                
                System.out.println("DEBUG: For loop complete, " + index + " iterations");
            } else {
                // Write error to console AND output
                System.err.println("ERROR: Collection expression did not evaluate to Iterable: " + collectionObj);
                String errorMsg = "[ERROR] For loop collection is not iterable: " + forInfo.collectionExpr + "\n" +
                                  "        Evaluated to: " + (collectionObj != null ? collectionObj.getClass().getSimpleName() : "null") + "\n" +
                                  "        Expected a collection or list.";
                result = result.append(new RichTextContent(errorMsg, new ArrayList<>()));
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
        
        return result;
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
            // Check for orphaned endfor (endfor without matching for)
            if (result.startsWith("{m:endfor}", startIdx)) {
                String errorMsg = "\n[ERROR] Missing {m:for} in the same cell";
                result = result.substring(0, startIdx) + result.substring(startIdx, startIdx + "{m:endfor}".length()) + 
                         errorMsg + result.substring(startIdx + "{m:endfor}".length());
                startIdx += "{m:endfor}".length() + errorMsg.length();
                continue;
            }
            
            // Skip if this is a for, endfor, if, endif, merge_row, or merge_column
            if (result.startsWith("{m:for ", startIdx) || 
                result.startsWith("{m:endfor", startIdx) ||
                result.startsWith("{m:if ", startIdx) ||
                result.startsWith("{m:endif", startIdx)) {
                startIdx += M_FIELD_START.length();
                continue;
            }
            
            // Remove merge directives (they should be invisible in output)
            if (result.startsWith("{m:merge_row ", startIdx) || result.startsWith("{m:merge_column ", startIdx)) {
                int closeBrace = result.indexOf('}', startIdx);
                if (closeBrace != -1) {
                    // Remove the entire directive
                    result = result.substring(0, startIdx) + result.substring(closeBrace + 1);
                    // Continue at the same position (since we removed content)
                    continue;
                } else {
                    startIdx += M_FIELD_START.length();
                    continue;
                }
            }
            
            int endIdx = result.indexOf(FIELD_END, startIdx);
            if (endIdx == -1) {
                break;
            }
            
            // Extract expression (without {m: and })
            String expression = result.substring(startIdx + M_FIELD_START.length(), endIdx).trim();
            
            // Evaluate using AQL
            Object value = null;
            String errorMessage = null;
            
            if (queryEnvironment != null) {
                AqlEvaluationResult aqlResult = evaluateAqlExpression(expression, variables, queryEnvironment);
                value = aqlResult.getResult();
                
                // Check for AQL diagnostics using M2Doc's pattern:
                // Check severity != OK to catch INFO, WARNING, and ERROR (not just errors)
                if (aqlResult.getDiagnostic() != null && aqlResult.getDiagnostic().getSeverity() != Diagnostic.OK) {
                    String diagnosticMsg = formatDiagnosticMessages(aqlResult.getDiagnostic());
                    if (!diagnosticMsg.isEmpty()) {
                        // Show ALL diagnostics (errors + warnings) so users can see issues
                        errorMessage = diagnosticMsg;
                    }
                }
                
                // Additional check: Warn if expression evaluates to null
                // This catches typos in property names (e.g., ref.datex instead of ref.date)
                // since AQL doesn't produce diagnostics for missing Map keys (M2Doc has same limitation)
                if (value == null && errorMessage == null) {
                    errorMessage = "[WARNING] Expression evaluated to null: May indicate a typo or missing field";
                }
            } else {
                // Fallback to simple evaluator if no query environment
                try {
                    value = evaluateSimpleExpression(expression, variables);
                } catch (Exception e) {
                    errorMessage = "[ERROR] " + e.getMessage();
                }
            }
            
            String replacement;
            if (errorMessage != null) {
                // Show original expression with error message
                replacement = result.substring(startIdx, endIdx + 1) + "\n" + errorMessage;
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
     * Process expressions in rich text content that are not for loops or if statements 
     * (regular {m:expr} patterns), preserving formatting.
     * This is separated to avoid infinite recursion.
     * 
     * Formatting preservation rule: The formatting of the 'm' character in {m:expr}
     * is applied to the replacement text.
     * 
     * @param richText The rich text content with expressions
     * @param variables Variable context for evaluation
     * @param queryEnvironment AQL query environment
     * @return Rich text content with expressions evaluated and formatting preserved
     */
    private static RichTextContent processNonLoopExpressionsRichText(RichTextContent richText,
            Map<String, Object> variables, IQueryEnvironment queryEnvironment) {
        
        String text = richText.text;
        RichTextContent result = RichTextContent.plain("");
        int pos = 0;
        
        // Find all {m:expression} patterns (but not {m:for, {m:endfor, {m:if, {m:endif, etc.})
        while (pos < text.length()) {
            int startIdx = text.indexOf(M_FIELD_START, pos);
            
            if (startIdx == -1) {
                // No more expressions, append remaining text
                result = result.append(richText.substring(pos));
                break;
            }
            
            // Skip if this is a for, endfor, if, endif, elseif, else, merge_row, or merge_column
            if (text.startsWith("{m:for ", startIdx) || 
                text.startsWith("{m:endfor", startIdx) ||
                text.startsWith("{m:if ", startIdx) ||
                text.startsWith("{m:endif", startIdx) ||
                text.startsWith("{m:elseif ", startIdx) ||
                text.startsWith("{m:else}", startIdx) ||
                text.startsWith("{m:merge_row ", startIdx) ||
                text.startsWith("{m:merge_column ", startIdx)) {
                // Find the closing brace for this directive
                int closeBrace = text.indexOf('}', startIdx);
                if (closeBrace != -1) {
                    // Skip this entire directive (merge directives should be removed, not displayed)
                    if (text.startsWith("{m:merge_row ", startIdx) || text.startsWith("{m:merge_column ", startIdx)) {
                        // Don't append merge directives - they should be invisible in output
                        pos = closeBrace + 1;
                    } else {
                        // Append control structures as-is and move past the opening tag
                        result = result.append(richText.substring(pos, startIdx + M_FIELD_START.length()));
                        pos = startIdx + M_FIELD_START.length();
                    }
                } else {
                    // No closing brace, skip the start tag
                    result = result.append(richText.substring(pos, startIdx + M_FIELD_START.length()));
                    pos = startIdx + M_FIELD_START.length();
                }
                continue;
            }
            
            int endIdx = text.indexOf(FIELD_END, startIdx);
            if (endIdx == -1) {
                // No closing brace, append remaining text
                result = result.append(richText.substring(pos));
                break;
            }
            
            // Append text before the expression
            if (startIdx > pos) {
                result = result.append(richText.substring(pos, startIdx));
            }
            
            // Get formatting of the 'm' character for this expression
            // The 'm' is at position startIdx + 1 (after the '{')
            XSSFFont expressionFont = richText.getFontAt(startIdx + 1);
            
            // Extract expression (without {m: and })
            String expression = text.substring(startIdx + M_FIELD_START.length(), endIdx).trim();
            
            // Evaluate using AQL
            Object value = null;
            String errorMessage = null;
            
            if (queryEnvironment != null) {
                AqlEvaluationResult aqlResult = evaluateAqlExpression(expression, variables, queryEnvironment);
                value = aqlResult.getResult();
                
                // Check for AQL diagnostics using M2Doc's pattern:
                // Check severity != OK to catch INFO, WARNING, and ERROR (not just errors)
                if (aqlResult.getDiagnostic() != null && aqlResult.getDiagnostic().getSeverity() != Diagnostic.OK) {
                    String diagnosticMsg = formatDiagnosticMessages(aqlResult.getDiagnostic());
                    if (!diagnosticMsg.isEmpty()) {
                        // Show ALL diagnostics (errors + warnings) so users can see issues
                        errorMessage = diagnosticMsg;
                    }
                }
                
                // Additional check: Warn if expression evaluates to null
                // This catches typos in property names (e.g., ref.datex instead of ref.date)
                // since AQL doesn't produce diagnostics for missing Map keys (M2Doc has same limitation)
                if (value == null && errorMessage == null) {
                    errorMessage = "[WARNING] Expression evaluated to null: May indicate a typo or missing field";
                }
            } else {
                // Fallback to simple evaluator if no query environment
                try {
                    value = evaluateSimpleExpression(expression, variables);
                } catch (Exception e) {
                    errorMessage = "[ERROR] " + e.getMessage();
                }
            }
            
            String replacementText;
            if (errorMessage != null) {
                // Show original expression with error message
                replacementText = text.substring(startIdx, endIdx + 1) + "\n" + errorMessage;
            } else {
                replacementText = value != null ? value.toString() : "";
            }
            
            // Create rich text content for the replacement
            // Apply the formatting from the 'm' character
            RichTextContent replacement;
            if (expressionFont != null && !replacementText.isEmpty()) {
                FormattingRun run = new FormattingRun(0, replacementText.length(), expressionFont);
                replacement = new RichTextContent(replacementText, java.util.Arrays.asList(run));
            } else {
                replacement = RichTextContent.plain(replacementText);
            }
            
            result = result.append(replacement);
            pos = endIdx + 1;
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
     * Evaluates AQL expressions in rich text content, preserving formatting.
     * Uses the user-defined rule: formatting of the 'm' character in {m:...} 
     * determines the formatting of the replacement text.
     * 
     * @param richText The rich text content with expressions
     * @param variables Variable context for evaluation
     * @param queryEnvironment AQL query environment
     * @return Evaluated rich text with formatting preserved according to the 'm' character rule
     */
    private static RichTextContent evaluateRichTextExpressions(RichTextContent richText, 
            Map<String, Object> variables, IQueryEnvironment queryEnvironment) {
        
        if (richText == null || richText.text == null) {
            return richText;
        }
        
        // Step 1: Process if statements with rich text formatting
        RichTextContent afterIfs = processIfStatementsRichText(richText, variables, queryEnvironment);
        
        // Step 2: Process for loops with rich text formatting
        RichTextContent afterLoops = processForLoopsRichText(afterIfs, variables, queryEnvironment);
        
        // The processForLoopsRichText already handles regular expressions via 
        // processNonLoopExpressionsRichText, so we can just return the result
        return afterLoops;
    }
    
    /**
     * Evaluates an AQL expression using the query environment.
     */
    /**
     * Simple wrapper for AQL evaluation results including diagnostics.
     */
    private static class AqlEvaluationResult {
        private final Object result;
        private final Diagnostic diagnostic;
        private final boolean hasError;
        
        public AqlEvaluationResult(Object result, Diagnostic diagnostic, boolean hasError) {
            this.result = result;
            this.diagnostic = diagnostic;
            this.hasError = hasError;
        }
        
        public Object getResult() {
            return result;
        }
        
        public Diagnostic getDiagnostic() {
            return diagnostic;
        }
        
        public boolean hasError() {
            return hasError;
        }
    }
    
    /**
     * Evaluates an AQL expression and returns result with diagnostics.
     * Does not throw exceptions - errors are captured in diagnostic.
     */
    private static AqlEvaluationResult evaluateAqlExpression(String expression, Map<String, Object> variables,
            IQueryEnvironment queryEnvironment) {
        // Create query builder engine
        IQueryBuilderEngine queryBuilder = new QueryBuilderEngine(queryEnvironment);
        
        // Parse the expression
        AstResult astResult = queryBuilder.build(expression);
        
        // Check for parse errors (M2Doc pattern: severity != OK)
        if (astResult.getDiagnostic().getSeverity() != Diagnostic.OK) {
            return new AqlEvaluationResult(null, astResult.getDiagnostic(), true);
        }
        
        // Create evaluation engine and evaluate
        IQueryEvaluationEngine evaluationEngine = new QueryEvaluationEngine(queryEnvironment);
        EvaluationResult evalResult = evaluationEngine.eval(astResult, variables);
        
        // Check severity of diagnostics
        boolean hasError = evalResult.getDiagnostic() != null && 
                          hasErrors(evalResult.getDiagnostic());
        
        return new AqlEvaluationResult(evalResult.getResult(), evalResult.getDiagnostic(), hasError);
    }
    
    /**
     * Formats diagnostic messages into a string for display in a cell.
     * Based on M2Doc's appendDiagnosticMessage pattern.
     */
    private static String formatDiagnosticMessages(Diagnostic diagnostic) {
        if (diagnostic == null || diagnostic.getChildren().isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Diagnostic child : diagnostic.getChildren()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            
            // Add severity prefix (M2Doc pattern)
            switch (child.getSeverity()) {
                case Diagnostic.ERROR:
                    sb.append("[ERROR] ");
                    break;
                case Diagnostic.WARNING:
                    sb.append("[WARNING] ");
                    break;
                case Diagnostic.INFO:
                    sb.append("[INFO] ");
                    break;
                default:
                    break;
            }
            
            sb.append(child.getMessage());
            
            // Recursively add child diagnostics
            if (!child.getChildren().isEmpty()) {
                String childMessages = formatDiagnosticMessages(child);
                if (!childMessages.isEmpty()) {
                    sb.append("\n").append(childMessages);
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Check if a diagnostic contains errors (not just warnings).
     */
    private static boolean hasErrors(Diagnostic diagnostic) {
        if (diagnostic.getSeverity() >= Diagnostic.ERROR) {
            return true;
        }
        for (Diagnostic child : diagnostic.getChildren()) {
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
