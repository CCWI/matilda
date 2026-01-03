package edu.hm.ccwi.matilda.gateway.dto;

public class CrawlerRequestDto {

    private String matildaId;

    private String uri;

    public CrawlerRequestDto(String matildaId, String uri) {
        this.matildaId = matildaId;
        this.uri = uri;
    }

    public String getMatildaId() {
        return matildaId;
    }

    public void setMatildaId(String matildaId) {
        this.matildaId = matildaId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return "CrawlerRequestDto{" +
                "matildaId='" + matildaId + '\'' +
                ", uri='" + uri + '\'' +
                '}';
    }
}
