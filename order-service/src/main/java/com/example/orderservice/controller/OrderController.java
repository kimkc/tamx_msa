package com.example.orderservice.controller;

import com.example.orderservice.client.CatalogServiceClient;
import com.example.orderservice.dto.OrderDto;
import com.example.orderservice.entity.OrderEntity;
import com.example.orderservice.mq.KafkaProducer;
import com.example.orderservice.mq.OrderProducer;
import com.example.orderservice.service.OrdersService;
import com.example.orderservice.vo.RequestOrder;
import com.example.orderservice.vo.ResponseCatalog;
import com.example.orderservice.vo.ResponseOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrdersService orderService;
    private final KafkaProducer kafkaProducer;
    private final CatalogServiceClient catalogServiceClient;
    private final OrderProducer orderProducer;
    private final Environment env;

    @PostMapping(value="/{userId}/orders")
    public ResponseEntity<ResponseOrder> createOrder(@PathVariable("userId") String userId, @RequestBody RequestOrder orderDetails, HttpServletRequest req){

        log.info("Before add orders data");

        //check how much stock is left
        // order-service -> catalog-service
        // restTemplate or openfeign(o)
        boolean isAvailabe = true;
        ResponseCatalog responseCatalog = catalogServiceClient.getCatalog(orderDetails.getProductId());

        if(responseCatalog != null && (responseCatalog.getStock() <= 0 || responseCatalog.getStock() - orderDetails.getQty() < 0)){
            isAvailabe = false;
        }

        if(isAvailabe){
            ModelMapper modelMapper = new ModelMapper();
            modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);

            OrderDto orderDto = modelMapper.map(orderDetails, OrderDto.class);
            orderDto.setUserId(userId);

/*            for multi order service, use kafka for data sync */
//            OrderDto createDto = orderService.createOrder(orderDto);
//            ResponseOrder returnValue = modelMapper.map(createDto, ResponseOrder.class);

            /* send kafka, bottom code is orderService.createOrder job*/
            orderDto.setOrderId(UUID.randomUUID().toString());
            orderDto.setTotalPrice(orderDto.getQty() * orderDto.getUnitPrice());
            // 아래시 항상 random 포트가 나옴. 실제 그 포트인지 알 수 없음
            orderDto.setInstanceId(String.format("%s : %s",env.getProperty("spring.cloud.client.hostname"), env.getProperty("local.server.port")));
            kafkaProducer.send("example-catalog-topic", orderDto);
            ResponseOrder responseOrder = modelMapper.map(orderDto, ResponseOrder.class);

            orderProducer.send("orders", orderDto);


            log.info("After added orders data");
            //return ResponseEntity.status(HttpStatus.CREATED).body(returnValue);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseOrder);
        }else{
            log.info("After added orders data");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

    }

    @GetMapping(value= "/{userId}/orders")
    public ResponseEntity<List<ResponseOrder>> getOrder(@PathVariable("userId") String userId) throws Exception{
        log.info("Before retrieve orders data");
        Iterable<OrderEntity> orderList = orderService.getOrdersByUserId(userId);
        List<ResponseOrder> result = new ArrayList<>();
        orderList.forEach(v -> {
            result.add(new ModelMapper().map(v, ResponseOrder.class));
        });

        //강제로 에러 추가
//        Random rnd = new Random(System.currentTimeMillis());
//        int time = rnd.nextInt(3);
//        if(time % 2 == 0){
//            try{
//                Thread.sleep(10000);
//                throw new Exception("에러 발생!");
//            }catch (Exception ex){
//                log.warn(ex.getMessage());
//            }
//        }

        log.info("After retrieve orders data");

        return ResponseEntity.status(HttpStatus.OK).body(result);
//        throw new Exception("Server not working!");
    }
}
