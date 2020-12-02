package process;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/*

    <timestamp,sender,message>
    messages                response
    0 - hey                 4 list or no answer
    1 - coordinator alive   3 ok - add time to last time and compare
    2 - election            3 ok or 6 victory
    resonses codes
    3 - ok, i received      no response
    4 - list of hosts is coming
    5 - new peer            3 ok and add peer to list
    6 - victory
 */
/// peer is responsile for sending and receiving only
public class Peer {
    static final int COORDINATOR_DEFAULT =9090;
    final int MAX_LIFE = 5000 ; /// in milliseconds
    final int NO_RESPONSE_SPAN = 5000 ; /// in milliseconds, received any message in 3 seconds, alive or election
    private int port = 8090;
    private String host = "127.0.0.1"; /// default, all on local host
    private int defaultTimeOut = 1000;
    private ServerSocket serverSocket = null;
    List<Peer> peers = new ArrayList<>();
    private boolean active = true ;
    private boolean AMA_COORDINATOR = false ;
    String last_response = null;
    Decoder decoder = null;
    int smallerCount = 0 ;
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
//            System.out.println("we got response "+ response);
            /*
                todo handle responses
             */
            /// moved up level
            /// todo fix
            decoder.decodeResponse(response);
            dout.flush();
            dout.close();
            s.close();
            return response;
        }catch(Exception e){System.out.println(e +" " + peer.getPort());}
        return null;
    }
    boolean receiveAndGiveResponse(int timeOut){
        try{
//            ServerSocket ss=new ServerSocket(this.getPort());
            if(timeOut> 0)
                serverSocket.setSoTimeout(timeOut);
            Socket s=serverSocket.accept();//establishes connection
            DataInputStream din=new DataInputStream(s.getInputStream());
            DataOutputStream dout=new DataOutputStream(s.getOutputStream());
            String str=din.readUTF();
            System.out.println("received message :  "+str);
            // TODO: 11/30/2020  handle the cases when str is null
            /// todo move to upper layer
            Message message = decoder.encodeResponse(str);
            String response = message.toString();
            System.out.println("responded with " +response);
            dout.writeUTF(response);
            dout.flush();
            dout.close();
            din.close();
//            serverSocket.close();
            return true;
        }catch(Exception e){
            // TODO: 12/2/2020
            /// if we timed out, we send election
//            / if timed out and amacoordinator then we do nothing
//            e.printStackTrace();
            if(!this.isAMA_COORDINATOR())
                active = false;
            //System.out.println(e);

        }
        return false;
    }

    void Listen(){
        System.out.println("I'm listening to " + this.getPort());
        while(active){
            if(AMA_COORDINATOR){
                /// when timed out the socket is not closed so we don't need to open it again
                if(serverSocket == null||serverSocket.isClosed())
                    bindServerSocket();
                receiveAndGiveResponse(1000); ///listen for 2 second and send alive wait indefinitely


                ///todo
                decoder.sendAlive();
            }else {
                /// when timed out the socket is not closed so we don't need to open it again
                if(serverSocket == null||serverSocket.isClosed())
                    bindServerSocket();

                receiveAndGiveResponse(NO_RESPONSE_SPAN ); /// wait 3 seconds
//                receiveAndGiveResponse(NO_RESPONSE_SPAN + Math.max(0,smallerCount)* decoder.AliveTimeOut); /// wait 3 seconds
            }
        }
        System.out.println(this.getPort()+" no coordinator");
        /// todo notifyElection();
        if(!isAMA_COORDINATOR())
            decoder.notifyElection();

    }

    void addNewPeer(){
        ///coordinator only
        if(this.peers.size() ==0 ){
            this.peers.add(new Peer(COORDINATOR_DEFAULT +1)) ;
        }
        else {
            int sz = this.peers.size();
            int last = this.peers.get(sz -1).getPort();

            this.peers.add(new Peer( last+ 1));
        }

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

    public List<Peer> getPeers() {
        return peers;
    }
    public void setPort(int port) {
        this.port = port;
    }

    public void setPeers(List<Peer> peers) {
        this.peers = peers;
    }

    public void setAMA_COORDINATOR(boolean AMA_COORDINATOR) {
        this.AMA_COORDINATOR = AMA_COORDINATOR;
    }

    public boolean isAMA_COORDINATOR() {
        return AMA_COORDINATOR;
    }

    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
