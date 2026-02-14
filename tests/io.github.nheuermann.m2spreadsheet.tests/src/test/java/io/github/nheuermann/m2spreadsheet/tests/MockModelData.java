package io.github.nheuermann.m2spreadsheet.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock model data for testing M2Spreadsheet with realistic automotive safety data.
 * 
 * <p>This class simulates EMF model data structure with:
 * - Variables section: Maps variable names to data objects
 * - Data section: Object definitions with properties
 * </p>
 * 
 * <p>Based on typical automotive safety engineering data structures.</p>
 */
public class MockModelData {
    
    // ============================================================================
    // VARIABLES SECTION - Top-level accessible variables for templates
    // ============================================================================
    
    /**
     * Get all variables available in the mock model.
     * This simulates the variables that would be passed to M2Spreadsheet generation.
     * 
     * @return Map of variable names to their values
     */
    public static Map<String, Object> getVariables() {
        Map<String, Object> variables = new HashMap<>();
        
        // Scalar variables
        variables.put("projectName", "Electronic Steering Lock System");
        variables.put("projectCode", "ESL-2026-01");
        variables.put("safetyStandard", "ISO 26262");
        variables.put("asil", "ASIL-D");
        variables.put("generationDate", "2026-02-12");
        
        // Collection variables
        variables.put("requirements", getRequirements());
        variables.put("faultTreeEvents", getFaultTreeBaseEvents());
        variables.put("referenceDocuments", getReferenceDocuments());
        variables.put("fmeaEntries", getFmeaEntries());
        variables.put("structuredFmea", getStructuredFmea());
        
        return variables;
    }
    
    // ============================================================================
    // DATA SECTION - Object Definitions
    // ============================================================================
    
    /**
     * Get 15 sample requirements with varying Development Assurance Levels.
     */
    public static List<Map<String, Object>> getRequirements() {
        List<Map<String, Object>> requirements = new ArrayList<>();
        
        requirements.add(createRequirement("REQ-ESL-001", "System Power Supply",
            "The ESL shall be supplied with 12V DC from the vehicle battery with reverse polarity protection.",
            "DAL-A", Arrays.asList("BE-004", "BE-019", "BE-020", "BE-023", "BE-032", "BE-035", "BE-018", "BE-027", "BE-021", "BE-010", "BE-031", "BE-022")));
        
        requirements.add(createRequirement("REQ-ESL-002", "Locking Force",
            "The ESL shall provide a minimum locking force of 3000 N at the steering column.",
            "DAL-A", Arrays.asList("BE-001", "BE-014", "BE-015", "BE-016", "BE-017", "BE-024", "BE-009", "BE-029")));
        
        requirements.add(createRequirement("REQ-ESL-003", "Lock Engagement Time",
            "The ESL shall engage the lock within 200 ms from receiving the lock command.",
            "DAL-B", Arrays.asList("BE-001", "BE-008", "BE-011", "BE-012", "BE-013", "BE-017", "BE-023", "BE-026", "BE-030", "BE-033")));
        
        requirements.add(createRequirement("REQ-ESL-004", "CAN Communication",
            "The ESL ECU shall communicate via CAN bus at 500 kbit/s with message timeout detection.",
            "DAL-A", Arrays.asList("BE-005", "BE-021", "BE-006", "BE-022", "BE-026", "BE-030", "BE-034", "BE-004", "BE-018", "BE-025", "BE-027")));
        
        requirements.add(createRequirement("REQ-ESL-005", "Redundant Sensors",
            "The ESL shall incorporate dual Hall sensors for position detection with cross-checking.",
            "DAL-A", Arrays.asList("BE-002", "BE-003", "BE-009", "BE-018", "BE-025", "BE-027", "BE-031", "BE-032", "BE-004", "BE-010", "BE-022", "BE-023", "BE-033", "BE-006")));
        
        requirements.add(createRequirement("REQ-ESL-006", "Diagnostic Capability",
            "The ESL shall support UDS diagnostic protocol for fault memory and actuator testing.",
            "DAL-C", Arrays.asList("BE-005", "BE-006", "BE-030", "BE-034", "BE-013", "BE-026", "BE-011")));
        
        requirements.add(createRequirement("REQ-ESL-007", "Emergency Unlock",
            "The ESL shall provide mechanical emergency unlock capability accessible from vehicle exterior.",
            "DAL-B", Arrays.asList("BE-014", "BE-015", "BE-016", "BE-024", "BE-033", "BE-017", "BE-001", "BE-029", "BE-009")));
        
        requirements.add(createRequirement("REQ-ESL-008", "Temperature Range",
            "The ESL shall operate within temperature range -40°C to +85°C ambient.",
            "DAL-B", Arrays.asList("BE-019", "BE-023", "BE-029", "BE-033", "BE-020", "BE-018", "BE-027", "BE-002", "BE-003", "BE-031")));
        
        requirements.add(createRequirement("REQ-ESL-009", "Vibration Resistance",
            "The ESL shall withstand vibration per ISO 16750-3 without degradation.",
            "DAL-C", Arrays.asList("BE-009", "BE-016", "BE-017", "BE-024", "BE-025", "BE-027", "BE-010", "BE-018", "BE-032", "BE-033", "BE-015")));
        
        requirements.add(createRequirement("REQ-ESL-010", "EMC Compliance",
            "The ESL shall meet EMC requirements per ISO 11452-2 for immunity to radiated electromagnetic fields.",
            "DAL-C", Arrays.asList("BE-022", "BE-004", "BE-006", "BE-026", "BE-031", "BE-021", "BE-005", "BE-030")));
        
        requirements.add(createRequirement("REQ-ESL-011", "Fault Detection Time",
            "The ESL shall detect sensor faults within 100 ms and enter safe state.",
            "DAL-A", Arrays.asList("BE-002", "BE-003", "BE-006", "BE-008", "BE-011", "BE-013", "BE-026", "BE-030", "BE-034", "BE-031", "BE-032", "BE-023", "BE-001")));
        
        requirements.add(createRequirement("REQ-ESL-012", "Motor Current Monitoring",
            "The ESL shall monitor motor current and detect overcurrent conditions exceeding 15A.",
            "DAL-B", Arrays.asList("BE-001", "BE-008", "BE-010", "BE-017", "BE-020", "BE-023", "BE-028", "BE-032", "BE-019", "BE-027")));
        
        requirements.add(createRequirement("REQ-ESL-013", "Locked State Verification",
            "The ESL shall verify locked state through dual sensor confirmation before signaling lock complete.",
            "DAL-A", Arrays.asList("BE-002", "BE-003", "BE-009", "BE-006", "BE-011", "BE-013", "BE-026", "BE-030", "BE-034", "BE-014", "BE-015", "BE-024")));
        
        requirements.add(createRequirement("REQ-ESL-014", "Watchdog Timer",
            "The ESL ECU shall implement internal and external watchdog with maximum 100ms timeout.",
            "DAL-A", Arrays.asList("BE-011", "BE-006", "BE-013", "BE-023", "BE-026", "BE-030", "BE-012", "BE-004", "BE-020", "BE-035", "BE-008")));
        
        requirements.add(createRequirement("REQ-ESL-015", "Ignition Interlock",
            "The ESL shall prevent ignition start when lock is not in fully engaged position.",
            "DAL-A", Arrays.asList("BE-002", "BE-003", "BE-009", "BE-014", "BE-015", "BE-024", "BE-006", "BE-026", "BE-013", "BE-034")));
        
        return requirements;
    }
    
