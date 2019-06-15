package pregel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;


public class Master<V, E, M> {
    private List<Worker<V, E, M>> works;

    /**
     * Master 
     * @param worksNum worker ����
     */
    public Master(int worksNum) {
        works = new LinkedList<>();
        for (int i = 0; i < worksNum; i++) {
            works.add(new Worker<>());
        }
    }

    public Master(int worksNum, String graphPath) {
        works = new LinkedList<>();
        for (int i = 0; i < worksNum; i++) {
            works.add(new Worker<>());
        }
        importGraph(graphPath);
    }

    /**
     * ��������
     */
    public void start() {
        for (Worker<V, E, M> worker : works) {
            // ��������
            worker.start();
        }
    }

    /**
     * ִ������һ�� SuperStep������workers���м���
     */
    public void run() {
        
    }

    /**
     * ����ͼ
     */
    public void importGraph(String graphPath) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(graphPath)));
            String line = "";
            while( (line=reader.readLine())!=null) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] strings = line.split(" ");
                
                
            }
            
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
    }

    /**
     * ����ͼ
     */
    public void partition() {

    }

    /**
     * ���滮�ֽ��
     */
    public void save() {
        
    }

    /**
     * ���ػ��ֽ��
     * @param partitionPath
     */
    public void load(String partitionPath) {
        
    }

    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
