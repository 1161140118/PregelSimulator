package pregel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Master<V, E, M> {
    protected final List<Worker<V, E, M>> workers = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Vertex<V, E, M>> vertices = new HashMap<>();
    private final Map<String, Integer> vertexIn = new HashMap<>();
    public final int workersNum;
    protected int verticesNum;
    int stepCounter = 0;

    // �������
    protected CountDownLatch countDownLatch;
    private ExecutorService executor;

    protected Combiner<M> combiner;


    /**
     * Master
     * 
     * @param worksNum worker ����
     */
    public Master(int workersNum) {
        this.workersNum = workersNum;
        for (int i = 0; i < workersNum; i++) {
            workers.add(new Worker<>(i, this));
        }
        executor = Executors.newFixedThreadPool(workersNum);
    }

    /**
     * ��������
     * <p>
     * Ĭ�����нڵ�inactiveʱ��ֹͣ
     */
    public void launch() {
        double startTime = 0;
        double endTime = 0;
        System.out.printf("Mission launch with %d workers on %d vertices.\n", workersNum,
                vertexIn.size());

        while (!allInactive()) {
            System.out.printf("Step %d start.\n", stepCounter);
            startTime = System.currentTimeMillis();
            run();
            endTime = System.currentTimeMillis();
            System.out.printf("Step %d end with %.2f seconds.\n", stepCounter,
                    (endTime - startTime) / 1000);
            stepCounter++;
        }
        executor.shutdown();
    }


    /**
     * �������㣬ִ��һ����������
     * @param maxItor ������������
     */
    public void launch(int maxItor) {
        System.out.printf("Mission launch with %d workers on %d vertices.\n", workersNum,
                verticesNum);

        while (stepCounter < maxItor) {
            System.out.printf("Step %d start.\n", stepCounter);
            run();
            System.out.printf("Step %d end.\n", stepCounter);
            stepCounter++;
        }
        executor.shutdown();
    }

    /**
     * ִ������һ�� SuperStep������workers���м���
     */
    private void run() {
        countDownLatch = new CountDownLatch(workersNum);
        for (Worker<V, E, M> worker : workers) {
            // ��������
            executor.execute(worker);
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
                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                }

                Triplet<V, E, M> triplet = utils.parseGraphFileLine(line);
                if (triplet == null) {
                    continue;
                }

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

                source.addTarget(target.vertexId, triplet.edgeValue);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.vertexIn.putAll(partition());
        verticesNum = vertexIn.size();
    }

    /**
     * random ����
     */
    private Map<String, Integer> partition() {
        Map<String, Integer> vertexIn = new HashMap<>();
        int cnt = 0;
        for (Vertex<V, E, M> vertex : vertices.values()) {
            int workerId = cnt++ % workersNum;
            workers.get(workerId).addVertex(vertex);
            vertexIn.put(vertex.vertexId, workerId);
        }
        return vertexIn;
    }

    /**
     * ����VertexId�����Vextex���ڻ��������ã��Ӷ�ʵ����Ϣ����
     * @param vertexId ����ѯvertex
     * @return Vextex���ڻ���������
     */
    protected synchronized Worker<V, E, M> getVertexInWorker(String vertexId) {
        return workers.get(vertexIn.get(vertexId));
    }

    public void setCombiner(Combiner<M> combiner) {
        this.combiner = combiner;
    }


    /**
     * ���滮�ֽ��
     * <p>
     * �����ʽ���� toString() ����ȷ��
     */
    public void save(String outputPath) {
        for (Worker<V, E, M> worker : workers) {
            worker.savePartition(outputPath + "/partition_" + worker.id);
        }
    }

    /**
     * ���ػ��ֽ��
     * @param partitionFolder �������ڵ��ļ���Ŀ¼
     * @param utils ָ��������ʽ
     */
    public void load(String partitionFolder, IUtils<V, E, M> utils) {
        File[] files = new File(partitionFolder).listFiles();
        if (files.length < workersNum) {
            System.out.println(
                    "Warning: the size of folder is " + files.length + " ,less than workers.");
        }
        if (files.length > workersNum) {
            System.err.println(
                    "Error: the size of folder is " + files.length + " , more than workers.");
            System.exit(1);
        }
        for (int i = 0; i < workersNum; i++) {
            Set<String> newVertices = workers.get(i).loadPartition(files[i], utils);
            for (String string : newVertices) {
                vertexIn.put(string, i);
            }
        }
        verticesNum = vertexIn.size();
        System.out.println("Load partition complete." + verticesNum);
    }


    /**
     * ����������������ʽ�� Vertex.resultFormater() ȷ��<p>
     * Ĭ����ʽΪ: vertexFrom    vertexTo
     * @param resultPath
     */
    public void resultOutput(String resultPath) {
        try {
            BufferedWriter writer =
                    new BufferedWriter(new OutputStreamWriter(new FileOutputStream(resultPath)));
            for (Worker<V, E, M> worker : workers) {
                worker.resultOutput(writer);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
