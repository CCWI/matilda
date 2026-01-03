# MATILDA

**M**achine-Learning **A**dvisor **T**o **I**dentify **L**ibrary-related **D**ecision **A**lternatives

A research system for mining software evolution patterns from GitHub and providing AI-powered technology migration recommendations.

## Overview

MATILDA consists of two main components:

### 1. Data Crawling System (Java/Spring Boot)

A distributed microservice architecture for collecting and analyzing software projects from GitHub.

**Key Features:**
- Crawls GitHub repositories and extracts dependency information
- Identifies technology migration patterns (design decisions)
- Stores migration data in Neo4j knowledge graph
- Analyzes statistics on technology evolution

**Technology Stack:** Java 11, Spring Boot, Spring Cloud, Apache Kafka, MongoDB, PostgreSQL, Neo4j

**[→ Full Documentation](data_crawling_system/README.md)**

### 2. Recommendation System (Python/FastAPI)

An AI-powered recommendation engine that provides technology migration suggestions based on real-world patterns.

**Key Features:**
- Query-based recommendations via web interface (Gradio)
- REST API for programmatic access (FastAPI)
- Multiple LLM support (OpenAI GPT, Google Gemini, Anthropic Claude, Mistral)
- Evidence-based suggestions from GitHub migration patterns

**Technology Stack:** Python 3.12, FastAPI, Gradio, LangChain, Neo4j, pandas

**[→ Full Documentation](recommendation_system/README.md)**

---

## Quick Start

### Prerequisites
- Docker & Docker Compose
- Java 11+ & Maven 3.5.4+ (for data crawling)
- Python 3.12 & Poetry (for recommendations)
- Neo4j database
- API keys for LLM providers

### 1. Start Data Crawling System

```bash
cd data_crawling_system

# Start infrastructure (MongoDB, Kafka, PostgreSQL)
sh docker-up.technologies.sh

# Start core services
sh docker-up.services.sh

# Start crawlers and processors
docker-compose up matilda-crawler matilda-dataextractor matilda-analyzer
```

**Access:** http://localhost:8080/swagger-ui.html

### 2. Start Recommendation System

```bash
cd recommendation_system

# Install dependencies
poetry install

# Configure environment variables
cp .env.example .env
# Edit .env with your credentials

# Run application
poetry run python -m recomsystem.main
```

**Access:** 
- Web UI: http://127.0.0.1:7860
- API: http://127.0.0.1:8000/docs

---

## Architecture

```
MATILDA System
├── Data Crawling System
│   ├── matilda-crawler        → Crawl GitHub repositories
│   ├── matilda-dataextractor  → Extract dependencies & design decisions
│   ├── matilda-analyzer       → Analyze migration patterns
│   └── Neo4j Knowledge Graph  → Store technology relationships
│
└── Recommendation System
    ├── FastAPI Backend        → REST API for recommendations
    ├── Gradio Frontend        → Interactive web interface
    ├── LLM Integration        → OpenAI, Gemini, Claude, Mistral
    └── MATILDA Processor      → Query knowledge graph & generate insights
```

---

## Dataset

The system generates and uses the following data:

**From Data Crawling:**
- GitHub project metadata and dependencies
- Technology migration patterns (design decisions)
- Library categorization and similarity scores

**For Recommendations:**
- `libs_to_predict_result.csv` - Maven artifact to technology mapping
- `matilda-design-decisions-[timestamp].csv` - Migration patterns
- `category_tech_map(generated).csv` - Technology categorization

---

## Documentation

Detailed documentation for each component:

- **[Data Crawling System →](data_crawling_system/README.md)**
  - Setup & deployment
  - Service architecture
  - API endpoints
  - Data extraction process

- **[Recommendation System →](recommendation_system/README.md)**
  - Installation guide
  - API documentation
  - LLM configuration
  - Data requirements

---

## Project Structure

```
matilda/
├── README.md                      # This file
├── LICENSE
├── data_crawling_system/          # Java microservices for GitHub mining
│   ├── README.md                  # Detailed documentation
│   ├── docker-compose.yml
│   ├── matilda-crawler/           # GitHub crawler
│   ├── matilda-dataextractor/     # Dependency extraction
│   ├── matilda-analyzer/          # Migration analysis
│   └── ...                        # Other services
│
└── recommendation_system/         # Python recommendation engine
    ├── README.md                  # Detailed documentation
    ├── pyproject.toml
    ├── recomsystem/               # Main application
    │   ├── api/                   # LLM integrations
    │   ├── processing/            # MATILDA logic
    │   ├── ui/                    # Gradio interface
    │   └── main.py                # Entry point
    └── tests/
```

---

## License

This is a research prototype developed as part of a doctoral research project.
