package taxi;

import taxi.config.kafka.KafkaProcessor;
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
    public void wheneverCalledTaxi_ReceiveCallList(@Payload CalledTaxi calledTaxi){

        if(!calledTaxi.validate()) return;

        System.out.println("\n\n##### listener ReceiveCallList : " + calledTaxi.toJson() + "\n\n");

        // Sample Logic //
        Driver driver = new Driver();
        driver.setCallId(calledTaxi.getId());
        driver.setCost(calledTaxi.getCost());
        driver.setDestination(calledTaxi.getDestination());
        driver.setStatus("Called");
        driverRepository.save(driver);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
