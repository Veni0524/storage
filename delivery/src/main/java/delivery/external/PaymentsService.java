package delivery.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

//@FeignClient(name="payment", url="http://payment:8080")
@FeignClient(name="payments", url="${feign.client.url.paymentsUrl}") //Insert
public interface PaymentsService {
    @RequestMapping(method= RequestMethod.GET, path="/payments")
    public void savePayments(@RequestBody Payments payments);

}

