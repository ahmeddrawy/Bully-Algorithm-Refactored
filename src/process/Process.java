package process;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Process {
    Peer myPeer =null;
    List<Peer> peers = new ArrayList<>();
    static final int COORDINATOR_DEFAULT =9090;
    final int AliveTimeOut = 200;
    final int VICTORY_TIMEOUT = 100;
    private boolean AMA_COORDINATOR = false ;
    Process(){
        /// initially our peer has the Default port
        myPeer = new Peer(COORDINATOR_DEFAULT);
        myPeer.setProcess(this);
    }
    void run(){
        if(this.sendHeyToCoordinator()!=null){
            ///we have coordinator
            myPeer.Listen();
        }
        else{
            /// we dont have coordinator create one
            System.out.println("can't conenct to coordinator");
            setAMA_COORDINATOR(true);
            myPeer.Listen();
        }
    }
    Message encodeResponse(String response){
        /// received this msg and encode a proper response and handle actions
        Message message= new Message(response);
        String messageBody=  message.getBody();
        Message.ContentType contentType = message.getContent();
        switch (contentType){
            case NEW: /// if we receive NEW
                /// if we received new peer we respond with list of other peers
                /// adding coordinator port and other ports including last which is the port the receiver will be listening to
                addNewPeer();
                notifyWithNewPeer();
                return new Message(getNowTimeStamp(),myPeer.getHost(),myPeer.getPort(),Message.ContentType.LIST,encodePeers(myPeer));
            case VICTORY:
                /// we need to remove the peer we got that won the election and become COORDINATOR
                removePeer(Integer.parseInt(messageBody));
                myPeer.setActive(true);
                return encodeMessage(Message.ContentType.OK);
//                return new Message(getNowTimeStamp(),myPeer.getHost(),myPeer.getPort(), Message.ContentType.OK);
            case ADD_PEER:
                /// we received new Peer and need to add it to peers list
                notifiedWithNewPeer(message);
                return encodeMessage(Message.ContentType.OK);
            default:
                return encodeMessage(Message.ContentType.OK);
        }
    }
    void decodeResponse(String response ){
        /// sent msg and got this as response
        Message message= new Message(response);
        String msg=  message.getBody();
        int sender = message.getPort();
        Message.ContentType contentType = message.getContent();
        switch (contentType){
            case LIST:
                System.out.println("received list of peers");
                notifiedListOfPeers(msg);
                break;
            case OK:
                /// received ok
                System.out.println("received okay from" + sender);
                break;
            default:
                System.out.println("we cannot resolve this response");
                break;
        }
    }

    /**
     * method overloading
     * @param contentType
     * @return
     */
    Message encodeMessage(Message.ContentType contentType){
        return encodeMessage(contentType," ");
    }
    /// utility function
    Message encodeMessage( Message.ContentType contentType,String body){
        switch (contentType){
            case ADD_PEER:
                return new Message(getNowTimeStamp(),myPeer.getHost(),myPeer.getPort(), contentType,body);
            case OK:
                return new Message(getNowTimeStamp() , myPeer.getHost(),myPeer.getPort(), Message.ContentType.OK);
            case NEW:
                return new Message(getNowTimeStamp() , myPeer.getHost(),myPeer.getPort(), Message.ContentType.NEW);
            default:
                // TODO: 12/2/2020
                return null;
        }
    }
    void notifiedListOfPeers( String body){
        List<Peer> l  =  decodePeers(body); /// remove first char
        Peer last = getPeerByIndex(l,l.size()-1);
        System.out.println(last.getPort());
        myPeer.setPort( last.getPort()); //setting my port as last in list
        l.remove(l.size()- 1);///remove myself -last-
        setPeers(l);
    }


    /**
     * we notify election by sending election message to all other processes
     * then listen to all processes
     * if i got an election from a process with higher priority which here is lower port
     * then i'm not coordinator
     * if i received victory then i'm not coordinator
     * else i'm coordinator
     *
     */
    void notifyElection() {
        Message message = new Message(getNowTimeStamp(),myPeer.getHost(),myPeer.getPort(), Message.ContentType.ELECTION);
        message.setBody(myPeer.getPort() + "");
        broadcast(message,100);
        boolean isCoordinator = true;
        for (int i = 0 ; i <peers.size()-1 ; ++i){
            Message message1 = myPeer.receiveAndGiveResponse(200);
            if(message1.getContent() == Message.ContentType.VICTORY){
                isCoordinator = false   ;
            }
            else if(message1.getContent() == Message.ContentType.ELECTION){
                System.out.println("election from "+ message1.getBody());
                if(Integer.parseInt( message1.getBody()) < myPeer.getPort()){
                    isCoordinator=  false;
                }
            }
        }
        if(isCoordinator)
            notifyVictory();
        myPeer.Listen();
    }

    /**
     * when i won the election i set my port to the defualt
     * i send my old port to others to remove from their lists
     */
    void notifyVictory(){

        System.out.println("Victory from "+ myPeer.getPort());
        int oldPort = myPeer.getPort();
        try {
            myPeer.getServerSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        myPeer.setActive(true);
        setAMA_COORDINATOR(true);
        myPeer.setPort(COORDINATOR_DEFAULT);
        removePeer(COORDINATOR_DEFAULT);/// remove coordinator
        broadcast(new Message(getNowTimeStamp(),myPeer.getHost()
                ,myPeer.getPort(), Message.ContentType.VICTORY,""+oldPort), VICTORY_TIMEOUT);
        myPeer.Listen();
    }
    void notifyWithNewPeer(){
        /// used in coordinator
        /// don't notify last one he already got response
        /// last in list is the new peer
        System.out.println("notifying other with the new ");
        for (int i = 0; i < peers.size()-1 ; i++) {
            myPeer.sendAndGetRespone(peers.get(i) ,encodeMessage(
                    Message.ContentType.ADD_PEER,""+peers.get(peers.size() -1).getPort()),1000);
        }

    }
    void notifiedWithNewPeer(Message message){
        int newPeerPort = Integer.parseInt(message.getBody());
        peers.add(new Peer(newPeerPort));
    }
    String sendHeyToCoordinator(){
        return myPeer.sendAndGetRespone(myPeer,encodeMessage(Message.ContentType.NEW),4000); /// wait for 4 seconds
    }
    /*
        send message to all other processes
     */
    void broadcast(Message message ,int timeOut ){
        for (Peer peer:peers ) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    myPeer.sendAndGetRespone(peer , message ,timeOut);
                }
            }) ;
            t.start();
        }
    }
    /// alive timeout 100ms
    void sendAlive(){
        broadcast(new Message(getNowTimeStamp(),myPeer.getHost(),myPeer.getPort(), Message.ContentType.ALIVE),AliveTimeOut);
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

    ///utility functions
    long getNowTimeStamp(){
        return new Timestamp(System.currentTimeMillis()).getTime();
    }
    /*
        i receive body of message as list of ports so i decode the body by splitting
        and encode when i want to send
     */
    String encodePeers(Peer peer){
        String ret= peer.getPort() +"";
        for(Peer p : peers){
            ret += (" " + p.getPort() );
        }
        return ret;
    }
    List<Peer> decodePeers(String body){
        List<Peer> ret = new ArrayList<>();
        for(String num : body.split(" ")){
            ret.add(new Peer(Integer.parseInt(num)));
        }
        return ret;
    }
    void removePeer(int port){
        for (Peer p: peers) {
            if(p.getPort() == port){
                peers.remove(p);
                return;
            }
        }
    }
    Peer getPeerByIndex(List <Peer> ret, int indx){
        for (int i = 0 ; i <ret.size() ;++i){
            if(i == indx){
                return ret.get(i);
            }
        }
        return null;
    }
    public boolean isAMA_COORDINATOR() {
        return AMA_COORDINATOR;
    }
    public void setAMA_COORDINATOR(boolean AMA_COORDINATOR) {
        this.AMA_COORDINATOR = AMA_COORDINATOR;
    }
    public void setPeers(List<Peer> peers) {
        this.peers = peers;
    }
}
