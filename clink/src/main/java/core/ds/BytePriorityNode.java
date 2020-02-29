package core.ds;

/**
 * Created by wangkang on 19/7/27.
 */
public class BytePriorityNode<Frame> {
    public byte priority;
    public Frame item;
    public BytePriorityNode<Frame> next;

    public BytePriorityNode(Frame item) {
        this.item = item;
    }

    public void appendWithPriority(BytePriorityNode<Frame> node) {
        if (next == null) {
            this.next = node;
        } else {
            BytePriorityNode<Frame> after = this.next;
            if (after.priority < node.priority) {
                this.next = node;
                node.next = after;
            } else {
                after.appendWithPriority(node);
            }
        }
    }
}
