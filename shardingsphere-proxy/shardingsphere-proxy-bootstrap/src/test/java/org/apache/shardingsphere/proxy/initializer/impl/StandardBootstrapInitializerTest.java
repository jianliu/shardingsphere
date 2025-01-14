/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.proxy.initializer.impl;

import org.apache.shardingsphere.authority.api.config.AuthorityRuleConfiguration;
import org.apache.shardingsphere.authority.yaml.config.YamlAuthorityRuleConfiguration;
import org.apache.shardingsphere.infra.config.RuleConfiguration;
import org.apache.shardingsphere.infra.config.datasource.DataSourceParameter;
import org.apache.shardingsphere.infra.config.persist.repository.DistMetaDataPersistRepository;
import org.apache.shardingsphere.infra.context.metadata.MetaDataContexts;
import org.apache.shardingsphere.infra.metadata.user.Grantee;
import org.apache.shardingsphere.infra.metadata.user.ShardingSphereUser;
import org.apache.shardingsphere.infra.metadata.user.ShardingSphereUsers;
import org.apache.shardingsphere.infra.spi.ShardingSphereServiceLoader;
import org.apache.shardingsphere.infra.yaml.config.pojo.YamlRuleConfiguration;
import org.apache.shardingsphere.infra.yaml.config.pojo.algorithm.YamlShardingSphereAlgorithmConfiguration;
import org.apache.shardingsphere.infra.yaml.config.swapper.YamlRuleConfigurationSwapper;
import org.apache.shardingsphere.proxy.config.ProxyConfiguration;
import org.apache.shardingsphere.proxy.config.YamlProxyConfiguration;
import org.apache.shardingsphere.proxy.config.yaml.YamlDataSourceParameter;
import org.apache.shardingsphere.proxy.config.yaml.YamlProxyRuleConfiguration;
import org.apache.shardingsphere.proxy.config.yaml.YamlProxyServerConfiguration;
import org.apache.shardingsphere.proxy.fixture.RuleConfigurationFixture;
import org.apache.shardingsphere.proxy.fixture.YamlRuleConfigurationFixture;
import org.apache.shardingsphere.transaction.context.TransactionContexts;
import org.apache.shardingsphere.transaction.core.XATransactionManagerType;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public final class StandardBootstrapInitializerTest extends AbstractBootstrapInitializerTest {
    
    @Test
    @Ignore
    // TODO fix test case
    public void assertGetProxyConfiguration() {
        YamlProxyConfiguration yamlConfig = makeProxyConfiguration();
        ProxyConfiguration actual = getInitializer().getProxyConfiguration(yamlConfig);
        assertNotNull(actual);
        assertProxyConfiguration(actual);
    }
    
    private YamlProxyConfiguration makeProxyConfiguration() {
        return new YamlProxyConfiguration(createYamlProxyServerConfiguration(), createYamlProxyRuleConfigurationMap());
    }
    
    private Map<String, YamlProxyRuleConfiguration> createYamlProxyRuleConfigurationMap() {
        Map<String, YamlProxyRuleConfiguration> result = new HashMap<>(1, 1);
        result.put("logic-db", createYamlProxyRuleConfiguration());
        return result;
    }
    
    private YamlProxyRuleConfiguration createYamlProxyRuleConfiguration() {
        YamlProxyRuleConfiguration result = new YamlProxyRuleConfiguration();
        result.setDataSources(createYamlDataSourceParameterMap());
        result.setRules(createYamlRuleConfigurations());
        return result;
    }
    
    private Collection<YamlRuleConfiguration> createYamlRuleConfigurations() {
        YamlRuleConfigurationFixture result = new YamlRuleConfigurationFixture();
        result.setName("testRule");
        return Collections.singletonList(result);
    }
    
    private Map<String, YamlDataSourceParameter> createYamlDataSourceParameterMap() {
        Map<String, YamlDataSourceParameter> result = new HashMap<>(1, 1);
        result.put("ds", createYamlDataSourceParameter());
        return result;
    }
    
    private YamlDataSourceParameter createYamlDataSourceParameter() {
        YamlDataSourceParameter result = new YamlDataSourceParameter();
        result.setUrl("jdbc:mysql://localhost:3306/ds");
        result.setUsername("root");
        result.setPassword("root");
        result.setReadOnly(false);
        result.setConnectionTimeoutMilliseconds(1000L);
        result.setIdleTimeoutMilliseconds(2000L);
        result.setMaxLifetimeMilliseconds(4000L);
        result.setMaxPoolSize(20);
        result.setMinPoolSize(10);
        return result;
    }
    
    private void assertProxyConfiguration(final ProxyConfiguration actual) {
        assertSchemaDataSources(actual.getSchemaDataSources());
        assertSchemaRules(actual.getSchemaRules());
        assertUsers(new ShardingSphereUsers(getUsersFromAuthorityRule(actual.getGlobalRules())));
        assertProps(actual.getProps());
    }

    private Collection<ShardingSphereUser> getUsersFromAuthorityRule(final Collection<RuleConfiguration> globalRuleConfigs) {
        for (RuleConfiguration ruleConfig : globalRuleConfigs) {
            if (ruleConfig instanceof AuthorityRuleConfiguration) {
                AuthorityRuleConfiguration authorityRuleConfiguration = (AuthorityRuleConfiguration) ruleConfig;
                return authorityRuleConfiguration.getUsers();
            }
        }
        return Collections.emptyList();
    }

    private void assertSchemaDataSources(final Map<String, Map<String, DataSourceParameter>> actual) {
        assertThat(actual.size(), is(1));
        assertTrue(actual.containsKey("logic-db"));
        Map<String, DataSourceParameter> dataSourceParameterMap = actual.get("logic-db");
        assertThat(dataSourceParameterMap.size(), is(1));
        assertTrue(dataSourceParameterMap.containsKey("ds"));
        assertDataSourceParameter(dataSourceParameterMap.get("ds"));
    }
    
    private void assertDataSourceParameter(final DataSourceParameter actual) {
        assertThat(actual.getUrl(), is("jdbc:mysql://localhost:3306/ds"));
        assertThat(actual.getUsername(), is("root"));
        assertThat(actual.getPassword(), is("root"));
        assertFalse(actual.isReadOnly());
        assertThat(actual.getConnectionTimeoutMilliseconds(), is(1000L));
        assertThat(actual.getIdleTimeoutMilliseconds(), is(2000L));
        assertThat(actual.getMaxLifetimeMilliseconds(), is(4000L));
        assertThat(actual.getMaxPoolSize(), is(20));
        assertThat(actual.getMinPoolSize(), is(10));
    }
    
    private void assertSchemaRules(final Map<String, Collection<RuleConfiguration>> actual) {
        assertThat(actual.size(), is(1));
        assertTrue(actual.containsKey("logic-db"));
        Collection<RuleConfiguration> ruleConfigurations = actual.get("logic-db");
        assertThat(ruleConfigurations.size(), is(1));
        assertRuleConfiguration(ruleConfigurations.iterator().next());
    }
    
    private void assertRuleConfiguration(final RuleConfiguration actual) {
        assertThat(actual, instanceOf(RuleConfigurationFixture.class));
        assertThat(((RuleConfigurationFixture) actual).getName(), is("testRule"));
    }
    
    private void assertUsers(final ShardingSphereUsers actual) {
        Optional<ShardingSphereUser> rootUser = actual.findUser(new Grantee("root", ""));
        assertTrue(rootUser.isPresent());
        assertThat(rootUser.get().getPassword(), is("root"));
    }
    
    private YamlProxyServerConfiguration createYamlProxyServerConfiguration() {
        final YamlProxyServerConfiguration result = new YamlProxyServerConfiguration();
        YamlAuthorityRuleConfiguration yamlRule = new YamlAuthorityRuleConfiguration();
        yamlRule.setUsers(Collections.singletonList("root@%:root"));
        YamlShardingSphereAlgorithmConfiguration provider = new YamlShardingSphereAlgorithmConfiguration();
        provider.setType("test");
        yamlRule.setProvider(provider);
        result.getRules().add(yamlRule);
        result.setProps(createProperties());
        return result;
    }
    
    private Properties createProperties() {
        Properties result = new Properties();
        result.setProperty("alpha-1", "alpha-A");
        result.setProperty("beta-2", "beta-B");
        return result;
    }

    @Test
    public void assertDecorateMetaDataContexts() {
        MetaDataContexts metaDataContexts = mock(MetaDataContexts.class);
        assertThat(getInitializer().decorateMetaDataContexts(metaDataContexts), is(metaDataContexts));
    }
    
    @Test
    public void assertDecorateTransactionContexts() {
        TransactionContexts transactionContexts = mock(TransactionContexts.class);
        TransactionContexts actualTransactionContexts = getInitializer().decorateTransactionContexts(transactionContexts, XATransactionManagerType.ATOMIKOS.getType());
        assertNotNull(actualTransactionContexts);
        assertThat(actualTransactionContexts.getEngines(), is(transactionContexts.getEngines()));
        assertThat(actualTransactionContexts.getDefaultTransactionManagerEngine(), is(transactionContexts.getDefaultTransactionManagerEngine()));
    }
    
    protected void doEnvironmentPrepare() {
        ShardingSphereServiceLoader.register(YamlRuleConfigurationSwapper.class);
    }
    
    protected void prepareSpecifiedInitializer() {
        setInitializer(new StandardBootstrapInitializer(mock(DistMetaDataPersistRepository.class)));
    }
}
