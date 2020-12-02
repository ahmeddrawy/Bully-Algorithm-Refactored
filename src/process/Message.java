package process;

public class Message {
    private String Host = null ;
    private int Port = 0 ;
    private long timeStamp = 0 ;
    private ContentType Content = null ;
    private String body = " ";
    public enum  ContentType{
        NEW,ALIVE,ELECTION,OK,LIST,ADD_PEER,VICTORY,REMOVE_PEER
    }
    Message(String message){
        String tmp[] = message.split(",");
        timeStamp=Long.parseLong( tmp[0]);
        Host = tmp[1];
        Port = Integer.parseInt(tmp[2]);
        Content = ContentType.valueOf(tmp[3]);
        if(tmp.length >4)
            body = tmp[4];
        else
            body =" ";
    }
    Message(long timeStamp,String host ,int port , ContentType contentType ){
        this(timeStamp ,host , port ,contentType," ");
    }
    Message(long timeStamp,String host ,int port , ContentType contentType,String body ){
        this.timeStamp = timeStamp;
        this.Host = host;
        this.Port = port;
        this.Content = contentType;
        this.body = body;
    }
    public String toString(){
        return timeStamp+","+Host +"," +Port+","+Content+","+body;
    }

    public String getHost() {
        return Host;
    }

    public int getPort() {
        return Port;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public ContentType getContent() {
        return Content;
    }

    public void setPort(int port) {
        Port = port;
    }

    public void setHost(String host) {
        Host = host;
    }

    public void setContent(ContentType content) {
        Content = content;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
