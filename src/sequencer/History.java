package sequencer;

import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.Hashtable;

public class History extends Hashtable<String, Long> {
    Hashtable<String, Long> history;
    public long historyCleanedTo;

    //Create constructor for history
    public History(){
        //Creating a hashTable to store the contents of the message, sender, sequence number.
        history = new Hashtable<>();
        historyCleanedTo = -1L;
    }

    //Adding the sender to history
    public void noteReceived(String sender, long lastSequenceReceived) {
        history.put(sender, lastSequenceReceived);
    }

    //Add the message to history
    public void addMessage(String sender, int sequenceNumber, byte[] msg) {
        //check if the message is not null.
        if (msg!=null){
            put(Long.toString(sequenceNumber), (long) msg[0]);
        }

        //removing already received values in the buffer
        if(size()>1024){
            long min = 0x7fffffL;

            //Getting the different senders in the hash table
            for(Enumeration<String> enum1 = history.keys(); enum1.hasMoreElements();){
                String sent = enum1.nextElement();
                long have = history.get(sent); // the already received datagram
                if(have<min)
                    min = have; //set the new minimum number;
            }

            //removing datagrams that are not needed in the buffer
            for(long s = historyCleanedTo+1L; s<=min; s++){
                remove(s);
                historyCleanedTo = s;
            }
        }
    }

    // Remove sender
    public synchronized void eraseSender(String sender) {
        history.remove(sender);
    }

    //Get the missing datagram
    public byte[] getMessage(long sequence) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.putLong(sequence);
        return buffer.array();
    }
}
