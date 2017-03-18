package org.powertac.visualizer.web.dto;

/**
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
public class Message {

    public static enum Type {
        DATA, INFO, INIT
    }

    private Type type;
    private String gameName;
    private Object message;
    private long time;

    public Message() {

    }

    public Message(Type type, String gameName, Object message) {
        this.type = type;
        this.gameName = gameName;
        this.message = message;
        this.time = System.currentTimeMillis();
    }

    public String getGame() {
        return gameName;
    }

    public void setGame(String gameName) {
        this.gameName = gameName;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

}
