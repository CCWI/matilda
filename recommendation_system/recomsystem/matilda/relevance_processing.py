import json
import logging
import os
from pathlib import Path

import pandas as pd

from recomsystem.config.config import env_config
from recomsystem.config.logging_config import setup_logging
from recomsystem.matilda.relevance_classification import classify_text_by_relevance

# Setup logging
logger = logging.getLogger(__name__)


def get_default_json_path():
    """
    Get the default path to the crawledDocumentation.json file.
    Uses the project root directory to construct an absolute path.
    """
    # Get the current file's directory
    current_dir = Path(__file__).parent

    # Navigate to the project root (assuming this file is in recomsystem/matilda/)
    project_root = current_dir.parent.parent

    # Construct path to the JSON file
    json_path = project_root / "data" / "files_for_relevance_check" / "crawledDocumentation.json"

    return str(json_path)


def _find_multiple_entries_by_ids(json_file_path, target_ids, store_entries=False, progress_interval=100000):
    """
    Efficiently finds multiple entries from a JSONL file by their '_id' values.
    Only reads the file once.

    Args:
        json_file_path (str): The path to the JSONL file.
        target_ids (set): Set of '_id' values to search for.
        store_entries (bool): Whether to store full entries or just track found IDs
        progress_interval (int): Show progress every N lines

    Returns:
        dict: Dictionary mapping found IDs to their entries (or True if store_entries=False)
    """
    if not os.path.exists(json_file_path):
        logger.error(f"The file '{json_file_path}' was not found.")
        return {}

    # Get file size for progress estimation
    file_size = os.path.getsize(json_file_path)
    logger.info(f"Processing file: {json_file_path}")
    logger.info(f"File size: {file_size / (1024 * 1024):.1f} MB")

    found_entries = {}
    target_ids_set = set(target_ids)
    lines_processed = 0

    try:
        with open(json_file_path, "r", encoding="utf-8") as f:
            for line_num, line in enumerate(f, 1):
                lines_processed += 1

                # Progress reporting (without f.tell() which causes issues)
                if lines_processed % progress_interval == 0:
                    logger.info(f"Processed {lines_processed:,} lines - Found {len(found_entries)}/{len(target_ids_set)}")

                # Early termination if all found
                if len(found_entries) == len(target_ids_set):
                    logger.info(f"All {len(target_ids_set)} entries found! Stopping at line {lines_processed:,}")
                    break

                line = line.strip()
                if not line:
                    continue

                try:
                    entry = json.loads(line)
                    entry_id = entry.get("_id")
                    if entry_id in target_ids_set:
                        found_entries[entry_id] = entry if store_entries else True
                        if len(found_entries) % 100 == 0:  # Progress for found entries
                            logger.debug(f"Found {len(found_entries)}/{len(target_ids_set)} entries...")
                except json.JSONDecodeError as e:
                    if line_num <= 10:  # Only show first few parse errors
                        logger.warning(f"Could not parse line {line_num}: {e}")
                    continue

    except Exception as e:
        logger.error(f"An unexpected error occurred: {e}")

    logger.info(f"Processed {lines_processed:,} lines total")
    logger.info(f"Found {len(found_entries)} out of {len(target_ids_set)} requested entries")
    return found_entries


def get_project_entry_by_id(project_id, json_file_path=None):
    """
    Retrieves a single project entry by its ID from the crawled documentation.

    Args:
        project_id (str): The project ID to search for
        json_file_path (str): Path to the JSONL file containing crawled documentation.
                             If None, uses default path.

    Returns:
        dict or None: The project entry if found, None otherwise
    """
    if not project_id:
        logger.error("project_id cannot be empty")
        return None

    if json_file_path is None:
        json_file_path = get_default_json_path()

    logger.debug(f"Searching for single project ID: {project_id}")

    # Use the existing function to find the single entry
    found_entries_dict = _find_multiple_entries_by_ids(
        json_file_path,
        [project_id],  # Convert single ID to list
        store_entries=True,
        progress_interval=500000,  # Less frequent progress for single ID
    )

    # Return the entry if found, None otherwise
    return found_entries_dict.get(project_id)


