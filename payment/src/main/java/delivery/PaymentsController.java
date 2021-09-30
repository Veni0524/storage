package delivery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

 @RestController
 public class PaymentsController {

    @Autowired 
    PaymentsRepository paymentsRepository;

    @RequestMapping(value = "/payments/settleCost",
    method = RequestMethod.GET,
    produces = "application/json;charset=UTF-8")

public Integer settleCost(@RequestParam("deliveryId") Long deliveryId)
    throws Exception {
       
        System.out.println("##### /payment/settleCost  called #####");
        
        Optional<Payments> paymentsOptional = paymentsRepository.findById(deliveryId);
        Payments payments = paymentsOptional.get();

        return payments.getCost();
        
     }

 }