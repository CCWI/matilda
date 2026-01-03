package edu.hm.ccwi.matilda.runner.analyzer;

import edu.hm.ccwi.matilda.base.model.enumeration.LibCategory;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

@Deprecated // First solution, but is now implemented in a Migrator-Routine (Integrationtest) in Analyzer-Service
public class MigrateLibraryCategoriesToDatabase {

    private static final String ANALYZER_CATEGORY_IMPORT_URL = "http://localhost:8084/category/import";

    public static void main(String[] args) {
        MigrateLibraryCategoriesToDatabase runner = new MigrateLibraryCategoriesToDatabase();
        runner.start();
    }

    private void start() {
        RestTemplate restTemplate = new RestTemplate();
        for (LibCategory value : LibCategory.values()) {
            System.out.println("Migrate: " + value.getMatildaCategory());
            requestAnalyzerServiceForCategoryMigration(value.getMatildaCategory(), restTemplate);
        }
    }

    protected ResponseEntity<String> requestAnalyzerServiceForCategoryMigration(String matildaCategory,
                                                                 RestTemplate restTemplate) {
        ResponseEntity exchange = restTemplate.exchange
                (ANALYZER_CATEGORY_IMPORT_URL + "?matildaCategory=" + matildaCategory, HttpMethod.GET,
                        new HttpEntity(createHeaders("admin", "admin")), String.class);
        return exchange;
    }

    protected HttpHeaders createHeaders(String username, String password){
        return new HttpHeaders() {{
            String auth = username + ":" + password;
            byte[] encodedAuth = Base64.encodeBase64(
                    auth.getBytes(Charset.forName("US-ASCII")) );
            String authHeader = "Basic " + new String( encodedAuth );
            set( "Authorization", authHeader );
        }};
    }

}
