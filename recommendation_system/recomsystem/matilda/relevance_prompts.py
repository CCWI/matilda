system_prompt = """
        Welcome to an advanced classification task. Your job is to classify text from README.md files which were extracted from a
        software repository, which were converted to HTML. The text may include different languages, like english or german or chinese.
        Carefully read the information, do not hallucinate or make assumptions.
    """

prompt = """
        Please give me an assessment in the form of a classification of how the text from the README.md file should be categorised.
        It should be detected if the respective software repository holds professional or rather a exploratory software projects.

        Please consider the following instructions while classifying the text:

        A) Professional Projects:
            1. Internal/Corporate Projects:
                * Software projects developed within an organization to support its internal operations, processes, and workflows.
                * Examples: Enterprise resource planning (ERP) systems, customer relationship management (CRM) tools, custom business applications.
            2. Commercial/Revenue-Generating Projects:
                * Software projects that are developed and sold as commercial products or services to generate revenue for the organization.
                * Examples: Productivity suites, cloud-based software, mobile apps, enterprise software solutions.
            3. Research Projects:
                * Software projects that are part of academic or industrial research efforts, often focused on exploring new technologies, algorithms, or techniques.
                * Examples: Prototypes for novel user interfaces, simulations for scientific research, proof-of-concept implementations of emerging technologies.
            4. Commissioned/Contract-Based Projects:
                * Software projects undertaken by a development team or agency on behalf of a client, with a specific set of requirements and a defined scope.
                * Examples: Custom software development for government agencies, enterprise applications built for specific clients, software consulting engagements.
            5. Enterprise-Grade Projects:
                * Software projects that are designed and developed to meet the stringent requirements of large-scale, mission-critical enterprise systems.
                * Examples: Financial trading platforms, air traffic control systems, medical device software.

        B) Exploratory Projects:
            1. Tutorials/Examples:
                * Software projects that are primarily created for educational or demonstrative purposes, to teach specific programming concepts, techniques, or best practices.
                * These projects are often small in scope, focused on a particular feature or functionality, and intended to be easily understood and replicated by learners.
                * Examples: Beginner-level coding exercises, sample applications demonstrating a framework or library, interactive tutorials.
            2. Prototypes:
                * Software projects that are built to validate an idea, test the feasibility of a concept, or explore potential solutions to a problem.
                * Prototypes are typically not intended for production use, but rather to gather feedback, identify technical challenges, and inform the development of a more polished, production-ready application.
                * Examples: Mockups of user interfaces, experimental features, proof-of-concept implementations.
            3. Proofs of Concept:
                * Software projects that are developed to demonstrate the viability of a particular approach, technology, or solution to a problem.
                * These projects are often small in scale and focused on validating a specific hypothesis or claim, rather than building a complete, production-ready application.
                * Examples: Implementations of novel algorithms, integrations of emerging technologies, explorations of alternative architectural patterns.
            4. School/Training Projects:
                * Software projects that are created as part of educational programs, such as university courses, coding bootcamps, or internal training initiatives.
                * These projects are primarily focused on the learning process, allowing students to apply their knowledge and develop practical programming skills.
                * Examples: Student-built web applications, mobile apps developed during a software engineering course, projects from a data structures and algorithms class.
            5. Technology Exploration/Testing:
                * Software projects that are developed to experiment with new technologies, languages, frameworks, or tools, without a specific production-oriented goal in mind.
                * These projects are often small-scale, exploratory in nature, and may not have a clear end-user or business value.
                * Examples: Trying out a new programming language, experimenting with a novel database technology, testing the capabilities of a newly released API.
            6. Experimental/R&D Projects:
                * Software projects that are part of research and development efforts, focused on advancing the state-of-the-art in a particular domain or exploring innovative solutions to complex problems.
                * These projects may not have immediate commercial or practical applications, but are driven by a desire to push the boundaries of what is possible with technology.
                * Examples: Research prototypes for machine learning algorithms, simulations for scientific computing, explorations of emerging paradigms like quantum computing.
            7. Hobby/Personal Projects:
                * Software projects that are developed by individuals for their own personal interest, enjoyment, or learning, without a focus on commercial or enterprise-level requirements.
                * These projects are often driven by the creator's passion, curiosity, or desire to explore a particular domain or technology.
                * Examples: Side projects, pet projects, open-source contributions made by developers in their spare time.
            8. Hackathon Projects:
                * Software projects developed during hackathons, coding competitions, or other time-bound events to solve specific problems or challenges.
                * Examples: Hackathon projects

        The key distinction between these "Exploratory Project" subcategories and the "Professional Project" subcategories is the primary intent and
        goals behind the software development efforts. Exploratory projects are more focused on learning, experimentation, and validation, while
        professional projects are geared towards production-ready, revenue-generating, or enterprise-level applications.

        README.md text:
        <<PLACEHOLDER:text_to_classify>>

        Please follow strictly the following output format without summarizing or describing anything.
        Extracted data must conform to this structure without deviation in field names:

        <<PLACEHOLDER:format_instructions>>

        Avoid any other text then the json output. Even don't write "Here is the JSON output:" or something similar. Just start with the JSON output and end with it.
        Start with the full JSON after the following data to analyse:
    """
