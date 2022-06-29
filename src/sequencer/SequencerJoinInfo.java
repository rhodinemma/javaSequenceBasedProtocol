package sequencer;

import java.io.Serializable;
import java.net.InetAddress;

public class SequencerJoinInfo implements Serializable {
    public InetAddress addr;
    public long sequence;

    public SequencerJoinInfo(InetAddress addr, long sequence) {
        this.addr = addr;
        this.sequence = sequence;
    }
}
