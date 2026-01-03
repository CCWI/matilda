import json
import logging
from typing import Dict, List, Union

logger = logging.getLogger(__name__)


def generate_css_styles() -> str:
    """Generate CSS styles for the HTML output."""
    return """
    <style>
        .matilda-container {
            font-family: Arial, sans-serif;
            max-width: 100%;
            margin: 0 auto;
            padding: 20px;
        }
        .matilda-tech-card {
            background-color: #f9f9f9;
            border-radius: 8px;
            padding: 15px;
            margin-bottom: 25px;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }
        .matilda-tech-header {
            display: flex;
            justify-content: space-between;
            border-bottom: 1px solid #ddd;
            padding-bottom: 10px;
            margin-bottom: 15px;
        }
        .matilda-tech-name {
            font-size: 18px;
            font-weight: bold;
            color: #333;
        }
        .matilda-tech-category {
            font-size: 14px;
            color: #666;
            padding: 3px 8px;
            background-color: #eee;
            border-radius: 15px;
        }
        .matilda-recommendation {
            margin-bottom: 20px;
        }
        .matilda-recommendation-list {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin-bottom: 15px;
        }
        .matilda-rec-badge {
            padding: 5px 10px;
            border-radius: 20px;
            font-weight: bold;
            color: white;
            display: inline-flex;
            align-items: center;
        }
        .matilda-rec-badge.rank-1 {
            background-color: #4CAF50;
        }
        .matilda-rec-badge.rank-2 {
            background-color: #2196F3;
        }
        .matilda-rec-badge.rank-3 {
            background-color: #9C27B0;
        }
        .matilda-rec-badge.other {
            background-color: #607D8B;
        }
        .matilda-rank-num {
            background: rgba(255,255,255,0.3);
            border-radius: 50%;
            width: 18px;
            height: 18px;
            display: inline-flex;
            justify-content: center;
            align-items: center;
            margin-right: 5px;
            font-size: 12px;
        }
        .matilda-details {
            border-left: 3px solid #ddd;
            padding-left: 15px;
            margin-bottom: 15px;
        }
        .matilda-details h4 {
            margin: 10px 0 5px 0;
            font-size: 16px;
        }
        .matilda-details p {
            margin: 0 0 10px 0;
            font-size: 14px;
            color: #333;
        }
        .matilda-pros-cons {
            display: flex;
            gap: 20px;
            margin-top: 10px;
        }
        .matilda-pros, .matilda-cons {
            flex: 1;
            padding: 10px;
            border-radius: 5px;
        }
        .matilda-pros {
            background-color: rgba(76, 175, 80, 0.1);
            border-left: 3px solid #4CAF50;
        }
        .matilda-cons {
            background-color: rgba(244, 67, 54, 0.1);
            border-left: 3px solid #F44336;
        }
        .matilda-error {
            color: #721c24;
            background-color: #f8d7da;
            padding: 10px;
            border-radius: 5px;
            margin-bottom: 10px;
        }
        .matilda-tech-icon {
            margin-right: 10px;
        }
    </style>
    """


def get_tech_icon(tech_name: str) -> str:
    """Get an icon for a technology based on its name."""
    tech_lower = tech_name.lower()

    icons = {
        "postgresql": "🐘",
        "mysql": "🐬",
        "mongodb": "🍃",
        "mariadb": "🐬",
        "oracle": "☁️",
        "wildfly": "🔥",
        "tomcat": "🐱",
        "jetty": "🚀",
        "spring": "🍃",
        "spring boot": "🍃",
        "hibernate": "🐻",
        "java": "☕",
        "python": "🐍",
        "javascript": "📜",
        "typescript": "📘",
        "react": "⚛️",
        "angular": "🅰️",
        "vue": "🖖",
        "docker": "🐳",
        "kubernetes": "☸️",
        "aws": "☁️",
        "azure": "☁️",
        "gcp": "☁️",
        "git": "📊",
    }

    # Look for partial matches
    for key, icon in icons.items():
        if key in tech_lower:
            return icon

    # Default icon if no match
    return "🔧"


def convert_json_to_html(json_data: Union[str, Dict, List]) -> str:
    """Convert JSON response to formatted HTML."""
    try:
        # Parse JSON if it's a string
        if isinstance(json_data, str):
            data = json.loads(json_data)
        else:
            data = json_data

        # If it's not a recommendation list structure, render as prettified JSON
        if not isinstance(data, dict) or "recommendation_list" not in data:
            return f"<pre>{json.dumps(data, indent=2)}</pre>"

        html_output = generate_css_styles() + "<div class='matilda-container'>"

        recommendation_list = data.get("recommendation_list", [])
        if not recommendation_list:
            html_output += "<div class='matilda-error'>No recommendations available.</div>"
            return html_output + "</div>"

        # Process each technology recommendation
        for tech_item in recommendation_list:
            old_tech = tech_item.get("old_technology", "Unknown")
            category = tech_item.get("category", "Technology")
            recs_overview = tech_item.get("recommendation_overview", [])
            recs_details = tech_item.get("recommendation_details", [])

            html_output += f"""
            <div class='matilda-tech-card'>
                <div class='matilda-tech-header'>
                    <div class='matilda-tech-name'>
                        {get_tech_icon(old_tech)} {old_tech}
                    </div>
                    <div class='matilda-tech-category'>{category}</div>
                </div>
            """

            # Recommendations overview
            html_output += "<div class='matilda-recommendation'>"
            html_output += "<h3>Empfohlene Alternativen:</h3>"
            html_output += "<div class='matilda-recommendation-list'>"

            # Add badges for recommendations
            for rec in recs_overview:
                rank = rec.get("rank", 999)
                tech = rec.get("technology", "Unknown")
                rank_class = f"rank-{rank}" if rank <= 3 else "other"
                html_output += f"""
                <div class='matilda-rec-badge {rank_class}'>
                    <span class='matilda-rank-num'>{rank}</span>
                    {get_tech_icon(tech)} {tech}
                </div>
                """

            html_output += "</div>"  # Close recommendation-list

            # Detailed recommendations
            if recs_details:
                for rec_detail in recs_details:
                    rank = rec_detail.get("rank", 0)
                    tech = rec_detail.get("technology", "Unknown")
                    desc = rec_detail.get("description", "")
                    reason = rec_detail.get("reason", "")
                    advantages = rec_detail.get("advantages", "")
                    tradeoffs = rec_detail.get("tradeoffs", "")

                    rank_class = f"rank-{rank}" if rank <= 3 else "other"

                    html_output += f"""
                    <div class='matilda-details'>
                        <div class='matilda-rec-badge {rank_class}' style='display:inline-block;margin-bottom:10px'>
                            <span class='matilda-rank-num'>{rank}</span>
                            {get_tech_icon(tech)} {tech}
                        </div>
                        <p><strong>{desc}</strong></p>
                        <p>{reason}</p>

                        <div class='matilda-pros-cons'>
                            <div class='matilda-pros'>
                                <h4>Vorteile</h4>
                                <p>{advantages}</p>
                            </div>
                            <div class='matilda-cons'>
                                <h4>Nachteile</h4>
                                <p>{tradeoffs}</p>
                            </div>
                        </div>
                    </div>
                    """

            html_output += "</div>"
            html_output += "</div>"

        html_output += "</div>"
        return html_output

    except Exception as e:
        logger.error(f"Error converting JSON to HTML: {e}", exc_info=True)
        return f"<div class='matilda-error'>Error formatting response: {str(e)}</div>"
