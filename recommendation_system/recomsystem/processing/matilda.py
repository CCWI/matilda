import json
import logging
import time
from datetime import datetime
from typing import Dict, List, Optional, Set, Tuple

import pandas as pd

from recomsystem.config.logging_config import setup_logging
from recomsystem.config.processing_config import ProcessingConfig
from recomsystem.connectors.neo4j.neo4j_connector import Neo4jManager
from recomsystem.matilda.relevance_processing import find_and_classify_project_documentation_by_dataframe

logger = logging.getLogger(__name__)


class DataLoader:
    """Handles loading and basic preprocessing of data files"""

    def __init__(self, config: ProcessingConfig):
        self.config = config
        self.libs_df = pd.read_csv(config.LIBS_RESULT_PATH)
        self.design_decisions_df = pd.read_csv(config.DESIGN_DECISIONS_PATH)
        self.category_tech_map_df = pd.read_csv(config.CATEGORY_TECH_MAP_PATH)

    @staticmethod
    def convert_date_to_epoch(ds_date: str) -> int:
        """Convert datetime string to epoch seconds"""
        words = ds_date.split()
        date_str = words[0]
        date = datetime.strptime(date_str, "%Y-%m-%d")
        return int(time.mktime(date.timetuple()))


class TechnologyMapper:
    """Handles mapping between libraries and technologies"""

    def __init__(self, libs_df: pd.DataFrame, category_tech_map_df: pd.DataFrame):
        self.libs_df = libs_df
        self.category_tech_map_df = category_tech_map_df

    def map_libraries_to_technologies(self, libraries: List[str]) -> List[str]:
        """Maps a list of libraries to their corresponding technologies"""
        technologies: Set[str] = set()
        for lib in libraries:
            tech = self._get_technology_of_library(lib)
            if tech and tech != "Unknown":
                technologies.add(tech)
        return list(technologies)

    def _get_technology_of_library(self, library: str) -> Optional[str]:
        """Gets the technology corresponding to a single library"""
        matched_row = self.libs_df[self.libs_df["GA"] == library]
        if not matched_row.empty:
            tech = matched_row.iloc[0]["result"]
            tech_unpacked = tech.strip("[]").strip('"')
            if pd.notna(tech_unpacked) and tech_unpacked != "Unknown":
                return tech_unpacked
        return None

    def get_categories_for_technologies(self, technologies: List[str]) -> List[str]:
        """Find relevant categories for the given technologies"""
        technologies = [tech.replace("'", "") for tech in technologies]
        filtered_df = self.category_tech_map_df[self.category_tech_map_df["result"].isin(technologies)]
        return filtered_df["MATILDA_CATEGORY"].tolist()


class MigrationAnalyzer:
    """Handles analysis of technology migrations"""

    def __init__(self, data_loader: DataLoader, tech_mapper: TechnologyMapper):
        self.data_loader = data_loader
        self.tech_mapper = tech_mapper

    def calculate_migration_degree(self, df_tech: pd.DataFrame, df_migs: pd.DataFrame) -> pd.DataFrame:
        """Calculates the weighted migration degree for technologies"""
        df_migs["decision_commit_epoch"] = pd.to_numeric(df_migs["decision_commit_epoch"], errors="coerce").fillna(0)
        df_migs["custom_similarity"] = pd.to_numeric(df_migs["custom_similarity"], errors="coerce").fillna(0)
        df_migs["weighted_value"] = df_migs["decision_commit_epoch"] * df_migs["custom_similarity"]

        incoming_weighted = df_migs.groupby("target_technology")["weighted_value"].sum()
        outgoing_weighted = df_migs.groupby("initial_technology")["weighted_value"].sum()

        df_tech["incoming_weight"] = df_tech["result"].map(incoming_weighted).fillna(0)
        df_tech["outgoing_weight"] = df_tech["result"].map(outgoing_weighted).fillna(0)
        df_tech["mig_rate_w"] = df_tech["incoming_weight"] - df_tech["outgoing_weight"]

        return df_tech.drop(columns=["incoming_weight", "outgoing_weight"])

    def process_migrations(self, df_combined: pd.DataFrame) -> Tuple[pd.DataFrame, pd.DataFrame]:
        """Processes migrations and additions/removals of technologies"""
        df_combined["initial_technology"] = df_combined["initial"].apply(
            lambda x: self.tech_mapper._get_technology_of_library(x) if pd.notna(x) else None
        )
        df_combined["target_technology"] = df_combined["target"].apply(
            lambda x: self.tech_mapper._get_technology_of_library(x) if pd.notna(x) else None
        )

        # Then proceed with the rest
        df_combined["initial_technology"] = df_combined["initial_technology"].str.replace("'", "")
        df_combined["target_technology"] = df_combined["target_technology"].str.replace("'", "")

        df_migrations = df_combined[
            (df_combined["initial_technology"] != "Unknown")
            & (df_combined["target_technology"] != "Unknown")
            & (df_combined["initial_technology"] != df_combined["target_technology"])
        ].dropna(subset=["initial_technology", "target_technology"])

        df_addition_or_removal = df_combined[df_combined.isna().any(axis=1)]

        return df_migrations, df_addition_or_removal


