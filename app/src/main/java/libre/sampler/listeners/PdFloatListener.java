package libre.sampler.listeners;

import org.puredata.core.PdListener;

public abstract class PdFloatListener implements PdListener {
    @Override
    public void receiveBang(String source) {
    }

    @Override
    public void receiveSymbol(String source, String symbol) {
    }

    @Override
    public void receiveList(String source, Object... args) {
    }

    @Override
    public void receiveMessage(String source, String symbol, Object... args) {
    }
}
