# Define response model for non-local LLMs
from typing import Optional

from langchain.output_parsers import PydanticOutputParser
from langchain.prompts import PromptTemplate
from pydantic import BaseModel, Field


class LLMResponse(BaseModel):
    content: str


class Recommendation(BaseModel):
    rank: int = Field(description="Rank of the recommendation, where 1 is the best")
    technology: str = Field(description="Name of the recommended technology")


class RecommendationDetails(BaseModel):
    rank: int = Field(description="Rank of the recommendation, where 1 is the best")
    technology: str = Field(description="Name of the recommended technology")
    description: str = Field(description="Description of the technology")
    reason: str = Field(description="Reason for the recommendation")
    advantages: str = Field(description="Advantages of the technology")
    tradeoffs: str = Field(description="Trade-offs of the technology")


class Recommendations(BaseModel):
    """Recommendations for a specific technology"""

    old_technology: str = Field(description="Name of the old technology")
    category: str = Field(description="Category of the technology")
    recommendation_overview: list[Recommendation] = Field(description="List of recommended technologies")
    recommendation_details: Optional[list[RecommendationDetails]] = Field(description="Details of the recommendations")


class RecommendationResponse(BaseModel):
    """Structured answer for all scenarios - from MATILDA und Baseline"""

    recommendation_list: list[Recommendations] = Field(description="List of recommendations for different technologies")


def get_structured_prompt_template() -> tuple[PromptTemplate, PydanticOutputParser]:
    """Gibt ein PromptTemplate zurück, das den Parser für strukturierte Ausgaben verwendet."""
    parser = PydanticOutputParser(pydantic_object=RecommendationResponse)
    format_instructions = parser.get_format_instructions()

    prompt_template = PromptTemplate(
        template="Beantworte die folgende Frage und gib Empfehlungen zu relevanten Technologien:\n{query}\n\n{format_instructions}",
        input_variables=["query"],
        partial_variables={"format_instructions": format_instructions},
    )

    return prompt_template, parser
