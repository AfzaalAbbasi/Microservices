package com.programmingtechie.orderservice.service;

import com.programmingtechie.orderservice.dtos.InventoryResponse;
import com.programmingtechie.orderservice.dtos.OrderLineItemsDto;
import com.programmingtechie.orderservice.dtos.OrderRequest;
import com.programmingtechie.orderservice.model.Order;
import com.programmingtechie.orderservice.model.OrderLineItems;
import com.programmingtechie.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    //private final WebClient webClient;
    private final WebClient.Builder webClientBuilder;

    public void placeOrder(OrderRequest orderRequest){
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList().stream().map(this::mapToDto).toList();
        order.setOrderLineItemsList(orderLineItems);

        List<String> skucodelist = order.getOrderLineItemsList()
                .stream()
                .map(OrderLineItems::getSkuCode)
                .toList();
        //inventory service place order if inventory is in stock
        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode",skucodelist)
                                .build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        Boolean allProductsResponse = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::getIsInStock);

        log.info("Received inventory", allProductsResponse);

        if(allProductsResponse){
            orderRepository.save(order);
        }else{
            throw new IllegalArgumentException("Prodcut not in stock,pleae try again later");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems items = new OrderLineItems();
        items.setPrice(orderLineItemsDto.getPrice());
        items.setQuantity(orderLineItemsDto.getQuantity());
        items.setSkuCode(orderLineItemsDto.getSkuCode());

        return items;
    }
}
