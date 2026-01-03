import json
import logging
from typing import Optional

import requests

from recomsystem.config.config import env_config
from recomsystem.config.predefined_prompts_for_usecases import prompt_handler, prompt_uc1, prompt_uc2, prompt_uc3
from recomsystem.processing.maven_parser import parse_pom_xml

logger = logging.getLogger(__name__)

API_BASE_URL = f"http://127.0.0.1:{env_config['backend_port']}"


def call_api_endpoints(
    message: str, selected_llm: str, dependencies: list = None
) -> tuple[str, str, str, str, str, str, str, str, str, str]:
    """
    Call both FastAPI endpoints and return responses.        Args:
        message: User message
        selected_llm: Selected LLM model name
        dependencies: Optional list of dependencies extracted from pom.xml

    Returns:
        tuple[str, str, str, str, str, str, str, str, str, str]: status, matilda_recommendations_filtered_formatted, matilda_recommendations_unfiltered_formatted, matilda_recommendations_rel_filtered_formatted, baseline_accordion_html, matilda_accordion_html, matilda_filtered_accordion_html, matilda_unfiltered_accordion_html, matilda_rel_filtered_accordion_html, anonymous_comparison_html
    """
    logger.info("Calling API Endpoints")

    if not message or message.strip() == "":
        empty_msg = "Please enter a query."
        return empty_msg, empty_msg, empty_msg, empty_msg, empty_msg, empty_msg, empty_msg, empty_msg, empty_msg, empty_msg

    # Determine use case based on message content
    prompt_template = None
    use_case = None  # Standard use case
    if prompt_uc1 in message:
        use_case = "uc1"
    elif prompt_uc2 in message:
        use_case = "uc2"
    elif prompt_uc3 in message:
        use_case = "uc3"

    if use_case:
        # Load predefined prompt template for the specific use case
        prompt_template = prompt_handler.get_prompt(prompt_type="recommendation", prompt_name=f"prompt_{use_case}")

    if not prompt_template:
        logger.debug(f"Prompt template for {use_case} not found, using default template")
        prompt_template = ""
    else:
        prompt_template = f"Szenario: \n{prompt_template}"

    full_request = f"{prompt_template} \n\nFrage: \n{message}"

    logger.debug(f"Full request to be sent:\n{full_request}")

    # Prepare data for API request
    data = {"message": full_request, "llm": selected_llm}

    # Add dependencies if available
    if dependencies and len(dependencies) > 0:
        data["dependencies"] = dependencies
        print(f"Including {len(dependencies)} dependencies in API request")

    headers = {"Content-Type": "application/json"}

    try:
        # Call baseline endpoint
        print(f"Calling baseline endpoint with LLM: {selected_llm}")
        baseline_response = requests.post(f"{API_BASE_URL}/chat/baseline", json=data, headers=headers)
        baseline_response.raise_for_status()

        # Parse JSON response
        baseline_data = baseline_response.json()
        baseline_text = baseline_data.get("content", "")
        baseline_structured = baseline_data.get("structured_response", {})

        # Call matilda endpoint
        print(f"Calling matilda endpoint with LLM: {selected_llm}")
        matilda_response = requests.post(f"{API_BASE_URL}/chat/matilda", json=data, headers=headers)
        matilda_response.raise_for_status()

        # Parse JSON response
        matilda_data = matilda_response.json()
        matilda_recommendations_filtered = matilda_data.get("matilda_recommendations_filtered", "")
        matilda_recommendations_unfiltered = matilda_data.get("matilda_recommendations_unfiltered", "")
        matilda_recommendations_rel_filtered = matilda_data.get("matilda_recommendations_rel_filtered", "")
        matilda_text = matilda_data.get("content", "")
        matilda_structured = matilda_data.get("structured_response", {})
        matilda_text_unfiltered = matilda_data.get("content_unf", "")
        matilda_structured_unfiltered = matilda_data.get("structured_response_unf", {})
        matilda_text_rel_filtered = matilda_data.get("content_rel_filtered", "")
        matilda_structured_response_rel_filtered = matilda_data.get("structured_response_rel_filtered", {})

        # Get detailed recommendations for accordion displays
        detailed_recommendations_filtered = matilda_data.get("detailed_recommendations_filtered", [])
        detailed_recommendations_unfiltered = matilda_data.get("detailed_recommendations_unfiltered", [])
        detailed_recommendations_rel_filtered = matilda_data.get("detailed_recommendations_rel_filtered", [])

        # Format MATILDA recommendations nicely for both types
        # Use the new integrated accordion format that includes details inline
        matilda_recommendations_filtered_formatted = format_matilda_recommendations_with_details(
            matilda_recommendations_filtered, "🧩 MATILDA Empfehlungen (Mit ähnlichen Projekten gefiltert)"
        )
        matilda_recommendations_unfiltered_formatted = format_matilda_recommendations_with_details(
            matilda_recommendations_unfiltered, "🧩 MATILDA Empfehlungen (Ohne Filterung ähnlicher Projekte)"
        )
        matilda_recommendations_rel_filtered_formatted = format_matilda_recommendations_with_details(
            matilda_recommendations_rel_filtered, "🧩 MATILDA Empfehlungen (Gefiltert nach Relevanz)"
        )

        # The details are now integrated into the main recommendations via accordion
        # No need for separate detailed formatting since accordion includes all details

        # Create accordion displays for UI updates with detailed statistics
        baseline_accordion_html = create_recommendation_display_components_with_accordion(
            baseline_structured, "Baseline Empfehlungen", show_score_column=False
        )
        matilda_accordion_html = create_recommendation_display_components_with_accordion(
            matilda_structured, "MATILDA Empfehlungen", detailed_recommendations_filtered
        )
        matilda_filtered_accordion_html = create_recommendation_display_components_with_accordion(
            matilda_structured, "MATILDA Empfehlungen (Gefiltert)", detailed_recommendations_filtered
        )
        matilda_unfiltered_accordion_html = create_recommendation_display_components_with_accordion(
            matilda_structured_unfiltered, "MATILDA Empfehlungen (Ungefiltert)", detailed_recommendations_unfiltered
        )
        matilda_rel_filtered_accordion_html = create_recommendation_display_components_with_accordion(
            matilda_structured_response_rel_filtered, "MATILDA Empfehlungen (Relevanz gefiltert)", detailed_recommendations_rel_filtered
        )

        # Create anonymous comparison
        anonymous_comparison_html = create_anonymous_comparison(baseline_structured, matilda_structured)

        # Check if either response contains error
        status = f"Responses generated successfully using {selected_llm}"
        if "Error" in baseline_text or "Error" in matilda_text:
            status = "Some responses may be incomplete in the baseline or similarity-filtered MATILDA response."
        if "Error" in baseline_text or "Error" in matilda_text_unfiltered:
            status = "Some responses may be incomplete in the baseline or unfiltered MATILDA response."
        if "Error" in baseline_text or "Error" in matilda_text_rel_filtered:
            status = "Some responses may be incomplete in the baseline or relevance-filtered MATILDA response."

        # Log the responses (using JSON serialization for safe copying)
        print("## BASELINE RESPONSE: #######################################################################")
        print(f"\nBASELINE RESPONSE:\n {repr(baseline_structured)}\n")
        print("## MATILDA RESPONSE: #######################################################################")
        print(f"MATILDA RESPONSE:\n {repr(matilda_structured)}\n\n")
        print(f"MATILDA RESPONSE UNFILTERED:\n {repr(matilda_structured_unfiltered)}\n")
        print(f"MATILDA RESPONSE REL FILTERED:\n {repr(matilda_structured_response_rel_filtered)}\n")
        print("## MATILDA RESPONSE DETAILS: ###############################################################")
        print(f"MATILDA RESPONSE DETAILS:\n {repr(detailed_recommendations_filtered)}\n")
        print(f"MATILDA RESPONSE DETAILS UNFILTERED:\n {repr(detailed_recommendations_unfiltered)}\n")
        print(f"MATILDA RESPONSE DETAILS REL FILTERED:\n {repr(detailed_recommendations_rel_filtered)}\n")
        print("## MATILDA RECOMMENDATIONS: ################################################################")
        print(f"MATILDA RECOMMENDATIONS FILTERED:\n {repr(matilda_recommendations_filtered)}\n")
        print(f"MATILDA RECOMMENDATIONS UNFILTERED:\n {repr(matilda_recommendations_unfiltered)}\n")
        print(f"MATILDA RECOMMENDATIONS REL FILTERED:\n {repr(matilda_recommendations_rel_filtered)}\n")
        print("############################################################################################")

        return (
            status,
            matilda_recommendations_filtered_formatted,
            matilda_recommendations_unfiltered_formatted,
            matilda_recommendations_rel_filtered_formatted,
            baseline_accordion_html,
            matilda_accordion_html,
            matilda_filtered_accordion_html,
            matilda_unfiltered_accordion_html,
            matilda_rel_filtered_accordion_html,
            anonymous_comparison_html,
        )

    except requests.exceptions.RequestException as e:
        error_msg = f"Error calling API endpoints: {str(e)}"
        print(error_msg)
        error_html = f"<div style='color:red;padding:10px;border:1px solid red;border-radius:5px'>Error: {str(e)}</div>"
        return (
            f"Failed to generate responses with {selected_llm}. Please try again.",
            error_html,
            error_html,
            error_html,
            error_html,
            error_html,
            error_html,
            error_html,
            error_html,
            error_html,
        )
    except json.JSONDecodeError as e:
        error_msg = f"Error parsing JSON response: {str(e)}"
        print(error_msg)
        error_html = f"<div style='color:red;padding:10px;border:1px solid red;border-radius:5px'>JSON Error: {str(e)}</div>"
        return (
            f"Failed to parse response from {selected_llm}.",
            error_html,
            error_html,
            error_html,
            error_html,
            error_html,
            error_html,
            error_html,
            error_html,
        )


