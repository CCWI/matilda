package edu.hm.ccwi.matilda.korpus.model;

public enum LibCategory {

	Actor_Frameworks("Actor Frameworks", true),
	Application_Metrics("Application Metrics", true),
	Aspect_Oriented("Aspect Oriented", true),
	Build_Tools("Build Tools", true),
	Bytecode_Libraries("Bytecode Libraries", true),
	Cache_Implementations("Cache Implementations", true),
	Cloud_Computing("Cloud Computing", true),
	Code_Analyzers("Code Analyzers", false), 							//????
	Collections("Collections", false),									//????
	Command_Line_Parsers("Command Line Parsers", true),
	Configuration_Libraries("Configuration Libraries", false),			//????
	Core_Utilities("Core Utilities", false),								//????
	Date_and_Time_Utilities("Date and Time Utilities", false),
	Dependency_Injection("Dependency Injection", true),
	Embedded_SQL_Databases("Embedded SQL Databases", true),
	HTML_Parsers("HTML Parsers", true),
	HTTP_Clients("HTTP Clients", true),
	IO_Utilities("IO Utilities", false),									//????
	JDBC_Extensions("JDBC Extensions", true),
	JDBC_Pools("JDBC Pools", true),
	JPA_Implementations("JPA Implementations", true),
	JSON_Libraries("JSON Libraries", true),
	JVM_Languages("JVM Languages", true),
	Logging_Bridges("Logging Bridges", false),
	Logging_Frameworks("Logging Frameworks", false),
	Mail_Clients("Mail Clients", true),
	Maven_Plugins("Maven Plugins", false),
	Mocking("Mocking", false),
	ObjectRelational_Mapping("Object Relational Mapping", true),
	PDF_Libraries("PDF Libraries", true),
	UNDEFINED("Unknown", false);

	private String name;
	private boolean relevant;

	LibCategory(String name, boolean relevant) {
		this.name = name;
		this.relevant = relevant;
	}

	public String getName() {
		return name;
	}

	public boolean isRelevant() {
		return relevant;
	}
}