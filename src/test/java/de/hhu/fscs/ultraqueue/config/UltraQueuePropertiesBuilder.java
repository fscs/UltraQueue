package de.hhu.fscs.ultraqueue.config;

public class UltraQueuePropertiesBuilder {
    private String songFolder = "songs";
    private boolean onlyOneSongPerUser = true;
    private int minIntervalMinutes = 0;
    private UltraQueueProperties.Pagination pagination = new UltraQueueProperties.Pagination(10);
    private UltraQueueProperties.Admin admin = new UltraQueueProperties.Admin("admin", "password");
    private UltraQueueProperties.Cookie cookie = new UltraQueueProperties.Cookie("test-cookie-signing-secret");

    public UltraQueuePropertiesBuilder onlyOneSongPerUser(boolean onlyOneSongPerUser) {
        this.onlyOneSongPerUser = onlyOneSongPerUser;
        return this;
    }

    public UltraQueuePropertiesBuilder minIntervalMinutes(int minIntervalMinutes) {
        this.minIntervalMinutes = minIntervalMinutes;
        return this;
    }

    public UltraQueueProperties build() {
        return new UltraQueueProperties(songFolder, onlyOneSongPerUser, minIntervalMinutes, pagination, admin, cookie);
    }
}