def format_matilda_recommendations(recommendations_data) -> str:
    """Format MATILDA recommendations data into nice HTML display"""
    # Handle different input types for backward compatibility
    if isinstance(recommendations_data, str):
        # Fallback for string data (old format)
        if not recommendations_data or recommendations_data.strip() == "":
            return "<p style='font-style:italic;color:#666;'>Keine MATILDA-Empfehlungen verfügbar</p>"
        return f"<div style='padding: 15px; border-radius: 8px;'><pre>{recommendations_data}</pre></div>"

    if not recommendations_data or not isinstance(recommendations_data, list):
        return "<p style='font-style:italic;color:#666;'>Keine MATILDA-Empfehlungen verfügbar</p>"

    html_parts = []
    html_parts.append("<div style='max-width: 100%; margin: 0 auto;'>")
    html_parts.append("<h3>MATILDA Empfehlungen</h3>")

    # Process each technology recommendation group
    for tech_group in recommendations_data:
        if not isinstance(tech_group, dict):
            continue

        old_technology = tech_group.get("technology", "Unbekannte Technologie")
        recommended_technologies = tech_group.get("recommended_technologies", [])

        if not recommended_technologies:
            continue

        # Create technology section
        html_parts.append(f"""
        <div style='border: 1px solid #e1e5e9; border-radius: 8px; margin: 15px 0; overflow: hidden;'>
            <div style='padding: 12px; border-bottom: 1px solid #e1e5e9;'>
                <h4 style='margin: 0;'>{old_technology}</h4>
                <small>Empfehlungen: {len(recommended_technologies)}</small>
            </div>
            <div style='padding: 0;'>
        """)

        # Create table for recommendations with all three metrics
        html_parts.append("<table style='width: 100%; border-collapse: collapse;'>")
        html_parts.append("""
        <tr>
            <th style='padding: 10px; border-bottom: 1px solid #dee2e6; text-align: center; width: 60px;'>Rang</th>
            <th style='padding: 10px; border-bottom: 1px solid #dee2e6; text-align: left;'>Technologie</th>
            <th style='padding: 10px; border-bottom: 1px solid #dee2e6; text-align: center; width: 120px;'>MATILDA Score</th>
            <th style='padding: 10px; border-bottom: 1px solid #dee2e6; text-align: center; width: 100px;'>Migration Rank</th>
            <th style='padding: 10px; border-bottom: 1px solid #dee2e6; text-align: center; width: 120px;'>Centrality</th>
        </tr>
        """)

        # Sort recommendations by matildaRecom_mig score (highest first)
        sorted_recommendations = sorted(recommended_technologies, key=lambda x: x.get("matildaRecom_mig", 0), reverse=True)

        for rank, rec in enumerate(sorted_recommendations[:10], 1):  # Show top 10
            technology = rec.get("technology", "Unbekannte Technologie")
            matilda_score = rec.get("matildaRecom_mig", 0)
            migrank = rec.get("migrank", 0)
            centrality = rec.get("strength_based_centrality", 0)

            # Color coding for ranks
            if rank == 1:
                rank_color = "#28a745"  # Green
                medal = "🥇"
            elif rank == 2:
                rank_color = "#17a2b8"  # Blue
                medal = "🥈"
            elif rank == 3:
                rank_color = "#6f42c1"  # Purple
                medal = "🥉"
            else:
                rank_color = "#6c757d"  # Gray
                medal = ""

            html_parts.append(f"""
            <tr style='border-bottom: 1px solid #dee2e6; transition: background-color 0.2s;'
                onmouseover="this.style.backgroundColor='#444444'"
                onmouseout="this.style.backgroundColor='transparent'">
                <td style='padding: 10px; text-align: center;'>
                    <span style='background-color: {rank_color}; color: white; padding: 4px 8px; border-radius: 50%; font-weight: bold; font-size: 12px;'>{rank}</span>
                </td>
                <td style='padding: 10px; font-weight: {600 if rank <= 3 else 400};'>
                    {technology} {medal}
                </td>
                <td style='padding: 10px; text-align: center; font-family: monospace;'>
                    <span style='background-color:rgba(40,167,69,{max(0.1, matilda_score * 2)});color:white;padding: 3px 6px; border-radius: 3px; font-weight: bold;'>
                        {matilda_score:.4f}
                    </span>
                </td>
                <td style='padding: 10px; text-align: center; font-family: monospace;'>
                    <span style='background-color:rgba(23,162,184,{max(0.1, migrank)});color:white;padding: 3px 6px; border-radius: 3px; font-weight: bold;'>
                        {migrank:.4f}
                    </span>
                </td>
                <td style='padding: 10px; text-align: center; font-family: monospace;'>
                    <span style='background-color:rgba(111,66,193,{max(0.1, centrality)});color:white;padding: 3px 6px; border-radius: 3px; font-weight: bold;'>
                        {centrality:.4f}
                    </span>
                </td>
            </tr>
            """)

        html_parts.append("</table>")
        html_parts.append("</div></div>")

    html_parts.append("</div>")
    return "".join(html_parts)


