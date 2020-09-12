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
import libre.sampler.models.Sample;

@Dao
public interface InstrumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Instrument> instruments);

    @Update
    void updateAll(Instrument... instruments);

    @Query("DELETE FROM instrument WHERE projectId = :projectId")
    void deleteAll(String projectId);

    @Query("DELETE FROM instrument WHERE projectId IN (:projectIds)")
    void deleteAll(List<String> projectIds);

    @Transaction
    @Query("SELECT * from instrument")
    List<InstrumentWithRelations> getAllWithRelations();

    @Transaction
    @Query("SELECT * FROM instrument WHERE id = :instrumentId")
    List<InstrumentWithRelations> getWithRelations(String instrumentId);

    @Transaction
    @Query("SELECT * FROM instrument WHERE id IN (:instrumentIds)")
    List<InstrumentWithRelations> getWithRelations(List<String> instrumentIds);

    class InstrumentWithRelations {
        @Embedded
        public Instrument instrument;

        @Relation(entity = Sample.class, parentColumn = "id", entityColumn = "instrumentId")
        public List<Sample> samples;
    }
}
