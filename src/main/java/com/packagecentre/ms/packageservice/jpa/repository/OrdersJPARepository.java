package com.packagecentre.ms.packageservice.jpa.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.*;
//import org.springframework.data.jdbc.repository.query.Modifying;
//import org.springframework.data.jpa.repository.*;
//import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.packagecentre.ms.packageservice.jpa.entity.Order;

@Repository
public interface OrdersJPARepository extends JpaRepository<Order, Integer>{
	
	
	List<Order> findByOrderID(String orderID);
	List<Order> findByOrderIDAndAddrID(String orderID, int addrID);
	List<Order> findByAddrID(int addrID);
	
	
	
	@Modifying(clearAutomatically = true)
	@Query(value = "UPDATE ORDERS O SET O.PKG_ID = ?1, O.ORD_STATUS = ?2, O.STATUS_DT = ?3 WHERE O.ORD_ID = ?4 AND O.ADDR_ID = ?5 AND O.ORD_STATUS = ?6", nativeQuery = true)
	int updateOrderStatusPkgID(@Param("pkgID") String pkgID, @Param("statusNew") String statusNew, @Param("orderStatusDate") LocalDateTime orderStatusDate,
			@Param("ordID") String ordID, @Param("addrID") int addrID,@Param("statusOld") String statusOld);
	
 
}