import logging
import xml.etree.ElementTree as ET

logger = logging.getLogger("matilda_recom.utils.maven_parser")


def parse_pom_xml(file_content: str) -> list[str]:
    """
    Parse Maven pom.xml file content and extract dependencies.

    Args:
        file_content: String containing the pom.xml file content

    Returns:
        List of dependencies in format "groupId:artifactId"
    """
    try:
        # Register the namespace
        namespaces = {"maven": "http://maven.apache.org/POM/4.0.0"}
        ET.register_namespace("", "http://maven.apache.org/POM/4.0.0")

        # Parse the XML content
        root = ET.fromstring(file_content)

        # Find all dependency elements
        dependencies = []

        # Try with namespace first
        deps_elements = root.findall(".//maven:dependencies/maven:dependency", namespaces)

        # If no dependencies found with namespace, try without
        if not deps_elements:
            deps_elements = root.findall(".//dependencies/dependency")

        logger.info(f"Found {len(deps_elements)} dependencies in POM file")

        # Extract groupId and artifactId from each dependency
        for dep in deps_elements:
            try:
                # Try with namespace first
                group_id = dep.find("maven:groupId", namespaces)
                artifact_id = dep.find("maven:artifactId", namespaces)

                # If not found, try without namespace
                if group_id is None:
                    group_id = dep.find("groupId")
                if artifact_id is None:
                    artifact_id = dep.find("artifactId")

                if group_id is not None and artifact_id is not None:
                    dependency_str = f"{group_id.text}:{artifact_id.text}"
                    dependencies.append(dependency_str)
                    logger.debug(f"Found dependency: {dependency_str}")
            except Exception as e:
                logger.warning(f"Error parsing dependency: {e}")
                continue

        logger.info(f"Successfully parsed {len(dependencies)} dependencies")
        return dependencies
    except Exception as e:
        logger.error(f"Error parsing POM file: {e}", exc_info=True)
        return []