def get_multiple_project_entries_by_ids(project_ids, json_file_path=None):
    """
    Retrieves multiple project entries by their IDs from the crawled documentation.

    Args:
        project_ids (list): List of project IDs to search for
        json_file_path (str): Path to the JSONL file. If None, uses default path.

    Returns:
        pandas.DataFrame: DataFrame containing all found entries
    """
    # Handle different types of inputs (list, numpy array, etc.)
    try:
        if len(project_ids) == 0:
            logger.warning("project_ids list is empty")
            return pd.DataFrame()
    except TypeError:
        # If project_ids doesn't have len(), it might be None or invalid
        if project_ids is None:
            logger.warning("project_ids is None")
            return pd.DataFrame()
        # Try to convert to list if it's a single value
        project_ids = [project_ids]

    # Convert to regular Python list if it's a numpy array
    if hasattr(project_ids, "tolist"):
        project_ids = project_ids.tolist()
    elif not isinstance(project_ids, list):
        project_ids = list(project_ids)

    if json_file_path is None:
        json_file_path = get_default_json_path()

    logger.info(f"Retrieving full entries for {len(project_ids)} project IDs...")

    # Use the existing function but store full entries this time
    found_entries_dict = _find_multiple_entries_by_ids(json_file_path, project_ids, store_entries=True, progress_interval=500000)

    if not found_entries_dict:
        logger.warning("No entries found!")
        return pd.DataFrame()

    # Convert to list of dictionaries for DataFrame creation
    entries_list = list(found_entries_dict.values())

    # Create DataFrame
    df = pd.DataFrame(entries_list)

    logger.info(f"Retrieved {len(df)} entries")
    logger.info(f"DataFrame shape: {df.shape}")

    return df


def export_found_ids_to_csv(found_ids, csv_file_path=None, classification_value=0):
    """
    Exports found project IDs to a CSV file with a classification value.
    Appends to existing file if it exists.

    Args:
        found_ids (list): List of project IDs that were found
        csv_file_path (str): Path to the CSV file. If None, uses default path.
        classification_value (int): Value to assign to found IDs (default: 0)

    Returns:
        str: Path to the CSV file that was written to
    """
    if csv_file_path is None:
        # Get project root and construct default path
        current_dir = Path(__file__).parent
        project_root = current_dir.parent.parent
        csv_file_path = project_root / "data" / "files_for_relevance_check" / "classified.csv"

    csv_file_path = Path(csv_file_path)

    # Create directory if it doesn't exist
    csv_file_path.parent.mkdir(parents=True, exist_ok=True)

    # Create DataFrame with found IDs and classification
    new_data = pd.DataFrame({"project_id": found_ids, "is_professional": [classification_value] * len(found_ids)})

    # Check if file exists
    if csv_file_path.exists():
        # Read existing data
        try:
            existing_data = pd.read_csv(csv_file_path)
            logger.info(f"Found existing CSV file with {len(existing_data)} entries")

            # Remove duplicates - keep existing classifications for IDs that already exist
            existing_ids = set(existing_data["project_id"])
            new_ids_only = new_data[~new_data["project_id"].isin(existing_ids)]

            if len(new_ids_only) > 0:
                # Append only new IDs
                combined_data = pd.concat([existing_data, new_ids_only], ignore_index=True)
                logger.info(f"Adding {len(new_ids_only)} new entries to existing CSV")
            else:
                combined_data = existing_data
                logger.info("No new entries to add - all IDs already exist in CSV")

        except Exception as e:
            logger.error(f"Error reading existing CSV file: {e}")
            logger.info("Creating new file with current data")
            combined_data = new_data
    else:
        # Create new file
        combined_data = new_data
        logger.info(f"Creating new CSV file with {len(new_data)} entries")

    # Write to CSV
    try:
        combined_data.to_csv(csv_file_path, index=False)
        logger.info(f"Successfully exported {len(combined_data)} entries to {csv_file_path}")
        return str(csv_file_path)
    except Exception as e:
        logger.error(f"Error writing to CSV file: {e}")
        raise