class RecommendationProcessor:
    """Main class for processing technology recommendations"""

    def __init__(self, config: ProcessingConfig):
        self.data_loader = DataLoader(config)
        self.tech_mapper = TechnologyMapper(self.data_loader.libs_df, self.data_loader.category_tech_map_df)
        self.neo4j_manager = Neo4jManager(config)
        self.migration_analyzer = MigrationAnalyzer(self.data_loader, self.tech_mapper)

    def process_by_libraries(
        self, libraries: List[str], filter_similar_projects: bool = True, filter_relevant_projects: bool = False
    ) -> List[Dict]:
        """Process recommendations starting from a list of libraries"""
        technologies = self.tech_mapper.map_libraries_to_technologies(libraries)
        logger.debug(f"MATILDA: Found technologies by libraries: {technologies}")
        return self.process_by_technologies(technologies, filter_similar_projects, filter_relevant_projects)

    def process_by_technologies(
        self, technologies: List[str], filter_similar_projects: bool = True, filter_relevant_projects: bool = False
    ) -> List[Dict]:
        """Process recommendations starting from a list of technologies

        Args:
            technologies: List of technology names
            filter_similar_projects: If True, only consider similar projects. If False, use all projects.
        """
        if filter_similar_projects:
            logger.info("MATILDA: Using similarity filtering for projects")
            df_projects = self.neo4j_manager.get_similar_projects(technologies)
            logger.info(f"MATILDA: Found {len(df_projects)} similar projects")
        else:
            logger.info("MATILDA: Using all projects without similarity filtering")
            df_projects = self.neo4j_manager.get_all_projects()
            logger.info(f"MATILDA: Found {len(df_projects)} total projects")

        df_decisions = self._process_design_decisions(df_projects, technologies)
        logger.info(f"MATILDA: Found {len(df_decisions)} design decisions")
        df_migrations, _ = self.migration_analyzer.process_migrations(df_decisions)
        logger.info(f"MATILDA: Found {len(df_migrations)} migrations")

        logger.info("MATILDA: Start filtering for professional projects by id")

        # TEMPORARY: Save migrations to CSV for debugging
        # df_migrations.to_csv("./data/migrations.csv", index=False)

        # Run relevance classification on migrations
        if filter_relevant_projects:
            df_migrations_classified = find_and_classify_project_documentation_by_dataframe(df_migrations)
            # Filter for professional projects (is_professional == True or 1)
            df_migrations = df_migrations_classified[
                (df_migrations_classified["is_professional"]) | (df_migrations_classified["is_professional"] == 1)
            ]
            logger.info(f"MATILDA: Filtered to {len(df_migrations)} professional projects from {len(df_migrations_classified)} total")

        logger.info("MATILDA: Start calculating recommendations...")
        return self._calculate_recommendations(technologies, df_migrations)

    def _process_design_decisions(self, df_projects: pd.DataFrame, technologies: List[str]) -> pd.DataFrame:
        """Process design decisions for projects (renamed from df_similar to df_projects)"""
        df_filtered = df_projects[
            df_projects["similar_project_id"].isin(self.data_loader.design_decisions_df["decision_commit_id"])
            | df_projects["similar_project_id"].isin(self.data_loader.design_decisions_df["initial_commit_id"])
        ]

        columns_to_merge = ["decision_commit_id", "initial_commit_id", "decision_commit_time", "decision_subject", "initial", "target"]
        decisions_subset = self.data_loader.design_decisions_df[columns_to_merge].drop_duplicates()

        df_merged_decision = df_filtered.merge(
            decisions_subset, left_on="similar_project_id", right_on="decision_commit_id", how="left", suffixes=("", "_decision_commit")
        )

        df_merged_initial = df_filtered.merge(
            decisions_subset, left_on="similar_project_id", right_on="initial_commit_id", how="left", suffixes=("", "_initial_commit")
        )

        df_combined = pd.concat([df_merged_decision, df_merged_initial])
        df_combined = df_combined.drop(columns=["decision_commit_id", "initial_commit_id"], errors="ignore")
        df_combined = df_combined.drop_duplicates().reset_index(drop=True)

        relevant_categories = self.tech_mapper.get_categories_for_technologies(technologies)
        df_combined = df_combined[df_combined["decision_subject"].isin(relevant_categories)]

        for idx, row in df_combined.iterrows():
            if pd.notna(row["decision_commit_time"]):
                df_combined.at[idx, "decision_commit_epoch"] = self.data_loader.convert_date_to_epoch(row["decision_commit_time"])

        return df_combined

    def _debug_technology_in_data(self, technology: str, df_migrations: pd.DataFrame, df_tech_rank: pd.DataFrame) -> None:
        """Debug method to analyze why a technology has no recommendations"""
        logger.debug(f"DEBUG: Analyzing technology '{technology}' in data sources")

        # Check if technology exists in tech_rank dataframe
        tech_in_rank = df_tech_rank[df_tech_rank["result"] == technology]
        logger.debug(f"DEBUG: Technology '{technology}' found in tech_rank: {not tech_in_rank.empty}")
        if not tech_in_rank.empty:
            logger.debug(f"DEBUG: Tech rank entry: {tech_in_rank.iloc[0].to_dict()}")

        # Check for similar technology names in tech_rank
        similar_techs = df_tech_rank[df_tech_rank["result"].str.contains(technology.split()[0], case=False, na=False)]
        if not similar_techs.empty:
            logger.debug(f"DEBUG: Similar technologies in tech_rank: {similar_techs['result'].tolist()}")

        # Check migrations dataframe for this technology as initial or target
        initial_occurrences = df_migrations[df_migrations["initial_technology"] == technology]
        target_occurrences = df_migrations[df_migrations["target_technology"] == technology]

        logger.debug(f"DEBUG: Technology '{technology}' appears as initial in {len(initial_occurrences)} migrations")
        logger.debug(f"DEBUG: Technology '{technology}' appears as target in {len(target_occurrences)} migrations")

        # Check for partial matches in migrations
        initial_partial = df_migrations[df_migrations["initial_technology"].str.contains(technology.split()[0], case=False, na=False)]
        target_partial = df_migrations[df_migrations["target_technology"].str.contains(technology.split()[0], case=False, na=False)]

        if not initial_partial.empty:
            logger.debug(f"DEBUG: Partial matches in initial_technology: {initial_partial['initial_technology'].unique().tolist()}")
        if not target_partial.empty:
            logger.debug(f"DEBUG: Partial matches in target_technology: {target_partial['target_technology'].unique().tolist()}")

        # Check category mapping
        cat_mapping = self.data_loader.category_tech_map_df[self.data_loader.category_tech_map_df["result"] == technology]
        if not cat_mapping.empty:
            category = cat_mapping.iloc[0]["MATILDA_CATEGORY"]
            logger.debug(f"DEBUG: Technology '{technology}' belongs to category: {category}")
            same_category_techs = self.data_loader.category_tech_map_df[
                self.data_loader.category_tech_map_df["MATILDA_CATEGORY"] == category
            ]
            logger.debug(f"DEBUG: Other technologies in same category: {same_category_techs['result'].tolist()}")
        else:
            logger.debug(f"DEBUG: Technology '{technology}' not found in category mapping")

    def _calculate_recommendations(self, technologies: List[str], df_migrations: pd.DataFrame) -> List[Dict]:
        """Calculate final technology recommendations"""
        logger.debug(f"MATILDA: Starting _calculate_recommendations with technologies: {technologies}")
        logger.debug(f"MATILDA: Migration dataframe shape: {df_migrations.shape}")
        logger.debug(f"MATILDA: Migration columns: {df_migrations.columns.tolist()}")

        df_tech_rank = self.migration_analyzer.calculate_migration_degree(self.data_loader.category_tech_map_df, df_migrations)
        df_tech_rank = df_tech_rank.sort_values(by="strength_based_centrality", ascending=False).reset_index(drop=True)
        logger.debug(f"MATILDA: Tech rank dataframe shape: {df_tech_rank.shape}")

        technologies = [tech.replace("'", "") for tech in technologies if tech != "Unknown"]
        logger.debug(f"MATILDA: Cleaned technologies: {technologies}")
        result_list = []

        for technology in technologies:
            logger.debug(f"MATILDA: Processing technology: {technology}")

            # Get the category of the source technology
            tech_rank_filtered = df_tech_rank[df_tech_rank["result"] == technology]
            if tech_rank_filtered.empty:
                logger.debug(f"MATILDA: Technology {technology} not found in tech_rank dataframe")
                self._debug_technology_in_data(technology, df_migrations, df_tech_rank)
                continue

            source_category = tech_rank_filtered["MATILDA_CATEGORY"].values[0]
            logger.debug(f"MATILDA: Technology {technology} belongs to category: {source_category}")

            # Find migrations from this technology
            mig_from_tech_df = df_migrations[
                (df_migrations["initial_technology"] == technology)
                & (df_migrations["target_technology"].notna())
                & (df_migrations["target_technology"] != "Unknown")
            ]
            logger.debug(f"MATILDA: Found {len(mig_from_tech_df)} migrations from {technology}")

            if len(mig_from_tech_df) == 0:
                logger.debug(f"MATILDA: No migrations found for {technology} - checking data...")
                self._debug_technology_in_data(technology, df_migrations, df_tech_rank)
                continue

            # Get unique target technologies from migrations
            unique_tech = mig_from_tech_df["target_technology"].unique()
            logger.debug(f"MATILDA: Migration targets before category filtering: {unique_tech.tolist()}")

            # Filter target technologies to only include those in the same category
            df_tech_rank_same_cat = df_tech_rank[df_tech_rank["MATILDA_CATEGORY"] == source_category]
            same_category_techs = df_tech_rank_same_cat["result"].tolist()

            # Only keep target technologies that are in the same category
            filtered_unique_tech = [tech for tech in unique_tech if tech in same_category_techs]
            logger.debug(f"MATILDA: Migration targets after category filtering ({source_category}): {filtered_unique_tech}")

            if not filtered_unique_tech:
                logger.warning(f"MATILDA: No migration targets found in same category ({source_category}) for technology {technology}")
                # Still add an entry but with empty recommendations
                result_list.append({"technology": technology, "recommended_technologies": []})
                continue

            recommended_technologies = []

            for tech in filtered_unique_tech:
                logger.debug(f"MATILDA: Processing recommendation target: {tech}")
                tech_rank_filtered_target = df_tech_rank[df_tech_rank["result"] == tech]
                if tech_rank_filtered_target.empty:
                    logger.debug(f"MATILDA: Target technology {tech} not found in tech_rank dataframe")
                    continue

                tech_mig_rate_w = tech_rank_filtered_target["mig_rate_w"].values[0]

                # Calculate migration statistics for this target technology
                incoming_migrations = df_migrations[df_migrations["target_technology"] == tech]
                outgoing_migrations = df_migrations[df_migrations["initial_technology"] == tech]

                # Calculate migration counts
                incoming_count = len(incoming_migrations)
                outgoing_count = len(outgoing_migrations)
                net_migration = incoming_count - outgoing_count

                # Calculate migration statistics with weights (custom_similarity)
                incoming_weighted_sum = incoming_migrations["custom_similarity"].sum() if incoming_count > 0 else 0
                outgoing_weighted_sum = outgoing_migrations["custom_similarity"].sum() if outgoing_count > 0 else 0

                # Calculate average project age (from decision_commit_epoch)
                all_migrations_for_tech = pd.concat([incoming_migrations, outgoing_migrations]).drop_duplicates()
                if len(all_migrations_for_tech) > 0 and "decision_commit_epoch" in all_migrations_for_tech.columns:
                    # Convert epochs to years ago (assuming current epoch is recent)
                    import time

                    current_epoch = int(time.time())
                    epochs = pd.to_numeric(all_migrations_for_tech["decision_commit_epoch"], errors="coerce").dropna()
                    if len(epochs) > 0:
                        avg_epoch = epochs.mean()
                        years_ago = (current_epoch - avg_epoch) / (365.25 * 24 * 3600)  # Convert to years
                        avg_project_age_years = round(max(0, years_ago), 1)
                        oldest_epoch = epochs.min()
                        newest_epoch = epochs.max()
                        oldest_years_ago = round(max(0, (current_epoch - oldest_epoch) / (365.25 * 24 * 3600)), 1)
                        newest_years_ago = round(max(0, (current_epoch - newest_epoch) / (365.25 * 24 * 3600)), 1)
                    else:
                        avg_project_age_years = None
                        oldest_years_ago = None
                        newest_years_ago = None
                else:
                    avg_project_age_years = None
                    oldest_years_ago = None
                    newest_years_ago = None

                # Calculate direct migrations from source technology to this target
                direct_migrations = mig_from_tech_df[mig_from_tech_df["target_technology"] == tech]
                direct_migration_count = len(direct_migrations)
                direct_similarity_avg = direct_migrations["custom_similarity"].mean() if direct_migration_count > 0 else 0

                # Calculate migrank with proper bounds checking
                min_mig_rate = min(df_tech_rank_same_cat["mig_rate_w"])
                max_mig_rate = max(df_tech_rank_same_cat["mig_rate_w"])

                if max_mig_rate == min_mig_rate:
                    migrank = 0.5  # Default value if all technologies have same migration rate
                    logger.debug(f"MATILDA: All technologies in category {source_category} have same migration rate, using migrank=0.5")
                else:
                    migrank = (tech_mig_rate_w - min_mig_rate) / (max_mig_rate - min_mig_rate)

                matildaRecom_mig = migrank * tech_rank_filtered_target["strength_based_centrality"].values[0]

                logger.debug(
                    f"MATILDA: Recommendation calc for {tech}: mig_rate_w={tech_mig_rate_w}, migrank={migrank}, matildaRecom_mig={matildaRecom_mig}"
                )

                # Build comprehensive recommendation entry
                recommendation_entry = {
                    "technology": tech,
                    "category": tech_rank_filtered_target["MATILDA_CATEGORY"].values[0],
                    "matildaRecom_mig": round(matildaRecom_mig, 4),
                    "migrank": round(migrank, 4),
                    "strength_based_centrality": round(tech_rank_filtered_target["strength_based_centrality"].values[0], 4),
                    # Migration statistics
                    "migration_stats": {
                        "incoming_migrations": incoming_count,
                        "outgoing_migrations": outgoing_count,
                        "net_migration": net_migration,
                        "incoming_weighted_sum": round(incoming_weighted_sum, 4),
                        "outgoing_weighted_sum": round(outgoing_weighted_sum, 4),
                        "direct_from_source": direct_migration_count,
                        "direct_similarity_avg": round(direct_similarity_avg, 4),
                    },
                    # Project age insights
                    "project_age_insights": {
                        "avg_project_age_years": avg_project_age_years,
                        "oldest_decision_years_ago": oldest_years_ago,
                        "newest_decision_years_ago": newest_years_ago,
                        "sample_size": len(epochs) if avg_project_age_years is not None else 0,
                    },
                }

                recommended_technologies.append(recommendation_entry)

            # Sort recommendations by matildaRecom_mig score (descending)
            recommended_technologies.sort(key=lambda x: x["matildaRecom_mig"], reverse=True)

            result_list.append({"technology": technology, "recommended_technologies": recommended_technologies})
            logger.info(f"MATILDA: Found for {technology}: {len(recommended_technologies)} recommendations")
            logger.debug(f"MATILDA: Recommendations for {technology}: {recommended_technologies}")

        return result_list


if __name__ == "__main__":
    logger = setup_logging()
    logger = logging.getLogger(__name__)

    logger.info("Starting MATILDA processing...")
    processor = RecommendationProcessor(ProcessingConfig())

    # Example with similarity filtering (default behavior)
    logger.info("Processing with similarity filtering...")
    results_filtered = processor.process_by_technologies(
        ["'Oracle'", "'Apache ActiveMQ / ServiceMix'"], filter_similar_projects=True, filter_relevant_projects=True
    )
    logger.info("Results with similarity filtering:")
    logger.info(json.dumps(results_filtered, sort_keys=True, indent=4))

    # Example without similarity filtering (use all projects)
    logger.info("Processing without similarity filtering...")
    results_all = processor.process_by_technologies(
        ["'Oracle'", "'Apache ActiveMQ / ServiceMix'"], filter_similar_projects=False, filter_relevant_projects=False
    )
    logger.info("Results without similarity filtering:")
    logger.info(json.dumps(results_all, sort_keys=True, indent=4))
