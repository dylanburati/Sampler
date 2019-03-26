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
    void insertAll(Project... projects);

    @Update
    void updateAll(Project... projects);

    @Query("SELECT id, name, mtime FROM project ORDER BY mtime DESC")
    List<Project> getAll();
}
