package edu.hm.ccwi.matilda.korpus.util;

import com.mongodb.*;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTag;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoUtils<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoUtils.class);

    private final MongoDatabase database;
    private final MongoCollection documentMongoCollection;

    /**
     * Create connection with default object DBObject.class
     *
     * @param collectionName
     */
    public MongoUtils(String collectionName) {
        CodecRegistry pojoCodecRegistry = getCodecRegistry();
        MongoClientSettings settings = MongoClientSettings.builder().codecRegistry(pojoCodecRegistry).build();
        database = MongoClients.create(settings).getDatabase("matilda");
        documentMongoCollection = database.getCollection(collectionName, DBObject.class);
    }

    public MongoUtils(Class<T> classType, String collectionName) {
        CodecRegistry pojoCodecRegistry = getCodecRegistry();
        MongoClientSettings settings = MongoClientSettings.builder().codecRegistry(pojoCodecRegistry).build();
        database = MongoClients.create(settings).getDatabase("matilda");
        documentMongoCollection = database.getCollection(collectionName, classType);
    }

    /**
     * Get all GACategoryTag-Entries from MongoDB Collection.
     */
    public List<T> retrieveCollectionFromDB() {
        List<T> retrievedCollectionList = new ArrayList<>();
        for (T t : (Iterable<T>) documentMongoCollection.find()) {
            retrievedCollectionList.add(t);
        }
        return retrievedCollectionList;
    }

    public List<GACategoryTag> mapBasicDBObjectToGACategoryTagObject(List<BasicDBObject> basicDBObjects) {
        List<GACategoryTag> gaCategoryTags = new ArrayList<>();
        for (BasicDBObject basicDBObject : basicDBObjects) {
            GACategoryTag gaCategoryTag = new GACategoryTag();
            gaCategoryTag.setId((String) basicDBObject.get("_id"));
            gaCategoryTag.setGroup((String) basicDBObject.get("group"));
            gaCategoryTag.setArtifact((String) basicDBObject.get("artifact"));
            gaCategoryTag.setCategory((String) basicDBObject.get("category"));
            List<String> tags = new ArrayList<>();
            BasicDBList tagBasicDBList = (BasicDBList) basicDBObject.get("tags");
            if(CollectionUtils.isNotEmpty(tagBasicDBList)) {
                for (Object tagObject : tagBasicDBList) {
                    tags.add(((String) tagObject));
                }
                gaCategoryTag.setTags(tags);
            }
            gaCategoryTag.setCrawled((boolean) basicDBObject.get("crawled"));

            gaCategoryTags.add(gaCategoryTag);
        }

        return gaCategoryTags;
    }

    /**
     * Save GACategoryTag to MongoDB Collection.
     */
    public void saveGACategoryTagToMongoDB(GACategoryTag gact) {

        try {
            BasicDBObjectBuilder basicDBObjectBuilder = new BasicDBObjectBuilder();
            basicDBObjectBuilder.add("_id", gact.getId());
            basicDBObjectBuilder.add("group", gact.getGroup());
            basicDBObjectBuilder.add("artifact", gact.getArtifact());
            basicDBObjectBuilder.add("category", gact.getCategory());
            basicDBObjectBuilder.add("tags", gact.getTags());
            basicDBObjectBuilder.add("crawled", false);

            documentMongoCollection.insertOne(basicDBObjectBuilder.get());
        } catch (Exception e) {
            LOGGER.error("Error while inserting a datapoint: ", e);
        }
    }

    /**
     * Save all GACategoryTag-Entries to MongoDB Collection.
     */
    public void saveGACategoryTagListToMongoDB(List<GACategoryTag> dataset) {
        for (GACategoryTag data : dataset) {
            saveGACategoryTagToMongoDB(data);
        }
    }

    /**
     * Update (remove and insert) GACategoryTag to MongoDB Collection.
     */
    public void updateGACategoryTagToMongoDB(GACategoryTag gact) {
        try {
            DeleteResult result = documentMongoCollection.deleteOne(Filters.eq("_id", gact.getId()));
            if(result.getDeletedCount() > 0) {
                saveGACategoryTagToMongoDB(gact);
            }
        } catch (Exception e) {
            LOGGER.error("Error while inserting a datapoint: ", e);
        }
    }

    public void saveDatasetToMongoDB(List<T> dataset) {
        documentMongoCollection.insertMany(dataset);
    }

    private CodecRegistry getCodecRegistry() {
        return fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
    }
}
