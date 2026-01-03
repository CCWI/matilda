import os
from dataclasses import dataclass


@dataclass
class ProcessingConfig:
    """Configuration class to store all constants and paths"""

    # Database configuration
    NEO4J_URI: str = os.getenv("NEO4J_URI", "bolt://localhost:7687")
    NEO4J_USER: str = os.getenv("NEO4J_USER", "neo4j")
    NEO4J_PASSWORD: str = os.getenv("NEO4J_PASSWORD", "")

    # Data file paths
    LIBS_RESULT_PATH: str = "data/libs_to_predict_result.csv"
    DESIGN_DECISIONS_PATH: str = "data/matilda-design-decisions-1679987748620.csv"
    CATEGORY_TECH_MAP_PATH: str = "data/category_tech_map(generated).csv"
