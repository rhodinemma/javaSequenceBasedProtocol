package sequencer;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TestSequencer extends MsgHandlerImpl implements Group.MsgHandler,Runnable {
    String returned;
    Group group;
    Thread myThread;
    String clientName;
    boolean paused;
    int rate;

    public  TestSequencer(String host,String clientName){
        MsgHandlerImpl hand = new MsgHandlerImpl();
        returned = "Fred";
        paused = false;
        this.clientName=clientName;
        try{
            group = new Group(host,hand,clientName);
            myThread = new Thread(this);
            myThread.start();
        }catch(Exception grp){
            System.out.println("Can't create group " + grp);
            grp.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if(args.length<2){
            System.out.println("we need more arguments");

        }else{
            TestSequencer ts = new TestSequencer(args[0],args[1]);
        }
//        TestSequencer ts = new TestSequencer("192.168.43.165","Fred");
    }

    public void run(){
        try{
            rate = 8;
            int i = 0;
            do{
                do{
                    if(rate<=90)
                        try{
                            Thread.sleep((90-rate)* 10L);
                        }catch(Exception ignored){}

                }while(paused);
                BufferedReader write = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Enter your message:");
                String message= write.readLine();

                if(message.trim().equals("exit")){
                    group.leave();
                    System.exit(1);
                }
                group.send((clientName + message + i++).getBytes());
            }while(true);
        }
        catch(Exception ignored){}
    }

    public void handle(int count, byte[] msg){
        String msg1 = new String(msg,0,count);
    }
}
