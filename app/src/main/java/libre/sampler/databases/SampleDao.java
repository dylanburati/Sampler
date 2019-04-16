package libre.sampler.databases;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Embedded;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Transaction;
import libre.sampler.models.Instrument;
import libre.sampler.models.Sample;

@Dao
public interface SampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Sample> samples);

    @Transaction
    @Query("SELECT * FROM instrument WHERE id IN (:instrumentIds)")
    List<InstrumentSampleRelation> getAll(List<Integer> instrumentIds);

    class InstrumentSampleRelation {
        @Embedded
        public Instrument instrument;

        @Relation(parentColumn = "id", entityColumn = "instrumentId")
        public List<Sample> samples;
    }
}

