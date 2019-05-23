package libre.sampler.tasks;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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

public class GetInstrumentsTask extends AsyncTask<Void, Void, GetInstrumentsTask.ProjectData> {
    private int projectId;
    private Consumer<Project> projectCallback;
    private Consumer<List<Instrument>> instrumentCallback;
    private Consumer<List<Pattern>> patternCallback;

    static class ProjectData {
        private Project project;
        private List<Instrument> instruments;
        private List<Pattern> patterns;

        public ProjectData(Project project, List<Instrument> instruments, List<Pattern> patterns) {
            this.project = project;
            this.instruments = instruments;
            this.patterns = patterns;
        }
    }

    public GetInstrumentsTask(int projectId, Consumer<Project> projectCallback, Consumer<List<Instrument>> instrumentCallback, Consumer<List<Pattern>> patternCallback) {
        this.projectId = projectId;
        this.projectCallback = projectCallback;
        this.instrumentCallback = instrumentCallback;
        this.patternCallback = patternCallback;
    }

    @Override
    protected ProjectData doInBackground(Void... voids) {
        Project proj = null;
        Map<Integer, Instrument> projInstrumentsMap = new HashMap<>();
        List<Pattern> projPatterns = new ArrayList<>();
        // List<ScheduledNoteEvent> tmp = DatabaseConnectionManager.getInstance().scheduledNoteEventDao().getAll();

        List<ProjectDao.ProjectWithRelations> relations = DatabaseConnectionManager.getInstance().projectDao().getWithRelations(projectId);
        for(ProjectDao.ProjectWithRelations prj : relations) {
            if(prj.project.id == projectId) {
                proj = prj.project;
                List<Integer> instrumentIds = new ArrayList<>();
                for(Instrument t : prj.instruments) {
                    instrumentIds.add(t.id);
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

                List<Integer> patternIds = new ArrayList<>();
                for(Pattern p : prj.patterns) {
                    patternIds.add(p.id);
                }
                List<PatternDao.PatternWithRelations> patternData = DatabaseConnectionManager.getInstance().patternDao().getWithRelations(patternIds);
                for(PatternDao.PatternWithRelations r : patternData) {
                    // DatabaseConnectionManager.getInstance().patternDao().delete(r.pattern);
                    Collections.sort(r.scheduledNoteEvents, new Comparator<ScheduledNoteEvent>() {
                        @Override
                        public int compare(ScheduledNoteEvent o1, ScheduledNoteEvent o2) {
                            return Long.compare(o1.offsetTicks, o2.offsetTicks);
                        }
                    });
                    for(ScheduledNoteEvent e : r.scheduledNoteEvents) {
                        Instrument t = projInstrumentsMap.get(e.instrumentId);
                        if(t != null) {
                            e.setInstrument(t);
                        }
                    }
                    r.pattern.setEvents(r.scheduledNoteEvents);
                    projPatterns.add(r.pattern);
                }
            }
        }

        List<Instrument> projInstruments = new ArrayList<>(projInstrumentsMap.values());
        Collections.sort(projInstruments, new Comparator<Instrument>() {
            @Override
            public int compare(Instrument o1, Instrument o2) {
                return Integer.compare(o1.id, o2.id);
            }
        });
        return new ProjectData(proj, projInstruments, projPatterns);
    }

    @Override
    protected void onPostExecute(ProjectData data) {
        if(this.projectCallback != null) this.projectCallback.accept(data.project);
        if(this.instrumentCallback != null) this.instrumentCallback.accept(data.instruments);
        if(this.patternCallback != null) this.patternCallback.accept(data.patterns);
    }
}
