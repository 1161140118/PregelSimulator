package pregel;

/**
 * �����ļ���ÿ�еĽ�����ʽ
 * @author standingby
 *
 * @param <V>
 * @param <E>
 * @param <M>
 */
public interface IUtils<V, E, M> {

	public Triplet<V, E, M> parseGraphFileLine(String line);
	
}


