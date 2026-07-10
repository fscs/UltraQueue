package de.hhu.fscs.ultraqueue.persistence.implementation.db;

import de.hhu.fscs.ultraqueue.model.PlayedSongLog;
import de.hhu.fscs.ultraqueue.model.QueueEntry;
import de.hhu.fscs.ultraqueue.model.Song;
import de.hhu.fscs.ultraqueue.persistence.implementation.db.dto.PlayedSongLogDto;
import de.hhu.fscs.ultraqueue.persistence.implementation.db.dto.QueueEntryDto;
import de.hhu.fscs.ultraqueue.persistence.implementation.db.dto.SongDto;
import de.hhu.fscs.ultraqueue.persistence.interfaces.QueueStateRepository;
import de.hhu.fscs.ultraqueue.persistence.interfaces.SongRepository;
import de.hhu.fscs.ultraqueue.service.SongCatalogService;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jdbc.core.mapping.AggregateReference;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Primary
@Repository
public class PersistentQueueStateRepository implements QueueStateRepository {
    private final SpringDataQueueEntryRepository springDataQueueEntryRepository;
    private final SpringDataPlayedSongLogRepository springDataPlayedSongLogRepository;
    private final SongRepository songRepository;

    public PersistentQueueStateRepository(SpringDataQueueEntryRepository springDataQueueEntryRepository, SpringDataPlayedSongLogRepository springDataPlayedSongLogRepository, SongRepository songRepository) {
        this.springDataQueueEntryRepository = springDataQueueEntryRepository;
        this.springDataPlayedSongLogRepository = springDataPlayedSongLogRepository;
        this.songRepository = songRepository;
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
        List<QueueEntryDto> entryDtos = state.queue().stream()
                .map(this::QueueEntryToDto)
                .toList();

        List<PlayedSongLogDto> logDtos = state.playedLog().stream()
                .map(this::PlayedSongLogToDto)
                .toList();

        springDataQueueEntryRepository.deleteAll();

        entryDtos.forEach(springDataQueueEntryRepository::addQueueEntry);
        logDtos.forEach(springDataPlayedSongLogRepository::save);
    }

    private QueueEntry DtoToQueueEntry(QueueEntryDto dto) {
            Song song = songRepository.songById(dto.songId().getId());
            return new QueueEntry(dto.id(), song, dto.userId(), dto.username(), dto.userColor(), dto.position());
    }

    private QueueEntryDto QueueEntryToDto(QueueEntry entry) {
        AggregateReference<SongDto, UUID> songReference = () -> entry.getSong().id();
        UUID entryId = entry.getId() != null ? entry.getId() : UUID.randomUUID();
        return new QueueEntryDto(entryId, songReference, entry.getUserId(), entry.getUsername(), entry.getUserColor(), entry.getPosition());
    }

    private PlayedSongLog DtoToPlayedSongLog(PlayedSongLogDto dto) {
        return new PlayedSongLog(dto.songId(), dto.playedAt());
    }

    private PlayedSongLogDto PlayedSongLogToDto(PlayedSongLog log) {
        return new PlayedSongLogDto(log.logId(), log.songId(), log.playedAt());
    }
}
