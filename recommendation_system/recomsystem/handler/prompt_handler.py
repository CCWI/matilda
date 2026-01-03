import os

import yaml
from jinja2 import Template

config_yaml = "../config/prompts.yaml"


class PromptHandler:
    def __init__(self, prompts_dir: str = None):
        self.prompts_dir = os.path.dirname(os.path.abspath(__file__)) if prompts_dir is None else prompts_dir
        self.prompts = parse_yaml_file(os.path.join(self.prompts_dir, config_yaml))

    def get_prompt(self, prompt_type: str, prompt_name: str, **kwargs) -> str:
        """
        Get an prompt by type and name, rendering it with the provided variables.

        Args:
            prompt_type: Category of prompt (e.g., "extraction", "optimization", ...)
            prompt_name: Specific prompt variant within the category
            **kwargs: Variables to render into the template

        Returns:
            Rendered prompt string
        """
        try:
            return Template(self.prompts[prompt_type][prompt_name]).render(**kwargs)
        except KeyError as e:
            raise ValueError(f"No prompt found for type '{prompt_type}' and name '{prompt_name}'") from e


def parse_yaml_file(file_path: str) -> dict:
    with open(file_path) as f:
        return yaml.safe_load(f)
