import json
import logging
import os
import re

from langchain_core.output_parsers import PydanticOutputParser
from pydantic import BaseModel

from recomsystem.api.api_shared import create_query
from recomsystem.config.supported_llms import LLM, LLMs, get_llm_by_name
from recomsystem.handler.prompt_handler import PromptHandler
from recomsystem.model.response import LLMResponse

logger = logging.getLogger("matilda_recom.ui.chat")

prompt_handler = PromptHandler()


def query_llm(llm: LLM, response_model: BaseModel, prompt: str):
    """Creates a query structure for LLM models using a pre-formatted prompt."""

    system_prompt = prompt_handler.get_prompt(
        prompt_type="recommendation",
        prompt_name="system_prompt",
    )

    query_prompt = create_query(
        response_object=response_model,
        prompt=prompt,
    )

    logger.debug(f"--------- Sending formatted prompt to LLM: --------- \n{prompt[:2000]}...")

    response, extr_price = llm.get_functions_module().query(
        system_prompt,
        query_prompt,
        llm.model,
        llm.input_price,
        llm.output_price,
        llm.output_limit,
        gcp_project=os.getenv("GCLOUD_PROJECT_ID", None),
        gcp_region=llm.available_region,
    )

    return response, extr_price


# Function to call the appropriate LLM based on selection
def get_llm_response(prompt: str, llm_choice: str) -> LLMResponse | str:
    """Get response from the selected LLM"""
    try:
        logger.info(f"Getting response using {llm_choice} for prompt: '{prompt[:50]}...'")

        llm = get_llm_by_name(LLMs, llm_choice)
        if not llm:
            logger.error(f"LLM {llm_choice} not found in configuration")
            return f"Error: LLM {llm_choice} not configured properly"

        logger.debug(f"Using external LLM: {llm.model}")
        response, price = query_llm(llm, LLMResponse, prompt)
        logger.info(f"External LLM response received. Cost: {price}")
        logger.info(f"Received response from {llm.model}: \n{response} \n -- Calculated price: {price} --")

        if isinstance(response, LLMResponse):
            return response.content
        elif isinstance(response, str):
            return response
        else:
            logger.warning(f"Unexpected response type: {type(response)}")
            return str(response)
    except Exception as e:
        error_msg = f"Error getting LLM response: {str(e)}"
        logger.error(error_msg, exc_info=True)
        return f"Error: {str(e)}"


def parse_llm_json_response(response_model: type[BaseModel], model_name: str, response: str) -> BaseModel | None:
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
