package process;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Decoder {
    Peer myPeer =null;
    final int AliveTimeOut = 200;
    final int VICTORY_TIMEOUT = 100;

    Decoder(){
        myPeer = new Peer(Peer.COORDINATOR_DEFAULT);
        myPeer.setDecoder(this);
    }
    void run(){
        if(this.sendHeyToCoordinator()!=null){
            ///we have coordinator
            myPeer.Listen();
        }
        else{
            /// we dont have coordinator create one
            System.out.println("can't conenct to coordinator");
            myPeer.setAMA_COORDINATOR(true);
            myPeer.Listen();
        }
    }
    Message encodeResponse(String response){
        /// received this msg and encode a proper response and handle actions
        Message message= new Message(response);
        String messageBody=  message.getBody();
        Message.ContentType contentType = message.getContent();
        switch (contentType){
            case NEW: /// if we receive 1
                /// if we received new peer we send list of other peers
                /// adding coordinator port and other ports including last which is the port the receiver will be listening to
                myPeer.smallerCount +=1;
                myPeer.addNewPeer();
                notifyWithNewPeer();
                return new Message(getNowTimeStamp(),myPeer.getHost(),myPeer.getPort(),Message.ContentType.LIST,encodePeers(myPeer));
//            case ELECTION:
            /// reduntant now
            /// we received election message
            /// if message from peer with lower priority and we have no higher peers than me respond with victory
            //  otherwise respond with ok then make election my self
            // TODO: 12/1/2020

//                return notifiedWithElection(response) ;
            ///case '5' we received new peer respond with okay
            case VICTORY:
                /// decrease the number of smaller
                // TODO: 12/2/2020
                /// we need to remove the peer
                removePeer(Integer.parseInt(messageBody));
                myPeer.setActive(true);
                return new Message(getNowTimeStamp(),myPeer.getHost(),myPeer.getPort(), Message.ContentType.OK);
            case ADD_PEER:
                notifiedWithNewPeer(message);
                return new Message(getNowTimeStamp() , myPeer.getHost()
                        ,myPeer.getPort(), Message.ContentType.OK,"Okay");
            default:
                return new Message(getNowTimeStamp(),myPeer.getHost(),myPeer.getPort(), Message.ContentType.OK);
        }
    }
    void decodeResponse(String response ){
        /// sent msg and got this as response
        myPeer.last_response = response;
        Message message= new Message(response);
        String msg=  message.getBody();
        int sender = message.getPort();
        long timestamp = message.getTimeStamp();
        Message.ContentType contentType = message.getContent();
        // TODO: 12/2/2020  check if i need timestamps
        //        checkTimeStampAndSender(timestamp , sender);
        ///receive c as response
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
    Message encodeMessage(Message.ContentType contentType){
        return encodeMessage(contentType," ");
    }
    Message encodeMessage( Message.ContentType contentType,String body){
        switch (contentType){
            case ADD_PEER:
                return new Message(getNowTimeStamp(),myPeer.getHost(),myPeer.getPort(), contentType,body);
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
        myPeer.setPeers(l);
    }
    long getNowTimeStamp(){
        return new Timestamp(System.currentTimeMillis()).getTime();
    }
    String encodePeers(Peer peer){
        String ret= peer.getPort() +"";
        for(Peer p : peer.getPeers()){
            ret += (" " + p.getPort() );
        }
        return ret;
    }
    List<Peer> decodePeers(String body){
        System.out.println("body of msg " + body);
        List<Peer> ret = new ArrayList<>();
        for(String num : body.split(" ")){
            ret.add(new Peer(Integer.parseInt(num)));
        }
        return ret;
    }
    void notifyElection() {
        // TODO: 12/1/2020
        /// we are supposed to be having the list in ascending order so we find our index
        /// then we notify all peers with higher priority (which is here with lower port number)//
        /// we need to
        Message message = new Message(getNowTimeStamp(),myPeer.getHost(),myPeer.getPort(), Message.ContentType.ELECTION);
        // TODO: 12/2/2020 we need to send this message to all peers and get n-2
        message.setBody(myPeer.getPort() + "");
        for (Peer peer : myPeer.getPeers()){
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    myPeer.sendAndGetRespone(peer,message,100);
                }
            });
            t.start();
        }
        boolean isCoordinator = true;
        for (int i = 0 ; i <myPeer.getPeers().size()-1 ; ++i){
//            if(timeOut> 0)
//                serverSocket.setSoTimeout(timeOut);
            Socket s= null;//establishes connection
            try {
                System.out.println("listening to others");
                myPeer.bindServerSocket();
                myPeer.getServerSocket().setSoTimeout(4000);
                s = myPeer.getServerSocket().accept();
                DataInputStream din=new DataInputStream(s.getInputStream());
                DataOutputStream dout=new DataOutputStream(s.getOutputStream());
                String str=din.readUTF();
                System.out.println("received message :  "+str);
                // TODO: 11/30/2020  handle the cases when str is null
                /// todo move to upper layer
                Message message1 = new Message(str);            /// received
                Message encodeResponse = this.encodeResponse(str);
                String response = encodeResponse.toString();
                System.out.println("responded with " +response);
                dout.writeUTF(response);
                dout.flush();
                dout.close();
                din.close();
                if(message1.getContent() == Message.ContentType.VICTORY){
                    isCoordinator = false   ;
//                    break;
                }
                else if(message1.getContent() == Message.ContentType.ELECTION){
                    System.out.println("election from "+ message1.getBody());
                    if(Integer.parseInt( message1.getBody()) < myPeer.getPort()){
                        isCoordinator=  false;
//                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(isCoordinator)
            notifyVictory();
//
        myPeer.Listen();

    }

    void notifyVictory(){
        System.out.println("Victory from "+ myPeer.getPort());
        /// decrease number of smaller
        int oldPort = myPeer.getPort();
        try {
            myPeer.getServerSocket().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        myPeer.setActive(true);
        myPeer.setAMA_COORDINATOR(true);
        myPeer.setPort(Peer.COORDINATOR_DEFAULT);
        removePeer(new Peer(Peer.COORDINATOR_DEFAULT));/// remove coordinator
        broadcast(new Message(getNowTimeStamp(),myPeer.getHost()
                ,myPeer.getPort(), Message.ContentType.VICTORY,""+oldPort), VICTORY_TIMEOUT);
        myPeer.Listen();
    }
    void notifyWithNewPeer(){
        /// used in coordinator
        /// don't notify last one he already got response
        /// last in list is the new peer
        /// todo specify timeout
        System.out.println("notifying other with the new ");
        for (int i = 0; i < myPeer.getPeers().size()-1 ; i++) {
            myPeer.sendAndGetRespone(myPeer.getPeers().get(i) ,encodeMessage(
                    Message.ContentType.ADD_PEER,""+myPeer.peers.get(myPeer.peers.size() -1).getPort()),1000);
        }

    }


    void notifiedWithNewPeer(Message message){
        int newPeerPort = Integer.parseInt(message.getBody());
        myPeer.peers.add(new Peer(newPeerPort));
        return ;
    }
    Peer getPeerByIndex(List <Peer> ret, int indx){
        for (int i = 0 ; i <ret.size() ;++i){
            if(i == indx){
                return ret.get(i);
            }
        }
        return null;
    }
    String sendHeyToCoordinator(){
        /// we create a peer here because the default settings is it of coordinator
        Message message = new Message(getNowTimeStamp(),myPeer.getHost(),myPeer.COORDINATOR_DEFAULT, Message.ContentType.NEW);

        /// response or null
        return myPeer.sendAndGetRespone(myPeer,message,4000); /// wait for 4 seconds
    }

    void broadcast(Message message ,int timeOut ){
        for (Peer peer:myPeer.getPeers() ) {
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
    void removePeer(Peer peer ){
        for (Peer p: myPeer.getPeers()) {
            if(p.getPort() == peer.getPort()){
                myPeer.getPeers().remove(p);
                return;
            }
        }
    }
    void removePeer(int port){
        Peer peer = new Peer(port);
        removePeer(peer);
    }
}
