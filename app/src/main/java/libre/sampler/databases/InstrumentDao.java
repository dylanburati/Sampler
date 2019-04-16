package libre.sampler.databases;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Embedded;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Transaction;
import androidx.room.Update;
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;

@Dao
public interface InstrumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Instrument> instruments);

    @Update
    void updateAll(Instrument... instruments);

    @Transaction
    @Query("SELECT * FROM project WHERE id = :projectId")
    List<ProjectInstrumentRelation> getAll(int projectId);

    static class ProjectInstrumentRelation {
        @Embedded
        public Project project;

        @Relation(parentColumn = "id", entityColumn = "projectId")
        public List<Instrument> instruments;
    }

}
