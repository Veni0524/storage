
package taxi.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

// @FeignClient(name="Payment", url="http://Payment:8083")         //Delete
@FeignClient(name="payment", url="${feign.client.url.paymentUrl}") //Insert
public interface PaymentService {

    @RequestMapping(method= RequestMethod.GET, path="/payments")
    public void savePayment(@RequestBody Payment payment);

}