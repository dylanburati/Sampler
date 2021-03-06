package libre.sampler.databases;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import libre.sampler.models.ScheduledNoteEvent;

@Dao
public interface ScheduledNoteEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ScheduledNoteEvent> events);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(ScheduledNoteEvent[] events);

    @Query("DELETE FROM scheduledNoteEvent WHERE patternId = :patternId")
    void deleteAll(String patternId);

    @Query("DELETE FROM scheduledNoteEvent WHERE patternId IN (:patternIds)")
    void deleteAll(List<String> patternIds);

    @Query("SELECT * FROM scheduledNoteEvent")
    List<ScheduledNoteEvent> getAll();
}

