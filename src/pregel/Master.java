package pregel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class Master<V, E, M> {
	public final int workersNum;
	protected CountDownLatch countDownLatch;
	private List<Worker<V, E, M>> workers;
	private Map<String, Vertex<V, E, M>> vertices = new HashMap<>();

	/**
	 * Master
	 * 
	 * @param worksNum worker ����
	 */
	public Master(int workersNum) {
		this.workersNum = workersNum;
		workers = new LinkedList<>();
		for (int i = 0; i < workersNum; i++) {
			workers.add(new Worker<>(this));
		}
		countDownLatch = new CountDownLatch(workersNum);
	}

	/**
	 * ��������
	 * <p>
	 * Ĭ�����нڵ�inactiveʱ��ֹͣ
	 */
	public void launch() {
		int stepCounter = 1;
		System.out.printf("Mission launch with %d workers on %d vertices.\n", workersNum, vertices.size());

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
		synchronized (countDownLatch) {
			try {
				countDownLatch.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
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
	 */
	public void importGraph(String graphPath, IUtils<V, E, M> utils) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(graphPath)));
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
		Random random = new Random();
		for (Vertex<V, E, M> vertex : vertices.values()) {
			workers.get(random.nextInt(workersNum)).addVertex(vertex);
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

}
