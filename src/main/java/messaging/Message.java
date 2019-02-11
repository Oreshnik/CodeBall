package messaging;

import mymodel.Robot;

public class Message {
    public MessageType type;
    public int tickNumber;
    public Object value;
    public int intValue;
    public Robot robot;

    public Message(MessageType messageType, int tickNumber) {
        this.type = messageType;
        this.tickNumber = tickNumber;
    }

    public Message(MessageType messageType, int tickNumber, Object value) {
        this(messageType, tickNumber);
        this.value = value;
    }

    public Message(MessageType messageType, int tickNumber, Object value, int intValue) {
        this(messageType, tickNumber, value);
        this.intValue = intValue;
    }

    public Message(MessageType messageType, int tickNumber, Object value, int intValue, Robot robot) {
        this(messageType, tickNumber, value, intValue);
        this.robot = robot;
    }
}