def format_matilda_recommendations_with_details(recommendations_data, title: str) -> str:
    """Format MATILDA recommendations with integrated accordion details"""
    # Handle different input types for backward compatibility
    if isinstance(recommendations_data, str):
        if not recommendations_data or recommendations_data.strip() == "":
            return f"<p style='font-style:italic;color:#666;'>Keine {title} verfügbar</p>"
        return f"<div style='padding: 15px; border-radius: 8px;'><h3>{title}</h3><pre>{recommendations_data}</pre></div>"

    if not recommendations_data or not isinstance(recommendations_data, list):
        return f"<p style='font-style:italic;color:#666;'>Keine {title} verfügbar</p>"

    html_parts = []
    html_parts.append(add_accordion_javascript())  # Add JavaScript
    html_parts.append("<div style='max-width: 100%; margin: 0 auto;'>")
    html_parts.append(f"<h3>{title}</h3>")

    # Process each technology recommendation group
    for tech_group in recommendations_data:
        if not isinstance(tech_group, dict):
            continue

        old_technology = tech_group.get("technology", "Unbekannte Technologie")
        recommended_technologies = tech_group.get("recommended_technologies", [])

        if not recommended_technologies:
            continue

        # Create technology section
        html_parts.append(f"""
        <div style='border: 1px solid #e1e5e9; border-radius: 8px; margin: 15px 0; overflow: hidden;'>
            <div style='padding: 12px; border-bottom: 1px solid #e1e5e9;'>
                <h4 style='margin: 0;'>{old_technology}</h4>
                <small>Empfehlungen: {len(recommended_technologies)}</small>
            </div>
            <div style='padding: 0;'>
        """)

        # Create table for recommendations
        html_parts.append("<table style='width: 100%; border-collapse: collapse;'>")
        html_parts.append("""
        <tr>
            <th style='padding: 10px; border-bottom: 1px solid #dee2e6; text-align: center; width: 60px;'>Rang</th>
            <th style='padding: 10px; border-bottom: 1px solid #dee2e6; text-align: left;'>Technologie</th>
            <th style='padding: 10px; border-bottom: 1px solid #dee2e6; text-align: center; width: 120px;'>MATILDA Score</th>
            <th style='padding: 10px; border-bottom: 1px solid #dee2e6; text-align: center; width: 100px;'>Migration Rank</th>
            <th style='padding: 10px; border-bottom: 1px solid #dee2e6; text-align: center; width: 120px;'>Centrality</th>
        </tr>
        """)

        # Sort recommendations by matildaRecom_mig score (highest first)
        sorted_recommendations = sorted(recommended_technologies, key=lambda x: x.get("matildaRecom_mig", 0), reverse=True)

        for rank, rec in enumerate(sorted_recommendations[:10], 1):  # Show top 10
            technology = rec.get("technology", "Unbekannte Technologie")
            category = rec.get("category", "Unknown")
            matilda_score = rec.get("matildaRecom_mig", 0)
            migrank = rec.get("migrank", 0)
            centrality = rec.get("strength_based_centrality", 0)

            # Get detailed information
            migration_stats = rec.get("migration_stats", {})
            project_insights = rec.get("project_age_insights", {})

            # Color coding for ranks
            if rank == 1:
                rank_color = "#28a745"  # Green
                medal = "🥇"
            elif rank == 2:
                rank_color = "#17a2b8"  # Blue
                medal = "🥈"
            elif rank == 3:
                rank_color = "#6f42c1"  # Purple
                medal = "🥉"
            else:
                rank_color = "#6c757d"  # Gray
                medal = ""

            # Generate unique ID for accordion
            accordion_id = f"details_{old_technology.replace(' ', '_').replace('/', '_').replace('-', '_')}_{technology.replace(' ', '_').replace('/', '_').replace('-', '_')}_{rank}"

            html_parts.append(f"""
            <tr style='border-bottom: 1px solid #dee2e6;'>
                <td style='padding: 10px; text-align: center;'>
                    <span style='background-color: {rank_color}; color: white; padding: 4px 8px; border-radius: 50%; font-weight: bold; font-size: 12px;'>{rank}</span>
                </td>
                <td style='padding: 10px; font-weight: {600 if rank <= 3 else 400};'>
                    <div style='display: flex; justify-content: space-between; align-items: center;'>
                        <div>
                            {technology} {medal}
                            <small style='color: #6c757d; font-size: 0.8em; margin-left: 8px;'>({category})</small>
                        </div>
                    </div>
                    <div id="{accordion_id}" style='display: none; margin-top: 10px; padding: 10px; border-radius: 6px; font-size: 0.9em;'>
                        <h6 style='margin: 0 0 8px 0;'> Migration Statistiken</h6>
                        <div style='display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 10px;'>
                            <div>➡️ <strong>Eingehend:</strong> {migration_stats.get("incoming_migrations", 0):,}</div>
                            <div>⬅️ <strong>Ausgehend:</strong> {migration_stats.get("outgoing_migrations", 0):,}</div>
                            <div style='color: {"#28a745" if migration_stats.get("net_migration", 0) > 0 else "#dc3545" if migration_stats.get("net_migration", 0) < 0 else "#6c757d"};'>
                                {"📈" if migration_stats.get("net_migration", 0) > 0 else "📉" if migration_stats.get("net_migration", 0) < 0 else "➡️"} <strong>Netto:</strong> {migration_stats.get("net_migration", 0):+,}
                            </div>
                            <div>🎯 <strong>Direkt von {old_technology}:</strong> {migration_stats.get("direct_from_source", 0)}</div>
                        </div>
                        <h6 style='margin: 8px 0 8px 0;'>⏰ Projekt-Alter</h6>
                        <div style='display: grid; grid-template-columns: 1fr 1fr; gap: 10px;'>
                            <div>📅 <strong>Ø Alter:</strong> {project_insights.get("avg_project_age_years", "N/A")} Jahre</div>
                            <div>🔄 <strong>Älteste:</strong> {project_insights.get("oldest_decision_years_ago", "N/A")} Jahre</div>
                            <div>🆕 <strong>Neueste:</strong> {project_insights.get("newest_decision_years_ago", "N/A")} Jahre</div>
                            <div>📈 <strong>Datenpunkte:</strong> {project_insights.get("sample_size", 0):,}</div>
                        </div>
                        <div style='margin-top: 8px; padding: 6px; background: #e9ecef; border-radius: 4px; font-size: 0.8em;'>
                            ⭐ <strong>Ø Ähnlichkeit der direkten Migrationen:</strong> {migration_stats.get("direct_similarity_avg", 0):.2f}
                        </div>
                    </div>
                </td>
                <td style='padding: 10px; text-align: center; font-family: monospace;'>
                    <span style='background-color:rgba(40,167,69,{max(0.1, matilda_score * 2)});color:white;padding: 3px 6px; border-radius: 3px; font-weight: bold;'>
                        {matilda_score:.4f}
                    </span>
                </td>
                <td style='padding: 10px; text-align: center; font-family: monospace;'>
                    <span style='background-color:rgba(23,162,184,{max(0.1, migrank)});color:white;padding: 3px 6px; border-radius: 3px; font-weight: bold;'>
                        {migrank:.4f}
                    </span>
                </td>
                <td style='padding: 10px; text-align: center; font-family: monospace;'>
                    <span style='background-color:rgba(111,66,193,{max(0.1, centrality)});color:white;padding: 3px 6px; border-radius: 3px; font-weight: bold;'>
                        {centrality:.4f}
                    </span>
                </td>
            </tr>
            """)

        html_parts.append("</table>")
        html_parts.append("</div></div>")

    html_parts.append("</div>")
    return "".join(html_parts)


