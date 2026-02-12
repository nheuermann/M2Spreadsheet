# Mock Model Data Reference

## Overview

`MockModelData.java` provides realistic automotive safety data for testing M2Spreadsheet with complex, EMF-like object structures. The data simulates a typical ISO 26262 project for an **Electronic Steering Lock (ESL) System**.

## Data Structure

### Variables Section (Top-Level)

The mock data provides both scalar and collection variables:

#### Scalar Variables
- `projectName`: "Electronic Steering Lock System"
- `projectCode`: "ESL-2026-01"
- `safetyStandard`: "ISO 26262"
- `asil`: "ASIL-D"
- `generationDate`: "2026-02-12"

#### Collection Variables
- `requirements`: List of 15 requirement objects
- `faultTreeEvents`: List of 35 fault tree base event objects
- `referenceDocuments`: List of 20 reference document objects
- `fmeaEntries`: List of 100 FMEA entry objects

### Object Definitions (Data Section)

#### 1. Requirements (15 objects)
Each requirement has:
- `id`: String (e.g., "REQ-ESL-001")
- `name`: String (e.g., "System Power Supply")
- `rationale`: String (detailed requirement description)
- `dal`: String - Development Assurance Level ("DAL-A", "DAL-B", or "DAL-C")

**Sample Data:**
```
REQ-ESL-001: System Power Supply (DAL-A)
REQ-ESL-002: Locking Force (DAL-A)
REQ-ESL-003: Lock Engagement Time (DAL-B)
...
REQ-ESL-015: Ignition Interlock (DAL-A)
```

**DAL Distribution:**
- DAL-A: 8 requirements (highest criticality)
- DAL-B: 4 requirements
- DAL-C: 3 requirements

#### 2. Fault Tree Base Events (35 objects)
Each event has:
- `id`: String (e.g., "BE-001")
- `name`: String (e.g., "Motor Winding Open Circuit")
- `failure_model`: String ("Permanent" or "Transient")
- `failure_rate`: String (scientific notation, e.g., "1.5E-7")
- `latency`: String (optional, present in ~1/3 of events, e.g., "0.5")

**Sample Data:**
```
BE-001: Motor Winding Open Circuit (Permanent, 1.5E-7, latency: 0.5)
BE-002: Hall Sensor A Failure (Transient, 3.2E-6, latency: 1.0)
BE-003: Hall Sensor B Failure (Transient, 3.2E-6, latency: 1.0)
...
BE-035: Backup Power Supply Depletion (Permanent, 8.2E-7)
```

**Latency Coverage:**
- Events with latency: BE-001 through BE-012 (12 events, exactly 1/3)
- Events without latency: BE-013 through BE-035 (23 events)

**Failure Model Distribution:**
- Permanent: 24 events (68.6%)
- Transient: 11 events (31.4%)

#### 3. Reference Documents (20 objects)
Each document has:
- `id`: String (e.g., "DOC-001")
- `name`: String (e.g., "ISO 26262-1:2018")
- `description`: String (document description)
- `author`: String (e.g., "ISO", "Engineering Team", "TÜV SÜD Assessor")
- `date`: String (ISO format: "YYYY-MM-DD")

**Sample Data:**
```
DOC-001: ISO 26262-1:2018 (ISO, 2018-12-01)
DOC-005: ESL System Requirements Specification (Engineering Team, 2025-08-15)
DOC-020: Functional Safety Assessment (TÜV SÜD Assessor, 2026-02-01)
```

**Document Categories:**
- ISO Standards: 4 documents (DOC-001 to DOC-004)
- Project Documentation: 9 documents (DOC-005 to DOC-013)
- Supplier Documentation: 2 documents (DOC-014 to DOC-015)
- Test Reports: 5 documents (DOC-016 to DOC-020)

#### 4. FMEA Entries (100 objects)
Each FMEA entry has **24 columns** representing a complete FMEA analysis:

**Identification Columns:**
- `item_number`: String (e.g., "FMEA-001")
- `component`: String (e.g., "Microcontroller", "DC Motor", "Hall Sensor A")
- `function`: String (component function description)

**Failure Analysis Columns:**
- `failure_mode`: String (how the component can fail)
- `effect_local`: String (immediate effect of failure)
- `effect_next_level`: String (effect at next system level)
- `effect_end`: String (end effect on vehicle/user)

**Current State Assessment:**
- `severity`: String (1-10, criticality of effect)
- `causes`: String (root causes of failure)
- `occurrence`: String (1-10, probability of occurrence)
- `current_controls`: String (existing detection/prevention measures)
- `detection`: String (1-10, ability to detect before reaching customer)
- `rpn`: String (Risk Priority Number = Severity × Occurrence × Detection)

