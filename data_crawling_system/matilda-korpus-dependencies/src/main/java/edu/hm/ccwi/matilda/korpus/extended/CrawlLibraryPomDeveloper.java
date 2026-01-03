package edu.hm.ccwi.matilda.korpus.extended;

public class CrawlLibraryPomDeveloper {

    private String name;

    private String email;

    private String organization;

    public CrawlLibraryPomDeveloper() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    @Override
    public String toString() {
        return "CrawlLibraryPomDeveloper{" +
                "name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", organization='" + organization + '\'' +
                '}';
    }
}