def create_recommendation_display_components_with_accordion(
    data: Optional[dict], header: str, detailed_recommendations: list = None, show_score_column: bool = True
) -> str:
    """Create HTML components with accordion for detailed recommendations"""
    if not data or not data.get("recommendation_list"):
        return "<p style='font-style:italic;'>Keine strukturierten Empfehlungen verfügbar</p>"

    recommendation_list = data.get("recommendation_list", [])
    tech_count = len(recommendation_list)

    # Create a mapping from technology names to detailed stats if provided
    detailed_stats_map = {}
    if detailed_recommendations:
        for tech_group in detailed_recommendations:
            if isinstance(tech_group, dict) and "technology" in tech_group:
                old_tech = tech_group["technology"]
                recommended_techs = tech_group.get("recommended_technologies", [])
                for rec_tech in recommended_techs:
                    tech_name = rec_tech.get("technology", "")
                    if tech_name:
                        detailed_stats_map[f"{old_tech}_{tech_name}"] = rec_tech

    html_parts = []

    # Add CSS variables for better dark mode support
    html_parts.append("""
    <style>
    :root {
        --primary-color: #007bff;
        --secondary-color: #6c757d;
        --success-color: #28a745;
        --danger-color: #dc3545;
        --warning-color: #ffc107;
        --info-color: #17a2b8;
        --dark-color: #343a40;
        --border-color-primary: #dee2e6;
    }

    /* Dark mode adjustments */
    @media (prefers-color-scheme: dark) {
        :root {
            --border-color-primary: #495057;
        }
    }

    .recommendation-container {
        border: 1px solid var(--border-color-primary);
        margin: 10px 0;
        border-radius: 8px;
        overflow: hidden;
    }

    .recommendation-header {
        padding: 12px;
        border-bottom: 1px solid var(--border-color-primary);
    }

    .recommendation-content {
        padding: 15px;
    }

    .stats-container {
        margin: 10px 0;
        padding: 10px;
        border: 1px solid var(--border-color-primary);
        border-radius: 6px;
        font-size: 0.9em;
    }

    .detail-section {
        border-left: 4px solid var(--info-color);
        margin: 10px 0;
        padding: 10px;
        border-radius: 4px;
    }

    .accordion-content {
        padding: 10px;
        margin-top: 10px;
        border: 1px solid var(--border-color-primary);
        border-radius: 5px;
    }
    </style>
    """)

    html_parts.append(f"<h3> {header} ({tech_count} Technologien)</h3>")

    for _i, rec in enumerate(recommendation_list):
        old_tech = rec.get("old_technology", "Unknown")
        category = rec.get("category", "Unknown")
        overview = rec.get("recommendation_overview", [])
        details = rec.get("recommendation_details", [])

        # Technology header with overview
        html_parts.append(f"""
        <div class='recommendation-container'>
            <div class='recommendation-header'>
                <h4 style='margin:0'>{old_tech}</h4>
                <small>Kategorie: {category}</small>
            </div>
            <div class='recommendation-content'>
        """)

        # Overview table (always visible)
        if overview:
            html_parts.append("<h5> Empfehlungs-Übersicht:</h5>")
            html_parts.append("<table style='width:100%;border-collapse:collapse;margin-bottom:15px;'>")

            # Create table header based on whether to show score column
            if show_score_column:
                html_parts.append(
                    "<tr><th style='padding:8px;border:1px solid;'>Rang</th><th style='padding:8px;border:1px solid;'>Technologie</th><th style='padding:8px;border:1px solid;text-align:center;'>Score</th></tr>"
                )
            else:
                html_parts.append(
                    "<tr><th style='padding:8px;border:1px solid;'>Rang</th><th style='padding:8px;border:1px solid;'>Technologie</th></tr>"
                )

            for ov in overview:
                rank = ov.get("rank", "?")
                technology = ov.get("technology", "Unknown")

                # Try to find the score from detailed_stats_map only if showing score column
                score_display = ""
                if show_score_column:
                    stats_key = f"{old_tech}_{technology}"
                    detailed_stats = detailed_stats_map.get(stats_key, {})
                    score = detailed_stats.get("matildaRecom_mig", "")
                    score_display = f"{score:.3f}" if score and isinstance(score, (int, float)) and score > 0 else ""

                rank_badge_style = (
                    "background-color:var(--primary-color, #007bff);color:white"
                    if rank == 1
                    else "background-color:var(--secondary-color, #6c757d);color:white"
                    if rank == 2
                    else "background-color:var(--info-color, #17a2b8);color:white"
                    if rank == 3
                    else "background-color:var(--dark-color, #343a40);color:white"
                )

                # Create table row based on whether to show score column
                if show_score_column:
                    html_parts.append(f"""
                    <tr>
                        <td style='padding:8px;border:1px solid;text-align:center;'>
                            <span style='{rank_badge_style};padding:2px 6px;border-radius:50%;font-weight:bold;'>{rank}</span>
                        </td>
                        <td style='padding:8px;border:1px solid;'>{technology}</td>
                        <td style='padding:8px;border:1px solid;text-align:center;font-family:monospace;'>{score_display}</td>
                    </tr>
                    """)
                else:
                    html_parts.append(f"""
                    <tr>
                        <td style='padding:8px;border:1px solid;text-align:center;'>
                            <span style='{rank_badge_style};padding:2px 6px;border-radius:50%;font-weight:bold;'>{rank}</span>
                        </td>
                        <td style='padding:8px;border:1px solid;'>{technology}</td>
                    </tr>
                    """)
            html_parts.append("</table>")

        # Details in a collapsible section (HTML details/summary)
        if details:
            html_parts.append(f"""
            <details style='margin-top:15px;'>
                <summary style='cursor:pointer;padding:10px;border-radius:5px;font-weight:bold;'>
                     Detaillierte Empfehlungen anzeigen ({len(details)} Empfehlungen)
                </summary>
                <div class='accordion-content'>
            """)

            for detail in details:
                rank = detail.get("rank", "?")
                technology = detail.get("technology", "Unknown")
                description = detail.get("description", "Keine Beschreibung")
                reason = detail.get("reason", "Kein Grund angegeben")
                advantages = detail.get("advantages", "Keine Vorteile genannt")
                tradeoffs = detail.get("tradeoffs", "Keine Nachteile genannt")

                rank_color = (
                    "var(--success-color, #28a745)"
                    if rank == 1
                    else "var(--primary-color, #007bff)"
                    if rank == 2
                    else "var(--info-color, #17a2b8)"
                    if rank == 3
                    else "var(--secondary-color, #6c757d)"
                )
                rank_badge_style = f"background-color:{rank_color};color:white"

                # Look for detailed statistics for this technology
                stats_key = f"{old_tech}_{technology}"
                detailed_stats = detailed_stats_map.get(stats_key, {})
                migration_stats = detailed_stats.get("migration_stats", {})
                project_insights = detailed_stats.get("project_age_insights", {})
                matilda_score = detailed_stats.get("matildaRecom_mig", 0)

                html_parts.append(f"""
                <div class='detail-section' style='border-left-color:{rank_color};'>
                    <h6 style='margin:0 0 8px 0;'>
                        <span style='{rank_badge_style};padding:2px 6px;border-radius:50%;margin-right:5px;'>{rank}</span>
                        {technology}
                        {f" (Score: {matilda_score:.3f})" if matilda_score > 0 else ""}
                    </h6>
                    <p style='margin:5px 0;'><strong>Beschreibung:</strong> {description}</p>
                    <p style='margin:5px 0;'><strong>Begründung:</strong> {reason}</p>
                """)

                # Add migration statistics if available
                if migration_stats:
                    html_parts.append(f"""
                    <div class='stats-container'>
                        <h6 style='margin:0 0 8px 0;'> Migration Statistiken</h6>
                        <div style='display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-bottom:8px;'>
                            <div>➡️ <strong>Eingehend:</strong> {migration_stats.get("incoming_migrations", 0):,}</div>
                            <div>⬅️ <strong>Ausgehend:</strong> {migration_stats.get("outgoing_migrations", 0):,}</div>
                            <div style='color:{"var(--success-color, #28a745)" if migration_stats.get("net_migration", 0) > 0 else "var(--danger-color, #dc3545)" if migration_stats.get("net_migration", 0) < 0 else "inherit"};'>
                                {"📈" if migration_stats.get("net_migration", 0) > 0 else "📉" if migration_stats.get("net_migration", 0) < 0 else "➡️"} <strong>Netto:</strong> {migration_stats.get("net_migration", 0):+,}
                            </div>
                            <div>🎯 <strong>Direkt von {old_tech}:</strong> {migration_stats.get("direct_from_source", 0)}</div>
                        </div>
                        <div style='text-align:center;padding:4px;border:1px solid var(--border-color-primary, #dee2e6);border-radius:4px;font-size:0.8em;'>
                            ⭐ <strong>Ø Ähnlichkeit:</strong> {migration_stats.get("direct_similarity_avg", 0):.2f}
                        </div>
                    </div>
                    """)

                # Add project age insights if available
                if project_insights:
                    html_parts.append(f"""
                    <div class='stats-container'>
                        <h6 style='margin:0 0 8px 0;'>⏰ Projekt-Alter</h6>
                        <div style='display:grid;grid-template-columns:1fr 1fr;gap:8px;'>
                            <div>📅 <strong>Ø Alter:</strong> {project_insights.get("avg_project_age_years", "N/A")} Jahre</div>
                            <div>🔄 <strong>Älteste:</strong> {project_insights.get("oldest_decision_years_ago", "N/A")} Jahre</div>
                            <div>🆕 <strong>Neueste:</strong> {project_insights.get("newest_decision_years_ago", "N/A")} Jahre</div>
                            <div>📈 <strong>Datenpunkte:</strong> {project_insights.get("sample_size", 0):,}</div>
                        </div>
                    </div>
                    """)

                # Add LLM advantages/tradeoffs
                html_parts.append(f"""
                    <div style='display:flex;gap:15px;margin-top:10px;'>
                        <div style='flex:1;padding:8px;border-radius:4px;border-left:3px solid var(--success-color, #4CAF50);'>
                            <strong>✅ Vorteile:</strong><br>{advantages}
                        </div>
                        <div style='flex:1;padding:8px;border-radius:4px;border-left:3px solid var(--warning-color, #FF9800);'>
                            <strong>⚠️ Nachteile:</strong><br>{tradeoffs}
                        </div>
                    </div>
                </div>
                """)

            html_parts.append("</div></details>")

        html_parts.append("</div></div>")

    return "".join(html_parts)


