package de.hhu.fscs.ultraqueue.persistence.implementation.db;

import de.hhu.fscs.ultraqueue.model.Song;
import de.hhu.fscs.ultraqueue.persistence.implementation.db.dto.SongDto;
import de.hhu.fscs.ultraqueue.persistence.interfaces.SongRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.UUID;

@Primary
@Repository
public class PersistentSongRepository implements SongRepository {

    private final SpringDataSongRepository springDataSongRepository;

    public PersistentSongRepository(SpringDataSongRepository springDataSongRepository) {
        this.springDataSongRepository = springDataSongRepository;
    }

    @Override
    public Song songById(UUID id) {
        return DtoToSong(springDataSongRepository.findBySongId(id));
    }

    @Override
    public UUID songIdByTitleArtist(String titleArtistKey) {
        return springDataSongRepository.findSongIdByTitleArtist(titleArtistKey);
    }

    @Override
    public Path txtById(UUID id) {
        return null; // path isn't saved in Database. Lyrics should be saved instead
    }

    @Override
    public int size() {
        return (int) springDataSongRepository.count();
    }

    @Override
    public void addSong(Song song, Path txt, String titleArtistKey) {
        // ignore path, see above
        SongDto dto = SongToDto(song, titleArtistKey);
        springDataSongRepository.addSong(dto);
    }

    @Override
    public Collection<Song> loadAll() {
        return springDataSongRepository.findAll()
                .stream()
                .map(this::DtoToSong)
                .toList();
    }

    @Override
    public void removeAll() {
        springDataSongRepository.deleteAll();
    }

    private Song DtoToSong(SongDto dto) {
        if(dto == null) return null;
        return new Song(dto.songId(), dto.title(), dto.artist(), dto.language(), dto.year(), Duration.ofSeconds((long) dto.length()), dto.genre(), dto.coverPath());
    }

    private SongDto SongToDto(Song song, String titleAndArtist) {
        return new SongDto(song.id(), song.title(), song.artist(), song.language(), song.year(), (int) song.length().getSeconds(), song.genre(), titleAndArtist, song.coverPath());
    }
}
