from recomsystem.handler.prompt_handler import PromptHandler

prompt_handler = PromptHandler()
prompt_uc1 = prompt_handler.get_prompt(prompt_type="recommendation", prompt_name="prompt_uc1")
prompt_uc2 = prompt_handler.get_prompt(prompt_type="recommendation", prompt_name="prompt_uc2")
prompt_uc3 = prompt_handler.get_prompt(prompt_type="recommendation", prompt_name="prompt_uc3")
