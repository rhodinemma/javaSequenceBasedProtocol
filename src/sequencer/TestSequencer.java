package sequencer;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;

public class TestSequencer {
    static Stack<Long> sequences;
    static Sequencer testsequencer;


    public static void main(String[] args) throws IOException {
        sequences = new Stack<>();

        //static multicast IPAddress
        String multicastAddress = "234.20.7.1";

        // Getting input from the user
        String sender = null;
        Scanner input = new Scanner(System.in);
        System.out.print("Enter your name: ");
        sender = input.nextLine();
        String finalSender = sender;
        System.out.println(finalSender);
        Group.MsgHandler handler = (count, msg) -> {
            try {
                Message messageFrom = Message.fromByteStream(msg);
                String message = new String(messageFrom.getMsg());

                if (messageFrom.getMsgID() != -1) {
                    System.out.println("Message from " + messageFrom.getSender() + ": " + message);
                }else {
                    System.out.println("Pinging: " + messageFrom.getSender() + ": " + message);
                }

                if (messageFrom.getMsgID() != -1) {
                    sequences.push(messageFrom.getLastSequence());
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw new RuntimeException(e);
            }
        };

        testsequencer = new SequencerImpl(multicastAddress, handler, sender);

        try {
            testsequencer.join(sender);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}