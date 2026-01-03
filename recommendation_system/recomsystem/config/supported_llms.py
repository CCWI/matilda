import logging
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Optional

import yaml

from recomsystem.api import anthropic, google, mistral, openai

# Setup logger
logger = logging.getLogger("matilda_recom.config")

api_mapping = {
    "google": google,
    "openai": openai,
    "anthropic": anthropic,
    "mistral": mistral,
}


@dataclass
class LLM:
    """Configuration class for Language Learning Models.

    Attributes:
        model: Model identifier string
        input_price: Cost per input token
        output_price: Cost per output token
        functions: API module name (e.g., 'openai', 'gemini')
        available_region: Region where model is available
        input_limit: Maximum input token limit
        output_limit: Maximum output token limit
        extra_attributes: Additional model-specific settings
    """

    model: str = None
    input_price: float = 0.0
    output_price: float = 0.0
    functions: str = None
    available_region: Optional[str] = None
    input_limit: Optional[int] = None
    output_limit: Optional[int] = 8192
    extra_attributes: dict[str, Any] = field(default_factory=dict)

    def __str__(self):
        return self.model

    def __repr__(self):
        return f"<LLM: {self.model}>"

    def get_functions_module(self):
        """Gets the API module for this LLM.

        Returns:
            module: Python module handling the API calls (e.g., openai, gemini)
        """
        if not self.functions:
            logger.error(f"No 'functions' attribute specified for LLM: {self.model}")
            return None

        module = api_mapping.get(self.functions.lower())
        if not module:
            logger.error(f"No API module found for function '{self.functions}' in LLM: {self.model}")
            logger.error(f"Available API modules: {list(api_mapping.keys())}")
        return module


def load_llms_from_yaml(config_file: str) -> dict[str, LLM]:
    """Loads LLM configurations from YAML.

    Args:
        config_file: Path to YAML configuration file

    Returns:
        dict[str, LLM]: Dictionary mapping model names to LLM objects

    Raises:
        FileNotFoundError: If config file doesn't exist
        yaml.YAMLError: If YAML parsing fails
    """
    try:
        with open(config_file, "r") as f:
            llms_data = yaml.safe_load(f)

        llm_objects = {}
        for llm_name, llm_config in llms_data.get("LLMs", {}).items():
            # Validate that each LLM has a functions attribute that exists in api_mapping
            if "functions" not in llm_config:
                logger.warning(f"LLM '{llm_name}' is missing 'functions' attribute in config")
            elif llm_config["functions"].lower() not in api_mapping:
                logger.warning(
                    f"LLM '{llm_name}' has unknown 'functions' value: '{llm_config['functions']}'. "
                    f"Must be one of: {list(api_mapping.keys())}"
                )

            llm_objects[llm_name] = LLM(**llm_config)

        return llm_objects
    except FileNotFoundError:
        logger.error(f"LLM configuration file not found: {config_file}")
        return {}
    except yaml.YAMLError as e:
        logger.error(f"Error parsing YAML configuration: {e}")
        return {}
    except Exception as e:
        logger.error(f"Unexpected error loading LLMs: {e}")
        return {}


def filter_llms_by_region(llms: dict[str, LLM], region: str) -> list[LLM]:
    """Filters LLMs by available region.

    Args:
        llms: Dictionary of available LLMs
        region: Region identifier to filter by

    Returns:
        list[LLM]: List of LLMs available in the specified region
    """
    return [llm for llm in llms.values() if llm.available_region == region]


def get_llm_by_name(llms: dict[str, LLM], name: str) -> Optional[LLM]:
    """Retrieves an LLM by its name.

    Args:
        llms: Dictionary of available LLMs
        name: Name of the LLM to find (case-insensitive)

    Returns:
        Optional[LLM]: Matching LLM object or None if not found
    """
    for llm_name, llm in llms.items():
        if llm_name.lower() == name.lower():
            return llm
    return None


CONFIG_PATH = Path(__file__).resolve().parent.parent / "config" / "supported_llms.yaml"
LLMs = load_llms_from_yaml(CONFIG_PATH)
