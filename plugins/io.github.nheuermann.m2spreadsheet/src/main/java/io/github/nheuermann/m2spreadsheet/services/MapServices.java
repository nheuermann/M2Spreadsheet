package io.github.nheuermann.m2spreadsheet.services;

import java.util.Map;

/**
 * AQL services for Map/HashMap access in templates.
 * 
 * <p>This service enables AQL expressions to access Map properties directly,
 * allowing expressions like {@code customer.name} where customer is a Map.</p>
 * 
 * <p>Based on M2Doc service architecture.</p>
 * 
 * @author nheuermann
 */
public class MapServices {
    
    /**
     * Access a Map property by key.
     * This method is automatically called by AQL when accessing properties on Map objects.
     * 
     * @param map the Map to access
     * @param key the property key
     * @return the value associated with the key, or null if not found
     */
    public Object aqlFeatureAccess(Map<?, ?> map, String key) {
        return map.get(key);
    }
}
