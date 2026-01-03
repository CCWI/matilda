package edu.hm.ccwi.matilda.crawler.kafka.dto;

public class CrawlerRequestDto {

    private final String matildaId;

    private final String uri;

    public CrawlerRequestDto(String matildaId, String uri) {
        this.matildaId = matildaId;
        this.uri = uri;
    }

    public String getMatildaId() {
        return matildaId;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public String toString() {
        return "CrawlerRequestDto{" +
                "matildaId='" + matildaId + '\'' +
                ", uri='" + uri + '\'' +
                '}';
    }
}
