package libre.sampler.databases;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Embedded;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Relation;
import androidx.room.Transaction;
import androidx.room.Update;
import libre.sampler.models.Instrument;
import libre.sampler.models.Pattern;
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

    @Transaction
    @Query("SELECT * FROM project WHERE id = :projectId")
    List<ProjectWithRelations> getWithRelations(int projectId);

    @Transaction
    @Query("SELECT * FROM project WHERE id IN (:projectIds)")
    List<ProjectWithRelations> getWithRelations(List<Integer> projectIds);

    static class ProjectWithRelations {
        @Embedded
        public Project project;

        @Relation(parentColumn = "id", entityColumn = "projectId", entity = Instrument.class)
        public List<Instrument> instruments;

        @Relation(parentColumn = "id", entityColumn = "projectId", entity = Pattern.class)
        public List<Pattern> patterns;
    }
}
