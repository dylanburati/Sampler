package libre.sampler.tasks;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import androidx.core.util.Consumer;
import libre.sampler.databases.InstrumentDao;
import libre.sampler.databases.PatternDao;
import libre.sampler.databases.ProjectDao;
import libre.sampler.models.Instrument;
import libre.sampler.models.Pattern;
import libre.sampler.models.Project;
import libre.sampler.models.Sample;
import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.utils.DatabaseConnectionManager;

public class LoadProjectTask extends AsyncTask<Void, Void, LoadProjectTask.ProjectData> {
    private int projectId;
    private Consumer<ProjectData> projectCallback;

    public static class ProjectData {
        public Project project;
        public List<Instrument> instruments;
        public List<Pattern> patterns;

        public ProjectData(Project project, List<Instrument> instruments, List<Pattern> patterns) {
            this.project = project;
            this.instruments = instruments;
            this.patterns = patterns;
        }
    }

    public LoadProjectTask(int projectId, Consumer<ProjectData> projectCallback) {
        this.projectId = projectId;
        this.projectCallback = projectCallback;
    }

    @Override
    protected ProjectData doInBackground(Void... voids) {
        Project project = null;
        List<Integer> instrumentIds = new ArrayList<>();
        List<Integer> patternIds = new ArrayList<>();

        Map<Integer, Instrument> projInstrumentsMap = new HashMap<>();
        List<Pattern> projPatterns = new ArrayList<>();

        List<ProjectDao.ProjectWithRelations> relations = DatabaseConnectionManager.getInstance().projectDao().getWithRelations(projectId);
        for(ProjectDao.ProjectWithRelations prj : relations) {
            if(prj.project.id == projectId) {
                project = prj.project;
                for(Instrument t : prj.instruments) {
                    instrumentIds.add(t.id);
                }
                for(Pattern p : prj.patterns) {
                    patternIds.add(p.id);
                }
                break;
            }
        }
        if(project == null) {
            throw new AssertionError("Database query did not find the project");
        }

        List<InstrumentDao.InstrumentWithRelations> sampleData = DatabaseConnectionManager.getInstance().instrumentDao().getWithRelations(instrumentIds);
        for(InstrumentDao.InstrumentWithRelations r : sampleData) {
            Collections.sort(r.samples, new Comparator<Sample>() {
                @Override
                public int compare(Sample o1, Sample o2) {
                    return Integer.compare(o1.id, o2.id);
                }
            });
            r.instrument.setSamples(r.samples);
            projInstrumentsMap.put(r.instrument.id, r.instrument);
        }

        List<PatternDao.PatternWithRelations> patternData = DatabaseConnectionManager.getInstance().patternDao().getWithRelations(patternIds);
        for(PatternDao.PatternWithRelations r : patternData) {
            Collections.sort(r.scheduledNoteEvents, new Comparator<ScheduledNoteEvent>() {
                @Override
                public int compare(ScheduledNoteEvent o1, ScheduledNoteEvent o2) {
                    return Long.compare(o1.offsetTicks, o2.offsetTicks);
                }
            });
            ListIterator<ScheduledNoteEvent> iter = r.scheduledNoteEvents.listIterator();
            while(iter.hasNext()) {
                ScheduledNoteEvent e = iter.next();
                Instrument t = projInstrumentsMap.get(e.instrumentId);
                if(t != null) {
                    e.setInstrument(t);
                } else {
                    iter.remove();
                }
            }
            r.pattern.setEvents(r.scheduledNoteEvents);
            projPatterns.add(r.pattern);
        }

        List<Instrument> projInstruments = new ArrayList<>(projInstrumentsMap.values());
        Collections.sort(projInstruments, new Comparator<Instrument>() {
            @Override
            public int compare(Instrument o1, Instrument o2) {
                return Integer.compare(o1.id, o2.id);
            }
        });
        return new ProjectData(project, projInstruments, projPatterns);
    }

    @Override
    protected void onPostExecute(ProjectData data) {
        if(this.projectCallback != null) this.projectCallback.accept(data);
    }
}
