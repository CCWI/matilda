import logging

import pandas as pd
from neo4j import GraphDatabase

from recomsystem.config.processing_config import ProcessingConfig

logger = logging.getLogger(__name__)


class Neo4jManager:
    """Handles all Neo4j database operations"""

    def __init__(self, config: ProcessingConfig):
        self.config = config

    def get_similar_projects(self, technologies: list[str], limit: int = 3000000) -> pd.DataFrame:
        """Finds similar projects based on shared technologies"""
        driver = GraphDatabase.driver(self.config.NEO4J_URI, auth=(self.config.NEO4J_USER, self.config.NEO4J_PASSWORD))

        try:
            with driver.session() as session:
                self._create_temp_node(session, technologies)
                result = self._execute_similarity_query(session, limit)
                self._cleanup_temp_node(session)
                return pd.DataFrame(result)
        finally:
            driver.close()

    def get_all_projects(self, limit: int = 3000000) -> pd.DataFrame:
        """Gets all projects without similarity filtering"""
        driver = GraphDatabase.driver(self.config.NEO4J_URI, auth=(self.config.NEO4J_USER, self.config.NEO4J_PASSWORD))

        try:
            with driver.session() as session:
                result = self._execute_all_projects_query(session, limit)
                return pd.DataFrame(result)
        finally:
            driver.close()

    def _execute_all_projects_query(self, session, limit: int) -> list[dict]:
        """Execute query to get all projects"""
        result = session.run(self._get_all_projects_query(), limit=limit)
        return [
            {
                "similar_project_id": record["project_id"],
                "similar_project_name": record["project_name"],
                "tech_similarity": 1.0,  # Set to 1.0 since we're not filtering
                "category_similarity": 1.0,  # Set to 1.0 since we're not filtering
                "custom_similarity": 1.0,  # Set to 1.0 since we're not filtering
            }
            for record in result
        ]

    @staticmethod
    def _get_all_projects_query() -> str:
        """Query to get all projects"""
        return """
            MATCH (p:ProjectRevision)
            RETURN DISTINCT p.id AS project_id, p.projectName AS project_name
            ORDER BY project_id
            LIMIT $limit
        """

    def _create_temp_node(self, session, technologies: list[str]) -> None:
        session.run(
            """
            MERGE (tempNode:ProjectRevision {id: "TEMP"})
            WITH tempNode
            UNWIND $technologies AS tech_id
            MATCH (t:Technology {id: tech_id})
            MERGE (tempNode)-[:USES]->(t);
            """,
            technologies=technologies,
        )

    def _execute_similarity_query(self, session, limit: int) -> list[dict]:
        result = session.run(self._get_similarity_query(), limit=limit)
        return [
            {
                "similar_project_id": record["similar_project_id"],
                "similar_project_name": record["similar_project_name"],
                "tech_similarity": record["tech_similarity"],
                "category_similarity": record["category_similarity"],
                "custom_similarity": record["custom_similarity"],
            }
            for record in result
        ]

    def _cleanup_temp_node(self, session) -> None:
        session.run("""
            MATCH (tempNode:ProjectRevision {id: "TEMP"})
            DETACH DELETE tempNode;
        """)

    @staticmethod
    def _get_similarity_query() -> str:
        return """
            MATCH (tempNode:ProjectRevision {id: "TEMP"})-[:USES]->(t:Technology)
            WITH tempNode, collect(t.id) AS temp_tech_ids, collect(t.category) AS temp_categories

            MATCH (t:Technology)<-[:USES]-(p:ProjectRevision)
            WHERE t.id IN temp_tech_ids AND p.id <> "TEMP"

            WITH p, temp_tech_ids, temp_categories,
                 collect(DISTINCT t.id) as shared_tech_ids,
                 collect(DISTINCT t.category) as p_categories

            WITH p.id AS similar_project_id,
                 p.projectName AS similar_project_name,
                 size(shared_tech_ids) AS common_tech_count,
                 size(temp_tech_ids) AS temp_tech_count,
                 size([tid IN shared_tech_ids WHERE tid IN temp_tech_ids]) AS p_tech_count,
                 size([cat IN temp_categories WHERE cat IN p_categories]) AS common_cat_count,
                 size(temp_categories) AS temp_cat_count,
                 size(p_categories) AS p_cat_count

            WITH similar_project_id, similar_project_name,
                 toFloat(common_tech_count) / (temp_tech_count + p_tech_count - common_tech_count) AS tech_similarity,
                 toFloat(common_cat_count) / (temp_cat_count + p_cat_count - common_cat_count) AS category_similarity

            WITH similar_project_id, similar_project_name,
                 tech_similarity, category_similarity,
                 (tech_similarity + category_similarity) / 2.0 as custom_similarity
            WHERE tech_similarity > 0
            RETURN similar_project_id, similar_project_name,
                   tech_similarity, category_similarity, custom_similarity
            ORDER BY custom_similarity DESC
            LIMIT $limit
        """