    /**
     * Get 35 fault tree base events with varying failure rates and models.
     * About 1/3 have latency values.
     */
    public static List<Map<String, Object>> getFaultTreeBaseEvents() {
        List<Map<String, Object>> events = new ArrayList<>();
        
        // Events with latency (first 12 = ~1/3 of 35)
        events.add(createBaseEvent("BE-001", "Motor Winding Open Circuit",
            "Permanent", "1.5E-7", "0.5"));
        
        events.add(createBaseEvent("BE-002", "Hall Sensor A Failure",
            "Transient", "3.2E-6", "1.0"));
        
        events.add(createBaseEvent("BE-003", "Hall Sensor B Failure",
            "Transient", "3.2E-6", "1.0"));
        
        events.add(createBaseEvent("BE-004", "ECU Power Supply Undervoltage",
            "Transient", "5.0E-5", "0.1"));
        
        events.add(createBaseEvent("BE-005", "CAN Transceiver Failure",
            "Permanent", "2.1E-7", "2.0"));
        
        events.add(createBaseEvent("BE-006", "Microcontroller RAM Error",
            "Transient", "1.2E-6", "0.05"));
        
        events.add(createBaseEvent("BE-007", "EEPROM Data Corruption",
            "Permanent", "8.5E-8", "5.0"));
        
        events.add(createBaseEvent("BE-008", "Motor Driver MOSFET Short",
            "Permanent", "4.3E-7", "0.2"));
        
        events.add(createBaseEvent("BE-009", "Position Sensor Mechanical Wear",
            "Permanent", "2.7E-7", "10.0"));
        
        events.add(createBaseEvent("BE-010", "Wiring Harness Short Circuit",
            "Permanent", "6.8E-8", "0.3"));
        
        events.add(createBaseEvent("BE-011", "Watchdog Timer Malfunction",
            "Permanent", "1.9E-8", "0.1"));
        
        events.add(createBaseEvent("BE-012", "Clock Oscillator Drift",
            "Permanent", "3.5E-8", "1.5"));
        
        // Events without latency (remaining 23)
        events.add(createBaseEvent("BE-013", "Software Stack Overflow",
            "Transient", "2.4E-7", null));
        
        events.add(createBaseEvent("BE-014", "Locking Mechanism Jam",
            "Permanent", "1.8E-6", null));
        
        events.add(createBaseEvent("BE-015", "Spring Return Failure",
            "Permanent", "5.2E-7", null));
        
        events.add(createBaseEvent("BE-016", "Gear Train Tooth Breakage",
            "Permanent", "3.1E-7", null));
        
        events.add(createBaseEvent("BE-017", "Motor Bearing Seizure",
            "Permanent", "4.6E-7", null));
        
        events.add(createBaseEvent("BE-018", "ECU Connector Corrosion",
            "Permanent", "2.9E-6", null));
        
        events.add(createBaseEvent("BE-019", "Capacitor Aging Failure",
            "Permanent", "1.3E-7", null));
        
        events.add(createBaseEvent("BE-020", "Voltage Regulator Failure",
            "Permanent", "8.7E-8", null));
        
        events.add(createBaseEvent("BE-021", "CAN Bus Off State",
            "Transient", "4.5E-5", null));
        
        events.add(createBaseEvent("BE-022", "EMI Induced Bit Flip",
            "Transient", "7.2E-6", null));
        
        events.add(createBaseEvent("BE-023", "Thermal Shutdown",
            "Transient", "3.8E-6", null));
        
        events.add(createBaseEvent("BE-024", "Lock Bolt Deformation",
            "Permanent", "2.3E-7", null));
        
        events.add(createBaseEvent("BE-025", "PCB Trace Fracture",
            "Permanent", "1.1E-7", null));
        
        events.add(createBaseEvent("BE-026", "Flash Memory Bit Error",
            "Transient", "5.6E-7", null));
        
        events.add(createBaseEvent("BE-027", "Solder Joint Failure",
            "Permanent", "9.8E-8", null));
        
        events.add(createBaseEvent("BE-028", "Relay Contact Welding",
            "Permanent", "6.4E-7", null));
        
        events.add(createBaseEvent("BE-029", "Magnet Demagnetization",
            "Permanent", "1.5E-8", null));
        
        events.add(createBaseEvent("BE-030", "Firmware Checksum Error",
            "Transient", "3.3E-7", null));
        
        events.add(createBaseEvent("BE-031", "A/D Converter Saturation",
            "Transient", "4.1E-6", null));
        
        events.add(createBaseEvent("BE-032", "Ground Connection Loss",
            "Permanent", "2.8E-7", null));
        
        events.add(createBaseEvent("BE-033", "Housing Seal Degradation",
            "Permanent", "5.9E-7", null));
        
        events.add(createBaseEvent("BE-034", "Message Timeout Detection Failure",
            "Permanent", "1.7E-7", null));
        
        events.add(createBaseEvent("BE-035", "Backup Power Supply Depletion",
            "Permanent", "8.2E-7", null));
        
        return events;
    }
    
    /**
     * Get 20 reference documents typical for automotive safety projects.
     */
    public static List<Map<String, Object>> getReferenceDocuments() {
        List<Map<String, Object>> documents = new ArrayList<>();
        
        documents.add(createDocument("DOC-001", "ISO 26262-1:2018",
            "Road vehicles - Functional safety - Part 1: Vocabulary",
            "ISO", "2018-12-01"));
        
        documents.add(createDocument("DOC-002", "ISO 26262-3:2018",
            "Road vehicles - Functional safety - Part 3: Concept phase",
            "ISO", "2018-12-01"));
        
        documents.add(createDocument("DOC-003", "ISO 26262-4:2018",
            "Road vehicles - Functional safety - Part 4: Product development at the system level",
            "ISO", "2018-12-01"));
        
        documents.add(createDocument("DOC-004", "ISO 26262-6:2018",
            "Road vehicles - Functional safety - Part 6: Product development at the software level",
            "ISO", "2018-12-01"));
        
        documents.add(createDocument("DOC-005", "ESL System Requirements Specification",
            "Complete system requirements specification for Electronic Steering Lock including functional and safety requirements",
            "Engineering Team", "2025-08-15"));
        
        documents.add(createDocument("DOC-006", "ESL Hardware Architecture",
            "Hardware design specification including electrical schematics, component selection, and PCB layout",
            "Hardware Team", "2025-09-20"));
        
        documents.add(createDocument("DOC-007", "ESL Software Architecture",
            "Software architecture document covering module structure, interfaces, and data flow",
            "Software Team", "2025-10-05"));
        
        documents.add(createDocument("DOC-008", "Hazard Analysis and Risk Assessment",
            "HARA document identifying operational situations, hazards, and ASIL classification",
            "Safety Team", "2025-07-10"));
        
        documents.add(createDocument("DOC-009", "Functional Safety Concept",
            "Safety concept defining safety goals and functional safety requirements",
            "Safety Team", "2025-08-01"));
        
        documents.add(createDocument("DOC-010", "Technical Safety Concept",
            "Technical safety requirements derived from functional safety concept with architectural measures",
            "System Team", "2025-09-15"));
        
        documents.add(createDocument("DOC-011", "Fault Tree Analysis Report",
            "FTA covering top-level hazardous events with quantitative analysis",
            "Safety Team", "2025-11-20"));
        
        documents.add(createDocument("DOC-012", "FMEA Worksheet",
            "Failure Mode and Effects Analysis for ESL system components and functions",
            "Quality Team", "2025-10-30"));
        
        documents.add(createDocument("DOC-013", "Safety Validation Plan",
            "Validation strategy for demonstrating achievement of safety goals",
            "Validation Team", "2025-12-01"));
        
        documents.add(createDocument("DOC-014", "Component Supplier FMEDA",
            "Microcontroller FMEDA from semiconductor supplier Infineon",
            "Infineon Technologies", "2024-03-15"));
        
        documents.add(createDocument("DOC-015", "Motor Supplier Quality Manual",
            "Quality assurance documentation for DC motor from supplier Bühler Motor",
            "Bühler Motor", "2024-06-10"));
        
        documents.add(createDocument("DOC-016", "EMC Test Report",
            "Electromagnetic compatibility test results per ISO 11452-2 and ISO 11451-2",
            "Test Lab TÜV", "2026-01-15"));
        
        documents.add(createDocument("DOC-017", "Environmental Test Report",
            "Temperature, vibration, and humidity testing per ISO 16750 series",
            "Test Lab TÜV", "2026-01-20"));
        
        documents.add(createDocument("DOC-018", "Software Unit Test Report",
            "Unit test coverage and results for all safety-relevant software modules",
            "Software Team", "2025-11-10"));
        
        documents.add(createDocument("DOC-019", "Hardware-Software Integration Report",
            "HSI test results demonstrating correct interaction between HW and SW",
            "Integration Team", "2025-12-15"));
        
        documents.add(createDocument("DOC-020", "Functional Safety Assessment",
            "Independent assessment of safety lifecycle activities per ISO 26262-2",
            "TÜV SÜD Assessor", "2026-02-01"));
        
        return documents;
    }
    
