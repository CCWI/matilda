import logging
import os
import sys
from datetime import datetime


def setup_logging(log_level=logging.DEBUG):
    """
    Set up logging configuration for the entire application
    """
    # Create logs directory if it doesn't exist
    log_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "logs")
    os.makedirs(log_dir, exist_ok=True)

    # Create a timestamp for the log file
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    log_file = os.path.join(log_dir, f"matilda_recom_{timestamp}.log")

    # Configure logging
    log_format = "%(asctime)s - %(name)s - %(levelname)s - %(message)s"

    # Configure root logger
    root_logger = logging.getLogger()
    root_logger.setLevel(log_level)

    # Clear existing handlers if any
    if root_logger.handlers:
        for handler in root_logger.handlers:
            root_logger.removeHandler(handler)

    # Add file handler with DEBUG level
    file_handler = logging.FileHandler(log_file)
    file_handler.setLevel(logging.DEBUG)  # <--- Set to DEBUG level
    file_handler.setFormatter(logging.Formatter(log_format))
    root_logger.addHandler(file_handler)

    # Add console handler with DEBUG level
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.DEBUG)  # <--- Set to DEBUG level
    console_handler.setFormatter(logging.Formatter(log_format))
    root_logger.addHandler(console_handler)

    logging.getLogger("recomsystem").setLevel(logging.DEBUG)
    logging.getLogger("urllib3").setLevel(logging.INFO)
    logging.getLogger("requests").setLevel(logging.INFO)
    logging.getLogger("httpcore").setLevel(logging.INFO)
    logging.getLogger("neo4j").setLevel(logging.INFO)

    # Create an application logger for convenience
    logger = logging.getLogger("recomsystem")
    logger.info(f"Logging initialized. Log file: {log_file}")

    return logger