def load_already_classified_ids(csv_file_path=None):
    """
    Load already classified project IDs from the CSV file to avoid re-processing.

    Args:
        csv_file_path (str): Path to the CSV file. If None, uses default path.

    Returns:
        dict: Dictionary mapping project_id to classification result (True/False)
              Returns empty dict if file doesn't exist or on error.
    """
    if csv_file_path is None:
        # Get project root and construct default path
        current_dir = Path(__file__).parent
        project_root = current_dir.parent.parent
        csv_file_path = project_root / "data" / "files_for_relevance_check" / "classified.csv"

    csv_file_path = Path(csv_file_path)

    if not csv_file_path.exists():
        logger.info(f"No existing classified CSV file found at {csv_file_path}")
        return {}

    try:
        existing_data = pd.read_csv(csv_file_path)

        if existing_data.empty:
            logger.info("Classified CSV file is empty")
            return {}

        # Check required columns exist
        required_columns = ["project_id", "is_professional"]
        missing_columns = [col for col in required_columns if col not in existing_data.columns]
        if missing_columns:
            logger.warning(f"Missing required columns in classified CSV: {missing_columns}")
            return {}

        # Create mapping from project_id to boolean classification
        classified_dict = {}
        for _, row in existing_data.iterrows():
            project_id = row["project_id"]
            is_professional_value = row["is_professional"]

            # Convert to boolean (handle different formats: 0/1, True/False, "true"/"false")
            if isinstance(is_professional_value, (int, float)):
                is_professional = bool(is_professional_value)
            elif isinstance(is_professional_value, str):
                is_professional = is_professional_value.lower() in ["true", "1", "yes"]
            else:
                is_professional = bool(is_professional_value)

            classified_dict[str(project_id)] = is_professional

        logger.info(f"Loaded {len(classified_dict)} already classified project IDs from CSV")
        return classified_dict

    except Exception as e:
        logger.error(f"Error loading classified IDs from CSV: {e}")
        return {}


def find_and_classify_project_documentation_by_id(project_id, json_file_path=None):
    """
    Finds a project entry by ID, classifies it as professional software, and exports the result.
    Checks if the project was already classified to avoid re-processing.

    Process:
    0. Check if project was already classified (skip if found)
    1. Search for project entry in JSON file
    2. Log the found entry
    3. Extract "documentationFileList" field for classification
    4. Classify using LLM (returns boolean)
    5. Export project_id and classification result (1/0) to CSV

    Args:
        project_id (str): The project ID to search for
        json_file_path (str): Path to the JSONL file. If None, uses default path.

    Returns:
        bool or None: Classification result (True=Professional, False=Exploratory, None=Error/NotFound)
    """
    # Step 0: Check if already classified
    already_classified = load_already_classified_ids()
    project_id_str = str(project_id)

    if project_id_str in already_classified:
        cached_result = already_classified[project_id_str]
        logger.info(f"Using cached classification for {project_id}: {'Professional' if cached_result else 'Exploratory'}")
        return cached_result

    # Step 1: Find the project entry
    entry = get_project_entry_by_id(project_id, json_file_path=json_file_path)
    if not entry:
        logger.warning(f"No entry found for project ID {project_id}")
        return None

    # Step 2: Log the found entry
    logger.info(f"Found entry for project {project_id}")
    logger.info(f"Entry keys: {list(entry.keys())}")
    if "title" in entry:
        logger.info(f"Title: {entry.get('title', 'N/A')}")
    if "url" in entry:
        logger.info(f"URL: {entry.get('url', 'N/A')}")

    # Step 3: Extract documentationFileList field for classification
    documentation_content = entry.get("documentationFileList", "")
    logger.info(f"Extracted documentationFileList for project {project_id} (length: {len(str(documentation_content))} chars)")

    # Step 4: Classify using LLM or set to False if not found
    if not documentation_content or len(documentation_content) < 10:
        logger.warning(f"No 'documentationFileList' field found for project {project_id}")
        is_professional = False
    else:
        logger.info(f"Found documentationFileList for project {project_id} (length: {len(str(documentation_content))} chars)")
        try:
            is_professional = classify_text_by_relevance(text_to_classify=str(documentation_content))
            logger.info(f"Classification result for {project_id}: {'Professional' if is_professional else 'Exploratory'}")
        except Exception as e:
            logger.error(f"Error during classification for project {project_id}: {e}")
            is_professional = None

    # Step 5: Export to CSV
    if is_professional is not None:
        try:
            classification_value = 1 if is_professional else 0
            csv_path = export_found_ids_to_csv([project_id], classification_value=classification_value)
            logger.info(f"Exported project {project_id} to {csv_path} with classification {classification_value}")
        except Exception as e:
            logger.error(f"Failed to export project {project_id}: {e}")

    return is_professional


