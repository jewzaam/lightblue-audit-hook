/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.redhat.lightblue.hook.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.redhat.lightblue.config.DataSourcesConfiguration;
import com.redhat.lightblue.config.LightblueFactory;
import com.redhat.lightblue.metadata.EntityMetadata;
import com.redhat.lightblue.metadata.mongo.MongoDataStoreParser;
import com.redhat.lightblue.metadata.parser.Extensions;
import com.redhat.lightblue.metadata.parser.JSONMetadataParser;
import com.redhat.lightblue.metadata.types.DefaultTypes;
import com.redhat.lightblue.mongo.config.MongoConfiguration;
import com.redhat.lightblue.mongo.test.AbstractMongoTest;
import com.redhat.lightblue.util.JsonUtils;
import com.redhat.lightblue.util.test.FileUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Abstract hook test assuming use of mongo backend and metadata for test as
 * resource(s) on classpath.
 *
 * @author nmalik
 */
public abstract class AbstractHookTest extends AbstractMongoTest {

    protected static final String DATASTORE_BACKEND = "mongo";

    // reuse the json node factory, no need to create new ones each test
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.withExactBigDecimals(false);

    protected static AuditHookConfigurationParser hookParser;
    protected static JSONMetadataParser parser;

    @BeforeClass
    public static void beforeClass() {
        DataSourcesConfiguration dsc = new DataSourcesConfiguration();
        dsc.add(DATASTORE_BACKEND, new MongoConfiguration());
        LightblueFactory mgr = new LightblueFactory(dsc);
    }

    @BeforeClass
    public static void setupClass() throws Exception {
        // prepare parsers
        Extensions<JsonNode> ex = new Extensions<>();
        ex.addDefaultExtensions();
        hookParser = new AuditHookConfigurationParser();
        ex.registerHookConfigurationParser(AuditHook.HOOK_NAME, hookParser);
        ex.registerDataStoreParser(DATASTORE_BACKEND, new MongoDataStoreParser());
        parser = new JSONMetadataParser(ex, new DefaultTypes(), NODE_FACTORY);
    }

    @Before
    public void setup() throws Exception {
        // create metadata
        for (String resource : getMetadataResources()) {
            String jsonString = FileUtil.readFile(resource);
            EntityMetadata em = parser.parseEntityMetadata(JsonUtils.json(jsonString));
            AuditHook.getFactory().getMetadata().createNewMetadata(em);
        }
    }

    @After
    @Override
    public void teardown() throws Exception {
        super.teardown();
        dropDatabase("mongo");
    }

    @AfterClass
    public static void teardownClass() throws Exception {
        parser = null;
        hookParser = null;
    }

    /**
     * Get list of resources on classpath representing metadata for the test
     * implementation.
     *
     * @return array of resource names
     */
    protected abstract String[] getMetadataResources();
}
