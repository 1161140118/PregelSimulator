package pregel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Master<V, E, M> {
    public final int workersNum;
    protected final List<Worker<V, E, M>> workers;
    private final Map<String, Vertex<V, E, M>> vertices = new HashMap<>();

    protected Combiner<M> combiner;

    protected CountDownLatch countDownLatch;

    /**
     * Master
     * 
     * @param worksNum worker ����
     */
    public Master(int workersNum) {
        this.workersNum = workersNum;
        workers = new LinkedList<>();
        for (int i = 0; i < workersNum; i++) {
            workers.add(new Worker<>(i, this));
        }
    }

    /**
     * ��������
     * <p>
     * Ĭ�����нڵ�inactiveʱ��ֹͣ
     */
    public void launch() {
        int stepCounter = 1;
        System.out.printf("Mission launch with %d workers on %d vertices.\n", workersNum,
                vertices.size());

        while (!allInactive()) {
            System.out.printf("Step %d start.\n", stepCounter);
            run();
            System.out.printf("Step %d end.\n", stepCounter);
            stepCounter++;
        }
    }

    /**
     * ִ������һ�� SuperStep������workers���м���
     */
    private void run() {
        countDownLatch = new CountDownLatch(workersNum);
        for (Worker<V, E, M> worker : workers) {
            // ��������
            worker.start();
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean allInactive() {
        for (Worker<V, E, M> worker : workers) {
            if (!worker.allInactive()) {
                return false;
            }
        }
        return true;
    }

    /**
     * ����ͼ
     * @param graphPath
     * @param utils IUtilsʵ���࣬����������
     */
    public void importGraph(String graphPath, IUtils<V, E, M> utils) {
        try {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(new FileInputStream(graphPath)));
            String line = "";
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }

                Triplet<V, E, M> triplet = utils.parseGraphFileLine(line);
                Vertex<V, E, M> source = triplet.source;
                if (vertices.containsKey(source.vertexId)) {
                    source = vertices.get(source.vertexId);
                } else {
                    vertices.put(source.vertexId, source);
                }

                Vertex<V, E, M> target = triplet.target;
                if (vertices.containsKey(target.vertexId)) {
                    target = vertices.get(target.vertexId);
                } else {
                    vertices.put(target.vertexId, target);
                }

                source.addTarget(target, triplet.edgeValue);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        partition();
    }

    /**
     * random ����
     */
    private void partition() {
        int cnt = 0;
        for (Vertex<V, E, M> vertex : vertices.values()) {
            workers.get(cnt++ % workersNum).addVertex(vertex);
        }
    }

    /**
     * ���滮�ֽ��
     * <p>
     * �����ʽ���� Vertex.outputFormater() ȷ����ʽ
     */
    public void save(String outputPath) {

    }

    /**
     * ���ػ��ֽ��
     * 
     * @param partitionPath
     */
    public void load(String partitionFolder, IUtils<V, E, M> utils) {

    }

    public void setCombiner(Combiner<M> combiner) {
        this.combiner = combiner;
    }

}
