package com.packagecentre.ms.packageservice.messaging;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.packagecentre.ms.packageservice.service.PackageOrdersService;



@Component
public class KafkaConsumer {
	
	static Logger logger = Logger.getLogger(KafkaConsumer.class.getName());
	
	@Autowired
	private PackageOrdersService pkgSvc;
	
	
	@KafkaListener(topics="pkgTopic", groupId="pkgTopicGroup")
	public void consume(String data)  {
		logger.info("data received"+ data);
		
		
		//package the orders received based on address
		pkgSvc.packageOrders(data);		
		
		
	}
	
}
