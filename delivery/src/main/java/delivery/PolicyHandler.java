package delivery;

import delivery.config.kafka.KafkaProcessor;

import java.util.Optional;

import delivery.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired DeliveryRepository deliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverUpdateStatus_UpdatedStatus(@Payload UpdateStatus updateStatus){

        if(!updateStatus.validate()) return;

        System.out.println("\n\n##### listener UpdatedStatus : " + updateStatus.toJson() + "\n\n");



        // Sample Logic //
        // Delivery delivery = new Delivery();
        // deliveryRepository.save(delivery);
        Optional<Delivery> deliveryOptional = deliveryRepository.findById(updateStatus.getDeliveryId());
        Delivery delivery = deliveryOptional.get();
        delivery.setStatus(updateStatus.getStatus());

        deliveryRepository.save(delivery);


    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}