package de.hhu.fscs.ultraqueue.persistence.interfaces;

import de.hhu.fscs.ultraqueue.model.Song;

import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;

public interface SongRepository {
    Song songById(UUID id);
    UUID songIdByTitleArtist(String titleArtistKey);
    Path txtById(UUID id);
    int size();
    void addSong(Song song, Path txt, String titleArtistKey);
    Collection<Song> loadAll();
    void removeAll();
}