    /**
     * Get 100 FMEA entries for Electronic Steering Lock with typical 24 columns.
     * Covers hardware, software, mechanical, and electrical failure modes.
     */
    public static List<Map<String, Object>> getFmeaEntries() {
        List<Map<String, Object>> fmeaEntries = new ArrayList<>();
        
        // Component: Microcontroller (Entries 1-15)
        fmeaEntries.add(createFmeaEntry("FMEA-001", "Microcontroller", "Execute control logic",
            "CPU core failure", "No command processing", "Lock command not executed", "Vehicle not secured",
            "10", "Manufacturing defect, ESD damage", "3", "Power-on self-test, watchdog", "7", "210",
            "ASIL-D", "Dual-core lockstep", "99%", "Improve ESD protection", "HW Team", "2026-03-15",
            "In Progress", "Added ESD diodes", "10", "2", "5", "100"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-002", "Microcontroller", "Execute control logic",
            "RAM bit flip", "Corrupted variable data", "Incorrect state machine behavior", "Unintended lock/unlock",
            "9", "Cosmic radiation, EMI", "4", "Parity checking, ECC", "5", "180",
            "ASIL-D", "ECC RAM", "95%", "None - adequate coverage", "SW Team", "2026-02-01",
            "Closed", "Verified ECC implementation", "9", "4", "3", "108"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-003", "Microcontroller", "Execute control logic",
            "Flash memory corruption", "Program code altered", "System malfunction", "Unpredictable behavior",
            "10", "Write cycle fatigue, voltage spike", "2", "CRC check at startup", "4", "80",
            "ASIL-D", "CRC verification", "90%", "Add runtime CRC", "SW Team", "2026-04-01",
            "Open", "", "10", "2", "3", "60"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-004", "Microcontroller", "Monitor system health",
            "Watchdog timer disabled", "No reset on software hang", "System stuck in fault state", "Lock inoperative",
            "8", "Software error, register corruption", "3", "External watchdog backup", "4", "96",
            "ASIL-D", "Independent external WD", "85%", "Add question-answer WD", "SW Team", "2026-05-01",
            "Open", "", "8", "2", "3", "48"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-005", "Microcontroller", "Process CAN messages",
            "CAN controller failure", "No message reception", "Command timeout", "Lock state unknown",
            "7", "Controller hardware fault", "2", "Message timeout detection", "3", "42",
            "ASIL-D", "Timeout monitoring", "80%", "None - acceptable", "HW Team", "2026-02-01",
            "Closed", "Verified timeout logic", "7", "2", "2", "28"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-006", "Microcontroller", "Read sensor inputs",
            "ADC saturation", "Incorrect current measurement", "Overcurrent not detected", "Motor damage possible",
            "6", "ADC reference voltage error", "3", "Plausibility check", "5", "90",
            "ASIL-C", "Redundant measurement", "75%", "Add voltage monitoring", "HW Team", "2026-04-15",
            "In Progress", "Schematic updated", "6", "2", "4", "48"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-007", "Microcontroller", "Generate PWM signals",
            "PWM timer malfunction", "No motor drive", "Lock mechanism not actuated", "Vehicle not secured",
            "9", "Timer peripheral defect", "2", "Motor current monitoring", "4", "72",
            "ASIL-D", "Current supervision", "85%", "None - acceptable", "HW Team", "2026-02-01",
            "Closed", "Design review passed", "9", "2", "3", "54"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-008", "Microcontroller", "Store calibration data",
            "EEPROM write failure", "Calibration data lost", "Default values used", "Suboptimal performance",
            "4", "Write cycle limit exceeded", "3", "Write verification", "3", "36",
            "ASIL-B", "Write-back verification", "90%", "None - acceptable", "SW Team", "2026-02-01",
            "Closed", "Verified implementation", "4", "3", "2", "24"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-009", "Microcontroller", "Execute diagnostic routines",
            "Diagnostic disable", "Faults not detected", "Latent faults accumulate", "Loss of safety margin",
            "8", "Software configuration error", "2", "Diagnostic counter monitoring", "5", "80",
            "ASIL-D", "Periodic test pattern", "80%", "Add diagnostic alive counter", "SW Team", "2026-06-01",
            "Open", "", "8", "1", "3", "24"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-010", "Microcontroller", "Manage power states",
            "Stuck in sleep mode", "No wake-up on CAN", "System unresponsive", "Lock commands ignored",
            "8", "Clock configuration error", "2", "Wake-up timeout detection", "4", "64",
            "ASIL-D", "External reset circuit", "85%", "None - acceptable", "SW Team", "2026-02-01",
            "Closed", "Verified with testing", "8", "2", "3", "48"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-011", "Microcontroller", "Execute safety checks",
            "Stack overflow", "Data corruption", "Control logic compromised", "Undefined behavior",
            "9", "Excessive recursion, ISR nesting", "3", "Stack canary monitoring", "4", "108",
            "ASIL-D", "Stack monitoring", "85%", "Optimize stack usage", "SW Team", "2026-03-30",
            "In Progress", "Static analysis done", "9", "2", "3", "54"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-012", "Microcontroller", "Decode CAN messages",
            "Buffer overflow", "Memory corruption", "System crash", "Lock inoperative",
            "10", "Invalid message length", "2", "Length validation", "3", "60",
            "ASIL-D", "Input validation", "95%", "None - adequate", "SW Team", "2026-02-01",
            "Closed", "Code review passed", "10", "2", "2", "40"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-013", "Microcontroller", "Control motor driver",
            "GPIO stuck at high", "Motor continuously powered", "Overheating", "Motor failure",
            "7", "GPIO peripheral defect", "2", "Current monitoring", "4", "56",
            "ASIL-C", "Current limiting", "80%", "None - acceptable", "HW Team", "2026-02-01",
            "Closed", "Testing confirmed", "7", "2", "3", "42"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-014", "Microcontroller", "Provide clock signal",
            "Oscillator frequency drift", "Timing errors", "CAN communication errors", "Timeout events",
            "5", "Component aging, temperature", "4", "CAN error frame detection", "5", "100",
            "ASIL-C", "Error counter monitoring", "70%", "Use more stable oscillator", "HW Team", "2026-05-15",
            "Open", "", "5", "3", "4", "60"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-015", "Microcontroller", "Reset handling",
            "Reset loop", "Continuous resets", "System never operational", "Lock unavailable",
            "9", "Configuration error, HW defect", "2", "Reset cause logging", "5", "90",
            "ASIL-D", "Reset cause analysis", "75%", "Add reset counter limit", "SW Team", "2026-04-01",
            "Open", "", "9", "1", "3", "27"));
        
        // Component: DC Motor (Entries 16-30)
        fmeaEntries.add(createFmeaEntry("FMEA-016", "DC Motor", "Provide mechanical actuation",
            "Winding open circuit", "No torque generation", "Lock not actuated", "Vehicle not secured",
            "10", "Wire breakage, manufacturing defect", "2", "Current monitoring", "3", "60",
            "ASIL-D", "Current supervision", "90%", "None - acceptable", "HW Team", "2026-02-01",
            "Closed", "Verified detection logic", "10", "2", "2", "40"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-017", "DC Motor", "Provide mechanical actuation",
            "Winding short circuit", "Overcurrent", "Fuse blown, no operation", "Lock inoperative",
            "9", "Insulation failure", "2", "Current limiting, fuse", "3", "54",
            "ASIL-D", "Electronic fuse", "95%", "None - adequate", "HW Team", "2026-02-01",
            "Closed", "FMEA review approved", "9", "2", "2", "36"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-018", "DC Motor", "Generate rotation",
            "Bearing seizure", "Motor jammed", "No actuation possible", "Lock fails to engage",
            "9", "Lubrication failure, contamination", "3", "Current rise detection", "4", "108",
            "ASIL-D", "Blocked rotor detection", "85%", "Improve bearing selection", "Mechanical Team", "2026-06-01",
            "Open", "", "9", "2", "3", "54"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-019", "DC Motor", "Generate rotation",
            "Brush wear excessive", "High resistance", "Reduced torque", "Slow or incomplete locking",
            "6", "Operating hours, contamination", "5", "Position timeout", "5", "150",
            "ASIL-C", "Timeout monitoring", "70%", "Specify brush lifetime", "Mechanical Team", "2026-04-15",
            "In Progress", "Durability testing ongoing", "6", "4", "4", "96"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-020", "DC Motor", "Provide mechanical actuation",
            "Magnet demagnetization", "Reduced magnetic field", "Lower torque output", "Intermittent locking",
            "7", "Temperature exposure, aging", "3", "Torque monitoring", "6", "126",
            "ASIL-C", "Indirect torque check", "60%", "Improve magnet grade", "HW Team", "2026-05-01",
            "Open", "", "7", "2", "5", "70"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-021", "DC Motor", "Convert electrical to mechanical energy",
            "Commutator segmentshort", "Irregular rotation", "Vibration, noise", "Premature wear",
            "5", "Manufacturing defect", "2", "Vibration sensor", "7", "70",
            "ASIL-B", "None", "40%", "Add vibration detection", "Mechanical Team", "2026-07-01",
            "Open", "", "5", "2", "5", "50"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-022", "DC Motor", "Provide mechanical power",
            "Shaft misalignment", "Increased friction", "Higher current draw", "Thermal stress",
            "6", "Assembly tolerances", "3", "Current monitoring", "5", "90",
            "ASIL-C", "Current limit", "75%", "Tighten assembly tolerances", "Mechanical Team", "2026-05-15",
            "In Progress", "Drawing revision in progress", "6", "2", "4", "48"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-023", "DC Motor", "Rotate continuously",
            "Thermal overload", "Motor overheating", "Thermal shutdown", "Lock actuation aborted",
            "7", "Excessive duty cycle, blocked rotor", "4", "Temperature sensor", "3", "84",
            "ASIL-C", "Thermal monitoring", "90%", "None - adequate", "HW Team", "2026-02-01",
            "Closed", "Verified in testing", "7", "4", "2", "56"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-024", "DC Motor", "Generate torque",
            "Rotor imbalance", "Vibration", "Accelerated bearing wear", "Premature failure",
            "5", "Manufacturing variation", "3", "None", "8", "120",
            "ASIL-B", "None", "30%", "Implement vibration monitoring", "Quality Team", "2026-08-01",
            "Open", "", "5", "3", "6", "90"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-025", "DC Motor", "Provide mechanical actuation",
            "Housing crack", "Moisture ingress", "Corrosion, short circuit", "Motor failure",
            "8", "Mechanical stress, impact", "2", "Visual inspection", "7", "112",
            "ASIL-C", "IP rating verification", "50%", "Improve housing design", "Mechanical Team", "2026-06-15",
            "Open", "", "8", "1", "5", "40"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-026", "DC Motor", "Convert power",
            "Efficiency degradation", "Increased power consumption", "Battery drain", "Shorter vehicle battery life",
            "3", "Wear, contamination", "5", "Current monitoring", "6", "90",
            "ASIL-A", "None", "60%", "None - not safety relevant", "HW Team", "2026-02-01",
            "Closed", "Accepted as low risk", "3", "5", "6", "90"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-027", "DC Motor", "Provide actuation",
            "Connection tab break", "Open circuit", "No motor function", "Lock inoperative",
            "10", "Vibration fatigue", "2", "Current detection", "3", "60",
            "ASIL-D", "Current monitoring", "90%", "Reinforce connection", "Mechanical Team", "2026-04-30",
            "In Progress", "Design change initiated", "10", "1", "2", "20"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-028", "DC Motor", "Generate rotation",
            "Reverse polarity damage", "Motor destroyed", "No function", "Lock unavailable",
            "10", "Incorrect installation", "1", "Polarity protection diode", "2", "20",
            "ASIL-D", "Hardware protection", "98%", "None - adequate", "HW Team", "2026-02-01",
            "Closed", "Protection verified", "10", "1", "1", "10"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-029", "DC Motor", "Provide torque",
            "Gear tooth wear", "Backlash increase", "Position uncertainty", "Lock verification issues",
            "6", "Operating cycles, contamination", "4", "Position sensor redundancy", "4", "96",
            "ASIL-C", "Dual sensors", "85%", "Improve gear material", "Mechanical Team", "2026-05-30",
            "Open", "", "6", "3", "3", "54"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-030", "DC Motor", "Actuate mechanism",
            "Cable chafing", "Intermittent connection", "Erratic operation", "Unpredictable behavior",
            "8", "Cable routing, vibration", "3", "Connection monitoring", "5", "120",
            "ASIL-D", "Continuous monitoring", "75%", "Improve cable routing", "HW Team", "2026-04-15",
            "In Progress", "Layout revision underway", "8", "2", "4", "64"));
        
        // Component: Hall Sensors (Entries 31-45)
        fmeaEntries.add(createFmeaEntry("FMEA-031", "Hall Sensor A", "Detect rotor position",
            "Sensor output stuck high", "Always indicates magnetic field", "Position misread", "Lock state unknown",
            "8", "Sensor IC failure", "2", "Sensor plausibility check", "4", "64",
            "ASIL-D", "Dual sensor comparison", "85%", "None - adequate", "HW Team", "2026-02-01",
            "Closed", "Redundancy verified", "8", "2", "3", "48"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-032", "Hall Sensor A", "Detect rotor position",
            "Sensor output stuck low", "Never detects magnetic field", "Position unknown", "Lock verification failed",
            "8", "Sensor IC failure", "2", "Sensor B cross-check", "4", "64",
            "ASIL-D", "Redundant sensor", "85%", "None - adequate", "HW Team", "2026-02-01",
            "Closed", "Design validated", "8", "2", "3", "48"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-033", "Hall Sensor A", "Provide position feedback",
            "Signal noise", "False transitions", "Position counting errors", "Incorrect lock state",
            "7", "EMI, poor PCB layout", "4", "Software filtering", "5", "140",
            "ASIL-D", "Digital filtering", "80%", "Improve PCB layout", "HW Team", "2026-05-01",
            "Open", "", "7", "3", "4", "84"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-034", "Hall Sensor A", "Sense magnetic field",
            "Sensitivity degradation", "Reduced detection range", "Missed position", "Lock not verified",
            "7", "Temperature, aging", "3", "Redundant sensor B", "4", "84",
            "ASIL-D", "Dual sensors", "85%", "None - acceptable", "HW Team", "2026-02-01",
            "Closed", "Redundancy sufficient", "7", "3", "3", "63"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-035", "Hall Sensor A", "Detect position",
            "Supply voltage too low", "Sensor not operational", "No position feedback", "Lock state unknown",
            "8", "Voltage regulator failure", "2", "Supply monitoring", "3", "48",
            "ASIL-D", "Voltage supervision", "90%", "None - adequate", "HW Team", "2026-02-01",
            "Closed", "Monitoring confirmed", "8", "2", "2", "32"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-036", "Hall Sensor B", "Detect rotor position",
            "Sensor output stuck high", "Always indicates field", "Cross-check fails", "System enters safe state",
            "6", "Sensor IC failure", "2", "Discrepancy detection", "3", "36",
            "ASIL-D", "Sensor comparison", "90%", "None - adequate", "HW Team", "2026-02-01",
            "Closed", "Verified with testing", "6", "2", "2", "24"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-037", "Hall Sensor B", "Detect rotor position",
            "Sensor output stuck low", "Never detects field", "Discrepancy detected", "Safe state entered",
            "6", "Sensor IC failure", "2", "Sensor A comparison", "3", "36",
            "ASIL-D", "Dual sensor check", "90%", "None - adequate", "HW Team", "2026-02-01",
            "Closed", "Fault injection tested", "6", "2", "2", "24"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-038", "Hall Sensor B", "Provide feedback",
            "Wrong mounting position", "Incorrect phase relationship", "Position calculation error", "Lock state error",
            "9", "Assembly error", "2", "Phase check at startup", "4", "72",
            "ASIL-D", "Self-test routine", "80%", "Add assembly check", "QA Team", "2026-03-31",
            "In Progress", "Procedure being written", "9", "1", "3", "27"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-039", "Hall Sensor B", "Sense field",
            "Magnetic interference", "False position detection", "Position errors", "Lock verification issues",
            "7", "External magnetic field", "3", "Plausibility with sensor A", "5", "105",
            "ASIL-D", "Cross-comparison", "80%", "Add magnetic shielding", "HW Team", "2026-06-15",
            "Open", "", "7", "2", "4", "56"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-040", "Hall Sensor B", "Detect position",
            "Connection open", "No signal", "Sensor B fault detected", "Degraded operation",
            "7", "Solder joint failure, connector issue", "2", "Pull-up/down, timeout", "3", "42",
            "ASIL-D", "Signal monitoring", "90%", "None - adequate", "HW Team", "2026-02-01",
            "Closed", "Detection verified", "7", "2", "2", "28"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-041", "Hall Sensors", "Provide redundant position",
            "Common cause failure", "Both sensors fail", "No position feedback", "Lock state unknown",
            "9", "PCB contamination, voltage spike", "2", "Different sensor types", "5", "90",
            "ASIL-D", "Sensor diversity", "70%", "Use different technologies", "HW Team", "2026-08-01",
            "Open", "", "9", "1", "4", "36"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-042", "Hall Sensors", "Measure position",
            "Sensors out of sync", "Timing mismatch", "Position ambiguity", "Verification delayed",
            "5", "Different sensor delays", "3", "Timestamp comparison", "4", "60",
            "ASIL-C", "Time windowing", "80%", "None - acceptable", "SW Team", "2026-02-01",
            "Closed", "Algorithm validated", "5", "3", "3", "45"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-043", "Hall Sensors", "Detect position",
            "Mounting bracket loose", "Air gap increased", "Reduced signal strength", "Intermittent detection",
            "7", "Vibration, thermal cycling", "3", "Signal amplitude check", "6", "126",
            "ASIL-C", "None", "50%", "Add signal strength monitoring", "SW Team", "2026-07-01",
            "Open", "", "7", "2", "4", "56"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-044", "Hall Sensors", "Provide feedback",
            "PCB trace corrosion", "High resistance connection", "Weak signal", "Detection errors",
            "6", "Moisture ingress", "2", "Conformal coating inspection", "6", "72",
            "ASIL-C", "IP rating", "60%", "Improve sealing", "Quality Team", "2026-05-15",
            "In Progress", "Seal design updated", "6", "1", "5", "30"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-045", "Hall Sensors", "Detect magnetic field",
            "Magnet misalignment", "Weak field at sensors", "Poor signal quality", "Position uncertainty",
            "6", "Assembly tolerance stack-up", "4", "Signal quality monitoring", "5", "120",
            "ASIL-C", "Amplitude check", "70%", "Tighten tolerances", "Mechanical Team", "2026-04-30",
            "Open", "", "6", "3", "4", "72"));
        
        // Component: Power Supply (Entries 46-58)
        fmeaEntries.add(createFmeaEntry("FMEA-046", "Power Supply", "Provide 5V regulated",
            "Regulator failure - output high", "Overvoltage on IC supply", "IC damage", "System inoperative",
            "10", "Regulator component failure", "2", "Overvoltage protection", "3", "60",
            "ASIL-D", "Crowbar circuit", "95%", "None - adequate", "HW Team", "2026-02-01",
            "Closed", "Protection tested", "10", "2", "2", "40"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-047", "Power Supply", "Provide 5V regulated",
            "Regulator failure - output low", "Undervoltage", "Brownout reset", "System restart",
            "7", "Regulator failure, overload", "2", "Brownout detection", "3", "42",
            "ASIL-D", "Undervoltage lockout", "90%", "None - adequate", "HW Team", "2026-02-01",
            "Closed", "UVLO verified", "7", "2", "2", "28"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-048", "Power Supply", "Filter supply voltage",
            "Input capacitor failure", "Voltage ripple increased", "Noise on circuits", "Malfunction possible",
            "6", "Capacitor aging, overvoltage", "3", "Supply monitoring", "5", "90",
            "ASIL-C", "Voltage supervision", "70%", "Use higher rated caps", "HW Team", "2026-05-01",
            "Open", "", "6", "2", "4", "48"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-049", "Power Supply", "Protect from reverse polarity",
            "Protection diode failure short", "No reverse protection", "System damaged if reversed", "Destruction",
            "10", "ESD, overvoltage", "1", "None - protection is the mitigation", "8", "80",
            "ASIL-D", "MOSFET protection", "60%", "Replace diode with MOSFET", "HW Team", "2026-06-01",
            "Open", "", "10", "1", "5", "50"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-050", "Power Supply", "Protect from overvoltage",
            "TVS diode failure open", "No transient protection", "IC damage from spike", "System failure",
            "9", "Transient absorption capacity exceeded", "2", "Voltage monitoring", "6", "108",
            "ASIL-D", "Redundant TVS", "60%", "Add second TVS stage", "HW Team", "2026-05-15",
            "In Progress", "BOM revision pending", "9", "1", "4", "36"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-051", "Power Supply", "Provide stable voltage",
            "Load dump event", "Voltage spike to 40V", "Component overstress", "Potential damage",
            "8", "Alternator load dump", "4", "TVS clamping", "4", "128",
            "ASIL-D", "Load dump protection", "85%", "Verify TVS rating", "HW Team", "2026-02-01",
            "Closed", "ISO 7637-2 tested", "8", "4", "3", "96"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-052", "Power Supply", "Supply motor driver",
            "Motor supply short to ground", "Fuse blown", "No motor operation", "Lock inoperative",
            "9", "Wiring damage, water ingress", "2", "Fuse status monitoring", "4", "72",
            "ASIL-D", "Electronic fuse", "85%", "None - adequate", "HW Team", "2026-02-01",
            "Closed", "Fuse detection verified", "9", "2", "3", "54"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-053", "Power Supply", "Distribute power",
            "PCB trace fusing", "Open circuit", "Partial system failure", "Unpredictable behavior",
            "8", "Overcurrent, poor design", "2", "Current monitoring", "5", "80",
            "ASIL-D", "Current limiting", "75%", "Increase trace width", "HW Team", "2026-04-15",
            "In Progress", "PCB redesign started", "8", "1", "4", "32"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-054", "Power Supply", "Filter noise",
            "EMI filter degradation", "Increased conducted emissions", "EMC compliance risk", "Potential recall",
            "5", "Component aging", "3", "EMC testing", "7", "105",
            "ASIL-B", "None", "40%", "Periodic EMC verification", "Test Team", "2026-06-30",
            "Open", "", "5", "3", "6", "90"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-055", "Power Supply", "Provide power",
            "Connector contact resistance", "Voltage drop", "Undervoltage at load", "System brownout",
            "7", "Connector corrosion, contamination", "3", "Voltage monitoring at load", "5", "105",
            "ASIL-C", "Voltage supervision", "75%", "Gold-plate contacts", "HW Team", "2026-05-01",
            "Open", "", "7", "2", "4", "56"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-056", "Power Supply", "Decouple supply",
            "Decoupling capacitor failure", "Voltage spikes on supply", "IC latchup possible", "System crash",
            "8", "Capacitor aging, overvoltage", "2", "Latchup protection in IC", "6", "96",
            "ASIL-D", "IC internal protection", "65%", "Add redundant capacitors", "HW Team", "2026-07-01",
            "Open", "", "8", "1", "5", "40"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-057", "Power Supply", "Regulate voltage",
            "Thermal shutdown", "Regulator turns off", "Power loss", "System reset",
            "7", "Overtemperature condition", "3", "Temperature monitoring", "4", "84",
            "ASIL-C", "Thermal sensor", "80%", "Improve cooling", "HW Team", "2026-04-30",
            "In Progress", "Thermal analysis ongoing", "7", "2", "3", "42"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-058", "Power Supply", "Monitor battery",
            "Battery voltage sense error", "Incorrect voltage reading", "False undervoltage warning", "Unnecessary shutdown",
            "5", "Divider resistor drift", "3", "Plausibility check with alternate source", "5", "75",
            "ASIL-B", "None", "60%", "Use precision resistors", "HW Team", "2026-05-15",
            "Open", "", "5", "2", "4", "40"));
        
        // Component: CAN Communication (Entries 59-70)
        fmeaEntries.add(createFmeaEntry("FMEA-059", "CAN Transceiver", "Transmit CAN messages",
            "Transceiver stuck dominant", "CAN bus blocked", "No communication possible", "System isolated",
            "8", "Transceiver IC failure", "2", "Bus error detection", "4", "64",
            "ASIL-D", "Bus-off recovery", "80%", "None - acceptable", "HW Team", "2026-02-01",
            "Closed", "Bus-off tested", "8", "2", "3", "48"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-060", "CAN Transceiver", "Receive CAN messages",
            "Transceiver stuck recessive", "Messages not transmitted", "Timeout at receivers", "Lock commands lost",
            "9", "Transceiver failure", "2", "ACK monitoring", "4", "72",
            "ASIL-D", "Message ACK check", "85%", "None - adequate", "HW Team", "2026-02-01",
            "Closed", "ACK monitoring verified", "9", "2", "3", "54"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-061", "CAN Controller", "Manage CAN protocol",
            "CAN controller bus-off", "No CAN communication", "Timeout detected", "Fallback mode entered",
            "7", "Excessive errors on bus", "4", "Bus-off detection", "3", "84",
            "ASIL-D", "Automatic recovery", "90%", "None - adequate", "SW Team", "2026-02-01",
            "Closed", "Recovery logic tested", "7", "4", "2", "56"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-062", "CAN Physical Layer", "Transmit differential signal",
            "CAN_H line short to ground", "Asymmetric signal", "Communication errors", "Message loss",
            "7", "Wiring damage", "2", "Error counter monitoring", "5", "70",
            "ASIL-D", "Error detection", "75%", "Add cable protection", "HW Team", "2026-05-01",
            "Open", "", "7", "1", "4", "28"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-063", "CAN Physical Layer", "Transmit differential signal",
            "CAN_L line short to Vbat", "Bus damage risk", "Multiple nodes damaged", "Network failure",
            "9", "Wiring damage, water ingress", "2", "Transceiver protection", "4", "72",
            "ASIL-D", "Overvoltage protection", "80%", "Verify protection rating", "HW Team", "2026-02-01",
            "Closed", "Protection verified", "9", "2", "3", "54"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-064", "CAN Physical Layer", "Differential signaling",
            "CAN lines short together", "No differential voltage", "Communication impossible", "Network down",
            "9", "Connector damage, pinched cable", "2", "Bus monitoring", "5", "90",
            "ASIL-D", "Timeout detection", "70%", "Improve cable routing", "HW Team", "2026-06-01",
            "Open", "", "9", "1", "4", "36"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-065", "CAN Termination", "Terminate CAN bus",
            "Termination resistor failure open", "Reflections, communication errors", "Intermittent messages", "Unreliable operation",
            "6", "Resistor failure, poor solder", "3", "Error rate monitoring", "6", "108",
            "ASIL-C", "Error statistics", "60%", "Use high reliability resistors", "HW Team", "2026-05-15",
            "Open", "", "6", "2", "5", "60"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-066", "CAN Message Handling", "Process received messages",
            "Message buffer overflow", "Messages lost", "Commands missed", "Lock state uncertain",
            "8", "High bus load, slow processing", "3", "Buffer monitoring", "4", "96",
            "ASIL-D", "Buffer overflow detection", "85%", "Increase buffer size", "SW Team", "2026-04-01",
            "In Progress", "Code optimization ongoing", "8", "2", "3", "48"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-067", "CAN Message Handling", "Decode messages",
            "Message ID filter wrong", "Wrong messages accepted", "Invalid data processed", "Undefined behavior",
            "8", "Configuration error", "2", "Message validation", "4", "64",
            "ASIL-D", "Data plausibility check", "80%", "Improve configuration management", "SW Team", "2026-03-30",
            "In Progress", "Filter review scheduled", "8", "1", "3", "24"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-068", "CAN Message Handling", "Transmit messages",
            "Message priority inverted", "Status sent before critical command ack", "Timing issues", "Command delays",
            "5", "Configuration error", "2", "Timing analysis", "6", "60",
            "ASIL-C", "Priority verification", "65%", "Review message prioritization", "SW Team", "2026-04-15",
            "Open", "", "5", "1", "5", "25"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-069", "CAN Protocol", "Ensure message delivery",
            "Babbling idiot node", "Bus congestion", "Reduced bandwidth", "Timeout events",
            "7", "Software error in another node", "3", "Bus load monitoring", "5", "105",
            "ASIL-C", "Timeout handling", "70%", "None - not in our control", "SW Team", "2026-02-01",
            "Closed", "Timeout mechanism verified", "7", "3", "4", "84"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-070", "CAN Protocol", "Detect communication errors",
            "Error passive state", "No error signaling", "Errors not propagated", "Silent failures",
            "7", "Excessive error count", "3", "Error state monitoring", "4", "84",
            "ASIL-D", "State machine monitoring", "80%", "None - adequate", "SW Team", "2026-02-01",
            "Closed", "Error states tested", "7", "3", "3", "63"));
        
        // Component: Locking Mechanism (Entries 71-85)
        fmeaEntries.add(createFmeaEntry("FMEA-071", "Lock Bolt", "Engage steering column",
            "Bolt deformation", "Incomplete engagement", "Reduced locking force", "Possible bypass",
            "9", "Excessive force, material fatigue", "3", "Position verification", "4", "108",
            "ASIL-D", "Dual position sensors", "85%", "Improve material strength", "Mechanical Team", "2026-06-15",
            "Open", "", "9", "2", "3", "54"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-072", "Lock Bolt", "Provide locking force",
            "Bolt breakage", "No locking force", "Lock ineffective", "Vehicle unsecured",
            "10", "Material defect, overload", "2", "Force sensor", "5", "100",
            "ASIL-D", "None", "50%", "Add force monitoring", "Mechanical Team", "2026-07-01",
            "Open", "", "10", "1", "4", "40"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-073", "Gear Train", "Transmit torque",
            "Gear tooth breakage", "Loss of drive", "No actuation", "Lock inoperative",
            "10", "Overload, material defect", "2", "Current monitoring", "4", "80",
            "ASIL-D", "Blocked rotor detection", "80%", "Increase gear strength", "Mechanical Team", "2026-05-30",
            "Open", "", "10", "1", "3", "30"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-074", "Gear Train", "Reduce speed, increase torque",
            "Gear backlash excessive", "Position uncertainty", "Lock verification delayed", "Extended lock time",
            "5", "Wear, manufacturing tolerances", "5", "Position sensor redundancy", "5", "125",
            "ASIL-C", "Dual sensors", "75%", "Tighten gear tolerances", "Mechanical Team", "2026-04-30",
            "In Progress", "Gear specification revision", "5", "4", "4", "80"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-075", "Gear Train", "Transmit motion",
            "Gear lubrication loss", "Increased friction", "Higher current", "Thermal stress",
            "6", "Seal failure, high temperature", "4", "Current monitoring", "5", "120",
            "ASIL-C", "Current limit", "75%", "Improve seal design", "Mechanical Team", "2026-05-15",
            "In Progress", "Seal testing ongoing", "6", "3", "4", "72"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-076", "Return Spring", "Provide return force",
            "Spring breakage", "No return force", "Manual unlock required", "Emergency release needed",
            "7", "Fatigue, corrosion", "3", "Position monitoring", "5", "105",
            "ASIL-C", "Position verification", "70%", "Improve spring material", "Mechanical Team", "2026-06-01",
            "Open", "", "7", "2", "4", "56"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-077", "Return Spring", "Store mechanical energy",
            "Spring relaxation", "Reduced return force", "Incomplete return", "Position uncertainty",
            "5", "Creep, high temperature", "4", "Position timeout", "5", "100",
            "ASIL-B", "Timeout detection", "75%", "Use high-temp spring", "Mechanical Team", "2026-05-01",
            "Open", "", "5", "3", "4", "60"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-078", "Lock Housing", "Contain mechanism",
            "Housing crack", "Misalignment", "Increased friction", "Lock malfunction",
            "7", "Mechanical stress, impact", "2", "Current monitoring", "6", "84",
            "ASIL-C", "Current monitoring", "60%", "Strengthen housing", "Mechanical Team", "2026-07-15",
            "Open", "", "7", "1", "5", "35"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-079", "Lock Housing", "Support components",
            "Water ingress", "Corrosion", "Mechanism seizure", "Lock failure",
            "8", "Seal degradation, damage", "3", "IP rating verification", "6", "144",
            "ASIL-C", "Sealed design", "65%", "Improve IP rating to IP6K9K", "Mechanical Team", "2026-08-01",
            "Open", "", "8", "2", "5", "80"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-080", "Locking Pawl", "Engage with column",
            "Pawl wear", "Engagement depth reduced", "Lower locking force", "Security compromised",
            "8", "Repeated cycling, contamination", "4", "Force verification", "6", "192",
            "ASIL-D", "Position verification", "65%", "Surface treatment for wear", "Mechanical Team", "2026-06-15",
            "Open", "", "8", "3", "5", "120"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-081", "Locking Pawl", "Retain position",
            "Pawl spring failure", "Pawl not held", "Premature release", "Unexpected unlock",
            "9", "Spring fatigue", "2", "Position monitoring", "4", "72",
            "ASIL-D", "Continuous position check", "80%", "Redundant retention", "Mechanical Team", "2026-05-30",
            "In Progress", "Design concept in work", "9", "1", "3", "27"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-082", "Cam Mechanism", "Convert rotation to linear motion",
            "Cam surface wear", "Lost motion", "Reduced bolt travel", "Incomplete locking",
            "8", "High contact stress, contamination", "4", "Position sensor end stops", "5", "160",
            "ASIL-D", "Position verification", "75%", "Improve cam material", "Mechanical Team", "2026-07-01",
            "Open", "", "8", "3", "4", "96"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-083", "Cam Follower", "Follow cam profile",
            "Follower roller seizure", "Increased friction", "Motor stall", "Lock incomplete",
            "8", "Bearing failure", "3", "Current/position monitoring", "4", "96",
            "ASIL-D", "Stall detection", "85%", "Use sealed bearings", "Mechanical Team", "2026-05-15",
            "In Progress", "Bearing selection ongoing", "8", "2", "3", "48"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-084", "Mechanical Stop", "Limit travel",
            "Stop wear", "Over-travel possible", "Component damage", "Mechanism damage",
            "6", "Repeated impact", "4", "Position limit in software", "4", "96",
            "ASIL-C", "Software position limit", "80%", "Reinforce stop", "Mechanical Team", "2026-06-01",
            "Open", "", "6", "3", "3", "54"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-085", "Fasteners", "Secure components",
            "Screw loosening", "Component movement", "Misalignment", "Mechanism binding",
            "7", "Vibration, thermal cycling", "4", "Periodic inspection", "7", "196",
            "ASIL-C", "None", "40%", "Use thread-locking compound", "Assembly Team", "2026-04-15",
            "In Progress", "Process change initiated", "7", "2", "5", "70"));
        
        // Component: Software (Entries 86-100)
        fmeaEntries.add(createFmeaEntry("FMEA-086", "State Machine", "Manage lock states",
            "Invalid state transition", "Undefined state", "Unpredictable behavior", "System malfunction",
            "9", "Software defect", "2", "State validation", "3", "54",
            "ASIL-D", "State plausibility check", "85%", "None - adequate", "SW Team", "2026-02-01",
            "Closed", "Code review passed", "9", "2", "2", "36"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-087", "State Machine", "Control lock sequence",
            "State machine stuck", "No state changes", "System frozen", "Lock inoperative",
            "9", "Software defect, timing issue", "2", "Watchdog timer", "3", "54",
            "ASIL-D", "Watchdog monitoring", "90%", "None - adequate", "SW Team", "2026-02-01",
            "Closed", "Watchdog tested", "9", "2", "2", "36"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-088", "Command Processing", "Execute commands",
            "Command ignored", "No response", "Timeout at sender", "Retry mechanism triggered",
            "6", "Buffer full, priority inversion", "3", "Acknowledgment required", "4", "72",
            "ASIL-C", "ACK mechanism", "80%", "Increase buffer size", "SW Team", "2026-04-01",
            "Open", "", "6", "2", "3", "36"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-089", "Command Processing", "Validate commands",
            "Invalid command accepted", "Undefined operation", "Unexpected behavior", "Potential malfunction",
            "8", "Validation logic error", "2", "Input validation", "3", "48",
            "ASIL-D", "Command validation", "90%", "None - adequate", "SW Team", "2026-02-01",
            "Closed", "Validation logic verified", "8", "2", "2", "32"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-090", "Diagnostic Module", "Execute diagnostics",
            "Diagnostic false positive", "False fault reported", "Unnecessary system shutdown", "Availability loss",
            "5", "Threshold too sensitive", "4", "Debouncing logic", "4", "80",
            "ASIL-C", "Fault confirmation", "80%", "Tune diagnostic thresholds", "SW Team", "2026-03-30",
            "In Progress", "Calibration ongoing", "5", "3", "3", "45"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-091", "Diagnostic Module", "Detect faults",
            "Diagnostic false negative", "Real fault not detected", "Latent fault exists", "Safety margin reduced",
            "8", "Diagnostic coverage insufficient", "3", "Redundant diagnostics", "4", "96",
            "ASIL-D", "Multiple diagnostic checks", "85%", "Improve diagnostic coverage", "SW Team", "2026-05-01",
            "Open", "", "8", "2", "3", "48"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-092", "Timing Control", "Schedule tasks",
            "Timing violation", "Task deadline missed", "Control loop delayed", "Performance degradation",
            "7", "CPU overload, interrupt latency", "3", "Timing monitoring", "5", "105",
            "ASIL-D", "Deadline monitoring", "75%", "Optimize task scheduling", "SW Team", "2026-04-15",
            "In Progress", "Performance analysis underway", "7", "2", "4", "56"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-093", "Timing Control", "Manage timeouts",
            "Timeout too short", "False timeout errors", "Premature error detection", "Nuisance faults",
            "5", "Calibration error", "3", "Timeout testing", "5", "75",
            "ASIL-C", "Calibration verification", "75%", "Review timeout values", "SW Team", "2026-03-15",
            "In Progress", "Testing in progress", "5", "2", "4", "40"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-094", "Memory Management", "Manage RAM",
            "Memory leak", "RAM exhausted", "Allocation failure", "System degradation",
            "8", "Improper deallocation", "2", "Static allocation strategy", "3", "48",
            "ASIL-D", "Static memory only", "95%", "None - adequate", "SW Team", "2026-02-01",
            "Closed", "Static allocation enforced", "8", "2", "2", "32"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-095", "Memory Management", "Manage variables",
            "Uninitialized variable", "Random value used", "Unpredictable behavior", "System malfunction",
            "8", "Coding error", "2", "Compiler warnings, static analysis", "3", "48",
            "ASIL-D", "Initialization checking", "90%", "Enable all warnings", "SW Team", "2026-02-01",
            "Closed", "MISRA checking enabled", "8", "1", "2", "16"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-096", "Calibration Module", "Apply calibration",
            "Wrong calibration loaded", "Incorrect parameters", "Poor performance", "Function degraded",
            "6", "Version mismatch", "3", "Version checking", "4", "72",
            "ASIL-C", "Calibration CRC check", "85%", "Implement version check", "SW Team", "2026-04-01",
            "Open", "", "6", "2", "3", "36"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-097", "Data Logging", "Log diagnostic data",
            "Log overflow", "Old data lost", "Diagnostic history incomplete", "Troubleshooting difficult",
            "3", "Excessive logging rate", "5", "Ring buffer strategy", "6", "90",
            "ASIL-A", "None", "70%", "None - not safety critical", "SW Team", "2026-02-01",
            "Closed", "Acceptable as-is", "3", "5", "6", "90"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-098", "Communication Module", "Format messages",
            "Byte order error", "Data misinterpreted", "Wrong values used", "Incorrect operation",
            "8", "Endianness handling error", "2", "Data validation", "3", "48",
            "ASIL-D", "Plausibility checking", "85%", "None - adequate", "SW Team", "2026-02-01",
            "Closed", "Endianness verified", "8", "2", "2", "32"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-099", "Safety Monitor", "Monitor safety parameters",
            "Monitor disabled", "Safety violations undetected", "Unsafe operation possible", "Hazard present",
            "10", "Software error, configuration", "2", "Monitor alive check", "3", "60",
            "ASIL-D", "Alive signal monitoring", "90%", "None - adequate", "SW Team", "2026-02-01",
            "Closed", "Alive logic verified", "10", "2", "2", "40"));
        
        fmeaEntries.add(createFmeaEntry("FMEA-100", "Software Integrator", "Integrate modules",
            "Interface mismatch", "Wrong data passed", "Module malfunction", "System error",
            "8", "Integration error", "2", "Interface testing", "3", "48",
            "ASIL-D", "Integration test suite", "90%", "None - adequate", "SW Team", "2026-02-01",
            "Closed", "Integration tests passed", "8", "2", "2", "32"));
        
        return fmeaEntries;
    }
    
