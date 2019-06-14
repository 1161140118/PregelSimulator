package element;

public abstract  class Vertex <VertexValue,EdgeValue,MessageValue> {
	/**
	 * ��Ƕ����Ծ״̬
	 * true���������
	 * false����������㣬������Ϣ��תΪtrue
	 */
	private boolean active = true;
	
	public final String vertexId;	

	
	public Vertex(String vertexId) {
		super();
		this.vertexId = vertexId;
	}

	abstract public void compute();
	
	
	public boolean isActive() {
		return active;
	}
}
