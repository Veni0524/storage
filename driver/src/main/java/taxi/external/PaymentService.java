
package taxi.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

// @FeignClient(name="payment", url="http://Payment:8080")         //Delete
@FeignClient(name="payment", url="${feign.client.url.paymentUrl}") //Insert
public interface PaymentService {

    @RequestMapping(method= RequestMethod.GET, path="/payments/settleCost")
    public Integer settleCost(@RequestParam("callId") Long callId) throws Exception;



}