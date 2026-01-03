# MATILDA Data Crawling System

**MATILDA** is a distributed system for collecting, extracting, and analyzing software projects, libraries, and their migration history between technologies (extraction of design decisions). The system crawls repositories, extracts design decisions, and provides recommendations based on migration and dependency analyses.

## Overview

The system is built on a microservice architecture using Spring Boot, Kafka as a message broker, and MongoDB/PostgreSQL as databases. It processes GitHub projects automatically through various stages: Crawling, Extraction, Analysis, and Recommendation.

## System Components

| Service | Port | Description |
|---------|------|-------------|
| **matilda-gateway** | 8080 | API Gateway for all services |
| **matilda-crawler** | 8082 | Crawling GitHub repositories |
| **matilda-dataextractor** | 8083 | Extraction of dependencies and design decisions |
| **matilda-analyzer** | 8084 | Analysis and statistics on migrations |
| **matilda-runner** | - | Batch processing and runner tasks |
| **matilda-auth** | - | Authentication and authorization |
| **matilda-state** | - | State management |
| **matilda-discovery** | - | Service discovery (Eureka) |
| **matilda-lib-manager** | - | Library management |

### Additional Components

- **matilda-base**: Common base classes and utilities
- **matilda-persistence-jpa**: JPA-based persistence (PostgreSQL)
- **matilda-persistence-mongo**: MongoDB-based persistence
- **matilda-korpus-dependencies**: Corpus for dependency analyses
- **matilda-korpus-projects**: Project corpus with analysis data
- **matilda-korpus-libsim-ki**: AI-based library similarity analysis (Python)

## Project Structure

```
data_crawling_system/
├── pom.xml                           # Maven Parent POM
├── docker-compose.yml                # Docker orchestration
├── docker-up.technologies.sh         # Start infrastructure (MongoDB, Kafka, PostgreSQL)
├── docker-up.services.sh             # Start core services
├── docker-up.processors.sh           # Start processing services
├── docker-down.services.sh           # Shutdown script
│
├── matilda-auth/                     # Authentication service
├── matilda-analyzer/                 # Analysis service with SpringBootTests
├── matilda-base/                     # Common base library
├── matilda-crawler/                  # GitHub crawler service
├── matilda-dataextractor/            # Data extraction service
├── matilda-discovery/                # Eureka service discovery
├── matilda-gateway/                  # API Gateway
├── matilda-lib-manager/              # Library management service
├── matilda-runner/                   # Batch runner service
├── matilda-state/                    # State management service
│
├── matilda-persistence-jpa/          # JPA persistence layer (PostgreSQL)
├── matilda-persistence-mongo/        # MongoDB persistence layer
│
├── matilda-korpus-dependencies/      # Dependency corpus
├── matilda-korpus-projects/          # Project corpus with CSV exports
└── matilda-korpus-libsim-ki/         # Python: AI library similarity
```

## Quick Start (Docker)

### Prerequisites
- Docker & Docker Compose
- Java 11+
- Maven 3.5.4+

### ⚠️ Security Configuration

**IMPORTANT**: Before deploying to production, you MUST configure the following:

1. **Change default passwords** in `application.properties` files:
   - Default admin password: `changeme` 
   - Default user password: `userPass`
   
   Set environment variables:
   ```bash
   export ADMIN_PASSWORD=your-secure-password
   export ADMIN_USER=your-admin-username
   export REGULAR_PASSWORD=your-user-password
   export REGULAR_USER=your-username
   ```

2. **Update hardcoded credentials** in:
   - `matilda-gateway/src/main/java/edu/hm/ccwi/matilda/gateway/WebSecurityConfig.java`
   - All `application.properties` files with `${ADMIN_PASSWORD:changeme}`

3. **Security findings from code review**:
   - 9 deprecated classes should be removed
   - Debug code (System.out, printStackTrace) should be replaced with proper logging
   - Consider implementing proper authentication (OAuth2, JWT, database-backed)

### Starting the System

1. **Start infrastructure** (MongoDB, Kafka, PostgreSQL):
   ```bash
   sh docker-up.technologies.sh
   ```
   ⏳ Wait until all services are ready (~2-3 minutes)

2. **Start core services**:
   ```bash
   sh docker-up.services.sh
   ```

3. **Start processing services** (optional):
   ```bash
   docker-compose up --force-recreate --no-deps matilda-crawler matilda-crawler2
   docker-compose up --force-recreate --no-deps matilda-dataextractor matilda-dataextractor2
   docker-compose up --force-recreate --no-deps matilda-analyzer
   ```

### Build (optional)
```bash
docker-compose build --no-cache matilda-crawler matilda-dataextractor matilda-analyzer
```

## API Interfaces

### REST Endpoints

