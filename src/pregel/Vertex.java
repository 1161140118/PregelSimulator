package pregel;

import java.io.BufferedWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author standingby
 *
 * @param <V> VatexValue
 * @param <E> EdgeValue
 * @param <M> MessageValue
 */
public abstract class Vertex<V, E, M> {
    private Worker<V, E, M> worker;
    public final String vertexId;
    protected V vertexValue;
    private int superStep = 0;

    /**
     * ��Ƕ����Ծ״̬
     * <p>
     * true��Ĭ��ֵ�����������
     * <p>
     * false����������㣬������Ϣ��תΪtrue
     */
    private boolean active = true;

    protected Map<String, E> targets = new HashMap<>();

    private List<M> lastMessage = Collections.synchronizedList(new LinkedList<>());
    private List<M> curMessage = Collections.synchronizedList(new LinkedList<>());

    public Vertex(String vertexId) {
        super();
        this.vertexId = vertexId;
    }

    public Vertex(Worker<V, E, M> worker, String vertexId) {
        super();
        this.worker = worker;
        this.vertexId = vertexId;
    }

    public Vertex(Worker<V, E, M> worker, String vertexId, V vertexValue) {
        super();
        this.worker = worker;
        this.vertexId = vertexId;
        this.vertexValue = vertexValue;
    }

    abstract public void compute(List<M> msgs);


    protected void sendMessageTo(String target, M msg) {
        worker.addSentMessage(target, msg);
    }

    protected void addTarget(String target, E edgeValue) {
        targets.put(target, edgeValue);
    }

    public void voteToHalt() {
        active = false;
    }

    /**
     * cur ��Ϣ���е���Ϊ last��Ϣ����<p>
     * ���� cur ��Ϣ����
     */
    protected List<M> resetMessages() {
        lastMessage.clear();
        if (curMessage.isEmpty()) {
            return lastMessage;
        }
        // ������Ϣ����Ϊactive
        active = true;
        lastMessage.addAll(curMessage);
        curMessage.clear();
        return lastMessage;
    }

    /**
     * ��Woker���ã�����Ϣ�����������Ϣ
     * @param msg ������Ϣ
     */
    protected synchronized void addNewMessage(M msg) {
        curMessage.add(msg);
        active = true;
    }

    protected synchronized void addNewMessages(List<M> msgs) {
        curMessage.addAll(msgs);
        active = true;
    }



    public boolean isActive() {
        return active;
    }

    public int getSuperStep() {
        return superStep;
    }

    public V getVertexValue() {
        return vertexValue;
    }

    public int getVerticesNum() {
        return worker.getVerticesNum();
    }

    protected void incSuperStep() {
        superStep++;
    }

    public void setWorker(Worker<V, E, M> worker) {
        this.worker = worker;
    }

    public int getWorkerId() {
        return worker.id;
    }

    public String resultFormater() {
        return toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String result = "";
        for (String target : targets.keySet()) {
            result += vertexId + "\t" + target + "\n";
        }
        if (targets.isEmpty()) {
            result = vertexId + "\n";
        }

        return result;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((vertexId == null) ? 0 : vertexId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vertex other = (Vertex) obj;
        if (vertexId == null) {
            if (other.vertexId != null)
                return false;
        } else if (!vertexId.equals(other.vertexId))
            return false;
        return true;
    }

}
