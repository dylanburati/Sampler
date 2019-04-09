package libre.sampler.utils;

import java.util.ArrayList;
import java.util.List;

import libre.sampler.models.NoteEvent;

public class VoiceBindingList {
    private static class VoiceBindingData {
        private NoteEvent event;
        private boolean closed;
    
        public VoiceBindingData(NoteEvent evt) {
            this.event = evt;
            this.closed = false;
        }
    }
    
    private List<VoiceBindingData> bindings;
    
    public VoiceBindingList(int size) {
        bindings = new ArrayList<>(size);
        for(int i = 0; i < size; i++) {
            bindings.add(null);
        }
    }
    
    public int openEvent(NoteEvent openEvt) {
        int voiceIndex = 0;
        while(voiceIndex < bindings.size() && bindings.get(voiceIndex) != null) {
            voiceIndex++;
        }
        if(voiceIndex >= bindings.size()) {
            return -1;
        }
        bindings.set(voiceIndex, new VoiceBindingData(openEvt));
        return voiceIndex;
    }

    public int closeEvent(NoteEvent closeEvt) {
        int voiceIndex = 0;
        while(voiceIndex < bindings.size()) {
            VoiceBindingData b = bindings.get(voiceIndex);
            if(b != null && !b.closed && b.event.eventId.equals(closeEvt.eventId)) {
                b.closed = true;
                return voiceIndex;
            }
            voiceIndex++;
        }
        return -1;
    }

    public void voiceFree(int voiceIndex) {
        if(voiceIndex > 0 && voiceIndex <= bindings.size()) {
            bindings.set(voiceIndex, null);
        }
    }
}