| Service | Endpoint | Description |
|---------|----------|-------------|
| MatildaAnalyzer | `GET /libraries?categoryId={id}` | Retrieve libraries by category |
| MatildaAnalyzer | `POST /technology/import?categoryId={id}` | Import technology |
| Gateway | `/swagger-ui.html` | API documentation |

### SpringBootTest Runners (MatildaAnalyzer)

The following runners are executed as SpringBootTests under `edu.hm.ccwi.matilda.analyzer.service.runner`:

**Note**: All runners are marked with `@Disabled` and must be enabled manually for execution.

#### InconsistentMongoRepositoriesAndRevisionsAndDocsHandler
Cleans up orphaned data in MongoDB collections that have no references.
- **Mode**: `WRITE_MODE = false` (analysis only) or `true` (cleanup)
- **Prerequisite**: Docker-Technologies and Docker-Services running

#### AssambleAndExportRevSimDataset
Creates CSV datasets about documents and used libraries for all revisions.
- **Output**: `target/` folder
- **Prerequisite**: Docker-Technologies and Docker-Services running

#### CleanupInconsistentDependencyCategoriesInEDDs
Cleans up inconsistent dependency categories in ExtractedDesignDecisions (PostgreSQL).
- **Prerequisite**: Docker-Technologies and Docker-Services running

#### AnalyzeStatsServiceImplRunner
⚠️ **Disabled for automated builds** - Manual runner for statistical analysis
Performs various statistical analyses and outputs results to console:
- `analyzeGeneralStatsRunner()`: General statistics
- `analyzeCategoriesOfMigrationsAndCommitsRunner()`: Migration analyses
- `analyzeCategoriesOfMigrationsAndProjectAgeRunner()`: Project age analyses
- `analyzeProjectCommitAgeAmountOfProjectMapRunner()`: Commit age analyses
- `analyzeProjectCommitAgeDesignDecisionMapRunner()`: Design decision analyses

**Prerequisite**: Docker-Technologies and Docker-Services running

**Note**: Integration tests (AnalyzerSpringIT, RecommenderSpringIT) are disabled as they require full infrastructure (MongoDB, Kafka). Enable them only for integration testing with live services.

### Service Runners (MatildaAnalyzer)

Under `edu.hm.ccwi.matilda.analyzer.service.library`:

#### TechnologyAndCategoryMigrator
OneShot service for initial data migration:
1. Persists all LibCategories from enum
2. Persists characteristic types
3. Links ExtractedDesignDecision entries with categories

#### CharacteristicMigrationUsageRankCalculator
🔬 Prototyping phase

### MatildaRunner Main Classes

- **GACategoryTagManualTagsToTotalEnricherRunner**: Enrichment of category tags
- **LibSimClassificationRunner**: ML classification for library similarity

## Manual Installation

### Environment Setup
1. Install Java 11
2. Install Maven 3.5.4+
3. Install and start MongoDB (required for persistence)
4. Install Confluent Platform (Kafka + Zookeeper) or Apache Kafka
5. Install PostgreSQL (required for JPA persistence)

### Security Configuration
Before first start, configure authentication:
```bash
export ADMIN_PASSWORD=your-secure-admin-password
export ADMIN_USER=admin
export REGULAR_PASSWORD=your-secure-user-password
export REGULAR_USER=user
```

### Build & Start
```bash
# Clean build all modules
mvn clean install

# Start individual services
cd matilda-gateway
mvn spring-boot:run -Drun.jvmArguments="-Xms2048m -Xmx4096m"
```

### Create Kafka Topics
```bash
confluent start
# Create topics for crawled, extracted and analyzed projects
kafka-topics --create --topic matildaAnalyzerTopic --bootstrap-server localhost:9092
kafka-topics --create --topic matildaRecommenderTopic --bootstrap-server localhost:9092
```

## Known Issues & Limitations

- **Integration Tests**: AnalyzerSpringIT and RecommenderSpringIT are disabled by default as they require full infrastructure
- **Security**: Default passwords are hardcoded and must be changed before production deployment
- **Test Data**: Some test classes reference non-existent status codes (RECOMMENDATION_FOUND → use FINISHED_ANALYZING_PROJECT instead)
- **Deprecated Code**: 9 classes marked @Deprecated should be reviewed for removal

## Access

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Admin Dashboard**: http://localhost:8081
- **API Gateway**: http://localhost:8080

## Technology Stack

- **Backend**: Java 11, Spring Boot, Spring Cloud
- **ML/AI**: Python (scikit-learn, pandas)
- **Messaging**: Apache Kafka
- **Databases**: MongoDB, PostgreSQL
- **Service Discovery**: Eureka
- **Containerization**: Docker, Docker Compose