package com.example.bank.config;

import com.example.bank.api.NonXABackendAPI;
import com.example.bank.model.Constants;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import javax.jms.Queue;
import javax.sql.DataSource;

@Configuration
public class NonXAConfiguration {

    // JMS configuration
    @Bean("jms")
    public JmsTemplate jms() {
        JmsTemplate template = new JmsTemplate(connectionFactory());
        template.setSessionTransacted(true);
        return template;
    }

    @Bean("moneyTransferQueue")
    public Queue requestQueue() {
        return new ActiveMQQueue(Constants.QUEUE_TRANSFER);
    }

    @Bean("cacheUpdateQueue")
    public Queue CacheUpdateQueue() {
        return new ActiveMQQueue(Constants.QUEUE_CACHE_UPDATE);
    }

    @Autowired
    @Bean("jmsContainer")
    public DefaultMessageListenerContainer container(NonXABackendAPI api) {
        DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
        container.setConnectionFactory(connectionFactory());
        container.setDestination(requestQueue());
        container.setMessageListener(api);
        container.setSessionTransacted(true);
        container.setTransactionTimeout(100);
        return container;
    }

    @Bean("connectionFactory")
    public ActiveMQConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory amq = new ActiveMQConnectionFactory("tcp://localhost:61616");

        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setMaximumRedeliveries(10);
        redeliveryPolicy.setInitialRedeliveryDelay(500); // 5 seconds redelivery delay
        redeliveryPolicy.setBackOffMultiplier(2);
        redeliveryPolicy.setUseExponentialBackOff(true);

        amq.setRedeliveryPolicy(redeliveryPolicy);

        // TODO define policy for specific queues
//        RedeliveryPolicyMap map = new RedeliveryPolicyMap();
//        map.setRedeliveryPolicyEntries();

        return amq;
    }

    // DB configuration
    @Bean("local")
    public JdbcTemplate local() {
        return new JdbcTemplate(localDS());
    }

    @Bean("partner")
    public JdbcTemplate partner() {
        return new JdbcTemplate(partnerDS());
    }

    @Primary
    @Bean("localDataSource")
    public DataSource localDS() {
        MysqlDataSource xaDataSource = new MysqlDataSource();
        xaDataSource.setPort(3306);
        xaDataSource.setServerName("localhost");
        xaDataSource.setUser("root");
        xaDataSource.setPassword("root");
        xaDataSource.setDatabaseName("bank1");
        return xaDataSource;
    }

    @Bean("partnerDataSource")
    public DataSource partnerDS() {
        MysqlDataSource xaDataSource = new MysqlDataSource();
        xaDataSource.setPort(3306);
        xaDataSource.setServerName("localhost");
        xaDataSource.setUser("root");
        xaDataSource.setPassword("root");
        xaDataSource.setDatabaseName("partner_bank");
        return xaDataSource;
    }
}