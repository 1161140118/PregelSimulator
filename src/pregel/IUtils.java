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

    /**
     * ���������ļ�һ�����ݵĽ�����ʽ
     * @param line  һ�������¼
     * @return  ��Ԫ��
     */
    public Triplet<V, E, M> parseGraphFileLine(String line);

}


