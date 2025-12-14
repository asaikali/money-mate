# Money Mate - Multi-Module Refactoring Plan

## Phase 1: Multi-Module Project Structure Refactoring ✅ COMPLETED

### Goal
Transform the current single-module Spring Boot application into a multi-module Maven project with:
- Parent POM at root
- `money-mate-api` module (Spring Boot HATEOAS backend)
- `money-mate-agent` module (Spring AI MCP server)

**Status**: ✅ All tasks completed successfully. Multi-module structure is in place and building.

### Current Structure
```
money-mate/
├── pom.xml                                    # Single module POM
├── src/
│   ├── main/java/com/example/moneymate/
│   │   ├── MoneyMateApplication.java
│   │   ├── ObpTestRunner.java
│   │   ├── config/
│   │   │   └── ObpClientConfig.java
│   │   ├── properties/
│   │   │   └── ObpProperties.java
│   │   └── obp/
│   │       ├── client/
│   │       │   ├── ObpAuthenticationService.java
│   │       │   └── ObpUserService.java
│   │       ├── dto/
│   │       │   ├── auth/DirectLoginResponse.java
│   │       │   └── user/UserDetailsResponse.java
│   │       ├── exception/
│   │       │   ├── ObpAuthenticationException.java
│   │       │   └── ObpClientException.java
│   │       └── interceptor/
│   │           └── DirectLoginInterceptor.java
│   ├── main/resources/
│   │   ├── application.yaml
│   │   ├── application-local.yaml
│   │   └── application-public-sandbox.yaml
│   └── test/java/com/example/moneymate/
│       └── MoneyMateApplicationTests.java
├── obp-api/                                   # HTTP request collection
├── sandbox/                                   # Docker compose for local OBP
└── README.md
```

### Target Structure
```
money-mate/                                    # Root (parent project)
├── pom.xml                                    # Parent POM (packaging: pom)
├── plan.md                                    # This file
├── README.md                                  # Updated root README
│
├── money-mate-api/                            # Module 1: HATEOAS API
│   ├── pom.xml                                # API module POM
│   ├── README.md                              # API-specific README
│   ├── src/
│   │   ├── main/java/com/example/moneymate/api/
│   │   │   ├── MoneyMateApiApplication.java   # New main class
│   │   │   ├── obp/                           # Moved from root
│   │   │   │   ├── client/
│   │   │   │   ├── dto/
│   │   │   │   ├── exception/
│   │   │   │   └── interceptor/
│   │   │   ├── config/                        # Moved from root
│   │   │   └── properties/                    # Moved from root
│   │   ├── main/resources/
│   │   │   ├── application.yaml               # Moved from root
│   │   │   ├── application-local.yaml
│   │   │   └── application-public-sandbox.yaml
│   │   └── test/java/com/example/moneymate/api/
│   │       └── MoneyMateApiApplicationTests.java
│   ├── obp-api/                               # Moved from root
│   └── sandbox/                               # Moved from root
│
└── money-mate-agent/                          # Module 2: MCP Server
    ├── pom.xml                                # Agent module POM
    ├── README.md                              # Agent-specific README
    └── src/
        ├── main/java/com/example/moneymate/agent/
        │   └── MoneyMateAgentApplication.java # New main class (placeholder)
        ├── main/resources/
        │   └── application.yaml               # Agent-specific config
        └── test/java/com/example/moneymate/agent/
            └── MoneyMateAgentApplicationTests.java
```

### Step-by-Step Refactoring Tasks

#### Step 1: Create Parent POM
- Create new `pom.xml` at root with `<packaging>pom</packaging>`
- Define common properties (Java 25, Spring Boot 3.5.8)
- Declare modules: `money-mate-api`, `money-mate-agent`
- Define dependency management for shared dependencies

#### Step 2: Create money-mate-api Module
- Create `money-mate-api/` directory
- Create `money-mate-api/pom.xml` with parent reference
- Move `src/` directory to `money-mate-api/src/`
- Update package structure: `com.example.moneymate` → `com.example.moneymate.api`
- Rename `MoneyMateApplication.java` → `MoneyMateApiApplication.java`
- Update all package declarations and imports
- Move configuration files to `money-mate-api/src/main/resources/`
- Move test files and update package names
- Move `obp-api/` directory to `money-mate-api/obp-api/`
- Move `sandbox/` directory to `money-mate-api/sandbox/`
- Create `money-mate-api/README.md`

