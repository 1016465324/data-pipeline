package com.clinbrain.source.mongo;

import com.mongodb.ReadPreference;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author p
 */
public class MongodbConfig implements Serializable {

    private String hostPorts;

    private String url;

    private String username;

    private String password;

    private String authenticationMechanism;

    private String database;

    private String collectionName;

    private String filter;

    private int fetchSize;

    private String writeMode;

    private String replaceKey;

    private List<String> monitorDatabases;

    private List<String> monitorCollections;

    private List<String> operateType;

    private boolean pavingData;

    private String clusterMode;

    private int startLocation;

    private boolean excludeDocId;

    private ConnectionConfig connectionConfig = new ConnectionConfig();

    public MongodbConfig() {

    }

    public MongodbConfig(Properties props) {
        this.hostPorts = props.getProperty("hostPorts");
        this.clusterMode = props.getProperty("clusterMode");
        String readPreference = props.getProperty("readPreference");
        if (StringUtils.isEmpty(readPreference)) {
            this.connectionConfig.setReadPreference(null);
        } else {
            if (StringUtils.equalsIgnoreCase(readPreference, "primary")) {
                this.connectionConfig.setReadPreference(ReadPreference.primary());
            } else if (StringUtils.equalsIgnoreCase(readPreference, "primaryPreferred")) {
                this.connectionConfig.setReadPreference(ReadPreference.primaryPreferred());
            } else if (StringUtils.equalsIgnoreCase(readPreference, "secondary")) {
                this.connectionConfig.setReadPreference(ReadPreference.secondary());
            } else if (StringUtils.equalsIgnoreCase(readPreference, "secondaryPreferred")) {
                this.connectionConfig.setReadPreference(ReadPreference.secondaryPreferred());
            } else if (StringUtils.equalsIgnoreCase(readPreference, "nearest")) {
                this.connectionConfig.setReadPreference(ReadPreference.nearest());
            } else {
                throw new RuntimeException("mongodb readPreference set error.");
            }
        }

        ArrayList<String> operateTypes = new ArrayList<>(3);
        operateTypes.add("INSERT");
        operateTypes.add("UPDATE");
        operateTypes.add("DELETE");
        this.operateType = operateTypes;
    }

    public static class ConnectionConfig implements Serializable{
        private int connectionsPerHost = 100;

        private int threadsForConnectionMultiplier = 100;

        private int connectionTimeout = 10000;

        private int maxWaitTime = 5000;

        private int socketTimeout = 0;

        private ReadPreference readPreference = ReadPreference.secondaryPreferred();

        public int getConnectionsPerHost() {
            return connectionsPerHost;
        }

        public void setConnectionsPerHost(int connectionsPerHost) {
            this.connectionsPerHost = connectionsPerHost;
        }

        public int getThreadsForConnectionMultiplier() {
            return threadsForConnectionMultiplier;
        }

        public void setThreadsForConnectionMultiplier(int threadsForConnectionMultiplier) {
            this.threadsForConnectionMultiplier = threadsForConnectionMultiplier;
        }

        public int getConnectionTimeout() {
            return connectionTimeout;
        }

        public void setConnectionTimeout(int connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
        }

        public int getMaxWaitTime() {
            return maxWaitTime;
        }

        public void setMaxWaitTime(int maxWaitTime) {
            this.maxWaitTime = maxWaitTime;
        }

        public int getSocketTimeout() {
            return socketTimeout;
        }

        public void setSocketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
        }

        public ReadPreference getReadPreference() {
            return readPreference;
        }

        public void setReadPreference(ReadPreference readPreference) {
            this.readPreference = readPreference;
        }

        @Override
        public String toString() {
            return "ConnectionConfig{" +
                    "connectionsPerHost=" + connectionsPerHost +
                    ", threadsForConnectionMultiplier=" + threadsForConnectionMultiplier +
                    ", connectionTimeout=" + connectionTimeout +
                    ", maxWaitTime=" + maxWaitTime +
                    ", socketTimeout=" + socketTimeout +
                    ", readPreference=" + readPreference +
                    '}';
        }
    }

    public boolean getExcludeDocId() {
        return excludeDocId;
    }

    public void setExcludeDocId(boolean excludeDocId) {
        this.excludeDocId = excludeDocId;
    }

    public int getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(int startLocation) {
        this.startLocation = startLocation;
    }

    public String getClusterMode() {
        return clusterMode;
    }

    public void setClusterMode(String clusterMode) {
        this.clusterMode = clusterMode;
    }

    public List<String> getMonitorDatabases() {
        return monitorDatabases;
    }

    public void setMonitorDatabases(List<String> monitorDatabases) {
        this.monitorDatabases = monitorDatabases;
    }

    public List<String> getMonitorCollections() {
        return monitorCollections;
    }

    public void setMonitorCollections(List<String> monitorCollections) {
        this.monitorCollections = monitorCollections;
    }

    public String getAuthenticationMechanism() {
        return authenticationMechanism;
    }

    public void setAuthenticationMechanism(String authenticationMechanism) {
        this.authenticationMechanism = authenticationMechanism;
    }

    public String getHostPorts() {
        return hostPorts;
    }

    public void setHostPorts(String hostPorts) {
        this.hostPorts = hostPorts;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public int getFetchSize() {
        return fetchSize;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public String getWriteMode() {
        return writeMode;
    }

    public void setWriteMode(String writeMode) {
        this.writeMode = writeMode;
    }

    public String getReplaceKey() {
        return replaceKey;
    }

    public void setReplaceKey(String replaceKey) {
        this.replaceKey = replaceKey;
    }

    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public void setConnectionConfig(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public List<String> getOperateType() {
        return operateType;
    }

    public void setOperateType(List<String> operateType) {
        this.operateType = operateType;
    }

    public boolean getPavingData() {
        return pavingData;
    }

    public void setPavingData(boolean pavingData) {
        this.pavingData = pavingData;
    }

    @Override
    public String toString() {
        return "MongodbConfig{" +
                "hostPorts='" + hostPorts + '\'' +
                ", url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", password='******" + '\'' +
                ", authenticationMechanism='" + authenticationMechanism + '\'' +
                ", database='" + database + '\'' +
                ", collectionName='" + collectionName + '\'' +
                ", filter='" + filter + '\'' +
                ", fetchSize=" + fetchSize +
                ", writeMode='" + writeMode + '\'' +
                ", replaceKey='" + replaceKey + '\'' +
                ", monitorDatabases=" + monitorDatabases +
                ", monitorCollections=" + monitorCollections +
                ", operateType=" + operateType +
                ", pavingData=" + pavingData +
                ", clusterMode='" + clusterMode + '\'' +
                ", startLocation=" + startLocation +
                ", excludeDocId=" + excludeDocId +
                ", mongodbConfig=" + connectionConfig +
                '}';
    }
}
