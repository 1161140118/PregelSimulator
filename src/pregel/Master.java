package pregel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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
    private DecimalFormat dFormat = new DecimalFormat("0.00");

    // �������
    protected CountDownLatch countDownLatch;
    private ExecutorService executor;

    protected Combiner<M> combiner;

    private Aggregator<Vertex<V, E, M>, M> aggregator;
    private List<M> aggValues;
    private M aggValue;

    // ͳ����Ϣ
    private double startTime = 0;
    private double endTime = 0;
    private List<String> workTime = new LinkedList<>();
    private List<Integer> traffics = new LinkedList<>();

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
    public void run() {

        System.out.println();
        System.out.printf("Step %d start.\n", stepCounter);

        countDownLatch = new CountDownLatch(workersNum);
        startTime = System.currentTimeMillis();
        for (Worker<V, E, M> worker : workers) {
            // ��������
            executor.execute(worker);
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        endTime = System.currentTimeMillis();

        // ͳ��
        for (Worker<V, E, M> worker : workers) {
            workTime.add(dFormat.format(worker.timespan));
            traffics.add(worker.traffic);
            if (aggregator != null) {
                aggValues.add(worker.aggValue);
            }
        }
        // �ۼ����㣨��ѡ��
        if (aggregator != null) {
            aggValue = aggregator.aggregate(aggValues);
            aggValues.clear();
        }

        System.out.println("Time cost /s: " + workTime);
        System.out.println("Traffic cost: " + traffics);
        System.out.printf("Step %d end with %.2f seconds.\n", stepCounter,
                (endTime - startTime) / 1000);

        stepCounter++;
        workTime.clear();
        traffics.clear();
    }

    public void shutdown() {
        executor.shutdown();
    }


    public boolean allInactive() {
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

        for (Worker<V, E, M> worker : workers) {
            worker.reportVertexAndEdge();
        }
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


    /**
     * @return the aggValue
     */
    public M getAggValue() {
        return aggValue;
    }

    /**
     * @return the verticesNum
     */
    public int getVerticesNum() {
        return verticesNum;
    }

    /**
     * @return the stepCounter
     */
    public int getStepCounter() {
        return stepCounter;
    }

    /**
     * ʹ��Combiner��ָ��combineʵ��
     * @param combiner
     */
    public void setCombiner(Combiner<M> combiner) {
        this.combiner = combiner;
        for (Worker<V, E, M> worker : workers) {
            worker.setCombiner(combiner);
        }
    }


    /**
     * ʹ�� Aggregator ��ָ�� report �� aggregate ��ʵ��
     * @param aggregator the aggregator to set
     */
    public void setAggregator(Aggregator<Vertex<V, E, M>, M> aggregator) {
        this.aggregator = aggregator;
        for (Worker<V, E, M> worker : workers) {
            worker.setAggregator(aggregator);
        }
        aggValues = new LinkedList<>();
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
            // report
            workers.get(i).reportVertexAndEdge();
        }
        verticesNum = vertexIn.size();
        System.out.println("Load partition complete.");
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
