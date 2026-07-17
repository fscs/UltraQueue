package de.hhu.fscs.ultraqueue.persistence.implementation.db;

import de.hhu.fscs.ultraqueue.persistence.implementation.db.dto.SongDto;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataSongRepository extends CrudRepository<SongDto, UUID> {
    SongDto findBySongId(UUID songId);

    @Query("select song_id from songs where title_artist like :titleArtist")
    UUID findSongIdByTitleArtist(String titleArtist);
    List<SongDto> findAll();
    @Override
    void deleteAll();

    @Query("""
        INSERT INTO songs
            (song_id,
             title,
             artist,
             language,
             year,
             length,
             genre,
             title_artist,
             cover_path)
        VALUES
            (:#{#dto.songId},
             :#{#dto.title},
             :#{#dto.artist},
             :#{#dto.language},
             :#{#dto.year},
             :#{#dto.length},
             :#{#dto.genre},
             :#{#dto.titleArtist},
             :#{#dto.coverPath})
        """)
    void addSong(SongDto dto);
}