def find_and_classify_project_documentation_by_dataframe(df, json_file_path=None) -> pd.DataFrame:
    """
    Finds and classifies project documentation based on a DataFrame input.

    Args:
        df (pd.DataFrame): DataFrame containing project IDs and other relevant information
        json_file_path (str): Path to the JSONL file. If None, uses default path.

    Returns:
        dict: Summary of classification results
    """
    logger.info(f"RELEVANCE: Starting dataframe classification for {len(df)} projects")
    result = find_and_classify_project_documentation_by_id_list(df["similar_project_id"].tolist(), json_file_path=json_file_path)

    # df["is_professional"] = df["similar_project_id"].apply(lambda x: result["results"].get(str(x), None))
    df["is_professional"] = df["similar_project_id"].map(lambda x: result["results"].get(str(x), None))

    return df


def find_and_classify_project_documentation_by_id_list(project_ids, json_file_path=None) -> dict:
    """
    Efficiently finds and classifies multiple project entries in one pass.
    Searches the JSON file only once for all project IDs.

    Process:
    1. Search JSON file once for all project IDs
    2. For each found project: log entry, extract documentationFileList, classify, export
    3. Return summary of results

    Args:
        project_ids (list): List of project IDs to search for and classify
        json_file_path (str): Path to the JSONL file. If None, uses default path.

    Returns:
        dict: Results dictionary with statistics and individual results
              {
                  'total_requested': int,
                  'total_found': int,
                  'total_classified': int,
                  'professional_count': int,
                  'exploratory_count': int,
                  'error_count': int,
                  'results': {project_id: classification_result, ...}
              }
    """
    logger.info(f"RELEVANCE: Starting batch classification for {len(project_ids)} projects")

    if project_ids is None:
        logger.warning("project_ids is None")
        return {
            "total_requested": 0,
            "total_found": 0,
            "total_classified": 0,
            "professional_count": 0,
            "exploratory_count": 0,
            "error_count": 0,
            "results": {},
        }

    # Convert numpy array to list if needed
    if hasattr(project_ids, "tolist"):
        project_ids = project_ids.tolist()
    elif not isinstance(project_ids, (list, tuple)):
        # Try to convert to list
        try:
            project_ids = list(project_ids)
        except TypeError:
            logger.error(f"Invalid project_ids type: {type(project_ids)}")
            return {
                "total_requested": 0,
                "total_found": 0,
                "total_classified": 0,
                "professional_count": 0,
                "exploratory_count": 0,
                "error_count": 0,
                "results": {},
            }

    # Check if empty
    if len(project_ids) == 0:
        logger.warning("No project IDs provided")
        return {
            "total_requested": 0,
            "total_found": 0,
            "total_classified": 0,
            "professional_count": 0,
            "exploratory_count": 0,
            "error_count": 0,
            "results": {},
        }

    # Step 0: Load already classified IDs to avoid re-processing
    already_classified = load_already_classified_ids()

    # Separate already classified from new IDs
    already_classified_ids = []
    new_ids_to_process = []

    for project_id in project_ids:
        project_id_str = str(project_id)
        if project_id_str in already_classified:
            already_classified_ids.append(project_id_str)
        else:
            new_ids_to_process.append(project_id_str)

    logger.info(f"RELEVANCE: Found {len(already_classified_ids)} already classified projects")
    logger.info(f"RELEVANCE: Need to process {len(new_ids_to_process)} new projects")

    # Initialize results with already classified projects
    results = {}
    professional_count = 0
    exploratory_count = 0
    error_count = 0

    # Add already classified results
    for project_id in already_classified_ids:
        classification = already_classified[project_id]
        results[project_id] = classification
        if classification:
            professional_count += 1
        else:
            exploratory_count += 1
        logger.debug(f"RELEVANCE: Using cached classification for {project_id}: {'Professional' if classification else 'Exploratory'}")

    # Step 1: Find entries for new IDs only (if any)
    if new_ids_to_process:
        found_entries_dict = _find_multiple_entries_by_ids(
            json_file_path or get_default_json_path(), new_ids_to_process, store_entries=True, progress_interval=500000
        )
    else:
        found_entries_dict = {}
        logger.info("No new IDs to process - all were already classified")

    classification_values_to_export = []
    project_ids_to_export = []

    # Step 2: Process each new entry that needs classification
    for project_id in new_ids_to_process:
        entry = found_entries_dict.get(project_id)

        if not entry:
            logger.warning(f"No entry found for project ID {project_id}")
            results[project_id] = None
            error_count += 1
            continue

        # Log the found entry
        logger.info(f"Processing project {project_id}")
        logger.debug(f"Entry keys: {list(entry.keys())}")

        # Extract documentationFileList field for classification
        documentation_content = entry.get("documentationFileList", "")
        logger.info(f"Extracted documentationFileList for project {project_id} (length: {len(str(documentation_content))} chars)")

        if not documentation_content:
            logger.warning(f"No 'documentationFileList' field found for project {project_id}")
            is_professional = False
            exploratory_count += 1
        else:
            logger.debug(f"Found documentationFileList for project {project_id} (length: {len(str(documentation_content))} chars)")

            # Classify using LLM
            try:
                is_professional = classify_text_by_relevance(text_to_classify=str(documentation_content))
                logger.info(f"Classification result for {project_id}: {'Professional' if is_professional else 'Exploratory'}")

                if is_professional:
                    professional_count += 1
                else:
                    exploratory_count += 1

            except Exception as e:
                logger.error(f"Error during classification for project {project_id}: {e}")
                is_professional = None
                error_count += 1

        results[project_id] = is_professional

        # Collect for batch export
        if is_professional is not None:
            project_ids_to_export.append(project_id)
            classification_values_to_export.append(1 if is_professional else 0)

    # Step 3: Batch export to CSV (only new classifications)
    if project_ids_to_export:
        try:
            # Create batch export data
            export_data = []
            for pid, classification in zip(project_ids_to_export, classification_values_to_export):
                export_data.append({"project_id": pid, "classification_value": classification})

            # Export in batches to avoid issues with large datasets
            batch_size = 100
            for i in range(0, len(export_data), batch_size):
                batch = export_data[i : i + batch_size]
                batch_project_ids = [item["project_id"] for item in batch]
                batch_classifications = [item["classification_value"] for item in batch]

                # Export each classification value separately (since export function takes single classification)
                for pid, classification in zip(batch_project_ids, batch_classifications):
                    export_found_ids_to_csv([pid], classification_value=classification)

            logger.info(f"Exported {len(project_ids_to_export)} newly classified projects to CSV")

        except Exception as e:
            logger.error(f"Failed to export batch results: {e}")

    # Calculate detailed statistics
    total_requested = len(project_ids)
    already_classified_count = len(already_classified_ids)
    newly_processed_count = len(new_ids_to_process)
    newly_found_count = len(found_entries_dict)
    newly_classified_count = len(project_ids_to_export)
    new_errors_count = newly_processed_count - newly_classified_count

    # Count classifications from already classified
    already_professional_count = sum(1 for pid in already_classified_ids if already_classified[pid])
    already_exploratory_count = already_classified_count - already_professional_count

    # Count classifications from newly processed
    new_professional_count = professional_count - already_professional_count
    new_exploratory_count = exploratory_count - already_exploratory_count

    # Step 4: Return summary with detailed breakdown
    summary = {
        "total_requested": total_requested,
        "already_classified_count": already_classified_count,
        "newly_processed_count": newly_processed_count,
        "newly_found_count": newly_found_count,
        "newly_classified_count": newly_classified_count,
        "total_classified": len([r for r in results.values() if r is not None]),
        "professional_count": professional_count,
        "exploratory_count": exploratory_count,
        "error_count": error_count + new_errors_count,
        "results": results,
    }

    logger.info("Batch classification completed:")
    logger.info(f"  - Total requested: {summary['total_requested']} projects")
    logger.info(f"  - Already classified (cached): {summary['already_classified_count']} projects")
    logger.info(f"    └─ Professional: {already_professional_count}, Exploratory: {already_exploratory_count}")
    logger.info(f"  - Newly processed: {summary['newly_processed_count']} projects")
    logger.info(f"    └─ Found in JSON: {summary['newly_found_count']} projects")
    logger.info(f"    └─ Successfully classified: {summary['newly_classified_count']} projects")
    logger.info(f"    └─ Professional: {new_professional_count}, Exploratory: {new_exploratory_count}")
    logger.info(f"    └─ Errors: {new_errors_count} projects")
    logger.info(
        f"  - Final totals: Professional: {summary['professional_count']}, Exploratory: {summary['exploratory_count']}, Errors: {summary['error_count']}"
    )

    return summary


