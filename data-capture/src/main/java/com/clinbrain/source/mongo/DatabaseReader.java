package com.clinbrain.source.mongo;

import com.mongodb.CursorType;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ReadPreference;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.BSONTimestamp;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Reads mutation from a mongodb database
 *
 * @author Andrea Patelli
 */
public class DatabaseReader implements Runnable {
    private final Logger log = LoggerFactory.getLogger(DatabaseReader.class);
    private final String host;
    private final Integer port;
    private final String uri;
    private final String db;
    private final Integer batchSize;
    private final String start;
    private MongoClient mongoClient;
    private int page = 0;

    private ConcurrentLinkedQueue<Document> messages;

    private MongoCollection<Document> oplog;
    private Bson query;
    
    public DatabaseReader(String uri, String db, String start, Integer batchSize, ConcurrentLinkedQueue<Document> messages) {
        this.uri = uri;
        this.host = null;
        this.port = null;
        this.batchSize = batchSize;
        this.db = db;
        this.start = start;
        this.messages = messages;
        try {
            init();
        } catch (Exception e) {
            throw e;
        }
    }

    public DatabaseReader(String host, Integer port, String db, String start, Integer batchSize, ConcurrentLinkedQueue<Document> messages) {
        this.uri = null;
        this.host = host;
        this.port = port;
        this.batchSize = batchSize;
        this.db = db;
        this.start = start;
        this.messages = messages;
        try {
            init();
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    public void run() {
    	while(true){
    		if(messages.isEmpty()){
		        log.debug("Query starting in page {}.", page);
		        final FindIterable<Document> documents = find(page);
		        try {
		            for (Document document : documents) {
                        log.trace(document.toString());
		                if (null != handleOp(document)) {
                            messages.add(document);
                        }
		            }
		        } catch(Exception e) {
		            log.error("Closed connection", e);
		        }
		        page++;
    		}
    		else{
    			try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					log.error(e.getMessage(), e);
				}
    		}
    	}
    }

    private Document handleOp(Document doc) {
        if (doc.getString("op").equalsIgnoreCase("u")) {
            Document updated = findOneById(doc);
            if (null == updated) {
                return null;
            } else {
                doc.append("o", updated);
            }
        }

        return doc;
    }

    private Document findOneById(Document doc) {
        try {
            MongoCollection<Document> nsCollection = getNSCollection(doc.getString("ns"));
            ObjectId id = ((Document) doc.get("o2")).getObjectId("_id");

            List<Document> docs = new ArrayList<>();
            nsCollection.find(Filters.eq("_id", id)).into(docs);
            if (docs.isEmpty()) {
                return null;
            } else {
                return docs.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Can not find document: {}", doc);
        }

        return null;
    }

    private MongoCollection<Document> getNSCollection(String ns) {
        String[] dbAndCollection = ns.split("\\.");
        MongoDatabase nsDB = mongoClient.getDatabase(dbAndCollection[0]);
        return nsDB.getCollection(dbAndCollection[1]);
    }
    
    private FindIterable<Document> find(int page){
        final FindIterable<Document> documents = oplog
                .find(query)
                .sort(new Document("$natural", 1))
                .skip(page * batchSize)
                .limit(batchSize)
                .projection(Projections.include("ts", "op", "ns", "o", "o2"))
                .cursorType(CursorType.TailableAwait);
        return documents;
    }
    
    @Override
    public void finalize(){
    	if(mongoClient != null){
    		mongoClient.close();
    	}
    }
    
    private MongoClient createMongoClient(){
    	MongoClient mongoClient;
    	if(uri != null){
    		final MongoClientURI mongoClientURI = new MongoClientURI(uri);
    		mongoClient = new MongoClient(mongoClientURI);
    	}
    	else{
    		mongoClient = new MongoClient(host, port);
    	}
        ReadPreference preference = ReadPreference.secondary();
//        ReadPreference preference = ReadPreference.primary();
    	mongoClient.setReadPreference(preference);
		return mongoClient;
    }
    
    private void init() {
        oplog = readCollection();
        query = createQuery();
    }

    /**
     * Loads the oplog collection.
     *
     * @return the oplog collection
     */
    private MongoCollection<Document> readCollection() {
		mongoClient = createMongoClient();
		
        log.trace("Starting database reader with configuration: ");
		log.trace("addresses: {}", StringUtils.join(mongoClient.getAllAddress(), ","));
        log.trace("db: {}", db);
        log.trace("start: {}", start);
        
        final MongoDatabase db = mongoClient.getDatabase("local");
        return db.getCollection("oplog.rs");
    }

    /**
     * Creates the query to execute on the collection.
     *
     * @return the query
     */
    private Bson createQuery() {
        // timestamps are used as offsets, saved as a concatenation of seconds and order
    	Integer timestamp = 0;
        Integer order = 0;
    	if(!start.equals("0")){    	
	    	final String[] splitted = start.split("_");
	    	timestamp = Integer.valueOf(splitted[0]);
	        order = Integer.valueOf(splitted[1]);
    	}
    	
        Bson query = Filters.and(
                Filters.exists("fromMigrate", false),
                Filters.gt("ts", new BSONTimestamp(timestamp, order)),
                Filters.or(
                        Filters.eq("op", "i"),
                        Filters.eq("op", "u"),
                        Filters.eq("op", "d")
                ),
                Filters.eq("ns", db)
        );

        return query;
    }
}
