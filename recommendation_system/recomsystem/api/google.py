import logging

from langchain_core.messages import (
    HumanMessage,
    SystemMessage,
)
from langchain_google_vertexai import ChatVertexAI

from recomsystem.api.api_shared import calculate_price, get_safety_settings

logger = logging.getLogger("__name__")


def query(
    system_prompt: str,
    prompt: list[dict[str, str]],
    model_name: str,
    input_price: float = 0.0,
    output_price: float = 0.0,
    max_tokens: int = 8192,
    **kwargs,
) -> tuple[str, float]:
    """Queries Google's Gemini model via Vertex AI.

    Args:
        system_prompt: Initial system context
        prompt: Main query content
        model_name: Gemini model identifier
        input_price: Cost per input token
        output_price: Cost per output token
        max_tokens: Maximum response length
        timeout: Query timeout in seconds
        **kwargs: Additional kwargs (gcp_project, gcp_region required)

    Returns:
        Tuple[str, float]: Model response and total cost

    Raises:
        ValueError: If required GCP parameters are missing
    """
    logger.info("Sending messages to Gemini via Vertex model garden")

    gcp_project = kwargs.get("gcp_project", None)
    gcp_region = kwargs.get("gcp_region", None)

    messages = []
    if system_prompt is not None:
        messages.append(SystemMessage(content=system_prompt))
    messages.append(HumanMessage(content=prompt))

    response = ChatVertexAI(
        model_name=model_name,
        project=gcp_project,
        location=gcp_region,
        max_tokens=max_tokens,
        safety_settings=get_safety_settings(),
        temperature=0.0,
    ).invoke(messages)

    price = calculate_price(
        response.usage_metadata["input_tokens"],
        response.usage_metadata["output_tokens"],
        input_price,
        output_price,
    )

    resp_str = str(response.content)
    if len(resp_str) > 100:
        resp_str = resp_str[:100] + "..."

    # logger.debug(f"Received response from Gemini: \n{resp_str} \n -- Calculated price: {price} --")

    return response.content, price
