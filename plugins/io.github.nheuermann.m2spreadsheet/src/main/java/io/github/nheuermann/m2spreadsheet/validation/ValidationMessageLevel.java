/*******************************************************************************
 *  Copyright (c) 2025 Nils Heuermann
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v2.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v20.html
 *   
 *   Contributors:
 *       Nils Heuermann - initial API and implementation
 *       (based on M2Doc's ValidationMessageLevel by Obeo)
 *  
 *******************************************************************************/
package io.github.nheuermann.m2spreadsheet.validation;

/**
 * {@link TemplateValidationMessage} level.
 * Represents the severity of validation messages found in templates.
 * 
 * @author nheuermann
 */
public enum ValidationMessageLevel {

    /**
     * No problem.
     */
    OK,

    /**
     * Used for simple information messages.
     */
    INFO,

    /**
     * Used to report potential errors (errors that can occur in certain cases 
     * but that do not always occur).
     */
    WARNING,

    /**
     * Used to report errors that will most probably occur.
     */
    ERROR;

    /**
     * Gets the highest {@link ValidationMessageLevel} between the given levels.
     * This is used to track the most severe validation issue found.
     * 
     * @param level1
     *            the first {@link ValidationMessageLevel}
     * @param levels
     *            additional {@link ValidationMessageLevel}s
     * @return the highest {@link ValidationMessageLevel} among all provided levels
     */
    public static ValidationMessageLevel updateLevel(ValidationMessageLevel level1, ValidationMessageLevel... levels) {
        ValidationMessageLevel res = level1;

        for (ValidationMessageLevel other : levels) {
            if (res != ValidationMessageLevel.ERROR) {
                switch (other) {
                    case ERROR:
                        res = ValidationMessageLevel.ERROR;
                        break;

                    case WARNING:
                        if (res != ValidationMessageLevel.ERROR) {
                            res = ValidationMessageLevel.WARNING;
                        }
                        break;

                    case INFO:
                        if (res == ValidationMessageLevel.OK) {
                            res = ValidationMessageLevel.INFO;
                        }
                        break;

                    default:
                        break;
                }
            } else {
                break;
            }
        }

        return res;
    }

}
