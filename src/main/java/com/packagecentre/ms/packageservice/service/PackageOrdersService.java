package com.packagecentre.ms.packageservice.service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.packagecentre.ms.packageservice.jpa.dto.*;
import com.packagecentre.ms.packageservice.jpa.entity.Order;
import com.packagecentre.ms.packageservice.jpa.repository.OrdersJPARepository;
import com.packagecentre.ms.packageservice.util.PackageConstants;

@Transactional
@Service
public class PackageOrdersService {
	
	static Logger logger = Logger.getLogger(PackageOrdersService.class.getName());
	
	@Autowired
	OrdersJPARepository orderJPARepo;
	
	@Autowired
	KafkaTemplate<String, DeliveryPackage> kafkaTemplate;
	
	public void packageOrders(String data) {
		
		
		List<ItemAddress> itemAddrListForPackaging = Arrays.asList(new Gson().fromJson(data, ItemAddress[].class));
		
		logger.info("itemAddrListForPackaging size: "+ itemAddrListForPackaging.size());
		for(ItemAddress item: itemAddrListForPackaging) {
			logger.info(Integer.toString(item.getAddrID()));
		}
		
		
		
		Map<Integer, List<ItemAddress>> packagedOrderMap = groupOrdersByAddr(itemAddrListForPackaging);
		
		//transform itemaddress to DlvyPkg list
		List<DeliveryPackage> packageList =  createDlvyPkg(packagedOrderMap);
		
		logger.info("Orders packaged===>>>>         " );
		logger.info(packageList.toString());
		
		//update order table with pkg id and status
		updateOrderPkgIDs(packageList, "PACKAGED", "CREATED");
		
		
		
		//publish Dlvy pkg to topic
		for(DeliveryPackage dlvy: packageList) {
			try {
			
			kafkaPublish(dlvy);
			}
			catch(Exception ex) {
				//if any publishing issue occurs, revert the Order status
				updateOrderPkgIDs(packageList, "PACKAGING FAILED", "PACKAGED");
			}
		}
	}

	
	
	private void kafkaPublish(DeliveryPackage dlvy) throws Exception {
		
		String TOPIC_DLVY = PackageConstants.DLVY_TOPIC;
		logger.info("Kafka Template Publish to Kafka topic: "+ TOPIC_DLVY );
		
		kafkaTemplate.send(TOPIC_DLVY,  dlvy);
		
	}
	
	
	
	public static String convertToByteString(DeliveryPackage object) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            final byte[] byteArray = bos.toByteArray();
            return Base64.getEncoder().encodeToString(byteArray);
        }
    }
	
	
	
	private void updateOrderPkgIDs(List<DeliveryPackage> packageList, String stsNew, String stsOld) {
		
		logger.info("Orders updating===>>>>" );
		
		List<Order> ordList = new ArrayList();
		for(DeliveryPackage dpkg : packageList) {
			logger.info("updating ORDERS table with pkgID : "+ dpkg.getPackageID()+ " for order: "+ dpkg.getOrderID() +" with status: "+ stsNew);
			if(orderJPARepo != null) {
				orderJPARepo.flush();
				int result = orderJPARepo.updateOrderStatusPkgID(dpkg.getPackageID(), stsNew, LocalDateTime.now(),  dpkg.getOrderID(), dpkg.getAddr().getAddrID(), stsOld);
				logger.info("execution result ===> "+ result);
			}
			else {
				logger.info("REPO NOT RESOLVED!!!!!!!!!!!!!!!!!");
			}
			
		}
		
	}
	
	
	
	private Map<Integer, List<ItemAddress>> groupOrdersByAddr(List<ItemAddress> itemAddrList) {
		logger.info("Address grouping");
		
		//lambda to group list items into map based on address id
		Map<Integer, List<ItemAddress>> packagedOrderMap = itemAddrList.stream().collect(Collectors.groupingBy(ItemAddress::getAddrID));
		logger.info("package Map--->>>"+ packagedOrderMap);
		
		return packagedOrderMap;
		
	}
	
	public List<DeliveryPackage> createDlvyPkg(Map<Integer, List<ItemAddress>> packagedOrderMap) {
		
		List<DeliveryPackage> packageList = new ArrayList<DeliveryPackage>();
		
		//
		for(int key : packagedOrderMap.keySet()) {
			DeliveryPackage dp = new DeliveryPackage();
			List<ItemAddress> itemAddrList =  (List<ItemAddress>) packagedOrderMap.get(key);
			String pkgID = null;
			List<Item> itemList = new ArrayList<Item>();
			for(ItemAddress itemAddr: itemAddrList) {
				//set pkgID, address, custid, orderid one time in DP
				if(pkgID == null) {
					pkgID = createPkgID(itemAddr);
					logger.info("Package ID ==>" + pkgID);
					dp.setPackageID(pkgID);
					AddressDetails ad = new AddressDetails(itemAddr.getAddrID(), itemAddr.getLine1(), itemAddr.getLine2(), itemAddr.getCity(), itemAddr.getState(),
							itemAddr.getPostalCode(), itemAddr.getZone());
					dp.setAddr(ad);
					dp.setCustID(itemAddr.getCustID());
					dp.setOrderID(itemAddr.getOrderID());
				}
				//set item list
				Item item = new Item(itemAddr.getItemID(), itemAddr.getItemName(), itemAddr.getPrice(), itemAddr.getQuantity(), itemAddr.getTotal());
				itemList.add(item);					
				
			}
			logger.info("Items in Package["+pkgID+"]="+ itemList.size());
			dp.setItems(itemList);
			itemList = null;//reset itemList for next iteration
			packageList.add(dp);
		}
		
		logger.info("Delivery package count=> "+ packageList.size());
		return packageList;
		
	}
	
	private String createPkgID(ItemAddress itemaddr) {
		
		logger.info("updating pkg id for order");
		
		LocalDateTime dateTime = LocalDateTime.now();
		//logger.info(dateTime.format(DateTimeFormatter.ofPattern("d::MMM::uuuu HH::mm::ss")));
		String dateTimeFormatted = dateTime.format(DateTimeFormatter.ofPattern("MMdduuuuHHmm"));
		
		
		String pkgID = itemaddr.getCustID()+"_"+itemaddr.getAddrID()+"_"+"P"+dateTimeFormatted;		
		logger.info(pkgID);
		
		return pkgID;
	}

}
	
