package io.github.nheuermann.m2spreadsheet.generator;

import java.util.ArrayList;
import java.util.List;

import io.github.nheuermann.m2spreadsheet.validation.TemplateValidationMessage;
import io.github.nheuermann.m2spreadsheet.validation.ValidationMessageLevel;

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
     * The list of validation messages from template validation.
     */
    private final List<TemplateValidationMessage> validationMessages = new ArrayList<>();
    
    /**
     * The highest validation level found.
     */
    private ValidationMessageLevel validationLevel = ValidationMessageLevel.OK;
    
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
    public List<TemplateValidationMessage> getValidationMessages() {
        return validationMessages;
    }
    
    /**
     * Gets the highest validation level.
     * 
     * @return the validation level
     */
    public ValidationMessageLevel getValidationLevel() {
        return validationLevel;
    }
    
    /**
     * Sets the validation level.
     * 
     * @param level the validation level
     */
    public void setValidationLevel(ValidationMessageLevel level) {
        this.validationLevel = level;
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
