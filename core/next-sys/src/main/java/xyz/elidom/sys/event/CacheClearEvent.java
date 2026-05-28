package xyz.elidom.sys.event;

public class CacheClearEvent {
    private final String targetResource;

    public CacheClearEvent(String targetResource) {
        this.targetResource = targetResource;
    }

    public String getTargetResource() {
        return targetResource;
    }
}