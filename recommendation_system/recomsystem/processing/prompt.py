import logging

from langchain.output_parsers import PydanticOutputParser

from recomsystem.config.processing_config import ProcessingConfig
from recomsystem.handler.prompt_handler import PromptHandler
from recomsystem.model.response import RecommendationResponse

# Get configuration
config = ProcessingConfig()
logger = logging.getLogger(__name__)
prompt_handler = PromptHandler()


def create_prompt(
    message: str,
    prompt_type: str = "baseline",
    dependencies: list = None,
    additional_context: str = None,
) -> tuple[str, RecommendationResponse]:
    """
    Erstellt einen Prompt für LLM-Anfragen, ohne den System-Prompt (wird in query_llm gesetzt).

    Args:
        message: Die Benutzeranfrage
        prompt_type: "baseline" oder "matilda"
        prompt: Der eigentliche Prompt-Text, der die Anfrage beschreibt
        dependencies: Optionale Liste von Bibliotheken, mit denen der Benutzer arbeitet
        additional_context: Optionaler zusätzlicher Kontext für den Prompt

    Returns:
        tuple: (formatted_prompt, response_model)
    """
    # Add dependencies and context information to the prompt
    context_info = ""
    if dependencies:
        context_info += "Der Nutzer hat folgende Bibliotheken in seinem Projekt:\n"
        for dep in dependencies:
            context_info += f"- {dep}\n"
        context_info += "\n"

    # Add additional context if provided (e.g. MATILDA recommendations)
    if additional_context:
        context_info += f"{additional_context}\n\n"

    final_prompt = f"{context_info}Nutzeranfrage: {message}\n\n"
    final_prompt += f"\n\n{PydanticOutputParser(pydantic_object=RecommendationResponse).get_format_instructions()}"

    logger.debug(f"Created {prompt_type}-Prompt with {len(final_prompt)} characters")

    return final_prompt, RecommendationResponse
