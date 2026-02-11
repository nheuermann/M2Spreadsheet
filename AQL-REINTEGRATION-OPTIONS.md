# AQL Re-integration Options for M2Spreadsheet

## Current Status

**Yes, AQL (Acceleo Query Language) was removed** to get the initial build working. This was necessary because:
- AQL libraries are **not available in Maven Central**
- AQL is distributed as OSGi bundles via Eclipse P2 repositories
- Initial goal was standalone VS Code testing without Eclipse/OSGi

## What We Removed

### From Plugin POM:
```xml
<!-- REMOVED -->
<dependency>
    <groupId>org.eclipse.acceleo</groupId>
    <artifactId>org.eclipse.acceleo.query</artifactId>
    <version>7.0.1</version>
</dependency>
```

### From Code:
- `IQueryEnvironment` parameter from generation methods
- AQL expression evaluation (replaced with simple Map navigation)
- Service registration mechanism

### Current Workaround:
Simple expression evaluator that only handles:
- Property access: `{m:customer.name}`
- Nested properties: `{m:customer.address.city}`
- **No** AQL features: conditionals, loops, type coercion, services, etc.

---

## Why You Need AQL

AQL provides:
1. **Expression Language**: Full featured query language (like AQL in M2Doc)
2. **Type System**: Proper type handling for EMF models
3. **Services**: Extensible service registration (@ServiceProvider)
4. **Built-in Operations**: String manipulation, collections, math, etc.
5. **Validation**: Expression validation before generation

Without AQL, you can't do:
```java
{m:customer.orders->select(o | o.total > 100)}  // Collection operations
{m:if customer.isPremium then 'VIP' else 'Regular' endif}  // Conditionals
{m:customer.name.toUpper()}  // Built-in services
{m:myCustomService(data)}  // Custom services
```

---

## Re-integration Options

### Option 1: Use Eclipse P2 Repository (Recommended for Long-term)

**Best for**: Production use, full M2Doc compatibility

#### Approach A: Tycho/OSGi Build
Switch from pure Maven to Tycho (Maven + OSGi):

**Pros**:
- Perfect Eclipse/OSGi integration
- Access to all Eclipse repositories
- Same build system as M2Doc
- Can use all Eclipse dependencies

**Cons**:
- More complex build
- Slower builds
- Still requires Eclipse for full development
- VS Code standalone testing becomes harder

**Implementation**:
```xml
<!-- Parent POM - switch to Tycho -->
<parent>
    <groupId>org.eclipse.tycho</groupId>
    <artifactId>tycho-pom</artifactId>
    <version>4.0.10</version>
</parent>

<build>
    <plugins>
        <plugin>
            <groupId>org.eclipse.tycho</groupId>
            <artifactId>tycho-maven-plugin</artifactId>
            <version>4.0.10</version>
            <extensions>true</extensions>
        </plugin>
    </plugins>
</build>

<!-- Add target platform with Eclipse repository -->
```

See: `releng/org.obeonetwork.m2doc.targetplatforms/` in M2Doc

---

#### Approach B: P2 Maven Plugin
Use Eclipse P2 repositories in regular Maven:

**Pros**:
- Keep Maven build (simpler than Tycho)
- Access Eclipse repositories
- VS Code testing still possible

**Cons**:
- Less common approach
- May have dependency resolution issues
- Not officially supported path

**Implementation**:
```xml
<!-- Add P2 repository -->
<repositories>
    <repository>
        <id>eclipse-2024-03</id>
        <layout>p2</layout>
        <url>https://download.eclipse.org/releases/2024-03/</url>
    </repository>
</repositories>

<!-- Then add AQL dependency -->
<dependency>
    <groupId>org.eclipse.acceleo</groupId>
    <artifactId>org.eclipse.acceleo.query</artifactId>
    <version>7.0.1</version>
</dependency>
```

**Issue**: Maven doesn't natively understand P2 repositories. Needs plugin:
```xml
<plugin>
    <groupId>org.reficio</groupId>
    <artifactId>p2-maven-plugin</artifactId>
</plugin>
```

---

