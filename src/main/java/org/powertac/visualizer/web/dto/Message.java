package org.powertac.visualizer.web.dto;

/**
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
public class Message {

    public static enum Type {
        DATA, INFO, INIT
    }

    private Type type;
    private Object message;
    private long time;

    public Message() {

    }

    public Message(Type type, Object message) {
        this.type = type;
        this.message = message;
        this.time = System.currentTimeMillis();
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
