import logging
import threading

import uvicorn
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from recomsystem.config.config import env_config
from recomsystem.config.logging_config import setup_logging
from recomsystem.handler.request_handler import chat_with_baseline, chat_with_matilda
from recomsystem.model.chat_request import ChatRequest
from recomsystem.ui.chat import demo

# Setup logging
logger = setup_logging()
logger = logging.getLogger(__name__)

# Create FastAPI app
app = FastAPI(title="AI Assistant API", description="API for streaming chatbot responses")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/chat/baseline")
async def baseline_chat(request: Request):
    """Endpoint for baseline explanations"""
    try:
        logger.info("Received request for baseline chat")
        data = await request.json()
        chat_request = ChatRequest(**data)

        if not chat_request.message:
            error_msg = "Empty message received"
            logger.warning(error_msg)
            raise HTTPException(status_code=400, detail=error_msg)

        logger.info(f"Processing baseline chat message with {chat_request.llm}: '{chat_request.message[:50]}...'")

        content, structured_response = chat_with_baseline(chat_request)

        response_data = {"content": content, "matilda_recommendations": None, "structured_response": structured_response}

        return JSONResponse(content=response_data)
    except Exception as e:
        error_msg = f"Error in baseline_chat endpoint: {str(e)}"
        logger.error(error_msg, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e)) from e


@app.post("/chat/matilda")
async def matilda_chat(request: Request):
    """Endpoint for explanations powered by MATILDA"""
    try:
        logger.info("Received request for matilda chat")
        data = await request.json()
        chat_request = ChatRequest(**data)

        if not chat_request.message:
            error_msg = "Empty message received"
            logger.warning(error_msg)
            raise HTTPException(status_code=400, detail=error_msg)

        logger.info(f"Processing technical chat message with {chat_request.llm}: '{chat_request.message[:50]}...'")

        # Get MATILDA recommendations with detailed insights
        (
            matilda_recommendations_rel_filtered,
            content_rel_filtered,
            structured_response_rel_filtered,
            detailed_recommendations_rel_filtered,
        ) = chat_with_matilda(chat_request, filter_by_similarity=True, filter_relevant_projects=True)
        matilda_recommendations_filtered, content, structured_response, detailed_recommendations_filtered = chat_with_matilda(
            chat_request, filter_by_similarity=True, filter_relevant_projects=False
        )
        matilda_recommendations_unfiltered, content_unf, structured_response_unf, detailed_recommendations_unfiltered = chat_with_matilda(
            chat_request, filter_by_similarity=False, filter_relevant_projects=False
        )

        response_data = {
            "matilda_recommendations_filtered": matilda_recommendations_filtered,
            "matilda_recommendations_unfiltered": matilda_recommendations_unfiltered,
            "matilda_recommendations_rel_filtered": matilda_recommendations_rel_filtered,
            "content": content,
            "structured_response": structured_response,
            "content_unf": content_unf,
            "structured_response_unf": structured_response_unf,
            "content_rel_filtered": content_rel_filtered,
            "structured_response_rel_filtered": structured_response_rel_filtered,
            # Add detailed recommendations with insights
            "detailed_recommendations_filtered": detailed_recommendations_filtered,
            "detailed_recommendations_unfiltered": detailed_recommendations_unfiltered,
            "detailed_recommendations_rel_filtered": detailed_recommendations_rel_filtered,
        }

        return JSONResponse(content=response_data)
    except Exception as e:
        error_msg = f"Error in matilda endpoint: {str(e)}"
        logger.error(error_msg, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e)) from e


def run_fastapi():
    uvicorn.run(app, host="127.0.0.1", port=env_config["backend_port"], log_level="info")


def run_gradio():
    demo.launch(server_name="127.0.0.1", server_port=env_config["frontend_port"])


if __name__ == "__main__":
    """Main function to run both services"""
    logger.info("Starting MATILDA Recommendation System")
    try:
        # Start FastAPI in a separate thread
        api_thread = threading.Thread(target=run_fastapi)
        api_thread.daemon = True
        api_thread.start()
        run_gradio()
        logger.info("MATILDA Recommendation System launched successfully")
    except Exception as e:
        logger.error(f"Fatal error in main: {str(e)}", exc_info=True)
        raise
