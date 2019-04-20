package libre.sampler.databases;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import libre.sampler.models.Project;

@Dao
public interface ProjectDao {
    @Insert
    long insert(Project toAdd);

    @Insert
    void insertAll(Project... projects);

    @Update
    void updateAll(Project... projects);

    @Query("SELECT * FROM project ORDER BY mtime DESC")
    List<Project> getAll();

    @Query("DELETE FROM project WHERE id IN (:projectIds)")
    void deleteAll(List<Integer> projectIds);
}
