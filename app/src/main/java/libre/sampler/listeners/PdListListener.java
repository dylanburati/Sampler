package libre.sampler.listeners;

import org.puredata.core.PdListener;

public abstract class PdListListener implements PdListener {
    @Override
    public void receiveBang(String source) {
    }

    @Override
    public void receiveFloat(String source, float x) {
    }

    @Override
    public void receiveSymbol(String source, String symbol) {
    }

    @Override
    public void receiveMessage(String source, String symbol, Object... args) {
    }
}
