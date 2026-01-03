from pydantic import BaseModel, Field


class Classification_model(BaseModel):
    is_professional_software: bool = Field(
        description="True if the given text describes professional software, like Internal Corporate Projects, Commercial/Revenue-Generating Projects, larger Open-Source Projects, Research Projects, Commissioned/Contract-Based Projects, Enterprise-Grade Projects, otherwise False. If there is no clear indication, please mark as False. Decide based on your reasoning and return 'True' or 'False'!",
    )
