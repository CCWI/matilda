import unittest
from unittest.mock import MagicMock, Mock, patch

import pandas as pd

from recomsystem.config.processing_config import ProcessingConfig
from recomsystem.processing.matilda import RecommendationProcessor


class TestRecommendationProcessorNoFilter(unittest.TestCase):
    """Test cases for RecommendationProcessor with similarity filtering disabled"""

    def setUp(self):
        """Set up test fixtures"""
        # Mock the configuration
        self.mock_config = Mock(spec=ProcessingConfig)
        self.mock_config.LIBS_RESULT_PATH = "mock_libs.csv"
        self.mock_config.DESIGN_DECISIONS_PATH = "mock_decisions.csv"
        self.mock_config.CATEGORY_TECH_MAP_PATH = "mock_category.csv"
        self.mock_config.NEO4J_URI = "mock_uri"
        self.mock_config.NEO4J_USER = "mock_user"
        self.mock_config.NEO4J_PASSWORD = "mock_password"

        # Mock DataFrames
        self.mock_libs_df = pd.DataFrame({"GA": ["com.test:library1", "com.test:library2"], "result": ["Technology1", "Technology2"]})

        self.mock_decisions_df = pd.DataFrame(
            {
                "decision_commit_id": ["commit1", "commit2", "commit3"],
                "initial_commit_id": ["initial1", "initial2", "initial3"],
                "decision_commit_time": ["2023-01-01 10:00:00", "2023-01-02 10:00:00", "2023-01-03 10:00:00"],
                "decision_subject": ["Category1", "Category1", "Category2"],
                "initial": ["com.test:library1", "com.test:library2", "com.test:library3"],
                "target": ["com.test:library2", "com.test:library3", "com.test:library4"],
            }
        )

        self.mock_category_tech_map_df = pd.DataFrame(
            {
                "result": ["Technology1", "Technology2", "Technology3"],
                "MATILDA_CATEGORY": ["Category1", "Category1", "Category2"],
                "strength_based_centrality": [0.8, 0.6, 0.7],
            }
        )

        # Mock all projects response (should be larger than similar projects)
        self.mock_all_projects = pd.DataFrame(
            {
                "similar_project_id": ["commit1", "commit2", "commit3", "commit4", "commit5"],
                "similar_project_name": ["Project1", "Project2", "Project3", "Project4", "Project5"],
                "tech_similarity": [1.0, 1.0, 1.0, 1.0, 1.0],  # All set to 1.0 for non-filtered
                "category_similarity": [1.0, 1.0, 1.0, 1.0, 1.0],  # All set to 1.0 for non-filtered
                "custom_similarity": [1.0, 1.0, 1.0, 1.0, 1.0],  # All set to 1.0 for non-filtered
            }
        )

    @patch("recomsystem.processing.matilda.pd.read_csv")
    @patch("recomsystem.processing.matilda.GraphDatabase.driver")
    def test_process_by_technologies_without_similarity_filtering(self, mock_driver, mock_read_csv):
        """Test process_by_technologies with similarity filtering disabled"""
        # Setup mocks
        mock_read_csv.side_effect = [self.mock_libs_df, self.mock_decisions_df, self.mock_category_tech_map_df]

        # Mock Neo4j session and results
        mock_session = MagicMock()
        mock_driver_instance = MagicMock()
        mock_driver.return_value = mock_driver_instance
        mock_driver_instance.session.return_value.__enter__.return_value = mock_session

        # Mock the all projects query result
        mock_session.run.return_value = [
            {"project_id": "commit1", "project_name": "Project1"},
            {"project_id": "commit2", "project_name": "Project2"},
            {"project_id": "commit3", "project_name": "Project3"},
        ]

        # Create processor instance
        processor = RecommendationProcessor(self.mock_config)

        # Test with similarity filtering disabled
        technologies = ["Technology1"]
        result = processor.process_by_technologies(technologies, filter_similar_projects=False)

        # Assertions
        self.assertIsInstance(result, list)
        mock_driver.assert_called_with(self.mock_config.NEO4J_URI, auth=(self.mock_config.NEO4J_USER, self.mock_config.NEO4J_PASSWORD))

        # Verify that all projects query was called (no temp node operations)
        self.assertTrue(mock_session.run.called)
        call_args_list = [call[0][0] for call in mock_session.run.call_args_list]

        # Check that no similarity-related queries (with TEMP node) were executed
        temp_queries = [query for query in call_args_list if "TEMP" in query]
        self.assertEqual(len(temp_queries), 0, "No TEMP node queries should be executed when filtering is disabled")

        # Check that all projects query was executed
        all_projects_queries = [query for query in call_args_list if "ProjectRevision" in query and "TEMP" not in query]
        self.assertGreater(len(all_projects_queries), 0, "All projects query should be executed")

    @patch("recomsystem.processing.matilda.pd.read_csv")
    @patch("recomsystem.processing.matilda.GraphDatabase.driver")
    def test_all_projects_query_structure(self, mock_driver, mock_read_csv):
        """Test that the all projects query has the correct structure"""
        # Setup mocks
        mock_read_csv.side_effect = [self.mock_libs_df, self.mock_decisions_df, self.mock_category_tech_map_df]

        mock_session = MagicMock()
        mock_driver_instance = MagicMock()
        mock_driver.return_value = mock_driver_instance
        mock_driver_instance.session.return_value.__enter__.return_value = mock_session
        mock_session.run.return_value = []

        processor = RecommendationProcessor(self.mock_config)

        # Test the Neo4jManager directly
        neo4j_manager = processor.neo4j_manager
        result = neo4j_manager.get_all_projects(limit=100)

        # Check that the query was called
        self.assertTrue(mock_session.run.called)

        # Get the query that was executed
        query_call = mock_session.run.call_args_list[0]
        executed_query = query_call[0][0]

        # Verify query structure
        self.assertIn("MATCH (p:ProjectRevision)", executed_query)
        self.assertIn("RETURN DISTINCT p.id AS project_id", executed_query)
        self.assertNotIn("TEMP", executed_query)

    @patch("recomsystem.processing.matilda.pd.read_csv")
    @patch("recomsystem.processing.matilda.GraphDatabase.driver")
    def test_process_by_technologies_logging_without_similarity(self, mock_driver, mock_read_csv):
        """Test that appropriate logging occurs when not using similarity filtering"""
        # Setup mocks
        mock_read_csv.side_effect = [self.mock_libs_df, self.mock_decisions_df, self.mock_category_tech_map_df]

        mock_session = MagicMock()
        mock_driver_instance = MagicMock()
        mock_driver.return_value = mock_driver_instance
        mock_driver_instance.session.return_value.__enter__.return_value = mock_session
        mock_session.run.return_value = []

        processor = RecommendationProcessor(self.mock_config)

        with patch("recomsystem.processing.matilda.logger") as mock_logger:
            processor.process_by_technologies(["Technology1"], filter_similar_projects=False)

            # Check that all projects filtering log message was called
            info_calls = [call[0][0] for call in mock_logger.info.call_args_list]
            all_projects_log_found = any("Using all projects without similarity filtering" in msg for msg in info_calls)
            self.assertTrue(all_projects_log_found, "Should log all projects usage")

    @patch("recomsystem.processing.matilda.pd.read_csv")
    @patch("recomsystem.processing.matilda.GraphDatabase.driver")
    def test_similarity_values_set_to_one_without_filtering(self, mock_driver, mock_read_csv):
        """Test that similarity values are set to 1.0 when not using filtering"""
        # Setup mocks
        mock_read_csv.side_effect = [self.mock_libs_df, self.mock_decisions_df, self.mock_category_tech_map_df]

        mock_session = MagicMock()
        mock_driver_instance = MagicMock()
        mock_driver.return_value = mock_driver_instance
        mock_driver_instance.session.return_value.__enter__.return_value = mock_session

        # Mock return with actual project data
        mock_session.run.return_value = [
            {"project_id": "commit1", "project_name": "Project1"},
            {"project_id": "commit2", "project_name": "Project2"},
        ]

        processor = RecommendationProcessor(self.mock_config)
        neo4j_manager = processor.neo4j_manager

        # Test get_all_projects method
        result_df = neo4j_manager.get_all_projects()

        # Verify that similarity values are set to 1.0
        self.assertTrue(all(result_df["tech_similarity"] == 1.0))
        self.assertTrue(all(result_df["category_similarity"] == 1.0))
        self.assertTrue(all(result_df["custom_similarity"] == 1.0))


if __name__ == "__main__":
    unittest.main()
