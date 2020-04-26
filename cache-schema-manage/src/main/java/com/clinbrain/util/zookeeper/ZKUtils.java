package com.clinbrain.util.zookeeper;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class ZKUtils implements Watcher {

    private static final Logger logger = LoggerFactory.getLogger(ZKUtils.class);
    private static CountDownLatch downLatch = new CountDownLatch(1);
    private static ZooKeeper zk;

    private ZKUtils(){}

    public static void initZK(String zkServerAddr) throws Exception{
        zk = new ZooKeeper(zkServerAddr,5000, new ZKUtils());
    }


    //创建节点
    public static void createNode(Map<String, String> nodeInfo) throws Exception{
        downLatch.await();
        String path = nodeInfo.get("path");
        String data = nodeInfo.get("data");
        String pathInfo = zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT); //持久节点
        logger.info("success create nodeDir on zookeeper: {}.", pathInfo);
        downLatch.countDown();
    }


    //更新节点
    public static void updateNode(Map<String, String> nodeInfo) throws Exception{
        downLatch.await();
        String path = nodeInfo.get("path");
        String data = nodeInfo.get("data");
        Stat stat = zk.setData(path, data.getBytes(), -1);
        logger.info("success update nodeData on zookeeper: {}. {}.", path, data);
    }


    //获取节点数据
    public static Object getNodeData(String path) throws Exception{
        downLatch.await();
        byte[] dataBytes = zk.getData(path, null, null);
        String data = new String(dataBytes);
        logger.info("success get nodeData on zookeeper: {}. {}.", path, data);
        return data;
    }


    @Override
    public void process(WatchedEvent watchedEvent) {
        if(Event.KeeperState.SyncConnected == watchedEvent.getState()){
            if(Event.EventType.None == watchedEvent.getType() && null == watchedEvent.getType()){
                downLatch.countDown();
            }
        }
    }


}
