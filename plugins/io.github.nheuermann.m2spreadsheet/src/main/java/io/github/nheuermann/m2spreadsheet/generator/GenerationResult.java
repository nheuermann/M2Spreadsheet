package io.github.nheuermann.m2spreadsheet.generator;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a spreadsheet generation operation.
 * Contains the generated workbook and any errors or warnings encountered.
 * 
 * <p>Based on M2Doc's GenerationResult.</p>
 * 
 * @author nheuermann
 */
public class GenerationResult {
    
    /**
     * The list of generation errors.
     */
    private final List<Exception> generationErrors = new ArrayList<>();
    
    /**
     * The list of validation messages.
     */
    private final List<String> validationMessages = new ArrayList<>();
    
    /**
     * Gets the list of generation errors.
     * 
     * @return the generation errors
     */
    public List<Exception> getGenerationErrors() {
        return generationErrors;
    }
    
    /**
     * Gets the list of validation messages.
     * 
     * @return the validation messages
     */
    public List<String> getValidationMessages() {
        return validationMessages;
    }
    
    /**
     * Checks if the generation was successful (no errors).
     * 
     * @return true if successful, false otherwise
     */
    public boolean isSuccessful() {
        return generationErrors.isEmpty();
    }
}
