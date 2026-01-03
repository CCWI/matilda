import logging
import os

import yaml
from dotenv import find_dotenv, load_dotenv

logger = logging.getLogger(__name__)


def parse_yaml_file(file_path: str) -> dict:
    with open(file_path) as f:
        return yaml.safe_load(f)


if os.getenv("EXEC_ENV") != "cloud":
    logger.info("Loading .env file for local environment")
    load_dotenv(find_dotenv())
else:
    logger.info("Running in cloud environment")

env_config = {
    "gcp_project_id": os.getenv("GCLOUD_PROJECT_ID", None),
    "backend_port": int(os.getenv("SERVICE_PORT", "8000")),
    "frontend_port": int(os.getenv("SERVICE_PORT", "7860")),
    "amount_matilda_recommendations": 10,
}

logger.info(f"Environment configuration loaded successfully: {env_config}")