**Safety Classification:**
- `asil`: String (Automotive Safety Integrity Level: "ASIL-A" through "ASIL-D")
- `safety_mechanism`: String (implemented safety measures)
- `diagnostic_coverage`: String (percentage, e.g., "85%")

**Action Tracking:**
- `recommended_actions`: String (proposed improvements)
- `responsibility`: String (team/person responsible)
- `target_date`: String (completion date, "YYYY-MM-DD")
- `status`: String ("Open", "In Progress", "Closed")
- `actions_taken`: String (description of completed actions)

**Revised Assessment (after actions):**
- `new_severity`: String (1-10, revised severity)
- `new_occurrence`: String (1-10, revised occurrence)
- `new_detection`: String (1-10, revised detection)
- `new_rpn`: String (revised RPN)

**Sample Data:**
```
FMEA-001: Microcontroller / Execute control logic
  Failure Mode: CPU core failure
  Effect: Vehicle not secured
  Current: S=10, O=3, D=7, RPN=210, ASIL-D
  Safety Mechanism: Dual-core lockstep (99% coverage)
  Action: Improve ESD protection (HW Team, 2026-03-15, In Progress)
  Revised: S=10, O=2, D=5, RPN=100

FMEA-016: DC Motor / Provide mechanical actuation
  Failure Mode: Winding open circuit
  Effect: Vehicle not secured
  Current: S=10, O=2, D=3, RPN=60, ASIL-D
  Safety Mechanism: Current supervision (90% coverage)
  Status: Closed

FMEA-071: Lock Bolt / Engage steering column
  Failure Mode: Bolt deformation
  Effect: Possible bypass
  Current: S=9, O=3, D=4, RPN=108, ASIL-D
  Safety Mechanism: Dual position sensors (85% coverage)
  Action: Improve material strength (Mechanical Team, 2026-06-15, Open)
  Revised: S=9, O=2, D=3, RPN=54
```

**Component Coverage (100 entries):**
- Microcontroller: FMEA-001 to FMEA-015 (15 entries)
- DC Motor: FMEA-016 to FMEA-030 (15 entries)
- Hall Sensors: FMEA-031 to FMEA-045 (15 entries)
- Power Supply: FMEA-046 to FMEA-058 (13 entries)
- CAN Communication: FMEA-059 to FMEA-070 (12 entries)
- Locking Mechanism: FMEA-071 to FMEA-085 (15 entries)
- Software: FMEA-086 to FMEA-100 (15 entries)

**Status Distribution:**
- Closed: ~45% (actions completed and verified)
- In Progress: ~30% (actions underway)
- Open: ~25% (actions planned but not started)

**ASIL Distribution:**
- ASIL-D: ~60% (highest safety criticality)
- ASIL-C: ~30%
- ASIL-B: ~8%
- ASIL-A: ~2%

## Usage in Tests

### Basic Usage

```java
import io.github.nheuermann.m2spreadsheet.tests.MockModelData;

// Get all variables from mock data
Map<String, Object> variables = MockModelData.getVariables();

// Pass to M2Spreadsheet generation
GenerationResult result = M2SpreadsheetUtils.generate(
    templateWorkbook,
    queryEnv,
    variables,
    resourceSet,
    outputURI,
    monitor
);
```

### AQL Expression Examples

#### Access Scalar Variables
```
{m:projectName}           → "Electronic Steering Lock System"
{m:projectCode}           → "ESL-2026-01"
{m:asil}                  → "ASIL-D"
```

#### Collection Operations
```
{m:requirements->size()}          → 15
{m:faultTreeEvents->size()}       → 35
{m:referenceDocuments->size()}    → 20
{m:fmeaEntries->size()}           → 100
```

#### Access First Element
```
{m:requirements->first().id}              → "REQ-ESL-001"
{m:requirements->first().name}            → "System Power Supply"
{m:requirements->first().dal}             → "DAL-A"

{m:faultTreeEvents->first().id}           → "BE-001"
{m:faultTreeEvents->first().name}         → "Motor Winding Open Circuit"
{m:faultTreeEvents->first().failure_rate} → "1.5E-7"
{m:faultTreeEvents->first().latency}      → "0.5"

{m:referenceDocuments->first().name}      → "ISO 26262-1:2018"
{m:referenceDocuments->first().author}    → "ISO"

{m:fmeaEntries->first().item_number}      → "FMEA-001"
{m:fmeaEntries->first().component}        → "Microcontroller"
{m:fmeaEntries->first().failure_mode}     → "CPU core failure"
{m:fmeaEntries->first().severity}         → "10"
{m:fmeaEntries->first().rpn}              → "210"
{m:fmeaEntries->first().asil}             → "ASIL-D"
{m:fmeaEntries->first().new_rpn}          → "100"
```

