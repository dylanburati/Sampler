package libre.sampler.databases;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import libre.sampler.models.Sample;

@Dao
public interface SampleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Sample> samples);

    @Query("DELETE FROM sample WHERE instrumentId = :instrumentId")
    void deleteAll(int instrumentId);

    @Query("DELETE FROM sample WHERE instrumentId IN (:instrumentIds)")
    void deleteAll(List<Integer> instrumentIds);

    @Query("SELECT * FROM sample")
    List<Sample> selectAll();
}

