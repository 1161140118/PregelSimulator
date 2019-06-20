package pregel;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author standingby
 *
 * @param <V>
 * @param <E>
 * @param <M>
 */
public class Worker<V, E, M> implements Runnable {
    public final int id;
    private final Master<V, E, M> master;

    /** ��¼��ǰWorker����ά���Ľ�� */
    private Map<String, Vertex<V, E, M>> vertices = new HashMap<>();
    /** ����Ŀ��������worker������ */
    private Map<String, Worker<V, E, M>> vertexIn = new HashMap<>();
    /** ��������Ϣ���У��洢��ǰ��Ϣ */
    private Map<String, List<M>> messagesTobeSent = new HashMap<>();


    protected Worker(int id, Master<V, E, M> master) {
        this.id = id;
        this.master = master;
    }

    /**
     * �������и��ڵ��compute����
     */
    @Override
    public void run() {
        for (Vertex<V, E, M> vertex : vertices.values()) {
            List<M> msgs = vertex.resetMessages();
            if (vertex.isActive()) {
                vertex.compute(msgs);
                vertex.incSuperStep();
            }
        }
        sendMessages();
        // ������һ����ʾ�������
        master.countDownLatch.countDown();
    }


    /**
     * �жϽ��Vertex��
     * @return true�����нڵ��InActive
     */
    protected boolean allInactive() {
        for (Vertex<V, E, M> vertex : vertices.values()) {
            if (vertex.isActive()) {
                return false;
            }
        }
        return true;
    }

    /**
     * ��Vertex���ã����Ͷ�������Ӵ�������Ϣ
     * @param targetId
     * @param msg
     */
    protected void addSentMessage(String targetId, M msg) {
        if (messagesTobeSent.containsKey(targetId)) {
            messagesTobeSent.get(targetId).add(msg);
        } else {
            messagesTobeSent.put(targetId, new LinkedList<>(Arrays.asList(msg)));
        }

        // ������������worker��Ϣ
        if (!vertexIn.containsKey(targetId)) {
            vertexIn.put(targetId, master.getVertexInWorker(targetId));
        }
    }

    private void sendMessages() {
        if (master.combiner != null) {
            // ָ�� Combiner
            try {
                Combiner<M> combiner = (Combiner<M>) master.combiner.getClass().newInstance();
                sendMessages(combiner);
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        } else {
            // Ĭ�Ϸ���ģʽ
            for (Map.Entry<String, List<M>> entry : messagesTobeSent.entrySet()) {
                vertexIn.get(entry.getKey()).addNewMessages(entry.getKey(), entry.getValue());
            }
        }
        // �����Ϣ����
        messagesTobeSent.clear();
    }

    /**
     * ��ָ��Combiner��������combine����������Ϣ����
     * @param combiner
     */
    private void sendMessages(Combiner<M> combiner) {
        for (Map.Entry<String, List<M>> entry : messagesTobeSent.entrySet()) {
            vertexIn.get(entry.getKey()).addNewMessage(entry.getKey(),
                    combiner.combine(entry.getValue()));
        }
    }

    /**
     * ������worker���ã���ָ��target vertex ������Ϣ
     * @param vertexId ��Ϣ����target
     * @param msgs ������Ϣ
     */
    private void addNewMessages(String vertexId, List<M> msgs) {
        vertices.get(vertexId).addNewMessages(msgs);
    }

    private void addNewMessage(String vertexId, M msg) {
        vertices.get(vertexId).addNewMessage(msg);
    }



    protected void addVertex(Vertex<V, E, M> vertex) {
        vertices.put(vertex.vertexId, vertex);
        vertex.setWorker(this);
    }

    protected int getVerticesNum() {
        return master.verticesNum;
    }


    protected Set<String> loadPartition(File file, IUtils<V, E, M> utils) {
        Set<String> result = new HashSet<>();
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            for (String line : lines) {
                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                }
                Triplet<V, E, M> triplet = utils.parseGraphFileLine(line);
                if (triplet == null) {
                    continue;
                }
                if (!result.contains(triplet.source.vertexId)) {
                    // ��δ��� source
                    result.add(triplet.source.vertexId);
                    vertices.put(triplet.source.vertexId, triplet.source);
                    triplet.source.setWorker(this);
                }
                if (triplet.target != null) {
                    // �г���
                    vertices.get(triplet.source.vertexId).targets.put(triplet.target.vertexId,
                            triplet.edgeValue);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("Worker %d load %d vertices... \n", id, result.size());
        return result;
    }

    protected void savePartition(String outputPath) {
        try {
            BufferedWriter writer =
                    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));
            for (Vertex<V, E, M> vertex : vertices.values()) {
                writer.write(vertex.toString());
            }
            writer.close();
            System.out.printf("Worker %d saved %d vertices on %s...\n", id, vertices.size(),
                    outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void resultOutput(BufferedWriter writer) throws IOException {
        for (Vertex<V, E, M> vertex : vertices.values()) {
            writer.write(vertex.resultFormater());
        }
    }

}
