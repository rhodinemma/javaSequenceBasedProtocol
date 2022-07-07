package sequencer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serial;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SequencerImpl extends UnicastRemoteObject implements Sequencer {
    History history;
    Vector<String> Clients;
    InetAddress groupAddress;
    MulticastSocket multicastSocket;

    public static String groupIPAddress = "224.6.7.8";
    @Serial
    private static final long serialVersionUID = 1L;
    final int port = 5554;
    final int maxMessageLength = 1024;
    int sequenceNumber;
    public static InetAddress host;

    public SequencerImpl(String groupIPAddress) throws IOException {
        SequencerImpl.groupIPAddress = groupIPAddress;
        history = new History();
        Clients = new Vector<>();
        multicastSocket = new MulticastSocket(port);
    }

    public static void main(String[] args) {
        try {
            new Registry();
            SequencerImpl stub = new SequencerImpl(groupIPAddress);

            // linking sequencer object with RMI registry
            Naming.rebind("//localhost/Sequencer", stub);

            System.out.println("Sequencer bound in registry");
            System.out.println("Continue..");
        } catch (IOException ex) {
            Logger.getLogger(SequencerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public SequencerJoinInfo join(String sender) {
        if (Clients.contains(sender)) {
            try {
                throw new SequencerException(sender + " not unique");
            } catch (SequencerException e) {
                e.printStackTrace();
            }
        } else {
            Clients.addElement(sender);
            history.noteReceived(sender, sequenceNumber);
            return new SequencerJoinInfo(groupAddress, sequenceNumber);
        }
        return null;
    }

    @Override
    public void send(String sender, byte[] msg, long msgID, long lastSequenceReceived) throws RemoteException {
//        System.out.println(sender);
        try {
            host = InetAddress.getByName(sender);

            // Marshalling the data
            ByteArrayOutputStream bstream = new ByteArrayOutputStream(maxMessageLength);
            DataOutputStream dstream = new DataOutputStream(bstream);

            dstream.writeLong(lastSequenceReceived++);
            dstream.write(msg, 0, msg.length);

            DatagramPacket message = new DatagramPacket(bstream.toByteArray(), bstream.size(), host, port);
            System.out.println(message);
            multicastSocket.send(message);
        } catch (Exception ex) {
            System.out.println("Couldn't send message because" + ex);
        }

        history.noteReceived(sender, lastSequenceReceived);
        history.addMessage(sender, sequenceNumber, msg);
    }

    @Override
    public void leave(String sender) throws RemoteException {
        // remove the client name from the sequencer list
        Clients.removeElement(sender);

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

    // helper function to retrieve messages
    public void receive() throws IOException {
        byte[] buffer = new byte[maxMessageLength];

        DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);
        multicastSocket.receive(messageIn);

        String receivedMessage = new String(messageIn.getData());
        System.out.println(receivedMessage);
    }
}
