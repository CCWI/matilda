import logging
import os

from recomsystem.api.api_shared import create_classification_query, parse_llm_json_response
from recomsystem.config.supported_llms import LLMs, get_llm_by_name
from recomsystem.matilda.model.relevance_model_simple import Classification_model

logger = logging.getLogger(__name__)


def classify_text_by_relevance(
    text_to_classify: str,
    expected_return_model=None,
    system_prompt: str = None,
    prompt: str = None,
    gcp_project: str = None,
    gcp_region: str = None,
) -> bool:
    """Classify text by relevance using LLM.

    Args:
        text_to_classify (str): Text to classify
        expected_return_model: The expected return model of the LLM. Defaults to Classification_model.
        system_prompt (str, optional): System prompt for the LLM. Defaults to None.
        prompt (str, optional): Prompt for the LLM. Defaults to None.
        gcp_project (str, optional): GCP project if the LLM is used via GCP. Defaults to None.
        gcp_region (str, optional): GCP region if the LLM is used via GCP. Defaults to None.

    Returns:
        bool: True if the text is classified as professional software, False otherwise.
    """
    # Use defaults if not provided
    if expected_return_model is None:
        expected_return_model = Classification_model
    if system_prompt is None:
        from recomsystem.matilda.relevance_prompts import system_prompt as default_system_prompt

        system_prompt = default_system_prompt
    if prompt is None:
        from recomsystem.matilda.relevance_prompts import prompt as default_prompt

        prompt = default_prompt

    llm = get_llm_by_name(LLMs, "GEMINI_2_5_FLASH")
    if not llm:
        raise ValueError("Error: MATILDA model GEMINI_2_5_FLASH not found in configuration")

    try:
        # 1) create query
        query = create_classification_query(expected_return_model, text_to_classify, prompt)

        # 2) query the model
        gcp_project = gcp_project or os.getenv("GCLOUD_PROJECT_ID", None)
        gcp_region = gcp_region or llm.available_region
        logger.info(f"Querying LLM {llm.model} in project {gcp_project} in region {gcp_region}")
        response_message, price = llm.get_functions_module().query(
            system_prompt,
            query,
            llm.model,
            llm.input_price,
            llm.output_price,
            gcp_project=gcp_project,
            gcp_region=gcp_region,
        )

        # 3) parse response
        parsed_response = parse_llm_json_response(expected_return_model, llm.model, response_message)

        if parsed_response is None:
            raise Exception("Response could not be parsed.")

        return parsed_response.is_professional_software

    except Exception as e:
        logger.error(f"Error classifying text: {e}")
        return False
