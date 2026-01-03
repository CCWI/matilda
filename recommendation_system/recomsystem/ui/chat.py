import ast
import logging
import os

import gradio as gr

from recomsystem.config.default_structured_output import (
    demo_default_baseline,
    demo_default_matilda,
    demo_default_matilda_details,
    demo_default_matilda_details_rel,
    demo_default_matilda_details_unf,
    demo_default_matilda_recom,
    demo_default_matilda_recom_rel,
    demo_default_matilda_recom_unf,
    demo_default_matilda_rel,
    demo_default_matilda_unf,
    uc1_default_baseline,
    uc1_default_matilda,
    uc1_default_matilda_details,
    uc1_default_matilda_details_rel,
    uc1_default_matilda_details_unf,
    uc1_default_matilda_recom,
    uc1_default_matilda_recom_rel,
    uc1_default_matilda_recom_unf,
    uc1_default_matilda_rel,
    uc1_default_matilda_unf,
    uc2_default_baseline,
    uc2_default_matilda,
    uc2_default_matilda_details,
    uc2_default_matilda_details_rel,
    uc2_default_matilda_details_unf,
    uc2_default_matilda_recom,
    uc2_default_matilda_recom_rel,
    uc2_default_matilda_recom_unf,
    uc2_default_matilda_rel,
    uc2_default_matilda_unf,
    uc3_default_baseline,
    uc3_default_matilda,
    uc3_default_matilda_details,
    uc3_default_matilda_details_rel,
    uc3_default_matilda_details_unf,
    uc3_default_matilda_recom,
    uc3_default_matilda_recom_rel,
    uc3_default_matilda_recom_unf,
    uc3_default_matilda_rel,
    uc3_default_matilda_unf,
)
from recomsystem.config.logging_config import setup_logging
from recomsystem.config.predefined_prompts_for_usecases import prompt_uc1, prompt_uc2, prompt_uc3
from recomsystem.config.supported_llms import LLMs
from recomsystem.config.szenarios import szenario_1, szenario_2, szenario_3
from recomsystem.ui.chat_handler import (
    call_api_endpoints,
    create_anonymous_comparison,
    create_recommendation_display_components_with_accordion,
    format_matilda_recommendations,
    process_pom_file,
)
from recomsystem.ui.css import custom_css

logger = logging.getLogger(__name__)

LOGO_PATH = "assets/logo_2.png"

if not os.path.exists(LOGO_PATH):
    logger.warning(f"Logo file not found at {LOGO_PATH}. Logo will not be displayed.")


