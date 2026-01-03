import unittest
from unittest.mock import MagicMock, Mock, patch

import pandas as pd

from recomsystem.config.processing_config import ProcessingConfig
from recomsystem.processing.matilda import RecommendationProcessor


class TestRecommendationProcessor(unittest.TestCase):
    """Test cases for RecommendationProcessor with similarity filtering enabled"""

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
                "decision_commit_id": ["commit1", "commit2"],
                "initial_commit_id": ["initial1", "initial2"],
                "decision_commit_time": ["2023-01-01 10:00:00", "2023-01-02 10:00:00"],
                "decision_subject": ["Category1", "Category2"],
                "initial": ["com.test:library1", "com.test:library2"],
                "target": ["com.test:library2", "com.test:library3"],
            }
        )

        self.mock_category_tech_map_df = pd.DataFrame(
            {
                "result": ["Technology1", "Technology2"],
                "MATILDA_CATEGORY": ["Category1", "Category1"],
                "strength_based_centrality": [0.8, 0.6],
            }
        )

        # Mock similar projects response
        self.mock_similar_projects = pd.DataFrame(
            {
                "similar_project_id": ["commit1", "commit2"],
                "similar_project_name": ["Project1", "Project2"],
                "tech_similarity": [0.9, 0.8],
                "category_similarity": [0.85, 0.75],
                "custom_similarity": [0.875, 0.775],
            }
        )

    @patch("recomsystem.processing.matilda.pd.read_csv")
    @patch("recomsystem.processing.matilda.GraphDatabase.driver")
    def test_process_by_technologies_with_similarity_filtering(self, mock_driver, mock_read_csv):
        """Test process_by_technologies with similarity filtering enabled"""
        # Setup mocks
        mock_read_csv.side_effect = [self.mock_libs_df, self.mock_decisions_df, self.mock_category_tech_map_df]

        # Mock Neo4j session and results
        mock_session = MagicMock()
        mock_driver_instance = MagicMock()
        mock_driver.return_value = mock_driver_instance
        mock_driver_instance.session.return_value.__enter__.return_value = mock_session

        # Mock the similarity query result
        mock_session.run.return_value = [
            {
                "similar_project_id": "commit1",
                "similar_project_name": "Project1",
                "tech_similarity": 0.9,
                "category_similarity": 0.85,
                "custom_similarity": 0.875,
            }
        ]

        # Create processor instance
        processor = RecommendationProcessor(self.mock_config)

        # Test with similarity filtering enabled (default)
        technologies = ["Technology1"]
        result = processor.process_by_technologies(technologies, filter_similar_projects=True)

        # Assertions
        self.assertIsInstance(result, list)
        mock_driver.assert_called_with(self.mock_config.NEO4J_URI, auth=(self.mock_config.NEO4J_USER, self.mock_config.NEO4J_PASSWORD))

        # Verify that similarity query was called (temp node creation, query execution, cleanup)
        self.assertTrue(mock_session.run.called)
        call_args_list = [call[0][0] for call in mock_session.run.call_args_list]

        # Check that similarity-related queries were executed
        similarity_queries = [query for query in call_args_list if "TEMP" in query]
        self.assertGreater(len(similarity_queries), 0, "Similarity queries should be executed")

    @patch("recomsystem.processing.matilda.pd.read_csv")
    @patch("recomsystem.processing.matilda.GraphDatabase.driver")
    def test_process_by_technologies_with_empty_technologies(self, mock_driver, mock_read_csv):
        """Test process_by_technologies with empty technologies list"""
        # Setup mocks
        mock_read_csv.side_effect = [self.mock_libs_df, self.mock_decisions_df, self.mock_category_tech_map_df]

        mock_session = MagicMock()
        mock_driver_instance = MagicMock()
        mock_driver.return_value = mock_driver_instance
        mock_driver_instance.session.return_value.__enter__.return_value = mock_session
        mock_session.run.return_value = []

        processor = RecommendationProcessor(self.mock_config)

        # Test with empty technologies list
        result = processor.process_by_technologies([], filter_similar_projects=True)

        # Should return empty list
        self.assertEqual(result, [])

    @patch("recomsystem.processing.matilda.pd.read_csv")
    @patch("recomsystem.processing.matilda.GraphDatabase.driver")
    def test_process_by_technologies_logging_with_similarity(self, mock_driver, mock_read_csv):
        """Test that appropriate logging occurs when using similarity filtering"""
        # Setup mocks
        mock_read_csv.side_effect = [self.mock_libs_df, self.mock_decisions_df, self.mock_category_tech_map_df]

        mock_session = MagicMock()
        mock_driver_instance = MagicMock()
        mock_driver.return_value = mock_driver_instance
        mock_driver_instance.session.return_value.__enter__.return_value = mock_session
        mock_session.run.return_value = []

        processor = RecommendationProcessor(self.mock_config)

        with patch("recomsystem.processing.matilda.logger") as mock_logger:
            processor.process_by_technologies(["Technology1"], filter_similar_projects=True)

            # Check that similarity filtering log message was called
            info_calls = [call[0][0] for call in mock_logger.info.call_args_list]
            similarity_log_found = any("Using similarity filtering" in msg for msg in info_calls)
            self.assertTrue(similarity_log_found, "Should log similarity filtering usage")


if __name__ == "__main__":
    unittest.main()
