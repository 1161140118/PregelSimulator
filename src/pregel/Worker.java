package pregel;

import java.util.HashSet;
import java.util.Set;

public class Worker<V, E, M> extends Thread {
	private final Master<V, E, M> master;
	private Set<Vertex<V, E, M>> vertices = new HashSet<>();

	/**
	 * 
	 */
	protected Worker(Master<V, E, M> master) {
		this.master = master;
	}

	/**
	 * �������и��ڵ��compute����
	 */
	@Override
	public void run() {
		for (Vertex<V, E, M> vertex : vertices) {
			if (vertex.isActive()) {
				vertex.compute();
			}
		}
		// ������һ����ʾ�������
		master.countDownLatch.countDown();
	}

	public boolean allInactive() {
		for (Vertex<V, E, M> vertex : vertices) {
			if (vertex.isActive()) {
				return false;
			}
		}
		return true;
	}

	public void addVertex(Vertex<V, E, M> vertex) {
		vertices.add(vertex);
	}

}