def create_gradio_interface():
    with gr.Blocks(theme=gr.themes.Monochrome(), fill_width=True, css=custom_css) as demo:
        # Logo and title in centered containers
        with gr.Row(elem_classes=["logo-container"]):
            if os.path.exists(LOGO_PATH):
                gr.Image(LOGO_PATH, show_label=False, container=False, height=120)
            else:
                gr.Markdown("## MATILDA")

        with gr.Row(elem_classes=["title-container"]):
            gr.Markdown(
                """<div style='text-align: center; font-style: italic; margin: 0 auto; width: 100%;'>
                Machine-Learning Advisor To Identify Library-related Decision Alternatives
                </div>""",
                elem_classes=["centered-title"],
            )

        available_llms = list(LLMs.keys())
        dependencies_state = gr.State([])

        # LLM selection dropdown
        with gr.Accordion("Konfiguration", open=False):
            with gr.Row():
                llm_choice = gr.Dropdown(
                    choices=available_llms,
                    label="Wähle ein Modell",
                    info="Wählen Sie das KI-Modell aus, das für die Antworten verwendet werden soll",
                    value="GEMINI_2_5_FLASH",
                )

            # Add file upload section
            with gr.Accordion("Upload Maven pom.xml", open=False):
                with gr.Row():
                    file_upload = gr.File(label="Upload pom.xml", file_types=[".xml"], file_count="single", type="binary")
                    parse_button = gr.Button("Starte Technologie-Analyse")

                dependencies_display = gr.TextArea(label="Extrahierte Abhängigkeiten", interactive=False, lines=5)
                parse_button.click(fn=process_pom_file, inputs=[file_upload], outputs=[dependencies_state, dependencies_display])

        with gr.Tabs():
            #######################################################################################################################
            with gr.Tab("Demo"):
                input_text_test = gr.Textbox(
                    label="Prompt:",
                    lines=4,
                    value="""What is the best UI Framework for data visualization in Java?
I was using JavaFX, but it is not supported anymore like it was earlier and I am open to alternatives.""",
                )
                btn_test = gr.Button("Sende Anfrage an LLM")
                status_text_test = gr.Textbox(value="...", label="Status", interactive=False)

                # Section 1: Anonyme Vergleichsansicht (ohne Farben und Scores)
                gr.HTML(
                    "<h2 style='text-align: center; margin: 30px 0 20px 0; padding: 15px; border: 2px solid #6c757d; border-radius: 8px; background: transparent;'>🔍 Anonyme Vergleichsansicht</h2>"
                )

                anonymous_comparison_uc1 = gr.HTML(
                    value=create_anonymous_comparison(ast.literal_eval(uc1_default_baseline), ast.literal_eval(uc1_default_matilda))
                )

                # Section 2: Baseline vs MATILDA mit Accordion
                gr.HTML(
                    "<h2 style='text-align: center; margin: 30px 0 20px 0; padding: 15px; border: 2px solid var(--primary-color, #007bff); border-radius: 8px; background: transparent;'>📊 Vergleich: Baseline vs. MATILDA Empfehlungen</h2>"
                )

                min_width = 400
                # Use accordion components for baseline vs MATILDA comparison
                with gr.Row():
                    with gr.Column(scale=1, min_width=min_width):
                        baseline_accordion_demo = gr.HTML(
                            value=create_recommendation_display_components_with_accordion(
                                ast.literal_eval(demo_default_baseline), "Baseline Empfehlungen", show_score_column=False
                            )
                        )
                    with gr.Column(scale=1, min_width=min_width):
                        matilda_accordion_demo = gr.HTML(
                            value=create_recommendation_display_components_with_accordion(
                                ast.literal_eval(demo_default_matilda),
                                "MATILDA Empfehlungen",
                                detailed_recommendations=demo_default_matilda_details,
                            )
                        )

                # Section 2: MATILDA Filtered vs Unfiltered mit Accordion
                gr.HTML(
                    "<h2 style='text-align: center; margin: 40px 0 20px 0; padding: 15px; border: 2px solid var(--info-color, #17a2b8); border-radius: 8px; background: transparent;'>🔍 Vergleich: MATILDA Gefiltert vs. Ungefiltert</h2>"
                )

                # Use accordion components for MATILDA filtered vs unfiltered comparison
                with gr.Row():
                    with gr.Column(scale=1, min_width=min_width):
                        matilda_filtered_accordion_demo = gr.HTML(
                            value=create_recommendation_display_components_with_accordion(
                                ast.literal_eval(demo_default_matilda),
                                "MATILDA Empfehlungen (Gefiltert)",
                                detailed_recommendations=demo_default_matilda_details,
                            )
                        )
                    with gr.Column(scale=1, min_width=min_width):
                        matilda_unfiltered_accordion_demo = gr.HTML(
                            value=create_recommendation_display_components_with_accordion(
                                ast.literal_eval(demo_default_matilda_unf),
                                "MATILDA Empfehlungen (Ungefiltert)",
                                detailed_recommendations=demo_default_matilda_details_unf,
                            )
                        )
                    with gr.Column(scale=1, min_width=min_width):
                        matilda_rel_filtered_accordion_demo = gr.HTML(
                            value=create_recommendation_display_components_with_accordion(
                                ast.literal_eval(demo_default_matilda_rel),
                                "MATILDA Empfehlungen (Relevanzgefiltert)",
                                detailed_recommendations=demo_default_matilda_details_rel,
                            )
                        )

                # Section 3: MATILDA Empfehlungen Details mit Accordion
                gr.HTML(
                    "<h2 style='text-align: center; margin: 40px 0 20px 0; padding: 15px; border: 2px solid var(--secondary-color, #6c757d); border-radius: 8px; background: transparent;'>📋 MATILDA Empfehlungs-Details</h2>"
                )

                # Add MATILDA recommendations after all other recommendations - both filtered and unfiltered
                with gr.Row():
                    with gr.Column(scale=1, min_width=min_width):
                        matilda_recommendations_filtered_test = gr.HTML(
                            label="MATILDA Empfehlungen (Gefiltert):",
                            value=format_matilda_recommendations(demo_default_matilda_recom),
                        )
                    with gr.Column(scale=1, min_width=min_width):
                        matilda_recommendations_unfiltered_test = gr.HTML(
                            label="MATILDA Empfehlungen (Ungefiltert):",
                            value=format_matilda_recommendations(demo_default_matilda_recom_unf),
                        )
                    with gr.Column(scale=1, min_width=min_width):
                        matilda_recommendations_rel_filtered_test = gr.HTML(
                            label="MATILDA Empfehlungen (Relevanzgefiltert):",
                            value=format_matilda_recommendations(demo_default_matilda_recom_rel),
                        )

                btn_test.click(
                    fn=call_api_endpoints,
                    inputs=[input_text_test, llm_choice, dependencies_state],
                    outputs=[
                        status_text_test,
                        matilda_recommendations_filtered_test,
                        matilda_recommendations_unfiltered_test,
                        matilda_recommendations_rel_filtered_test,
                        baseline_accordion_demo,
                        matilda_accordion_demo,
                        matilda_filtered_accordion_demo,
                        matilda_unfiltered_accordion_demo,
                        matilda_rel_filtered_accordion_demo,
                        anonymous_comparison_uc1,
                    ],
                )
                input_text_test.submit(
                    fn=call_api_endpoints,
                    inputs=[input_text_test, llm_choice, dependencies_state],
                    outputs=[
                        status_text_test,
                        matilda_recommendations_filtered_test,
                        matilda_recommendations_unfiltered_test,
                        matilda_recommendations_rel_filtered_test,
                        baseline_accordion_demo,
                        matilda_accordion_demo,
                        matilda_filtered_accordion_demo,
                        matilda_unfiltered_accordion_demo,
                        matilda_rel_filtered_accordion_demo,
                        anonymous_comparison_uc1,
                    ],
                )


            ##############################################################################################################
            with gr.Tab("Anwendungsfall 1"):
                with gr.Row():
                    with gr.Column():
                        gr.HTML(szenario_1, elem_classes=["scenario-card"])

                input_text_uc1 = gr.Textbox(label="Query:", lines=8, value=prompt_uc1)
                btn_uc1 = gr.Button("Sende Anfrage an LLM")
                status_text_uc1 = gr.Textbox(value="...", label="Status", interactive=False)

                ##################################################################################################################################################
                # Section 1: Baseline vs MATILDA mit Accordion
                ##################################################################################################################################################
                with gr.Accordion("Vergleich mit Scoring", open=False):
                    gr.HTML(
                        "<h2 style='text-align: center; margin: 30px 0 20px 0; padding: 15px; border: 2px solid var(--primary-color, #007bff); border-radius: 8px; background: transparent;'>📊 Vergleich: Baseline vs. MATILDA Empfehlungen</h2>"
                    )

                    # Use accordion components for baseline vs MATILDA comparison
                    with gr.Row():
                        with gr.Column(scale=1, min_width=300):
                            baseline_accordion_uc1 = gr.HTML(
                                value=create_recommendation_display_components_with_accordion(
                                    ast.literal_eval(uc1_default_baseline), "Baseline Empfehlungen", show_score_column=False
                                )
                            )
                        with gr.Column(scale=1, min_width=300):
                            matilda_accordion_uc1 = gr.HTML(
                                value=create_recommendation_display_components_with_accordion(
                                    ast.literal_eval(uc1_default_matilda),
                                    "MATILDA Empfehlungen",
                                    detailed_recommendations=uc1_default_matilda_details,
                                )
                            )

                ##################################################################################################################################################
                # Section 2: MATILDA Filtered vs Unfiltered mit Accordion ########################################################################################
                ##################################################################################################################################################
                with gr.Accordion("Vergleich Filtered vs Unfiltered", open=False):
                    gr.HTML(
                        "<h2 style='text-align: center; margin: 40px 0 20px 0; padding: 15px; border: 2px solid var(--info-color, #17a2b8); border-radius: 8px; background: transparent;'>🔍 Vergleich: MATILDA Gefiltert vs. Ungefiltert</h2>"
                    )

                    # Use accordion components for MATILDA filtered vs unfiltered comparison
                    with gr.Row():
                        with gr.Column(scale=1, min_width=min_width):
                            matilda_filtered_accordion_uc1 = gr.HTML(
                                value=create_recommendation_display_components_with_accordion(
                                    ast.literal_eval(uc1_default_matilda),
                                    "MATILDA Empfehlungen (Gefiltert)",
                                    detailed_recommendations=uc1_default_matilda_details,
                                )
                            )
                        with gr.Column(scale=1, min_width=min_width):
                            matilda_unfiltered_accordion_uc1 = gr.HTML(
                                value=create_recommendation_display_components_with_accordion(
                                    ast.literal_eval(uc1_default_matilda_unf),
                                    "MATILDA Empfehlungen (Ungefiltert)",
                                    detailed_recommendations=uc1_default_matilda_details_unf,
                                )
                            )
                        with gr.Column(scale=1, min_width=min_width):
                            matilda_rel_filtered_accordion_uc1 = gr.HTML(
                                value=create_recommendation_display_components_with_accordion(
                                    ast.literal_eval(uc1_default_matilda_rel),
                                    "MATILDA Empfehlungen (Relevanzgefiltert)",
                                    detailed_recommendations=uc1_default_matilda_details_rel,
                                )
                            )

                ##################################################################################################################################################
                # Section 3: MATILDA Empfehlungen Details mit Accordion ##########################################################################################
                ##################################################################################################################################################
                with gr.Accordion("Empfehlungsdetails im Vergleich", open=False):
                    gr.HTML(
                        "<h2 style='text-align: center; margin: 40px 0 20px 0; padding: 15px; border: 2px solid var(--secondary-color, #6c757d); border-radius: 8px; background: transparent;'>📋 MATILDA Empfehlungs-Details</h2>"
                    )

                    # Regular MATILDA recommendations display (for API updates)
                    with gr.Row():
                        with gr.Column(scale=1, min_width=min_width):
                            matilda_recommendations_filtered_uc1 = gr.HTML(
                                label="MATILDA Empfehlungen (Gefiltert):",
                                value=format_matilda_recommendations(uc1_default_matilda_recom),
                            )
                        with gr.Column(scale=1, min_width=min_width):
                            matilda_recommendations_unfiltered_uc1 = gr.HTML(
                                label="MATILDA Empfehlungen (Ungefiltert):",
                                value=format_matilda_recommendations(uc1_default_matilda_recom_unf),
                            )
                        with gr.Column(scale=1, min_width=min_width):
                            matilda_recommendations_rel_filtered_uc1 = gr.HTML(
                                label="MATILDA Empfehlungen (Relevanzgefiltert):",
                                value=format_matilda_recommendations(uc1_default_matilda_recom_rel),
                            )

                btn_uc1.click(
                    fn=call_api_endpoints,
                    inputs=[input_text_uc1, llm_choice, dependencies_state],
                    outputs=[
                        status_text_uc1,
                        matilda_recommendations_filtered_uc1,
                        matilda_recommendations_unfiltered_uc1,
                        matilda_recommendations_rel_filtered_uc1,
                        baseline_accordion_uc1,
                        matilda_accordion_uc1,
                        matilda_filtered_accordion_uc1,
                        matilda_unfiltered_accordion_uc1,
                        matilda_rel_filtered_accordion_uc1,
                    ],
                )
                input_text_uc1.submit(
                    fn=call_api_endpoints,
                    inputs=[input_text_uc1, llm_choice, dependencies_state],
                    outputs=[
                        status_text_uc1,
                        matilda_recommendations_filtered_uc1,
                        matilda_recommendations_unfiltered_uc1,
                        matilda_recommendations_rel_filtered_uc1,
                        baseline_accordion_uc1,
                        matilda_accordion_uc1,
                        matilda_filtered_accordion_uc1,
                        matilda_unfiltered_accordion_uc1,
                        matilda_rel_filtered_accordion_uc1,
                    ],
                )

            ##############################################################################################################
            with gr.Tab("Anwendungsfall 2"):
                with gr.Row():
                    with gr.Column():
                        gr.HTML(szenario_2, elem_classes=["scenario-card"])

                input_text_uc2 = gr.Textbox(label="Query:", lines=10, value=prompt_uc2)
                btn_uc2 = gr.Button("Sende Anfrage an LLM")
                status_text_uc2 = gr.Textbox(value="...", label="Status", interactive=False)

                ##################################################################################################################################################
                # Section 1: Baseline vs MATILDA mit Accordion ###################################################################################################
                ##################################################################################################################################################
                with gr.Accordion("Vergleich mit Scoring", open=False):
                    gr.HTML(
                        "<h2 style='text-align: center; margin: 30px 0 20px 0; padding: 15px; border: 2px solid var(--primary-color, #007bff); border-radius: 8px; background: transparent;'>📊 Vergleich: Baseline vs. MATILDA Empfehlungen</h2>"
                    )

                    # Use accordion components for baseline vs MATILDA comparison
                    with gr.Row():
                        with gr.Column(scale=1, min_width=300):
                            baseline_accordion_uc2 = gr.HTML(
                                value=create_recommendation_display_components_with_accordion(
                                    ast.literal_eval(uc2_default_baseline), "Baseline Empfehlungen", show_score_column=False
                                )
                            )
                        with gr.Column(scale=1, min_width=300):
                            matilda_accordion_uc2 = gr.HTML(
                                value=create_recommendation_display_components_with_accordion(
                                    ast.literal_eval(uc2_default_matilda),
                                    "MATILDA Empfehlungen",
                                    detailed_recommendations=uc2_default_matilda_details,
                                )
                            )

                ##################################################################################################################################################
                # Section 2: MATILDA Filtered vs Unfiltered mit Accordion ########################################################################################
                ##################################################################################################################################################
                with gr.Accordion("Vergleich Filtered vs Unfiltered", open=False):
                    gr.HTML(
                        "<h2 style='text-align: center; margin: 40px 0 20px 0; padding: 15px; border: 2px solid var(--info-color, #17a2b8); border-radius: 8px; background: transparent;'>🔍 Vergleich: MATILDA Gefiltert vs. Ungefiltert</h2>"
                    )

                    # Use accordion components for MATILDA filtered vs unfiltered comparison
                    with gr.Row():
                        with gr.Column(scale=1, min_width=min_width):
                            matilda_filtered_accordion_uc2 = gr.HTML(
                                value=create_recommendation_display_components_with_accordion(
                                    ast.literal_eval(uc2_default_matilda),
                                    "MATILDA Empfehlungen (Gefiltert)",
                                    detailed_recommendations=uc2_default_matilda_details,
                                )
                            )
                        with gr.Column(scale=1, min_width=min_width):
                            matilda_unfiltered_accordion_uc2 = gr.HTML(
                                value=create_recommendation_display_components_with_accordion(
                                    ast.literal_eval(uc2_default_matilda_unf),
                                    "MATILDA Empfehlungen (Ungefiltert)",
                                    detailed_recommendations=uc2_default_matilda_details_unf,
                                )
                            )
                        with gr.Column(scale=1, min_width=min_width):
                            matilda_rel_filtered_accordion_uc2 = gr.HTML(
                                value=create_recommendation_display_components_with_accordion(
                                    ast.literal_eval(uc2_default_matilda_rel),
                                    "MATILDA Empfehlungen (Relevanzgefiltert)",
                                    detailed_recommendations=uc2_default_matilda_details_rel,
                                )
                            )

                ##################################################################################################################################################
                # Section 3: MATILDA Empfehlungen Details mit Accordion ##########################################################################################
                ##################################################################################################################################################
                with gr.Accordion("Empfehlungsdetails im Vergleich", open=False):
                    gr.HTML(
                        "<h2 style='text-align: center; margin: 40px 0 20px 0; padding: 15px; border: 2px solid var(--secondary-color, #6c757d); border-radius: 8px; background: transparent;'>📋 MATILDA Empfehlungs-Details</h2>"
                    )

                    # Regular MATILDA recommendations display (for API updates)
                    with gr.Row():
                        with gr.Column(scale=1, min_width=min_width):
                            matilda_recommendations_filtered_uc2 = gr.HTML(
                                label="MATILDA Empfehlungen (Gefiltert):",
                                value=format_matilda_recommendations(uc2_default_matilda_recom),
                            )
                        with gr.Column(scale=1, min_width=min_width):
                            matilda_recommendations_unfiltered_uc2 = gr.HTML(
                                label="MATILDA Empfehlungen (Ungefiltert):",
                                value=format_matilda_recommendations(uc2_default_matilda_recom_unf),
                            )
                        with gr.Column(scale=1, min_width=min_width):
                            matilda_recommendations_rel_filtered_uc2 = gr.HTML(
                                label="MATILDA Empfehlungen (Relevanzgefiltert):",
                                value=format_matilda_recommendations(uc2_default_matilda_recom_rel),
                            )

                btn_uc2.click(
                    fn=call_api_endpoints,
                    inputs=[input_text_uc2, llm_choice, dependencies_state],
                    outputs=[
                        status_text_uc2,
                        matilda_recommendations_filtered_uc2,
                        matilda_recommendations_unfiltered_uc2,
                        matilda_recommendations_rel_filtered_uc2,
                        baseline_accordion_uc2,
                        matilda_accordion_uc2,
                        matilda_filtered_accordion_uc2,
                        matilda_unfiltered_accordion_uc2,
                        matilda_rel_filtered_accordion_uc2,
                    ],
                )
                input_text_uc2.submit(
                    fn=call_api_endpoints,
                    inputs=[input_text_uc2, llm_choice, dependencies_state],
                    outputs=[
                        status_text_uc2,
                        matilda_recommendations_filtered_uc2,
                        matilda_recommendations_unfiltered_uc2,
                        matilda_recommendations_rel_filtered_uc2,
                        baseline_accordion_uc2,
                        matilda_accordion_uc2,
                        matilda_filtered_accordion_uc2,
                        matilda_unfiltered_accordion_uc2,
                        matilda_rel_filtered_accordion_uc2,
                    ],
                )

            ##############################################################################################################
            with gr.Tab("Anwendungsfall 3"):
                with gr.Row():
                    with gr.Column():
                        gr.HTML(szenario_3, elem_classes=["scenario-card"])

                input_text_uc3 = gr.Textbox(label="Query:", lines=12, value=prompt_uc3)
                btn_uc3 = gr.Button("Sende Anfrage an LLM")
                status_text_uc3 = gr.Textbox(value="...", label="Status", interactive=False)

                # Section 1: Baseline vs MATILDA mit Accordion
                gr.HTML(
                    "<h2 style='text-align: center; margin: 30px 0 20px 0; padding: 15px; border: 2px solid var(--primary-color, #007bff); border-radius: 8px; background: transparent;'>📊 Vergleich: Baseline vs. MATILDA Empfehlungen</h2>"
                )

                # Use accordion components for baseline vs MATILDA comparison
                with gr.Row():
                    with gr.Column(scale=1, min_width=300):
                        baseline_accordion_uc3 = gr.HTML(
                            value=create_recommendation_display_components_with_accordion(
                                ast.literal_eval(uc3_default_baseline), "Baseline Empfehlungen", show_score_column=False
                            )
                        )
                    with gr.Column(scale=1, min_width=300):
                        matilda_accordion_uc3 = gr.HTML(
                            value=create_recommendation_display_components_with_accordion(
                                ast.literal_eval(uc3_default_matilda),
                                "MATILDA Empfehlungen",
                                detailed_recommendations=uc3_default_matilda_details,
                            )
                        )

                # Section 2: MATILDA Filtered vs Unfiltered mit Accordion
                gr.HTML(
                    "<h2 style='text-align: center; margin: 40px 0 20px 0; padding: 15px; border: 2px solid var(--info-color, #17a2b8); border-radius: 8px; background: transparent;'>🔍 Vergleich: MATILDA Gefiltert vs. Ungefiltert</h2>"
                )

                # Use accordion components for MATILDA filtered vs unfiltered comparison
                with gr.Row():
                    with gr.Column(scale=1, min_width=min_width):
                        matilda_filtered_accordion_uc3 = gr.HTML(
                            value=create_recommendation_display_components_with_accordion(
                                ast.literal_eval(uc3_default_matilda),
                                "MATILDA Empfehlungen (Gefiltert)",
                                detailed_recommendations=uc3_default_matilda_details,
                            )
                        )
                    with gr.Column(scale=1, min_width=min_width):
                        matilda_unfiltered_accordion_uc3 = gr.HTML(
                            value=create_recommendation_display_components_with_accordion(
                                ast.literal_eval(uc3_default_matilda_unf),
                                "MATILDA Empfehlungen (Ungefiltert)",
                                detailed_recommendations=uc3_default_matilda_details_unf,
                            )
                        )
                    with gr.Column(scale=1, min_width=min_width):
                        matilda_rel_filtered_accordion_uc3 = gr.HTML(
                            value=create_recommendation_display_components_with_accordion(
                                ast.literal_eval(uc3_default_matilda_rel),
                                "MATILDA Empfehlungen (Relevanzgefiltert)",
                                detailed_recommendations=uc3_default_matilda_details_rel,
                            )
                        )

                # Section 3: MATILDA Empfehlungen Details mit Accordion
                gr.HTML(
                    "<h2 style='text-align: center; margin: 40px 0 20px 0; padding: 15px; border: 2px solid var(--secondary-color, #6c757d); border-radius: 8px; background: transparent;'>📋 MATILDA Empfehlungs-Details</h2>"
                )

                # Regular MATILDA recommendations display (for API updates)
                with gr.Row():
                    with gr.Column(scale=1, min_width=min_width):
                        matilda_recommendations_filtered_uc3 = gr.HTML(
                            label="MATILDA Empfehlungen (Gefiltert):",
                            value=format_matilda_recommendations(uc3_default_matilda_recom),
                        )
                    with gr.Column(scale=1, min_width=min_width):
                        matilda_recommendations_unfiltered_uc3 = gr.HTML(
                            label="MATILDA Empfehlungen (Ungefiltert):",
                            value=format_matilda_recommendations(uc3_default_matilda_recom_unf),
                        )
                    with gr.Column(scale=1, min_width=min_width):
                        matilda_recommendations_rel_filtered_uc3 = gr.HTML(
                            label="MATILDA Empfehlungen (Relevanzgefiltert):",
                            value=format_matilda_recommendations(uc3_default_matilda_recom_rel),
                        )

                btn_uc3.click(
                    fn=call_api_endpoints,
                    inputs=[input_text_uc3, llm_choice, dependencies_state],
                    outputs=[
                        status_text_uc3,
                        matilda_recommendations_filtered_uc3,
                        matilda_recommendations_unfiltered_uc3,
                        matilda_recommendations_rel_filtered_uc3,
                        baseline_accordion_uc3,
                        matilda_accordion_uc3,
                        matilda_filtered_accordion_uc3,
                        matilda_unfiltered_accordion_uc3,
                        matilda_rel_filtered_accordion_uc3,
                    ],
                )
                input_text_uc3.submit(
                    fn=call_api_endpoints,
                    inputs=[input_text_uc3, llm_choice, dependencies_state],
                    outputs=[
                        status_text_uc3,
                        matilda_recommendations_filtered_uc3,
                        matilda_recommendations_unfiltered_uc3,
                        matilda_recommendations_rel_filtered_uc3,
                        baseline_accordion_uc3,
                        matilda_accordion_uc3,
                        matilda_filtered_accordion_uc3,
                        matilda_unfiltered_accordion_uc3,
                        matilda_rel_filtered_accordion_uc3,
                    ],
                )

    return demo


logger.info("Creating Gradio interface")
demo = create_gradio_interface()
logger.info("Gradio interface created successfully")

if __name__ == "__main__":
    setup_logging()
    demo.launch()
