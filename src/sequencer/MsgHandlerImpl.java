package sequencer;

public class MsgHandlerImpl implements Group.MsgHandler {
    public MsgHandlerImpl() {
    }

    @Override
    public void handle(int count, byte[] msg) {
        String mg = new String(msg);
        String[] mgs = mg.split(" ");
        System.out.println("Message received is: " + mgs[0]);
    }
}
