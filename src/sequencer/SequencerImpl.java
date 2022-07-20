package sequencer;

import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SequencerImpl implements Sequencer {
    History history;
    Group group;
    ExecutorService executorService;
    Vector<String> Clients;
    int sequenceNumber;

    public SequencerImpl(String host, Group.MsgHandler handler, String senderName) {
        executorService = Executors.newSingleThreadExecutor();
        history = new History();
        Clients = new Vector<>();

        group = new Group(host, handler, senderName);
        executorService.execute(group);
    }
    
    @Override
    public SequencerJoinInfo join(String sender) throws SequencerException {
        if (Clients.contains(sender)) {
            throw new SequencerException(sender + "exists");
        } else {
            Clients.addElement(sender);
            history.noteReceived(sender, sequenceNumber);
            InetAddress address = group.getInetAddress();
            return new SequencerJoinInfo(address, sequenceNumber);
        }
    }

    @Override
    public void send(String sender, byte[] msg, long msgID, long lastSequenceReceived) throws RemoteException {
        try {
           group.send(msg);
           history.noteReceived(sender, lastSequenceReceived);
           history.addMessage(sender, sequenceNumber, msg);
        } catch (Exception ex) {
            System.out.println("Couldn't send message because" + ex);
        }
    }

    @Override
    public void leave(String sender) throws RemoteException {
        // remove the client name from the sequencer list
        Clients.removeElement(sender);
        group.leave();
        executorService.shutdownNow();
        // remove from our history file
        history.eraseSender(sender);
    }

    @Override
    public byte[] getMissing(String sender, long sequence) throws RemoteException, SequencerException {
        byte[] exist = history.getMessage(sequence);

        if (exist != null) {
            System.out.print("Sequencer gets missing " + sequence);
            return exist;
        } else {
            System.out.print("Sequencer couldn't get sequence number " + sequence);
            throw new SequencerException("Couldn't get sequence number " + sequence);
        }
    }

    @Override
    public void heartbeat(String sender, long lastSequenceReceived) throws RemoteException {
        System.out.print(sender + "Heartbeat: " + lastSequenceReceived);
        history.noteReceived(sender, lastSequenceReceived);
    }
}
