package de.hhu.fscs.ultraqueue.persistence.implementation.db;

import de.hhu.fscs.ultraqueue.model.Song;
import de.hhu.fscs.ultraqueue.persistence.interfaces.SongRepository;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class PersistentSongRepository implements SongRepository {
    @Override
    public Song songById(UUID id) {
        return null;
    }

    @Override
    public UUID songIdByTitleArtist(String titleArtistKey) {
        return null;
    }

    @Override
    public Path txtById(UUID id) {
        return null;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void addSong(Song song, Path txt, String titleArtistKey) {

    }

    @Override
    public Collection<Song> loadAll() {
        return List.of();
    }

    @Override
    public void removeAll() {

    }
}
