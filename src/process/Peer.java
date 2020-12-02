package process;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


/// peer is responsible for sending and receiving only
public class Peer {
    final int NO_RESPONSE_SPAN = 5000 ; /// in milliseconds, received any message in 3 seconds, alive or election
    private int port = 8090;
    private String host = "127.0.0.1"; /// default, all on local host
    private ServerSocket serverSocket = null;
    private boolean active = true ;
    Process process = null;
    Peer(int port){
        this.port= port;
    }
    String sendAndGetRespone(Peer peer , Message message , int timeOut){
        try{
            Socket s=new Socket(peer.getHost(),peer.getPort());
            System.out.println("sending "+message.toString()+" to "+ peer.getPort());
            s.setSoTimeout(timeOut);
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            DataInputStream din = new DataInputStream(s.getInputStream());
            dout.writeUTF(message.toString());
            String response = din.readUTF(); /// we need to decode response
            process.decodeResponse(response);
            dout.flush();
            dout.close();
            s.close();
            return response;
        }catch(Exception e){System.out.println(e +" " + peer.getPort());}
        return null;
    }
    Message receiveAndGiveResponse(int timeOut){
        try{
//            ServerSocket ss=new ServerSocket(this.getPort());
            if(timeOut> 0)
                serverSocket.setSoTimeout(timeOut);
            Socket s=serverSocket.accept();//establishes connection
            DataInputStream din=new DataInputStream(s.getInputStream());
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            String str=din.readUTF();
            System.out.println("received message :  "+str);
            Message message = process.encodeResponse(str);
            String response = message.toString();
            System.out.println("responded with " +response);
            dout.writeUTF(response);
            dout.flush();
            dout.close();
            din.close();
            return new Message(str);
        }catch(Exception e){
            /// if we timed out and !AMACoordinator, we send election
            if(!process.isAMA_COORDINATOR())
                active = false;

        }
        return null;
    }
    void Listen(){
        System.out.println("I'm listening to " + this.getPort());
        while(active){
            if(process.isAMA_COORDINATOR()){
                /// when timed out the socket is not closed so we don't need to open it again
                if(serverSocket == null||serverSocket.isClosed())
                    bindServerSocket();
                receiveAndGiveResponse(1000); ///listen for 2 second and send alive wait indefinitely
                process.sendAlive();
            }else {
                /// when timed out the socket is not closed so we don't need to open it again
                if(serverSocket == null||serverSocket.isClosed())
                    bindServerSocket();

                receiveAndGiveResponse(NO_RESPONSE_SPAN ); /// wait 3 seconds
            }
        }
        System.out.println(this.getPort()+" no coordinator");
        if(!process.isAMA_COORDINATOR())
            process.notifyElection();

    }



    boolean bindServerSocket(){
        System.out.println( "binding "+ this.getPort());
        try {
            serverSocket = new ServerSocket(this.getPort());
            return true;
        } catch (Exception e) {
            System.out.println("can't bind here "+this.getPort());
//            e.printStackTrace();
        }
        return false;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }


    public void setProcess(Process process) {
        this.process = process;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
