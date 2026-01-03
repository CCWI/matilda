import logging
from typing import Tuple

from langchain_core.messages import (
    HumanMessage,
    SystemMessage,
)
from langchain_google_vertexai.model_garden import ChatAnthropicVertex

from recomsystem.api.api_shared import calculate_price, get_safety_settings

logger = logging.getLogger(__name__)


def query(
    system_prompt: str,
    prompt: str,
    model_name: str,
    input_price: float = 0.0,
    output_price: float = 0.0,
    max_tokens: int = 8192,
    timeout: int = 240,
    **kwargs,
) -> Tuple[str, float]:
    """Queries Anthropic Claude model either directly or via Vertex AI.

    Automatically chooses between direct Anthropic API or Vertex AI based on
    provided GCP credentials.

    Args:
        system_prompt: Initial context for the model
        prompt: Main query content
        model_name: Anthropic model identifier
        input_price: Cost per input token
        output_price: Cost per output token
        max_tokens: Maximum response length
        timeout: Query timeout in seconds
        **kwargs: Additional parameters (gcp_project, gcp_region)

    Returns:
        Tuple containing:
            - str: Model's response text
            - float: Total cost of the query

    Example:
        >>> response, cost = query(
        ...     "You are a helpful AI",
        ...     "What is 2+2?",
        ...     "claude-3",
        ...     gcp_project="my-project"
        ... )
    """
    response = None

    gcp_project = kwargs.get("gcp_project", None)
    gcp_region = kwargs.get("gcp_region", None)

    return query_claude_model_at_vertex_as_batch(
        gcp_project, gcp_region, system_prompt, prompt, model_name, input_price, output_price, max_tokens, timeout
    )


def query_claude_model_at_vertex_as_batch(
    gcp_project_id: str,
    gcp_region: str,
    system_prompt: str,
    prompt: str,
    model_name: str,
    input_price: float = 0.0,
    output_price: float = 0.0,
    max_tokens: int = 8192,
    timeout: int = 240,
) -> Tuple[str, float]:
    logger.info("Sending messages to Anthropic Claude via VertexAI service:")
    logger.debug(f"System prompt: {system_prompt[:200]}")
    prompt_str = "".join(str(x) for x in prompt)
    logger.debug(f"Prompt: {prompt_str[:500]}")

    messages = []
    if system_prompt is not None:
        messages.append(SystemMessage(content=system_prompt))
    messages.append(HumanMessage(content=prompt))

    response = ChatAnthropicVertex(
        model_name=model_name,
        project=gcp_project_id,
        location=gcp_region,
        max_output_tokens=max_tokens,
        safety_settings=get_safety_settings(),
        timeout=timeout,
        temperature=0.0,
        verbose=True,
    ).invoke(messages)

    logger.debug("Received response from Anthropic Claude")
    logger.debug(f"Raw response content: {response.content}")
    logger.debug(f"Response metadata: {response.response_metadata}")

    price = calculate_price(
        response.response_metadata["usage"]["input_tokens"],
        response.response_metadata["usage"]["output_tokens"],
        input_price,
        output_price,
    )

    logger.debug(f"Calculated price from calling Anthropic Claude: {price}")

    return response.content, price
