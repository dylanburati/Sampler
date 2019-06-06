package libre.sampler.publishers;

import org.puredata.core.utils.PdDispatcher;

public class MyPdDispatcher extends PdDispatcher {
    @Override
    public synchronized void release() {
    }

    @Override
    public void print(String s) {
    }
}
