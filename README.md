# UltraQueue

## General idea

Participants at an UltraStar Karaoke party can open `/` to see a list of available songs and queue them to the global song queue.

`/queue` displays the queue with the next song (and already sung songs), with an estimate when which song is played.

This UltraStart main application communicates with the queue as follows:

- When opening the song selection screen, a GET request is sent to `/nextsong`, which sends the next song _title_ as plaintext.
- When reaching a song score screen, a POST request is sont to `/songfinished` with `{"title": "the title", "artist", "the artist"}` to indicate that the song has been sung an can be removed from the queue.

The selction/dequeuing is flexible: Users are free to select a song which is not queued next (i.e. if a person is missing right now or a song is broken).

## Admin Login

Go to `/admin/` to login as admin. You will then be able to remove anyone's song from the queue.

## Functional requirements

- Normal users (participants) do not have to log in. They are identified based on a cookie.
- Each user may only have one queue in the song list at the same time. (default value, can be set in `application.properties`).
- The same song (title + artist) may not appear more than one in the queue.
- The same song may not be sung more than once within 60 mninutes (default, can be changed in `application.properties`); this considers queuing time (i.e. re-queuing a song which has just been sung is possible when the estimated queuing time is more than 60 minutes).
- Any rule violation is communicated to the user.
- A user can de-qeue (warning: queue position is lost) or replace their song.
- The song selection list is sortable and searchable; pagination is used.
- There is a single admin account (username and password hardcoded in `application.properties` -- hardcoding is ok, as we do not save any sensitive data) which can remove any songs from the queue.

## Technical requirements

- JDK 25
- Spring Boot
- Thymeleaf
- for now, in memory storage, no database (i.e. if the app crashes, the queues are lost)
- every request is logged, incl. user ids
- only Ultrastar txt files are analysed to get meta data (artist, title, language, year, estimated length); the path to the song folder is set in the `application.properties`
- libraries are used for standard tasks
- JUnit/AssertJ
