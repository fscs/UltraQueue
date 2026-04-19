package de.hhu.fscs.ultraqueue.config;

public class UltraQueuePropertiesBuilder {
    private String songFolder = "songs";
    private int maxSongsPerUser = 1;
    private int minIntervalMinutes = 0;
    private UltraQueueProperties.Pagination pagination = new UltraQueueProperties.Pagination(10);
    private UltraQueueProperties.Admin admin = new UltraQueueProperties.Admin("admin", "password");

    public UltraQueuePropertiesBuilder maxSongsPerUser(int maxSongsPerUser) {
        this.maxSongsPerUser = maxSongsPerUser;
        return this;
    }

    public UltraQueuePropertiesBuilder minIntervalMinutes(int minIntervalMinutes) {
        this.minIntervalMinutes = minIntervalMinutes;
        return this;
    }

    public UltraQueueProperties build() {
        return new UltraQueueProperties(songFolder, maxSongsPerUser, minIntervalMinutes, pagination, admin);
    }
}