if __name__ == "__main__":
    # Setup logging
    setup_logging(level=logging.DEBUG)
    logger.info("Starting relevance processing example")

    # Test the path resolution
    default_path = get_default_json_path()
    logger.info(f"Default JSON path: {default_path}")
    logger.info(f"File exists: {os.path.exists(default_path)}")

    # Configuration switches for examples
    run_example_1 = False  # Single project entry (without classification)
    run_example_2 = False  # Single project classification
    run_example_3 = False  # Multiple projects individually
    run_example_4 = False  # Batch classification (efficient method)

    # Load some real project IDs from migrations export for testing
    try:
        migrations_path = Path(__file__).parent.parent.parent / "data" / "files_for_relevance_check" / "migrations-export.csv"
        migrations_df = pd.read_csv(migrations_path)
        sample_ids = migrations_df["similar_project_id"].unique()  # [:2000]
        logger.info(f"Loaded {len(sample_ids)} sample project IDs from migrations")
    except Exception as e:
        logger.error(f"Could not load sample IDs: {e}")
        sample_ids = ["example_project_id"]

    # Example 1: Get a single project entry (without classification)
    if run_example_1:
        logger.info("=== Example 1: Single project entry ===")
        project_id = sample_ids[0] if len(sample_ids) > 0 else "example_project_id"
        entry = get_project_entry_by_id(project_id)
        if entry:
            logger.info(f"Found entry for {project_id}")
            logger.info(f"Title: {entry.get('title', 'N/A')}")
            logger.info(f"URL: {entry.get('url', 'N/A')}")
        else:
            logger.warning(f"No entry found for {project_id}")

    # Example 2: Find and classify project documentation (main function)
    if run_example_2:
        logger.info("=== Example 2: Find and classify project documentation ===")
        if len(sample_ids) > 0:
            test_project_id = sample_ids[0]
            try:
                classification_result = find_and_classify_project_documentation_by_id(test_project_id)
                if classification_result is not None:
                    logger.info(f"Project {test_project_id} classified as: {'Professional' if classification_result else 'Exploratory'}")
                else:
                    logger.warning(f"Project {test_project_id} could not be classified")
            except Exception as e:
                logger.error(f"Error in classification example: {e}")

    # Example 3: Process multiple projects individually
    if run_example_3:
        logger.info("=== Example 3: Process multiple projects individually ===")
        if len(sample_ids) >= 2:
            for project_id in sample_ids:
                logger.info(f"Processing project {project_id}")
                try:
                    result = find_and_classify_project_documentation_by_id(project_id)
                    logger.info(f"Project {project_id} result: {result}")
                except Exception as e:
                    logger.error(f"Error processing project {project_id}: {e}")

    # Example 4: Batch classification of multiple projects
    if run_example_4:
        logger.info("=== Example 4: Batch classification of multiple projects ===")
        if len(sample_ids) > 0:
            try:
                batch_result_summary = find_and_classify_project_documentation_by_id_list(sample_ids)
                logger.info(f"Batch classification results: {batch_result_summary}")
            except Exception as e:
                logger.error(f"Error in batch classification example: {e}")

    logger.info("All examples completed")
