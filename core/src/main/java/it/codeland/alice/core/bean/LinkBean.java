package it.codeland.alice.core.bean;

public class LinkBean {

    private String label;
    private String link;
    private String target;
    private String icon;

    public LinkBean(String link, String target, String label, String icon) {
        this.label =label;
        this.target =target;
        this.link = link;
        this.icon = icon;

    };
    public String getLabel() {
        return label;
    };
    public String getLink() {
        return link;
    };
    public String getTarget() {
        return target;
    };
    public String getIcon() {
        return  icon;
    };
}
