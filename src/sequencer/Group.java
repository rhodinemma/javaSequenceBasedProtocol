package sequencer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.Serial;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.util.Arrays;
import java.util.Date;

public class Group implements Runnable {
    protected InetAddress grpIpAddress;
    int maxBuffer = 1024;
    protected int port = 5554;
    MulticastSocket multicastSock;
    DatagramPacket packet;
    InetAddress localhost;
    String myAddress;
    Sequencer sequencer;
    long lastSequenceRecd;
    long lastSequenceSent;
    long lastSendTime;
    String myName;
    Thread heartBeater;
    MsgHandlerImpl handler;
    DatagramSocket socket;
    public static String groupIPAddress = "224.6.7.8";

    public Group(String host, MsgHandlerImpl handler, String senderName) throws IOException, NotBoundException, SequencerException {
        lastSequenceRecd=-1L;
        lastSequenceSent=-1L;

        socket = new MulticastSocket(port);
        //socket to receive multicast messages from the sequencer

        Sequencer sequencer = (Sequencer) Naming.lookup("//localhost/Sequencer");
        //instantiate sequencer via rmi using the name on which it bound itself

        //gets the address of the local host
        localhost = InetAddress.getLocalHost();

        //combines the localhost address and the senders name
        myAddress = senderName+localhost;

        SequencerJoinInfo info = sequencer.join(host);

        //retrieve group ip address form join in sequencerimpl class
        grpIpAddress = info.addr;

        //create socket where to listen
        multicastSock = new MulticastSocket(5554);

        //join group
        multicastSock.joinGroup(new InetSocketAddress(InetAddress.getByName("224.6.7.8"), 5554), NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));

        this.handler = handler;

        //create a thread
        Thread myThread = new Thread(this);
        //connects to the run method
        myThread.start();
        heartBeater = new HeartBeater(5);
        heartBeater.start();
    }

    public void send(byte[] msg) throws Exception {
        //check if there is a global/multicasting socket specified
        if(multicastSock!=null) {
            try{
                Sequencer sequencer = (Sequencer) Naming.lookup("//localhost/Sequencer");

                //send the message to the sequencer so that it is marshalled.
                ++lastSequenceSent;
                // Create a string from the byte array with "UTF-8" encoding
                String string = new String(msg);
                System.out.println(string);
                System.out.println("Message contains " + myAddress + "," + Arrays.toString(msg) + "," + lastSequenceSent + "," + lastSequenceRecd);
                sequencer.send(groupIPAddress,msg,lastSequenceSent,lastSequenceRecd);

                /*++lastSequenceSent;
                String str = new String(msg);
                String[] strArr = str.split(" ");  //split message received from client into constituents for special treatment
                byte[] msgStr = strArr[0].getBytes();
                long msgID = Long.parseLong(String.valueOf(lastSequenceSent));
                long msgSeq = Long.parseLong(String.valueOf(lastSequenceRecd));
                sequencer.send(groupIPAddress, msgStr, msgID, msgSeq);*/

                //change the last send time to long
                lastSendTime=(new Date()).getTime();
            }catch(Exception e){
                System.out.println("Couldn't contact sequencer because of this issue " + e);
                throw new GroupException("Couldn't send to sequencer");

            }
        }else{
            throw new GroupException("Group not joined");
        }
    }

    // leave group
    public void leave() {
        //check if there is a global/multicasting socket specified
        if(multicastSock!=null)
        {
            try{
                Sequencer sequencer = (Sequencer) Naming.lookup("//localhost/Sequencer");
                socket = new DatagramSocket();
                multicastSock.leaveGroup(new InetSocketAddress(InetAddress.getByName("224.6.7.8"), 5554), NetworkInterface.getByInetAddress(InetAddress.getLocalHost()));
                sequencer.leave(myAddress);
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
                //System.out.println("Hello Rhodin");
                byte[] buffer = new byte[maxBuffer];

                //create the datagram packet to receive the multicast message
                packet = new DatagramPacket(buffer,buffer.length);
                multicastSock.receive(packet);

                //unmarshal
                ByteArrayInputStream bstream = new ByteArrayInputStream(buffer,0,packet.getLength());
                DataInputStream dstream = new DataInputStream(bstream);

                long gotSequence = dstream.readLong();
                int count =dstream.read(buffer);
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
        @Serial
        private static final long serialVersionUID = 1L;
        public GroupException(String s) {
            super(s);
        }
    }

    public class HeartBeater extends Thread
    {
        // This thread sends heartbeat messages when required
        @SuppressWarnings("InfiniteLoopStatement")
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
