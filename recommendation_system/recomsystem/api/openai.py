import logging
import os
from typing import Tuple

from openai import OpenAI

from recomsystem.api.api_shared import calculate_price

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
    """Queries OpenAI models via direct API.

    Args:
        system_prompt: Initial system context
        prompt: Main query content
        model_name: OpenAI model identifier
        input_price: Cost per input token
        output_price: Cost per output token
        max_tokens: Maximum response length
        timeout: Query timeout in seconds
        **kwargs: Additional parameters (optional)

    Returns:
        Tuple[str, float]: Model response and total cost

    Raises:
        ValueError: If OpenAI API key is missing
        openai.APIError: For API-related errors
    """
    messages = []
    if system_prompt is not None:
        messages.append({"role": "system", "content": system_prompt})
    messages.append({"role": "user", "content": prompt})

    logger.info("Sending message to OpenAI")
    msg_str = " ".join(str(x) for x in messages)
    logger.debug(f"System prompt / Prompt: {msg_str[:500]}")

    response = OpenAI(api_key=os.getenv("OPENAI_API_KEY")).chat.completions.create(
        model=model_name,
        messages=messages,
        max_tokens=max_tokens,
        temperature=0.0,
        timeout=timeout,
    )

    logger.info("Received response from OpenAI")
    logger.debug(f"Response: {response}")

    response_price = calculate_price(
        int(response.usage.prompt_tokens),
        int(response.usage.completion_tokens),
        input_price,
        output_price,
    )

    logger.debug(f"Calculated price from calling OpenAI: {response_price}")

    return response.choices[0].message.content, response_price
