from typing import List

from langchain.output_parsers import PydanticOutputParser
from langchain.prompts import PromptTemplate
from pydantic import BaseModel, Field

from recomsystem.handler.prompt_handler import PromptHandler


class TechnologiesExtraction(BaseModel):
    """Modell zur Extraktion von Technologienamen aus einem Benutzertext."""

    technologies: List[str] = Field(
        description="List of technologies, which were extracted from the user text",
    )


def get_tech_extraction_prompt_template() -> tuple[PromptTemplate, PydanticOutputParser]:
    """Gibt ein PromptTemplate zurück, das den Parser für Technologieextraktion verwendet."""
    parser = PydanticOutputParser(pydantic_object=TechnologiesExtraction)
    format_instructions = parser.get_format_instructions()

    prompt_template = PromptHandler().get_prompt(prompt_type="extraction", prompt_name="relevant_technologies")

    prompt_template = PromptTemplate(
        template=prompt_template,
        input_variables=["query"],
        partial_variables={"format_instructions": format_instructions},
    )

    return prompt_template, parser
