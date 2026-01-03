custom_css = """
    /* CSS Variables for consistent theming */
    :root {
        --primary-color: #3b82f6;
        --secondary-color: #6c757d;
        --success-color: #10b981;
        --danger-color: #ef4444;
        --warning-color: #f59e0b;
        --info-color: #06b6d4;
        --dark-color: #1f2937;
        --border-color-primary: #d1d5db;
        --dark-border-color: #4b5563;
        --dark-primary-color: #1e40af;
    }

    /* Dark mode adjustments */
    @media (prefers-color-scheme: dark) {
        :root {
            --border-color-primary: #4b5563;
            --primary-color: #60a5fa;
            --success-color: #34d399;
            --danger-color: #f87171;
            --warning-color: #fbbf24;
            --info-color: #22d3ee;
        }
    }

    footer {visibility: hidden}

    .logo-container {
        display: flex;
        justify-content: center;
        margin-bottom: 1rem;
    }

    .title-container {
        text-align: center !important;
        width: 100% !important;
        justify-content: center !important;
    }

    /* Neutralize Gradio's HTML component container for scenario cards */
    .scenario-card.gradio-html {
        border: none !important;
        padding: 0 !important;
        margin: 0 !important;
        background: transparent !important;
        box-shadow: none !important;
    }

    /* Also target common Gradio container classes */
    .scenario-card .prose,
    .scenario-card .gradio-container,
    .scenario-card .svelte-1b6s6s,
    .scenario-card .gr-box {
        border: none !important;
        padding: 0 !important;
        margin: 0 !important;
        background: transparent !important;
        box-shadow: none !important;
    }

    /* Main scenario card styling - this creates the actual card */
    .scenario-content {
        border: 1px solid var(--border-color-primary, #d1d5db) !important;
        border-radius: 12px !important;
        padding: 24px !important;
        margin: 16px auto !important;
        max-width: 800px !important;
        box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06) !important;
        position: relative !important;
        background: inherit !important;
    }

    .scenario-content::before {
        content: '📋 Szenario-Beschreibung' !important;
        position: absolute !important;
        top: -12px !important;
        left: 24px !important;
        background: var(--primary-color, #3b82f6) !important;
        color: white !important;
        padding: 6px 16px !important;
        border-radius: 20px !important;
        font-size: 14px !important;
        font-weight: 600 !important;
    }

    /* Content styling within the scenario card */
    .scenario-content h3 {
        margin: 16px 0 12px 0 !important;
        font-size: 1.2em !important;
        font-weight: 600 !important;
    }

    .scenario-content hr {
        border: none !important;
        height: 1px !important;
        background: var(--border-color-primary, #e5e7eb) !important;
        margin: 16px 0 !important;
    }

    .scenario-content strong {
        font-weight: 600 !important;
    }

    .scenario-content p {
        margin: 8px 0 !important;
        line-height: 1.6 !important;
    }

    .scenario-content ul, .scenario-content ol {
        margin: 12px 0 !important;
        padding-left: 20px !important;
    }

    .scenario-content li {
        margin: 4px 0 !important;
        line-height: 1.5 !important;
    }

    /* Hover-Effekt für bessere Interaktivität */
    .scenario-content:hover {
        box-shadow: 0 4px 6px 0 rgba(0, 0, 0, 0.1), 0 2px 4px 0 rgba(0, 0, 0, 0.06) !important;
        transform: translateY(-1px) !important;
        transition: all 0.2s ease-in-out !important;
    }

    /* Dark Mode Support */
    @media (prefers-color-scheme: dark) {
        .scenario-content {
            border-color: var(--dark-border-color, #4b5563) !important;
        }

        .scenario-content hr {
            background: var(--dark-border-color, #4b5563) !important;
        }

        .scenario-content::before {
            background: var(--dark-primary-color, #1e40af) !important;
        }
    }
"""