### Option 2: Manual JAR Installation (Quick & Dirty)

**Best for**: Development, quick prototyping, learning

Download AQL JARs from Eclipse and install to local Maven repository:

**Steps**:
```bash
# 1. Download from Eclipse update site
wget https://download.eclipse.org/acceleo/updates/releases/7.0.1/plugins/org.eclipse.acceleo.query_7.0.1.jar

# 2. Install to local Maven repo
mvn install:install-file \
  -Dfile=org.eclipse.acceleo.query_7.0.1.jar \
  -DgroupId=org.eclipse.acceleo \
  -DartifactId=org.eclipse.acceleo.query \
  -Dversion=7.0.1 \
  -Dpackaging=jar

# 3. Repeat for dependencies (antlr, guava, etc.)
```

**Pros**:
- Quick to set up
- Works with regular Maven
- VS Code testing works
- Good for learning/prototyping

**Cons**:
- Manual process (not reproducible in CI/CD)
- Must track and download all transitive dependencies
- Version updates require manual work
- Not suitable for team development

---

### Option 3: Embed AQL JARs in Repository (Team-friendly)

**Best for**: Small teams, want simplicity

Create `libs/` folder and commit JARs:

```
M2Spreadsheet/
├── libs/
│   ├── org.eclipse.acceleo.query-7.0.1.jar
│   ├── org.antlr.runtime-3.2.0.jar
│   └── (other dependencies)
└── plugins/
    └── io.github.nheuermann.m2spreadsheet/
        └── pom.xml
```

**POM Configuration**:
```xml
<dependency>
    <groupId>org.eclipse.acceleo</groupId>
    <artifactId>org.eclipse.acceleo.query</artifactId>
    <version>7.0.1</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/../../libs/org.eclipse.acceleo.query-7.0.1.jar</systemPath>
</dependency>
```

**Pros**:
- Simple for team (just clone and build)
- Works with regular Maven
- VS Code testing works
- Reproducible

**Cons**:
- Increases repository size
- Version updates still manual
- Frowned upon in open-source projects
- License restrictions may apply

---

### Option 4: Build AQL from Source (Advanced)

**Best for**: Need latest features, want full control

Clone and build Acceleo Query:

```bash
git clone https://github.com/eclipse-acceleo/acceleo.git
cd acceleo/query
mvn clean install
```

Then use as local dependency.

**Pros**:
- Full control over version
- Can contribute fixes
- Latest features

**Cons**:
- Complex build process
- Must maintain fork if customizations needed
- Overkill for most use cases

---

## Recommended Approach

### For Your Use Case (Learning/Development in VS Code):

**Go with Option 2: Manual JAR Installation**

Why:
1. ✅ Quick to set up (30 minutes)
2. ✅ Works with VS Code standalone testing
3. ✅ Keeps Maven build simple
4. ✅ Perfect for learning M2Spreadsheet
5. ✅ Can transition to Option 1 (Tycho) later for production

### Implementation Steps:

1. **Download AQL and dependencies**
2. **Install to local Maven repo**
3. **Update POMs to add AQL dependency back**
4. **Update code to re-add IQueryEnvironment**
5. **Test with AQL expressions**

---

## Let Me Know Your Choice

**Question**: Which option do you prefer?

A. **Option 2 (Manual JAR)** - I can script the download/install for you right now (15 mins)
B. **Option 1 (Tycho)** - More work, but production-ready (2-3 hours)
C. **Option 3 (Embed JARs)** - Middle ground (30 mins)
D. **Keep current simple evaluator** - Finish basic M2Spreadsheet first, add AQL later

### My Recommendation: **Option A (Manual JAR)**
- Gets you AQL quickly
- Doesn't complicate build
- You can focus on M2Spreadsheet logic, not build infrastructure
- Later: transition to Tycho when ready for production

**Want me to set it up?** I can:
1. Find and download all required AQL JARs
2. Script the Maven install commands
3. Update your POMs
4. Restore AQL integration in M2SpreadsheetUtils
5. Test with a full AQL expression

Just say "yes, set up AQL with Option 2" and I'll do it! 🚀