#### Filtering and Selection
```
{m:requirements->select(r | r.dal = 'DAL-A')->size()}  → 8
{m:faultTreeEvents->select(e | e.failure_model = 'Permanent')->size()}  → 24
{m:fmeaEntries->select(f | f.asil = 'ASIL-D')->size()}  → ~60
{m:fmeaEntries->select(f | f.status = 'Closed')->size()}  → ~45
{m:fmeaEntries->select(f | f.component = 'Microcontroller')->size()}  → 15
```

### Example Test Template

```java
XSSFWorkbook templateWorkbook = new XSSFWorkbook();

// Summary sheet
Sheet summarySheet = templateWorkbook.createSheet("Summary");
summarySheet.createRow(0).createCell(0).setCellValue("Project: {m:projectName}");
summarySheet.createRow(1).createCell(0).setCellValue("Standard: {m:safetyStandard}");
summarySheet.createRow(2).createCell(0).setCellValue("ASIL: {m:asil}");

// Requirements sheet
Sheet reqSheet = templateWorkbook.createSheet("Requirements");
Row header = reqSheet.createRow(0);
header.createCell(0).setCellValue("ID");
header.createCell(1).setCellValue("Name");
header.createCell(2).setCellValue("DAL");

Row dataRow = reqSheet.createRow(1);
dataRow.createCell(0).setCellValue("{m:requirements->first().id}");
dataRow.createCell(1).setCellValue("{m:requirements->first().name}");
dataRow.createCell(2).setCellValue("{m:requirements->first().dal}");

// Use mock data
Map<String, Object> variables = MockModelData.getVariables();
// ... generate with variables
```

## Real-World Context

The mock data is based on typical automotive safety engineering artifacts:

### Electronic Steering Lock (ESL) System
- **Safety Function**: Prevents unauthorized vehicle operation
- **ASIL Level**: ASIL-D (highest automotive safety integrity level)
- **Standard**: ISO 26262 (Road vehicles - Functional safety)

### Requirements Coverage
- Power supply and electrical requirements
- Mechanical locking force and timing
- Communication (CAN bus)
- Sensors and diagnostics
- Safety mechanisms (watchdog, fault detection)
- Environmental robustness

### Fault Tree Events
- Hardware failures (motor, sensors, PCB)
- Electronic component failures (microcontroller, memory, MOSFETs)
- Software failures (stack overflow, checksum errors)
- Environmental factors (EMI, thermal, vibration)

### Reference Documents
- ISO 26262 standard series
- System/software/hardware specifications
- Safety analyses (HARA, FTA, FMEA)
- Test reports (EMC, environmental)
- Assessment reports

## Extending the Mock Data

To add more data or modify existing data:

1. **Add new object types**: Create new `createXxx()` methods
2. **Add to variables**: Update `getVariables()` method
3. **Modify object properties**: Update `createXxx()` helper methods
4. **Adjust counts**: Change loop iterations in `getXxx()` methods

Example:
```java
public static List<Map<String, Object>> getTestCases() {
    List<Map<String, Object>> testCases = new ArrayList<>();
    // Add test case objects...
    return testCases;
}

// In getVariables():
variables.put("testCases", getTestCases());
```

## Test Results

The `testWithMockModelData()` test verifies:
- ✓ Scalar variables correctly evaluated
- ✓ Collection sizes correctly computed (15, 35, 20)
- ✓ First element access working
- ✓ Property access on complex objects
- ✓ Multiple sheets generated correctly
- ✓ All 70 objects (15+35+20) accessible

The `testFmeaData()` test verifies:
- ✓ Large collection handling (100 FMEA entries)
- ✓ Complex objects with 24 properties each
- ✓ Full FMEA table generation
- ✓ All FMEA columns accessible (item_number through new_rpn)
- ✓ Status tracking, ASIL classification, RPN calculations

**Total Mock Data:**
- **170 objects** total (15 requirements + 35 FT events + 20 documents + 100 FMEA entries)
- **24 columns** in FMEA table (typical automotive FMEA structure)
- **7 component types** in FMEA (Microcontroller, Motor, Sensors, Power, CAN, Mechanism, Software)

## Performance

- **Generation time**: 
  - ~0.5 seconds for 4 sheets with 70 objects (testWithMockModelData)
  - ~0.6 seconds for FMEA table with 100 entries × 24 columns (testFmeaData)
- **Memory footprint**: Minimal (all data in simple Maps, ~2400 properties for FMEA alone)
- **Scalability**: Successfully handles 100+ objects with 20+ properties each

## See Also

- `SimpleM2SpreadsheetTest.java` - See `testWithMockModelData()` and `testFmeaData()` for usage examples
- `MapServices.java` - Enables property access on Map objects
- `M2SpreadsheetUtils.java` - Main API for generation with `createQueryEnvironment()`
- `AQL-INTEGRATION-STATUS.md` - Technical details on AQL integration
