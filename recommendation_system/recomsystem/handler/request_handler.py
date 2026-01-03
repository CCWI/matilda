import json
import logging

from recomsystem.api.api_shared import parse_llm_json_response
from recomsystem.config.config import env_config
from recomsystem.config.processing_config import ProcessingConfig
from recomsystem.config.supported_llms import LLM, LLMs, get_llm_by_name
from recomsystem.handler.prompt_handler import PromptHandler
from recomsystem.handler.tech_extraction_handler import extract_technologies_from_message
from recomsystem.model.chat_request import ChatRequest
from recomsystem.model.response import RecommendationResponse
from recomsystem.processing.matilda import RecommendationProcessor
from recomsystem.processing.prompt import create_prompt
from recomsystem.processing.query_llm import query_llm

logger = logging.getLogger(__name__)
prompt_handler = PromptHandler()


def chat_with_matilda(request: ChatRequest, filter_by_similarity: bool, filter_relevant_projects: bool) -> tuple[list[dict], str, dict]:
    """Generate response from MATILDA with RAG-style technology recommendations"""
    try:
        llm = get_llm_by_name(LLMs, request.llm)
        if not llm:
            return f"Error: MATILDA model {request.llm} not found in configuration", None, None

        recom_processor = RecommendationProcessor(ProcessingConfig())
        enhanced_context = None
        matilda_recommendations = None
        recommendations = None

        # Base prompt based on whether dependencies are provided
        if request.dependencies and len(request.dependencies) > 0:
            logger.info(f"Using {len(request.dependencies)} dependencies for MATILDA prompt")

            try:
                # Generate recommendations
                recommendations = recom_processor.process_by_libraries(
                    request.dependencies, filter_similar_projects=filter_by_similarity, filter_relevant_projects=filter_relevant_projects
                )

                # Create enhanced prompt with recommendations
                matilda_recommendations = format_recommendations_for_prompt(recommendations)
                enhanced_context = f"""
MATILDA hat basierend auf den Projekt-Abhängigkeiten folgende Technologie-Empfehlungen identifiziert:
{matilda_recommendations}
                """
                logger.info("Enhanced context with MATILDA recommendations")
            except Exception as rec_error:
                # Fall back to normal dependency handling
                logger.error(f"Error processing recommendations: {str(rec_error)}", exc_info=True)

        else:
            logger.info("No dependencies provided for MATILDA prompt - trying to detect technologies from message")
            extracted_technologies = extract_technologies_from_message(request.message, request.llm)

            if extracted_technologies:
                try:
                    logger.info(f"Processing recommendations based on {len(extracted_technologies)} extracted technologies")

                    # Transform extracted technologies to add single quotes as required by process_by_technologies
                    quoted_technologies = [f"'{tech}'" for tech in extracted_technologies]
                    logger.debug(f"Transformed technologies: {quoted_technologies}")

                    # Generate recommendations
                    recommendations = recom_processor.process_by_technologies(
                        quoted_technologies, filter_similar_projects=filter_by_similarity, filter_relevant_projects=filter_relevant_projects
                    )

                    logger.info("########################### Recommendations generated ###########################")
                    logger.info(f"Recommendations: {recommendations}")
                    logger.info("########################### Recommendations generated ###########################")

                    # Create enhanced prompt with simplified recommendations (for LLM)
                    matilda_recommendations = format_recommendations_for_prompt(recommendations)
                    enhanced_context = f"""
MATILDA hat basierend auf den Projekt-Abhängigkeiten folgende Technologie-Empfehlungen identifiziert:
{matilda_recommendations}
                    """
                    logger.info("Enhanced context with automatically detected technology recommendations")
                except Exception as tech_error:
                    logger.error(f"Error processing technologies: {str(tech_error)}", exc_info=True)

        content, structured_response = query_final_recommendation_by_llm(request, enhanced_context, llm)

        # Return both simplified and detailed recommendations
        return recommendations, content, structured_response, recommendations  # detailed_recommendations same as recommendations for now

    except Exception as e:
        error_msg = f"Error in MATILDA generation: {str(e)}"
        logger.error(error_msg, exc_info=True)
        return [], f"Error: {str(e)}", {}, []
        return f"Error: {str(e)}", None, None, None


