import logging

from langchain_core.messages import (
    HumanMessage,
    SystemMessage,
)
from langchain_google_vertexai.model_garden_maas.mistral import VertexModelGardenMistral

from recomsystem.api.api_shared import calculate_price

logger = logging.getLogger(__name__)


def query(
    system_prompt: str,
    prompt: list[dict[str, str]],
    model_name: str,
    input_price: float = 0.0,
    output_price: float = 0.0,
    max_tokens: int = 8192,
    timeout: int = 240,
    **kwargs,
) -> tuple[str, float]:
    """Queries Mistral model via Vertex AI.

    Args:
        system_prompt: Initial system context
        prompt: Main query content
        model_name: Mistral model identifier
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
    logger.info("-- Sending messages to Mistral via Vertex model garden: --")

    gcp_project = kwargs.get("gcp_project", None)
    gcp_region = kwargs.get("gcp_region", None)

    if not gcp_project or not gcp_region:
        raise ValueError("GCP project and region must be provided for Vertex AI integration")

    messages = []
    if system_prompt is not None:
        messages.append(SystemMessage(content=system_prompt))
    messages.append(HumanMessage(content=prompt))

    response = VertexModelGardenMistral(
        model_name=model_name,
        project=gcp_project,
        location=gcp_region,
        max_tokens=max_tokens,
        timeout=timeout,
        temperature=0.0,
        verbose=False,
    ).invoke(messages)

    # Try to access token usage from different possible response formats
    input_tokens = 0
    output_tokens = 0

    if hasattr(response, "usage_metadata") and response.usage_metadata:
        input_tokens = response.usage_metadata.get("input_tokens", 0)
        output_tokens = response.usage_metadata.get("output_tokens", 0)
    elif "token_usage" in response.response_metadata:
        input_tokens = response.response_metadata["token_usage"].get("prompt_tokens", 0)
        output_tokens = response.response_metadata["token_usage"].get("completion_tokens", 0)
    elif "usage" in response.response_metadata:
        input_tokens = response.response_metadata["usage"].get("input_tokens", 0)
        output_tokens = response.response_metadata["usage"].get("output_tokens", 0)
    else:
        logger.warning("Could not find token usage information in response metadata")

    price = calculate_price(
        input_tokens,
        output_tokens,
        input_price,
        output_price,
    )

    logger.debug(f"Received response from Mistral: \n{response} \n -- Calculated price: {price} --")

    return response.content, price
