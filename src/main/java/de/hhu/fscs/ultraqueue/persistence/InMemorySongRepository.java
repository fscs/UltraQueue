package de.hhu.fscs.ultraqueue.persistence;

import de.hhu.fscs.ultraqueue.model.Song;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Primary
@Repository
public class InMemorySongRepository implements SongRepository{
    /** UUID → Song */
    private final Map<UUID, Song> songById = new ConcurrentHashMap<>();

    /** UUID -> source txt path for lazy lyrics loading */
    private final Map<UUID, Path> songTxtById = new ConcurrentHashMap<>();

    /** (title‑lowercase, artist‑lowercase) → UUID – gives O(1) lookup for the API */
    private final Map<String, UUID> titleArtistIndex = new ConcurrentHashMap<>();

    @Override
    public Song songById(UUID id) {
        return null;
    }

    @Override
    public UUID songIdByTitleArtist(String titleArtistKey) {
        return titleArtistIndex.get(titleArtistKey);
    }

    @Override
    public Path txtById(UUID id) {
        return songTxtById.get(id);
    }

    @Override
    public int size() {
        return songById.size();
    }

    @Override
    public void addSong(Song song, Path txt, String titleArtistKey) {
        songById.put(song.id(), song);
        songTxtById.put(song.id(), txt);
        titleArtistIndex.put(titleArtistKey, song.id());
    }

    @Override
    public Collection<Song> loadAll() {
        return songById.values();
    }

    @Override
    public void removeAll() {
        songById.clear();
        songTxtById.clear();
        titleArtistIndex.clear();
    }
}
