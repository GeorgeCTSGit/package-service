package com.packagecentre.ms.packageservice.messaging;

import java.util.*;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.*;
import org.springframework.context.annotation.*;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.packagecentre.ms.packageservice.jpa.dto.DeliveryPackage;
import com.packagecentre.ms.packageservice.util.PackageConstants;

@EnableKafka
@Configuration
public class KafkaConfig {
	
	//CONSUMER 
	
	@Bean
    public ConsumerFactory<String, String> consumerFactory()
    {
		Map<String, Object> config = new HashMap<>();
		
		config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, new StringDeserializer().getClass().getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, new StringDeserializer().getClass().getName());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, PackageConstants.PKG_TOPIC_GRP_ID);
        config.put(ConsumerConfig.CLIENT_ID_CONFIG, PackageConstants.CLIENT_ID);
		
		return new DefaultKafkaConsumerFactory<>(config);
    }
	

	
	// Creating a Listener
    public ConcurrentKafkaListenerContainerFactory
    concurrentKafkaListenerContainerFactory()
    {
        ConcurrentKafkaListenerContainerFactory<
            String, String> factory
            = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
	

    
    
    //PRODUCER
    
    @Bean
	public ProducerFactory<String, DeliveryPackage> producerFactory(){
		Map<String, Object> configMap = new HashMap<>();
		
		
		configMap.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
		configMap.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		configMap.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,JsonSerializer.class);
		
		
		return new DefaultKafkaProducerFactory<>(configMap);
	}
	
	
	@Bean	  
    public KafkaTemplate<String, DeliveryPackage> kafkaTemplate()
    {
  
        return new KafkaTemplate<>(producerFactory());
		
    }

}
