import logging

from recomsystem.api.api_shared import parse_llm_json_response
from recomsystem.config.supported_llms import LLMs, get_llm_by_name
from recomsystem.model.tech_extraction import TechnologiesExtraction, get_tech_extraction_prompt_template
from recomsystem.processing.query_llm import query_llm

logger = logging.getLogger(__name__)


def extract_technologies_from_message(message: str, llm_name: str) -> list:
    """
    Extrahiert eine Liste von Technologien aus der Benutzeranfrage mit Hilfe des LLM.

    Args:
        message: Die Benutzeranfrage
        llm_name: Der Name des zu verwendenden LLMs

    Returns:
        Liste von extrahierten Technologienamen oder leere Liste bei Fehler
    """
    try:
        # Prompt-Template und Parser für die Extraktion verwenden
        prompt_template, parser = get_tech_extraction_prompt_template()

        # Prompt formatieren
        extraction_prompt = prompt_template.format(query=message)

        # LLM für die Extraktion abrufen
        llm = get_llm_by_name(LLMs, llm_name)
        if not llm:
            logger.error(f"LLM {llm_name} not found for technology extraction")
            return []

        # Technologie-Extraktion vom LLM anfordern
        response, _ = query_llm(llm, TechnologiesExtraction, extraction_prompt)
        logger.info(f"LLM response for technology extraction: {response}")
        parsed_techs = parse_llm_json_response(TechnologiesExtraction, llm.model, response)
        logger.info(f"Parsed technologies: {parsed_techs}")

        return parsed_techs.technologies if parsed_techs and hasattr(parsed_techs, "technologies") else []

    except Exception as e:
        logger.error(f"Error extracting technologies: {str(e)}", exc_info=True)
        return []
