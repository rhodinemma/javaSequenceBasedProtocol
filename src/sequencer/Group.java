package sequencer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.*;
import java.util.Date;

public class Group implements Runnable {
    protected InetAddress receivedAddress;
    int maxBuffer = 1024;
    protected int PORT = 5554;
    MulticastSocket multicastSocket;
    String myAddress;
    Sequencer sequencer;
    long lastSequenceRecd;
    long lastSequenceSent;
    long lastSendTime;
    String myName;
    Thread heartBeater;
    DatagramPacket packet;
    private final MsgHandler handler;

    public Group(String host, MsgHandler handler, String senderName) {
        lastSequenceRecd=-1L;
        lastSequenceSent=-1L;

        this.handler = handler;

        try {
            //socket to receive multicast messages from the sequencer
            multicastSocket = new MulticastSocket(PORT);

            //getting the host address to join group
            receivedAddress = InetAddress.getByName(host);

            //joining a group here
            multicastSocket.joinGroup(receivedAddress);

            //create a thread to listen to "this multicast socket"
            Thread myThread = new Thread(this);
            //connects to the run method
            myThread.start();
            heartBeater = new HeartBeater(5);
            heartBeater.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InetAddress getInetAddress() {
        return multicastSocket.getInetAddress();
    }

    public void send(byte[] msg) throws Exception {
        //check if there is a global/multicasting socket specified
        if(multicastSocket!=null) {
            try{
                //send the message to the sequencer so that it is marshalled.
                ++lastSequenceSent;
                packet = new DatagramPacket(msg, msg.length, receivedAddress, PORT);
                multicastSocket.send(packet);
            }catch(Exception e){
                System.out.println("Couldn't contact sequencer because of this issue " + e);
                throw new GroupException("Couldn't send to sequencer because " + e.getMessage());

            }
        }else{
            throw new GroupException("Group not joined");
        }
    }

    // leave group
    public void leave() {
        //check if there is a global/multicasting socket specified
        if(multicastSocket!=null)
        {
            try{
                multicastSocket.leaveGroup(receivedAddress);
                multicastSocket.close();
            }catch(Exception e){System.out.println("Couldn't leave group " + e);}
        }
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public void run()
    {
        // repeatedly: listen to MulticastSocket created in constructor, and on receipt
        // of a datagram call "handle" on the instance
        // of Group.MsgHandler which was supplied to the constructor
        try{
            while(true){

                byte[] buffer = new byte[maxBuffer];

                //create the datagram packet to receive the multicast message
                packet = new DatagramPacket(buffer,buffer.length);
                multicastSocket.receive(packet);

                //unmarshall
                ByteArrayInputStream byteStream = new ByteArrayInputStream(buffer,0,packet.getLength());
                DataInputStream dataStream = new DataInputStream(byteStream);

                long gotSequence = dataStream.readLong();
                int count =dataStream.read(buffer);
                long wantSeq = lastSequenceRecd + 1L;

                if(lastSequenceRecd>=0 && wantSeq<gotSequence){
                    for(long getSeq=wantSeq;getSeq<gotSequence;getSeq++){
                        byte[] extra = sequencer.getMissing(myAddress,getSeq);
                        int countExtra = extra.length;
                        System.out.println("get missing sequence number "+ getSeq);
                        handler.handle(countExtra,extra);
                    }
                }
                lastSequenceRecd = gotSequence;
                handler.handle(count,buffer);
            }
        }catch(Exception e){
            System.out.println("error"+e);
            e.printStackTrace();
        }
    }

    public interface MsgHandler {
        void handle(int count, byte[] msg);
    }

    public static class GroupException extends Exception {
        private static final long serialVersionUID = 1L;
        public GroupException(String s) {
            super(s);
        }
    }

    public class HeartBeater extends Thread
    {
        // This thread sends heartbeat messages when required
        public void run(){
            do {
                try{
                    do {
                        Thread.sleep(period * 1000L);
                    }while((new Date()).getTime() - lastSendTime< (period* 1000L));
                    sequencer.heartbeat(myName, lastSequenceRecd);
                }catch(Exception ignored){}
            }
            while(true);
        }
        int period;
        public HeartBeater(int period){
            this.period=period;
        }
    }
}