#### Step 3: Create money-mate-agent Module Skeleton
- Create `money-mate-agent/` directory
- Create `money-mate-agent/pom.xml` with parent reference
- Create `money-mate-agent/src/main/java/com/example/moneymate/agent/` structure
- Create placeholder `MoneyMateAgentApplication.java`
- Create `money-mate-agent/src/main/resources/application.yaml`
- Create placeholder test class
- Create `money-mate-agent/README.md`

#### Step 4: Update Build Configuration
- Remove old single-module `pom.xml` (replaced by parent POM)
- Verify Maven wrapper still works
- Update `.gitignore` if needed for module-specific targets

#### Step 5: Verification
- Run `./mvnw clean install` from root to build all modules
- Run `./mvnw spring-boot:run` from `money-mate-api/` to verify API starts
- Verify OBP authentication still works (ObpTestRunner)
- Run tests in both modules

### Expected Outcomes
- ✅ Multi-module Maven structure in place
- ✅ money-mate-api module builds and runs successfully
- ✅ All existing OBP integration code works unchanged
- ✅ money-mate-agent skeleton ready for implementation
- ✅ Clean separation between API and agent concerns
- ✅ Shared dependencies managed in parent POM

### Notes
- This phase is purely structural reorganization
- No functional changes to existing code (except package names)
- ObpTestRunner stays in money-mate-api for now (may remove later)
- Agent module is placeholder only - implementation in later phases

---

## Phase 1 Completion Summary

### ✅ Completed Tasks
1. Created parent POM with multi-module structure
2. Created money-mate-api module with complete source migration
3. Updated all packages from `com.example.moneymate` to `com.example.moneymate.api`
4. Renamed main application class to `MoneyMateApiApplication`
5. Moved obp-api/ and sandbox/ directories to money-mate-api
6. Created money-mate-agent skeleton module
7. All modules build successfully with `mvnw clean package`

### Final Project Structure
```
money-mate/                                    # Root (parent project)
├── pom.xml                                    # Parent POM
├── plan.md                                    # This plan
├── README.md                                  # Root README
├── mvnw / mvnw.cmd                            # Maven wrapper
│
├── money-mate-api/                            # Module 1: HATEOAS API
│   ├── pom.xml
│   ├── README.md
│   ├── src/
│   │   ├── main/java/com/example/moneymate/api/
│   │   │   ├── MoneyMateApiApplication.java
│   │   │   ├── ObpTestRunner.java
│   │   │   ├── config/ObpClientConfig.java
│   │   │   ├── properties/ObpProperties.java
│   │   │   └── obp/                           # OBP client integration
│   │   │       ├── client/
│   │   │       ├── dto/
│   │   │       ├── exception/
│   │   │       └── interceptor/
│   │   ├── main/resources/
│   │   │   ├── application.yaml
│   │   │   ├── application-local.yaml
│   │   │   └── application-public-sandbox.yaml
│   │   └── test/java/com/example/moneymate/api/
│   ├── obp-api/                               # HTTP request collection
│   └── sandbox/                               # Docker compose
│
└── money-mate-agent/                          # Module 2: MCP Server
    ├── pom.xml
    ├── README.md
    └── src/
        ├── main/java/com/example/moneymate/agent/
        │   └── MoneyMateAgentApplication.java
        ├── main/resources/
        │   └── application.yaml
        └── test/java/com/example/moneymate/agent/
            └── MoneyMateAgentApplicationTests.java
```

### Build Verification
```bash
$ ./mvnw clean package

[INFO] Reactor Summary for money-mate 0.0.1-SNAPSHOT:
[INFO]
[INFO] money-mate ......................................... SUCCESS
[INFO] money-mate-api ..................................... SUCCESS
[INFO] money-mate-agent ................................... SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Next Steps
Ready for Phase 2 planning (domain modeling and implementation details to be defined).
