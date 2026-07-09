package de.hhu.fscs.ultraqueue.persistence.implementation.db;

import de.hhu.fscs.ultraqueue.model.PlayedSongLog;
import de.hhu.fscs.ultraqueue.model.QueueEntry;
import de.hhu.fscs.ultraqueue.model.Song;
import de.hhu.fscs.ultraqueue.persistence.implementation.db.dto.PlayedSongLogDto;
import de.hhu.fscs.ultraqueue.persistence.implementation.db.dto.QueueEntryDto;
import de.hhu.fscs.ultraqueue.persistence.interfaces.QueueStateRepository;
import de.hhu.fscs.ultraqueue.service.SongCatalogService;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PersistentQueueStateRepository implements QueueStateRepository {
    private final SpringDataQueueEntryRepository springDataQueueEntryRepository;
    private final SpringDataPlayedSongLogRepository springDataPlayedSongLogRepository;
    private final SongCatalogService songCatalogService;

    public PersistentQueueStateRepository(SpringDataQueueEntryRepository springDataQueueEntryRepository, SpringDataPlayedSongLogRepository springDataPlayedSongLogRepository, SongCatalogService songCatalogService) {
        this.springDataQueueEntryRepository = springDataQueueEntryRepository;
        this.springDataPlayedSongLogRepository = springDataPlayedSongLogRepository;
        this.songCatalogService = songCatalogService;
    }

    @Override
    public QueueState loadQueue() {
        List<QueueEntry> entries = springDataQueueEntryRepository
                .findAll()
                .stream()
                .map(this::DtoToQueueEntry)
                .toList();

        List<PlayedSongLog> playedSongLogs = springDataPlayedSongLogRepository
                .findAll()
                .stream()
                .map(this::DtoToPlayedSongLog)
                .toList();

        return new QueueStateRepository.QueueState(entries, playedSongLogs);
    }


    @Override
    public void saveQueue(QueueState state) {

    }

    private QueueEntry DtoToQueueEntry(QueueEntryDto dto) {
        Song song = songCatalogService.findById(dto.id()).orElseThrow(() -> new IllegalStateException("song in database not found in catalog"));
        return new QueueEntry(dto.id(), song, dto.userId(), dto.username(), dto.userColor(), dto.position());
    }

    private PlayedSongLog DtoToPlayedSongLog(PlayedSongLogDto dto) {
        return new PlayedSongLog(dto.songId(), dto.playedAt());
    }
}
