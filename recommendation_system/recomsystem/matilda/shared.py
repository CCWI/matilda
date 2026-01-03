import ast
import json
import logging
import os
import re

import pandas as pd
from pandas import Timestamp

logger = logging.getLogger(__name__)


def check_and_create_dir(destination_file_path: str):
    if not os.path.exists(destination_file_path):
        os.makedirs(destination_file_path)
        logger.info(f"Created folder: '{destination_file_path}'")


def safe_load_json(data_string: str) -> dict:
    if pd.isnull(data_string) or data_string == "" or data_string == "nan" or data_string == "NaN" or data_string == "None":
        return {}
    try:
        # Add missing commas between JSON objects
        fixed_string = re.sub(r"}\s*{", "},{", str(data_string))

        # Step 1: Replace Python Timestamp with ISO format date strings
        # Replace each match with its corresponding ISOformatted date string
        for match in re.findall(r"Timestamp\('(.*?)'\)", fixed_string):
            iso_formatted_date = Timestamp(match).isoformat()
            fixed_string = fixed_string.replace(f"Timestamp('{match}')", f'"{iso_formatted_date}"')

        # Replace numpy array representation with a valid list format
        correct_string = " ".join(fixed_string.split())  # Remove new lines and unnecessary whitespace
        correct_string = correct_string.replace(", dtype=object)", "").replace(", dtype=float64)", "").replace("array(", "")
        correct_string = ast.literal_eval(correct_string)
        correct_string = json.dumps(correct_string)

        return json.loads(correct_string)
    except (json.JSONDecodeError, ValueError, SyntaxError, Exception):
        pass  # Ignore the error and start second attempt by calling safe_load_json_v2

    return safe_load_json_v2(data_string)


def safe_load_json_v2(data_string: str) -> dict:
    if pd.isnull(data_string) or data_string == "" or data_string.lower() in {"nan", "none"}:
        return {}

    try:
        # Remove unnecessary spaces and newlines
        data_string = re.sub(r"\s+", " ", data_string).strip()

        # Step 1: Replace Python Timestamp with ISO format date strings
        data_string = re.sub(r"Timestamp\('([^']*)'\)", lambda m: f'"{Timestamp(m.group(1)).isoformat()}"', data_string)

        # Step 2: Handle numpy array representations
        data_string = re.sub(r"array\((\[.*?\])(?:, dtype=\w+)?\)", lambda m: m.group(1), data_string)

        # Step 3: Handle leading zeros in integer literals
        data_string = re.sub(r"\b0+(\d+)\b", lambda m: str(int(m.group(0))), data_string)

        # Validate the JSON string by attempting to load it
        json_data = json.loads(data_string)

        return json_data
    except (json.JSONDecodeError, ValueError, SyntaxError) as e:
        logger.error(f"JSON parsing error: {e}", exc_info=True)
        logger.debug(f"Problematic data: {data_string}")
    except Exception as exc:
        logger.error(f"Unexpected error in JSON parsing: {exc}", exc_info=True)
        logger.debug(f"Problematic data: {data_string}")

    return {}
