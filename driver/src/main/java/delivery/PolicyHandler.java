package delivery;

import delivery.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired DriverRepository driverRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCalledDriver_ReceiveCall(@Payload CalledDriver calledDriver){

        if(!calledDriver.validate()) return;

        System.out.println("\n\n##### listener ReceiveCall : " + calledDriver.toJson() + "\n\n");



        // Sample Logic //
        // Driver driver = new Driver();
        // driverRepository.save(driver);
        Driver driver = new Driver();
        driver.setDeliveryId(calledDriver.getId());
        driver.setCost(calledDriver.getCost());
        driver.setDestination(calledDriver.getDestination());
        driver.setStatus("Called");
        driverRepository.save(driver);

    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}