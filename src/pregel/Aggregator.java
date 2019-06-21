/**
 * 
 */
package pregel;

import java.util.List;

/**
 * @author standingby
 *
 */
public interface Aggregator<V, M> {

    /**
     * ��ö��㷢�͵�����
     * @param vertex
     * @return
     */
    public M report(V vertex);

    /**
     * �ۼ�����
     * @param msgs
     * @return
     */
    public M aggregate(List<M> msgs);

}
