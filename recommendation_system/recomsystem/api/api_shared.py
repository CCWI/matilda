import json
import logging
import re
from typing import Type

from langchain_core.output_parsers import PydanticOutputParser
from langchain_google_vertexai import HarmBlockThreshold, HarmCategory
from pydantic import BaseModel

logger = logging.getLogger(__name__)


def create_query(
    response_object: Type[BaseModel],
    prompt: str = None,
) -> list[dict[str, str]]:
    """Creates a query structure for vision models combining images, text and data.

    Args:
        response_object: Pydantic model defining expected response structure
        prompt: Base prompt text

    Returns:
        list[dict[str, str]]: List of query text elements
    """
    # Prüfen, ob der Prompt bereits Formatanweisungen enthält
    has_format_instructions = prompt and ("format_instructions" in prompt or "{format_instructions}" in prompt)

    # Nur Format-Anweisungen hinzufügen, wenn sie nicht bereits enthalten sind
    if not has_format_instructions:
        ouput_parser = PydanticOutputParser(pydantic_object=response_object)
        format_instructions = str(ouput_parser.get_format_instructions())

        if prompt:
            # Suche nach Platzhalter, ansonsten ans Ende anhängen
            if "<<PLACEHOLDER:format_instructions>>" in prompt:
                prompt = prompt.replace("<<PLACEHOLDER:format_instructions>>", format_instructions)
            else:
                prompt += f"\n\n{format_instructions}"

    query = [{"type": "text", "text": prompt}] if prompt else []

    # Add final prompt element if missing
    if not query or "Start with the full JSON Response here:" not in query[-1].get("text", ""):
        query.append({"type": "text", "text": "\n\n Start with the full JSON Response here: \n"})

    return query


def create_classification_query(
    model: Type[BaseModel],
    text: str = None,
    prompt: str = None,
) -> str:
    ouput_parser = PydanticOutputParser(pydantic_object=model)
    prompt = prompt.replace("<<PLACEHOLDER:text_to_classify>>", text)
    prompt = prompt.replace("<<PLACEHOLDER:format_instructions>>", str(ouput_parser.get_format_instructions()))

    return prompt


def parse_llm_json_response(response_model: Type[BaseModel], model_name: str, response: str) -> BaseModel:
    """Parses LLM response into a Pydantic model using multiple parsing strategies.

    Args:
        response_model: Pydantic model class for response validation
        model_name: Name of the LLM model (for logging)
        response: Raw response string from LLM

    Returns:
        BaseModel: Parsed and validated response object

    Raises:
        None: Returns None if all parsing attempts fail
    """
    parser = PydanticOutputParser(pydantic_object=response_model)

    # Convert single response to list for uniform handling
    responses = response if isinstance(response, list) else [response]

    for response_item in responses:
        # Skip None responses
        if response_item is None:
            continue

        # Try various parsing strategies
        try:
            return parser.parse(response_item)
        except Exception as e:
            logger.debug(f"Failed to parse direct response: {e}")

        try:
            # Extract JSON between code blocks
            json_matches = re.findall(r"```(?:json)?\s*(.*?)\s*```", response_item, re.DOTALL)
            if json_matches:
                for json_str in json_matches:
                    try:
                        parsed_response = parser.parse(json_str.strip())
                        logger.info("Successfully parsed response from code block")
                        return parsed_response
                    except Exception as json_err:
                        logger.debug(f"Failed to parse JSON from code block: {json_err}")
        except Exception as e:
            logger.debug(f"Error extracting JSON from code blocks: {e}")

        try:
            # Look for JSON-like structures
            json_like_pattern = r"({[\s\S]*}|\[[\s\S]*\])"
            json_matches = re.findall(json_like_pattern, response_item)

            for json_str in json_matches:
                try:
                    parsed_response = parser.parse(json_str.strip())
                    logger.info("Successfully parsed response from JSON-like structure")
                    return parsed_response
                except Exception as json_err:
                    logger.debug(f"Failed to parse JSON-like structure: {json_err}")
        except Exception as e:
            logger.debug(f"Error extracting JSON-like structures: {e}")

        try:
            # Fix common JSON issues and try to parse
            fixed_json = response_item

            # Fix case where JSON starts with "items": instead of {"items":
            if fixed_json.strip().startswith('"items":'):
                fixed_json = "{" + fixed_json + "}"

            # Fix unbalanced quotes or missing commas
            fixed_json = re.sub(r'"\s*"', '","', fixed_json)

            # Ensure array items are properly separated
            fixed_json = re.sub(r"}\s*{", "},{", fixed_json)

            # Fix unbalanced brackets
            def balance_brackets(json_str):
                # Count open/close curly braces and square brackets
                open_curly = json_str.count("{")
                close_curly = json_str.count("}")
                open_square = json_str.count("[")
                close_square = json_str.count("]")

                # Add missing closing braces/brackets
                if open_curly > close_curly:
                    json_str += "}" * (open_curly - close_curly)
                if open_square > close_square:
                    json_str += "]" * (open_square - close_square)

                return json_str

            fixed_json = balance_brackets(fixed_json)

            try:
                # Validate fixed JSON
                json.loads(fixed_json)  # Just to check if it's valid
                parsed_response = parser.parse(fixed_json)
                logger.info("Successfully parsed response after fixing JSON structure")
                return parsed_response
            except Exception as json_err:
                logger.debug(f"Failed to parse fixed JSON: {json_err}")

        except Exception as e:
            logger.debug(f"Error attempting to fix JSON: {e}")

    logger.error(f"Failed to parse any response items from {model_name}")
    logger.debug(f"Response: {response}")
    return None


def get_safety_settings():
    return {
        HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT: HarmBlockThreshold.BLOCK_NONE,
        HarmCategory.HARM_CATEGORY_HATE_SPEECH: HarmBlockThreshold.BLOCK_NONE,
        HarmCategory.HARM_CATEGORY_HARASSMENT: HarmBlockThreshold.BLOCK_NONE,
        HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT: HarmBlockThreshold.BLOCK_NONE,
        HarmCategory.HARM_CATEGORY_UNSPECIFIED: HarmBlockThreshold.BLOCK_NONE,
    }


def calculate_price(input_token: int, output_token: int, input_token_price, output_token_price) -> float:
    """Calculates the price of the response.

    Args:
        response (dict): Response as dict from OpenAI

    Returns:
        float: The calculated price in USD
    """

    price_per_input_token = float(input_token_price) / 1000000
    price_per_output_token = float(output_token_price) / 1000000

    return (price_per_input_token * input_token) + (price_per_output_token * output_token)
