package libre.sampler.publishers;

import android.media.midi.MidiDevice;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.os.Build;
import android.util.Log;
import android.util.Pair;

import java.io.IOException;
import java.util.Arrays;

import androidx.annotation.RequiresApi;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.utils.MidiConstants;

@RequiresApi(api = Build.VERSION_CODES.M)
public class MidiEventDispatcher implements MidiManager.OnDeviceOpenedListener {
    private ProjectViewModel viewModel; 

    private MidiReceiver receiver;
    private MidiOutputPort receivePort;

    public MidiEventDispatcher(ProjectViewModel viewModel) {
        this.viewModel = viewModel;
        receiver = new MyMidiReceiver();
    }

    @Override
    public void onDeviceOpened(MidiDevice device) {
        closeMidi();
        receivePort = device.openOutputPort(0);
        receivePort.onConnect(receiver);
    }

    public void closeMidi() {
        if(receivePort != null) {
            receivePort.onDisconnect(receiver);
        }
    }

    private class MyMidiReceiver extends MidiReceiver {
        @Override
        public void onSend(byte[] msg, int offset, int count, long timestamp) throws IOException {
            byte[] data = new byte[count];
            System.arraycopy(msg, offset, data, 0, count);
            Log.d("MidiEventDispatcher", Arrays.toString(data));

            for(int commandIdx = 0; commandIdx < count; /* increment handled per message */) {
                byte command = (byte) (data[commandIdx] & MidiConstants.STATUS_COMMAND_MASK);
                if(command == MidiConstants.STATUS_NOTE_ON) {
                    int keyNum = data[commandIdx + 1] & 0xFF;
                    int velocity = data[commandIdx + 2] & 0xFF;
                    Pair<Long, Integer> eventId = new Pair<>(-1L, keyNum);  // associate eventIndex with key
                    NoteEvent event = new NoteEvent(NoteEvent.NOTE_ON, viewModel.getKeyboardInstrument(), keyNum, velocity, eventId);
                    viewModel.noteEventSource.dispatch(event);
                } else if(command == MidiConstants.STATUS_NOTE_OFF) {
                    int keyNum = data[commandIdx + 1] & 0xFF;
                    int velocity = data[commandIdx + 2] & 0xFF;
                    Pair<Long, Integer> eventId = new Pair<>(-1L, keyNum);  // associate eventIndex with key
                    NoteEvent event = new NoteEvent(NoteEvent.NOTE_OFF, viewModel.getKeyboardInstrument(), keyNum, velocity, eventId);
                    viewModel.noteEventSource.dispatch(event);
                } else if(command == MidiConstants.STATUS_CONTROL_CHANGE) {
                    int controller = data[commandIdx + 1] & 0xFF;
                    if(controller == 0x30) {
                        if(data[commandIdx + 2] != 0) {
                            // viewModel.patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_ON, ));
                        } else {
                            // viewModel.patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_OFF, ));
                        }
                    }
                }
                commandIdx += MidiConstants.getBytesPerMessage(data[commandIdx]);
            }
        }
    }
}
