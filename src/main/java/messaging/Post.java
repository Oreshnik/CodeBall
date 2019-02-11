package messaging;

import java.util.ArrayList;
import java.util.List;

public class Post {
    private List<Message> postBox;
    public Post() {
        postBox = new ArrayList<>();
    }

    public void update(int gameTick) {
        //remove old mails
        postBox.removeIf(m -> gameTick - m.tickNumber > 1);
    }

    public void putMessage(Message message) {
        postBox.add(message);
    }

    public Message getMessage(MessageType type) {
        for (int i = 0; i < postBox.size(); i++) {
            if (postBox.get(i).type.equals(type)) {
                return postBox.get(i);
            }
        }
        return null;
    }

    public Message getMessage(MessageType type, int robotId) {
        for (int i = 0; i < postBox.size(); i++) {
            if (postBox.get(i).type.equals(type)
                    && (postBox.get(i).robot == null || postBox.get(i).robot.id != robotId)) {
                return postBox.get(i);
            }
        }
        return null;
    }
}
