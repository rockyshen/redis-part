package org.rockyshen.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.rockyshen.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "redis分布式锁测试")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @ApiOperation("扣减库存sale，一次卖一个")
    @GetMapping(value = "/inventory/sale")
    public String sale()
    {
        return inventoryService.sale();
    }


//    @ApiOperation("扣减库存，一次卖一个")
//    @GetMapping(value = "/inventory/sale")
////    @ApiOperation("扣减库存saleByRedisson，一次卖一个")
////    @GetMapping(value = "/inventory/saleByRedisson")
//    public String saleByRedisson()
//    {
//        return inventoryService.sale();
////        return inventoryService.saleByRedisson();
//    }
}