def query_final_recommendation_by_llm(request: ChatRequest, enhanced_context: str, llm: LLM) -> tuple[str, dict]:
    # Create the MATILDA prompt
    prompt, response_model = create_prompt(
        message=request.message, prompt_type="matilda", dependencies=request.dependencies, additional_context=enhanced_context
    )

    if enhanced_context:
        matilda_enrichment = prompt_handler.get_prompt(
            prompt_type="recommendation", prompt_name="prompt_matilda_enrichment", matilda_ranking=enhanced_context
        )

        prompt = f"{prompt}\n\n{matilda_enrichment}"

    response, price = query_llm(llm, response_model, prompt)
    logger.info(f"MATILDA response received. Cost: {price}")

    # Parse response to structured format
    if isinstance(response, str):
        parsed_response = parse_llm_json_response(RecommendationResponse, request.llm, response)
        structured_response = parsed_response.model_dump() if parsed_response else None
    elif hasattr(response, "model_dump"):
        structured_response = response.model_dump()
    else:
        structured_response = None

    # Keep original content formatting
    if hasattr(response, "model_dump_json"):
        content = response.model_dump_json(indent=2)
    elif hasattr(response, "__dict__"):
        content = json.dumps(response.__dict__, indent=2, default=str)
    else:
        content = str(response)

    return content, structured_response


def format_recommendations_for_prompt(recommendations: list) -> str:
    """Format recommendations for inclusion in a prompt"""
    if not recommendations:
        return "No specific technology recommendations found."

    formatted = []
    for tech_item in recommendations:
        technology = tech_item.get("technology", "Unknown")
        recommended_techs = tech_item.get("recommended_technologies", [])

        # Only include if there are actual recommendations
        if recommended_techs:
            formatted.append(f"For {technology}, MATILDA recommends:")

            # Sort recommendations by score (highest first)
            sorted_recs = sorted(recommended_techs, key=lambda x: x.get("matildaRecom_mig", 0), reverse=True)

            # Include top N recommendations with scores
            amount_recoms = env_config["amount_matilda_recommendations"]
            for i, rec in enumerate(sorted_recs[:amount_recoms], 1):
                rec_tech = rec.get("technology", "Unknown")
                score = rec.get("matildaRecom_mig", 0)
                category = rec.get("category", "Unknown")
                formatted.append(f"  {i}. {rec_tech} (Category: {category}, Score: {score:.4f})")

            formatted.append("")

    return "\n".join(formatted)


def chat_with_baseline(request: ChatRequest) -> tuple[str, dict]:
    """Generate response from LLM"""
    try:
        # Erstelle Prompt mit zentraler Funktion
        prompt, response_model = create_prompt(message=request.message, prompt_type="baseline", dependencies=request.dependencies)

        llm = get_llm_by_name(LLMs, request.llm)
        if not llm:
            return f"Error: LLM {request.llm} not found in configuration", None

        response, price = query_llm(llm, response_model, prompt)
        logger.info(f"External LLM response received. Cost: {price}")

        # Parse response to structured format
        if isinstance(response, str):
            # Try to parse string response to RecommendationResponse
            parsed_response = parse_llm_json_response(RecommendationResponse, request.llm, response)
            structured_response = parsed_response.model_dump() if parsed_response else None
        elif hasattr(response, "model_dump"):
            structured_response = response.model_dump()
        else:
            structured_response = None

        # Keep original content formatting
        if hasattr(response, "model_dump_json"):
            content = response.model_dump_json(indent=2)
        elif hasattr(response, "__dict__"):
            content = json.dumps(response.__dict__, indent=2, default=str)
        else:
            content = str(response)

        return content, structured_response

    except Exception as e:
        error_msg = f"Error in LLM generation: {str(e)}"
        logger.error(error_msg, exc_info=True)
        return f"Error: {str(e)}", None
