from typing import List, Optional

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    message: str = Field(description="Die Benutzeranfrage")
    llm: str = Field(description="Der zu verwendende LLM")
    dependencies: Optional[List[str]] = Field(default=None, description="Liste von Abhängigkeiten")
    # use_case: str | None = Field(description="Der Anwendungsfall ('uc1', 'uc2', 'uc3')")