def process_pom_file(file_obj):
    """
    Process uploaded pom.xml file and extract dependencies

    Args:
        file_obj: Uploaded file object

    Returns:
        Tuple of (dependencies list, formatted display string)
    """
    if file_obj is None:
        return [], "No file uploaded"

    try:
        # Parse the pom.xml content
        dependencies = parse_pom_xml(file_obj.decode("utf-8"))

        # Format for display
        display_text = f"Extracted {len(dependencies)} dependencies:\n"
        for i, dep in enumerate(dependencies, 1):
            display_text += f"{i}. {dep}\n"

        print(f"Successfully parsed POM file, found {len(dependencies)} dependencies")
        return dependencies, display_text

    except Exception as e:
        error_msg = f"Error processing POM file: {str(e)}"
        print(error_msg)
        return [], f"Error: {error_msg}"


def create_anonymous_comparison(baseline_data: dict, matilda_data: dict) -> str:
    """Create an anonymous comparison of recommendations without colors or scores"""
    html_parts = []

    # Extract recommendation lists from both datasets
    baseline_recommendations = baseline_data.get("recommendation_list", [])
    matilda_recommendations = matilda_data.get("recommendation_list", [])

    html_parts.append("""
    <div style='display: flex; gap: 20px; width: 100%;'>
        <div style='flex: 1; border: 1px solid #ddd; border-radius: 8px; padding: 20px;'>
            <h3 style='margin-top: 0; text-align: center;'> Empfehlungssystem 🅰️ </h3>
            <div style='padding: 10px;'>
    """)

    # Process baseline recommendations (Ansatz A)
    for rec in baseline_recommendations:
        old_tech = rec.get("old_technology", "Unknown")
        category = rec.get("category", "Unknown")
        overview = rec.get("recommendation_overview", [])

        if overview:
            html_parts.append(f"""
                <h2>{category}</h2>
                <h4>{old_tech}</h4>
                <ol style='margin: 10px 0; padding-left: 20px;'>
            """)

            # Show top 5 recommendations
            for item in overview[:5]:
                tech_name = item.get("technology", "Unknown")
                html_parts.append(f"<li>{tech_name}</li>")

            html_parts.append("</ol>")

    html_parts.append("""
            </div>
        </div>

        <div style='flex: 1; border: 1px solid #ddd; border-radius: 8px; padding: 20px;'>
            <h3 style='margin-top: 0; text-align: center;'> Empfehlungssystem 🅱️ </h3>
            <div style='padding: 10px;'>
    """)

    # Process MATILDA recommendations (Ansatz B)
    for rec in matilda_recommendations:
        old_tech = rec.get("old_technology", "Unknown")
        category = rec.get("category", "Unknown")
        overview = rec.get("recommendation_overview", [])

        if overview:
            html_parts.append(f"""
                <h2>{category}</h2>
                <h4>{old_tech}</h4>
                <ol style='margin: 10px 0; padding-left: 20px;'>
            """)

            # Show top 5 recommendations
            for item in overview[:5]:
                tech_name = item.get("technology", "Unknown")
                html_parts.append(f"<li>{tech_name}</li>")

            html_parts.append("</ol>")

    html_parts.append("""
            </div>
        </div>
    </div>
    """)

    return "".join(html_parts)


# Add JavaScript for accordion functionality
def add_accordion_javascript() -> str:
    """Add JavaScript for accordion functionality"""
    return """
    <script>
    function toggleDetails(elementId) {
        var element = document.getElementById(elementId);
        if (element.style.display === 'none' || element.style.display === '') {
            element.style.display = 'block';
        } else {
            element.style.display = 'none';
        }
    }
    </script>
    """
