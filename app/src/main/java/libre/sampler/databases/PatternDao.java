package libre.sampler.databases;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Embedded;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Transaction;
import androidx.room.Update;
import libre.sampler.models.Pattern;
import libre.sampler.models.ScheduledNoteEvent;

@Dao
public interface PatternDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Pattern> patterns);

    @Update
    void updateAll(Pattern... patterns);

    @Query("DELETE FROM pattern WHERE projectId = :projectId")
    void deleteAll(int projectId);

    @Query("DELETE FROM pattern WHERE projectId IN (:projectIds)")
    void deleteAll(List<Integer> projectIds);

    @Transaction
    @Query("SELECT * FROM pattern WHERE id = :patternId")
    List<PatternWithRelations> getWithRelations(int patternId);

    @Transaction
    @Query("SELECT * FROM pattern WHERE id IN (:patternIds)")
    List<PatternWithRelations> getWithRelations(List<Integer> patternIds);

    @Delete
    void delete(Pattern pattern);

    static class PatternWithRelations {
        @Embedded
        public Pattern pattern;

        @Relation(entity = ScheduledNoteEvent.class, parentColumn = "id", entityColumn = "patternId")
        public List<ScheduledNoteEvent> scheduledNoteEvents;
    }

}