    // ============================================================================
    // HELPER METHODS - Object Creators
    // ============================================================================
    
    private static Map<String, Object> createRequirement(String id, String name, String rationale, String dal, List<String> traces) {
        Map<String, Object> req = new HashMap<>();
        req.put("id", id);
        req.put("name", name);
        req.put("rationale", rationale);
        req.put("dal", dal);
        req.put("traces", traces != null ? traces : new ArrayList<>());
        return req;
    }
    
    private static Map<String, Object> createBaseEvent(String id, String name, String failureModel, 
                                                        String failureRate, String latency) {
        Map<String, Object> event = new HashMap<>();
        event.put("id", id);
        event.put("name", name);
        event.put("failure_model", failureModel);
        event.put("failure_rate", failureRate);
        if (latency != null) {
            event.put("latency", latency);
        } else {
            event.put("latency", 0); // Default to 0 if not provided
        }
        return event;
    }
    
    private static Map<String, Object> createDocument(String id, String name, String description, 
                                                       String author, String date) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("id", id);
        doc.put("name", name);
        doc.put("description", description);
        doc.put("author", author);
        doc.put("date", date);
        return doc;
    }
    
    private static Map<String, Object> createFmeaEntry(
            String itemNumber, String component, String function,
            String failureMode, String effectLocal, String effectNextLevel, String effectEnd,
            String severity, String causes, String occurrence, 
            String currentControls, String detection, String rpn,
            String asil, String safetyMechanism, String diagnosticCoverage,
            String recommendedActions, String responsibility, String targetDate,
            String status, String actionsTaken,
            String newSeverity, String newOccurrence, String newDetection, String newRpn) {
        
        Map<String, Object> fmea = new HashMap<>();
        
        // Item identification
        fmea.put("item_number", itemNumber);
        fmea.put("component", component);
        fmea.put("function", function);
        
        // Failure analysis
        fmea.put("failure_mode", failureMode);
        fmea.put("effect_local", effectLocal);
        fmea.put("effect_next_level", effectNextLevel);
        fmea.put("effect_end", effectEnd);
        
        // Current state assessment
        fmea.put("severity", severity);
        fmea.put("causes", causes);
        fmea.put("occurrence", occurrence);
        fmea.put("current_controls", currentControls);
        fmea.put("detection", detection);
        fmea.put("rpn", rpn);
        
        // Safety classification
        fmea.put("asil", asil);
        fmea.put("safety_mechanism", safetyMechanism);
        fmea.put("diagnostic_coverage", diagnosticCoverage);
        
        // Action tracking
        fmea.put("recommended_actions", recommendedActions);
        fmea.put("responsibility", responsibility);
        fmea.put("target_date", targetDate);
        fmea.put("status", status);
        fmea.put("actions_taken", actionsTaken);
        
        // Revised assessment
        fmea.put("new_severity", newSeverity);
        fmea.put("new_occurrence", newOccurrence);
        fmea.put("new_detection", newDetection);
        fmea.put("new_rpn", newRpn);
        
        return fmea;
    }
    
    // ============================================================================
    // STRUCTURED FMEA - Hierarchical model
    // ============================================================================
    
    /**
     * Get structured FMEA model with hierarchical component-failure-effect-cause structure.
     * 
     * @return FMEA model containing 10 components, each with 3 failure modes,
     *         each failure mode with 3 effects and 2 causes
     */
    public static Map<String, Object> getStructuredFmea() {
        Map<String, Object> fmeaModel = new HashMap<>();
        fmeaModel.put("id", "FMEA-MODEL-001");
        fmeaModel.put("name", "ESL System FMEA");
        
        List<Map<String, Object>> components = new ArrayList<>();
        
        // Create 10 components
        for (int c = 1; c <= 10; c++) {
            Map<String, Object> component = new HashMap<>();
            component.put("id", "COMP-" + String.format("%03d", c));
            component.put("name", "Component " + c);
            
            List<Map<String, Object>> failureModes = new ArrayList<>();
            
            // Each component has 3 failure modes
            for (int f = 1; f <= 3; f++) {
                Map<String, Object> failureMode = new HashMap<>();
                failureMode.put("id", "FM-" + String.format("%03d", c) + "-" + String.format("%03d", f));
                failureMode.put("name", "Failure Mode " + c + "." + f);
                
                List<Map<String, Object>> effects = new ArrayList<>();
                
                // Each failure mode has 3 effects
                for (int e = 1; e <= 3; e++) {
                    Map<String, Object> effect = new HashMap<>();
                    effect.put("id", "EFF-" + String.format("%03d", c) + "-" + String.format("%03d", f) + "-" + String.format("%03d", e));
                    effect.put("name", "Effect " + c + "." + f + "." + e);
                    effects.add(effect);
                }
                
                List<Map<String, Object>> causes = new ArrayList<>();
                
                // Each failure mode has 2 causes
                for (int ca = 1; ca <= 2; ca++) {
                    Map<String, Object> cause = new HashMap<>();
                    cause.put("id", "CAUSE-" + String.format("%03d", c) + "-" + String.format("%03d", f) + "-" + String.format("%03d", ca));
                    cause.put("name", "Cause " + c + "." + f + "." + ca);
                    causes.add(cause);
                }
                
                failureMode.put("effects", effects);
                failureMode.put("causes", causes);
                failureModes.add(failureMode);
            }
            
            component.put("failureModes", failureModes);
            components.add(component);
        }
        
        fmeaModel.put("components", components);
        return fmeaModel;
    }
}
