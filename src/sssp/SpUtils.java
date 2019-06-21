/**
 * 
 */
package sssp;

import pregel.IUtils;
import pregel.Triplet;

/**
 * @author standingby
 *
 */
public class SpUtils implements IUtils<Integer, Integer, SpMessage> {

    /*
     * (non-Javadoc)
     * 
     * @see pregel.IUtils#parseGraphFileLine(java.lang.String)
     */
    @Override
    public Triplet<Integer, Integer, SpMessage> parseGraphFileLine(String line) {
        String[] strings = line.split("\\t");
        SpVertex source = new SpVertex(strings[0]);
        SpVertex target;
        if (strings.length >= 2) {
            target = new SpVertex(strings[1]);
        } else {
            // û�г���
            target = null;
        }
        return new Triplet<Integer, Integer, SpMessage>(source, target, 1);
    }


}
